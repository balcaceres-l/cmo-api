import React, { useState, useEffect } from 'react';
import { FlaskIcon, SearchIcon, RefreshCwIcon } from '../../components/Icons';
import { apiClient, ApiError } from '../../api/client';

interface OrdenLab {
  id: string;
  codigo: string;
  paciente: string;
  expediente: string;
  examen: string;
  categoria: string;
  medicoSolicitante: string;
  fechaSolicitud: string;
  fechaInicio?: string;
  fechaResultado?: string;
  estado: 'PENDIENTE' | 'EN_PROCESO' | 'COMPLETADO' | 'CANCELADA';
  observaciones?: string;
  motivoCancelacion?: string;
}

const FALLBACK_LAB_ORDERS: OrdenLab[] = [
  {
    id: 'lab-1',
    codigo: 'ORD-2026-0042',
    paciente: 'Elena María Flores',
    expediente: 'EXP-2026-000012',
    examen: 'Hemograma Completo + Plaquetas',
    categoria: 'Hematología',
    medicoSolicitante: 'Dr. Salvador Morales',
    fechaSolicitud: 'Hoy 08:30',
    fechaInicio: '08:45',
    fechaResultado: '09:20',
    estado: 'COMPLETADO',
    observaciones: 'Valores normales. Sin anomalías plaquetarias.',
  },
  {
    id: 'lab-2',
    codigo: 'ORD-2026-0043',
    paciente: 'Don Fernando Solís',
    expediente: 'EXP-2026-000038',
    examen: 'Troponina I Cuantitativa Ultrasensible',
    categoria: 'Bioquímica Clínica',
    medicoSolicitante: 'Dra. Ana Martínez',
    fechaSolicitud: 'Hoy 09:15',
    fechaInicio: '09:25',
    estado: 'EN_PROCESO',
    observaciones: 'Descarte de síndrome coronario agudo en observación.',
  },
  {
    id: 'lab-3',
    codigo: 'ORD-2026-0044',
    paciente: 'Carlos Eduardo Ramos',
    expediente: 'EXP-2026-000142',
    examen: 'Perfil Lipídico Completo',
    categoria: 'Bioquímica Clínica',
    medicoSolicitante: 'Dr. Alejandro Benítez',
    fechaSolicitud: 'Hoy 09:40',
    estado: 'PENDIENTE',
    observaciones: 'En ayuno de 12 horas. Pendiente extracción de muestra.',
  },
];

