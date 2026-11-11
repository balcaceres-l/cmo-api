import React, { useState, useEffect } from 'react';
import { AmbulanceIcon, RefreshCwIcon, PlusCircleIcon } from '../../components/Icons';
import { apiClient, ApiError } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { isJefeOperaciones } from '../../utils/roles';

interface ReporteTraslado {
  id: string;
  minutosLlegada?: number;
  llegada_estimada?: string;
  estadoPaciente: string;
  observaciones: string;
  hora: string;
  autor?: string;
}

interface ServicioAmbulancia {
  id: string;
  codigo: string;
  solicitante: string;
  pacienteAtendido: string;
  direccion: string;
  prioridad: 'ALTA' | 'MEDIA' | 'BAJA' | 'CRITICA';
  estado: 'EN_CURSO' | 'FINALIZADO' | 'CANCELADO';
  horaDespacho: string;
  horaLlegada?: string;
  paramedico: string;
  conductor: string;
  reportes: ReporteTraslado[];
}

const FALLBACK_SERVICIOS: ServicioAmbulancia[] = [
  {
    id: 's-101',
    codigo: 'EMG-2026-089',
    solicitante: 'Dra. Ana Martínez (Triage Central)',
    pacienteAtendido: 'Julio César Campos (28 años)',
    direccion: 'Carretera a Santa Ana Km 64, Col. San Juan',
    prioridad: 'ALTA',
    estado: 'FINALIZADO',
    horaDespacho: 'Ayer 21:40',
    horaLlegada: 'Ayer 22:25',
    paramedico: 'Lic. José Ramírez',
    conductor: 'Manuel Antonio Quintanilla',
    reportes: [
      {
        id: 'r-1',
        minutosLlegada: 12,
        estadoPaciente: 'Estable en camilla, hemorragia contenida',
        observaciones: 'Vía endovenosa permeable con Hartman 500cc',
        hora: '21:52',
      },
    ],
  },
];

