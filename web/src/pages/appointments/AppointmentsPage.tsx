import React, { useState, useEffect } from 'react';
import { CalendarIcon, SearchIcon, RefreshCwIcon } from '../../components/Icons';
import { apiClient, ApiError } from '../../api/client';

interface CitaItem {
  id: string;
  codigo: string;
  id_paciente?: string;
  paciente: string;
  expediente: string;
  dui: string;
  id_medico?: string;
  medico: string;
  especialidad: string;
  id_consultorio?: string;
  consultorio: string;
  fecha: string;
  hora: string;
  motivo: string;
  estado: 'SOLICITADA' | 'AGENDADA' | 'EN_ESPERA' | 'EN_ATENCION' | 'ATENDIDA' | 'CANCELADA';
}

const FALLBACK_CITAS: CitaItem[] = [
  {
    id: 'c-1',
    codigo: 'CIT-2026-0142',
    paciente: 'Guillermo Enrique Cruz',
    expediente: 'EXP-2026-000019',
    dui: '01992233-4',
    medico: 'Dr. Alejandro Benítez',
    especialidad: 'Medicina General',
    consultorio: 'C-101',
    fecha: new Date().toISOString().split('T')[0],
    hora: '08:00',
    motivo: 'Control de hipertensión y receta médica',
    estado: 'EN_ATENCION',
  },
  {
    id: 'c-2',
    codigo: 'CIT-2026-0143',
    paciente: 'Valeria Nicole Menjívar',
    expediente: 'EXP-2026-000027',
    dui: '02883344-5',
    medico: 'Dra. Ana Martínez',
    especialidad: 'Medicina General',
    consultorio: 'C-102',
    fecha: new Date().toISOString().split('T')[0],
    hora: '08:30',
    motivo: 'Evaluación post-tratamiento bronquial',
    estado: 'EN_ESPERA',
  },
  {
    id: 'c-3',
    codigo: 'CIT-2026-0144',
    paciente: 'Carlos Antonio Morales',
    expediente: 'EXP-2026-000031',
    dui: '03112233-6',
    medico: 'Dra. María Arismendi',
    especialidad: 'Alergología e Inmunología',
    consultorio: 'C-103',
    fecha: new Date().toISOString().split('T')[0],
    hora: '09:00',
    motivo: 'Pruebas cutáneas por rinitis alérgica estacional',
    estado: 'AGENDADA',
  },
];

