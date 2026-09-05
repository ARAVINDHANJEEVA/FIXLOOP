import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Eye,
  EyeOff,
  LogIn,
  ShieldCheck,
  Wrench,
  CheckCircle2,
  Mail,
  Lock,
  AlertCircle,
  Zap,
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';

const schema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(4, 'Password must be at least 4 characters'),
});

type FormValues = z.infer<typeof schema>;

const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [selectedRole, setSelectedRole] = useState<'admin' | 'staff'>('admin');
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showSignoutAlert, setShowSignoutAlert] = useState(false);

  const { register, handleSubmit, setValue, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      email: 'admin@sentinel.edu',
      password: 'admin123',
    },
  });

  useEffect(() => {
    if (location.state && (location.state as any).justLoggedOut) {
      setShowSignoutAlert(true);
      const timer = setTimeout(() => setShowSignoutAlert(false), 5000);
      return () => clearTimeout(timer);
    }
  }, [location.state]);

  const selectRoleAndFill = (role: 'admin' | 'staff') => {
    setSelectedRole(role);
    setServerError('');
    if (role === 'admin') {
      setValue('email', 'admin@sentinel.edu');
      setValue('password', 'admin123');
    } else {
      setValue('email', 'staff@sentinel.edu');
      setValue('password', 'staff123');
    }
  };

  const onSubmit = async (data: FormValues) => {
    setIsLoading(true);
    setServerError('');
    const result = await login(data.email, data.password);
    setIsLoading(false);
    if (result.success) {
      navigate('/');
    } else {
      setServerError(result.error || 'Invalid credentials. Please verify and retry.');
    }
  };

  return (
    <div className="clean-login-page">
      {/* Moving Blurred Waves Ambient Background */}
      <div className="login-ambient-background" aria-hidden="true">
        <div className="ambient-wave-orb wave-orb-orange" />
        <div className="ambient-wave-orb wave-orb-blue" />
        <div className="ambient-wave-orb wave-orb-amber" />
        <div className="ambient-wave-orb wave-orb-cyan" />
        <svg className="ambient-wave-lines" viewBox="0 0 1440 800" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M-100,280 C240,460 480,140 820,310 C1160,480 1340,220 1600,360" stroke="url(#waveGrad1)" strokeWidth="2.5" strokeOpacity="0.45" />
          <path d="M-100,390 C280,210 560,490 900,330 C1240,170 1380,410 1600,290" stroke="url(#waveGrad2)" strokeWidth="2" strokeOpacity="0.35" />
          <defs>
            <linearGradient id="waveGrad1" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#ff6b35" stopOpacity="0.75" />
              <stop offset="50%" stopColor="#3b82f6" stopOpacity="0.55" />
              <stop offset="100%" stopColor="#f59e0b" stopOpacity="0.65" />
            </linearGradient>
            <linearGradient id="waveGrad2" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#3b82f6" stopOpacity="0.5" />
              <stop offset="50%" stopColor="#ff6b35" stopOpacity="0.65" />
              <stop offset="100%" stopColor="#10b981" stopOpacity="0.45" />
            </linearGradient>
          </defs>
        </svg>
      </div>

      {/* Top Floating Controls (Dedicated Light Theme Only) */}
      <header className="clean-login-top-bar">
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 10 }}>
          <div className="shopall-logo-mark" style={{ width: 26, height: 26 }}>
            <span className="petal petal-1" />
            <span className="petal petal-2" />
            <span className="petal petal-3" />
            <span className="petal petal-4" />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span style={{ fontWeight: 800, fontSize: '0.98rem', letterSpacing: '-0.02em', color: '#0f172a' }}>FixLoop</span>
            <span style={{ fontSize: '0.64rem', fontWeight: 600, color: '#64748b', letterSpacing: '0.04em', textTransform: 'uppercase' }}>Campus Operations</span>
          </div>
        </div>

        {/* Live System Operational Status Badge */}
        <div className="clean-system-status-pill">
          <span className="live-pulse-dot" />
          <span>System Status: Operational</span>
        </div>
      </header>

      {/* Logout Notice Banner */}
      <AnimatePresence>
        {showSignoutAlert && (
          <motion.div
            initial={{ opacity: 0, y: -16, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -16, scale: 0.95 }}
            transition={{ type: 'spring', stiffness: 350, damping: 28 }}
            className="clean-logout-toast"
          >
            <CheckCircle2 size={15} />
            <span>Signed out successfully. Have a great day!</span>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Main Glassmorphic Login Card */}
      <motion.div
        className="clean-login-card"
        initial={{ opacity: 0, y: 24, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ type: 'spring', stiffness: 280, damping: 26 }}
      >
        {/* Left Column: Clean Interactive Form */}
        <div className="clean-login-form-box">
          <div style={{ marginBottom: 20 }}>
            <h1 className="clean-login-title">Welcome back</h1>
            <p className="clean-login-punchline">
              Campus incident resolution made simple and swift.
            </p>
          </div>

          {/* Role Toggle Switch */}
          <div className="clean-role-toggle">
            <button
              type="button"
              className={`clean-role-btn role-admin ${selectedRole === 'admin' ? 'active' : ''}`}
              onClick={() => selectRoleAndFill('admin')}
            >
              <ShieldCheck size={14} />
              <span>Admin Authority</span>
            </button>
            <button
              type="button"
              className={`clean-role-btn role-staff ${selectedRole === 'staff' ? 'active' : ''}`}
              onClick={() => selectRoleAndFill('staff')}
            >
              <Wrench size={14} />
              <span>Staff Dispatch</span>
            </button>
          </div>

          {/* Quick 1-Click Demo Credentials */}
          <div className="clean-demo-capsules">
            <div
              className={`clean-demo-capsule ${selectedRole === 'admin' ? 'active' : ''}`}
              onClick={() => selectRoleAndFill('admin')}
              title="Click to fill admin credentials"
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 2 }}>
                <span style={{ fontSize: '0.68rem', fontWeight: 800, color: '#2563eb' }}>ADMIN DEMO</span>
                {selectedRole === 'admin' && <CheckCircle2 size={12} color="#2563eb" />}
              </div>
              <div style={{ fontFamily: 'JetBrains Mono', fontSize: '0.70rem', color: '#64748b' }}>admin@sentinel.edu</div>
            </div>

            <div
              className={`clean-demo-capsule ${selectedRole === 'staff' ? 'active' : ''}`}
              onClick={() => selectRoleAndFill('staff')}
              title="Click to fill staff credentials"
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 2 }}>
                <span style={{ fontSize: '0.68rem', fontWeight: 800, color: '#ea580c' }}>STAFF DEMO</span>
                {selectedRole === 'staff' && <CheckCircle2 size={12} color="#ea580c" />}
              </div>
              <div style={{ fontFamily: 'JetBrains Mono', fontSize: '0.70rem', color: '#64748b' }}>staff@sentinel.edu</div>
            </div>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit(onSubmit)}>
            <div className="clean-input-group">
              <label className="clean-input-label">Email address</label>
              <div className="clean-input-wrap">
                <Mail size={15} className="clean-input-icon" />
                <input
                  {...register('email')}
                  type="email"
                  className="clean-input"
                  placeholder="name@sentinel.edu"
                  autoComplete="email"
                />
              </div>
              {errors.email && (
                <span style={{ display: 'block', fontSize: '0.72rem', color: '#ef4444', marginTop: 4, fontWeight: 600 }}>
                  {errors.email.message}
                </span>
              )}
            </div>

            <div className="clean-input-group">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                <label className="clean-input-label" style={{ marginBottom: 0 }}>Password</label>
                <span style={{ fontSize: '0.70rem', color: '#ea580c', fontWeight: 600, cursor: 'pointer' }}>
                  Forgot?
                </span>
              </div>
              <div className="clean-input-wrap">
                <Lock size={15} className="clean-input-icon" />
                <input
                  {...register('password')}
                  type={showPassword ? 'text' : 'password'}
                  className="clean-input"
                  placeholder="••••••••"
                  autoComplete="current-password"
                  style={{ paddingRight: 38 }}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((p) => !p)}
                  style={{
                    position: 'absolute',
                    right: 12,
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    color: '#94a3b8',
                    display: 'flex',
                    alignItems: 'center',
                    padding: 0,
                  }}
                  title={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {errors.password && (
                <span style={{ display: 'block', fontSize: '0.72rem', color: '#ef4444', marginTop: 4, fontWeight: 600 }}>
                  {errors.password.message}
                </span>
              )}
            </div>

            {/* Server Error Alert */}
            <AnimatePresence>
              {serverError && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  style={{
                    background: '#fef2f2',
                    border: '1px solid rgba(239, 68, 68, 0.25)',
                    borderRadius: 9,
                    padding: '9px 12px',
                    fontSize: '0.76rem',
                    color: '#b91c1c',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 7,
                    fontWeight: 600,
                    marginBottom: 12,
                  }}
                >
                  <AlertCircle size={14} />
                  <span>{serverError}</span>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Submit Button */}
            <motion.button
              type="submit"
              className="clean-btn-submit"
              disabled={isLoading}
              whileHover={{ scale: 1.01 }}
              whileTap={{ scale: 0.98 }}
              style={{
                background: selectedRole === 'admin'
                  ? 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)'
                  : 'linear-gradient(135deg, #ea580c 0%, #c2410c 100%)',
                boxShadow: selectedRole === 'admin'
                  ? '0 6px 20px rgba(15, 23, 42, 0.28)'
                  : '0 6px 20px rgba(234, 88, 12, 0.32)',
              }}
            >
              {isLoading ? (
                <>
                  <div style={{
                    width: 15,
                    height: 15,
                    border: '2px solid rgba(255,255,255,0.3)',
                    borderTop: '2px solid white',
                    borderRadius: '50%',
                    animation: 'spin 0.75s linear infinite',
                  }} />
                  <span>Signing in…</span>
                </>
              ) : (
                <>
                  <LogIn size={16} />
                  <span>Sign in as {selectedRole === 'admin' ? 'Admin' : 'Staff'}</span>
                </>
              )}
            </motion.button>
          </form>
        </div>

        {/* Right Column: Hero Transparent Doodle in Black Outline & Warm Orange */}
        <div className="clean-login-doodle-box">
          {/* Warm Ambient Radial Aura */}
          <div className="clean-doodle-ambient-glow" />

          {/* Floating Operational Pill Top */}
          <motion.div
            className="clean-doodle-floating-badge badge-top"
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: [0, -6, 0] }}
            transition={{
              opacity: { duration: 0.6, delay: 0.3 },
              y: { repeat: Infinity, duration: 4.5, ease: 'easeInOut' },
            }}
          >
            <span className="live-pulse-dot" />
            <span>Campus Incident System Online</span>
          </motion.div>

          {/* Large Transparent Vector Doodle - Black Outlines & Warm Orange */}
          <motion.div
            className="clean-doodle-wrapper"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1, y: [0, -8, 0] }}
            transition={{
              opacity: { duration: 0.7, delay: 0.2 },
              scale: { duration: 0.7, delay: 0.2 },
              y: { repeat: Infinity, duration: 5.2, ease: 'easeInOut' },
            }}
          >
            <img
              src="/SitReadingDoodle.svg"
              alt="Campus member reading and relaxing"
              className="clean-doodle-svg"
            />
          </motion.div>

          {/* Floating SLA Routing Pill Bottom */}
          <motion.div
            className="clean-doodle-floating-badge badge-bottom"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: [0, 6, 0] }}
            transition={{
              opacity: { duration: 0.6, delay: 0.4 },
              y: { repeat: Infinity, duration: 5, ease: 'easeInOut', delay: 0.4 },
            }}
          >
            <Zap size={13} color="#ea580c" />
            <span>Automated Priority & SLA Routing</span>
          </motion.div>
        </div>
      </motion.div>
    </div>
  );
};

export default Login;