export const AmbulancePage: React.FC = () => {
  const { user } = useAuth();
  const isOps = isJefeOperaciones(user);

  const [estadoAmbulancia, setEstadoAmbulancia] = useState<'DISPONIBLE' | 'EN_SERVICIO' | 'EN_MANTENIMIENTO'>('DISPONIBLE');
  const [matricula, setMatricula] = useState<string>('P-123456');
  const [servicios, setServicios] = useState<ServicioAmbulancia[]>([]);
  const [servicioActivo, setServicioActivo] = useState<ServicioAmbulancia | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [filterPrioridad, setFilterPrioridad] = useState<string>('TODAS');
  const [searchQuery, setSearchQuery] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Modales
  const [showDispatchModal, setShowDispatchModal] = useState(false);
  const [showReportModal, setShowReportModal] = useState(false);
  const [showFinalizeModal, setShowFinalizeModal] = useState(false);
  const [notasFinalizacion, setNotasFinalizacion] = useState('');

  // Formularios
  const [dispatchForm, setDispatchForm] = useState({
    paciente_nombre: '',
    direccion: '',
    descripcion: '',
    prioridad: 'ALTA' as 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA',
  });

  const [reportForm, setReportForm] = useState({
    estado_paciente: 'ESTABLE' as 'ESTABLE' | 'DELICADO' | 'CRITICO',
    observaciones: '',
    minutos_llegada: 10,
  });

  const notify = (msg: string) => {
    setFeedback(msg);
    setTimeout(() => setFeedback(null), 4500);
  };

  const fetchAmbulanciaData = async () => {
    setLoading(true);
    try {
      const [ambRes, bitacoraRes]: any[] = await Promise.allSettled([
        apiClient.get('/ambulancia'),
        apiClient.get('/ambulancia/servicios'),
      ]);

      if (ambRes.status === 'fulfilled' && ambRes.value) {
        const amb = ambRes.value;
        setEstadoAmbulancia(amb.estado || 'DISPONIBLE');
        if (amb.matricula) setMatricula(amb.matricula);

        // Si tiene servicio activo en curso
        const activeServ = amb.servicios && amb.servicios.length > 0 ? amb.servicios[0] : null;
        if (activeServ) {
          const mappedActive: ServicioAmbulancia = {
            id: activeServ.id_servicio || activeServ.id,
            codigo: activeServ.id_servicio ? `EMG-${String(activeServ.id_servicio).slice(0, 8).toUpperCase()}` : 'EMG-ACTIVO',
            solicitante: 'Centro de Despacho CMO',
            pacienteAtendido: activeServ.paciente_nombre || 'Paciente en Traslado',
            direccion: activeServ.direccion || 'En ruta de urgencia',
            prioridad: activeServ.prioridad || 'ALTA',
            estado: 'EN_CURSO',
            horaDespacho: activeServ.hora_salida ? new Date(activeServ.hora_salida).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Reciente',
            paramedico: 'Personal de Ambulancia',
            conductor: 'Conductor Operativo',
            reportes: activeServ.ultimo_reporte
              ? [
                  {
                    id: 'r-last',
                    estadoPaciente: activeServ.ultimo_reporte.estado_paciente,
                    observaciones: activeServ.ultimo_reporte.observaciones || 'Sin observaciones',
                    hora: activeServ.ultimo_reporte.fecha ? new Date(activeServ.ultimo_reporte.fecha).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Último reporte',
                    llegada_estimada: activeServ.ultimo_reporte.llegada_estimada,
                  },
                ]
              : [],
          };
          setServicioActivo(mappedActive);

          // Cargar reportes completos del servicio activo
          try {
            const repsRes: any = await apiClient.get(`/ambulancia/servicios/${mappedActive.id}/reportes`);
            const repList = Array.isArray(repsRes) ? repsRes : repsRes?.data || [];
            if (repList.length > 0) {
              setServicioActivo((prev) =>
                prev
                  ? {
                      ...prev,
                      reportes: repList.map((r: any) => ({
                        id: r.id_reporte || r.id,
                        estadoPaciente: r.estado_paciente,
                        observaciones: r.observaciones || '',
                        hora: r.fecha ? new Date(r.fecha).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '',
                        autor: r.autor?.nombre_completo,
                        llegada_estimada: r.llegada_estimada,
                      })),
                    }
                  : null
              );
            }
          } catch {
            // Ignorar
          }
        } else {
          setServicioActivo(null);
        }
      }

      if (bitacoraRes.status === 'fulfilled' && bitacoraRes.value) {
        const list = Array.isArray(bitacoraRes.value)
          ? bitacoraRes.value
          : bitacoraRes.value?.data || [];

        const mapped: ServicioAmbulancia[] = list.map((s: any) => ({
          id: s.id_servicio || s.id,
          codigo: s.id_servicio ? `EMG-${String(s.id_servicio).slice(0, 8).toUpperCase()}` : `EMG-${s.id}`,
          solicitante: s.id_solicitante ? 'Personal Hospitalario' : 'Emergencias CMO',
          pacienteAtendido: s.paciente_nombre || 'Paciente',
          direccion: s.direccion || 'Ubicación de emergencia',
          prioridad: s.prioridad || 'ALTA',
          estado: s.estado || 'FINALIZADO',
          horaDespacho: s.hora_salida ? new Date(s.hora_salida).toLocaleString() : s.hora_solicitud ? new Date(s.hora_solicitud).toLocaleString() : 'N/A',
          horaLlegada: s.hora_llegada ? new Date(s.hora_llegada).toLocaleString() : undefined,
          paramedico: 'Paramédico en Turno',
          conductor: 'Chofer de Guardia',
          reportes: [],
        }));
        setServicios(mapped);
      } else {
        setServicios(FALLBACK_SERVICIOS);
      }
    } catch {
      setServicios(FALLBACK_SERVICIOS);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAmbulanciaData();
  }, []);

  // Cambiar estado (DISPONIBLE <-> EN_MANTENIMIENTO)
  const handleCambiarEstado = async (nuevoEstado: 'DISPONIBLE' | 'EN_MANTENIMIENTO') => {
    try {
      await apiClient.patch('/ambulancia/estado', { estado: nuevoEstado });
      setEstadoAmbulancia(nuevoEstado);
      notify(`Estado de ambulancia actualizado a ${nuevoEstado} en base de datos.`);
      fetchAmbulanciaData();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al actualizar estado: ${e.message}`);
    }
  };

  // Despachar ambulancia
  const handleDespacharNuevo = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!dispatchForm.paciente_nombre || !dispatchForm.direccion || !dispatchForm.descripcion) {
      alert('Completa paciente, dirección y motivo de despacho.');
      return;
    }

    setIsSubmitting(true);
    try {
      await apiClient.post('/ambulancia/servicios', dispatchForm);
      notify('Ambulancia despachada exitosamente. Servicio EN CURSO registrado.');
      setShowDispatchModal(false);
      setDispatchForm({
        paciente_nombre: '',
        direccion: '',
        descripcion: '',
        prioridad: 'ALTA',
      });
      fetchAmbulanciaData();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al despachar: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Finalizar servicio
  const handleFinalizarSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!servicioActivo) return;

    setIsSubmitting(true);
    try {
      await apiClient.patch(`/ambulancia/servicios/${servicioActivo.id}/finalizar`, {
        notas: notasFinalizacion || 'Servicio completado satisfactoriamente.',
      });
      notify('Servicio finalizado exitosamente. La ambulancia vuelve a estar DISPONIBLE.');
      setShowFinalizeModal(false);
      setNotasFinalizacion('');
      setServicioActivo(null);
      fetchAmbulanciaData();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al finalizar servicio: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Enviar reporte desde la ambulancia en ruta
  const handleReportarSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!servicioActivo) return;

    setIsSubmitting(true);
    try {
      await apiClient.post(`/ambulancia/servicios/${servicioActivo.id}/reportes`, reportForm);
      notify('Reporte clínico de ruta transmitido con éxito al equipo de emergencias.');
      setShowReportModal(false);
      setReportForm({
        estado_paciente: 'ESTABLE',
        observaciones: '',
        minutos_llegada: 10,
      });
      fetchAmbulanciaData();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al enviar reporte: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredServicios = servicios.filter((s) => {
    if (filterPrioridad !== 'TODAS' && s.prioridad !== filterPrioridad) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return (
        s.codigo.toLowerCase().includes(q) ||
        s.pacienteAtendido.toLowerCase().includes(q) ||
        s.direccion.toLowerCase().includes(q)
      );
    }
    return true;
  });

  return (
    <div style={{ display: 'grid', gap: '24px' }}>
      {/* Top Header */}
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
              width: '40px',
              height: '40px',
              borderRadius: '10px',
              background: '#e0f2fe',
              color: '#0284c7',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <AmbulanceIcon size={24} />
          </div>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 800, margin: 0, color: '#13221c' }}>
              Gestión de Ambulancia y Soporte Vital
            </h1>
            <p style={{ margin: 0, fontSize: '0.85rem', color: '#526b60' }}>
              {isOps
                ? `Control operativo de la ambulancia (${matricula}): despacho de emergencias, estado mecánico y reportes en ruta.`
                : `Supervisión institucional y auditoría de la unidad médica única (${matricula}).`}
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={() => {
              fetchAmbulanciaData();
              notify('Estado de ambulancia y bitácora actualizados desde la BD.');
            }}
            disabled={loading}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '8px 14px',
              borderRadius: '8px',
              border: '1px solid #cad8d1',
              background: '#ffffff',
              fontSize: '0.82rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            <RefreshCwIcon size={14} /> {loading ? 'Cargando...' : 'Sincronizar'}
          </button>
          {isOps && (
            <button
              onClick={() => setShowDispatchModal(true)}
              disabled={estadoAmbulancia !== 'DISPONIBLE'}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                padding: '8px 16px',
                borderRadius: '8px',
                border: 'none',
                background: estadoAmbulancia === 'DISPONIBLE' ? '#0b5d4b' : '#9ca3af',
                color: '#ffffff',
                fontSize: '0.82rem',
                fontWeight: 700,
                cursor: estadoAmbulancia === 'DISPONIBLE' ? 'pointer' : 'not-allowed',
              }}
            >
              <PlusCircleIcon size={16} /> Despachar Ambulancia
            </button>
          )}
        </div>
      </div>

      {feedback && (
        <div
          style={{
            padding: '12px 18px',
            background: '#ecfdf5',
            color: '#065f46',
            borderRadius: '10px',
            border: '1px solid #a7f3d0',
            fontWeight: 600,
            fontSize: '0.88rem',
          }}
        >
          ✓ {feedback}
        </div>
      )}

      {/* Main Status Hero */}
      <div
        style={{
          background: '#ffffff',
          borderRadius: '16px',
          border: '1px solid #e2ece7',
          padding: '24px',
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '24px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
        }}
      >
        <div>
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#526b60', textTransform: 'uppercase' }}>
            Estado Operativo en Tiempo Real
          </span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginTop: '8px' }}>
            <span
              style={{
                fontSize: '1.4rem',
                fontWeight: 800,
                color:
                  estadoAmbulancia === 'DISPONIBLE'
                    ? '#059669'
                    : estadoAmbulancia === 'EN_SERVICIO'
                    ? '#0284c7'
                    : '#dc2626',
              }}
            >
              ● {estadoAmbulancia}
            </span>
            <span style={{ fontSize: '0.85rem', color: '#849a90' }}>Placa: {matricula}</span>
          </div>

          {isOps ? (
            <div style={{ display: 'flex', gap: '8px', marginTop: '16px' }}>
              <button
                onClick={() => handleCambiarEstado('DISPONIBLE')}
                disabled={estadoAmbulancia === 'EN_SERVICIO' || estadoAmbulancia === 'DISPONIBLE'}
                style={{
                  padding: '6px 12px',
                  borderRadius: '6px',
                  border: '1px solid #a7f3d0',
                  background: '#ecfdf5',
                  color: '#047857',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  cursor: estadoAmbulancia === 'DISPONIBLE' ? 'default' : 'pointer',
                }}
              >
                Poner Disponible
              </button>
              <button
                onClick={() => handleCambiarEstado('EN_MANTENIMIENTO')}
                disabled={estadoAmbulancia === 'EN_SERVICIO' || estadoAmbulancia === 'EN_MANTENIMIENTO'}
                style={{
                  padding: '6px 12px',
                  borderRadius: '6px',
                  border: '1px solid #fecaca',
                  background: '#fff5f5',
                  color: '#b91c1c',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  cursor: estadoAmbulancia === 'EN_MANTENIMIENTO' ? 'default' : 'pointer',
                }}
              >
                Pasar a Mantenimiento
              </button>
            </div>
          ) : (
            <div style={{ marginTop: '16px', fontSize: '0.78rem', color: '#526b60', fontWeight: 600 }}>
              Operado y controlado por Jefatura de Operaciones.
            </div>
          )}
        </div>

        {/* Servicio Activo si existe */}
        {servicioActivo ? (
          <div
            style={{
              background: '#f0f9ff',
              borderRadius: '12px',
              border: '1px solid #bae6fd',
              padding: '16px',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 800, color: '#0369a1', textTransform: 'uppercase' }}>
                Servicio en Curso ({servicioActivo.codigo})
              </span>
              <span
                style={{
                  padding: '2px 8px',
                  borderRadius: '999px',
                  fontSize: '0.7rem',
                  fontWeight: 800,
                  background: '#fee2e2',
                  color: '#b91c1c',
                }}
              >
                {servicioActivo.prioridad}
              </span>
            </div>

            <div style={{ marginTop: '8px' }}>
              <div style={{ fontWeight: 800, fontSize: '0.95rem', color: '#13221c' }}>
                {servicioActivo.pacienteAtendido}
              </div>
              <div style={{ fontSize: '0.8rem', color: '#526b60' }}>
                📍 {servicioActivo.direccion}
              </div>
            </div>

            {servicioActivo.reportes.length > 0 && (
              <div style={{ marginTop: '10px', background: '#ffffff', padding: '10px', borderRadius: '8px', border: '1px solid #e0f2fe' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#0b5d4b' }}>
                  Último Reporte Clínico en Ruta ({servicioActivo.reportes[0].hora}):
                </div>
                <div style={{ fontSize: '0.8rem', color: '#13221c', marginTop: '2px' }}>
                  Estado: <strong>{servicioActivo.reportes[0].estadoPaciente}</strong>
                </div>
                <div style={{ fontSize: '0.75rem', color: '#526b60' }}>
                  {servicioActivo.reportes[0].observaciones}
                </div>
              </div>
            )}

            {isOps ? (
              <div style={{ display: 'flex', gap: '8px', marginTop: '14px' }}>
                <button
                  onClick={() => setShowReportModal(true)}
                  style={{
                    flex: 1,
                    padding: '8px',
                    borderRadius: '6px',
                    border: '1px solid #38bdf8',
                    background: '#ffffff',
                    color: '#0284c7',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  + Reporte de Ruta
                </button>
                <button
                  onClick={() => setShowFinalizeModal(true)}
                  style={{
                    flex: 1,
                    padding: '8px',
                    borderRadius: '6px',
                    border: 'none',
                    background: '#0284c7',
                    color: '#ffffff',
                    fontSize: '0.75rem',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  Finalizar Servicio
                </button>
              </div>
            ) : (
              <div
                style={{
                  marginTop: '14px',
                  padding: '8px 12px',
                  background: '#e0f2fe',
                  color: '#0369a1',
                  borderRadius: '6px',
                  fontSize: '0.76rem',
                  fontWeight: 700,
                  textAlign: 'center',
                }}
              >
                • Despacho y reportes en ruta operados por Jefatura de Operaciones
              </div>
            )}
          </div>
        ) : (
          <div
            style={{
              background: '#f8faf9',
              borderRadius: '12px',
              border: '1px solid #e2ece7',
              padding: '16px',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              alignItems: 'center',
              color: '#849a90',
            }}
          >
            <div style={{ fontSize: '2rem' }}>🚑</div>
            <div style={{ fontSize: '0.85rem', fontWeight: 600, marginTop: '4px' }}>
              Sin servicios de emergencia en curso
            </div>
            <div style={{ fontSize: '0.75rem' }}>La ambulancia está disponible en rampa para despacho.</div>
          </div>
        )}
      </div>

      {/* Bitácora de Servicios */}
      <div
        style={{
          background: '#ffffff',
          borderRadius: '16px',
          border: '1px solid #e2ece7',
          padding: '20px',
          boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 800, margin: 0, color: '#13221c' }}>
              Bitácora de Despachos y Servicios
            </h3>
            <p style={{ margin: 0, fontSize: '0.8rem', color: '#526b60' }}>
              Historial de traslados, prioridades de atención y horas de respuesta
            </p>
          </div>

          <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
            <div style={{ position: 'relative', minWidth: '220px' }}>
              <input
                type="text"
                placeholder="Buscar en bitácora..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                style={{
                  width: '100%',
                  padding: '6px 10px',
                  borderRadius: '6px',
                  border: '1px solid #cad8d1',
                  fontSize: '0.8rem',
                }}
              />
            </div>
            {['TODAS', 'CRITICA', 'ALTA', 'MEDIA', 'BAJA'].map((prio) => (
              <button
                key={prio}
                onClick={() => setFilterPrioridad(prio)}
                style={{
                  padding: '4px 10px',
                  borderRadius: '6px',
                  border: 'none',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  background: filterPrioridad === prio ? '#0b5d4b' : '#f0f4f2',
                  color: filterPrioridad === prio ? '#ffffff' : '#526b60',
                }}
              >
                {prio}
              </button>
            ))}
          </div>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
            <thead>
              <tr style={{ background: '#f8faf9', borderBottom: '1px solid #e2ece7', color: '#526b60' }}>
                <th style={{ padding: '12px 14px', fontWeight: 800 }}>Código / Horas</th>
                <th style={{ padding: '12px 14px', fontWeight: 800 }}>Paciente y Dirección</th>
                <th style={{ padding: '12px 14px', fontWeight: 800 }}>Prioridad</th>
                <th style={{ padding: '12px 14px', fontWeight: 800 }}>Estado</th>
              </tr>
            </thead>
            <tbody>
              {filteredServicios.length === 0 ? (
                <tr>
                  <td colSpan={4} style={{ padding: '24px', textAlign: 'center', color: '#849a90' }}>
                    No se registran despachos con este filtro.
                  </td>
                </tr>
              ) : (
                filteredServicios.map((s) => (
                  <tr key={s.id} style={{ borderBottom: '1px solid #f0f4f2' }}>
                    <td style={{ padding: '12px 14px' }}>
                      <div style={{ fontWeight: 800, color: '#13221c' }}>{s.codigo}</div>
                      <div style={{ fontSize: '0.72rem', color: '#849a90' }}>Salida: {s.horaDespacho}</div>
                      {s.horaLlegada && <div style={{ fontSize: '0.72rem', color: '#0b5d4b' }}>Llegada: {s.horaLlegada}</div>}
                    </td>
                    <td style={{ padding: '12px 14px' }}>
                      <div style={{ fontWeight: 700, color: '#13221c' }}>{s.pacienteAtendido}</div>
                      <div style={{ fontSize: '0.75rem', color: '#526b60' }}>{s.direccion}</div>
                    </td>
                    <td style={{ padding: '12px 14px' }}>
                      <span
                        style={{
                          padding: '3px 8px',
                          borderRadius: '999px',
                          fontSize: '0.72rem',
                          fontWeight: 800,
                          background: s.prioridad === 'CRITICA' || s.prioridad === 'ALTA' ? '#fee2e2' : '#fef3c7',
                          color: s.prioridad === 'CRITICA' || s.prioridad === 'ALTA' ? '#b91c1c' : '#b45309',
                        }}
                      >
                        {s.prioridad}
                      </span>
                    </td>
                    <td style={{ padding: '12px 14px' }}>
                      <span
                        style={{
                          padding: '3px 8px',
                          borderRadius: '999px',
                          fontSize: '0.72rem',
                          fontWeight: 800,
                          background: s.estado === 'EN_CURSO' ? '#e0f2fe' : '#ecfdf5',
                          color: s.estado === 'EN_CURSO' ? '#0369a1' : '#047857',
                        }}
                      >
                        {s.estado}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* MODAL DESPACHAR */}
      {showDispatchModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 50,
            padding: '16px',
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              maxWidth: '480px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
            }}
          >
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
              Despachar Unidad de Soporte Vital
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.82rem', color: '#526b60' }}>
              Asigna la ambulancia a una emergencia inmediata.
            </p>

            <form onSubmit={handleDespacharNuevo} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Nombre del Paciente
                </label>
                <input
                  type="text"
                  value={dispatchForm.paciente_nombre}
                  onChange={(e) => setDispatchForm({ ...dispatchForm, paciente_nombre: e.target.value })}
                  placeholder="Ej. Roberto Morales o Adulto mayor sin identificar"
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                  required
                />
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Dirección o Lugar de Emergencia
                </label>
                <input
                  type="text"
                  value={dispatchForm.direccion}
                  onChange={(e) => setDispatchForm({ ...dispatchForm, direccion: e.target.value })}
                  placeholder="Ej. Carretera a Santa Ana Km 64, Col. San Juan"
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                  required
                />
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Prioridad del Código
                </label>
                <select
                  value={dispatchForm.prioridad}
                  onChange={(e) => setDispatchForm({ ...dispatchForm, prioridad: e.target.value as any })}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                >
                  <option value="CRITICA">CRÍTICA (Código Rojo - Paro / Trauma Mayor)</option>
                  <option value="ALTA">ALTA (Hemorragia activa / Disnea severa)</option>
                  <option value="MEDIA">MEDIA (Traslado asistido urgente)</option>
                  <option value="BAJA">BAJA (Traslado programado interhospitalario)</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Descripción de la Situación
                </label>
                <textarea
                  value={dispatchForm.descripcion}
                  onChange={(e) => setDispatchForm({ ...dispatchForm, descripcion: e.target.value })}
                  placeholder="Ej. Accidente vial, paciente atrapado con politraumatismo..."
                  rows={3}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                  required
                />
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  onClick={() => setShowDispatchModal(false)}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    background: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: 'none',
                    background: '#0b5d4b',
                    color: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isSubmitting ? 'Despachando...' : 'Confirmar Salida'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL REPORTE DE RUTA */}
      {showReportModal && servicioActivo && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 50,
            padding: '16px',
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              maxWidth: '440px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
            }}
          >
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
              Emitir Reporte Clínico en Ruta
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.82rem', color: '#526b60' }}>
              Transmite el estado del paciente para que el servicio de emergencias prepare la recepción.
            </p>

            <form onSubmit={handleReportarSubmit} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Estado del Paciente
                </label>
                <select
                  value={reportForm.estado_paciente}
                  onChange={(e) => setReportForm({ ...reportForm, estado_paciente: e.target.value as any })}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                >
                  <option value="ESTABLE">ESTABLE (Signos vitales normales en traslado)</option>
                  <option value="DELICADO">DELICADO (Requiere monitoreo continuo)</option>
                  <option value="CRITICO">CRÍTICO (Alerta de reanimación inmediata)</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Minutos Estimados de Llegada al Hospital
                </label>
                <input
                  type="number"
                  min={0}
                  max={240}
                  value={reportForm.minutos_llegada}
                  onChange={(e) => setReportForm({ ...reportForm, minutos_llegada: parseInt(e.target.value, 10) || 0 })}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                />
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Observaciones Clínicas
                </label>
                <textarea
                  value={reportForm.observaciones}
                  onChange={(e) => setReportForm({ ...reportForm, observaciones: e.target.value })}
                  placeholder="Ej. Consciente, SatO2 96%, vía endovenosa permeable con Hartman..."
                  rows={3}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                />
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  onClick={() => setShowReportModal(false)}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    background: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: 'none',
                    background: '#0284c7',
                    color: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isSubmitting ? 'Transmitiendo...' : 'Transmitir Reporte'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL FINALIZAR SERVICIO */}
      {showFinalizeModal && servicioActivo && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 50,
            padding: '16px',
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              maxWidth: '440px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
            }}
          >
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
              Finalizar Servicio de Ambulancia
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.82rem', color: '#526b60' }}>
              La unidad médica retornará al estado <strong>DISPONIBLE</strong> en la rampa de emergencias.
            </p>

            <form onSubmit={handleFinalizarSubmit} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Notas Finales de Cierre de Traslado
                </label>
                <textarea
                  value={notasFinalizacion}
                  onChange={(e) => setNotasFinalizacion(e.target.value)}
                  placeholder="Ej. Paciente entregado a médico de guardia en Servicio de Emergencias sin novedades..."
                  rows={3}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                />
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  onClick={() => setShowFinalizeModal(false)}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    background: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  Volver
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: 'none',
                    background: '#0b5d4b',
                    color: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isSubmitting ? 'Finalizando...' : 'Confirmar Llegada y Cierre'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
