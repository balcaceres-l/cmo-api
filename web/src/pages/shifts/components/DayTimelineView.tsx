import React from 'react';
import { ShiftPrefill, TurnoRaw } from '../types';
import {
  addDays,
  getAreaGroupsForShifts,
  getShiftVisualStyle,
  isOvernightShift,
  isShiftActiveNow,
  normalizeAreaGroup,
  timeToMinutes,
} from '../shiftUtils';

interface DayTimelineViewProps {
  date: string;
  todayStr: string;
  nowHHMM: string;
  shifts: TurnoRaw[];
  isOps: boolean;
  onSelectShift: (turno: TurnoRaw) => void;
  onQuickCreate: (prefill: ShiftPrefill) => void;
}

interface TimelineSegment {
  turno: TurnoRaw;
  startMin: number;
  endMin: number;
  continuesFromPrev: boolean;
  continuesToNext: boolean;
  lane: number;
}

const HOURS = Array.from({ length: 24 }, (_, i) => i);

function assignLanes(segments: Omit<TimelineSegment, 'lane'>[]): TimelineSegment[] {
  const sorted = [...segments].sort((a, b) => a.startMin - b.startMin || a.endMin - b.endMin);
  const laneEnds: number[] = [];

  return sorted.map((seg) => {
    let assigned = laneEnds.findIndex((end) => end <= seg.startMin);
    if (assigned === -1) {
      assigned = laneEnds.length;
      laneEnds.push(seg.endMin);
    } else {
      laneEnds[assigned] = seg.endMin;
    }
    return { ...seg, lane: assigned };
  });
}