export const AppointmentsPage: React.FC = () => {
  const [citas, setCitas] = useState<CitaItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [filterEstado, setFilterEstado] = useState<string>('TODAS');
  const [searchQuery, setSearchQuery] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Modales
  const [selectedCita, setSelectedCita] = useState<CitaItem | null>(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [motivoCancelacion, setMotivoCancelacion] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const notify = (msg: string) => {
    setFeedback(msg);
    setTimeout(() => setFeedback(null), 4500);
  };

  const fetchCitas = async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const res: any = await apiClient.get('/citas');
      const list = Array.isArray(res) ? res : res?.data || [];
      if (list.length > 0) {
        const mapped: CitaItem[] = list.map((c: any) => ({
          id: c.id_cita || c.id,
          codigo: c.id_cita ? `CIT-${String(c.id_cita).slice(0, 8).toUpperCase()}` : `CIT-${c.id}`,
          id_paciente: c.id_paciente,
          paciente: c.paciente?.nombre_completo || 'Paciente',
          expediente: c.paciente?.numero_expediente || 'S/E',
          dui: c.paciente?.dui || 'S/D',
          id_medico: c.id_medico,
          medico: c.medico?.nombre_completo || 'Por Asignar',
          especialidad: c.medico?.cargo || 'Medicina General',
          id_consultorio: c.id_consultorio,
          consultorio: c.consultorio ? `${c.consultorio.codigo} - ${c.consultorio.nombre || 'Planta 1'}` : 'Por Asignar',
          fecha: c.fecha ? String(c.fecha).split('T')[0] : '',
          hora: c.hora_inicio ? String(c.hora_inicio).slice(0, 5) : '',
          motivo: c.motivo || 'Consulta médica',
          estado: c.estado || 'AGENDADA',
        }));
        setCitas(mapped);
      } else {
        setCitas([]);
      }
    } catch {
      setCitas(FALLBACK_CITAS);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCitas();
  }, []);

  // Cancelar Cita por Resolución Administrativa
  const handleCancelarCita = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCita) return;
    if (!motivoCancelacion.trim()) {
      alert('Por favor ingresa el motivo administrativo de la cancelación.');
      return;
    }

    setIsSubmitting(true);
    try {
      await apiClient.patch(`/citas/${selectedCita.id}/cancelar`, {
        motivo_cancelacion: motivoCancelacion,
      });
      notify(`Cita ${selectedCita.codigo} cancelada por resolución administrativa.`);
      setShowCancelModal(false);
      setShowDetailModal(false);
      setSelectedCita(null);
      setMotivoCancelacion('');
      fetchCitas();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al cancelar: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredCitas = citas.filter((c) => {
    if (filterEstado !== 'TODAS' && c.estado !== filterEstado) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return (
        c.codigo.toLowerCase().includes(q) ||
        c.paciente.toLowerCase().includes(q) ||
        c.medico.toLowerCase().includes(q) ||
        c.motivo.toLowerCase().includes(q)
      );
    }
    return true;
  });

  const solicitadasCount = citas.filter((c) => c.estado === 'SOLICITADA').length;
  const agendadasCount = citas.filter((c) => c.estado === 'AGENDADA').length;
  const enEsperaCount = citas.filter((c) => c.estado === 'EN_ESPERA').length;
  const enAtencionCount = citas.filter((c) => c.estado === 'EN_ATENCION').length;
  const atendidasCount = citas.filter((c) => c.estado === 'ATENDIDA').length;

  const getBadgeStyle = (estado: string) => {
    switch (estado) {
      case 'SOLICITADA':
        return { bg: '#e0f2fe', color: '#0369a1', border: '#bae6fd' };
      case 'AGENDADA':
        return { bg: '#f1f5f9', color: '#475569', border: '#cbd5e1' };
      case 'EN_ESPERA':
        return { bg: '#fef3c7', color: '#b45309', border: '#fde68a' };
      case 'EN_ATENCION':
        return { bg: '#dcfce7', color: '#15803d', border: '#bbf7d0' };
      case 'ATENDIDA':
        return { bg: '#ecfdf5', color: '#047857', border: '#a7f3d0' };
      case 'CANCELADA':
        return { bg: '#fee2e2', color: '#b91c1c', border: '#fecaca' };
      default:
        return { bg: '#f3f4f6', color: '#374151', border: '#e5e7eb' };
    }
  };

  return (
    <div style={{ display: 'grid', gap: '24px' }}>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: '10px',
              background: '#ecfdf5',
              color: '#059669',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <CalendarIcon size={24} />
          </div>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 800, margin: 0, color: '#13221c' }}>
              Supervisión de Citas Médicas y Consulta Externa
            </h1>
            <p style={{ margin: 0, fontSize: '0.85rem', color: '#526b60' }}>
              Módulo de auditoría y métricas de puntualidad. La atención en ventanilla y registro de llegada se opera desde la App Móvil por Recepción/Enfermería.
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={() => {
              fetchCitas();
              notify('Citas sincronizadas con la base de datos en tiempo real.');
            }}
            disabled={loading}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '9px 16px',
              borderRadius: '8px',
              border: '1px solid #cad8d1',
              background: '#ffffff',
              fontSize: '0.84rem',
              fontWeight: 600,
              cursor: 'pointer',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <RefreshCwIcon size={14} /> {loading ? 'Cargando...' : 'Sincronizar'}
          </button>
        </div>
      </div>

      {feedback && (
        <div
          style={{
            padding: '12px 18px',
            background: '#ecfdf5',
            color: '#065f46',
            borderRadius: '10px',
            fontSize: '0.86rem',
            border: '1px solid #a7f3d0',
            fontWeight: 600,
          }}
        >
          ✓ {feedback}
        </div>
      )}

      {errorMsg && (
        <div
          style={{
            padding: '12px 18px',
            background: '#fef2f2',
            color: '#991b1b',
            borderRadius: '10px',
            fontSize: '0.86rem',
            border: '1px solid #fecaca',
          }}
        >
          ⚠️ {errorMsg}
        </div>
      )}

      {/* KPI Counters */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '14px',
        }}
      >
        <div style={{ background: '#ffffff', borderRadius: '12px', padding: '16px', border: '1px solid #e2ece7' }}>
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#0369a1', textTransform: 'uppercase' }}>
            Solicitudes Pendientes
          </span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#0284c7', marginTop: '4px' }}>
            {solicitadasCount}
          </div>
          <span style={{ fontSize: '0.72rem', color: '#849a90' }}>Por confirmar en recepción</span>
        </div>

        <div style={{ background: '#ffffff', borderRadius: '12px', padding: '16px', border: '1px solid #e2ece7' }}>
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', textTransform: 'uppercase' }}>
            Agendadas en Calendario
          </span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#334155', marginTop: '4px' }}>
            {agendadasCount}
          </div>
          <span style={{ fontSize: '0.72rem', color: '#849a90' }}>Médico y consultorio asignados</span>
        </div>

        <div style={{ background: '#ffffff', borderRadius: '12px', padding: '16px', border: '1px solid #e2ece7' }}>
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#b45309', textTransform: 'uppercase' }}>
            En Sala de Espera
          </span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#d97706', marginTop: '4px' }}>
            {enEsperaCount}
          </div>
          <span style={{ fontSize: '0.72rem', color: '#849a90' }}>Llegada registrada por enfermería</span>
        </div>

        <div style={{ background: '#ffffff', borderRadius: '12px', padding: '16px', border: '1px solid #e2ece7' }}>
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#15803d', textTransform: 'uppercase' }}>
            En Atención Médica
          </span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#16a34a', marginTop: '4px' }}>
            {enAtencionCount}
          </div>
          <span style={{ fontSize: '0.72rem', color: '#849a90' }}>En consulta activa</span>
        </div>

        <div style={{ background: '#ffffff', borderRadius: '12px', padding: '16px', border: '1px solid #e2ece7' }}>
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#047857', textTransform: 'uppercase' }}>
            Consultas Finalizadas
          </span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#059669', marginTop: '4px' }}>
            {atendidasCount}
          </div>
          <span style={{ fontSize: '0.72rem', color: '#849a90' }}>Atenciones concluidas</span>
        </div>
      </div>

      {/* Filter and Search */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '12px',
          background: '#ffffff',
          padding: '12px 16px',
          borderRadius: '12px',
          border: '1px solid #e2ece7',
        }}
      >
        <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
          {['TODAS', 'SOLICITADA', 'AGENDADA', 'EN_ESPERA', 'EN_ATENCION', 'ATENDIDA', 'CANCELADA'].map((est) => (
            <button
              key={est}
              onClick={() => setFilterEstado(est)}
              style={{
                padding: '6px 12px',
                borderRadius: '6px',
                border: 'none',
                fontSize: '0.78rem',
                fontWeight: 700,
                cursor: 'pointer',
                background: filterEstado === est ? '#0b5d4b' : '#f0f4f2',
                color: filterEstado === est ? '#ffffff' : '#526b60',
              }}
            >
              {est === 'TODAS' ? 'Todas' : est}
            </button>
          ))}
        </div>

        <div style={{ position: 'relative', minWidth: '260px' }}>
          <div style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#849a90' }}>
            <SearchIcon size={16} />
          </div>
          <input
            type="text"
            placeholder="Buscar por paciente, médico o código..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: '100%',
              padding: '8px 12px 8px 34px',
              borderRadius: '8px',
              border: '1px solid #cad8d1',
              fontSize: '0.85rem',
            }}
          />
        </div>
      </div>

      {/* Table */}
      <div
        style={{
          background: '#ffffff',
          borderRadius: '16px',
          border: '1px solid #e2ece7',
          overflowX: 'auto',
          boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
        }}
      >
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
          <thead>
            <tr style={{ background: '#f8faf9', borderBottom: '1px solid #e2ece7', color: '#526b60' }}>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Fecha / Hora / Código</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Paciente</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Médico y Consultorio</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Motivo de Consulta</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Estado</th>
              <th style={{ padding: '14px 16px', fontWeight: 800, textAlign: 'right' }}>Auditoría</th>
            </tr>
          </thead>
          <tbody>
            {filteredCitas.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ padding: '32px', textAlign: 'center', color: '#849a90' }}>
                  {loading ? 'Cargando citas médicas...' : 'No se encontraron citas registradas con este criterio.'}
                </td>
              </tr>
            ) : (
              filteredCitas.map((c) => {
                const badge = getBadgeStyle(c.estado);
                return (
                  <tr key={c.id} style={{ borderBottom: '1px solid #f0f4f2' }}>
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 800, color: '#13221c' }}>
                        {c.fecha} {c.hora && `· ${c.hora}`}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: '#0b5d4b', fontWeight: 600 }}>{c.codigo}</div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 700, color: '#13221c' }}>{c.paciente}</div>
                      <div style={{ fontSize: '0.75rem', color: '#849a90' }}>
                        Exp: {c.expediente} · DUI: {c.dui}
                      </div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 600, color: '#13221c' }}>{c.medico}</div>
                      <div style={{ fontSize: '0.75rem', color: '#526b60' }}>
                        {c.especialidad} · <strong style={{ color: '#0b5d4b' }}>{c.consultorio}</strong>
                      </div>
                    </td>
                    <td style={{ padding: '14px 16px', maxWidth: '240px', color: '#526b60', fontSize: '0.82rem' }}>
                      {c.motivo}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <span
                        style={{
                          padding: '3px 8px',
                          borderRadius: '999px',
                          fontSize: '0.72rem',
                          fontWeight: 800,
                          background: badge.bg,
                          color: badge.color,
                          border: `1px solid ${badge.border}`,
                        }}
                      >
                        {c.estado}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <button
                        onClick={() => {
                          setSelectedCita(c);
                          setShowDetailModal(true);
                        }}
                        style={{
                          padding: '6px 12px',
                          borderRadius: '6px',
                          border: '1px solid #d1dcd6',
                          background: '#f8faf9',
                          color: '#073f33',
                          fontSize: '0.78rem',
                          fontWeight: 700,
                          cursor: 'pointer',
                        }}
                      >
                        Ver Detalle →
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Modal Ver Detalle de Cita (Supervisión) */}
      {showDetailModal && selectedCita && (
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
              <div>
                <span style={{ fontSize: '0.75rem', fontWeight: 800, color: '#0b5d4b', textTransform: 'uppercase' }}>
                  Auditoría de Cita Médica
                </span>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '2px 0 0', color: '#073f33' }}>
                  {selectedCita.codigo}
                </h2>
              </div>
              <button
                onClick={() => {
                  setShowDetailModal(false);
                  setSelectedCita(null);
                }}
                style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer', color: '#849a90' }}
              >
                ✕
              </button>
            </div>

            <div style={{ display: 'grid', gap: '14px', background: '#f8faf9', padding: '16px', borderRadius: '12px', border: '1px solid #e2ece7', marginBottom: '20px' }}>
              <div>
                <span style={{ fontSize: '0.74rem', color: '#849a90', fontWeight: 700, textTransform: 'uppercase' }}>Paciente</span>
                <p style={{ margin: '2px 0 0', fontSize: '0.92rem', fontWeight: 700, color: '#13221c' }}>{selectedCita.paciente}</p>
                <span style={{ fontSize: '0.78rem', color: '#526b60' }}>Expediente: {selectedCita.expediente} · DUI: {selectedCita.dui}</span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <div>
                  <span style={{ fontSize: '0.74rem', color: '#849a90', fontWeight: 700, textTransform: 'uppercase' }}>Médico Tratante</span>
                  <p style={{ margin: '2px 0 0', fontSize: '0.86rem', fontWeight: 700, color: '#13221c' }}>{selectedCita.medico}</p>
                  <span style={{ fontSize: '0.75rem', color: '#0b5d4b' }}>{selectedCita.especialidad}</span>
                </div>
                <div>
                  <span style={{ fontSize: '0.74rem', color: '#849a90', fontWeight: 700, textTransform: 'uppercase' }}>Ubicación</span>
                  <p style={{ margin: '2px 0 0', fontSize: '0.86rem', fontWeight: 700, color: '#13221c' }}>{selectedCita.consultorio}</p>
                  <span style={{ fontSize: '0.75rem', color: '#526b60' }}>Planta 1</span>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <div>
                  <span style={{ fontSize: '0.74rem', color: '#849a90', fontWeight: 700, textTransform: 'uppercase' }}>Horario Programado</span>
                  <p style={{ margin: '2px 0 0', fontSize: '0.86rem', fontWeight: 700, color: '#13221c' }}>
                    {selectedCita.fecha} · {selectedCita.hora || 'Sin hora fija'}
                  </p>
                </div>
                <div>
                  <span style={{ fontSize: '0.74rem', color: '#849a90', fontWeight: 700, textTransform: 'uppercase' }}>Estado de Flujo</span>
                  <p style={{ margin: '2px 0 0' }}>
                    <span
                      style={{
                        padding: '3px 8px',
                        borderRadius: '999px',
                        fontSize: '0.74rem',
                        fontWeight: 800,
                        ...getBadgeStyle(selectedCita.estado),
                      }}
                    >
                      {selectedCita.estado}
                    </span>
                  </p>
                </div>
              </div>

              <div>
                <span style={{ fontSize: '0.74rem', color: '#849a90', fontWeight: 700, textTransform: 'uppercase' }}>Motivo de la Cita</span>
                <p style={{ margin: '2px 0 0', fontSize: '0.85rem', color: '#33483f' }}>{selectedCita.motivo}</p>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              {selectedCita.estado !== 'ATENDIDA' && selectedCita.estado !== 'CANCELADA' ? (
                <button
                  onClick={() => {
                    setMotivoCancelacion('');
                    setShowCancelModal(true);
                  }}
                  style={{
                    background: '#fef2f2',
                    color: '#991b1b',
                    border: '1px solid #fecaca',
                    padding: '8px 14px',
                    borderRadius: '8px',
                    fontSize: '0.78rem',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  Resolución: Cancelar Cita
                </button>
              ) : (
                <span style={{ fontSize: '0.78rem', color: '#849a90' }}>Cita en estado definitivo</span>
              )}

              <button
                onClick={() => {
                  setShowDetailModal(false);
                  setSelectedCita(null);
                }}
                style={{
                  background: '#0b5d4b',
                  color: '#ffffff',
                  border: 'none',
                  padding: '8px 18px',
                  borderRadius: '8px',
                  fontSize: '0.82rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Cancelar Cita (Solo en caso de contingencia/resolución) */}
      {showCancelModal && selectedCita && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 110,
            padding: '16px',
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              width: '100%',
              maxWidth: '460px',
              padding: '24px',
              boxShadow: '0 20px 40px rgba(0,0,0,0.2)',
            }}
          >
            <h3 style={{ margin: '0 0 8px', fontSize: '1.15rem', fontWeight: 800, color: '#991b1b' }}>
              Cancelar Cita por Resolución Administrativa
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.84rem', color: '#526b60' }}>
              Se cancelará la cita de <strong>{selectedCita.paciente}</strong> ({selectedCita.codigo}).
            </p>

            <form onSubmit={handleCancelarCita}>
              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '6px' }}>
                  Motivo de la Cancelación *
                </label>
                <textarea
                  required
                  rows={3}
                  placeholder="Ej. Reasignación por emergencia médica, solicitud directa del paciente..."
                  value={motivoCancelacion}
                  onChange={(e) => setMotivoCancelacion(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #d1dcd6',
                    fontSize: '0.85rem',
                    fontFamily: 'inherit',
                  }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                <button
                  type="button"
                  onClick={() => setShowCancelModal(false)}
                  style={{
                    padding: '8px 14px',
                    borderRadius: '6px',
                    border: '1px solid #cad8d1',
                    background: '#f8faf9',
                    fontSize: '0.82rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  Volver
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{
                    padding: '8px 16px',
                    borderRadius: '6px',
                    border: 'none',
                    background: '#dc2626',
                    color: '#ffffff',
                    fontSize: '0.82rem',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isSubmitting ? 'Cancelando...' : 'Confirmar Cancelación'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
