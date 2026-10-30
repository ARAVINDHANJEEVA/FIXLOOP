import apiClient from './client';
import { Vendor } from '../types';
import { toVendor } from './mappers';

export const vendorApi = {
  getVendors: async (skillIds?: number[]): Promise<Vendor[]> => {
    const params = skillIds && skillIds.length > 0 ? { skillIds: skillIds.join(',') } : {};
    const res = await apiClient.get('/api/vendors', { params });
    return Array.isArray(res.data) ? res.data.map(toVendor) : [];
  },
};

export default vendorApi;
