import React from 'react';
import { BedData, IngresoRecord, getStayHours } from '../types';

interface BedsKpiHeaderProps {
  isOps: boolean;
  beds: BedData[];
  ingresosHistory: IngresoRecord[];
  halfOccupiedRoomsCount: number;
}

export const BedsKpiHeader: React.FC<BedsKpiHeaderProps> = ({
  isOps,
  beds,
  ingresosHistory,
  halfOccupiedRoomsCount,
}) => {
  const totalCamas = beds.length || 17;
  const ocupadas = beds.filter((b) => b.estado === 'OCUPADA').length;
  const disponibles = totalCamas - ocupadas;
  const pctOcupacion = totalCamas > 0 ? Math.round((ocupadas / totalCamas) * 100) : 0;

  const p3Total = beds.filter((b) => b.planta === 3).length || 5;
  const p2Total = beds.filter((b) => b.planta === 2).length || 12;
  const p3Ocupadas = beds.filter((b) => b.planta === 3 && b.estado === 'OCUPADA').length;
  const p2Ocupadas = beds.filter((b) => b.planta === 2 && b.estado === 'OCUPADA').length;

  const activeHours = beds
    .filter((b) => b.estado === 'OCUPADA' && b.paciente?.fechaIngresoIso)
    .map((b) => getStayHours(b.paciente?.fechaIngresoIso));

  const avgHours =
    activeHours.length > 0
      ? Math.round((activeHours.reduce((acc, h) => acc + h, 0) / activeHours.length) * 10) / 10
      : 0;
  const longStaysCount = activeHours.filter((h) => h >= 48).length;

  const altasCount = ingresosHistory.filter((i) => !!i.fecha_alta).length;

  const cardStyle: React.CSSProperties = {
    background: '#ffffff',
    padding: '16px 18px',
    borderRadius: '12px',
    border: '1px solid #e2ece7',
  };

  if (isOps) {
    const saturacionAlta = pctOcupacion >= 80;
    return (
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))',
          gap: '14px',
        }}
      >
        <div style={cardStyle}>
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#0369a1', textTransform: 'uppercase' }}>
            Ocupación en Tiempo Real
          </span>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', marginTop: '4px' }}>
            <span style={{ fontSize: '1.75rem', fontWeight: 800, color: saturacionAlta ? '#b45309' : '#13221c' }}>
              {ocupadas} / {totalCamas}
            </span>
            <span
              style={{
                fontSize: '0.78rem',
                fontWeight: 700,
                padding: '2px 8px',
                borderRadius: '999px',
                background: saturacionAlta ? '#fef3c7' : '#e0f2fe',
                color: saturacionAlta ? '#b45309' : '#0369a1',
              }}
            >
              {pctOcupacion}%
            </span>
          </div>
          <span style={{ fontSize: '0.75rem', color: '#526b60' }}>
            {saturacionAlta ? 'Saturación elevada — vigilar altas' : 'Capacidad operativa estable'}
          </span>
        </div>

        <div style={cardStyle}>
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#047857', textTransform: 'uppercase' }}>
            Camas Disponibles
          </span>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#0b5d4b', marginTop: '4px' }}>
            {disponibles} Libres
          </div>
          <span style={{ fontSize: '0.75rem', color: '#526b60' }}>
            P3 VIP: {p3Total - p3Ocupadas}/{p3Total} · P2 Semi: {p2Total - p2Ocupadas}/{p2Total}
          </span>
        </div>

        <div style={cardStyle}>
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#526b60', textTransform: 'uppercase' }}>
            Estancia Promedio Activa
          </span>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#13221c', marginTop: '4px' }}>
            {activeHours.length > 0 ? `${avgHours} h` : '—'}
          </div>
          <span style={{ fontSize: '0.75rem', color: '#849a90' }}>
            {activeHours.length > 0 ? `Sobre ${activeHours.length} pacientes ingresados` : 'Sin pacientes activos'}
          </span>
        </div>

        <div style={cardStyle}>
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#b45309', textTransform: 'uppercase' }}>
            Focos Operativos
          </span>
          <div style={{ fontSize: '1.15rem', fontWeight: 800, color: longStaysCount > 0 ? '#b45309' : '#0369a1', marginTop: '6px' }}>
            {longStaysCount} estancias ≥48h · {halfOccupiedRoomsCount} hab. 1/2
          </div>
          <span style={{ fontSize: '0.75rem', color: '#849a90' }}>
            Detección de cuellos de botella y traslados
          </span>
        </div>
      </div>
    );
  }

  // Administrador General — Enfoque ejecutivo, capacidad legal y auditoría
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))',
        gap: '14px',
      }}
    >
      <div style={cardStyle}>
        <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#92400e', textTransform: 'uppercase' }}>
          Aforo Máximo Legal
        </span>
        <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#13221c', marginTop: '4px' }}>
          {totalCamas} Camas
        </div>
        <span style={{ fontSize: '0.75rem', color: '#849a90' }}>
          Planta 3 ({p3Total} VIP) + Planta 2 ({p2Total} Semi)
        </span>
      </div>

      <div style={cardStyle}>
        <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#0b5d4b', textTransform: 'uppercase' }}>
          Tasa de Ocupación
        </span>
        <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#0b5d4b', marginTop: '4px' }}>
          {pctOcupacion}%
        </div>
        <span style={{ fontSize: '0.75rem', color: '#526b60' }}>
          {ocupadas} ocupadas · {disponibles} disponibles
        </span>
      </div>

      <div style={cardStyle}>
        <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#0369a1', textTransform: 'uppercase' }}>
          Distribución por Tipología
        </span>
        <div style={{ fontSize: '1.15rem', fontWeight: 800, color: '#0369a1', marginTop: '6px' }}>
          P3 VIP: {p3Ocupadas}/{p3Total} · P2 Semi: {p2Ocupadas}/{p2Total}
        </div>
        <span style={{ fontSize: '0.75rem', color: '#849a90' }}>Control institucional por piso</span>
      </div>

      <div style={cardStyle}>
        <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#526b60', textTransform: 'uppercase' }}>
          Auditoría de Movimientos
        </span>
        <div style={{ fontSize: '1.15rem', fontWeight: 800, color: '#13221c', marginTop: '6px' }}>
          {ocupadas} Activos · {altasCount} Altas
        </div>
        <span style={{ fontSize: '0.75rem', color: '#849a90' }}>Trazabilidad de ingresos recientes</span>
      </div>
    </div>
  );
};
