import React from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import SideNav from './SideNav';
import AppHeader from './AppHeader';
import ToastContainer from './ToastContainer';
import ErrorBoundary from '../ui/ErrorBoundary';

import { useAuth } from '../../hooks/useAuth';

const pageVariants = {
  initial: { opacity: 0, y: 10 },
  animate: {
    opacity: 1,
    y: 0,
    transition: { type: 'spring' as const, stiffness: 300, damping: 30, mass: 0.8 },
  },
  exit: {
    opacity: 0,
    y: -6,
    transition: { duration: 0.15, ease: 'easeIn' as const },
  },
};

const AppShell: React.FC = () => {
  const location = useLocation();
  const { user } = useAuth();

  return (
    <div className={`app-shell role-${user?.role?.toLowerCase() || 'admin'}`}>
      <SideNav />
      <div className="main-content">
        <AppHeader />
        <ErrorBoundary>
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              className="page-content"
              variants={pageVariants}
              initial="initial"
              animate="animate"
              exit="exit"
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </ErrorBoundary>
      </div>
      <ToastContainer />
    </div>
  );
};

export default AppShell;
