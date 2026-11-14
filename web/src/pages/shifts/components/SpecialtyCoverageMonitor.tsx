import React, { useState } from 'react';
import { PersonalOption, ShiftPrefill, TurnoRaw } from '../types';
import {
  formatDayHeader,
  getMedicalSpecialties,
  getStaffCategory,
  getWeekDays,
  normalizeSpecialtyLabel,
  shiftCoversBlock,
} from '../shiftUtils';

interface SpecialtyCoverageMonitorProps {
  anchorDate: string;
  todayStr: string;
  shifts: TurnoRaw[];
  personal: PersonalOption[];
  isOps: boolean;
  onQuickCreate: (prefill: ShiftPrefill) => void;
  onOpenSuggestModal: () => void;
}

export const SpecialtyCoverageMonitor: React.FC<SpecialtyCoverageMonitorProps> = ({
  anchorDate,
  todayStr,
  shifts,
  personal,
  isOps,
  onQuickCreate,
  onOpenSuggestModal,
}) => {
  const [expanded, setExpanded] = useState(true);
  const weekDays = getWeekDays(anchorDate);
  const specialties = getMedicalSpecialties(personal, shifts);

  // Solo exigir cobertura planificada a partir de las 00:00 de mañana (misma regla que el sugeridor)
  const isPastBlock = (day: string, _block: 'MANANA' | 'TARDE' | 'NOCHE') => {
    return day <= todayStr;
  };

  // 24/7 Minimum Staff Checks (evaluando los bloques vigentes/futuros igual que el sugeridor)
  let emergCovered = 0;
  let nurseCovered = 0;
  let recepCovered = 0;
  let emergActiveCovered = 0;
  let nurseActiveCovered = 0;
  let recepActiveCovered = 0;
  let activeBlocks = 0;
  const totalBlocks = weekDays.length * 3;

  weekDays.forEach((day) => {
    (['MANANA', 'TARDE', 'NOCHE'] as const).forEach((block) => {
      const past = isPastBlock(day, block);
      if (!past) activeBlocks++;

      if (
        shifts.some(
          (s) =>
            shiftCoversBlock(s, day, block) &&
            s.rol === 'MEDICO' &&
            /emergenc|urgenc|trauma/i.test(s.area)
        )
      ) {
        emergCovered++;
        if (!past) emergActiveCovered++;
      }
      if (
        shifts.some(
          (s) =>
            shiftCoversBlock(s, day, block) &&
            getStaffCategory(s.rol, s.cargo) === 'ENFERMERIA'
        )
      ) {
        nurseCovered++;
        if (!past) nurseActiveCovered++;
      }
      if (
        shifts.some(
          (s) =>
            shiftCoversBlock(s, day, block) &&
            getStaffCategory(s.rol, s.cargo) === 'RECEPCION'
        )
      ) {
        recepCovered++;
        if (!past) recepActiveCovered++;
      }
    });
  });

  const uncoveredSpecialtiesCount = specialties.filter((spec) => {
    return !weekDays.some((day) =>
      shifts.some(
        (s) =>
          s.fecha === day &&
          s.rol === 'MEDICO' &&
          normalizeSpecialtyLabel(s.cargo) === spec
      )
    );
  }).length;

  const missing247 =
    activeBlocks > 0 &&
    (emergActiveCovered < activeBlocks ||
      nurseActiveCovered < activeBlocks ||
      recepActiveCovered < activeBlocks);

  return (
    <div
      style={{
        background: '#ffffff',
        borderRadius: '16px',
        border: '1px solid #e2ece7',
        padding: '16px 20px',
        boxShadow: '0 2px 6px rgba(11, 44, 35, 0.04)',
      }}
    >
      {/* Header Row */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '12px',
          marginBottom: expanded ? '14px' : '0',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          <h2 style={{ margin: 0, fontSize: '0.98rem', fontWeight: 800, color: '#073f33' }}>
            Cobertura Semanal de Especialidades y Guardia 24/7
          </h2>
          {uncoveredSpecialtiesCount === 0 && !missing247 ? (
            <span
              style={{
                background: '#dcfce7',
                color: '#15803d',
                fontSize: '0.72rem',
                fontWeight: 800,
                padding: '3px 9px',
                borderRadius: '999px',
              }}
            >
              ✓ Cobertura completa
            </span>
          ) : (
            <span
              style={{
                background: '#fef2f2',
                color: '#b91c1c',
                fontSize: '0.72rem',
                fontWeight: 800,
                padding: '3px 9px',
                borderRadius: '999px',
              }}
            >
              {uncoveredSpecialtiesCount > 0
                ? `${uncoveredSpecialtiesCount} especialidad(es) sin turno esta semana`
                : 'Faltan bloques 24/7 por cubrir'}
            </span>
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
          {isOps && (
            <button
              type="button"
              onClick={onOpenSuggestModal}
              style={{
                padding: '7px 14px',
                borderRadius: '9px',
                border: 'none',
                background: '#0b5d4b',
                color: '#ffffff',
                fontSize: '0.78rem',
                fontWeight: 800,
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              ⚡ Sugerir Cobertura 24/7
            </button>
          )}
          <button
            type="button"
            onClick={() => setExpanded((v) => !v)}
            style={{
              padding: '6px 12px',
              borderRadius: '8px',
              border: '1px solid #cad8d1',
              background: '#f8faf9',
              color: '#33483f',
              fontSize: '0.76rem',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            {expanded ? 'Ocultar' : 'Mostrar'}
          </button>
        </div>
      </div>

      {expanded && (
        <div style={{ display: 'grid', gap: '14px' }}>
          {/* 24/7 Minimum Staff Strip */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
              gap: '10px',
            }}
          >
            {[
              {
                label: 'Emergencias 24/7 (Médico)',
                covered: activeBlocks > 0 ? emergActiveCovered : emergCovered,
                target: activeBlocks > 0 ? activeBlocks : totalBlocks,
                icon: '🚨',
                okColor: '#0369a1',
                okBg: '#f0f9ff',
              },
              {
                label: 'Enfermería 24/7 (Triaje)',
                covered: activeBlocks > 0 ? nurseActiveCovered : nurseCovered,
                target: activeBlocks > 0 ? activeBlocks : totalBlocks,
                icon: '💉',
                okColor: '#0b5d4b',
                okBg: '#eaf5f1',
              },
              {
                label: 'Recepción 24/7 (Admisión)',
                covered: activeBlocks > 0 ? recepActiveCovered : recepCovered,
                target: activeBlocks > 0 ? activeBlocks : totalBlocks,
                icon: '🖥',
                okColor: '#0b5d4b',
                okBg: '#eaf5f1',
              },
            ].map((item) => {
              const complete = item.covered >= item.target;
              const isPartialWeek = activeBlocks > 0 && activeBlocks < totalBlocks;
              return (
                <div
                  key={item.label}
                  style={{
                    padding: '10px 14px',
                    borderRadius: '10px',
                    border: `1px solid ${complete ? '#a7f3d0' : '#fecaca'}`,
                    background: complete ? item.okBg : '#fff8f8',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '8px',
                  }}
                >
                  <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#13221c' }}>
                    {item.icon} {item.label}
                  </div>
                  <span
                    style={{
                      fontSize: '0.76rem',
                      fontWeight: 800,
                      color: complete ? item.okColor : '#b91c1c',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {item.covered}/{item.target} {isPartialWeek ? 'vigentes' : 'turnos'}
                  </span>
                </div>
              );
            })}
          </div>

          {/* Specialty Day-by-Day Strip */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(255px, 1fr))',
              gap: '10px',
            }}
          >
            {specialties.map((spec) => {
              const isEmerg = spec === 'Emergencias';
              const dayStatus = weekDays.map((day) => {
                const dayShifts = shifts.filter(
                  (s) =>
                    s.fecha === day &&
                    s.rol === 'MEDICO' &&
                    normalizeSpecialtyLabel(s.cargo) === spec
                );
                return {
                  day,
                  dayLetter: formatDayHeader(day).dayLetter,
                  count: dayShifts.length,
                };
              });

              const coveredDaysCount = dayStatus.filter((d) => d.count > 0).length;
              const isMissing = coveredDaysCount === 0;

              return (
                <div
                  key={spec}
                  style={{
                    padding: '10px 12px',
                    borderRadius: '10px',
                    border: `1px solid ${isMissing ? '#fecaca' : '#e2ece7'}`,
                    background: isMissing ? '#fef2f2' : '#f8faf9',
                    display: 'grid',
                    gap: '8px',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
                    <span style={{ fontSize: '0.82rem', fontWeight: 800, color: '#13221c' }}>
                      {isEmerg ? '🚨 ' : '🩺 '}
                      {spec}
                    </span>
                    <span
                      style={{
                        fontSize: '0.7rem',
                        fontWeight: 800,
                        color: isMissing ? '#b91c1c' : '#0b5d4b',
                      }}
                    >
                      {isMissing
                        ? '0 días'
                        : `${coveredDaysCount} ${coveredDaysCount === 1 ? 'día' : 'días'}`}
                    </span>
                  </div>

                  {/* L M X J V S D pills */}
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '6px' }}>
                    <div style={{ display: 'flex', gap: '4px' }}>
                      {dayStatus.map((d) => {
                        const isToday = d.day === todayStr;
                        const active = d.count > 0;
                        return (
                          <span
                            key={d.day}
                            title={`${d.day}: ${d.count} turno(s)`}
                            style={{
                              width: '22px',
                              height: '22px',
                              borderRadius: '6px',
                              display: 'inline-flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              fontSize: '0.68rem',
                              fontWeight: 800,
                              background: active ? '#0284c7' : '#ffffff',
                              color: active ? '#ffffff' : '#94a3b8',
                              border: isToday ? '2px solid #0b5d4b' : '1px solid #d5e0db',
                            }}
                          >
                            {d.dayLetter}
                          </span>
                        );
                      })}
                    </div>

                    {isOps && isMissing && (
                      <button
                        type="button"
                        onClick={() =>
                          onQuickCreate({
                            fecha: weekDays[0],
                            area: isEmerg ? 'Servicio de Emergencias' : 'Consulta Externa',
                            hora_inicio: '08:00',
                            hora_fin: '16:00',
                            specialtyFilter: spec,
                          })
                        }
                        style={{
                          padding: '3px 8px',
                          borderRadius: '6px',
                          border: '1px solid #fecaca',
                          background: '#ffffff',
                          color: '#b91c1c',
                          fontSize: '0.68rem',
                          fontWeight: 800,
                          cursor: 'pointer',
                        }}
                      >
                        + Programar
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};

