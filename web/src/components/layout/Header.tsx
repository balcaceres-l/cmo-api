import React from 'react';
import { useAuth } from '../../context/AuthContext';

interface HeaderProps {
  title?: string;
  subtitle?: string;
}

export const Header: React.FC<HeaderProps> = ({
  title = 'Panel de Control y Operaciones',
  subtitle = 'Centro Médico de Occidente — Hospital Universitario',
}) => {
  const { user } = useAuth();

  return (
    <header
      style={{
        background: '#ffffff',
        borderBottom: '1px solid #e2ece7',
        padding: '16px 32px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        position: 'sticky',
        top: 0,
        zIndex: 10,
        boxShadow: '0 1px 3px rgba(0,0,0,0.02)',
      }}
    >
      <div>
        <h2
          style={{
            fontSize: '1.25rem',
            fontWeight: 800,
            margin: 0,
            color: '#073f33',
            letterSpacing: '-0.01em',
          }}
        >
          {title}
        </h2>
        <p
          style={{
            margin: '2px 0 0 0',
            fontSize: '0.82rem',
            color: '#526b60',
          }}
        >
          {subtitle}
        </p>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '18px' }}>
        {/* Status Chip */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            background: '#eaf5f1',
            padding: '6px 14px',
            borderRadius: '999px',
            fontSize: '0.78rem',
            fontWeight: 600,
            color: '#0b5d4b',
            border: '1px solid #d1dcd6',
          }}
        >
          <span
            style={{
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              backgroundColor: '#22c55e',
              boxShadow: '0 0 8px rgba(34, 197, 94, 0.6)',
            }}
          />
          <span>API Conectada</span>
        </div>

        {/* User Badge */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            paddingLeft: '16px',
            borderLeft: '1px solid #e2ece7',
          }}
        >
          <div style={{ textAlign: 'right' }}>
            <p style={{ margin: 0, fontSize: '0.85rem', fontWeight: 700, color: '#13221c' }}>
              {user?.nombre_completo || 'Administrador General'}
            </p>
            <p style={{ margin: 0, fontSize: '0.72rem', color: '#849a90' }}>
              {user?.cargo || 'Jefe de Operaciones'}
            </p>
          </div>
          <div
            style={{
              width: '36px',
              height: '36px',
              borderRadius: '10px',
              background: '#0b5d4b',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: '0.9rem',
              boxShadow: '0 2px 6px rgba(11, 93, 75, 0.25)',
            }}
          >
            {user?.nombre_completo ? user.nombre_completo.charAt(0).toUpperCase() : 'A'}
          </div>
        </div>
      </div>
    </header>
  );
};
