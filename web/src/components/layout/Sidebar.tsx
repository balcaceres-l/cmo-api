import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  ActivityIcon,
  BedIcon,
  AmbulanceIcon,
  UsersIcon,
  CalendarIcon,
  PackageIcon,
  FileTextIcon,
  FlaskIcon,
  BellIcon,
  LogOutIcon,
} from '../Icons';

export const Sidebar: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const navItemClass = ({ isActive }: { isActive: boolean }) => `
    cmo-nav-link ${isActive ? 'active' : ''}
  `;

  return (
    <aside
      style={{
        width: '260px',
        background: 'linear-gradient(180deg, #091a14 0%, #0d221b 100%)',
        color: '#ffffff',
        display: 'flex',
        flexDirection: 'column',
        borderRight: '1px solid #14352a',
        flexShrink: 0,
        height: '100vh',
        position: 'sticky',
        top: 0,
        zIndex: 20,
      }}
    >
      {/* Brand Header */}
      <div
        style={{
          padding: '20px 18px',
          borderBottom: '1px solid #17382d',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
        }}
      >
        <div
          style={{
            background: '#ffffff',
            padding: '4px',
            borderRadius: '10px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.2)',
          }}
        >
          <img
            src="/logo-icon.png"
            alt="CMO Logo"
            style={{ width: '38px', height: '38px', objectFit: 'contain' }}
          />
        </div>
        <div>
          <h1
            style={{
              fontSize: '1.05rem',
              fontWeight: 800,
              margin: 0,
              letterSpacing: '0.02em',
              color: '#ffffff',
            }}
          >
            HOSPITAL CMO
          </h1>
          <p
            style={{
              color: '#5eead4',
              fontSize: '0.68rem',
              fontWeight: 600,
              textTransform: 'uppercase',
              letterSpacing: '0.06em',
              margin: 0,
            }}
          >
            Centro Médico de Occidente
          </p>
        </div>
      </div>

      {/* Navigation Sections */}
      <nav
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: '16px 12px',
          display: 'flex',
          flexDirection: 'column',
          gap: '18px',
        }}
      >
        {/* OPERACIONES */}
        <div>
          <span
            style={{
              display: 'block',
              padding: '0 10px 6px',
              fontSize: '0.7rem',
              fontWeight: 700,
              color: '#4fc4a2',
              textTransform: 'uppercase',
              letterSpacing: '0.07em',
            }}
          >
            Operaciones
          </span>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
            <NavLink to="/dashboard" className={navItemClass}>
              <ActivityIcon size={18} />
              <span>Dashboard</span>
            </NavLink>
            <NavLink to="/camas" className={navItemClass}>
              <BedIcon size={18} />
              <span>Camas y Habitaciones</span>
            </NavLink>
            <NavLink to="/ambulancia" className={navItemClass}>
              <AmbulanceIcon size={18} />
              <span>Ambulancia en Vivo</span>
            </NavLink>
          </div>
        </div>

        {/* PERSONAL */}
        <div>
          <span
            style={{
              display: 'block',
              padding: '0 10px 6px',
              fontSize: '0.7rem',
              fontWeight: 700,
              color: '#4fc4a2',
              textTransform: 'uppercase',
              letterSpacing: '0.07em',
            }}
          >
            Personal
          </span>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
            <NavLink to="/usuarios" className={navItemClass}>
              <UsersIcon size={18} />
              <span>Usuarios y Personal</span>
            </NavLink>
            <NavLink to="/turnos" className={navItemClass}>
              <CalendarIcon size={18} />
              <span>Control de Turnos</span>
            </NavLink>
          </div>
        </div>

        {/* CLÍNICO */}
        <div>
          <span
            style={{
              display: 'block',
              padding: '0 10px 6px',
              fontSize: '0.7rem',
              fontWeight: 700,
              color: '#4fc4a2',
              textTransform: 'uppercase',
              letterSpacing: '0.07em',
            }}
          >
            Clínico
          </span>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
            <NavLink to="/pacientes" className={navItemClass}>
              <FileTextIcon size={18} />
              <span>Pacientes & Expedientes</span>
            </NavLink>
            <NavLink to="/laboratorio" className={navItemClass}>
              <FlaskIcon size={18} />
              <span>Laboratorio</span>
            </NavLink>
          </div>
        </div>

        {/* LOGÍSTICA & AVISOS */}
        <div>
          <span
            style={{
              display: 'block',
              padding: '0 10px 6px',
              fontSize: '0.7rem',
              fontWeight: 700,
              color: '#4fc4a2',
              textTransform: 'uppercase',
              letterSpacing: '0.07em',
            }}
          >
            Logística & Avisos
          </span>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
            <NavLink to="/inventario" className={navItemClass}>
              <PackageIcon size={18} />
              <span>Recursos e Inventario</span>
            </NavLink>
            <NavLink to="/avisos" className={navItemClass}>
              <BellIcon size={18} />
              <span>Avisos del Hospital</span>
            </NavLink>
          </div>
        </div>
      </nav>

      {/* User Footer Card */}
      <div
        style={{
          padding: '14px 16px',
          borderTop: '1px solid #17382d',
          background: 'rgba(5, 18, 14, 0.4)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '10px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', overflow: 'hidden' }}>
          <div
            style={{
              width: '34px',
              height: '34px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #0b5d4b, #158066)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 700,
              fontSize: '0.85rem',
              flexShrink: 0,
            }}
          >
            {user?.nombre_completo ? user.nombre_completo.charAt(0).toUpperCase() : 'A'}
          </div>
          <div style={{ overflow: 'hidden' }}>
            <p
              style={{
                margin: 0,
                fontSize: '0.82rem',
                fontWeight: 600,
                color: '#ffffff',
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
              title={user?.nombre_completo}
            >
              {user?.nombre_completo || 'Administrador'}
            </p>
            <span
              style={{
                fontSize: '0.68rem',
                color: '#5eead4',
                fontWeight: 600,
                textTransform: 'uppercase',
                letterSpacing: '0.04em',
              }}
            >
              {user?.rol || 'ADMIN'}
            </span>
          </div>
        </div>
        <button
          onClick={handleLogout}
          title="Cerrar sesión"
          style={{
            background: 'none',
            border: 'none',
            color: '#f08a8c',
            cursor: 'pointer',
            padding: '6px',
            borderRadius: '6px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'background 0.15s ease',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(240, 138, 140, 0.15)')}
          onMouseLeave={(e) => (e.currentTarget.style.background = 'none')}
        >
          <LogOutIcon size={18} />
        </button>
      </div>
    </aside>
  );
};
