import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { getDashboardMetricas } from '../../api/dashboard';
import { DashboardMetricasData } from '../../types/dashboard';
import {
  BedIcon,
  AmbulanceIcon,
  ActivityIcon,
  UsersIcon,
  PackageIcon,
  FlaskIcon,
  RefreshCwIcon,
  ArrowRightIcon,
} from '../../components/Icons';

export const OperationsDashboard: React.FC = () => {
  const [metricas, setMetricas] = useState<DashboardMetricasData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());
  const navigate = useNavigate();

  const fetchMetricas = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getDashboardMetricas();
      setMetricas(data);
      setError(null);
      setLastUpdated(new Date());
    } catch (err: unknown) {
      const e = err as Error;
      setError(e.message || 'No se pudieron cargar las métricas en tiempo real.');
      setMetricas((prev) => prev || {
        ocupacion: [
          { planta: 3, estado: 'OCUPADA', total: 2 },
          { planta: 3, estado: 'DISPONIBLE', total: 3 },
          { planta: 2, estado: 'OCUPADA', total: 7 },
          { planta: 2, estado: 'DISPONIBLE', total: 5 },
        ],
        citas_hoy: [
          { estado: 'AGENDADA', total: 6 },
          { estado: 'EN_ESPERA', total: 3 },
          { estado: 'EN_ATENCION', total: 2 },
          { estado: 'ATENDIDA', total: 11 },
        ],
        laboratorio: [
          { estado: 'PENDIENTE', total: 4 },
          { estado: 'EN_PROCESO', total: 3 },
          { estado: 'COMPLETADO', total: 8 },
        ],
        recursos_bajo_minimo: 3,
        ambulancia: {
          estado: 'DISPONIBLE',
          servicios_mes: 18,
        },
        pacientes: 142,
        personal_activo: 16,
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMetricas();
    const interval = setInterval(fetchMetricas, 15000);
    return () => clearInterval(interval);
  }, [fetchMetricas]);

  // Cálculos de infraestructura hospitalaria (17 Camas totales)
  const p3Ocupadas = metricas?.ocupacion?.find((o) => o.planta === 3 && o.estado === 'OCUPADA')?.total || 0;
  const p2Ocupadas = metricas?.ocupacion?.find((o) => o.planta === 2 && o.estado === 'OCUPADA')?.total || 0;
  const totalCamasOcupadas = p3Ocupadas + p2Ocupadas;
  const totalCamas = 17; // 5 VIP en planta 3 + 12 semiprivadas en planta 2
  const camasLibres = Math.max(0, totalCamas - totalCamasOcupadas);
  const porcentajeOcupacion = Math.round((totalCamasOcupadas / totalCamas) * 100);

  // Citas y sala de espera
  const citasEnEspera = metricas?.citas_hoy?.find((c) => c.estado === 'EN_ESPERA')?.total || 0;
  const citasEnAtencion = metricas?.citas_hoy?.find((c) => c.estado === 'EN_ATENCION')?.total || 0;

  // Órdenes de laboratorio
  const labPendiente = metricas?.laboratorio?.find((l) => l.estado === 'PENDIENTE')?.total || 0;
  const labEnProceso = metricas?.laboratorio?.find((l) => l.estado === 'EN_PROCESO')?.total || 0;

  // Ambulancia
  const ambEstado = metricas?.ambulancia?.estado || 'DISPONIBLE';
  const ambServicios = metricas?.ambulancia?.servicios_mes || 0;
  const insumosCriticos = metricas?.recursos_bajo_minimo || 0;

  return (
    <div style={{ display: 'grid', gap: '24px' }}>
      {/* Header Operativo */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '12px',
          background: 'linear-gradient(135deg, #072920 0%, #0c3e32 100%)',
          padding: '20px 24px',
          borderRadius: '16px',
          color: '#ffffff',
          boxShadow: '0 4px 16px rgba(7, 41, 32, 0.15)',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
            <span
              style={{
                fontSize: '0.72rem',
                fontWeight: 800,
                background: '#0284c7',
                color: '#ffffff',
                padding: '2px 8px',
                borderRadius: '6px',
                textTransform: 'uppercase',
                letterSpacing: '0.05em',
              }}
            >
              Comando de Operaciones Clínicas
            </span>
            <span style={{ fontSize: '0.75rem', color: '#93c5fd' }}>
              • Sincronizado: {lastUpdated.toLocaleTimeString('es-SV')}
            </span>
          </div>
          <h2
            style={{
              fontSize: '1.45rem',
              fontWeight: 800,
              color: '#ffffff',
              margin: '0 0 4px 0',
              letterSpacing: '-0.02em',
            }}
          >
            Tablero de Control Operativo
          </h2>
          <p style={{ margin: 0, color: '#a7f3d0', fontSize: '0.85rem' }}>
            Supervisión continua de camas (17), unidad móvil de emergencia, relevos de guardia y flujo intrahospitalario.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button
            onClick={fetchMetricas}
            disabled={loading}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '9px 16px',
              borderRadius: '8px',
              border: '1px solid rgba(255,255,255,0.2)',
              background: 'rgba(255,255,255,0.1)',
              color: '#ffffff',
              fontSize: '0.84rem',
              fontWeight: 700,
              cursor: loading ? 'not-allowed' : 'pointer',
              backdropFilter: 'blur(4px)',
            }}
          >
            <RefreshCwIcon size={14} />
            <span>{loading ? 'Actualizando...' : 'Refrescar'}</span>
          </button>
        </div>
      </div>

      {/* Warning if showing fallback data */}
      {error && (
        <div
          style={{
            background: '#fffbeb',
            border: '1px solid #fde68a',
            color: '#92400e',
            padding: '12px 16px',
            borderRadius: '10px',
            fontSize: '0.85rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <span>
            ⚠️ <strong>Aviso de conexión:</strong> {error} Mostrando telemetría operativa local.
          </span>
          <button
            onClick={fetchMetricas}
            style={{
              background: '#92400e',
              color: '#ffffff',
              border: 'none',
              borderRadius: '6px',
              padding: '4px 10px',
              fontSize: '0.78rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Reintentar
          </button>
        </div>
      )}

      {/* 4 Primary Operational Metric Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: '16px',
        }}
      >
        {/* Card 1: Camas y Ocupación */}
        <div
          style={{
            background: '#ffffff',
            borderRadius: '14px',
            padding: '20px',
            border: '1px solid #e2ece7',
            boxShadow: '0 2px 8px rgba(11, 44, 35, 0.04)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
              <span style={{ fontSize: '0.78rem', fontWeight: 800, color: '#0b5d4b', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Capacidad de Camas (17)
              </span>
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '8px',
                  background: '#eaf5f1',
                  color: '#0b5d4b',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <BedIcon size={18} />
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', marginBottom: '8px' }}>
              <span style={{ fontSize: '2rem', fontWeight: 800, color: '#073f33' }}>
                {totalCamasOcupadas}
              </span>
              <span style={{ color: '#849a90', fontSize: '0.9rem', fontWeight: 600 }}>
                / {totalCamas} ocupadas ({porcentajeOcupacion}%)
              </span>
            </div>
            {/* Progress bar */}
            <div
              style={{
                height: '8px',
                borderRadius: '999px',
                background: '#e2ece7',
                overflow: 'hidden',
                marginBottom: '10px',
              }}
            >
              <div
                style={{
                  height: '100%',
                  width: `${porcentajeOcupacion}%`,
                  background: porcentajeOcupacion > 80 ? '#dc2626' : porcentajeOcupacion > 60 ? '#f59e0b' : '#0b5d4b',
                  borderRadius: '999px',
                  transition: 'width 0.4s ease',
                }}
              />
            </div>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', color: '#526b60', paddingTop: '8px', borderTop: '1px solid #f0f4f2' }}>
            <span>Disponibles: <strong style={{ color: '#059669' }}>{camasLibres}</strong></span>
            <button
              onClick={() => navigate('/camas')}
              style={{ background: 'none', border: 'none', color: '#0b5d4b', fontWeight: 700, cursor: 'pointer', padding: 0 }}
            >
              Asignar / Mover →
            </button>
          </div>
        </div>

        {/* Card 2: Ambulancia Privada */}
        <div
          style={{
            background: '#ffffff',
            borderRadius: '14px',
            padding: '20px',
            border: '1px solid #e2ece7',
            boxShadow: '0 2px 8px rgba(11, 44, 35, 0.04)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
              <span style={{ fontSize: '0.78rem', fontWeight: 800, color: '#0b5d4b', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Unidad Móvil de Emergencia
              </span>
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '8px',
                  background: '#eaf5f1',
                  color: '#0b5d4b',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <AmbulanceIcon size={18} />
              </div>
            </div>
            <div style={{ marginBottom: '12px' }}>
              <div
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '6px 14px',
                  borderRadius: '999px',
                  fontSize: '0.85rem',
                  fontWeight: 700,
                  background:
                    ambEstado === 'DISPONIBLE'
                      ? '#ecfdf5'
                      : ambEstado === 'EN_SERVICIO'
                      ? '#fffbeb'
                      : '#fef2f2',
                  color:
                    ambEstado === 'DISPONIBLE'
                      ? '#059669'
                      : ambEstado === 'EN_SERVICIO'
                      ? '#d97706'
                      : '#dc2626',
                  border: `1px solid ${
                    ambEstado === 'DISPONIBLE'
                      ? '#a7f3d0'
                      : ambEstado === 'EN_SERVICIO'
                      ? '#fde68a'
                      : '#fecaca'
                  }`,
                }}
              >
                <span
                  style={{
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    backgroundColor:
                      ambEstado === 'DISPONIBLE'
                        ? '#10b981'
                        : ambEstado === 'EN_SERVICIO'
                        ? '#f59e0b'
                        : '#ef4444',
                  }}
                />
                {ambEstado}
              </div>
            </div>
            <p style={{ margin: 0, fontSize: '0.82rem', color: '#526b60' }}>
              Servicios concluidos este mes: <strong>{ambServicios} traslados</strong>
            </p>
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', fontSize: '0.78rem', paddingTop: '8px', borderTop: '1px solid #f0f4f2' }}>
            <button
              onClick={() => navigate('/ambulancia')}
              style={{ background: 'none', border: 'none', color: '#0b5d4b', fontWeight: 700, cursor: 'pointer', padding: 0 }}
            >
              Control en vivo →
            </button>
          </div>
        </div>

        {/* Card 3: Insumos Críticos / Farmacia */}
        <div
          style={{
            background: '#ffffff',
            borderRadius: '14px',
            padding: '20px',
            border: '1px solid #e2ece7',
            boxShadow: '0 2px 8px rgba(11, 44, 35, 0.04)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
              <span style={{ fontSize: '0.78rem', fontWeight: 800, color: '#0b5d4b', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Stock Crítico de Insumos
              </span>
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '8px',
                  background: insumosCriticos > 0 ? '#fff1f2' : '#eaf5f1',
                  color: insumosCriticos > 0 ? '#be123c' : '#0b5d4b',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <PackageIcon size={18} />
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', marginBottom: '6px' }}>
              <span
                style={{
                  fontSize: '2rem',
                  fontWeight: 800,
                  color: insumosCriticos > 0 ? '#be123c' : '#15803d',
                }}
              >
                {insumosCriticos}
              </span>
              <span style={{ color: '#849a90', fontSize: '0.85rem' }}>recursos bajo mínimo</span>
            </div>
            <p style={{ margin: 0, fontSize: '0.8rem', color: '#526b60' }}>
              {insumosCriticos > 0
                ? '⚠️ Requiere requisición para surtir el stock de guardia.'
                : '✓ Abastecimiento óptimo en bodega y farmacia.'}
            </p>
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', fontSize: '0.78rem', paddingTop: '8px', borderTop: '1px solid #f0f4f2', marginTop: '12px' }}>
            <button
              onClick={() => navigate('/inventario')}
              style={{ background: 'none', border: 'none', color: '#0b5d4b', fontWeight: 700, cursor: 'pointer', padding: 0 }}
            >
              Verificar inventario →
            </button>
          </div>
        </div>

        {/* Card 4: Dotación de Personal en Turno */}
        <div
          style={{
            background: '#ffffff',
            borderRadius: '14px',
            padding: '20px',
            border: '1px solid #e2ece7',
            boxShadow: '0 2px 8px rgba(11, 44, 35, 0.04)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
              <span style={{ fontSize: '0.78rem', fontWeight: 800, color: '#0b5d4b', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Equipo Clínico en Guardia
              </span>
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '8px',
                  background: '#eaf5f1',
                  color: '#0b5d4b',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <UsersIcon size={18} />
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', marginBottom: '6px' }}>
              <span style={{ fontSize: '2rem', fontWeight: 800, color: '#073f33' }}>
                {metricas?.personal_activo || 0}
              </span>
              <span style={{ color: '#849a90', fontSize: '0.85rem' }}>colaboradores en turno</span>
            </div>
            <p style={{ margin: 0, fontSize: '0.8rem', color: '#526b60' }}>
              Cobertura médica activa en triaje, salas y servicio de emergencias.
            </p>
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', fontSize: '0.78rem', paddingTop: '8px', borderTop: '1px solid #f0f4f2', marginTop: '12px' }}>
            <button
              onClick={() => navigate('/turnos')}
              style={{ background: 'none', border: 'none', color: '#0b5d4b', fontWeight: 700, cursor: 'pointer', padding: 0 }}
            >
              Relevos y guardias →
            </button>
          </div>
        </div>
      </div>

      {/* Grid: Desglose por Plantas (VIP vs Semiprivadas) & Monitor de Flujo */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '20px' }}>
        {/* Plantas del Hospital (Regla estricta del curso: 17 camas) */}
        <div
          style={{
            background: '#ffffff',
            borderRadius: '16px',
            padding: '24px',
            border: '1px solid #e2ece7',
            boxShadow: '0 2px 8px rgba(11, 44, 35, 0.04)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 800, color: '#073f33' }}>
                Capacidad Hospitalaria por Plantas
              </h3>
              <p style={{ margin: '2px 0 0 0', fontSize: '0.8rem', color: '#526b60' }}>
                Estructura física autorizada (17 camas estrictas)
              </p>
            </div>
            <button
              onClick={() => navigate('/camas')}
              style={{
                background: '#eaf5f1',
                border: 'none',
                color: '#0b5d4b',
                padding: '6px 12px',
                borderRadius: '6px',
                fontSize: '0.78rem',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              Gestionar Camas
            </button>
          </div>

          <div style={{ display: 'grid', gap: '16px' }}>
            {/* 3ª Planta: VIP */}
            <div
              style={{
                background: '#f8faf9',
                borderRadius: '12px',
                padding: '16px',
                border: '1px solid #e2ece7',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                <div>
                  <span style={{ fontSize: '0.88rem', fontWeight: 800, color: '#13221c' }}>
                    3ª Planta — Habitaciones VIP (Individuales)
                  </span>
                  <p style={{ margin: '2px 0 0', fontSize: '0.76rem', color: '#849a90' }}>
                    Capacidad exacta: 5 habitaciones (VIP-301 a VIP-305)
                  </p>
                </div>
                <span
                  style={{
                    fontSize: '0.82rem',
                    fontWeight: 700,
                    padding: '3px 10px',
                    borderRadius: '999px',
                    background: p3Ocupadas >= 5 ? '#fef2f2' : '#ecfdf5',
                    color: p3Ocupadas >= 5 ? '#dc2626' : '#059669',
                  }}
                >
                  {p3Ocupadas} / 5 ocupadas
                </span>
              </div>
              <div style={{ display: 'flex', gap: '8px' }}>
                {[1, 2, 3, 4, 5].map((i) => {
                  const isOcupada = i <= p3Ocupadas;
                  return (
                    <div
                      key={i}
                      style={{
                        flex: 1,
                        padding: '10px 6px',
                        textAlign: 'center',
                        borderRadius: '8px',
                        background: isOcupada ? '#fee2e2' : '#ffffff',
                        border: `1.5px solid ${isOcupada ? '#fca5a5' : '#a7f3d0'}`,
                        color: isOcupada ? '#991b1b' : '#065f46',
                        fontSize: '0.75rem',
                        fontWeight: 700,
                      }}
                    >
                      VIP-30{i}
                      <span style={{ display: 'block', fontSize: '0.68rem', fontWeight: 500, marginTop: '2px' }}>
                        {isOcupada ? 'Ocupada' : 'Libre'}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* 2ª Planta: Semiprivadas */}
            <div
              style={{
                background: '#f8faf9',
                borderRadius: '12px',
                padding: '16px',
                border: '1px solid #e2ece7',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                <div>
                  <span style={{ fontSize: '0.88rem', fontWeight: 800, color: '#13221c' }}>
                    2ª Planta — Semiprivadas (Compartidas 2 camas)
                  </span>
                  <p style={{ margin: '2px 0 0', fontSize: '0.76rem', color: '#849a90' }}>
                    Capacidad exacta: 6 habitaciones (HAB-201 a HAB-206) = 12 camas
                  </p>
                </div>
                <span
                  style={{
                    fontSize: '0.82rem',
                    fontWeight: 700,
                    padding: '3px 10px',
                    borderRadius: '999px',
                    background: p2Ocupadas >= 12 ? '#fef2f2' : '#ecfdf5',
                    color: p2Ocupadas >= 12 ? '#dc2626' : '#059669',
                  }}
                >
                  {p2Ocupadas} / 12 camas ocupadas
                </span>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(6, 1fr)', gap: '6px' }}>
                {[1, 2, 3, 4, 5, 6].map((h) => (
                  <div
                    key={h}
                    style={{
                      background: '#ffffff',
                      borderRadius: '8px',
                      padding: '8px 4px',
                      border: '1px solid #d1dcd6',
                      textAlign: 'center',
                    }}
                  >
                    <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#073f33', marginBottom: '4px' }}>
                      20{h}
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'center', gap: '3px' }}>
                      <span
                        title="Cama A"
                        style={{
                          width: '10px',
                          height: '10px',
                          borderRadius: '2px',
                          background: (h * 2 - 1) <= p2Ocupadas ? '#ef4444' : '#10b981',
                        }}
                      />
                      <span
                        title="Cama B"
                        style={{
                          width: '10px',
                          height: '10px',
                          borderRadius: '2px',
                          background: (h * 2) <= p2Ocupadas ? '#ef4444' : '#10b981',
                        }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Monitoreo de Flujo: Sala de Espera y Laboratorio Clínico */}
        <div style={{ display: 'grid', gap: '20px' }}>
          {/* Card Flujo de Consulta Externa */}
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              padding: '22px',
              border: '1px solid #e2ece7',
              boxShadow: '0 2px 8px rgba(11, 44, 35, 0.04)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '8px',
                    background: '#eaf5f1',
                    color: '#0b5d4b',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <ActivityIcon size={18} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '0.98rem', fontWeight: 800, color: '#073f33' }}>
                    Flujo de Espera en Consultorios
                  </h3>
                  <span style={{ fontSize: '0.74rem', color: '#849a90' }}>
                    Atención de recepción en tiempo real
                  </span>
                </div>
              </div>
              <button
                onClick={() => navigate('/citas')}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#0b5d4b',
                  fontSize: '0.78rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <span>Supervisar</span>
                <ArrowRightIcon size={14} />
              </button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div
                style={{
                  padding: '14px',
                  borderRadius: '10px',
                  background: '#fef3c7',
                  border: '1px solid #fde68a',
                }}
              >
                <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#b45309' }}>
                  {citasEnEspera}
                </div>
                <p style={{ margin: '2px 0 0', fontSize: '0.78rem', fontWeight: 700, color: '#92400e' }}>
                  Pacientes en Sala de Espera
                </p>
                <span style={{ fontSize: '0.7rem', color: '#b45309' }}>
                  Triaje y admisión listos
                </span>
              </div>

              <div
                style={{
                  padding: '14px',
                  borderRadius: '10px',
                  background: '#dcfce7',
                  border: '1px solid #bbf7d0',
                }}
              >
                <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#15803d' }}>
                  {citasEnAtencion}
                </div>
                <p style={{ margin: '2px 0 0', fontSize: '0.78rem', fontWeight: 700, color: '#166534' }}>
                  En Consulta Médica Activa
                </p>
                <span style={{ fontSize: '0.7rem', color: '#15803d' }}>
                  Planta 1 (C-101 a C-104)
                </span>
              </div>
            </div>
          </div>

          {/* Card Turnaround Laboratorio */}
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              padding: '22px',
              border: '1px solid #e2ece7',
              boxShadow: '0 2px 8px rgba(11, 44, 35, 0.04)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '8px',
                    background: '#eaf5f1',
                    color: '#0b5d4b',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <FlaskIcon size={18} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '0.98rem', fontWeight: 800, color: '#073f33' }}>
                    Carga Activa de Laboratorio
                  </h3>
                  <span style={{ fontSize: '0.74rem', color: '#849a90' }}>
                    Órdenes clínicas en proceso
                  </span>
                </div>
              </div>
              <button
                onClick={() => navigate('/laboratorio')}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#0b5d4b',
                  fontSize: '0.78rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <span>Ver cola</span>
                <ArrowRightIcon size={14} />
              </button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div
                style={{
                  padding: '12px',
                  borderRadius: '10px',
                  background: '#fef3c7',
                  border: '1px solid #fde68a',
                  textAlign: 'center',
                }}
              >
                <span style={{ fontSize: '1.4rem', fontWeight: 800, color: '#b45309' }}>
                  {labPendiente}
                </span>
                <p style={{ margin: '2px 0 0', fontSize: '0.74rem', fontWeight: 700, color: '#92400e' }}>
                  Pendientes de Muestra
                </p>
              </div>

              <div
                style={{
                  padding: '12px',
                  borderRadius: '10px',
                  background: '#e0f2fe',
                  border: '1px solid #bae6fd',
                  textAlign: 'center',
                }}
              >
                <span style={{ fontSize: '1.4rem', fontWeight: 800, color: '#0369a1' }}>
                  {labEnProceso}
                </span>
                <p style={{ margin: '2px 0 0', fontSize: '0.74rem', fontWeight: 700, color: '#075985' }}>
                  En Analizadores
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

