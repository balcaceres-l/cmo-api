import React from 'react';
import { BedData, formatStayDuration, getStayHours } from '../types';
import { LockIcon, PlusCircleIcon } from '../../../components/Icons';

interface BedDetailDrawerProps {
  bed: BedData | null;
  isOps: boolean;
  onClose: () => void;
  onOpenIngreso: (bed: BedData) => void;
  onOpenTraslado: (bed: BedData) => void;
  onOpenAlta: (bed: BedData) => void;
}

export const BedDetailDrawer: React.FC<BedDetailDrawerProps> = ({
  bed,
  isOps,
  onClose,
  onOpenIngreso,
  onOpenTraslado,
  onOpenAlta,
}) => {
  if (!bed) return null;

  const isOcupada = bed.estado === 'OCUPADA';
  const stayHours = isOcupada ? getStayHours(bed.paciente?.fechaIngresoIso) : 0;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(15, 23, 42, 0.38)',
        zIndex: 45,
        display: 'flex',
        justifyContent: 'flex-end',
      }}
      onClick={onClose}
    >
      <aside
        onClick={(e) => e.stopPropagation()}
        style={{
          width: '100%',
          maxWidth: '410px',
          height: '100%',
          background: '#ffffff',
          boxShadow: '-8px 0 30px rgba(0, 0, 0, 0.14)',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          padding: '22px',
          overflowY: 'auto',
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* Cabecera */}
          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '12px' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span
                  style={{
                    fontFamily: 'monospace',
                    fontSize: '1.05rem',
                    fontWeight: 800,
                    padding: '4px 10px',
                    borderRadius: '8px',
                    background: isOcupada ? '#fef3c7' : '#e6f4ea',
                    color: isOcupada ? '#92400e' : '#0b5d4b',
                  }}
                >
                  {bed.codigo}
                </span>
                <span
                  style={{
                    fontSize: '0.74rem',
                    fontWeight: 800,
                    padding: '3px 9px',
                    borderRadius: '999px',
                    background: isOcupada ? '#fef3c7' : '#ecfdf5',
                    color: isOcupada ? '#b45309' : '#047857',
                  }}
                >
                  {isOcupada ? '● OCUPADA' : '○ DISPONIBLE'}
                </span>
              </div>
              <p style={{ margin: '6px 0 0', fontSize: '0.82rem', color: '#526b60' }}>
                Planta {bed.planta} · Habitación {bed.habitacion} · {bed.tipo}
              </p>
            </div>

            <button
              type="button"
              onClick={onClose}
              style={{
                border: '1px solid #cbd5e1',
                background: '#ffffff',
                borderRadius: '8px',
                padding: '6px 10px',
                cursor: 'pointer',
                fontWeight: 700,
                color: '#475569',
                fontSize: '0.8rem',
              }}
            >
              Cerrar
            </button>
          </div>

          {/* Contenido */}
          {isOcupada && bed.paciente ? (
            <div
              style={{
                background: '#f8faf9',
                border: '1px solid #e2ece7',
                borderRadius: '12px',
                padding: '16px',
                display: 'flex',
                flexDirection: 'column',
                gap: '12px',
              }}
            >
              <div>
                <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#849a90', textTransform: 'uppercase' }}>
                  Paciente Hospitalizado
                </div>
                <div style={{ fontSize: '1.05rem', fontWeight: 800, color: '#13221c', marginTop: '2px' }}>
                  {bed.paciente.nombre}
                </div>
                <div style={{ fontSize: '0.78rem', color: '#526b60', marginTop: '2px' }}>
                  Exp: {bed.paciente.expediente}
                  {bed.paciente.dui ? ` · DUI: ${bed.paciente.dui}` : ''}
                </div>
              </div>

              <div style={{ borderTop: '1px solid #e2ece7', paddingTop: '10px', display: 'grid', gap: '8px' }}>
                <div>
                  <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#849a90', display: 'block' }}>
                    Motivo / Diagnóstico
                  </span>
                  <span style={{ fontSize: '0.84rem', color: '#13221c' }}>
                    {bed.paciente.diagnostico || 'Observación clínica'}
                  </span>
                </div>

                <div>
                  <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#849a90', display: 'block' }}>
                    Médico Tratante
                  </span>
                  <span style={{ fontSize: '0.84rem', fontWeight: 700, color: '#0b5d4b' }}>
                    {bed.paciente.medico || 'Médico de Turno'}
                  </span>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                  <div>
                    <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#849a90', display: 'block' }}>
                      Fecha de Ingreso
                    </span>
                    <span style={{ fontSize: '0.8rem', color: '#13221c' }}>
                      {bed.paciente.fechaIngreso || 'Reciente'}
                    </span>
                  </div>
                  <div>
                    <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#849a90', display: 'block' }}>
                      Tiempo de Estancia
                    </span>
                    <span
                      style={{
                        fontSize: '0.82rem',
                        fontWeight: 800,
                        color: stayHours >= 48 ? '#b45309' : '#0369a1',
                      }}
                    >
                      {formatStayDuration(bed.paciente.fechaIngresoIso)}
                    </span>
                  </div>
                </div>

                {bed.paciente.registradoPor && (
                  <div>
                    <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#849a90', display: 'block' }}>
                      Registrado por
                    </span>
                    <span style={{ fontSize: '0.8rem', color: '#475569' }}>
                      {bed.paciente.registradoPor}
                    </span>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div
              style={{
                background: '#f0fdf4',
                border: '1px solid #bbf7d0',
                borderRadius: '12px',
                padding: '16px',
                fontSize: '0.85rem',
                color: '#166534',
              }}
            >
              <strong>Cama lista para ingreso inmediato.</strong>
              <p style={{ margin: '6px 0 0', fontSize: '0.8rem', color: '#15803d' }}>
                Sin paciente activo ni bloqueo de mantenimiento.
              </p>
            </div>
          )}
        </div>

        {/* Pie de acciones según rol */}
        <div style={{ borderTop: '1px solid #e2ece7', paddingTop: '16px', marginTop: '20px' }}>
          {isOps ? (
            isOcupada ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <div style={{ fontSize: '0.74rem', color: '#526b60' }}>
                  Intervención operativa (resolución de conflictos / contingencias):
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <button
                    type="button"
                    onClick={() => onOpenTraslado(bed)}
                    style={{
                      flex: 1,
                      padding: '10px 12px',
                      borderRadius: '8px',
                      border: '1px solid #0369a1',
                      background: '#e0f2fe',
                      color: '#0369a1',
                      fontWeight: 700,
                      fontSize: '0.82rem',
                      cursor: 'pointer',
                    }}
                  >
                    Trasladar Cama
                  </button>
                  <button
                    type="button"
                    onClick={() => onOpenAlta(bed)}
                    style={{
                      flex: 1,
                      padding: '10px 12px',
                      borderRadius: '8px',
                      border: 'none',
                      background: '#0b5d4b',
                      color: '#ffffff',
                      fontWeight: 700,
                      fontSize: '0.82rem',
                      cursor: 'pointer',
                    }}
                  >
                    Registrar Alta
                  </button>
                </div>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => onOpenIngreso(bed)}
                style={{
                  width: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  padding: '11px 14px',
                  borderRadius: '8px',
                  border: 'none',
                  background: '#0b5d4b',
                  color: '#ffffff',
                  fontWeight: 700,
                  fontSize: '0.85rem',
                  cursor: 'pointer',
                }}
              >
                <PlusCircleIcon size={16} /> Asignar Ingreso en {bed.codigo}
              </button>
            )
          ) : (
            <div
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '10px',
                padding: '12px',
                borderRadius: '10px',
                background: '#fef3c7',
                border: '1px solid #fde68a',
                color: '#92400e',
                fontSize: '0.78rem',
              }}
            >
              <LockIcon size={16} />
              <div>
                <strong>Supervisión Ejecutiva (Solo Lectura)</strong>
                <div style={{ marginTop: '2px' }}>
                  Los ingresos, traslados y altas corresponden al Jefe de Operaciones o Recepción.
                </div>
              </div>
            </div>
          )}
        </div>
      </aside>
    </div>
  );
};
