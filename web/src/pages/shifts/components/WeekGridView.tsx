import React from 'react';
import { PersonalOption, ShiftFranja, ShiftPrefill, TurnoRaw, WeekGroupBy } from '../types';
import {
  FRANJA_CONFIG,
  formatDayHeader,
  getAreaGroupsForShifts,
  getShiftFranja,
  getShiftSpecialtyGroup,
  getShiftVisualStyle,
  getSpecialtyGroupRows,
  getWeekDays,
  isRelevoShift,
  isShiftActiveNow,
  normalizeAreaGroup,
} from '../shiftUtils';

interface WeekGridViewProps {
  anchorDate: string;
  todayStr: string;
  nowHHMM: string;
  shifts: TurnoRaw[];
  personal: PersonalOption[];
  groupBy: WeekGroupBy;
  isOps: boolean;
  onSelectShift: (turno: TurnoRaw) => void;
  onSelectDay: (date: string) => void;
  onQuickCreate: (prefill: ShiftPrefill) => void;
}

const FRANJAS: ShiftFranja[] = ['MANANA', 'TARDE', 'NOCHE'];

export const WeekGridView: React.FC<WeekGridViewProps> = ({
  anchorDate,
  todayStr,
  nowHHMM,
  shifts,
  personal,
  groupBy,
  isOps,
  onSelectShift,
  onSelectDay,
  onQuickCreate,
}) => {
  const days = getWeekDays(anchorDate);
  const daysSet = new Set(days);
  const weekShifts = shifts.filter((s) => daysSet.has(s.fecha));

  const areas = getAreaGroupsForShifts(weekShifts);
  const specialtyRows = getSpecialtyGroupRows(personal, weekShifts);

  const rows =
    groupBy === 'area'
      ? areas.map((area) => ({ key: area, title: area, subtitle: '' }))
      : groupBy === 'especialidad'
      ? specialtyRows.map((spec) => ({
          key: spec,
          title: spec,
          subtitle: spec === 'Emergencias' ? '24/7 (3 turnos 8h)' : spec === 'Enfermería' || spec === 'Recepción' ? 'Mínimo 24/7 (8h)' : 'Horario 08:00–16:00',
        }))
      : FRANJAS.map((f) => ({
          key: f,
          title: FRANJA_CONFIG[f].label,
          subtitle: FRANJA_CONFIG[f].range,
        }));

  const rowHeaderTitle =
    groupBy === 'area'
      ? 'Área / Servicio'
      : groupBy === 'especialidad'
      ? 'Especialidad / Rol'
      : 'Franja (8 horas)';

  return (
    <div
      style={{
        background: '#ffffff',
        borderRadius: '16px',
        border: '1px solid #e2ece7',
        overflowX: 'auto',
        boxShadow: '0 2px 6px rgba(11, 44, 35, 0.04)',
      }}
    >
      <div style={{ minWidth: '1040px' }}>
        {/* Header Row: 7 Days */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '195px repeat(7, minmax(120px, 1fr))',
            background: '#f8faf9',
            borderBottom: '1px solid #e2ece7',
          }}
        >
          <div
            style={{
              padding: '12px 14px',
              fontSize: '0.75rem',
              fontWeight: 800,
              color: '#526b60',
              textTransform: 'uppercase',
              borderRight: '1px solid #e2ece7',
              display: 'flex',
              alignItems: 'center',
            }}
          >
            {rowHeaderTitle}
          </div>

          {days.map((day) => {
            const { dayName, dayNum, monthShort } = formatDayHeader(day);
            const isToday = day === todayStr;
            const dayShifts = weekShifts.filter((s) => s.fecha === day);
            const medCount = dayShifts.filter((s) => s.rol === 'MEDICO').length;
            const enfCount = dayShifts.filter((s) => s.rol !== 'MEDICO').length;

            return (
              <button
                key={day}
                type="button"
                onClick={() => onSelectDay(day)}
                title="Clic para abrir vista detallada del día"
                style={{
                  padding: '10px 8px',
                  border: 'none',
                  borderLeft: '1px solid #e2ece7',
                  background: isToday ? '#eaf5f1' : 'transparent',
                  cursor: 'pointer',
                  textAlign: 'center',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: '4px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                  <span style={{ fontSize: '0.72rem', fontWeight: 700, color: isToday ? '#0b5d4b' : '#526b60' }}>
                    {dayName}
                  </span>
                  <span style={{ fontSize: '0.88rem', fontWeight: 800, color: isToday ? '#073f33' : '#13221c' }}>
                    {dayNum} {monthShort}
                  </span>
                  {isToday && (
                    <span
                      style={{
                        background: '#0b5d4b',
                        color: '#ffffff',
                        fontSize: '0.62rem',
                        fontWeight: 800,
                        padding: '1px 5px',
                        borderRadius: '4px',
                      }}
                    >
                      HOY
                    </span>
                  )}
                </div>
                <div style={{ display: 'flex', gap: '6px', fontSize: '0.68rem', fontWeight: 700, color: '#526b60' }}>
                  <span>🩺 {medCount}</span>
                  <span>💉 {enfCount}</span>
                </div>
              </button>
            );
          })}
        </div>

        {/* Matrix Rows */}
        {rows.map((row) => {
          const isCriticalRow =
            (groupBy === 'area' && /emergenc/i.test(row.key)) ||
            (groupBy === 'especialidad' &&
              (row.key === 'Emergencias' || row.key === 'Enfermería' || row.key === 'Recepción'));

          return (
            <div
              key={row.key}
              style={{
                display: 'grid',
                gridTemplateColumns: '195px repeat(7, minmax(120px, 1fr))',
                borderBottom: '1px solid #f0f4f2',
              }}
            >
              {/* Row Header */}
              <div
                style={{
                  padding: '12px 14px',
                  borderRight: '1px solid #e2ece7',
                  background: '#fcfdfd',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'center',
                  gap: '3px',
                }}
              >
                <span style={{ fontSize: '0.82rem', fontWeight: 800, color: '#13221c' }}>{row.title}</span>
                {row.subtitle && (
                  <span style={{ fontSize: '0.7rem', color: '#526b60', fontWeight: 600 }}>{row.subtitle}</span>
                )}
              </div>

              {/* 7 Day Cells */}
              {days.map((day) => {
                const isToday = day === todayStr;
                const cellShifts = weekShifts.filter((s) => {
                  if (s.fecha !== day) return false;
                  if (groupBy === 'area') return normalizeAreaGroup(s.area) === row.key;
                  if (groupBy === 'especialidad') return getShiftSpecialtyGroup(s) === row.key;
                  return getShiftFranja(s.hora_inicio) === row.key;
                });

                const isPastDay = day < todayStr;
                const isEmptyCritical = !isPastDay && isCriticalRow && cellShifts.length === 0;

                const prefill: ShiftPrefill =
                  groupBy === 'area'
                    ? { fecha: day, area: row.key, hora_inicio: '08:00', hora_fin: '16:00' }
                    : groupBy === 'especialidad'
                    ? {
                        fecha: day,
                        area:
                          row.key === 'Emergencias'
                            ? 'Servicio de Emergencias'
                            : row.key === 'Enfermería'
                            ? 'Triaje y Enfermería'
                            : row.key === 'Recepción'
                            ? 'Admisión y Recepción'
                            : 'Consulta Externa',
                        hora_inicio: '08:00',
                        hora_fin: '16:00',
                        specialtyFilter:
                          row.key !== 'Enfermería' && row.key !== 'Recepción' ? row.key : undefined,
                      }
                    : {
                        fecha: day,
                        hora_inicio: FRANJA_CONFIG[row.key as ShiftFranja].defaultStart,
                        hora_fin: FRANJA_CONFIG[row.key as ShiftFranja].defaultEnd,
                      };

                return (
                  <div
                    key={`${row.key}-${day}`}
                    style={{
                      padding: '8px',
                      borderLeft: '1px solid #eef2f0',
                      minHeight: '92px',
                      background: isEmptyCritical
                        ? '#fff8f8'
                        : isToday
                        ? 'rgba(234, 245, 241, 0.35)'
                        : isPastDay
                        ? '#fafcfb'
                        : '#ffffff',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '6px',
                    }}
                  >
                    {cellShifts.map((s) => {
                      const style = getShiftVisualStyle(s);
                      const activeNow = isShiftActiveNow(s, todayStr, nowHHMM);
                      const relevo = isRelevoShift(s);

                      return (
                        <button
                          key={s.id}
                          type="button"
                          onClick={() => onSelectShift(s)}
                          style={{
                            width: '100%',
                            textAlign: 'left',
                            padding: '6px 8px',
                            borderRadius: '8px',
                            border: `1px solid ${style.border}`,
                            borderLeft: `3px solid ${style.accent}`,
                            background: style.bg,
                            color: style.text,
                            cursor: 'pointer',
                            display: 'grid',
                            gap: '2px',
                            boxShadow: activeNow ? '0 0 0 2px rgba(34, 197, 94, 0.3)' : 'none',
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '4px' }}>
                            <span
                              style={{
                                fontSize: '0.74rem',
                                fontWeight: 800,
                                whiteSpace: 'nowrap',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                              }}
                            >
                              {activeNow ? '● ' : ''}
                              {s.empleado}
                            </span>
                            {relevo && <span style={{ fontSize: '0.68rem' }}>🔄</span>}
                          </div>
                          <div style={{ fontSize: '0.68rem', fontWeight: 700, opacity: 0.9 }}>
                            {s.hora_inicio}–{s.hora_fin}
                          </div>
                          <div
                            style={{
                              fontSize: '0.65rem',
                              opacity: 0.8,
                              whiteSpace: 'nowrap',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                            }}
                          >
                            {groupBy === 'especialidad' ? s.area : style.roleLabel}
                          </div>
                        </button>
                      );
                    })}

                    {cellShifts.length === 0 && isEmptyCritical && (
                      <div
                        style={{
                          fontSize: '0.68rem',
                          fontWeight: 700,
                          color: '#b91c1c',
                          textAlign: 'center',
                          padding: '4px',
                          border: '1px dashed #fecaca',
                          borderRadius: '6px',
                        }}
                      >
                        Sin guardia
                      </div>
                    )}

                    {isOps && (
                      <button
                        type="button"
                        onClick={() => onQuickCreate(prefill)}
                        title="Asignar turno en esta celda"
                        style={{
                          marginTop: 'auto',
                          padding: '3px 6px',
                          borderRadius: '6px',
                          border: '1px dashed #cad8d1',
                          background: 'transparent',
                          color: '#526b60',
                          fontSize: '0.68rem',
                          fontWeight: 700,
                          cursor: 'pointer',
                        }}
                      >
                        + Asignar
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          );
        })}
      </div>
    </div>
  );
};
