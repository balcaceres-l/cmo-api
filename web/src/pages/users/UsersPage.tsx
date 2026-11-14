import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { CARGOS_POR_ROL, isJefeOperaciones, getRoleBadgeConfig } from '../../utils/roles';
import { EspecialidadItem } from '../../types/auth';
import { apiClient, ApiError } from '../../api/client';

interface UsuarioItem {
  id: number | string;
  nombre_completo: string;
  usuario: string;
  email: string | null;
  rol: 'ADMINISTRADOR' | 'MEDICO' | 'RECEPCIONISTA' | 'PACIENTE';
  cargo: string | null;
  id_especialidad?: string | null;
  especialidad_nombre?: string | null;
  activo: boolean;
  fecha_ingreso?: string | null;
  fecha_salida?: string | null;
  created_at?: string;
}

type ModalidadContrato = 'INDEFINIDO' | '2M' | '6M' | '12M' | 'PERSONALIZADO';
type UsersTab = 'personal' | 'especialidades';

function addMonthsToDateIso(baseIso: string, months: number): string {
  const [y, m, d] = (baseIso || new Date().toISOString().split('T')[0]).split('-').map(Number);
  const dt = new Date(y, (m || 1) - 1 + months, d || 1);
  const ry = dt.getFullYear();
  const rm = String(dt.getMonth() + 1).padStart(2, '0');
  const rd = String(dt.getDate()).padStart(2, '0');
  return `${ry}-${rm}-${rd}`;
}

function inferModalidad(fechaIngreso?: string | null, fechaSalida?: string | null): ModalidadContrato {
  if (!fechaSalida) return 'INDEFINIDO';
  const start = (fechaIngreso || new Date().toISOString().split('T')[0]).slice(0, 10);
  const end = fechaSalida.slice(0, 10);
  if (addMonthsToDateIso(start, 2) === end) return '2M';
  if (addMonthsToDateIso(start, 6) === end) return '6M';
  if (addMonthsToDateIso(start, 12) === end) return '12M';
  return 'PERSONALIZADO';
}

