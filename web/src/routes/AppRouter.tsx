import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import ProtectedRoute from './ProtectedRoute';
import AppShell from '../components/layout/AppShell';
import Login from '../pages/Login';

const AdminQueue = lazy(() => import('../pages/admin/AdminQueue'));
const AdminComplaintDetail = lazy(() => import('../pages/admin/AdminComplaintDetail'));
const SLAMonitor = lazy(() => import('../pages/admin/SLAMonitor'));
const Analytics = lazy(() => import('../pages/admin/Analytics'));
const RestrictedComplaints = lazy(() => import('../pages/admin/RestrictedComplaints'));
const StaffManagement = lazy(() => import('../pages/admin/StaffManagement'));
const Settings = lazy(() => import('../pages/admin/Settings'));
const ResolutionHistory = lazy(() => import('../pages/common/ResolutionHistory'));
const StaffQueue = lazy(() => import('../pages/staff/StaffQueue'));
const StaffComplaintDetail = lazy(() => import('../pages/staff/StaffComplaintDetail'));
const Workload = lazy(() => import('../pages/staff/Workload'));

const LoadingFallback = () => (
  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', gap: 12, color: 'var(--text-3)', fontSize: '0.875rem' }}>
    <div className="skeleton" style={{ width: 24, height: 24, borderRadius: '50%' }} />
    Loading...
  </div>
);

const RootRedirect: React.FC = () => {
  const { user, isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <Navigate to={user?.role === 'ADMIN' ? '/admin/queue' : '/staff/queue'} replace />;
};

const AppRouter: React.FC = () => {
  return (
    <BrowserRouter>
      <Suspense fallback={<LoadingFallback />}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<RootRedirect />} />

          {/* Admin Routes */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute requiredRole="ADMIN">
                <AppShell />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/admin/queue" replace />} />
            <Route path="queue" element={<AdminQueue />} />
            <Route path="complaint/:id" element={<AdminComplaintDetail />} />
            <Route path="complaints/:id" element={<AdminComplaintDetail />} />
            <Route path="sla" element={<SLAMonitor />} />
            <Route path="analytics" element={<Analytics />} />
            <Route
              path="restricted"
              element={
                <ProtectedRoute requiredRole="ADMIN" requireRestricted>
                  <RestrictedComplaints />
                </ProtectedRoute>
              }
            />
            <Route path="staff" element={<StaffManagement />} />
            <Route path="history" element={<ResolutionHistory role="admin" />} />
            <Route path="settings" element={<Settings />} />
          </Route>

          {/* Staff Routes */}
          <Route
            path="/staff"
            element={
              <ProtectedRoute requiredRole="STAFF">
                <AppShell />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/staff/queue" replace />} />
            <Route path="queue" element={<StaffQueue />} />
            <Route path="complaint/:id" element={<StaffComplaintDetail />} />
            <Route path="complaints/:id" element={<StaffComplaintDetail />} />
            <Route path="history" element={<ResolutionHistory role="staff" />} />
            <Route path="workload" element={<Workload />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
};

export default AppRouter;
