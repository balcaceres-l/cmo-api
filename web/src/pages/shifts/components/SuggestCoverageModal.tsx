import React, { useEffect, useMemo, useState } from 'react';
import { PersonalOption, ShiftFranja, TurnoRaw } from '../types';
import { formatDayHeader, getWeekDays } from '../shiftUtils';
import { SuggestedShift, SuggesterOptions, suggestWeeklyCoverage } from '../shiftSuggester';

interface SuggestCoverageModalProps {
  isOpen: boolean;
  anchorDate: string;
  shifts: TurnoRaw[];
  personal: PersonalOption[];
  isSubmitting: boolean;
  onClose: () => void;
  onConfirmSave: (selected: SuggestedShift[]) => Promise<void>;
}

const BLOCK_BADGE: Record<ShiftFranja, { label: string; bg: string; color: string }> = {
  MANANA: { label: '☀ 08:00–16:00', bg: '#fef3c7', color: '#b45309' },
  TARDE: { label: '🌤 16:00–23:59', bg: '#e0f2fe', color: '#0369a1' },
  NOCHE: { label: '🌙 00:00–08:00', bg: '#ede9fe', color: '#6d28d9' },
};

export const SuggestCoverageModal: React.FC<SuggestCoverageModalProps> = ({
  isOpen,
  anchorDate,
  shifts,
  personal,
  isSubmitting,
  onClose,
  onConfirmSave,
}) => {
  const [options, setOptions] = useState<SuggesterOptions>({
    maxWeeklyHours: 40,
    minRestHours: 16,
    maxNightsPerWeek: 2,
    minSpecialtyDays: 1,
    allowGeneralBackupForEmergencies: true,
  });

  const weekDays = useMemo(() => getWeekDays(anchorDate), [anchorDate]);

  const computed = useMemo(
    () => suggestWeeklyCoverage(weekDays, shifts, personal, options),
    [weekDays, shifts, personal, options]
  );

  const [editableProposals, setEditableProposals] = useState<SuggestedShift[]>([]);

  useEffect(() => {
    if (isOpen) {
      setEditableProposals(computed.proposals);
    }
  }, [isOpen, computed.proposals]);

  if (!isOpen) return null;

  const selectedList = editableProposals.filter((p) => p.selected);

  const toggleAll = (checked: boolean) => {
    setEditableProposals((prev) => prev.map((p) => ({ ...p, selected: checked })));
  };

  const toggleRow = (tempId: string) => {
    setEditableProposals((prev) =>
      prev.map((p) => (p.tempId === tempId ? { ...p, selected: !p.selected } : p))
    );
  };

  const updatePerson = (tempId: string, newUserId: string) => {
    const found = personal.find((u) => String(u.id) === String(newUserId));
    if (!found) return;
    setEditableProposals((prev) =>
      prev.map((p) =>
        p.tempId === tempId
          ? {
              ...p,
              id_usuario: String(found.id),
              empleado: found.nombre_completo,
              cargo: found.cargo || found.rol,
              rol: found.rol,
            }
          : p
      )
    );
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(11, 28, 22, 0.52)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 65,
        padding: '16px',
      }}
    >
      <div
        style={{
          background: '#ffffff',
          borderRadius: '18px',
          maxWidth: '960px',
          width: '100%',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 24px 40px rgba(0, 0, 0, 0.28)',
          overflow: 'hidden',
        }}
      >
        {/* Header */}
        <div
          style={{
            padding: '20px 24px',
            borderBottom: '1px solid #e2ece7',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            gap: '12px',
          }}
        >
          <div>
            <span style={{ fontSize: '0.72rem', fontWeight: 800, color: '#0b5d4b', textTransform: 'uppercase' }}>
              Planificación Asistida 24/7 (3 Turnos de 8h · Descanso mín. 16h)
            </span>
            <h3 style={{ margin: '4px 0 4px', fontSize: '1.22rem', fontWeight: 800, color: '#13221c' }}>
              ⚡ Sugerir Cobertura Semanal ({weekDays[0]} al {weekDays[6]})
            </h3>
            <p style={{ margin: 0, fontSize: '0.8rem', color: '#526b60' }}>
              Turnos de 8 horas (00–08, 08–16, 16–24) con mínimo 16h de descanso entre turnos, al menos 1 día libre semanal y cobertura de todas las especialidades en horario regular.
            </p>
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

        {/* Rule Controls */}
        <div
          style={{
            padding: '14px 24px',
            background: '#f8faf9',
            borderBottom: '1px solid #e2ece7',
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            gap: '16px',
          }}
        >
          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.78rem', fontWeight: 700, color: '#13221c' }}>
            Tope semanal:
            <select
              value={options.maxWeeklyHours}
              onChange={(e) =>
                setOptions((o) => ({ ...o, maxWeeklyHours: Number(e.target.value) }))
              }
              style={{ padding: '4px 8px', borderRadius: '6px', border: '1px solid #cad8d1', fontWeight: 700 }}
            >
              <option value={32}>32 h (4 turnos)</option>
              <option value={40}>40 h (5 turnos)</option>
              <option value={48}>48 h (6 turnos)</option>
            </select>
          </label>

          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.78rem', fontWeight: 700, color: '#13221c' }}>
            Descanso mín.:
            <select
              value={options.minRestHours}
              onChange={(e) =>
                setOptions((o) => ({ ...o, minRestHours: Number(e.target.value) }))
              }
              style={{ padding: '4px 8px', borderRadius: '6px', border: '1px solid #cad8d1', fontWeight: 700 }}
            >
              <option value={16}>16 h (2 turnos)</option>
              <option value={24}>24 h</option>
            </select>
          </label>

          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.78rem', fontWeight: 700, color: '#13221c' }}>
            Máx. noches/sem:
            <select
              value={options.maxNightsPerWeek}
              onChange={(e) =>
                setOptions((o) => ({ ...o, maxNightsPerWeek: Number(e.target.value) }))
              }
              style={{ padding: '4px 8px', borderRadius: '6px', border: '1px solid #cad8d1', fontWeight: 700 }}
            >
              <option value={2}>Máx. 2 noches</option>
              <option value={3}>Máx. 3 noches</option>
              <option value={4}>Máx. 4 noches</option>
            </select>
          </label>

          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.78rem', fontWeight: 700, color: '#13221c' }}>
            Mín. especialidad:
            <select
              value={options.minSpecialtyDays}
              onChange={(e) =>
                setOptions((o) => ({ ...o, minSpecialtyDays: Number(e.target.value) }))
              }
              style={{ padding: '4px 8px', borderRadius: '6px', border: '1px solid #cad8d1', fontWeight: 700 }}
            >
              {[1, 2, 3].map((n) => (
                <option key={n} value={n}>
                  {n} {n === 1 ? 'día/sem' : 'días/sem'}
                </option>
              ))}
            </select>
          </label>

          <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.78rem', fontWeight: 700, color: '#13221c', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={options.allowGeneralBackupForEmergencies}
              onChange={(e) =>
                setOptions((o) => ({ ...o, allowGeneralBackupForEmergencies: e.target.checked }))
              }
            />
            Refuerzo Medicina General en Emergencias
          </label>
        </div>

        {/* Body */}
        <div style={{ padding: '18px 24px', overflowY: 'auto', display: 'grid', gap: '16px' }}>
          {/* Unfilled Gaps Alert */}
          {computed.unfilledGaps.length > 0 && (
            <div
              style={{
                background: '#fef2f2',
                border: '1px solid #fecaca',
                borderRadius: '12px',
                padding: '12px 16px',
              }}
            >
              <div style={{ fontSize: '0.82rem', fontWeight: 800, color: '#991b1b', marginBottom: '6px' }}>
                ⚠ {computed.unfilledGaps.length} bloque(s) sin personal suficiente bajo las reglas laborales actuales:
              </div>
              <div style={{ fontSize: '0.76rem', color: '#7f1d1d', display: 'grid', gap: '3px', maxHeight: '110px', overflowY: 'auto' }}>
                {computed.unfilledGaps.map((g, i) => (
                  <div key={`${g.fecha}-${g.block}-${g.area}-${i}`}>
                    • <strong>{g.roleNeeded}</strong> ({g.area}): {g.reason}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Proposals Table */}
          {editableProposals.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '28px', color: '#526b60', fontSize: '0.88rem' }}>
              {computed.unfilledGaps.length === 0
                ? '✓ La semana ya cumple con toda la cobertura 24/7 y de especialidades.'
                : 'No hay propuestas adicionales disponibles bajo las restricciones actuales.'}
            </div>
          ) : (
            <div style={{ border: '1px solid #e2ece7', borderRadius: '12px', overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.82rem', textAlign: 'left' }}>
                <thead>
                  <tr style={{ background: '#f8faf9', borderBottom: '1px solid #e2ece7', color: '#526b60' }}>
                    <th style={{ padding: '10px 12px', width: '36px' }}>
                      <input
                        type="checkbox"
                        checked={selectedList.length === editableProposals.length && editableProposals.length > 0}
                        onChange={(e) => toggleAll(e.target.checked)}
                      />
                    </th>
                    <th style={{ padding: '10px 12px', fontWeight: 800 }}>Día / Bloque 8h</th>
                    <th style={{ padding: '10px 12px', fontWeight: 800 }}>Área</th>
                    <th style={{ padding: '10px 12px', fontWeight: 800 }}>Personal Sugerido</th>
                    <th style={{ padding: '10px 12px', fontWeight: 800 }}>Regla que Cubre</th>
                  </tr>
                </thead>
                <tbody>
                  {editableProposals.map((p) => {
                    const hdr = formatDayHeader(p.fecha);
                    const sameRolePool = personal.filter((u) => u.rol === p.rol);
                    const badge = BLOCK_BADGE[p.block];

                    return (
                      <tr
                        key={p.tempId}
                        style={{
                          borderBottom: '1px solid #f0f4f2',
                          background: p.selected ? '#ffffff' : '#f8faf9',
                          opacity: p.selected ? 1 : 0.55,
                        }}
                      >
                        <td style={{ padding: '10px 12px' }}>
                          <input
                            type="checkbox"
                            checked={p.selected}
                            onChange={() => toggleRow(p.tempId)}
                          />
                        </td>
                        <td style={{ padding: '10px 12px', whiteSpace: 'nowrap' }}>
                          <div style={{ fontWeight: 800, color: '#13221c' }}>
                            {hdr.dayName} {hdr.dayNum} {hdr.monthShort}
                          </div>
                          <span
                            style={{
                              display: 'inline-block',
                              marginTop: '2px',
                              padding: '1px 7px',
                              borderRadius: '999px',
                              fontSize: '0.7rem',
                              fontWeight: 700,
                              background: badge.bg,
                              color: badge.color,
                            }}
                          >
                            {badge.label}
                          </span>
                        </td>
                        <td style={{ padding: '10px 12px', fontWeight: 700, color: '#13221c' }}>{p.area}</td>
                        <td style={{ padding: '10px 12px' }}>
                          <select
                            value={p.id_usuario}
                            onChange={(e) => updatePerson(p.tempId, e.target.value)}
                            style={{
                              padding: '5px 8px',
                              borderRadius: '7px',
                              border: '1px solid #cad8d1',
                              fontSize: '0.8rem',
                              fontWeight: 700,
                              color: '#13221c',
                              background: '#ffffff',
                            }}
                          >
                            {sameRolePool.map((u) => (
                              <option key={u.id} value={u.id}>
                                {u.nombre_completo} ({u.cargo || u.rol})
                              </option>
                            ))}
                          </select>
                        </td>
                        <td style={{ padding: '10px 12px' }}>
                          <span
                            style={{
                              padding: '3px 8px',
                              borderRadius: '6px',
                              fontSize: '0.72rem',
                              fontWeight: 700,
                              background: p.isBackup ? '#fef3c7' : '#eaf5f1',
                              color: p.isBackup ? '#92400e' : '#073f33',
                            }}
                          >
                            {p.reason}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Footer */}
        <div
          style={{
            padding: '16px 24px',
            borderTop: '1px solid #e2ece7',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: '12px',
            background: '#fcfdfd',
          }}
        >
          <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#526b60' }}>
            {selectedList.length} turno(s) de 8h listos para asignar
          </span>
          <div style={{ display: 'flex', gap: '10px' }}>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              style={{
                padding: '9px 16px',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                background: '#ffffff',
                fontWeight: 700,
                fontSize: '0.84rem',
                cursor: 'pointer',
              }}
            >
              Cancelar
            </button>
            <button
              type="button"
              disabled={isSubmitting || selectedList.length === 0}
              onClick={() => onConfirmSave(selectedList)}
              style={{
                padding: '9px 20px',
                borderRadius: '8px',
                border: 'none',
                background: selectedList.length === 0 ? '#94a3b8' : '#0b5d4b',
                color: '#ffffff',
                fontWeight: 800,
                fontSize: '0.84rem',
                cursor: selectedList.length === 0 ? 'not-allowed' : 'pointer',
              }}
            >
              {isSubmitting ? 'Guardando turnos...' : `Aprobar y Guardar (${selectedList.length})`}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
