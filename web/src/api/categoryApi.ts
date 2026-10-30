import apiClient from './client';
import { Category } from '../types';
import { toCategory } from './mappers';

export const categoryApi = {
  getAll: async (): Promise<Category[]> => {
    const res = await apiClient.get('/api/categories');
    return Array.isArray(res.data) ? res.data.map(toCategory) : [];
  },

  update: async (id: number | string, payload: Partial<Category>): Promise<Category> => {
    const body: Record<string, any> = {};
    if (payload.name) body.name = payload.name;
    if (payload.baseSeverityWeight !== undefined) body.baseSeverityWeight = payload.baseSeverityWeight;
    if (payload.slaHours !== undefined) body.defaultSlaHours = payload.slaHours;
    if (payload.formSchema) body.formSchema = JSON.stringify(payload.formSchema);

    const res = await apiClient.patch(`/api/categories/${id}`, body);
    return toCategory(res.data);
  },
};

export default categoryApi;
