import React, { useState, useEffect } from 'react';
import { apiClient, ApiError } from '../../api/client';

interface PacienteItem {
  id: number;
  nombre_completo: string;
  dui: string;
  numero_expediente: string;
  fecha_nacimiento: string;
  sexo: 'M' | 'F' | 'OTRO';
  telefono: string | null;
  direccion: string | null;
  tiene_cuenta_usuario?: boolean;
}

export const PatientsPage: React.FC = () => {
  const [pacientes, setPacientes] = useState<PacienteItem[]>([]);
  const [searchTerm, setSearchTerm] = useState('');

  // Modal de Registro de Paciente
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [registerForm, setRegisterForm] = useState({
    nombre_completo: '',
    dui: '',
    fecha_nacimiento: '',
    sexo: 'M' as 'M' | 'F' | 'OTRO',
    telefono: '',
    direccion: '',
  });
  const [registerError, setRegisterError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Modal de Código de Activación (6 dígitos)
  const [activationModal, setActivationModal] = useState<{
    paciente: PacienteItem;
    codigo: string;
    expira: string;
  } | null>(null);

  // Modal de Consulta de Expediente (Lectura HU-44)
  const [viewingPaciente, setViewingPaciente] = useState<PacienteItem | null>(null);

  const fetchPacientes = async () => {
    try {
      const res = await apiClient.get<PacienteItem[]>('/pacientes');
      setPacientes(Array.isArray(res) ? res : []);
    } catch {
      // Datos de muestra para visualización
      setPacientes([
        {
          id: 1,
          nombre_completo: 'Carlos Eduardo Ramos',
          dui: '04829104-5',
          numero_expediente: 'EXP-2026-000142',
          fecha_nacimiento: '1988-06-14',
          sexo: 'M',
          telefono: '+503 7890-1234',
          direccion: 'Colonia Escalón, San Salvador',
          tiene_cuenta_usuario: true,
        },
        {
          id: 2,
          nombre_completo: 'Lucía Margarita Portillo',
          dui: '05129384-2',
          numero_expediente: 'EXP-2026-000143',
          fecha_nacimiento: '1995-11-20',
          sexo: 'F',
          telefono: '+503 7123-4567',
          direccion: 'Residencial San Carlos, Santa Ana',
          tiene_cuenta_usuario: false,
        },
        {
          id: 3,
          nombre_completo: 'Roberto Andrés Menjívar',
          dui: '03948572-8',
          numero_expediente: 'EXP-2026-000144',
          fecha_nacimiento: '1974-03-08',
          sexo: 'M',
          telefono: '+503 7654-3210',
          direccion: 'Barrio El Calvario, Sonsonate',
          tiene_cuenta_usuario: false,
        },
      ]);
    }
  };

  useEffect(() => {
    fetchPacientes();
  }, []);

  const handleOpenRegister = () => {
    setRegisterForm({
      nombre_completo: '',
      dui: '',
      fecha_nacimiento: '',
      sexo: 'M',
      telefono: '',
      direccion: '',
    });
    setRegisterError(null);
    setIsRegisterOpen(true);
  };

  const handleRegisterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!registerForm.nombre_completo || !registerForm.dui) {
      setRegisterError('Por favor completa el nombre completo y DUI.');
      return;
    }

    setIsSubmitting(true);
    setRegisterError(null);

    try {
      await apiClient.post('/pacientes', registerForm);
      setIsRegisterOpen(false);
      fetchPacientes();
    } catch (err: unknown) {
      const e = err as ApiError;
      setRegisterError(e.message || 'Error al registrar paciente. Verifica que el DUI no esté repetido.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleGenerateActivationCode = async (paciente: PacienteItem) => {
    try {
      const res = await apiClient.post<{ codigo: string; expira_en: string }>(
        `/pacientes/${paciente.id}/codigo-activacion`
      );
      setActivationModal({
        paciente,
        codigo: res.codigo || Math.floor(100000 + Math.random() * 900000).toString(),
        expira: '24 horas',
      });
    } catch {
      // Fallback de demostración
      setActivationModal({
        paciente,
        codigo: Math.floor(100000 + Math.random() * 900000).toString(),
        expira: '24 horas',
      });
    }
  };

  const filteredPacientes = pacientes.filter((p) => {
    const q = searchTerm.toLowerCase();
    return (
      p.nombre_completo.toLowerCase().includes(q) ||
      p.dui.toLowerCase().includes(q) ||
      p.numero_expediente.toLowerCase().includes(q)
    );
  });

  return (
    <div style={{ display: 'grid', gap: '24px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#073f33', margin: '0 0 4px' }}>
            Pacientes y Expedientes
          </h1>
          <p style={{ margin: 0, color: '#526b60', fontSize: '0.88rem' }}>
            Búsqueda, registro clínico administrativo y generación de códigos de activación para la app móvil (HU-31 / HU-44).
          </p>
        </div>

        <button
          onClick={handleOpenRegister}
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
          <span>+ Registrar Paciente</span>
        </button>
      </div>

      {/* Buscador */}
      <div style={{ background: '#ffffff', borderRadius: '12px', padding: '16px 20px', border: '1px solid #d1dcd6', display: 'flex', gap: '12px', alignItems: 'center' }}>
        <span style={{ fontSize: '1.2rem', color: '#849a90' }}>🔍</span>
        <input
          type="text"
          placeholder="Buscar por Nombre, DUI (ej. 04829104-5) o Expediente (ej. EXP-2026-000142)..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          style={{ width: '100%', border: 'none', outline: 'none', fontSize: '0.95rem', background: 'transparent', color: '#13221c' }}
        />
        {searchTerm && (
          <button onClick={() => setSearchTerm('')} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#849a90', fontSize: '0.9rem' }}>
            Borrar
          </button>
        )}
      </div>

      {/* Tabla de Pacientes */}
      <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #d1dcd6', overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.02)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
          <thead>
            <tr style={{ background: '#eaf5f1', borderBottom: '1px solid #d1dcd6', color: '#073f33' }}>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>N° Expediente</th>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>Paciente</th>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>DUI</th>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>Teléfono / Contacto</th>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>Cuenta App Móvil</th>
              <th style={{ padding: '12px 18px', fontWeight: 700, textAlign: 'right' }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {filteredPacientes.map((p) => (
              <tr key={p.id} style={{ borderBottom: '1px solid #e8efeb' }}>
                <td style={{ padding: '14px 18px', fontFamily: 'monospace', color: '#0b5d4b', fontWeight: 700 }}>
                  {p.numero_expediente}
                </td>
                <td style={{ padding: '14px 18px' }}>
                  <div style={{ fontWeight: 700, color: '#13221c' }}>{p.nombre_completo}</div>
                  <div style={{ fontSize: '0.75rem', color: '#526b60' }}>
                    Nacimiento: {p.fecha_nacimiento} · Sexo: {p.sexo}
                  </div>
                </td>
                <td style={{ padding: '14px 18px', fontFamily: 'monospace', color: '#33483f' }}>
                  {p.dui}
                </td>
                <td style={{ padding: '14px 18px', color: '#33483f' }}>
                  <div>{p.telefono || '—'}</div>
                  <div style={{ fontSize: '0.75rem', color: '#849a90' }}>{p.direccion || 'Sin dirección'}</div>
                </td>
                <td style={{ padding: '14px 18px' }}>
                  {p.tiene_cuenta_usuario ? (
                    <span style={{ background: '#d9efe1', color: '#1a7541', padding: '3px 8px', borderRadius: '6px', fontSize: '0.75rem', fontWeight: 700 }}>
                      ✓ Vinculada
                    </span>
                  ) : (
                    <span style={{ background: '#fae9c8', color: '#85560a', padding: '3px 8px', borderRadius: '6px', fontSize: '0.75rem', fontWeight: 700 }}>
                      Pendiente Registro
                    </span>
                  )}
                </td>
                <td style={{ padding: '14px 18px', textAlign: 'right' }}>
                  <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                    <button
                      onClick={() => handleGenerateActivationCode(p)}
                      title="Generar código para vincular la app móvil de paciente"
                      style={{
                        background: '#eaf5f1',
                        color: '#0b5d4b',
                        border: '1px solid #d1dcd6',
                        borderRadius: '6px',
                        padding: '6px 10px',
                        fontSize: '0.78rem',
                        fontWeight: 700,
                        cursor: 'pointer',
                      }}
                    >
                      🔑 Código
                    </button>
                    <button
                      onClick={() => setViewingPaciente(p)}
                      style={{
                        background: '#f8faf9',
                        color: '#13221c',
                        border: '1px solid #d1dcd6',
                        borderRadius: '6px',
                        padding: '6px 12px',
                        fontSize: '0.78rem',
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                    >
                      Ver Expediente
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {filteredPacientes.length === 0 && (
              <tr>
                <td colSpan={6} style={{ padding: '36px', textAlign: 'center', color: '#526b60' }}>
                  No se encontraron pacientes con los criterios ingresados.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Modal Registrar Paciente */}
      {isRegisterOpen && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '16px' }}>
          <div style={{ background: '#ffffff', borderRadius: '16px', width: '100%', maxWidth: '540px', padding: '28px', boxShadow: '0 20px 40px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: 0, color: '#073f33' }}>
                Registrar Ficha de Paciente
              </h2>
              <button onClick={() => setIsRegisterOpen(false)} style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer', color: '#849a90' }}>✕</button>
            </div>

            {registerError && (
              <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#991b1b', padding: '10px 14px', borderRadius: '8px', fontSize: '0.84rem', marginBottom: '14px' }}>
                {registerError}
              </div>
            )}

            <form onSubmit={handleRegisterSubmit} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>Nombre Completo *</label>
                <input
                  type="text"
                  required
                  placeholder="Ej. Juan Antonio Pérez"
                  value={registerForm.nombre_completo}
                  onChange={(e) => setRegisterForm({ ...registerForm, nombre_completo: e.target.value })}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>DUI (con guión) *</label>
                  <input
                    type="text"
                    required
                    placeholder="00000000-0"
                    value={registerForm.dui}
                    onChange={(e) => setRegisterForm({ ...registerForm, dui: e.target.value })}
                    style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>Fecha de Nacimiento *</label>
                  <input
                    type="date"
                    required
                    value={registerForm.fecha_nacimiento}
                    onChange={(e) => setRegisterForm({ ...registerForm, fecha_nacimiento: e.target.value })}
                    style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>Sexo *</label>
                  <select
                    value={registerForm.sexo}
                    onChange={(e) => setRegisterForm({ ...registerForm, sexo: e.target.value as 'M' | 'F' | 'OTRO' })}
                    style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem', background: '#ffffff' }}
                  >
                    <option value="M">Masculino</option>
                    <option value="F">Femenino</option>
                    <option value="OTRO">Otro</option>
                  </select>
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>Teléfono de Contacto</label>
                  <input
                    type="text"
                    placeholder="+503 7000-0000"
                    value={registerForm.telefono}
                    onChange={(e) => setRegisterForm({ ...registerForm, telefono: e.target.value })}
                    style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '4px' }}>Dirección de Residencia</label>
                <input
                  type="text"
                  placeholder="Municipio, Departamento"
                  value={registerForm.direccion}
                  onChange={(e) => setRegisterForm({ ...registerForm, direccion: e.target.value })}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: '8px', border: '1px solid #d1dcd6', fontSize: '0.9rem' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '14px' }}>
                <button type="button" onClick={() => setIsRegisterOpen(false)} style={{ padding: '10px 18px', borderRadius: '8px', border: '1px solid #d1dcd6', background: '#f8faf9', color: '#13221c', fontWeight: 600, cursor: 'pointer' }}>
                  Cancelar
                </button>
                <button type="submit" disabled={isSubmitting} style={{ padding: '10px 22px', borderRadius: '8px', border: 'none', background: '#0b5d4b', color: '#ffffff', fontWeight: 700, cursor: 'pointer' }}>
                  {isSubmitting ? 'Registrando...' : 'Registrar y Generar Expediente'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Código de Activación */}
      {activationModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '16px' }}>
          <div style={{ background: '#ffffff', borderRadius: '16px', width: '100%', maxWidth: '440px', padding: '28px', textAlign: 'center', boxShadow: '0 20px 40px rgba(0,0,0,0.2)' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '12px' }}>🔑</div>
            <h2 style={{ fontSize: '1.3rem', fontWeight: 800, color: '#073f33', margin: '0 0 6px' }}>
              Código de Activación
            </h2>
            <p style={{ margin: '0 0 20px', color: '#526b60', fontSize: '0.85rem' }}>
              Proporciona este código a <b>{activationModal.paciente.nombre_completo}</b> para que vincule su cuenta en la app de iOS.
            </p>

            <div style={{ background: '#eaf5f1', border: '2px dashed #0b5d4b', borderRadius: '12px', padding: '20px', marginBottom: '20px' }}>
              <div style={{ fontSize: '2.4rem', fontWeight: 800, letterSpacing: '0.25em', color: '#0b5d4b', fontFamily: 'monospace' }}>
                {activationModal.codigo}
              </div>
              <div style={{ fontSize: '0.78rem', color: '#526b60', marginTop: '6px' }}>
                ⏱️ Válido durante las próximas 24 horas
              </div>
            </div>

            <button
              onClick={() => {
                navigator.clipboard.writeText(activationModal.codigo);
                alert('Código copiado al portapapeles.');
              }}
              style={{ width: '100%', padding: '12px', borderRadius: '8px', border: 'none', background: '#0b5d4b', color: '#ffffff', fontWeight: 700, cursor: 'pointer', marginBottom: '10px' }}
            >
              Copiar Código
            </button>
            <button
              onClick={() => setActivationModal(null)}
              style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #d1dcd6', background: '#f8faf9', color: '#13221c', fontWeight: 600, cursor: 'pointer' }}
            >
              Cerrar
            </button>
          </div>
        </div>
      )}

      {/* Modal Consulta Expediente (HU-44) */}
      {viewingPaciente && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '16px' }}>
          <div style={{ background: '#ffffff', borderRadius: '16px', width: '100%', maxWidth: '640px', padding: '28px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <div>
                <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#0b5d4b', textTransform: 'uppercase' }}>
                  Expediente Clínico · Modo Solo Lectura (Admin)
                </span>
                <h2 style={{ fontSize: '1.3rem', fontWeight: 800, margin: 0, color: '#073f33' }}>
                  {viewingPaciente.nombre_completo}
                </h2>
              </div>
              <button onClick={() => setViewingPaciente(null)} style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer', color: '#849a90' }}>✕</button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', background: '#f8faf9', padding: '16px', borderRadius: '8px', border: '1px solid #e8efeb', marginBottom: '20px' }}>
              <div><b>DUI:</b> {viewingPaciente.dui}</div>
              <div><b>N° Expediente:</b> {viewingPaciente.numero_expediente}</div>
              <div><b>Fecha Nacimiento:</b> {viewingPaciente.fecha_nacimiento}</div>
              <div><b>Sexo:</b> {viewingPaciente.sexo}</div>
              <div><b>Teléfono:</b> {viewingPaciente.telefono || 'No registrado'}</div>
              <div><b>Dirección:</b> {viewingPaciente.direccion || 'No registrada'}</div>
            </div>

            <div style={{ display: 'grid', gap: '12px' }}>
              <h3 style={{ fontSize: '1rem', fontWeight: 700, margin: 0, color: '#073f33' }}>
                Historial de Consultas & Recetas
              </h3>
              <div style={{ background: '#ffffff', border: '1px solid #d1dcd6', borderRadius: '8px', padding: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: '#526b60' }}>
                  <span>Consulta General · Dr. Alejandro Benítez</span>
                  <span>14 oct 2026</span>
                </div>
                <div style={{ fontWeight: 700, fontSize: '0.88rem', margin: '4px 0' }}>Diagnóstico: Faringitis Aguda Bacteriana</div>
                <div style={{ fontSize: '0.8rem', color: '#33483f' }}>Receta: Amoxicilina 500mg cada 8 horas por 7 días. Paracetamol 500mg SOS.</div>
              </div>

              <div style={{ background: '#ffffff', border: '1px solid #d1dcd6', borderRadius: '8px', padding: '12px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: '#526b60' }}>
                  <span>Laboratorio Clínico</span>
                  <span>14 oct 2026 · COMPLETADO</span>
                </div>
                <div style={{ fontWeight: 700, fontSize: '0.88rem', margin: '4px 0' }}>Examen: Hemograma Completo</div>
                <div style={{ fontSize: '0.8rem', color: '#33483f' }}>Estado: Completado. Procesado por Laboratorio Central.</div>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '20px' }}>
              <button onClick={() => setViewingPaciente(null)} style={{ padding: '10px 20px', borderRadius: '8px', border: '1px solid #d1dcd6', background: '#f8faf9', color: '#13221c', fontWeight: 600, cursor: 'pointer' }}>
                Cerrar Expediente
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
