import apiClient from './client';
import { Staff } from '../types';
import { toStaff } from './mappers';

export interface StaffCreatePayload {
  name: string;
  email: string;
  password?: string;
  jobTitle?: string;
  departmentId: number | string;
  skillIds?: number[];
  status?: string;
}

export interface StaffUpdatePayload {
  name?: string;
  jobTitle?: string;
  departmentId?: number | string;
  skillIds?: number[];
  status?: string;
}

export interface WorkloadData {
  userId: number;
  name: string;
  openTickets: number;
  currentWeightedLoad: number;
  resolvedThisWeek: number;
  avgResolutionHours: number;
}

export const staffApi = {
  getAll: async (): Promise<Staff[]> => {
    const res = await apiClient.get('/api/staff');
    return Array.isArray(res.data) ? res.data.map(toStaff) : [];
  },

  create: async (payload: StaffCreatePayload): Promise<Staff> => {
    const body = {
      ...payload,
      departmentId: Number(payload.departmentId),
    };
    const res = await apiClient.post('/api/staff', body);
    return toStaff(res.data);
  },

  update: async (id: number | string, payload: StaffUpdatePayload): Promise<Staff> => {
    const body = {
      ...payload,
      departmentId: payload.departmentId ? Number(payload.departmentId) : undefined,
    };
    const res = await apiClient.patch(`/api/staff/${id}`, body);
    return toStaff(res.data);
  },

  getWorkload: async (): Promise<WorkloadData> => {
    const res = await apiClient.get('/api/staff/workload');
    return {
      userId: res.data.userId,
      name: res.data.name,
      openTickets: Number(res.data.openTickets || 0),
      currentWeightedLoad: Number(res.data.currentWeightedLoad || 0),
      resolvedThisWeek: Number(res.data.resolvedThisWeek || 0),
      avgResolutionHours: Number(res.data.avgResolutionHours || 0),
    };
  },
};

export default staffApi;
