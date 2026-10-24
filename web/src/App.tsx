import { useState } from 'react';
import Login from './pages/auth/Login';
import Dashboard from './pages/operaciones/Dashboard';
import Camas from './pages/operaciones/Camas';
import Ambulancia from './pages/operaciones/Ambulancia';
import Usuarios from './pages/personal/Usuarios';
import Turnos from './pages/personal/Turnos';
import Inventario from './pages/logistica/Inventario';
import Pacientes from './pages/clinico/Pacientes';
import Avisos from './pages/clinico/Avisos';

type Tab =
  | 'dashboard'
  | 'camas'
  | 'ambulancia'
  | 'usuarios'
  | 'turnos'
  | 'inventario'
  | 'pacientes'
  | 'avisos'
  | 'login';

export default function App() {
  const [activeTab, setActiveTab] = useState<Tab>('dashboard');

  if (activeTab === 'login') {
    return (
      <div>
        <div style={{ padding: '8px 16px', background: '#e8efeb', textAlign: 'right' }}>
          <button
            onClick={() => setActiveTab('dashboard')}
            style={{ background: 'none', border: 'none', color: '#0b5d4b', fontWeight: 600, cursor: 'pointer' }}
          >
            ← Volver al Panel
          </button>
        </div>
        <Login />
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: '#f3f6f4' }}>
      {/* Barra lateral / Sidebar */}
      <aside style={{ width: '260px', background: '#13221c', color: '#fff', padding: '24px', display: 'flex', flexDirection: 'column' }}>
        <div style={{ marginBottom: '28px' }}>
          <h1 style={{ fontSize: '1.2rem', margin: '0 0 4px 0', letterSpacing: '-0.01em' }}>CMO Central</h1>
          <p style={{ color: '#a2b7ad', fontSize: '0.82rem', margin: 0 }}>Panel Administrativo</p>
        </div>

        <nav style={{ display: 'flex', flexDirection: 'column', gap: '20px', flex: 1, fontSize: '0.9rem' }}>
          {/* Operaciones */}
          <div>
            <span style={{ color: '#4fc4a2', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Operaciones
            </span>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '8px' }}>
              <button
                onClick={() => setActiveTab('dashboard')}
                style={navBtnStyle(activeTab === 'dashboard')}
              >
                📊 Dashboard
              </button>
              <button
                onClick={() => setActiveTab('camas')}
                style={navBtnStyle(activeTab === 'camas')}
              >
                🛏️ Camas y Habitaciones
              </button>
              <button
                onClick={() => setActiveTab('ambulancia')}
                style={navBtnStyle(activeTab === 'ambulancia')}
              >
                🚑 Ambulancia en Vivo
              </button>
            </div>
          </div>

          {/* Personal */}
          <div>
            <span style={{ color: '#4fc4a2', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Personal
            </span>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '8px' }}>
              <button
                onClick={() => setActiveTab('usuarios')}
                style={navBtnStyle(activeTab === 'usuarios')}
              >
                👥 Usuarios y Personal
              </button>
              <button
                onClick={() => setActiveTab('turnos')}
                style={navBtnStyle(activeTab === 'turnos')}
              >
                📅 Control de Turnos
              </button>
            </div>
          </div>

          {/* Logística */}
          <div>
            <span style={{ color: '#4fc4a2', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Logística
            </span>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '8px' }}>
              <button
                onClick={() => setActiveTab('inventario')}
                style={navBtnStyle(activeTab === 'inventario')}
              >
                📦 Recursos e Inventario
              </button>
            </div>
          </div>

          {/* Clínico */}
          <div>
            <span style={{ color: '#4fc4a2', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Clínico y Avisos
            </span>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '8px' }}>
              <button
                onClick={() => setActiveTab('pacientes')}
                style={navBtnStyle(activeTab === 'pacientes')}
              >
                📁 Pacientes y Expedientes
              </button>
              <button
                onClick={() => setActiveTab('avisos')}
                style={navBtnStyle(activeTab === 'avisos')}
              >
                📢 Avisos del Hospital
              </button>
            </div>
          </div>
        </nav>

        {/* Usuario inferior */}
        <div style={{ borderTop: '1px solid #2b4036', paddingTop: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <p style={{ margin: 0, fontSize: '0.85rem', fontWeight: 600 }}>Admin General</p>
            <p style={{ margin: 0, fontSize: '0.75rem', color: '#a2b7ad' }}>Administrador</p>
          </div>
          <button
            onClick={() => setActiveTab('login')}
            style={{ background: 'none', border: 'none', color: '#f08a8c', cursor: 'pointer', fontSize: '0.8rem' }}
          >
            Salir
          </button>
        </div>
      </aside>

      {/* Contenido Principal */}
      <main style={{ flex: 1, padding: '36px', overflowY: 'auto' }}>
        {activeTab === 'dashboard' && <Dashboard />}
        {activeTab === 'camas' && <Camas />}
        {activeTab === 'ambulancia' && <Ambulancia />}
        {activeTab === 'usuarios' && <Usuarios />}
        {activeTab === 'turnos' && <Turnos />}
        {activeTab === 'inventario' && <Inventario />}
        {activeTab === 'pacientes' && <Pacientes />}
        {activeTab === 'avisos' && <Avisos />}
      </main>
    </div>
  );
}

function navBtnStyle(isActive: boolean): React.CSSProperties {
  return {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    width: '100%',
    padding: '8px 12px',
    borderRadius: '6px',
    border: 'none',
    background: isActive ? '#1d2d26' : 'transparent',
    color: isActive ? '#4fc4a2' : '#a2b7ad',
    fontWeight: isActive ? 600 : 400,
    cursor: 'pointer',
    textAlign: 'left',
    transition: 'all 0.15s ease',
  };
}
