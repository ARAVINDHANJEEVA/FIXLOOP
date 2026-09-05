import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { AuthUser } from '../types';

interface AuthContextType {
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<{ success: boolean; error?: string }>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

const MOCK_USERS: Record<string, AuthUser & { password: string }> = {
  'admin@sentinel.edu': {
    id: 'adm-1',
    name: 'Admin Krishnan',
    email: 'admin@sentinel.edu',
    password: 'admin123',
    role: 'ADMIN',
    restrictedAccess: true,
    avatarInitials: 'AK',
  },
  'staff@sentinel.edu': {
    id: 'stf-5',
    name: 'Deepak Joshi',
    email: 'staff@sentinel.edu',
    password: 'staff123',
    role: 'STAFF',
    avatarInitials: 'DJ',
  },
};

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const saved = sessionStorage.getItem('sentinel_user');
    return saved ? JSON.parse(saved) : null;
  });

  const login = useCallback(async (email: string, password: string) => {
    await new Promise((res) => setTimeout(res, 800));
    const found = MOCK_USERS[email.toLowerCase()];
    if (!found || found.password !== password) {
      return { success: false, error: 'Invalid credentials. Please try again.' };
    }
    const { password: _, ...authUser } = found;
    setUser(authUser);
    sessionStorage.setItem('sentinel_user', JSON.stringify(authUser));
    return { success: true };
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    sessionStorage.removeItem('sentinel_user');
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};

export default AuthContext;
