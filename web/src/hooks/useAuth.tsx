import React, { createContext, useContext, useState, useCallback, useEffect, ReactNode } from 'react';
import { AuthUser } from '../types';
import { authApi } from '../api/authApi';

interface AuthContextType {
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<{ success: boolean; error?: string }>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

// Fallback users for offline development when backend server is not running
export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(() => authApi.getStoredUser());

  // Listen for unauthorized 401 events from the API client
  useEffect(() => {
    const handleUnauthorized = () => {
      setUser(null);
      authApi.logout();
    };
    window.addEventListener('fixloop:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('fixloop:unauthorized', handleUnauthorized);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const result = await authApi.login(email, password);
    if (result.success && result.user) {
      setUser(result.user);
      return { success: true };
    }

    return { success: false, error: result.error || 'Invalid email or password' };
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    authApi.logout();
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
