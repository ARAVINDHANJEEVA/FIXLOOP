import React, { useState, useRef, useEffect } from 'react';
import { Search, ChevronDown, X } from 'lucide-react';

interface Option {
  value: string;
  label: string;
  sublabel?: string;
}

interface SearchableDropdownProps {
  options: Option[];
  value: string | null;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  clearable?: boolean;
}

const SearchableDropdown: React.FC<SearchableDropdownProps> = ({
  options,
  value,
  onChange,
  placeholder = 'Select...',
  disabled,
  clearable,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState('');
  const ref = useRef<HTMLDivElement>(null);

  const selected = options.find((o) => o.value === value);

  const filtered = options.filter(
    (o) =>
      o.label.toLowerCase().includes(query.toLowerCase()) ||
      (o.sublabel || '').toLowerCase().includes(query.toLowerCase())
  );

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setIsOpen(false);
        setQuery('');
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  return (
    <div ref={ref} style={{ position: 'relative', width: '100%' }}>
      <button
        type="button"
        className="form-input"
        style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          cursor: disabled ? 'not-allowed' : 'pointer',
          textAlign: 'left',
        }}
        onClick={() => !disabled && setIsOpen((p) => !p)}
        disabled={disabled}
      >
        <span style={{ color: selected ? 'var(--text-1)' : 'var(--text-3)' }}>
          {selected ? selected.label : placeholder}
        </span>
        <span style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
          {clearable && value && (
            <span
              onClick={(e) => { e.stopPropagation(); onChange(''); }}
              style={{ display: 'flex', color: 'var(--text-3)', cursor: 'pointer' }}
            >
              <X size={12} />
            </span>
          )}
          <ChevronDown size={14} style={{ color: 'var(--text-3)', transition: 'transform 0.2s', transform: isOpen ? 'rotate(180deg)' : 'none' }} />
        </span>
      </button>

      {isOpen && (
        <div
          style={{
            position: 'absolute', top: 'calc(100% + 4px)', left: 0, right: 0,
            background: 'var(--bg-elevated)', border: '1px solid var(--border)',
            borderRadius: 'var(--radius-md)', boxShadow: 'var(--shadow-lg)',
            zIndex: 200, overflow: 'hidden', animation: 'slide-down 0.15s ease',
          }}
        >
          <div style={{ padding: '8px 10px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 6 }}>
            <Search size={12} style={{ color: 'var(--text-3)', flexShrink: 0 }} />
            <input
              autoFocus
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search..."
              style={{
                background: 'none', border: 'none', outline: 'none',
                color: 'var(--text-1)', fontSize: '0.875rem', width: '100%',
              }}
            />
          </div>
          <div style={{ maxHeight: 220, overflowY: 'auto' }}>
            {filtered.length === 0 ? (
              <div style={{ padding: '16px', textAlign: 'center', color: 'var(--text-3)', fontSize: '0.8rem' }}>
                No results found
              </div>
            ) : (
              filtered.map((opt) => (
                <div
                  key={opt.value}
                  style={{
                    padding: '10px 14px', cursor: 'pointer', fontSize: '0.875rem',
                    color: opt.value === value ? 'var(--accent)' : 'var(--text-1)',
                    background: opt.value === value ? 'var(--accent-dim)' : 'transparent',
                    transition: 'background 0.1s',
                  }}
                  onClick={() => { onChange(opt.value); setIsOpen(false); setQuery(''); }}
                  onMouseEnter={(e) => { if (opt.value !== value) (e.currentTarget as HTMLElement).style.background = 'rgba(255,255,255,0.04)'; }}
                  onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.background = opt.value === value ? 'var(--accent-dim)' : 'transparent'; }}
                >
                  <div style={{ fontWeight: 500 }}>{opt.label}</div>
                  {opt.sublabel && (
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-3)', marginTop: 2 }}>{opt.sublabel}</div>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default SearchableDropdown;
