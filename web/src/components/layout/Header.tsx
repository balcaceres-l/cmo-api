import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { isJefeOperaciones } from '../../utils/roles';

interface HeaderProps {
  title?: string;
  subtitle?: string;
}

export const Header: React.FC<HeaderProps> = ({
  title,
  subtitle,
}) => {
  const { user } = useAuth();
  const esOperaciones = isJefeOperaciones(user);

  const defaultTitle = esOperaciones
    ? 'Centro de Control y Logística Hospitalaria'
    : 'Panel de Control y Gobernanza';

  const defaultSubtitle = esOperaciones
    ? 'Centro Médico de Occidente — Monitoreo Operativo y Recursos en Tiempo Real'
    : 'Centro Médico de Occidente — Dirección General y Administración Hospitalaria';

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
          {title || defaultTitle}
        </h2>
        <p
          style={{
            margin: '2px 0 0 0',
            fontSize: '0.82rem',
            color: '#526b60',
          }}
        >
          {subtitle || defaultSubtitle}
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
            gap: '12px',
            paddingLeft: '16px',
            borderLeft: '1px solid #e2ece7',
          }}
        >
          <div style={{ textAlign: 'right' }}>
            <p style={{ margin: 0, fontSize: '0.85rem', fontWeight: 700, color: '#13221c' }}>
              {user?.nombre_completo || (esOperaciones ? 'Lic. Carolina Valenzuela' : 'Administrador General')}
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '2px' }}>
              <span
                style={{
                  fontSize: '0.68rem',
                  fontWeight: 800,
                  textTransform: 'uppercase',
                  padding: '2px 8px',
                  borderRadius: '999px',
                  letterSpacing: '0.04em',
                  background: esOperaciones ? '#eff6ff' : '#fef3c7',
                  color: esOperaciones ? '#1d4ed8' : '#92400e',
                  border: `1px solid ${esOperaciones ? '#bfdbfe' : '#fde68a'}`,
                }}
              >
                {esOperaciones ? '⚙️ Jefe de Operaciones' : '👑 Administrador General'}
              </span>
            </div>
          </div>
          <div
            style={{
              width: '38px',
              height: '38px',
              borderRadius: '10px',
              background: esOperaciones
                ? 'linear-gradient(135deg, #1d4ed8 0%, #2563eb 100%)'
                : 'linear-gradient(135deg, #0b5d4b 0%, #158066 100%)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: '0.92rem',
              boxShadow: esOperaciones
                ? '0 2px 6px rgba(29, 78, 216, 0.3)'
                : '0 2px 6px rgba(11, 93, 75, 0.25)',
            }}
          >
            {user?.nombre_completo ? user.nombre_completo.charAt(0).toUpperCase() : esOperaciones ? 'O' : 'A'}
          </div>
        </div>
      </div>
    </header>
  );
};
