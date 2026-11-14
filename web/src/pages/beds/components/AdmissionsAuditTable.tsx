import React, { useState } from 'react';
import { IngresoRecord, formatStayDuration } from '../types';
import { FileTextIcon, SearchIcon } from '../../../components/Icons';

interface AdmissionsAuditTableProps {
  ingresos: IngresoRecord[];
  isOps: boolean;
}

export const AdmissionsAuditTable: React.FC<AdmissionsAuditTableProps> = ({ ingresos, isOps }) => {
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVOS' | 'ALTAS'>('ALL');
  const [query, setQuery] = useState('');

  const filtered = ingresos.filter((ing) => {
    const isActivo = !ing.fecha_alta;
    if (statusFilter === 'ACTIVOS' && !isActivo) return false;
    if (statusFilter === 'ALTAS' && isActivo) return false;

    if (!query.trim()) return true;
    const q = query.toLowerCase();
    const cama = ing.cama?.codigo?.toLowerCase() || '';
    const paciente = ing.paciente?.nombre_completo?.toLowerCase() || '';
    const exp = ing.paciente?.numero_expediente?.toLowerCase() || '';
    const medico = ing.medico?.nombre_completo?.toLowerCase() || '';
    const registrado = ing.registrado_por?.nombre_completo?.toLowerCase() || '';
    return (
      cama.includes(q) ||
      paciente.includes(q) ||
      exp.includes(q) ||
      medico.includes(q) ||
      registrado.includes(q)
    );
  });

  const handleExportCsv = () => {
    const headers = [
      'Cama',
      'Planta',
      'Tipo',
      'Paciente',
      'Expediente',
      'Medico Tratante',
      'Motivo',
      'Fecha Ingreso',
      'Fecha Alta',
      'Estancia',
      'Estado',
      'Registrado Por',
    ];

    const escapeCsv = (val: string) => `"${(val || '').replace(/"/g, '""')}"`;

    const rows = filtered.map((ing) => [
      ing.cama?.codigo || 'N/A',
      ing.cama?.habitacion?.planta ? `Planta ${ing.cama.habitacion.planta}` : 'N/A',
      ing.cama?.habitacion?.tipo || 'N/A',
      ing.paciente?.nombre_completo || 'N/A',
      ing.paciente?.numero_expediente || 'N/A',
      ing.medico?.nombre_completo || 'N/A',
      ing.motivo || '',
      ing.fecha_ingreso ? new Date(ing.fecha_ingreso).toLocaleString() : '',
      ing.fecha_alta ? new Date(ing.fecha_alta).toLocaleString() : 'Activo',
      formatStayDuration(ing.fecha_ingreso, ing.fecha_alta),
      ing.fecha_alta ? 'ALTA' : 'ACTIVO',
      ing.registrado_por?.nombre_completo || 'Sistema',
    ]);

    const csvContent =
      '\uFEFF' +
      [headers.map(escapeCsv).join(','), ...rows.map((r) => r.map(escapeCsv).join(','))].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `censo-auditoria-camas-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div
      style={{
        background: '#ffffff',
        borderRadius: '14px',
        border: '1px solid #e2ece7',
        padding: '18px',
        display: 'flex',
        flexDirection: 'column',
        gap: '14px',
      }}
    >
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '12px',
        }}
      >
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          {[
            { id: 'ALL', label: `Todos (${ingresos.length})` },
            { id: 'ACTIVOS', label: `Activos (${ingresos.filter((i) => !i.fecha_alta).length})` },
            { id: 'ALTAS', label: `Altas (${ingresos.filter((i) => !!i.fecha_alta).length})` },
          ].map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setStatusFilter(tab.id as any)}
              style={{
                padding: '6px 12px',
                borderRadius: '8px',
                border: 'none',
                fontSize: '0.78rem',
                fontWeight: 700,
                cursor: 'pointer',
                background: statusFilter === tab.id ? '#0b5d4b' : '#f0f4f2',
                color: statusFilter === tab.id ? '#ffffff' : '#526b60',
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ position: 'relative', minWidth: '240px' }}>
            <div style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#849a90' }}>
              <SearchIcon size={15} />
            </div>
            <input
              type="text"
              placeholder="Filtrar paciente, cama, médico o responsable..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              style={{
                width: '100%',
                padding: '7px 12px 7px 32px',
                borderRadius: '8px',
                border: '1px solid #cad8d1',
                fontSize: '0.82rem',
              }}
            />
          </div>

          {!isOps && (
            <button
              type="button"
              onClick={handleExportCsv}
              disabled={filtered.length === 0}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                padding: '7px 14px',
                borderRadius: '8px',
                border: '1px solid #fde68a',
                background: '#fef3c7',
                color: '#92400e',
                fontSize: '0.8rem',
                fontWeight: 700,
                cursor: filtered.length === 0 ? 'not-allowed' : 'pointer',
              }}
            >
              <FileTextIcon size={15} /> Exportar Auditoría CSV
            </button>
          )}
        </div>
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.82rem' }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #e2ece7', textAlign: 'left', color: '#526b60', fontSize: '0.74rem', textTransform: 'uppercase' }}>
              <th style={{ padding: '10px 8px' }}>Cama</th>
              <th style={{ padding: '10px 8px' }}>Paciente</th>
              <th style={{ padding: '10px 8px' }}>Médico Tratante</th>
              <th style={{ padding: '10px 8px' }}>Motivo</th>
              <th style={{ padding: '10px 8px' }}>Ingreso / Estancia</th>
              <th style={{ padding: '10px 8px' }}>Estado</th>
              <th style={{ padding: '10px 8px' }}>Registrado por</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ padding: '24px', textAlign: 'center', color: '#849a90' }}>
                  No hay registros de ingresos para el filtro actual.
                </td>
              </tr>
            ) : (
              filtered.map((ing) => {
                const isActivo = !ing.fecha_alta;
                return (
                  <tr key={ing.id_ingreso} style={{ borderBottom: '1px solid #f0f4f2' }}>
                    <td style={{ padding: '10px 8px', fontFamily: 'monospace', fontWeight: 800, color: '#13221c' }}>
                      {ing.cama?.codigo || '—'}
                    </td>
                    <td style={{ padding: '10px 8px' }}>
                      <div style={{ fontWeight: 700, color: '#13221c' }}>
                        {ing.paciente?.nombre_completo || 'Paciente'}
                      </div>
                      <div style={{ fontSize: '0.73rem', color: '#849a90' }}>
                        {ing.paciente?.numero_expediente || ''}
                      </div>
                    </td>
                    <td style={{ padding: '10px 8px', color: '#0b5d4b', fontWeight: 600 }}>
                      {ing.medico?.nombre_completo || '—'}
                    </td>
                    <td style={{ padding: '10px 8px', color: '#526b60', maxWidth: '220px' }}>
                      {ing.motivo}
                    </td>
                    <td style={{ padding: '10px 8px' }}>
                      <div style={{ color: '#13221c' }}>
                        {ing.fecha_ingreso ? new Date(ing.fecha_ingreso).toLocaleString() : '—'}
                      </div>
                      <div style={{ fontSize: '0.73rem', color: '#0369a1', fontWeight: 700 }}>
                        Duración: {formatStayDuration(ing.fecha_ingreso, ing.fecha_alta)}
                      </div>
                    </td>
                    <td style={{ padding: '10px 8px' }}>
                      <span
                        style={{
                          padding: '3px 8px',
                          borderRadius: '999px',
                          fontSize: '0.7rem',
                          fontWeight: 800,
                          background: isActivo ? '#fef3c7' : '#ecfdf5',
                          color: isActivo ? '#b45309' : '#047857',
                        }}
                      >
                        {isActivo ? 'ACTIVO' : 'ALTA'}
                      </span>
                    </td>
                    <td style={{ padding: '10px 8px', color: '#475569' }}>
                      {ing.registrado_por?.nombre_completo || 'Sistema'}
                      {ing.registrado_por?.cargo ? (
                        <div style={{ fontSize: '0.7rem', color: '#849a90' }}>{ing.registrado_por.cargo}</div>
                      ) : null}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
