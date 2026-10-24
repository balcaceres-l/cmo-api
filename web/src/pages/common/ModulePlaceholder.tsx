import React from 'react';
import { useNavigate } from 'react-router-dom';

interface ModulePlaceholderProps {
  title: string;
  hu: string;
  description: string;
  rules?: string[];
  icon?: string;
}

export const ModulePlaceholder: React.FC<ModulePlaceholderProps> = ({
  title,
  hu,
  description,
  rules = [],
  icon = '📋',
}) => {
  const navigate = useNavigate();

  return (
    <div
      style={{
        maxWidth: '720px',
        margin: '20px auto',
        background: '#ffffff',
        borderRadius: '16px',
        padding: '36px',
        border: '1px solid #e2ece7',
        boxShadow: '0 4px 16px rgba(11, 44, 35, 0.05)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginBottom: '16px' }}>
        <div
          style={{
            width: '48px',
            height: '48px',
            borderRadius: '12px',
            background: '#eaf5f1',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '24px',
          }}
        >
          {icon}
        </div>
        <div>
          <span
            style={{
              fontSize: '0.74rem',
              fontWeight: 700,
              color: '#0b5d4b',
              background: '#d6ebe3',
              padding: '2px 8px',
              borderRadius: '4px',
              textTransform: 'uppercase',
            }}
          >
            {hu}
          </span>
          <h2 style={{ margin: '4px 0 0', fontSize: '1.4rem', fontWeight: 800, color: '#073f33' }}>
            {title}
          </h2>
        </div>
      </div>

      <p style={{ color: '#526b60', fontSize: '0.92rem', lineHeight: 1.6, marginBottom: '20px' }}>
        {description}
      </p>

      {rules.length > 0 && (
        <div
          style={{
            background: '#f8faf9',
            border: '1px solid #e2ece7',
            borderRadius: '10px',
            padding: '16px 20px',
            marginBottom: '24px',
          }}
        >
          <strong style={{ fontSize: '0.82rem', color: '#13221c', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Reglas de negocio aplicables:
          </strong>
          <ul style={{ margin: '8px 0 0', paddingLeft: '18px', color: '#526b60', fontSize: '0.85rem', lineHeight: 1.6 }}>
            {rules.map((rule, idx) => (
              <li key={idx}>{rule}</li>
            ))}
          </ul>
        </div>
      )}

      <div style={{ display: 'flex', gap: '12px' }}>
        <button
          onClick={() => navigate('/dashboard')}
          style={{
            background: '#0b5d4b',
            color: '#ffffff',
            border: 'none',
            borderRadius: '8px',
            padding: '10px 18px',
            fontSize: '0.85rem',
            fontWeight: 700,
            cursor: 'pointer',
          }}
        >
          ← Volver al Dashboard
        </button>
      </div>
    </div>
  );
};
