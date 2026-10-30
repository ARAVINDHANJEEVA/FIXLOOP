import apiClient from './client';
import { IncidentSummary, ClusterMember } from '../types';

const toIncidentMember = (m: any): ClusterMember => ({
  id: String(m.id),
  token: m.token || '',
  description: m.description || '',
  status: m.status,
  clusterScore: m.clusterScore ? Number(m.clusterScore) : undefined,
  createdAt: m.createdAt || '',
});

const toIncidentSummary = (dto: any): IncidentSummary => ({
  id: String(dto.id),
  status: dto.status || 'OPEN',
  complaintCount: Number(dto.complaintCount || 0),
  summary: dto.summary,
  members: Array.isArray(dto.members) ? dto.members.map(toIncidentMember) : [],
  createdAt: dto.createdAt || '',
  lastComplaintAt: dto.lastComplaintAt || '',
});

export const incidentApi = {
  /**
   * GET /api/incidents?categoryId=3&status=OPEN
   * Returns open incidents for the manual-cluster target picker dropdown.
   */
  getOpenByCategory: async (categoryId?: string | number): Promise<IncidentSummary[]> => {
    const params: Record<string, any> = { status: 'OPEN' };
    if (categoryId) params.categoryId = categoryId;
    const res = await apiClient.get('/api/incidents', { params });
    return Array.isArray(res.data) ? res.data.map(toIncidentSummary) : [];
  },

  /**
   * GET /api/incidents/{id}
   * Returns full incident detail with all member complaints.
   */
  getById: async (id: string | number): Promise<IncidentSummary> => {
    const res = await apiClient.get(`/api/incidents/${id}`);
    return toIncidentSummary(res.data);
  },
};

export default incidentApi;
