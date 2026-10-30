import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useQueryClient } from '@tanstack/react-query';
import { useAuth } from './useAuth';
import { useToast } from './useToast';
import { API_BASE_URL } from '../api/client';

export const useWebSocket = () => {
  const { user, isAuthenticated } = useAuth();
  const { addToast } = useToast();
  const queryClient = useQueryClient();
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!isAuthenticated || !user) {
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
      }
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
      reconnectDelay: 8000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        // Topic subscriptions based on user role
        if (user.role === 'ADMIN') {
          // 1. Admin incoming queue
          client.subscribe('/topic/admin/queue', (message) => {
            try {
              const body = JSON.parse(message.body);
              queryClient.invalidateQueries({ queryKey: ['complaints'] });
              addToast({
                type: 'info',
                title: 'New Complaint Submitted',
                message: `${body.token || 'Complaint'} — ${body.categoryName || 'General'} has been received`,
                duration: 6000,
              });
            } catch (e) {
              queryClient.invalidateQueries({ queryKey: ['complaints'] });
            }
          });

          // 2. Admin alerts (manual assignment required, urgent triage)
          client.subscribe('/topic/admin/alerts', (message) => {
            try {
              const body = JSON.parse(message.body);
              queryClient.invalidateQueries({ queryKey: ['complaints'] });
              addToast({
                type: 'warning',
                title: 'Triage Alert',
                message: body.message || `Complaint ${body.complaintId || ''} requires manual intervention`,
                duration: 7000,
              });
            } catch (e) {
              queryClient.invalidateQueries({ queryKey: ['complaints'] });
            }
          });

          // 3. SLA alerts (at-risk and breaches)
          client.subscribe('/topic/sla/alerts', (message) => {
            try {
              const body = JSON.parse(message.body);
              queryClient.invalidateQueries({ queryKey: ['complaints'] });
              queryClient.invalidateQueries({ queryKey: ['sla'] });
              const isBreach = body.type === 'SLA_BREACH';
              addToast({
                type: isBreach ? 'error' : 'warning',
                title: isBreach ? 'SLA Breached' : 'SLA At Risk',
                message: body.message || `Complaint #${body.complaintId} reached SLA threshold`,
                duration: 7000,
              });
            } catch (e) {
              queryClient.invalidateQueries({ queryKey: ['sla'] });
            }
          });
        }

        if (user.role === 'STAFF') {
          // Personal queue assignments
          client.subscribe(`/topic/staff/${user.id}/queue`, (message) => {
            try {
              const body = JSON.parse(message.body);
              queryClient.invalidateQueries({ queryKey: ['complaints'] });
              queryClient.invalidateQueries({ queryKey: ['workload'] });
              addToast({
                type: 'info',
                title: 'New Assignment',
                message: `Ticket ${body.token || ''} assigned to you in ${body.categoryName || 'your department'}`,
                duration: 7000,
              });
            } catch (e) {
              queryClient.invalidateQueries({ queryKey: ['complaints'] });
            }
          });

          // SLA notifications relevant to staff
          client.subscribe('/topic/sla/alerts', () => {
            queryClient.invalidateQueries({ queryKey: ['complaints'] });
            queryClient.invalidateQueries({ queryKey: ['workload'] });
          });
        }
      },
      onStompError: (frame) => {
        // Silent recovery on websocket disconnect
      },
      onWebSocketError: () => {
        // Silent backoff
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
      }
    };
  }, [isAuthenticated, user, queryClient, addToast]);
};

export default useWebSocket;
