import React from 'react';
import { SearchIcon } from '../../../components/Icons';
import { ShiftViewMode, TurnoRaw, WeekGroupBy } from '../types';
import { formatRangeTitle } from '../shiftUtils';

interface ShiftToolbarProps {
  viewMode: ShiftViewMode;
  onViewModeChange: (mode: ShiftViewMode) => void;
  anchorDate: string;
  todayStr: string;
  onNavigate: (direction: 'prev' | 'next' | 'today') => void;
  weekGroupBy: WeekGroupBy;
  onWeekGroupByChange: (group: WeekGroupBy) => void;
  filterRol: string;
  onFilterRolChange: (rol: string) => void;
  searchQuery: string;
  onSearchQueryChange: (q: string) => void;
  visibleShifts: TurnoRaw[];
  emergenciasUncoveredDays: number;
}

const VIEW_LABELS: { id: ShiftViewMode; label: string }[] = [
  { id: 'day', label: 'Día' },
  { id: 'week', label: 'Semana' },
  { id: 'month', label: 'Mes' },
  { id: 'list', label: 'Lista' },
];

export const ShiftToolbar: React.FC<ShiftToolbarProps> = ({
  viewMode,
  onViewModeChange,
  anchorDate,
  todayStr,
  onNavigate,
  weekGroupBy,
  onWeekGroupByChange,
  filterRol,
  onFilterRolChange,
  searchQuery,
  onSearchQueryChange,
  visibleShifts,
  emergenciasUncoveredDays,
}) => {
  const medicosCount = visibleShifts.filter((s) => s.rol === 'MEDICO').length;
  const apoyoCount = visibleShifts.filter((s) => s.rol !== 'MEDICO').length;

  return (
    <div
      style={{
        background: '#ffffff',
        borderRadius: '16px',
        border: '1px solid #e2ece7',
        padding: '16px 20px',
        display: 'grid',
        gap: '14px',
        boxShadow: '0 2px 6px rgba(11, 44, 35, 0.04)',
      }}
    >
      {/* Top Row: View Switcher + Date Navigation + Group Toggle */}
      <div className="shift-toolbar-top">
        {/* Segmented View Switcher */}
        <div
          style={{
            justifySelf: 'start',
            display: 'inline-flex',
            background: '#f0f4f2',
            padding: '4px',
            borderRadius: '10px',
            border: '1px solid #e2ece7',
            gap: '2px',
          }}
        >
          {VIEW_LABELS.map((v) => {
            const active = viewMode === v.id;
            return (
              <button
                key={v.id}
                type="button"
                onClick={() => onViewModeChange(v.id)}
                style={{
                  padding: '6px 14px',
                  borderRadius: '7px',
                  border: 'none',
                  fontSize: '0.82rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  background: active ? '#0b5d4b' : 'transparent',
                  color: active ? '#ffffff' : '#526b60',
                  boxShadow: active ? '0 1px 3px rgba(0,0,0,0.12)' : 'none',
                  transition: 'all 0.15s ease',
                }}
              >
                {v.label}
              </button>
            );
          })}
        </div>

        {/* Date Navigation: mantiene su posición central al cambiar entre Día, Semana y Mes */}
        {viewMode !== 'list' ? (
          <div style={{ justifySelf: 'center', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', flexWrap: 'wrap' }}>
            {anchorDate !== todayStr && (
              <button
                type="button"
                onClick={() => onNavigate('today')}
                style={{
                  padding: '5px 11px',
                  borderRadius: '999px',
                  border: '1px solid #a7f3d0',
                  background: '#eaf5f1',
                  color: '#0b5d4b',
                  fontSize: '0.76rem',
                  fontWeight: 800,
                  cursor: 'pointer',
                }}
              >
                ↺ Ir a hoy
              </button>
            )}
            <span
              style={{
                fontSize: '0.96rem',
                fontWeight: 800,
                color: '#13221c',
                minWidth: 'clamp(190px, 22vw, 250px)',
                textAlign: 'center',
              }}
            >
              {formatRangeTitle(viewMode, anchorDate)}
            </span>
            <div style={{ display: 'inline-flex', borderRadius: '8px', border: '1px solid #cad8d1', overflow: 'hidden', flexShrink: 0 }}>
              <button
                type="button"
                onClick={() => onNavigate('prev')}
                title="Período anterior"
                style={{
                  padding: '6px 12px',
                  border: 'none',
                  background: '#ffffff',
                  color: '#13221c',
                  fontWeight: 800,
                  cursor: 'pointer',
                  borderRight: '1px solid #e2ece7',
                }}
              >
                ‹
              </button>
              <button
                type="button"
                onClick={() => onNavigate('next')}
                title="Período siguiente"
                style={{
                  padding: '6px 12px',
                  border: 'none',
                  background: '#ffffff',
                  color: '#13221c',
                  fontWeight: 800,
                  cursor: 'pointer',
                }}
              >
                ›
              </button>
            </div>
          </div>
        ) : (
          <div aria-hidden="true" />
        )}

        {/* Group By Toggle for Week View (reserva espacio en desktop para no desplazar la barra) */}
        <div
          className={viewMode === 'week' ? undefined : 'shift-toolbar-group--hidden'}
          aria-hidden={viewMode !== 'week'}
          style={{ justifySelf: 'end', display: 'flex', alignItems: 'center', gap: '6px' }}
        >
          <span style={{ fontSize: '0.76rem', fontWeight: 700, color: '#526b60' }}>Agrupar:</span>
          <div
            style={{
              display: 'inline-flex',
              background: '#f8faf9',
              border: '1px solid #cad8d1',
              borderRadius: '8px',
              padding: '2px',
            }}
          >
            <button
              type="button"
              tabIndex={viewMode === 'week' ? 0 : -1}
              onClick={() => onWeekGroupByChange('area')}
              style={{
                padding: '4px 10px',
                borderRadius: '6px',
                border: 'none',
                fontSize: '0.76rem',
                fontWeight: 700,
                cursor: 'pointer',
                background: weekGroupBy === 'area' ? '#073f33' : 'transparent',
                color: weekGroupBy === 'area' ? '#ffffff' : '#526b60',
              }}
            >
              Por Área
            </button>
            <button
              type="button"
              tabIndex={viewMode === 'week' ? 0 : -1}
              onClick={() => onWeekGroupByChange('especialidad')}
              style={{
                padding: '4px 10px',
                borderRadius: '6px',
                border: 'none',
                fontSize: '0.76rem',
                fontWeight: 700,
                cursor: 'pointer',
                background: weekGroupBy === 'especialidad' ? '#073f33' : 'transparent',
                color: weekGroupBy === 'especialidad' ? '#ffffff' : '#526b60',
              }}
            >
              Por Especialidad
            </button>
            <button
              type="button"
              tabIndex={viewMode === 'week' ? 0 : -1}
              onClick={() => onWeekGroupByChange('franja')}
              style={{
                padding: '4px 10px',
                borderRadius: '6px',
                border: 'none',
                fontSize: '0.76rem',
                fontWeight: 700,
                cursor: 'pointer',
                background: weekGroupBy === 'franja' ? '#073f33' : 'transparent',
                color: weekGroupBy === 'franja' ? '#ffffff' : '#526b60',
              }}
            >
              Por Franja (8h)
            </button>
          </div>
        </div>
      </div>

      {/* Bottom Row: Role Filters + Search + Period Distribution Summary & Legend */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '12px',
          paddingTop: '10px',
          borderTop: '1px solid #f0f4f2',
        }}
      >
        <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', alignItems: 'center' }}>
          {(['TODOS', 'MEDICO', 'RECEPCIONISTA'] as const).map((rol) => (
            <button
              key={rol}
              type="button"
              onClick={() => onFilterRolChange(rol)}
              style={{
                padding: '6px 12px',
                borderRadius: '8px',
                border: 'none',
                fontSize: '0.78rem',
                fontWeight: 700,
                cursor: 'pointer',
                background:
                  filterRol === rol
                    ? rol === 'MEDICO'
                      ? '#0284c7'
                      : rol === 'RECEPCIONISTA'
                      ? '#0b5d4b'
                      : '#073f33'
                    : '#f0f4f2',
                color: filterRol === rol ? '#ffffff' : '#526b60',
              }}
            >
              {rol === 'TODOS' ? 'Todo el Personal' : rol === 'MEDICO' ? 'Médicos' : 'Recepción / Enfermería'}
            </button>
          ))}

          {/* Period distribution pills */}
          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginLeft: '6px' }}>
            <span
              style={{
                background: '#f0f9ff',
                color: '#0369a1',
                border: '1px solid #bae6fd',
                fontSize: '0.74rem',
                fontWeight: 700,
                padding: '4px 9px',
                borderRadius: '999px',
              }}
            >
              🩺 {medicosCount} Médicos
            </span>
            <span
              style={{
                background: '#eaf5f1',
                color: '#0b5d4b',
                border: '1px solid #a7f3d0',
                fontSize: '0.74rem',
                fontWeight: 700,
                padding: '4px 9px',
                borderRadius: '999px',
              }}
            >
              💉 {apoyoCount} Enfermería/Recep.
            </span>
            {viewMode !== 'list' && emergenciasUncoveredDays > 0 && (
              <span
                style={{
                  background: '#fef2f2',
                  color: '#b91c1c',
                  border: '1px solid #fecaca',
                  fontSize: '0.74rem',
                  fontWeight: 800,
                  padding: '4px 9px',
                  borderRadius: '999px',
                }}
              >
                ⚠ {emergenciasUncoveredDays} {emergenciasUncoveredDays === 1 ? 'día' : 'días'} sin Emergencias
              </span>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          {/* Compact Legend */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.72rem', color: '#526b60', fontWeight: 600 }}>
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
              <span style={{ width: '8px', height: '8px', borderRadius: '2px', background: '#0284c7' }} />
              Médico
            </span>
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
              <span style={{ width: '8px', height: '8px', borderRadius: '2px', background: '#0b5d4b' }} />
              Enfermería
            </span>
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
              <span style={{ width: '8px', height: '8px', borderRadius: '2px', background: '#d97706' }} />
              Relevo
            </span>
          </div>

          <div style={{ position: 'relative', minWidth: '220px' }}>
            <div style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#849a90' }}>
              <SearchIcon size={15} />
            </div>
            <input
              type="text"
              placeholder="Buscar persona o área..."
              value={searchQuery}
              onChange={(e) => onSearchQueryChange(e.target.value)}
              style={{
                width: '100%',
                padding: '7px 10px 7px 32px',
                borderRadius: '8px',
                border: '1px solid #cad8d1',
                fontSize: '0.82rem',
                boxSizing: 'border-box',
              }}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

