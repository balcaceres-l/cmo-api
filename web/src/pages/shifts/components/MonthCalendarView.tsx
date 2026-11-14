import React from 'react';
import { ShiftPrefill, TurnoRaw } from '../types';
import {
  getMonthGridDays,
  getShiftVisualStyle,
  parseISODate,
  shiftCoversBlock,
} from '../shiftUtils';

interface MonthCalendarViewProps {
  anchorDate: string;
  todayStr: string;
  shifts: TurnoRaw[];
  isOps: boolean;
  onSelectShift: (turno: TurnoRaw) => void;
  onSelectDay: (date: string) => void;
  onQuickCreate: (prefill: ShiftPrefill) => void;
}

const WEEKDAYS_HEADER = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

export const MonthCalendarView: React.FC<MonthCalendarViewProps> = ({
  anchorDate,
  todayStr,
  shifts,
  isOps,
  onSelectShift,
  onSelectDay,
  onQuickCreate,
}) => {
  const gridDays = getMonthGridDays(anchorDate);

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
      <div style={{ minWidth: '840px' }}>
        {/* Weekday Header */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(7, 1fr)',
            background: '#f8faf9',
            borderBottom: '1px solid #e2ece7',
          }}
        >
          {WEEKDAYS_HEADER.map((wd) => (
            <div
              key={wd}
              style={{
                padding: '10px 12px',
                fontSize: '0.76rem',
                fontWeight: 800,
                color: '#526b60',
                textAlign: 'center',
                textTransform: 'uppercase',
                letterSpacing: '0.04em',
              }}
            >
              {wd}
            </div>
          ))}
        </div>

        {/* Calendar Grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(7, 1fr)',
          }}
        >
          {gridDays.map(({ date, inMonth }, idx) => {
            const dayNum = parseISODate(date).getDate();
            const isToday = date === todayStr;
            const dayShifts = shifts
              .filter((s) => s.fecha === date)
              .sort((a, b) => a.hora_inicio.localeCompare(b.hora_inicio));

            const medCount = dayShifts.filter((s) => s.rol === 'MEDICO').length;
            const enfCount = dayShifts.filter((s) => s.rol !== 'MEDICO').length;
            const hasEmergMedico = dayShifts.some(
              (s) => s.rol === 'MEDICO' && /emergenc|urgenc|trauma/i.test(s.area)
            );

            const hasAM = dayShifts.some((s) => shiftCoversBlock(s, date, 'MANANA'));
            const hasPM = dayShifts.some((s) => shiftCoversBlock(s, date, 'TARDE'));
            const hasNight = dayShifts.some((s) => shiftCoversBlock(s, date, 'NOCHE'));

            // Health dot color
            const healthColor =
              dayShifts.length === 0
                ? '#cbd5e1'
                : hasEmergMedico && enfCount > 0
                ? '#10b981'
                : hasEmergMedico
                ? '#f59e0b'
                : '#ef4444';

            const healthTitle =
              dayShifts.length === 0
                ? 'Sin turnos programados'
                : hasEmergMedico && enfCount > 0
                ? 'Cobertura completa (Médico en Emergencias + Apoyo)'
                : hasEmergMedico
                ? 'Cobertura parcial (falta apoyo/enfermería)'
                : 'Sin médico asignado en Emergencias';

            const visibleShifts = dayShifts.slice(0, 2);
            const extraCount = dayShifts.length - visibleShifts.length;

            return (
              <div
                key={date}
                style={{
                  minHeight: '132px',
                  padding: '8px',
                  borderRight: (idx + 1) % 7 === 0 ? 'none' : '1px solid #eef2f0',
                  borderBottom: '1px solid #eef2f0',
                  background: isToday
                    ? '#f0fdf4'
                    : inMonth
                    ? '#ffffff'
                    : '#f8faf9',
                  opacity: inMonth ? 1 : 0.62,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '6px',
                }}
              >
                {/* Top Day Bar */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '4px' }}>
                  <button
                    type="button"
                    onClick={() => onSelectDay(date)}
                    title="Ver timeline completo del día"
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '5px',
                      border: 'none',
                      background: 'transparent',
                      padding: '2px 4px',
                      borderRadius: '6px',
                      cursor: 'pointer',
                    }}
                  >
                    <span
                      style={{
                        fontSize: '0.84rem',
                        fontWeight: 800,
                        color: isToday ? '#ffffff' : '#13221c',
                        background: isToday ? '#0b5d4b' : 'transparent',
                        padding: isToday ? '1px 7px' : '0',
                        borderRadius: '999px',
                      }}
                    >
                      {dayNum}
                    </span>
                    <span
                      title={healthTitle}
                      style={{
                        width: '8px',
                        height: '8px',
                        borderRadius: '50%',
                        background: healthColor,
                        display: 'inline-block',
                      }}
                    />
                  </button>

                  {/* Role Counts */}
                  {dayShifts.length > 0 ? (
                    <div style={{ display: 'flex', gap: '4px', fontSize: '0.68rem', fontWeight: 700, color: '#526b60' }}>
                      {medCount > 0 && <span>🩺{medCount}</span>}
                      {enfCount > 0 && <span>💉{enfCount}</span>}
                    </div>
                  ) : (
                    isOps && (
                      <button
                        type="button"
                        onClick={() => onQuickCreate({ fecha: date })}
                        title="Asignar turno este día"
                        style={{
                          border: 'none',
                          background: '#f0f4f2',
                          color: '#0b5d4b',
                          borderRadius: '5px',
                          padding: '1px 6px',
                          fontSize: '0.7rem',
                          fontWeight: 800,
                          cursor: 'pointer',
                        }}
                      >
                        +
                      </button>
                    )
                  )}
                </div>

                {/* Mini 3-Band Coverage Bar (00-08 / 08-16 / 16-24) */}
                {dayShifts.length > 0 && (
                  <div
                    title={`Bloques 8h cubiertos: ${hasNight ? 'Noche (00–08) ' : ''}${hasAM ? 'Mañana (08–16) ' : ''}${hasPM ? 'Tarde (16–24)' : ''}`}
                    style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '3px' }}
                  >
                    <div
                      style={{
                        height: '4px',
                        borderRadius: '2px',
                        background: hasAM ? '#f59e0b' : '#e2e8f0',
                      }}
                    />
                    <div
                      style={{
                        height: '4px',
                        borderRadius: '2px',
                        background: hasPM ? '#0284c7' : '#e2e8f0',
                      }}
                    />
                    <div
                      style={{
                        height: '4px',
                        borderRadius: '2px',
                        background: hasNight ? '#7c3aed' : '#e2e8f0',
                      }}
                    />
                  </div>
                )}

                {/* Compact Shift Chips */}
                <div style={{ display: 'grid', gap: '4px' }}>
                  {visibleShifts.map((s) => {
                    const style = getShiftVisualStyle(s);
                    return (
                      <button
                        key={s.id}
                        type="button"
                        onClick={() => onSelectShift(s)}
                        style={{
                          width: '100%',
                          textAlign: 'left',
                          padding: '3px 6px',
                          borderRadius: '6px',
                          border: `1px solid ${style.border}`,
                          borderLeft: `3px solid ${style.accent}`,
                          background: style.bg,
                          color: style.text,
                          fontSize: '0.69rem',
                          fontWeight: 700,
                          cursor: 'pointer',
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                        }}
                      >
                        {s.hora_inicio} {s.empleado}
                      </button>
                    );
                  })}

                  {extraCount > 0 && (
                    <button
                      type="button"
                      onClick={() => onSelectDay(date)}
                      style={{
                        border: 'none',
                        background: '#f0f4f2',
                        color: '#073f33',
                        borderRadius: '6px',
                        padding: '3px 6px',
                        fontSize: '0.68rem',
                        fontWeight: 800,
                        cursor: 'pointer',
                        textAlign: 'center',
                      }}
                    >
                      +{extraCount} más · Ver día
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

