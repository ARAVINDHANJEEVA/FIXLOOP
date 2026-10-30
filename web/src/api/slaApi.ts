import apiClient from './client';
import { Complaint, SLARule } from '../types';
import { toComplaint, toSLARule } from './mappers';

export const slaApi = {
  getAtRisk: async (): Promise<Complaint[]> => {
    const res = await apiClient.get('/api/sla/at-risk');
    return Array.isArray(res.data) ? res.data.map(toComplaint) : [];
  },

  getBreached: async (): Promise<Complaint[]> => {
    const res = await apiClient.get('/api/sla/breached');
    return Array.isArray(res.data) ? res.data.map(toComplaint) : [];
  },

  forceEscalate: async (id: number | string): Promise<void> => {
    await apiClient.post(`/api/sla/${id}/force-escalate`);
  },

  getRules: async (): Promise<SLARule[]> => {
    const res = await apiClient.get('/api/sla-rules');
    return Array.isArray(res.data) ? res.data.map(toSLARule) : [];
  },

  updateRule: async (
    id: number | string,
    payload: { resolutionHours?: number; escalationHours?: number }
  ): Promise<SLARule> => {
    const res = await apiClient.patch(`/api/sla-rules/${id}`, payload);
    return toSLARule(res.data);
  },
};

export default slaApi;
