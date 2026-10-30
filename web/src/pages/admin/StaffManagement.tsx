import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus,
  Edit2,
  X,
  Check,
  Users,
  Building2,
  Search,
  CheckCircle2,
  XCircle,
} from 'lucide-react';
import { staffApi, departmentApi } from '../../api';
import { Staff, Department } from '../../types';
import ErrorState from '../../components/ui/ErrorState';
import { useToast } from '../../hooks/useToast';

const StaffManagement: React.FC = () => {
  const { addToast } = useToast();
  const [staff, setStaff] = useState<Staff[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [activeTab, setActiveTab] = useState<'staff' | 'departments'>('staff');
  const [isError, setIsError] = useState(false);

  const loadData = useCallback(async () => {
    setIsError(false);
    try {
      const [staffRes, deptsRes] = await Promise.all([
        staffApi.getAll(),
        departmentApi.getAll(),
      ]);
      setStaff(staffRes || []);
      setDepartments(deptsRes || []);
    } catch {
      setIsError(true);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Staff Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('ALL');

  // Department Filters
  const [deptSearchQuery, setDeptSearchQuery] = useState('');

  // Staff Modal
  const [showModal, setShowModal] = useState(false);
  const [editingStaff, setEditingStaff] = useState<Staff | null>(null);
  const [newTag, setNewTag] = useState('');
  const [form, setForm] = useState({
    name: '',
    role: '',
    departmentId: '',
    email: '',
    skillTags: [] as string[],
  });

  // Department Modal
  const [showDeptModal, setShowDeptModal] = useState(false);
  const [editingDept, setEditingDept] = useState<Department | null>(null);
  const [deptForm, setDeptForm] = useState({
    name: '',
    description: '',
    headOfDepartment: '',
  });

  const handleToggleStatus = async (staffId: string) => {
    const member = staff.find((s) => s.id === staffId);
    const newStatus = member?.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await staffApi.update(staffId, { status: newStatus });
      await loadData();
    } catch {
      setStaff((prev) =>
        prev.map((s) =>
          s.id === staffId ? { ...s, status: newStatus } : s
        )
      );
    }
    addToast({
      type: 'success',
      title: 'Status updated',
      message: `${member?.name} is now marked as ${newStatus === 'ACTIVE' ? 'Active' : 'Inactive'}`,
    });
  };

  const openAdd = () => {
    setEditingStaff(null);
    setForm({ name: '', role: '', departmentId: '', email: '', skillTags: [] });
    setShowModal(true);
  };

  const openEdit = (s: Staff) => {
    setEditingStaff(s);
    setForm({
      name: s.name,
      role: s.role,
      departmentId: s.departmentId,
      email: s.email,
      skillTags: [...s.skillTags],
    });
    setShowModal(true);
  };

  const handleSave = async () => {
    if (!form.name || !form.role || !form.departmentId) {
      addToast({
        type: 'error',
        title: 'Validation error',
        message: 'Name, role, and department are required',
      });
      return;
    }
    const dept = departments.find((d) => d.id === form.departmentId);
    try {
      if (editingStaff) {
        await staffApi.update(editingStaff.id, {
          name: form.name,
          jobTitle: form.role,
          departmentId: form.departmentId,
        });
        await loadData();
      } else {
        await staffApi.create({
          name: form.name,
          email: form.email || `${form.name.toLowerCase().replace(/\s+/g, '.')}@fixloop.edu`,
          jobTitle: form.role,
          departmentId: form.departmentId,
        });
        await loadData();
      }
    } catch {
      if (editingStaff) {
        setStaff((prev) =>
          prev.map((s) =>
            s.id === editingStaff.id
              ? { ...s, ...form, departmentName: dept?.name || '' }
              : s
          )
        );
      } else {
        const newStaff: Staff = {
          id: `stf-${Date.now()}`,
          ...form,
          departmentName: dept?.name || '',
          status: 'ACTIVE',
          currentWeightedLoad: 0,
          avatarInitials: form.name
            .split(' ')
            .map((n) => n[0])
            .join('')
            .slice(0, 2)
            .toUpperCase(),
        };
        setStaff((prev) => [...prev, newStaff]);
      }
    }
    addToast({
      type: 'success',
      title: editingStaff ? 'Staff updated' : 'Staff added',
      message: `${form.name}'s profile has been saved`,
    });
    setShowModal(false);
  };

  // Department Handlers
  const openAddDept = () => {
    setEditingDept(null);
    setDeptForm({ name: '', description: '', headOfDepartment: '' });
    setShowDeptModal(true);
  };

  const openEditDept = (dept: Department) => {
    setEditingDept(dept);
    setDeptForm({
      name: dept.name,
      description: dept.description,
      headOfDepartment: dept.headOfDepartment,
    });
    setShowDeptModal(true);
  };

  const handleSaveDept = async () => {
    if (!deptForm.name.trim() || !deptForm.headOfDepartment.trim()) {
      addToast({
        type: 'error',
        title: 'Validation error',
        message: 'Department Name and Head of Department are required',
      });
      return;
    }

    try {
      if (!editingDept) {
        await departmentApi.create({
          name: deptForm.name.trim(),
          description: deptForm.description.trim() || 'General campus operations',
          headOfDept: deptForm.headOfDepartment.trim(),
        });
        await loadData();
      } else {
        setDepartments((prev) =>
          prev.map((d) => (d.id === editingDept.id ? { ...d, ...deptForm } : d))
        );
      }
    } catch {
      if (editingDept) {
        setDepartments((prev) =>
          prev.map((d) => (d.id === editingDept.id ? { ...d, ...deptForm } : d))
        );
      } else {
        const newDept: Department = {
          id: `dept-${Date.now()}`,
          name: deptForm.name.trim(),
          description: deptForm.description.trim() || 'General campus operations',
          headOfDepartment: deptForm.headOfDepartment.trim(),
        };
        setDepartments((prev) => [...prev, newDept]);
      }
    }
    addToast({
      type: 'success',
      title: editingDept ? 'Department updated' : 'Department created',
      message: `Department "${deptForm.name}" saved.`,
    });
    setShowDeptModal(false);
  };

  const addTag = () => {
    if (newTag.trim() && !form.skillTags.includes(newTag.trim())) {
      setForm((f) => ({ ...f, skillTags: [...f.skillTags, newTag.trim()] }));
      setNewTag('');
    }
  };

  const filteredStaff = staff.filter((s) => {
    const matchesSearch =
      s.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.role.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.email.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesDept =
      departmentFilter === 'ALL' || s.departmentId === departmentFilter;
    return matchesSearch && matchesDept;
  });

  const filteredDepartments = departments.filter((d) => {
    return (
      d.name.toLowerCase().includes(deptSearchQuery.toLowerCase()) ||
      d.description.toLowerCase().includes(deptSearchQuery.toLowerCase()) ||
      d.headOfDepartment.toLowerCase().includes(deptSearchQuery.toLowerCase())
    );
  });

  return (
    <div className="admin-queue-page">
      {/* Page Header */}
      <div className="page-header" style={{ marginBottom: 18 }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <div>
            <h1 className="page-title">Staff & Team Management</h1>
            <p className="page-subtitle">
              Manage campus departments, assigned personnel, skill sets, and live workload capacities
            </p>
          </div>
          <div style={{ display: 'flex', gap: 10 }}>
            {activeTab === 'staff' ? (
              <button className="btn btn-primary" onClick={openAdd}>
                <Plus size={15} />
                <span>Add Staff Member</span>
              </button>
            ) : (
              <button className="btn btn-primary" onClick={openAddDept}>
                <Plus size={15} />
                <span>Add Department</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Main Elevated Card Container */}
      <div className="main-management-card">
        {/* Top Folder-Style Tabs */}
        <div className="folder-tabs-bar">
          <div
            className={`folder-tab ${activeTab === 'staff' ? 'active' : ''}`}
            onClick={() => setActiveTab('staff')}
          >
            <Users size={15} />
            <span>Staff Members</span>
            <span className="folder-tab-badge">{staff.length}</span>
          </div>
          <div
            className={`folder-tab ${activeTab === 'departments' ? 'active' : ''}`}
            onClick={() => setActiveTab('departments')}
          >
            <Building2 size={15} />
            <span>Departments</span>
            <span className="folder-tab-badge">{departments.length}</span>
          </div>
        </div>

        {/* Error State */}
        {isError && (
          <div style={{ padding: '32px 24px' }}>
            <ErrorState
              title="Failed to Load Staff & Departments"
              message="Could not load staff directory and department records from the server. Please verify your connection."
              onRetry={loadData}
            />
          </div>
        )}

        {/* Tab 1: Staff Members */}
        {!isError && activeTab === 'staff' && (
          <div>
            {/* Filter Controls Row */}
            <div className="filter-controls-row">
              <div className="filter-input-group">
                <span className="filter-label">Search</span>
                <div className="search-input-wrapper" style={{ width: '260px' }}>
                  <Search size={14} className="search-icon" />
                  <input
                    type="text"
                    placeholder="Search staff by name, role, email..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="filter-text-input"
                  />
                </div>
              </div>

              <div className="filter-input-group">
                <span className="filter-label">Department</span>
                <select
                  className="filter-select-input"
                  value={departmentFilter}
                  onChange={(e) => setDepartmentFilter(e.target.value)}
                >
                  <option value="ALL">All Departments</option>
                  {departments.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ marginLeft: 'auto' }}>
                <span
                  style={{
                    fontSize: '0.74rem',
                    fontWeight: 700,
                    padding: '4px 10px',
                    borderRadius: 999,
                    background: 'var(--bg-elevated)',
                    border: '1px solid var(--border)',
                    color: 'var(--text-2)',
                  }}
                >
                  {filteredStaff.length} members
                </span>
              </div>
            </div>

            {/* Staff Data Table */}
            <div className="management-table-container">
              <table className="management-data-table">
                <thead>
                  <tr>
                    <th style={{ width: '240px' }}>Staff Member</th>
                    <th style={{ width: '180px' }}>Role</th>
                    <th style={{ width: '160px' }}>Department</th>
                    <th style={{ minWidth: '220px' }}>Skill Tags</th>
                    <th style={{ width: '150px' }}>Workload</th>
                    <th style={{ width: '120px' }}>Status</th>
                    <th style={{ width: '80px', textAlign: 'right' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredStaff.map((s) => (
                    <tr key={s.id} className="modern-table-row">
                      {/* Name & Avatar */}
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                          <div
                            style={{
                              width: 32,
                              height: 32,
                              borderRadius: '50%',
                              background: 'linear-gradient(135deg, #2563eb, #38bdf8)',
                              color: '#ffffff',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              fontWeight: 800,
                              fontSize: '0.74rem',
                              flexShrink: 0,
                            }}
                          >
                            {s.avatarInitials}
                          </div>
                          <div>
                            <div style={{ fontWeight: 700, color: 'var(--text-1)', fontSize: '0.84rem' }}>
                              {s.name}
                            </div>
                            <div style={{ fontSize: '0.72rem', color: 'var(--text-3)' }}>
                              {s.email}
                            </div>
                          </div>
                        </div>
                      </td>

                      {/* Role */}
                      <td style={{ fontSize: '0.8rem', color: 'var(--text-2)', fontWeight: 600 }}>
                        {s.role}
                      </td>

                      {/* Department */}
                      <td>
                        <span
                          style={{
                            fontSize: '0.74rem',
                            fontWeight: 700,
                            padding: '3px 8px',
                            borderRadius: 6,
                            background: 'var(--bg-elevated)',
                            color: 'var(--text-1)',
                            border: '1px solid var(--border)',
                          }}
                        >
                          {s.departmentName}
                        </span>
                      </td>

                      {/* Skill Tags */}
                      <td>
                        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                          {s.skillTags.map((tag) => (
                            <span
                              key={tag}
                              style={{
                                fontSize: '0.68rem',
                                fontWeight: 700,
                                padding: '2px 7px',
                                borderRadius: 999,
                                background: 'var(--accent-dim)',
                                color: 'var(--accent)',
                                border: '1px solid rgba(37, 99, 235, 0.2)',
                              }}
                            >
                              {tag}
                            </span>
                          ))}
                        </div>
                      </td>

                      {/* Workload */}
                      <td>
                        <div>
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem', marginBottom: 3 }}>
                            <span style={{ color: 'var(--text-3)' }}>Capacity</span>
                            <span style={{ fontWeight: 800, color: s.currentWeightedLoad > 50 ? 'var(--warning)' : 'var(--text-1)' }}>
                              {s.currentWeightedLoad}%
                            </span>
                          </div>
                          <div
                            style={{
                              height: 5,
                              width: '100%',
                              background: 'var(--bg-elevated)',
                              borderRadius: 999,
                              overflow: 'hidden',
                              border: '1px solid var(--border)',
                            }}
                          >
                            <div
                              style={{
                                height: '100%',
                                width: `${Math.min(100, s.currentWeightedLoad)}%`,
                                background:
                                  s.currentWeightedLoad > 50
                                    ? 'var(--warning)'
                                    : 'var(--accent)',
                                borderRadius: 999,
                              }}
                            />
                          </div>
                        </div>
                      </td>

                      {/* Status Toggle */}
                      <td>
                        <button
                          onClick={() => handleToggleStatus(s.id)}
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 5,
                            padding: '3px 9px',
                            borderRadius: 999,
                            fontSize: '0.72rem',
                            fontWeight: 700,
                            cursor: 'pointer',
                            border: 'none',
                            background: s.status === 'ACTIVE' ? 'rgba(34, 197, 94, 0.12)' : 'rgba(100, 116, 139, 0.12)',
                            color: s.status === 'ACTIVE' ? 'var(--success)' : 'var(--text-3)',
                            transition: 'all var(--transition-fast)',
                          }}
                          title={`Click to mark ${s.status === 'ACTIVE' ? 'Inactive' : 'Active'}`}
                        >
                          {s.status === 'ACTIVE' ? (
                            <>
                              <CheckCircle2 size={12} />
                              Active
                            </>
                          ) : (
                            <>
                              <XCircle size={12} />
                              Inactive
                            </>
                          )}
                        </button>
                      </td>

                      {/* Action */}
                      <td style={{ textAlign: 'right' }}>
                        <button
                          className="btn btn-ghost btn-icon"
                          onClick={() => openEdit(s)}
                          title="Edit staff profile"
                        >
                          <Edit2 size={14} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Tab 2: Departments */}
        {!isError && activeTab === 'departments' && (
          <div>
            {/* Department Filter / Actions Row */}
            <div className="filter-controls-row">
              <div className="filter-input-group">
                <span className="filter-label">Search</span>
                <div className="search-input-wrapper" style={{ width: '260px' }}>
                  <Search size={14} className="search-icon" />
                  <input
                    type="text"
                    placeholder="Search department name, head..."
                    value={deptSearchQuery}
                    onChange={(e) => setDeptSearchQuery(e.target.value)}
                    className="filter-text-input"
                  />
                </div>
              </div>

              <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 10 }}>
                <span
                  style={{
                    fontSize: '0.74rem',
                    fontWeight: 700,
                    padding: '4px 10px',
                    borderRadius: 999,
                    background: 'var(--bg-elevated)',
                    border: '1px solid var(--border)',
                    color: 'var(--text-2)',
                  }}
                >
                  {filteredDepartments.length} departments
                </span>

                <button className="btn btn-primary btn-sm" onClick={openAddDept}>
                  <Plus size={13} />
                  <span>New Department</span>
                </button>
              </div>
            </div>

            <div style={{ padding: '20px 24px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 16 }}>
                {filteredDepartments.map((dept, i) => {
                  const deptStaff = staff.filter((s) => s.departmentId === dept.id);
                  const activeStaff = deptStaff.filter((s) => s.status === 'ACTIVE');

                  return (
                    <motion.div
                      key={dept.id}
                      className="card"
                      initial={{ opacity: 0, y: 12 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: i * 0.05 }}
                      style={{ margin: 0, position: 'relative' }}
                    >
                      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 12 }}>
                        <div>
                          <div style={{ fontWeight: 800, fontSize: '1.05rem', color: 'var(--text-1)' }}>
                            {dept.name}
                          </div>
                          <div style={{ fontSize: '0.78rem', color: 'var(--text-3)', marginTop: 2 }}>
                            {dept.description}
                          </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                          <span
                            style={{
                              fontSize: '0.74rem',
                              fontWeight: 800,
                              background: 'var(--accent-dim)',
                              color: 'var(--accent)',
                              padding: '3px 10px',
                              borderRadius: 999,
                            }}
                          >
                            {deptStaff.length} staff
                          </span>
                          <button
                            className="btn btn-ghost btn-icon"
                            style={{ width: 26, height: 26, padding: 0 }}
                            onClick={() => openEditDept(dept)}
                            title="Edit Department"
                          >
                            <Edit2 size={13} />
                          </button>
                        </div>
                      </div>

                      <div style={{ fontSize: '0.78rem', color: 'var(--text-2)', marginBottom: 14 }}>
                        Head of Dept: <span style={{ fontWeight: 700, color: 'var(--text-1)' }}>{dept.headOfDepartment}</span>
                      </div>

                      <div style={{ borderTop: '1px solid var(--border)', paddingTop: 12 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                          <div style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase' }}>
                            Team Members ({activeStaff.length} active)
                          </div>
                        </div>
                        {deptStaff.length === 0 ? (
                          <div style={{ fontSize: '0.74rem', color: 'var(--text-3)', fontStyle: 'italic' }}>
                            No staff members assigned yet
                          </div>
                        ) : (
                          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                            {deptStaff.map((s) => (
                              <div
                                key={s.id}
                                style={{
                                  display: 'flex',
                                  alignItems: 'center',
                                  gap: 6,
                                  padding: '4px 10px',
                                  background: 'var(--bg-elevated)',
                                  borderRadius: 999,
                                  fontSize: '0.74rem',
                                  border: '1px solid var(--border)',
                                  color: 'var(--text-1)',
                                  fontWeight: 600,
                                }}
                              >
                                <span
                                  style={{
                                    width: 7,
                                    height: 7,
                                    borderRadius: '50%',
                                    background: s.status === 'ACTIVE' ? '#22c55e' : 'var(--text-3)',
                                  }}
                                />
                                {s.name}
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    </motion.div>
                  );
                })}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Staff Modal */}
      <AnimatePresence>
        {showModal && (
          <motion.div
            className="modal-overlay"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setShowModal(false)}
          >
            <motion.div
              className="modal"
              initial={{ scale: 0.94, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.94, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 380, damping: 28 }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="modal-header">
                <span className="modal-title">{editingStaff ? 'Edit Staff Profile' : 'Add Staff Member'}</span>
                <button className="btn btn-ghost btn-icon" onClick={() => setShowModal(false)}>
                  <X size={16} />
                </button>
              </div>
              <div className="modal-body">
                <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                  <div className="form-group">
                    <label className="form-label form-label-required">Full Name</label>
                    <input
                      className="form-input"
                      value={form.name}
                      onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                      placeholder="e.g. Ramesh Kumar"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label form-label-required">Role / Title</label>
                    <input
                      className="form-input"
                      value={form.role}
                      onChange={(e) => setForm((f) => ({ ...f, role: e.target.value }))}
                      placeholder="e.g. Senior Technician"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label form-label-required">Email</label>
                    <input
                      className="form-input"
                      type="email"
                      value={form.email}
                      onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
                      placeholder="name@fixloop.edu"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label form-label-required">Department</label>
                    <select
                      className="form-select"
                      value={form.departmentId}
                      onChange={(e) => setForm((f) => ({ ...f, departmentId: e.target.value }))}
                    >
                      <option value="">Select department...</option>
                      {departments.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Skill Tags</label>
                    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 8 }}>
                      {form.skillTags.map((tag) => (
                        <span
                          key={tag}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 4,
                            padding: '2px 8px',
                            background: 'var(--accent-dim)',
                            color: 'var(--accent)',
                            borderRadius: 999,
                            fontSize: '0.75rem',
                          }}
                        >
                          {tag}
                          <X
                            size={10}
                            style={{ cursor: 'pointer' }}
                            onClick={() =>
                              setForm((f) => ({
                                ...f,
                                skillTags: f.skillTags.filter((t) => t !== tag),
                              }))
                            }
                          />
                        </span>
                      ))}
                    </div>
                    <div style={{ display: 'flex', gap: 6 }}>
                      <input
                        className="form-input"
                        value={newTag}
                        onChange={(e) => setNewTag(e.target.value)}
                        placeholder="Add skill tag (e.g. electrical)..."
                        onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addTag())}
                      />
                      <button className="btn btn-secondary btn-sm" onClick={addTag}>
                        <Plus size={13} />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button className="btn btn-secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button className="btn btn-primary" onClick={handleSave}>
                  <Check size={14} /> {editingStaff ? 'Update Profile' : 'Save Staff'}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Department Modal */}
      <AnimatePresence>
        {showDeptModal && (
          <motion.div
            className="modal-overlay"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setShowDeptModal(false)}
          >
            <motion.div
              className="modal"
              initial={{ scale: 0.94, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.94, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 380, damping: 28 }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="modal-header">
                <span className="modal-title" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Building2 size={18} style={{ color: 'var(--accent)' }} />
                  {editingDept ? 'Edit Department' : 'Add New Department'}
                </span>
                <button className="btn btn-ghost btn-icon" onClick={() => setShowDeptModal(false)}>
                  <X size={16} />
                </button>
              </div>
              <div className="modal-body">
                <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                  <div className="form-group">
                    <label className="form-label form-label-required">Department Name</label>
                    <input
                      className="form-input"
                      value={deptForm.name}
                      onChange={(e) => setDeptForm((f) => ({ ...f, name: e.target.value }))}
                      placeholder="e.g. Campus Safety & Security"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label form-label-required">Head of Department</label>
                    <input
                      className="form-input"
                      value={deptForm.headOfDepartment}
                      onChange={(e) => setDeptForm((f) => ({ ...f, headOfDepartment: e.target.value }))}
                      placeholder="e.g. Dr. Sunil Chawla"
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Operational Scope & Description</label>
                    <textarea
                      className="form-textarea"
                      value={deptForm.description}
                      onChange={(e) => setDeptForm((f) => ({ ...f, description: e.target.value }))}
                      placeholder="Briefly describe what issues this department handles..."
                      style={{ minHeight: 80 }}
                    />
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button className="btn btn-secondary" onClick={() => setShowDeptModal(false)}>
                  Cancel
                </button>
                <button className="btn btn-primary" onClick={handleSaveDept}>
                  <Check size={14} /> {editingDept ? 'Update Department' : 'Create Department'}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default StaffManagement;