export const DayTimelineView: React.FC<DayTimelineViewProps> = ({
  date,
  todayStr,
  nowHHMM,
  shifts,
  isOps,
  onSelectShift,
  onQuickCreate,
}) => {
  const prevDate = addDays(date, -1);
  const activeDayShifts = shifts.filter(
    (t) => t.fecha === date || (t.fecha === prevDate && isOvernightShift(t))
  );
  const areas = getAreaGroupsForShifts(activeDayShifts);
  const isToday = date === todayStr;
  const nowPercent = (timeToMinutes(nowHHMM) / 1440) * 100;

  // Build segments per area for the selected date
  const segmentsByArea = new Map<string, TimelineSegment[]>();
  areas.forEach((a) => segmentsByArea.set(a, []));

  const rawByArea = new Map<string, Omit<TimelineSegment, 'lane'>[]>();
  areas.forEach((a) => rawByArea.set(a, []));

  shifts.forEach((t) => {
    const areaGroup = normalizeAreaGroup(t.area);
    const list = rawByArea.get(areaGroup) || [];
    const overnight = isOvernightShift(t);

    if (t.fecha === date) {
      const startMin = timeToMinutes(t.hora_inicio);
      const endMin = overnight ? 1440 : timeToMinutes(t.hora_fin);
      list.push({
        turno: t,
        startMin,
        endMin: Math.max(endMin, startMin + 30),
        continuesFromPrev: false,
        continuesToNext: overnight,
      });
      rawByArea.set(areaGroup, list);
    } else if (t.fecha === prevDate && overnight) {
      const endMin = timeToMinutes(t.hora_fin);
      if (endMin > 0) {
        list.push({
          turno: t,
          startMin: 0,
          endMin: Math.max(endMin, 30),
          continuesFromPrev: true,
          continuesToNext: false,
        });
        rawByArea.set(areaGroup, list);
      }
    }
  });

  areas.forEach((area) => {
    segmentsByArea.set(area, assignLanes(rawByArea.get(area) || []));
  });

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
      <div style={{ minWidth: '960px' }}>
        {/* Hour Header */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '210px 1fr',
            borderBottom: '1px solid #e2ece7',
            background: '#f8faf9',
          }}
        >
          <div
            style={{
              padding: '12px 16px',
              fontSize: '0.76rem',
              fontWeight: 800,
              color: '#526b60',
              textTransform: 'uppercase',
              letterSpacing: '0.04em',
              borderRight: '1px solid #e2ece7',
            }}
          >
            Área / Servicio
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(24, 1fr)', position: 'relative' }}>
            {HOURS.map((h) => (
              <div
                key={h}
                style={{
                  padding: '10px 2px',
                  textAlign: 'center',
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  color: h % 2 === 0 ? '#526b60' : '#94a3b8',
                  borderLeft: h > 0 ? '1px solid #eef2f0' : 'none',
                }}
              >
                {String(h).padStart(2, '0')}
              </div>
            ))}
          </div>
        </div>

        {/* Area Rows */}
        {areas.map((area) => {
          const segs = segmentsByArea.get(area) || [];
          const laneCount = segs.length > 0 ? Math.max(...segs.map((s) => s.lane)) + 1 : 1;
          const rowHeight = Math.max(66, laneCount * 46 + 16);
          const isEmergencias = /emergenc/i.test(area);
          const hasCoverage = segs.length > 0;

          return (
            <div
              key={area}
              style={{
                display: 'grid',
                gridTemplateColumns: '210px 1fr',
                borderBottom: '1px solid #f0f4f2',
                minHeight: `${rowHeight}px`,
              }}
            >
              {/* Area Label Column */}
              <div
                style={{
                  padding: '12px 16px',
                  borderRight: '1px solid #e2ece7',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'center',
                  gap: '6px',
                  background: !hasCoverage && isEmergencias ? '#fef2f2' : '#ffffff',
                }}
              >
                <div style={{ fontSize: '0.84rem', fontWeight: 800, color: '#13221c' }}>{area}</div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '6px' }}>
                  <span
                    style={{
                      fontSize: '0.72rem',
                      fontWeight: 700,
                      color: hasCoverage ? '#0b5d4b' : isEmergencias ? '#b91c1c' : '#849a90',
                    }}
                  >
                    {hasCoverage
                      ? `${segs.length} ${segs.length === 1 ? 'turno' : 'turnos'}`
                      : isEmergencias
                      ? '⚠ Sin cobertura'
                      : 'Sin turnos'}
                  </span>
                  {isOps && (
                    <button
                      type="button"
                      onClick={() => onQuickCreate({ fecha: date, area })}
                      style={{
                        padding: '2px 8px',
                        borderRadius: '6px',
                        border: '1px solid #cad8d1',
                        background: '#f8faf9',
                        color: '#0b5d4b',
                        fontSize: '0.7rem',
                        fontWeight: 800,
                        cursor: 'pointer',
                      }}
                    >
                      + Asignar
                    </button>
                  )}
                </div>
              </div>

              {/* Timeline Track */}
              <div style={{ position: 'relative', background: !hasCoverage && isEmergencias ? '#fffafa' : '#ffffff' }}>
                {/* Vertical Hour Gridlines */}
                <div
                  style={{
                    position: 'absolute',
                    inset: 0,
                    display: 'grid',
                    gridTemplateColumns: 'repeat(24, 1fr)',
                    pointerEvents: 'none',
                  }}
                >
                  {HOURS.map((h) => (
                    <div
                      key={h}
                      style={{
                        borderLeft: h > 0 ? '1px solid #f1f5f3' : 'none',
                        background: h >= 22 || h < 6 ? 'rgba(15, 23, 42, 0.018)' : 'transparent',
                      }}
                    />
                  ))}
                </div>

                {/* Live "Now" Indicator */}
                {isToday && (
                  <div
                    style={{
                      position: 'absolute',
                      top: 0,
                      bottom: 0,
                      left: `${nowPercent}%`,
                      width: '2px',
                      background: '#dc2626',
                      zIndex: 4,
                      pointerEvents: 'none',
                    }}
                  >
                    <div
                      style={{
                        position: 'absolute',
                        top: '2px',
                        left: '-4px',
                        width: '10px',
                        height: '10px',
                        borderRadius: '50%',
                        background: '#dc2626',
                      }}
                    />
                  </div>
                )}

                {/* Empty state hint */}
                {!hasCoverage && (
                  <div
                    style={{
                      position: 'absolute',
                      inset: 0,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '0.76rem',
                      color: isEmergencias ? '#b91c1c' : '#94a3b8',
                      fontWeight: isEmergencias ? 700 : 500,
                      pointerEvents: 'none',
                    }}
                  >
                    {isEmergencias
                      ? 'Área crítica sin personal asignado en esta jornada'
                      : 'Sin turnos programados'}
                  </div>
                )}

                {/* Shift Bars */}
                {segs.map((seg) => {
                  const leftPct = (seg.startMin / 1440) * 100;
                  const widthPct = Math.max(((seg.endMin - seg.startMin) / 1440) * 100, 3);
                  const style = getShiftVisualStyle(seg.turno);
                  const activeNow = isShiftActiveNow(seg.turno, todayStr, nowHHMM);

                  return (
                    <button
                      key={`${seg.turno.id}-${seg.continuesFromPrev ? 'cont' : 'main'}`}
                      type="button"
                      onClick={() => onSelectShift(seg.turno)}
                      title={`${seg.turno.empleado} · ${seg.turno.hora_inicio}–${seg.turno.hora_fin} · ${seg.turno.area}`}
                      style={{
                        position: 'absolute',
                        top: `${8 + seg.lane * 46}px`,
                        left: `${leftPct}%`,
                        width: `${widthPct}%`,
                        height: '38px',
                        borderRadius: '8px',
                        border: `1px solid ${style.border}`,
                        borderLeft: `4px solid ${style.accent}`,
                        background: style.bg,
                        color: style.text,
                        padding: '4px 8px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: '6px',
                        overflow: 'hidden',
                        cursor: 'pointer',
                        textAlign: 'left',
                        boxShadow: activeNow ? '0 0 0 2px rgba(34, 197, 94, 0.35)' : '0 1px 2px rgba(0,0,0,0.04)',
                        zIndex: 2,
                      }}
                    >
                      <div style={{ minWidth: 0, display: 'flex', alignItems: 'center', gap: '5px' }}>
                        {activeNow && (
                          <span
                            style={{
                              width: '7px',
                              height: '7px',
                              borderRadius: '50%',
                              background: '#16a34a',
                              flexShrink: 0,
                            }}
                          />
                        )}
                        <span
                          style={{
                            fontSize: '0.76rem',
                            fontWeight: 800,
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                          }}
                        >
                          {seg.continuesFromPrev ? '← ' : ''}
                          {seg.turno.empleado}
                          {seg.continuesToNext ? ' →' : ''}
                        </span>
                      </div>
                      <span
                        style={{
                          fontSize: '0.68rem',
                          fontWeight: 700,
                          whiteSpace: 'nowrap',
                          opacity: 0.85,
                          flexShrink: 0,
                        }}
                      >
                        {seg.turno.hora_inicio}–{seg.turno.hora_fin}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

