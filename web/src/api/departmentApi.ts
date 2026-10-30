import apiClient from './client';
import { Department } from '../types';
import { toDepartment } from './mappers';

export interface DepartmentCreatePayload {
  name: string;
  code?: string;
  description?: string;
  headOfDept?: string;
}

export const departmentApi = {
  getAll: async (): Promise<Department[]> => {
    const res = await apiClient.get('/api/departments');
    return Array.isArray(res.data) ? res.data.map(toDepartment) : [];
  },

  create: async (payload: DepartmentCreatePayload): Promise<Department> => {
    const res = await apiClient.post('/api/departments', payload);
    return toDepartment(res.data);
  },
};

export default departmentApi;
