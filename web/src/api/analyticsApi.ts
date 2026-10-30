import apiClient from './client';
import { HotspotData, SLATrendData, CategoryVolumeData, StaffPerformanceData } from '../types';

export const analyticsApi = {
  getHotspots: async (days: number = 30): Promise<HotspotData[]> => {
    const res = await apiClient.get<HotspotData[]>('/api/analytics/hotspots', {
      params: { days },
    });
    return Array.isArray(res.data) ? res.data : [];
  },

  getSlaTrends: async (weeks: number = 12): Promise<SLATrendData[]> => {
    const res = await apiClient.get<SLATrendData[]>('/api/analytics/sla-trends', {
      params: { weeks },
    });
    return Array.isArray(res.data) ? res.data : [];
  },

  getCategoryVolume: async (days: number = 30): Promise<CategoryVolumeData[]> => {
    const res = await apiClient.get<CategoryVolumeData[]>('/api/analytics/category-volume', {
      params: { days },
    });
    return Array.isArray(res.data) ? res.data : [];
  },

  getStaffPerformance: async (days: number = 30): Promise<StaffPerformanceData[]> => {
    const res = await apiClient.get<StaffPerformanceData[]>('/api/analytics/staff-performance', {
      params: { days },
    });
    return Array.isArray(res.data) ? res.data : [];
  },
};

export default analyticsApi;
