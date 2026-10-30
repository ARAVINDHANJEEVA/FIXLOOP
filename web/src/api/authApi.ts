import apiClient from './client';
import { AuthUser } from '../types';

export interface LoginResponse {
  token: string;
  type: string;
  expiresIn: number;
  user: {
    id: number;
    name: string;
    email: string;
    role: 'ADMIN' | 'STAFF' | 'STUDENT';
    restrictedAccess: boolean;
    avatarInitials: string;
  };
}

export const authApi = {
  login: async (email: string, password: string): Promise<{ success: boolean; user?: AuthUser; error?: string }> => {
    try {
      const response = await apiClient.post<LoginResponse>('/api/auth/login', {
        email,
        password,
      });

      const data = response.data;
      const authUser: AuthUser = {
        id: String(data.user.id),
        name: data.user.name,
        email: data.user.email,
        role: data.user.role === 'STAFF' ? 'STAFF' : 'ADMIN',
        restrictedAccess: Boolean(data.user.restrictedAccess),
        avatarInitials: data.user.avatarInitials,
      };

      sessionStorage.setItem('fixloop_token', data.token);
      sessionStorage.setItem('fixloop_user', JSON.stringify(authUser));

      return { success: true, user: authUser };
    } catch (err: any) {
      return {
        success: false,
        error: err.message || 'Login failed. Please verify credentials.',
      };
    }
  },

  logout: () => {
    sessionStorage.removeItem('fixloop_token');
    sessionStorage.removeItem('fixloop_user');
  },

  getStoredUser: (): AuthUser | null => {
    const saved = sessionStorage.getItem('fixloop_user');
    if (!saved) return null;
    try {
      return JSON.parse(saved);
    } catch {
      return null;
    }
  },

  getToken: (): string | null => {
    return sessionStorage.getItem('fixloop_token');
  },
};

export default authApi;