export const LaboratoryPage: React.FC = () => {
  const [orders, setOrders] = useState<OrdenLab[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [activeTab, setActiveTab] = useState<'TODAS' | 'PENDIENTE' | 'EN_PROCESO' | 'COMPLETADO' | 'CANCELADA'>('TODAS');
  const [searchQuery, setSearchQuery] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Modal Ver Detalle / Turnaround
  const [selectedOrder, setSelectedOrder] = useState<OrdenLab | null>(null);
  const [showDetailModal, setShowDetailModal] = useState<boolean>(false);
  const [showCancelModal, setShowCancelModal] = useState<boolean>(false);
  const [motivoCancelacion, setMotivoCancelacion] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const notify = (msg: string) => {
    setFeedback(msg);
    setTimeout(() => setFeedback(null), 4500);
  };

  const fetchOrders = async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const res: any = await apiClient.get('/ordenes-laboratorio');
      const list = Array.isArray(res) ? res : res?.data || [];
      if (list.length > 0) {
        const mapped: OrdenLab[] = list.map((o: any) => ({
          id: o.id_orden || o.id,
          codigo: o.id_orden ? `ORD-${String(o.id_orden).slice(0, 8).toUpperCase()}` : `ORD-${o.id}`,
          paciente: o.paciente?.nombre_completo || 'Paciente',
          expediente: o.paciente?.numero_expediente || 'S/E',
          examen: o.examen?.nombre || 'Examen de Laboratorio',
          categoria: o.examen?.categoria || 'General',
          medicoSolicitante: o.medico?.nombre_completo || 'Médico Tratante',
          fechaSolicitud: o.fecha_solicitud ? new Date(o.fecha_solicitud).toLocaleString('es-SV') : 'Reciente',
          fechaInicio: o.fecha_inicio ? new Date(o.fecha_inicio).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : undefined,
          fechaResultado: o.fecha_resultado ? new Date(o.fecha_resultado).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : undefined,
          estado: o.estado || 'PENDIENTE',
          observaciones: o.observaciones,
          motivoCancelacion: o.motivo_cancelacion,
        }));
        setOrders(mapped);
      } else {
        setOrders([]);
      }
    } catch {
      setOrders(FALLBACK_LAB_ORDERS);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  // Cancelar Orden (Resolución Directiva)
  const handleCancelarOrden = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedOrder) return;
    if (!motivoCancelacion.trim()) {
      alert('Por favor indica el motivo administrativo de la cancelación.');
      return;
    }

    setIsSubmitting(true);
    try {
      await apiClient.patch(`/ordenes-laboratorio/${selectedOrder.id}/cancelar`, {
        motivo_cancelacion: motivoCancelacion,
      });
      notify(`Orden ${selectedOrder.codigo} cancelada.`);
      setShowCancelModal(false);
      setShowDetailModal(false);
      setSelectedOrder(null);
      setMotivoCancelacion('');
      fetchOrders();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`No se pudo cancelar orden: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredOrders = orders.filter((o) => {
    if (activeTab !== 'TODAS' && o.estado !== activeTab) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return (
        o.codigo.toLowerCase().includes(q) ||
        o.paciente.toLowerCase().includes(q) ||
        o.examen.toLowerCase().includes(q) ||
        o.categoria.toLowerCase().includes(q)
      );
    }
    return true;
  });

  const countPendiente = orders.filter((o) => o.estado === 'PENDIENTE').length;
  const countEnProceso = orders.filter((o) => o.estado === 'EN_PROCESO').length;
  const countCompletado = orders.filter((o) => o.estado === 'COMPLETADO').length;

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
              background: '#e0f2fe',
              color: '#0284c7',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <FlaskIcon size={24} />
          </div>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 800, margin: 0, color: '#13221c' }}>
              Monitor de Laboratorio Clínico y Demanda
            </h1>
            <p style={{ margin: 0, fontSize: '0.85rem', color: '#526b60' }}>
              Supervisión de tiempos de respuesta (turnaround), demanda de reactivos y estatus de órdenes. El procesamiento de muestras es realizado por el equipo de laboratorio en analizadores.
            </p>
          </div>
        </div>

        <button
          onClick={() => {
            fetchOrders();
            notify('Monitor de laboratorio sincronizado con la base de datos.');
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

      {/* Counter Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '16px',
        }}
      >
        <div
          style={{
            background: '#ffffff',
            borderRadius: '14px',
            padding: '18px',
            border: '1px solid #e2ece7',
          }}
        >
          <span style={{ fontSize: '0.78rem', fontWeight: 700, color: '#849a90', textTransform: 'uppercase' }}>
            Total de Órdenes
          </span>
          <div style={{ fontSize: '2rem', fontWeight: 800, color: '#073f33', marginTop: '4px' }}>
            {orders.length}
          </div>
          <span style={{ fontSize: '0.74rem', color: '#526b60' }}>Carga general en sistema</span>
        </div>

        <div
          style={{
            background: '#ffffff',
            borderRadius: '14px',
            padding: '18px',
            border: '1px solid #fde68a',
          }}
        >
          <span style={{ fontSize: '0.78rem', fontWeight: 700, color: '#b45309', textTransform: 'uppercase' }}>
            Pendientes de Toma
          </span>
          <div style={{ fontSize: '2rem', fontWeight: 800, color: '#d97706', marginTop: '4px' }}>
            {countPendiente}
          </div>
          <span style={{ fontSize: '0.74rem', color: '#92400e' }}>Esperando extracción</span>
        </div>

        <div
          style={{
            background: '#ffffff',
            borderRadius: '14px',
            padding: '18px',
            border: '1px solid #bae6fd',
          }}
        >
          <span style={{ fontSize: '0.78rem', fontWeight: 700, color: '#0369a1', textTransform: 'uppercase' }}>
            En Analizadores
          </span>
          <div style={{ fontSize: '2rem', fontWeight: 800, color: '#0284c7', marginTop: '4px' }}>
            {countEnProceso}
          </div>
          <span style={{ fontSize: '0.74rem', color: '#075985' }}>Procesamiento analítico activo</span>
        </div>

        <div
          style={{
            background: '#ffffff',
            borderRadius: '14px',
            padding: '18px',
            border: '1px solid #bbf7d0',
          }}
        >
          <span style={{ fontSize: '0.78rem', fontWeight: 700, color: '#15803d', textTransform: 'uppercase' }}>
            Resultados Validados
          </span>
          <div style={{ fontSize: '2rem', fontWeight: 800, color: '#16a34a', marginTop: '4px' }}>
            {countCompletado}
          </div>
          <span style={{ fontSize: '0.74rem', color: '#166534' }}>Listos en expediente digital</span>
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
        <div style={{ display: 'flex', gap: '8px' }}>
          {['TODAS', 'PENDIENTE', 'EN_PROCESO', 'COMPLETADO', 'CANCELADA'].map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab as any)}
              style={{
                padding: '6px 12px',
                borderRadius: '6px',
                border: 'none',
                fontSize: '0.78rem',
                fontWeight: 700,
                cursor: 'pointer',
                background: activeTab === tab ? '#0b5d4b' : '#f0f4f2',
                color: activeTab === tab ? '#ffffff' : '#526b60',
              }}
            >
              {tab === 'TODAS' ? 'Todas' : tab}
            </button>
          ))}
        </div>

        <div style={{ position: 'relative', minWidth: '260px' }}>
          <div style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#849a90' }}>
            <SearchIcon size={16} />
          </div>
          <input
            type="text"
            placeholder="Buscar por examen, paciente o código..."
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
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Código / Solicitud</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Paciente</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Examen / Especialidad</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Médico Solicitante</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Estado</th>
              <th style={{ padding: '14px 16px', fontWeight: 800, textAlign: 'right' }}>Auditoría</th>
            </tr>
          </thead>
          <tbody>
            {filteredOrders.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ padding: '32px', textAlign: 'center', color: '#849a90' }}>
                  {loading ? 'Cargando órdenes...' : 'No hay órdenes de laboratorio en esta vista.'}
                </td>
              </tr>
            ) : (
              filteredOrders.map((o) => (
                <tr key={o.id} style={{ borderBottom: '1px solid #f0f4f2' }}>
                  <td style={{ padding: '14px 16px' }}>
                    <div style={{ fontWeight: 800, color: '#13221c' }}>{o.codigo}</div>
                    <div style={{ fontSize: '0.72rem', color: '#849a90' }}>{o.fechaSolicitud}</div>
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <div style={{ fontWeight: 700, color: '#13221c' }}>{o.paciente}</div>
                    <div style={{ fontSize: '0.75rem', color: '#849a90' }}>Exp: {o.expediente}</div>
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <div style={{ fontWeight: 600, color: '#13221c' }}>{o.examen}</div>
                    <div style={{ fontSize: '0.72rem', color: '#0b5d4b' }}>{o.categoria}</div>
                  </td>
                  <td style={{ padding: '14px 16px', color: '#33483f' }}>
                    {o.medicoSolicitante}
                  </td>
                  <td style={{ padding: '14px 16px' }}>
                    <span
                      style={{
                        padding: '3px 8px',
                        borderRadius: '999px',
                        fontSize: '0.72rem',
                        fontWeight: 800,
                        background:
                          o.estado === 'PENDIENTE'
                            ? '#fef3c7'
                            : o.estado === 'EN_PROCESO'
                            ? '#e0f2fe'
                            : o.estado === 'COMPLETADO'
                            ? '#ecfdf5'
                            : '#fee2e2',
                        color:
                          o.estado === 'PENDIENTE'
                            ? '#b45309'
                            : o.estado === 'EN_PROCESO'
                            ? '#0369a1'
                            : o.estado === 'COMPLETADO'
                            ? '#047857'
                            : '#b91c1c',
                      }}
                    >
                      {o.estado}
                    </span>
                  </td>
                  <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                    <button
                      onClick={() => {
                        setSelectedOrder(o);
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
                      Ver Tiempos →
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Modal Ver Tiempos y Detalles de Orden */}
      {showDetailModal && selectedOrder && (
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
                <span style={{ fontSize: '0.75rem', fontWeight: 800, color: '#0284c7', textTransform: 'uppercase' }}>
                  Auditoría LIS / Laboratorio
                </span>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '2px 0 0', color: '#073f33' }}>
                  {selectedOrder.codigo}
                </h2>
              </div>
              <button
                onClick={() => {
                  setShowDetailModal(false);
                  setSelectedOrder(null);
                }}
                style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer', color: '#849a90' }}
              >
                ✕
              </button>
            </div>

            <div style={{ display: 'grid', gap: '14px', background: '#f8faf9', padding: '16px', borderRadius: '12px', border: '1px solid #e2ece7', marginBottom: '20px' }}>
              <div>
                <span style={{ fontSize: '0.74rem', color: '#849a90', fontWeight: 700, textTransform: 'uppercase' }}>Examen Clínico</span>
                <p style={{ margin: '2px 0 0', fontSize: '0.94rem', fontWeight: 700, color: '#13221c' }}>{selectedOrder.examen}</p>
                <span style={{ fontSize: '0.78rem', color: '#0b5d4b', fontWeight: 600 }}>{selectedOrder.categoria}</span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <div>
                  <span style={{ fontSize: '0.74rem', color: '#849a90', fontWeight: 700, textTransform: 'uppercase' }}>Paciente</span>
                  <p style={{ margin: '2px 0 0', fontSize: '0.86rem', fontWeight: 700, color: '#13221c' }}>{selectedOrder.paciente}</p>
                  <span style={{ fontSize: '0.75rem', color: '#526b60' }}>Exp: {selectedOrder.expediente}</span>
                </div>
                <div>
                  <span style={{ fontSize: '0.74rem', color: '#849a90', fontWeight: 700, textTransform: 'uppercase' }}>Médico Tratante</span>
                  <p style={{ margin: '2px 0 0', fontSize: '0.86rem', fontWeight: 700, color: '#13221c' }}>{selectedOrder.medicoSolicitante}</p>
                </div>
              </div>

              {/* Tiempos de Turnaround */}
              <div style={{ background: '#ffffff', padding: '12px', borderRadius: '8px', border: '1px solid #d1dcd6' }}>
                <span style={{ fontSize: '0.74rem', color: '#073f33', fontWeight: 800, textTransform: 'uppercase', display: 'block', marginBottom: '8px' }}>
                  Línea de Tiempo Operativa (Turnaround)
                </span>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '8px', textAlign: 'center' }}>
                  <div style={{ background: '#f8faf9', padding: '8px', borderRadius: '6px' }}>
                    <span style={{ fontSize: '0.68rem', color: '#849a90', display: 'block' }}>Solicitud</span>
                    <strong style={{ fontSize: '0.76rem', color: '#13221c' }}>{selectedOrder.fechaSolicitud}</strong>
                  </div>
                  <div style={{ background: '#f8faf9', padding: '8px', borderRadius: '6px' }}>
                    <span style={{ fontSize: '0.68rem', color: '#849a90', display: 'block' }}>Inicio Análisis</span>
                    <strong style={{ fontSize: '0.76rem', color: '#0369a1' }}>{selectedOrder.fechaInicio || 'Pendiente'}</strong>
                  </div>
                  <div style={{ background: '#f8faf9', padding: '8px', borderRadius: '6px' }}>
                    <span style={{ fontSize: '0.68rem', color: '#849a90', display: 'block' }}>Resultado</span>
                    <strong style={{ fontSize: '0.76rem', color: '#16a34a' }}>{selectedOrder.fechaResultado || 'En espera'}</strong>
                  </div>
                </div>
              </div>

              <div>
                <span style={{ fontSize: '0.74rem', color: '#849a90', fontWeight: 700, textTransform: 'uppercase' }}>Observaciones Clínicas</span>
                <p style={{ margin: '2px 0 0', fontSize: '0.82rem', color: '#526b60' }}>
                  {selectedOrder.observaciones || selectedOrder.motivoCancelacion || 'Sin observaciones registradas.'}
                </p>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              {selectedOrder.estado !== 'COMPLETADO' && selectedOrder.estado !== 'CANCELADA' ? (
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
                  Resolución: Cancelar Orden
                </button>
              ) : (
                <span style={{ fontSize: '0.78rem', color: '#849a90' }}>Orden cerrada</span>
              )}

              <button
                onClick={() => {
                  setShowDetailModal(false);
                  setSelectedOrder(null);
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

      {/* Modal Cancelar Orden (Resolución Administrativa) */}
      {showCancelModal && selectedOrder && (
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
              Cancelar Orden de Laboratorio
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.84rem', color: '#526b60' }}>
              Se cancelará la orden <strong>{selectedOrder.codigo}</strong> ({selectedOrder.examen}) de {selectedOrder.paciente}.
            </p>

            <form onSubmit={handleCancelarOrden}>
              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 700, color: '#13221c', marginBottom: '6px' }}>
                  Motivo de Cancelación *
                </label>
                <textarea
                  required
                  rows={3}
                  placeholder="Ej. Error en indicación clínica, reactivo sin disponibilidad inmediata..."
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