export const UsersPage: React.FC = () => {
  const { user: currentUser } = useAuth();
  const navigate = useNavigate();
  const isOps = isJefeOperaciones(currentUser);
  const todayIso = new Date().toISOString().split('T')[0];

  const [activeTab, setActiveTab] = useState<UsersTab>('personal');
  const [usuarios, setUsuarios] = useState<UsuarioItem[]>([]);
  const [especialidades, setEspecialidades] = useState<EspecialidadItem[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [pageError, setPageError] = useState<string | null>(null);

  // Estado para Modal de Creación / Edición de Usuario (Exclusivo Administrador General)
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UsuarioItem | null>(null);
  const [modalidadContrato, setModalidadContrato] = useState<ModalidadContrato>('INDEFINIDO');
  const [formData, setFormData] = useState({
    nombre_completo: '',
    usuario: '',
    email: '',
    password: '',
    rol: 'MEDICO' as 'ADMINISTRADOR' | 'MEDICO' | 'RECEPCIONISTA',
    cargo: '',
    id_especialidad: '',
    fecha_ingreso: todayIso,
    fecha_salida: '',
  });
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Estado para Modal CRUD de Especialidades (Exclusivo Administrador General)
  const [isEspModalOpen, setIsEspModalOpen] = useState(false);
  const [editingEsp, setEditingEsp] = useState<EspecialidadItem | null>(null);
  const [espForm, setEspForm] = useState({
    nombre: '',
    descripcion: '',
    activo: true,
  });
  const [espError, setEspError] = useState<string | null>(null);
  const [isSavingEsp, setIsSavingEsp] = useState(false);

  const notify = (msg: string) => {
    setPageError(null);
    setFeedback(msg);
    setTimeout(() => setFeedback(null), 4000);
  };

  const fetchEspecialidades = async () => {
    try {
      const res: any = await apiClient.get('/especialidades');
      const list = Array.isArray(res) ? res : res?.data || [];
      setEspecialidades(list);
    } catch {
      setEspecialidades([]);
    }
  };

  const fetchUsuarios = async () => {
    try {
      const res: any = await apiClient.get('/usuarios?per_page=100');
      const list = Array.isArray(res) ? res : res?.data || [];
      if (list.length > 0) {
        const mapped: UsuarioItem[] = list.map((u: any) => ({
          id: u.id,
          nombre_completo: u.nombre_completo,
          usuario: u.usuario,
          email: u.email,
          rol: u.rol,
          cargo: u.cargo,
          id_especialidad: u.id_especialidad || null,
          especialidad_nombre: u.especialidad_nombre || u.especialidad?.nombre || null,
          activo: u.estado === 'ACTIVO',
          fecha_ingreso: u.fecha_ingreso ? String(u.fecha_ingreso).slice(0, 10) : null,
          fecha_salida: u.fecha_salida ? String(u.fecha_salida).slice(0, 10) : null,
          created_at: u.created_at,
        }));
        setUsuarios(mapped);
        return;
      }
      setUsuarios([]);
    } catch {
      setUsuarios([]);
    }
  };

  useEffect(() => {
    fetchUsuarios();
    fetchEspecialidades();
  }, []);

  const especialidadesActivas = especialidades.filter((e) => e.activo);

  const handleOpenCreate = () => {
    if (isOps) return;
    setEditingUser(null);
    setModalidadContrato('INDEFINIDO');
    setFormData({
      nombre_completo: '',
      usuario: '',
      email: '',
      password: '',
      rol: 'MEDICO',
      cargo: '',
      id_especialidad: especialidadesActivas[0]?.id_especialidad || '',
      fecha_ingreso: todayIso,
      fecha_salida: '',
    });
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (u: UsuarioItem) => {
    if (isOps || u.rol === 'PACIENTE') return;
    setEditingUser(u);
    const mod = inferModalidad(u.fecha_ingreso, u.fecha_salida);
    setModalidadContrato(mod);

    let initialCargo = u.cargo || '';
    if (u.rol === 'ADMINISTRADOR' && !CARGOS_POR_ROL.ADMINISTRADOR.includes(initialCargo)) {
      initialCargo = CARGOS_POR_ROL.ADMINISTRADOR[0];
    } else if (u.rol === 'RECEPCIONISTA' && !CARGOS_POR_ROL.RECEPCIONISTA.includes(initialCargo)) {
      initialCargo = CARGOS_POR_ROL.RECEPCIONISTA[0];
    }

    setFormData({
      nombre_completo: u.nombre_completo,
      usuario: u.usuario,
      email: u.email || '',
      password: '',
      rol: u.rol,
      cargo: u.rol === 'MEDICO' ? '' : initialCargo,
      id_especialidad: u.rol === 'MEDICO' ? u.id_especialidad || especialidadesActivas[0]?.id_especialidad || '' : '',
      fecha_ingreso: u.fecha_ingreso || todayIso,
      fecha_salida: u.fecha_salida || '',
    });
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleRolChange = (nextRol: 'ADMINISTRADOR' | 'MEDICO' | 'RECEPCIONISTA') => {
    if (editingUser) return;
    if (nextRol === 'MEDICO') {
      setFormData((prev) => ({
        ...prev,
        rol: nextRol,
        cargo: '',
        id_especialidad: prev.id_especialidad || especialidadesActivas[0]?.id_especialidad || '',
      }));
    } else {
      setFormData((prev) => ({
        ...prev,
        rol: nextRol,
        id_especialidad: '',
        cargo: CARGOS_POR_ROL[nextRol][0],
      }));
    }
  };

  const handleChangeModalidad = (mod: ModalidadContrato, baseIngreso?: string) => {
    const start = baseIngreso ?? (formData.fecha_ingreso || todayIso);
    setModalidadContrato(mod);
    if (mod === 'INDEFINIDO') {
      setFormData((prev) => ({ ...prev, fecha_ingreso: start, fecha_salida: '' }));
    } else if (mod === '2M') {
      setFormData((prev) => ({ ...prev, fecha_ingreso: start, fecha_salida: addMonthsToDateIso(start, 2) }));
    } else if (mod === '6M') {
      setFormData((prev) => ({ ...prev, fecha_ingreso: start, fecha_salida: addMonthsToDateIso(start, 6) }));
    } else if (mod === '12M') {
      setFormData((prev) => ({ ...prev, fecha_ingreso: start, fecha_salida: addMonthsToDateIso(start, 12) }));
    } else {
      setFormData((prev) => ({
        ...prev,
        fecha_ingreso: start,
        fecha_salida: prev.fecha_salida || addMonthsToDateIso(start, 3),
      }));
    }
  };

  const handleToggleEstado = async (targetUser: UsuarioItem) => {
    if (isOps) return;
    if (currentUser?.id === targetUser.id) {
      alert('Seguridad: No puedes desactivar tu propia cuenta de Administrador.');
      return;
    }

    const actionText = targetUser.activo ? 'desactivar' : 'activar';
    if (!confirm(`¿Estás seguro de que deseas ${actionText} la cuenta de ${targetUser.nombre_completo}?`)) {
      return;
    }

    try {
      await apiClient.patch(`/usuarios/${targetUser.id}/estado`, {
        estado: targetUser.activo ? 'INACTIVO' : 'ACTIVO',
      });
      setUsuarios((prev) =>
        prev.map((u) => (u.id === targetUser.id ? { ...u, activo: !u.activo } : u))
      );
      notify(`Cuenta ${targetUser.activo ? 'desactivada' : 'activada'} correctamente.`);
    } catch (err: unknown) {
      const e = err as ApiError;
      setPageError(e.message || 'No se pudo cambiar el estado del usuario.');
    }
  };

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isOps) return;
    if (!formData.nombre_completo || !formData.usuario) {
      setFormError('Por favor completa todos los campos requeridos.');
      return;
    }
    if (formData.rol === 'MEDICO' && !formData.id_especialidad) {
      setFormError('Debes seleccionar una especialidad médica.');
      return;
    }
    if (formData.rol !== 'MEDICO' && !formData.cargo) {
      setFormError('Debes seleccionar un cargo.');
      return;
    }
    if (formData.fecha_salida && formData.fecha_ingreso && formData.fecha_salida < formData.fecha_ingreso) {
      setFormError('La fecha de salida no puede ser anterior a la fecha de ingreso.');
      return;
    }

    setIsSubmitting(true);
    setFormError(null);

    try {
      const payload: Record<string, unknown> = {
        nombre_completo: formData.nombre_completo,
        usuario: formData.usuario,
        email: formData.email || undefined,
        password: formData.password || undefined,
        cargo: formData.rol === 'MEDICO' ? null : formData.cargo,
        id_especialidad: formData.rol === 'MEDICO' ? formData.id_especialidad : null,
        fecha_ingreso: formData.fecha_ingreso || null,
        fecha_salida: modalidadContrato === 'INDEFINIDO' ? null : formData.fecha_salida || null,
      };

      if (!editingUser) {
        payload.rol = formData.rol;
      }

      if (editingUser) {
        await apiClient.put(`/usuarios/${editingUser.id}`, payload);
        notify('Colaborador actualizado correctamente.');
      } else {
        await apiClient.post('/usuarios', payload);
        notify('Colaborador creado correctamente.');
      }

      setIsModalOpen(false);
      await Promise.all([fetchUsuarios(), fetchEspecialidades()]);
    } catch (err: unknown) {
      const e = err as ApiError;
      setFormError(e.message || 'No fue posible guardar los cambios.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handlers CRUD Especialidades
  const handleOpenCreateEsp = () => {
    if (isOps) return;
    setEditingEsp(null);
    setEspForm({ nombre: '', descripcion: '', activo: true });
    setEspError(null);
    setIsEspModalOpen(true);
  };

  const handleOpenEditEsp = (esp: EspecialidadItem) => {
    if (isOps) return;
    setEditingEsp(esp);
    setEspForm({
      nombre: esp.nombre,
      descripcion: esp.descripcion || '',
      activo: esp.activo,
    });
    setEspError(null);
    setIsEspModalOpen(true);
  };

  const handleSubmitEsp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isOps) return;
    if (!espForm.nombre.trim()) {
      setEspError('El nombre de la especialidad es obligatorio.');
      return;
    }

    setIsSavingEsp(true);
    setEspError(null);
    try {
      if (editingEsp) {
        const payload: Record<string, unknown> = {
          descripcion: espForm.descripcion.trim() || null,
          activo: espForm.activo,
        };
        if (!editingEsp.es_sistema) {
          payload.nombre = espForm.nombre.trim();
        }
        await apiClient.put(`/especialidades/${editingEsp.id_especialidad}`, payload);
        notify('Especialidad actualizada correctamente.');
      } else {
        await apiClient.post('/especialidades', {
          nombre: espForm.nombre.trim(),
          descripcion: espForm.descripcion.trim() || null,
          activo: espForm.activo,
        });
        notify('Especialidad agregada al catálogo.');
      }
      setIsEspModalOpen(false);
      await Promise.all([fetchEspecialidades(), fetchUsuarios()]);
    } catch (err: unknown) {
      const e = err as ApiError;
      setEspError(e.message || 'No se pudo guardar la especialidad.');
    } finally {
      setIsSavingEsp(false);
    }
  };

  const handleToggleEspActivo = async (esp: EspecialidadItem) => {
    if (isOps || esp.es_sistema) return;
    try {
      await apiClient.put(`/especialidades/${esp.id_especialidad}`, {
        activo: !esp.activo,
      });
      notify(`Especialidad ${esp.activo ? 'desactivada' : 'activada'}.`);
      await fetchEspecialidades();
    } catch (err: unknown) {
      const e = err as ApiError;
      setPageError(e.message || 'No se pudo actualizar el estado de la especialidad.');
    }
  };

  const handleDeleteEsp = async (esp: EspecialidadItem) => {
    if (isOps || esp.es_sistema) return;
    if (!confirm(`¿Eliminar la especialidad "${esp.nombre}"? Esta acción no se puede deshacer.`)) {
      return;
    }
    try {
      await apiClient.delete(`/especialidades/${esp.id_especialidad}`);
      notify(`Especialidad "${esp.nombre}" eliminada.`);
      await fetchEspecialidades();
    } catch (err: unknown) {
      const e = err as ApiError;
      setPageError(e.message || 'No se pudo eliminar la especialidad.');
    }
  };

  const getCargoOEspecialidadLabel = (u: UsuarioItem): string => {
    if (u.rol === 'MEDICO') {
      return u.especialidad_nombre || u.cargo || 'Sin especialidad';
    }
    return u.cargo || '—';
  };

  // Filtrado de usuarios
  const filteredUsuarios = usuarios.filter((u) => {
    const labelCargoEsp = getCargoOEspecialidadLabel(u).toLowerCase();
    const matchesSearch =
      u.nombre_completo.toLowerCase().includes(searchTerm.toLowerCase()) ||
      u.usuario.toLowerCase().includes(searchTerm.toLowerCase()) ||
      labelCargoEsp.includes(searchTerm.toLowerCase());

    const matchesRole = roleFilter ? u.rol === roleFilter : true;
    const matchesStatus =
      statusFilter === 'active'
        ? u.activo === true
        : statusFilter === 'inactive'
        ? u.activo === false
        : true;

    return matchesSearch && matchesRole && matchesStatus;
  });

  // Opciones disponibles en el select de especialidad al editar/crear médico
  const especialidadOptions = (() => {
    if (!editingUser || !editingUser.id_especialidad) return especialidadesActivas;
    const current = especialidades.find((e) => e.id_especialidad === editingUser.id_especialidad);
    if (current && !current.activo) {
      return [current, ...especialidadesActivas];
    }
    return especialidadesActivas;
  })();

  return (
    <div style={{ display: 'grid', gap: '20px' }}>
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
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
            <span
              style={{
                fontSize: '0.72rem',
                fontWeight: 800,
                background: isOps ? '#0284c7' : '#0b5d4b',
                color: '#ffffff',
                padding: '2px 8px',
                borderRadius: '6px',
                textTransform: 'uppercase',
                letterSpacing: '0.04em',
              }}
            >
              {isOps ? 'Directorio Operativo' : 'Gestión de Cuentas'}
            </span>
          </div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#073f33', margin: '0 0 4px' }}>
            {isOps ? 'Directorio de Personal Hospitalario' : 'Gestión de Personal y Cuentas'}
          </h1>
          <p style={{ margin: 0, color: '#526b60', fontSize: '0.88rem' }}>
            {isOps
              ? 'Consulta de colaboradores, especialidades, turnos y canales de comunicación operativa.'
              : 'Control de usuarios, cargos institucionales y catálogo de especialidades médicas.'}
          </p>
        </div>

        {!isOps && (
          <button
            onClick={activeTab === 'personal' ? handleOpenCreate : handleOpenCreateEsp}
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
            <span>{activeTab === 'personal' ? '+ Nuevo Colaborador' : '+ Nueva Especialidad'}</span>
          </button>
        )}
      </div>

      {/* Pestañas exclusivas Administrador General */}
      {!isOps && (
        <div
          style={{
            display: 'inline-flex',
            gap: '6px',
            background: '#eaf5f1',
            padding: '5px',
            borderRadius: '10px',
            border: '1px solid #d1dcd6',
            width: 'fit-content',
          }}
        >
          <button
            type="button"
            onClick={() => setActiveTab('personal')}
            style={{
              padding: '8px 16px',
              borderRadius: '7px',
              border: 'none',
              fontSize: '0.84rem',
              fontWeight: 700,
              cursor: 'pointer',
              background: activeTab === 'personal' ? '#0b5d4b' : 'transparent',
              color: activeTab === 'personal' ? '#ffffff' : '#073f33',
            }}
          >
            Personal y Cuentas ({usuarios.length})
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('especialidades')}
            style={{
              padding: '8px 16px',
              borderRadius: '7px',
              border: 'none',
              fontSize: '0.84rem',
              fontWeight: 700,
              cursor: 'pointer',
              background: activeTab === 'especialidades' ? '#0b5d4b' : 'transparent',
              color: activeTab === 'especialidades' ? '#ffffff' : '#073f33',
            }}
          >
            Catálogo de Especialidades ({especialidades.length})
          </button>
        </div>
      )}

      {feedback && (
        <div
          style={{
            padding: '12px 16px',
            background: '#ecfdf5',
            color: '#065f46',
            borderRadius: '10px',
            border: '1px solid #a7f3d0',
            fontSize: '0.86rem',
            fontWeight: 600,
          }}
        >
          ✓ {feedback}
        </div>
      )}

      {pageError && (
        <div
          style={{
            padding: '12px 16px',
            background: '#fef2f2',
            color: '#991b1b',
            borderRadius: '10px',
            border: '1px solid #fecaca',
            fontSize: '0.86rem',
            fontWeight: 600,
          }}
        >
          ⚠️ {pageError}
        </div>
      )}

      {(isOps || activeTab === 'personal') ? (
        <>
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
                placeholder="Buscar por nombre, usuario, cargo o especialidad..."
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
                  <th style={{ padding: '12px 18px', fontWeight: 700 }}>Cargo / Especialidad</th>
                  <th style={{ padding: '12px 18px', fontWeight: 700 }}>Vigencia / Rotación</th>
                  <th style={{ padding: '12px 18px', fontWeight: 700 }}>Estado</th>
                  <th style={{ padding: '12px 18px', fontWeight: 700, textAlign: 'right' }}>
                    {isOps ? 'Gestión de Guardias' : 'Acciones'}
                  </th>
                </tr>
              </thead>
              <tbody>
                {filteredUsuarios.map((u) => {
                  const isSelf = currentUser?.id === u.id;
                  return (
                    <tr key={u.id} style={{ borderBottom: '1px solid #e8efeb', transition: 'background 0.15s ease' }}>
                      <td style={{ padding: '14px 18px' }}>
                        <div style={{ fontWeight: 700, color: '#13221c' }}>{u.nombre_completo}</div>
                        <div style={{ fontSize: '0.78rem', color: '#526b60' }}>
                          {u.email ? (
                            <a href={`mailto:${u.email}`} style={{ color: '#0b5d4b', textDecoration: 'none' }}>
                              ✉️ {u.email}
                            </a>
                          ) : (
                            'Sin correo registrado'
                          )}
                        </div>
                      </td>
                      <td style={{ padding: '14px 18px', fontFamily: 'monospace', color: '#0b5d4b', fontWeight: 600 }}>
                        @{u.usuario}
                      </td>
                      <td style={{ padding: '14px 18px' }}>
                        {(() => {
                          const badge = getRoleBadgeConfig(u.rol);
                          return (
                            <span
                              style={{
                                padding: '3px 10px',
                                borderRadius: '999px',
                                fontSize: '0.75rem',
                                fontWeight: 700,
                                background: badge.bg,
                                color: badge.color,
                                border: badge.border,
                              }}
                            >
                              {u.rol}
                            </span>
                          );
                        })()}
                      </td>
                      <td style={{ padding: '14px 18px', fontWeight: 600, color: '#33483f' }}>
                        {u.rol === 'MEDICO' ? (
                          <span
                            style={{
                              display: 'inline-block',
                              padding: '3px 10px',
                              borderRadius: '8px',
                              background: '#f0f9ff',
                              border: '1px solid #bae6fd',
                              color: '#0369a1',
                              fontSize: '0.8rem',
                              fontWeight: 700,
                            }}
                          >
                            {getCargoOEspecialidadLabel(u)}
                          </span>
                        ) : (
                          getCargoOEspecialidadLabel(u)
                        )}
                      </td>
                      <td style={{ padding: '14px 18px' }}>
                        {u.rol === 'PACIENTE' ? (
                          <span style={{ fontSize: '0.75rem', color: '#849a90' }}>Expediente Clínico</span>
                        ) : !u.fecha_salida ? (
                          <div>
                            <span
                              style={{
                                display: 'inline-block',
                                padding: '2px 8px',
                                borderRadius: '999px',
                                fontSize: '0.72rem',
                                fontWeight: 700,
                                background: '#ecfdf5',
                                color: '#047857',
                                border: '1px solid #a7f3d0',
                              }}
                            >
                              Planta · Indefinido
                            </span>
                            {u.fecha_ingreso && (
                              <div style={{ fontSize: '0.7rem', color: '#64748b', marginTop: '2px' }}>
                                Ingreso: {u.fecha_ingreso}
                              </div>
                            )}
                          </div>
                        ) : (
                          (() => {
                            const isExpired = u.fecha_salida < todayIso;
                            const daysLeft = Math.ceil(
                              (new Date(u.fecha_salida + 'T00:00:00').getTime() -
                                new Date(todayIso + 'T00:00:00').getTime()) /
                                (1000 * 60 * 60 * 24)
                            );
                            const isSoon = !isExpired && daysLeft <= 14;
                            return (
                              <div>
                                <span
                                  style={{
                                    display: 'inline-block',
                                    padding: '2px 8px',
                                    borderRadius: '999px',
                                    fontSize: '0.72rem',
                                    fontWeight: 700,
                                    background: isExpired ? '#fef2f2' : isSoon ? '#fffbeb' : '#eff6ff',
                                    color: isExpired ? '#b91c1c' : isSoon ? '#b45309' : '#1d4ed8',
                                    border: `1px solid ${isExpired ? '#fecaca' : isSoon ? '#fde68a' : '#bfdbfe'}`,
                                  }}
                                >
                                  {isExpired
                                    ? `Finalizado (${u.fecha_salida})`
                                    : isSoon
                                    ? `⏳ Vence ${u.fecha_salida} (${daysLeft}d)`
                                    : `Hasta ${u.fecha_salida}`}
                                </span>
                                {u.fecha_ingreso && (
                                  <div style={{ fontSize: '0.7rem', color: '#64748b', marginTop: '2px' }}>
                                    Desde: {u.fecha_ingreso}
                                  </div>
                                )}
                              </div>
                            );
                          })()
                        )}
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
                        {isOps ? (
                          <button
                            onClick={() => navigate('/turnos')}
                            style={{
                              background: '#f0fdf4',
                              color: '#166534',
                              border: '1px solid #bbf7d0',
                              borderRadius: '6px',
                              padding: '6px 12px',
                              fontSize: '0.78rem',
                              fontWeight: 700,
                              cursor: 'pointer',
                            }}
                          >
                            Ver Turnos →
                          </button>
                        ) : (
                          <div style={{ display: 'inline-flex', gap: '6px', alignItems: 'center' }}>
                            {u.rol !== 'PACIENTE' && (
                              <button
                                onClick={() => handleOpenEdit(u)}
                                style={{
                                  background: '#f8fafc',
                                  color: '#0f172a',
                                  border: '1px solid #cbd5e1',
                                  borderRadius: '6px',
                                  padding: '6px 10px',
                                  fontSize: '0.76rem',
                                  fontWeight: 600,
                                  cursor: 'pointer',
                                }}
                              >
                                Editar / Rotación
                              </button>
                            )}
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
                          </div>
                        )}
                      </td>
                    </tr>
                  );
                })}
                {filteredUsuarios.length === 0 && (
                  <tr>
                    <td colSpan={7} style={{ padding: '36px', textAlign: 'center', color: '#526b60' }}>
                      No se encontraron colaboradores que coincidan con la búsqueda.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      ) : (
        /* Pestaña Catálogo de Especialidades Médicas */
        <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #d1dcd6', overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.02)' }}>
          <div style={{ padding: '16px 20px', borderBottom: '1px solid #e2ece7', background: '#f8faf9' }}>
            <div style={{ fontWeight: 800, color: '#073f33', fontSize: '0.98rem' }}>
              Catálogo de Especialidades y Subespecialidades Médicas
            </div>
            <div style={{ fontSize: '0.8rem', color: '#526b60', marginTop: '2px' }}>
              Las especialidades base (<strong>Emergencia</strong> y <strong>Medicina general</strong>) están protegidas por el sistema para garantizar guardias 24/7 y consulta primaria.
            </div>
          </div>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
            <thead>
              <tr style={{ background: '#eaf5f1', borderBottom: '1px solid #d1dcd6', color: '#073f33' }}>
                <th style={{ padding: '12px 18px', fontWeight: 700 }}>Especialidad</th>
                <th style={{ padding: '12px 18px', fontWeight: 700 }}>Descripción</th>
                <th style={{ padding: '12px 18px', fontWeight: 700 }}>Tipo</th>
                <th style={{ padding: '12px 18px', fontWeight: 700 }}>Médicos</th>
                <th style={{ padding: '12px 18px', fontWeight: 700 }}>Estado</th>
                <th style={{ padding: '12px 18px', fontWeight: 700, textAlign: 'right' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {especialidades.map((esp) => {
                const count = esp.medicos_count ?? 0;
                const deleteDisabled = esp.es_sistema || count > 0;
                return (
                  <tr key={esp.id_especialidad} style={{ borderBottom: '1px solid #e8efeb' }}>
                    <td style={{ padding: '14px 18px', fontWeight: 700, color: '#13221c' }}>
                      {esp.nombre}
                    </td>
                    <td style={{ padding: '14px 18px', color: '#526b60', fontSize: '0.84rem' }}>
                      {esp.descripcion || '—'}
                    </td>
                    <td style={{ padding: '14px 18px' }}>
                      {esp.es_sistema ? (
                        <span
                          style={{
                            padding: '3px 10px',
                            borderRadius: '999px',
                            fontSize: '0.73rem',
                            fontWeight: 700,
                            background: '#fef3c7',
                            color: '#92400e',
                            border: '1px solid #fde68a',
                          }}
                        >
                          🔒 Base del Sistema
                        </span>
                      ) : (
                        <span
                          style={{
                            padding: '3px 10px',
                            borderRadius: '999px',
                            fontSize: '0.73rem',
                            fontWeight: 700,
                            background: '#e0f2fe',
                            color: '#0369a1',
                            border: '1px solid #bae6fd',
                          }}
                        >
                          Configurable
                        </span>
                      )}
                    </td>
                    <td style={{ padding: '14px 18px', fontWeight: 700, color: '#073f33' }}>
                      {count}
                    </td>
                    <td style={{ padding: '14px 18px' }}>
                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '6px',
                          fontSize: '0.78rem',
                          fontWeight: 700,
                          color: esp.activo ? '#1a7541' : '#a3262a',
                        }}
                      >
                        <span
                          style={{
                            width: '8px',
                            height: '8px',
                            borderRadius: '50%',
                            background: esp.activo ? '#1a7541' : '#a3262a',
                          }}
                        />
                        {esp.activo ? 'Activa' : 'Inactiva'}
                      </span>
                    </td>
                    <td style={{ padding: '14px 18px', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '6px', alignItems: 'center' }}>
                        <button
                          onClick={() => handleOpenEditEsp(esp)}
                          style={{
                            background: '#f8fafc',
                            color: '#0f172a',
                            border: '1px solid #cbd5e1',
                            borderRadius: '6px',
                            padding: '6px 10px',
                            fontSize: '0.76rem',
                            fontWeight: 600,
                            cursor: 'pointer',
                          }}
                        >
                          Editar
                        </button>
                        <button
                          onClick={() => handleToggleEspActivo(esp)}
                          disabled={esp.es_sistema}
                          title={esp.es_sistema ? 'Las especialidades base no se pueden desactivar' : ''}
                          style={{
                            background: esp.activo ? '#fffbeb' : '#f0fdf4',
                            color: esp.activo ? '#b45309' : '#166534',
                            border: `1px solid ${esp.activo ? '#fde68a' : '#bbf7d0'}`,
                            borderRadius: '6px',
                            padding: '6px 10px',
                            fontSize: '0.76rem',
                            fontWeight: 600,
                            cursor: esp.es_sistema ? 'not-allowed' : 'pointer',
                            opacity: esp.es_sistema ? 0.45 : 1,
                          }}
                        >
                          {esp.activo ? 'Desactivar' : 'Activar'}
                        </button>
                        <button
                          onClick={() => handleDeleteEsp(esp)}
                          disabled={deleteDisabled}
                          title={
                            esp.es_sistema
                              ? 'Especialidad base protegida'
                              : count > 0
                              ? 'Tiene médicos asignados'
                              : 'Eliminar especialidad'
                          }
                          style={{
                            background: '#fef2f2',
                            color: '#991b1b',
                            border: '1px solid #fecaca',
                            borderRadius: '6px',
                            padding: '6px 10px',
                            fontSize: '0.76rem',
                            fontWeight: 600,
                            cursor: deleteDisabled ? 'not-allowed' : 'pointer',
                            opacity: deleteDisabled ? 0.45 : 1,
                          }}
                        >
                          Eliminar
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
              {especialidades.length === 0 && (
                <tr>
                  <td colSpan={6} style={{ padding: '36px', textAlign: 'center', color: '#526b60' }}>
                    No hay especialidades registradas.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal Crear / Editar Colaborador (Solo visible para Admin General) */}
      {!isOps && isModalOpen && (
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
              maxWidth: '560px',
              padding: '28px',
              boxShadow: '0 20px 40px rgba(0,0,0,0.2)',
              maxHeight: '92vh',
              overflowY: 'auto',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: 0, color: '#073f33' }}>
                {editingUser ? 'Editar Colaborador / Vigencia' : 'Registrar Nuevo Colaborador'}
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
                    {editingUser ? 'Nueva Contraseña (opcional)' : 'Contraseña *'}
                  </label>
                  <input
                    type="password"
                    required={!editingUser}
                    placeholder={editingUser ? 'Dejar vacío para conservar' : 'Mínimo 8 caracteres'}
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
                    disabled={Boolean(editingUser)}
                    onChange={(e) => handleRolChange(e.target.value as 'ADMINISTRADOR' | 'MEDICO' | 'RECEPCIONISTA')}
                    style={{
                      width: '100%',
                      padding: '9px 12px',
                      borderRadius: '8px',
                      border: '1px solid #d1dcd6',
                      fontSize: '0.9rem',
                      background: editingUser ? '#f1f5f9' : '#ffffff',
                      cursor: editingUser ? 'not-allowed' : 'pointer',
                    }}
                  >
                    <option value="MEDICO">MÉDICO</option>
                    <option value="RECEPCIONISTA">RECEPCIÓN / ENFERMERÍA</option>
                    <option value="ADMINISTRADOR">ADMINISTRADOR</option>
                  </select>
                  {editingUser && (
                    <span style={{ display: 'block', fontSize: '0.7rem', color: '#64748b', marginTop: '3px' }}>
                      El rol no se modifica tras crear la cuenta.
                    </span>
                  )}
                </div>

                {formData.rol === 'MEDICO' ? (
                  <div>
                    <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>
                      Especialidad Médica *
                    </label>
                    <select
                      required
                      value={formData.id_especialidad}
                      onChange={(e) => setFormData({ ...formData, id_especialidad: e.target.value })}
                      style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem', background: '#ffffff' }}
                    >
                      <option value="">Selecciona especialidad...</option>
                      {especialidadOptions.map((esp) => (
                        <option key={esp.id_especialidad} value={esp.id_especialidad}>
                          {esp.nombre}{!esp.activo ? ' (Inactiva)' : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                ) : (
                  <div>
                    <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>
                      {formData.rol === 'ADMINISTRADOR' ? 'Cargo Directivo *' : 'Cargo Operativo *'}
                    </label>
                    <select
                      required
                      value={formData.cargo}
                      onChange={(e) => setFormData({ ...formData, cargo: e.target.value })}
                      style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem', background: '#ffffff' }}
                    >
                      {CARGOS_POR_ROL[formData.rol].map((cargoOpt) => (
                        <option key={cargoOpt} value={cargoOpt}>
                          {cargoOpt}
                        </option>
                      ))}
                    </select>
                  </div>
                )}
              </div>

              {/* Sección de Período de Contratación / Rotación Universitaria */}
              <div
                style={{
                  background: '#f8faf9',
                  border: '1px solid #d1dcd6',
                  borderRadius: '10px',
                  padding: '12px 14px',
                }}
              >
                <div style={{ fontSize: '0.8rem', fontWeight: 800, color: '#073f33', marginBottom: '6px' }}>
                  Período de Contratación / Rotación (Hospital Universitario)
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '10px' }}>
                  {(
                    [
                      { id: 'INDEFINIDO', label: 'Indefinido (Planta)' },
                      { id: '2M', label: '2 Meses' },
                      { id: '6M', label: '6 Meses' },
                      { id: '12M', label: '12 Meses' },
                      { id: 'PERSONALIZADO', label: 'Fechas Personalizadas' },
                    ] as { id: ModalidadContrato; label: string }[]
                  ).map((opt) => {
                    const active = modalidadContrato === opt.id;
                    return (
                      <button
                        key={opt.id}
                        type="button"
                        onClick={() => handleChangeModalidad(opt.id)}
                        style={{
                          padding: '5px 10px',
                          borderRadius: '999px',
                          fontSize: '0.74rem',
                          fontWeight: 700,
                          border: active ? '1px solid #0b5d4b' : '1px solid #cbd5e1',
                          background: active ? '#0b5d4b' : '#ffffff',
                          color: active ? '#ffffff' : '#334155',
                          cursor: 'pointer',
                        }}
                      >
                        {opt.label}
                      </button>
                    );
                  })}
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#33483f', marginBottom: '4px' }}>
                      Fecha de Ingreso
                    </label>
                    <input
                      type="date"
                      value={formData.fecha_ingreso}
                      onChange={(e) => {
                        const nextIngreso = e.target.value;
                        if (modalidadContrato === '2M') {
                          setFormData({ ...formData, fecha_ingreso: nextIngreso, fecha_salida: addMonthsToDateIso(nextIngreso, 2) });
                        } else if (modalidadContrato === '6M') {
                          setFormData({ ...formData, fecha_ingreso: nextIngreso, fecha_salida: addMonthsToDateIso(nextIngreso, 6) });
                        } else if (modalidadContrato === '12M') {
                          setFormData({ ...formData, fecha_ingreso: nextIngreso, fecha_salida: addMonthsToDateIso(nextIngreso, 12) });
                        } else {
                          setFormData({ ...formData, fecha_ingreso: nextIngreso });
                        }
                      }}
                      style={{ width: '100%', padding: '7px 10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '0.84rem', background: '#ffffff' }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#33483f', marginBottom: '4px' }}>
                      Fecha de Salida (Baja Automática)
                    </label>
                    {modalidadContrato === 'INDEFINIDO' ? (
                      <div
                        style={{
                          padding: '8px 10px',
                          borderRadius: '8px',
                          border: '1px dashed #94a3b8',
                          background: '#f1f5f9',
                          color: '#475569',
                          fontSize: '0.8rem',
                          fontWeight: 600,
                        }}
                      >
                        Sin fecha límite (Planta)
                      </div>
                    ) : (
                      <input
                        type="date"
                        min={formData.fecha_ingreso || undefined}
                        value={formData.fecha_salida}
                        onChange={(e) => {
                          setModalidadContrato('PERSONALIZADO');
                          setFormData({ ...formData, fecha_salida: e.target.value });
                        }}
                        required
                        style={{ width: '100%', padding: '7px 10px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '0.84rem', background: '#ffffff' }}
                      />
                    )}
                  </div>
                </div>
                <div style={{ fontSize: '0.72rem', color: '#526b60', marginTop: '6px' }}>
                  {modalidadContrato === 'INDEFINIDO'
                    ? 'El colaborador permanecerá elegible para turnos y citas de forma continua.'
                    : `El sistema bloqueará turnos, citas y acceso automáticamente después del ${formData.fecha_salida || 'día de salida'}.`}
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

      {/* Modal Crear / Editar Especialidad (Solo Admin General) */}
      {!isOps && isEspModalOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 105,
            padding: '16px',
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              width: '100%',
              maxWidth: '460px',
              padding: '26px',
              boxShadow: '0 20px 40px rgba(0,0,0,0.2)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h2 style={{ fontSize: '1.2rem', fontWeight: 800, margin: 0, color: '#073f33' }}>
                {editingEsp ? 'Editar Especialidad Médica' : 'Nueva Especialidad Médica'}
              </h2>
              <button
                onClick={() => setIsEspModalOpen(false)}
                style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer', color: '#849a90' }}
              >
                ✕
              </button>
            </div>

            {espError && (
              <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#991b1b', padding: '10px 14px', borderRadius: '8px', fontSize: '0.84rem', marginBottom: '14px' }}>
                {espError}
              </div>
            )}

            <form onSubmit={handleSubmitEsp} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>
                  Nombre de la Especialidad / Subespecialidad *
                </label>
                <input
                  type="text"
                  required
                  disabled={Boolean(editingEsp?.es_sistema)}
                  placeholder="Ej. Neurocirugía, Cirugía Pediátrica..."
                  value={espForm.nombre}
                  onChange={(e) => setEspForm({ ...espForm, nombre: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '9px 12px',
                    borderRadius: '8px',
                    border: '1px solid #d1dcd6',
                    fontSize: '0.9rem',
                    background: editingEsp?.es_sistema ? '#f1f5f9' : '#ffffff',
                  }}
                />
                {editingEsp?.es_sistema && (
                  <span style={{ display: 'block', fontSize: '0.72rem', color: '#92400e', marginTop: '4px' }}>
                    Las especialidades base del sistema no pueden cambiar de nombre.
                  </span>
                )}
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>
                  Descripción
                </label>
                <textarea
                  rows={3}
                  placeholder="Alcance clínico de la especialidad..."
                  value={espForm.descripcion}
                  onChange={(e) => setEspForm({ ...espForm, descripcion: e.target.value })}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.88rem', fontFamily: 'inherit' }}
                />
              </div>

              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.84rem', fontWeight: 600, color: '#13221c', cursor: editingEsp?.es_sistema ? 'not-allowed' : 'pointer' }}>
                <input
                  type="checkbox"
                  checked={espForm.activo}
                  disabled={Boolean(editingEsp?.es_sistema)}
                  onChange={(e) => setEspForm({ ...espForm, activo: e.target.checked })}
                />
                Especialidad activa en el catálogo
              </label>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '8px' }}>
                <button
                  type="button"
                  onClick={() => setIsEspModalOpen(false)}
                  style={{ padding: '9px 16px', borderRadius: '8px', border: '1px solid #d1dcd6', background: '#f8faf9', color: '#13221c', fontWeight: 600, cursor: 'pointer' }}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSavingEsp}
                  style={{ padding: '9px 20px', borderRadius: '8px', border: 'none', background: '#0b5d4b', color: '#ffffff', fontWeight: 700, cursor: 'pointer' }}
                >
                  {isSavingEsp ? 'Guardando...' : 'Guardar Especialidad'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
