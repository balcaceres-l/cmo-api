import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { apiClient, ApiError } from '../../api/client';

interface UsuarioItem {
  id: number;
  nombre_completo: string;
  usuario: string;
  email: string | null;
  rol: 'ADMINISTRADOR' | 'MEDICO' | 'RECEPCIONISTA' | 'PACIENTE';
  cargo: string | null;
  activo: boolean;
  created_at?: string;
}

export const UsersPage: React.FC = () => {
  const { user: currentUser } = useAuth();
  const [usuarios, setUsuarios] = useState<UsuarioItem[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // Estado para Modal de Creación / Edición
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UsuarioItem | null>(null);
  const [formData, setFormData] = useState({
    nombre_completo: '',
    usuario: '',
    email: '',
    password: '',
    rol: 'MEDICO' as 'ADMINISTRADOR' | 'MEDICO' | 'RECEPCIONISTA',
    cargo: 'Medicina General',
  });
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Cargar usuarios de la API
  const fetchUsuarios = async () => {
    try {
      const res = await apiClient.get<UsuarioItem[]>('/usuarios');
      setUsuarios(Array.isArray(res) ? res : []);
    } catch {
      // Datos de muestra si el backend no está encendido en local
      setUsuarios([
        {
          id: 1,
          nombre_completo: 'Lic. Administrador General',
          usuario: 'admin',
          email: 'admin@hospitalcmo.com',
          rol: 'ADMINISTRADOR',
          cargo: 'Dirección General',
          activo: true,
        },
        {
          id: 2,
          nombre_completo: 'Dra. Carolina Valenzuela',
          usuario: 'carolina.valenzuela',
          email: 'c.valenzuela@hospitalcmo.com',
          rol: 'ADMINISTRADOR',
          cargo: 'Jefe de Operaciones',
          activo: true,
        },
        {
          id: 3,
          nombre_completo: 'Dr. Alejandro Benítez',
          usuario: 'medico.general',
          email: 'a.benitez@hospitalcmo.com',
          rol: 'MEDICO',
          cargo: 'Medicina General',
          activo: true,
        },
        {
          id: 4,
          nombre_completo: 'Dr. Salvador Morales',
          usuario: 'medico.emergencia',
          email: 's.morales@hospitalcmo.com',
          rol: 'MEDICO',
          cargo: 'Emergencias Médicas',
          activo: true,
        },
        {
          id: 5,
          nombre_completo: 'Dra. María Arismendi',
          usuario: 'medico.alergista',
          email: 'm.arismendi@hospitalcmo.com',
          rol: 'MEDICO',
          cargo: 'Alergología e Inmunología',
          activo: true,
        },
        {
          id: 6,
          nombre_completo: 'Sonia Guardado',
          usuario: 'recepcion',
          email: 'recepcion@hospitalcmo.com',
          rol: 'RECEPCIONISTA',
          cargo: 'Recepcionista Principal',
          activo: true,
        },
        {
          id: 7,
          nombre_completo: 'Enf. Patricia Henríquez',
          usuario: 'enfermeria',
          email: 'p.henriquez@hospitalcmo.com',
          rol: 'RECEPCIONISTA',
          cargo: 'Enfermera de Turno',
          activo: true,
        },
      ]);
    }
  };

  useEffect(() => {
    fetchUsuarios();
  }, []);

  const handleOpenCreate = () => {
    setEditingUser(null);
    setFormData({
      nombre_completo: '',
      usuario: '',
      email: '',
      password: '',
      rol: 'MEDICO',
      cargo: 'Medicina General',
    });
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleToggleEstado = async (targetUser: UsuarioItem) => {
    if (currentUser?.id === targetUser.id) {
      alert('Seguridad: No puedes desactivar tu propia cuenta de Administrador.');
      return;
    }

    const actionText = targetUser.activo ? 'desactivar' : 'activar';
    if (!confirm(`¿Estás seguro de que deseas ${actionText} la cuenta de ${targetUser.nombre_completo}?`)) {
      return;
    }

    try {
      await apiClient.patch(`/usuarios/${targetUser.id}/estado`, { activo: !targetUser.activo });
      setUsuarios((prev) =>
        prev.map((u) => (u.id === targetUser.id ? { ...u, activo: !u.activo } : u))
      );
    } catch {
      // Simular cambio local si la API está desconectada
      setUsuarios((prev) =>
        prev.map((u) => (u.id === targetUser.id ? { ...u, activo: !u.activo } : u))
      );
    }
  };

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.nombre_completo || !formData.usuario) {
      setFormError('Por favor completa todos los campos requeridos.');
      return;
    }

    setIsSubmitting(true);
    setFormError(null);

    try {
      const payload = {
        nombre_completo: formData.nombre_completo,
        usuario: formData.usuario,
        email: formData.email || undefined,
        password: formData.password || undefined,
        rol: formData.rol,
        cargo: formData.cargo,
      };

      if (editingUser) {
        await apiClient.put(`/usuarios/${editingUser.id}`, payload);
      } else {
        await apiClient.post('/usuarios', payload);
      }

      setIsModalOpen(false);
      fetchUsuarios();
    } catch (err: unknown) {
      const e = err as ApiError;
      setFormError(e.message || 'No fue posible guardar los cambios.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Filtrado de usuarios
  const filteredUsuarios = usuarios.filter((u) => {
    const matchesSearch =
      u.nombre_completo.toLowerCase().includes(searchTerm.toLowerCase()) ||
      u.usuario.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (u.cargo && u.cargo.toLowerCase().includes(searchTerm.toLowerCase()));

    const matchesRole = roleFilter ? u.rol === roleFilter : true;
    const matchesStatus =
      statusFilter === 'active'
        ? u.activo === true
        : statusFilter === 'inactive'
        ? u.activo === false
        : true;

    return matchesSearch && matchesRole && matchesStatus;
  });

  return (
    <div style={{ display: 'grid', gap: '24px' }}>
      {/* Header bar */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '16px',
        }}
      >
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#073f33', margin: '0 0 4px' }}>
            Gestión de Personal y Cuentas
          </h1>
          <p style={{ margin: 0, color: '#526b60', fontSize: '0.88rem' }}>
            Control de usuarios con roles de Administrador, Médicos y Personal Operativo (HU-30).
          </p>
        </div>

        <button
          onClick={handleOpenCreate}
          style={{
            background: 'linear-gradient(135deg, #0b5d4b 0%, #158066 100%)',
            color: '#ffffff',
            border: 'none',
            borderRadius: '8px',
            padding: '10px 20px',
            fontWeight: 700,
            fontSize: '0.9rem',
            cursor: 'pointer',
            boxShadow: '0 4px 12px rgba(11, 93, 75, 0.25)',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <span>+ Nuevo Colaborador</span>
        </button>
      </div>

      {/* Filtros */}
      <div
        style={{
          background: '#ffffff',
          borderRadius: '12px',
          padding: '18px 24px',
          border: '1px solid #d1dcd6',
          display: 'flex',
          flexWrap: 'wrap',
          gap: '16px',
          alignItems: 'center',
          boxShadow: '0 2px 6px rgba(0,0,0,0.02)',
        }}
      >
        <div style={{ flex: '1 1 260px' }}>
          <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: '#4a5c53', marginBottom: '4px' }}>
            Búsqueda Rápida
          </label>
          <input
            type="text"
            placeholder="Buscar por nombre, usuario o cargo..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{
              width: '100%',
              padding: '9px 12px',
              borderRadius: '8px',
              border: '1px solid #d1dcd6',
              fontSize: '0.88rem',
              outline: 'none',
              background: '#f8faf9',
            }}
          />
        </div>

        <div style={{ flex: '0 1 180px' }}>
          <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: '#4a5c53', marginBottom: '4px' }}>
            Filtrar por Rol
          </label>
          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
            style={{
              width: '100%',
              padding: '9px 12px',
              borderRadius: '8px',
              border: '1px solid #d1dcd6',
              fontSize: '0.88rem',
              background: '#ffffff',
            }}
          >
            <option value="">Todos los roles</option>
            <option value="ADMINISTRADOR">ADMINISTRADOR</option>
            <option value="MEDICO">MEDICO</option>
            <option value="RECEPCIONISTA">RECEPCIONISTA / OPERATIVO</option>
          </select>
        </div>

        <div style={{ flex: '0 1 160px' }}>
          <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: '#4a5c53', marginBottom: '4px' }}>
            Estado
          </label>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            style={{
              width: '100%',
              padding: '9px 12px',
              borderRadius: '8px',
              border: '1px solid #d1dcd6',
              fontSize: '0.88rem',
              background: '#ffffff',
            }}
          >
            <option value="">Cualquier estado</option>
            <option value="active">Solo Activos</option>
            <option value="inactive">Solo Inactivos</option>
          </select>
        </div>
      </div>

      {/* Tabla de Usuarios */}
      <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #d1dcd6', overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.02)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
          <thead>
            <tr style={{ background: '#eaf5f1', borderBottom: '1px solid #d1dcd6', color: '#073f33' }}>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>Colaborador</th>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>Usuario</th>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>Rol del Sistema</th>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>Cargo Específico</th>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>Estado</th>
              <th style={{ padding: '12px 18px', fontWeight: 700, textAlign: 'right' }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsuarios.map((u) => {
              const isSelf = currentUser?.id === u.id;
              return (
                <tr key={u.id} style={{ borderBottom: '1px solid #e8efeb', transition: 'background 0.15s ease' }}>
                  <td style={{ padding: '14px 18px' }}>
                    <div style={{ fontWeight: 700, color: '#13221c' }}>{u.nombre_completo}</div>
                    <div style={{ fontSize: '0.78rem', color: '#526b60' }}>{u.email || 'Sin correo registrado'}</div>
                  </td>
                  <td style={{ padding: '14px 18px', fontFamily: 'monospace', color: '#0b5d4b', fontWeight: 600 }}>
                    @{u.usuario}
                  </td>
                  <td style={{ padding: '14px 18px' }}>
                    <span
                      style={{
                        padding: '3px 10px',
                        borderRadius: '999px',
                        fontSize: '0.75rem',
                        fontWeight: 700,
                        background:
                          u.rol === 'ADMINISTRADOR'
                            ? '#d6ebe3'
                            : u.rol === 'MEDICO'
                            ? '#dbebf4'
                            : '#fae9c8',
                        color:
                          u.rol === 'ADMINISTRADOR'
                            ? '#0b5d4b'
                            : u.rol === 'MEDICO'
                            ? '#0e5c86'
                            : '#85560a',
                      }}
                    >
                      {u.rol}
                    </span>
                  </td>
                  <td style={{ padding: '14px 18px', fontWeight: 600, color: '#33483f' }}>
                    {u.cargo || '—'}
                  </td>
                  <td style={{ padding: '14px 18px' }}>
                    <span
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '6px',
                        fontSize: '0.78rem',
                        fontWeight: 700,
                        color: u.activo ? '#1a7541' : '#a3262a',
                      }}
                    >
                      <span
                        style={{
                          width: '8px',
                          height: '8px',
                          borderRadius: '50%',
                          background: u.activo ? '#1a7541' : '#a3262a',
                        }}
                      />
                      {u.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </td>
                  <td style={{ padding: '14px 18px', textAlign: 'right' }}>
                    <button
                      onClick={() => handleToggleEstado(u)}
                      disabled={isSelf}
                      title={isSelf ? 'No puedes desactivar tu propia cuenta' : ''}
                      style={{
                        background: u.activo ? '#fef2f2' : '#f0fdf4',
                        color: u.activo ? '#991b1b' : '#166534',
                        border: `1px solid ${u.activo ? '#fecaca' : '#bbf7d0'}`,
                        borderRadius: '6px',
                        padding: '6px 12px',
                        fontSize: '0.78rem',
                        fontWeight: 600,
                        cursor: isSelf ? 'not-allowed' : 'pointer',
                        opacity: isSelf ? 0.4 : 1,
                      }}
                    >
                      {u.activo ? 'Desactivar' : 'Activar'}
                    </button>
                  </td>
                </tr>
              );
            })}
            {filteredUsuarios.length === 0 && (
              <tr>
                <td colSpan={6} style={{ padding: '36px', textAlign: 'center', color: '#526b60' }}>
                  No se encontraron colaboradores que coincidan con la búsqueda.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Modal Crear / Editar */}
      {isModalOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 100,
            padding: '16px',
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              width: '100%',
              maxWidth: '520px',
              padding: '28px',
              boxShadow: '0 20px 40px rgba(0,0,0,0.2)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: 0, color: '#073f33' }}>
                Registrar Nuevo Colaborador
              </h2>
              <button
                onClick={() => setIsModalOpen(false)}
                style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer', color: '#849a90' }}
              >
                ✕
              </button>
            </div>

            {formError && (
              <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#991b1b', padding: '10px 14px', borderRadius: '8px', fontSize: '0.84rem', marginBottom: '16px' }}>
                {formError}
              </div>
            )}

            <form onSubmit={handleSubmitForm} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>
                  Nombre Completo *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ej. Dr. Mario Alabi"
                  value={formData.nombre_completo}
                  onChange={(e) => setFormData({ ...formData, nombre_completo: e.target.value })}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>
                    Usuario *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="mario.alabi"
                    value={formData.usuario}
                    onChange={(e) => setFormData({ ...formData, usuario: e.target.value })}
                    style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>
                    Contraseña *
                  </label>
                  <input
                    type="password"
                    required
                    placeholder="Mínimo 8 caracteres"
                    value={formData.password}
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                    style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>
                  Correo Electrónico
                </label>
                <input
                  type="email"
                  placeholder="mario@hospitalcmo.com"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>
                    Rol en el Sistema *
                  </label>
                  <select
                    value={formData.rol}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        rol: e.target.value as 'ADMINISTRADOR' | 'MEDICO' | 'RECEPCIONISTA',
                      })
                    }
                    style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem', background: '#ffffff' }}
                  >
                    <option value="MEDICO">MÉDICO</option>
                    <option value="RECEPCIONISTA">RECEPCIÓN / ENFERMERÍA</option>
                    <option value="ADMINISTRADOR">ADMINISTRADOR</option>
                  </select>
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>
                    Cargo / Especialidad *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="Ej. Jefe de Operaciones, Alergista..."
                    value={formData.cargo}
                    onChange={(e) => setFormData({ ...formData, cargo: e.target.value })}
                    style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem' }}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '14px' }}>
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  style={{ padding: '10px 18px', borderRadius: '8px', border: '1px solid #d1dcd6', background: '#f8faf9', color: '#13221c', fontWeight: 600, cursor: 'pointer' }}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{ padding: '10px 22px', borderRadius: '8px', border: 'none', background: '#0b5d4b', color: '#ffffff', fontWeight: 700, cursor: 'pointer' }}
                >
                  {isSubmitting ? 'Guardando...' : 'Guardar Colaborador'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
