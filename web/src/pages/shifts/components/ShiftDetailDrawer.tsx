import React from 'react';
import { getRoleBadgeConfig } from '../../../utils/roles';
import { TurnoRaw } from '../types';
import {
  addDays,
  FRANJA_CONFIG,
  getShiftDurationHours,
  getShiftFranja,
  isOvernightShift,
  isRelevoShift,
  isShiftActiveNow,
} from '../shiftUtils';

interface ShiftDetailDrawerProps {
  turno: TurnoRaw | null;
  allShifts: TurnoRaw[];
  todayStr: string;
  nowHHMM: string;
  isOps: boolean;
  onClose: () => void;
  onOpenRelief: (turno: TurnoRaw) => void;
  onDelete: (turno: TurnoRaw) => void;
}

export const ShiftDetailDrawer: React.FC<ShiftDetailDrawerProps> = ({
  turno,
  allShifts,
  todayStr,
  nowHHMM,
  isOps,
  onClose,
  onOpenRelief,
  onDelete,
}) => {
  if (!turno) return null;

  // Si el backend dividió un turno nocturno en dos tramos (23:00–23:59 y 00:00–07:00),
  // localizamos el tramo compañero para mostrar el turno completo en el detalle.
  const companionNext = turno.hora_fin.startsWith('23:59')
    ? allShifts.find(
        (s) =>
          s.id !== turno.id &&
          String(s.id_usuario) === String(turno.id_usuario) &&
          s.area === turno.area &&
          s.fecha === addDays(turno.fecha, 1) &&
          s.hora_inicio.startsWith('00:00')
      )
    : undefined;

  const companionPrev = turno.hora_inicio.startsWith('00:00')
    ? allShifts.find(
        (s) =>
          s.id !== turno.id &&
          String(s.id_usuario) === String(turno.id_usuario) &&
          s.area === turno.area &&
          s.fecha === addDays(turno.fecha, -1) &&
          s.hora_fin.startsWith('23:59')
      )
    : undefined;

  const firstPart = companionPrev ?? turno;
  const secondPart = companionNext ?? (companionPrev ? turno : undefined);
  const isMergedOvernight = Boolean(companionNext || companionPrev);

  const badge = getRoleBadgeConfig(turno.rol);
  const activeNow =
    isShiftActiveNow(turno, todayStr, nowHHMM) ||
    (companionNext ? isShiftActiveNow(companionNext, todayStr, nowHHMM) : false) ||
    (companionPrev ? isShiftActiveNow(companionPrev, todayStr, nowHHMM) : false);
  const relevo = isRelevoShift(turno);
  const franja = FRANJA_CONFIG[getShiftFranja(firstPart.hora_inicio)];
  const durationHours = secondPart
    ? Math.round((getShiftDurationHours(firstPart) + getShiftDurationHours(secondPart)) * 10) / 10
    : getShiftDurationHours(turno);
  const overnight = isOvernightShift(turno) || isMergedOvernight;

  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(11, 28, 22, 0.42)',
        zIndex: 55,
        display: 'flex',
        justifyContent: 'flex-end',
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          width: '100%',
          maxWidth: '400px',
          height: '100%',
          background: '#ffffff',
          boxShadow: '-8px 0 30px rgba(0, 0, 0, 0.16)',
          padding: '24px',
          display: 'flex',
          flexDirection: 'column',
          gap: '18px',
          overflowY: 'auto',
          boxSizing: 'border-box',
        }}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px' }}>
          <div>
            <span style={{ fontSize: '0.72rem', fontWeight: 800, color: '#0b5d4b', textTransform: 'uppercase' }}>
              Detalle del Turno
            </span>
            <h3 style={{ margin: '4px 0 0', fontSize: '1.2rem', fontWeight: 800, color: '#13221c' }}>
              {turno.empleado}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            style={{
              border: '1px solid #e2ece7',
              background: '#f8faf9',
              borderRadius: '8px',
              width: '32px',
              height: '32px',
              cursor: 'pointer',
              fontSize: '1rem',
              color: '#526b60',
            }}
          >
            ✕
          </button>
        </div>

        {/* Status & Role Badges */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
          <span
            style={{
              padding: '4px 10px',
              borderRadius: '6px',
              fontSize: '0.74rem',
              fontWeight: 700,
              background: badge.bg,
              color: badge.color,
              border: badge.border,
            }}
          >
            {turno.rol}
          </span>
          <span
            style={{
              padding: '4px 10px',
              borderRadius: '999px',
              fontSize: '0.74rem',
              fontWeight: 700,
              background: franja.badgeBg,
              color: franja.badgeColor,
            }}
          >
            Franja {franja.label}
          </span>
          {activeNow && (
            <span
              style={{
                padding: '4px 10px',
                borderRadius: '999px',
                fontSize: '0.74rem',
                fontWeight: 800,
                background: '#dcfce7',
                color: '#15803d',
              }}
            >
              ● En Turno Activo
            </span>
          )}
          {relevo && (
            <span
              style={{
                padding: '4px 10px',
                borderRadius: '999px',
                fontSize: '0.74rem',
                fontWeight: 800,
                background: '#fef3c7',
                color: '#b45309',
              }}
            >
              🔄 Relevo de Guardia
            </span>
          )}
        </div>

        {/* Info Card */}
        <div
          style={{
            background: '#f8faf9',
            borderRadius: '12px',
            border: '1px solid #e2ece7',
            padding: '16px',
            display: 'grid',
            gap: '12px',
          }}
        >
          <div>
            <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#526b60', textTransform: 'uppercase' }}>
              Cargo / Especialidad
            </div>
            <div style={{ fontSize: '0.9rem', fontWeight: 700, color: '#13221c', marginTop: '2px' }}>
              {turno.cargo}
            </div>
          </div>

          <div>
            <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#526b60', textTransform: 'uppercase' }}>
              Área Asignada
            </div>
            <div style={{ fontSize: '0.9rem', fontWeight: 700, color: '#13221c', marginTop: '2px' }}>
              📍 {turno.area}
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <div>
              <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#526b60', textTransform: 'uppercase' }}>
                Fecha
              </div>
              <div style={{ fontSize: '0.9rem', fontWeight: 800, color: '#13221c', marginTop: '2px' }}>
                📅 {secondPart ? `${firstPart.fecha} → ${secondPart.fecha}` : turno.fecha}
              </div>
            </div>
            <div>
              <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#526b60', textTransform: 'uppercase' }}>
                Duración
              </div>
              <div style={{ fontSize: '0.9rem', fontWeight: 800, color: '#0b5d4b', marginTop: '2px' }}>
                ⏱ {durationHours} h {overnight ? '(Nocturno)' : ''}
              </div>
            </div>
          </div>

          <div>
            <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#526b60', textTransform: 'uppercase' }}>
              Horario
            </div>
            <div style={{ fontSize: '0.95rem', fontWeight: 800, color: '#0b5d4b', marginTop: '2px' }}>
              🕒 {secondPart ? `${firstPart.hora_inicio} — ${secondPart.hora_fin} (+1 día)` : `${turno.hora_inicio} — ${turno.hora_fin}`}
            </div>
            {secondPart && (
              <div style={{ fontSize: '0.72rem', color: '#526b60', marginTop: '4px' }}>
                Guardado en 2 tramos por cruce de medianoche ({firstPart.hora_inicio}–{firstPart.hora_fin} y {secondPart.hora_inicio}–{secondPart.hora_fin}).
              </div>
            )}
          </div>
        </div>

        {/* Operational Actions */}
        {isOps ? (
          <div style={{ marginTop: 'auto', display: 'grid', gap: '10px', paddingTop: '12px', borderTop: '1px solid #e2ece7' }}>
            <button
              type="button"
              onClick={() => {
                onClose();
                onOpenRelief(turno);
              }}
              style={{
                width: '100%',
                padding: '11px 14px',
                borderRadius: '10px',
                border: '1px solid #bae6fd',
                background: '#f0f9ff',
                color: '#0369a1',
                fontSize: '0.85rem',
                fontWeight: 800,
                cursor: 'pointer',
              }}
            >
              🔄 Gestionar Incapacidad / Relevo
            </button>
            <button
              type="button"
              onClick={() => {
                onClose();
                onDelete(turno);
              }}
              style={{
                width: '100%',
                padding: '10px 14px',
                borderRadius: '10px',
                border: '1px solid #fecaca',
                background: '#fff5f5',
                color: '#b91c1c',
                fontSize: '0.82rem',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              Eliminar Turno
            </button>
          </div>
        ) : (
          <div
            style={{
              marginTop: 'auto',
              padding: '12px',
              borderRadius: '10px',
              background: '#f8faf9',
              border: '1px solid #e2ece7',
              fontSize: '0.78rem',
              color: '#526b60',
            }}
          >
            Solo el Jefe de Operaciones o Administrador puede reasignar o eliminar turnos.
          </div>
        )}
      </div>
    </div>
  );
};

