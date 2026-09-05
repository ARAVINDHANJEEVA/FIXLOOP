import React from 'react';
import { FormField } from '../../types';

interface DynamicFieldRendererProps {
  extraData: Record<string, string | number | boolean>;
  formSchema: FormField[];
}

const DynamicFieldRenderer: React.FC<DynamicFieldRendererProps> = ({
  extraData,
  formSchema,
}) => {
  if (!formSchema.length || !Object.keys(extraData).length) {
    return <span style={{ fontSize: '0.78rem', color: 'var(--text-3)' }}>No additional parameters</span>;
  }

  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
      {formSchema.map((field) => {
        const val = extraData[field.key];
        if (val === undefined || val === null) return null;

        let displayVal: string;
        if (typeof val === 'boolean') {
          displayVal = val ? 'Yes' : 'No';
        } else {
          displayVal = String(val);
        }

        return (
          <div
            key={field.key}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              padding: '5px 11px',
              background: 'var(--bg-elevated)',
              border: '1px solid var(--border)',
              borderRadius: 8,
              fontSize: '0.78rem',
            }}
          >
            <span style={{ color: 'var(--text-3)', fontWeight: 600 }}>{field.label}:</span>
            <span
              style={{
                color: typeof val === 'boolean' ? (val ? 'var(--success)' : 'var(--danger)') : 'var(--text-1)',
                fontWeight: 700,
              }}
            >
              {displayVal}
            </span>
          </div>
        );
      })}
    </div>
  );
};

export default DynamicFieldRenderer;
