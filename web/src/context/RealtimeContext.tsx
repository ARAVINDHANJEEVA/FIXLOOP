import React, { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../hooks/useToast';
import { API_BASE_URL } from '../api/client';
import { Complaint } from '../types';
import { toComplaint } from '../api/mappers';

export interface RealtimeEvent {
  type: 'NEW' | 'UPDATE';
  complaint: Complaint;
  message?: string;
}

type QueueListener = (event: RealtimeEvent) => void;
type ComplaintListener = (complaint: Complaint) => void;

interface RealtimeContextType {
  isConnected: boolean;
  subscribeQueue: (listener: QueueListener) => () => void;
  subscribeComplaint: (idOrToken: string, listener: ComplaintListener) => () => void;
}

const RealtimeContext = createContext<RealtimeContextType | null>(null);

export const RealtimeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, isAuthenticated } = useAuth();
  const { addToast } = useToast();
  const queryClient = useQueryClient();
  const [isConnected, setIsConnected] = useState(false);

  const clientRef = useRef<Client | null>(null);
  const queueListenersRef = useRef<Set<QueueListener>>(new Set());
  const complaintListenersRef = useRef<Map<string, Set<ComplaintListener>>>(new Map());
  const recentToastRef = useRef<Map<string, number>>(new Map());

  // Debounced toast helper to avoid duplicate notifications across multiple topics
  const notifyOnce = useCallback((key: string, toastOptions: Parameters<typeof addToast>[0]) => {
    const now = Date.now();
    const lastTime = recentToastRef.current.get(key) || 0;
    if (now - lastTime < 3000) {
      return; // Skip duplicate within 3s
    }
    recentToastRef.current.set(key, now);
    addToast(toastOptions);
  }, [addToast]);

  const dispatchToQueue = useCallback((event: RealtimeEvent) => {
    queueListenersRef.current.forEach((listener) => {
      try {
        listener(event);
      } catch (err) {
        console.error('Error in queue listener:', err);
      }
    });
  }, []);

  const dispatchToComplaint = useCallback((complaint: Complaint) => {
    const keys = [complaint.id, complaint.token].filter(Boolean);
    keys.forEach((k) => {
      const listeners = complaintListenersRef.current.get(k);
      if (listeners) {
        listeners.forEach((listener) => {
          try {
            listener(complaint);
          } catch (err) {
            console.error(`Error in complaint listener for ${k}:`, err);
          }
        });
      }
    });
  }, []);

  const subscribeQueue = useCallback((listener: QueueListener) => {
    queueListenersRef.current.add(listener);
    return () => {
      queueListenersRef.current.delete(listener);
    };
  }, []);

  const subscribeComplaint = useCallback((idOrToken: string, listener: ComplaintListener) => {
    if (!complaintListenersRef.current.has(idOrToken)) {
      complaintListenersRef.current.set(idOrToken, new Set());
    }
    complaintListenersRef.current.get(idOrToken)!.add(listener);

    return () => {
      const set = complaintListenersRef.current.get(idOrToken);
      if (set) {
        set.delete(listener);
        if (set.size === 0) {
          complaintListenersRef.current.delete(idOrToken);
        }
      }
    };
  }, []);

  useEffect(() => {
    if (!isAuthenticated || !user) {
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
        setIsConnected(false);
      }
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
      reconnectDelay: 6000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setIsConnected(true);

        // 1. General complaints topic (dispatches updates across the app)
        client.subscribe('/topic/complaints', (message) => {
          try {
            const raw = JSON.parse(message.body);
            const complaint = toComplaint(raw);
            if (!complaint.id && !complaint.token) return;

            queryClient.invalidateQueries({ queryKey: ['complaints'] });

            const isStudentReply = complaint.statusHistory?.some((h) =>
              h.note?.toLowerCase().includes('student reply') ||
              h.note?.toLowerCase().includes('student provided information')
            );

            if (isStudentReply) {
              notifyOnce(`reply-${complaint.token}`, {
                type: 'info',
                title: 'Student Reply Received',
                message: `${complaint.token}: Student provided response. Status updated to IN_PROGRESS.`,
                duration: 7000,
              });
            }

            dispatchToQueue({ type: 'UPDATE', complaint });
            dispatchToComplaint(complaint);
          } catch (e) {
            console.error('Failed to parse /topic/complaints message', e);
          }
        });

        // 2. Admin incoming queue
        if (user.role === 'ADMIN') {
          client.subscribe('/topic/admin/queue', (message) => {
            try {
              const raw = JSON.parse(message.body);
              const complaint = toComplaint(raw);
              queryClient.invalidateQueries({ queryKey: ['complaints'] });

              const isNew = complaint.status === 'SUBMITTED';
              if (isNew) {
                notifyOnce(`new-${complaint.token}`, {
                  type: 'info',
                  title: 'New Complaint Submitted',
                  message: `${complaint.token || 'Complaint'} — ${complaint.categoryName || 'General'} has been received`,
                  duration: 6000,
                });
                dispatchToQueue({ type: 'NEW', complaint });
              } else {
                dispatchToQueue({ type: 'UPDATE', complaint });
              }
              dispatchToComplaint(complaint);
            } catch (e) {
              queryClient.invalidateQueries({ queryKey: ['complaints'] });
            }
          });

          // Admin triage alerts
          client.subscribe('/topic/admin/alerts', (message) => {
            try {
              const body = JSON.parse(message.body);
              queryClient.invalidateQueries({ queryKey: ['complaints'] });
              notifyOnce(`alert-${body.complaintId || body.token}`, {
                type: 'warning',
                title: 'Triage Alert',
                message: body.message || `Complaint ${body.token || body.complaintId || ''} requires manual intervention`,
                duration: 7000,
              });
            } catch (e) {
              queryClient.invalidateQueries({ queryKey: ['complaints'] });
            }
          });
        }

        // 3. Staff personal queue
        if (user.role === 'STAFF') {
          client.subscribe(`/topic/staff/${user.id}/queue`, (message) => {
            try {
              const raw = JSON.parse(message.body);
              const complaint = toComplaint(raw);
              queryClient.invalidateQueries({ queryKey: ['complaints'] });
              queryClient.invalidateQueries({ queryKey: ['workload'] });

              const isStudentReply = complaint.statusHistory?.some((h) =>
                h.note?.toLowerCase().includes('student reply')
              );

              if (isStudentReply) {
                notifyOnce(`reply-${complaint.token}`, {
                  type: 'info',
                  title: 'Student Reply Received',
                  message: `${complaint.token}: Student replied to information request`,
                  duration: 7000,
                });
              } else {
                notifyOnce(`assign-${complaint.token}`, {
                  type: 'info',
                  title: 'New Queue Update',
                  message: `Ticket ${complaint.token || ''} assigned or updated in ${complaint.categoryName || 'your department'}`,
                  duration: 7000,
                });
              }

              dispatchToQueue({ type: 'UPDATE', complaint });
              dispatchToComplaint(complaint);
            } catch (e) {
              queryClient.invalidateQueries({ queryKey: ['complaints'] });
            }
          });
        }

        // 4. SLA alerts (Admin & Staff)
        client.subscribe('/topic/sla/alerts', (message) => {
          try {
            const body = JSON.parse(message.body);
            queryClient.invalidateQueries({ queryKey: ['complaints'] });
            queryClient.invalidateQueries({ queryKey: ['sla'] });
            const isBreach = body.type === 'SLA_BREACH';
            notifyOnce(`sla-${body.complaintId}`, {
              type: isBreach ? 'error' : 'warning',
              title: isBreach ? 'SLA Breached' : 'SLA At Risk',
              message: body.message || `Complaint #${body.complaintId} reached SLA threshold`,
              duration: 7000,
            });
          } catch (e) {
            queryClient.invalidateQueries({ queryKey: ['sla'] });
          }
        });
      },
      onDisconnect: () => {
        setIsConnected(false);
      },
      onStompError: () => {
        setIsConnected(false);
      },
      onWebSocketError: () => {
        setIsConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
        setIsConnected(false);
      }
    };
  }, [isAuthenticated, user, queryClient, notifyOnce, dispatchToQueue, dispatchToComplaint]);

  return (
    <RealtimeContext.Provider value={{ isConnected, subscribeQueue, subscribeComplaint }}>
      {children}
    </RealtimeContext.Provider>
  );
};

export const useRealtime = (): RealtimeContextType => {
  const context = useContext(RealtimeContext);
  if (!context) {
    throw new Error('useRealtime must be used within a RealtimeProvider');
  }
  return context;
};

export default RealtimeContext;
