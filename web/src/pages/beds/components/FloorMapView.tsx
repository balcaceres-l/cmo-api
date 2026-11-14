import React from 'react';
import { BedData, RoomGroup, formatStayDuration, getStayHours } from '../types';

interface FloorMapViewProps {
  rooms: RoomGroup[];
  selectedBedId?: string | null;
  onSelectBed: (bed: BedData) => void;
  isOps: boolean;
}

export const FloorMapView: React.FC<FloorMapViewProps> = ({
  rooms,
  selectedBedId,
  onSelectBed,
  isOps,
}) => {
  const p3Rooms = rooms.filter((r) => r.planta === 3);
  const p2Rooms = rooms.filter((r) => r.planta === 2);

  if (rooms.length === 0) {
    return (
      <div
        style={{
          background: '#ffffff',
          borderRadius: '14px',
          border: '1px solid #e2ece7',
          padding: '40px 20px',
          textAlign: 'center',
          color: '#526b60',
          fontSize: '0.9rem',
        }}
      >
        No se encontraron camas o habitaciones que coincidan con los filtros seleccionados.
      </div>
    );
  }

  const renderBedSlot = (bed: BedData, compactLabel?: string) => {
    const isOcupada = bed.estado === 'OCUPADA';
    const isSelected = selectedBedId === bed.id;
    const stayHours = isOcupada ? getStayHours(bed.paciente?.fechaIngresoIso) : 0;
    const isLongStay = isOcupada && stayHours >= 48;

    return (
      <button
        key={bed.id}
        type="button"
        onClick={() => onSelectBed(bed)}
        style={{
          textAlign: 'left',
          width: '100%',
          background: isSelected
            ? isOps
              ? '#f0f9ff'
              : '#fffbeb'
            : isOcupada
              ? '#fffdf7'
              : '#f8fbf9',
          borderRadius: '10px',
          border: isSelected
            ? `2px solid ${isOps ? '#0369a1' : '#b45309'}`
            : `1px solid ${isOcupada ? '#fde68a' : '#d8e6df'}`,
          padding: '12px',
          cursor: 'pointer',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          gap: '8px',
          transition: 'all 0.15s ease',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
          <span
            style={{
              fontFamily: 'monospace',
              fontSize: '0.82rem',
              fontWeight: 800,
              color: isOcupada ? '#92400e' : '#0b5d4b',
              background: isOcupada ? '#fef3c7' : '#e6f4ea',
              padding: '2px 8px',
              borderRadius: '6px',
            }}
          >
            {compactLabel || bed.codigo}
          </span>

          <span
            style={{
              fontSize: '0.7rem',
              fontWeight: 800,
              padding: '2px 7px',
              borderRadius: '999px',
              background: isOcupada ? '#fef3c7' : '#ecfdf5',
              color: isOcupada ? '#b45309' : '#047857',
            }}
          >
            {isOcupada ? '● OCUPADA' : '○ LIBRE'}
          </span>
        </div>

        {isOcupada && bed.paciente ? (
          <div>
            <div
              style={{
                fontWeight: 700,
                fontSize: '0.86rem',
                color: '#13221c',
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
            >
              {bed.paciente.nombre}
            </div>
            <div style={{ fontSize: '0.73rem', color: '#526b60', marginTop: '2px' }}>
              {bed.paciente.medico || 'Médico de turno'}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '6px', flexWrap: 'wrap' }}>
              <span
                style={{
                  fontSize: '0.69rem',
                  fontWeight: 700,
                  color: isLongStay ? '#b45309' : '#0369a1',
                  background: isLongStay ? '#fef3c7' : '#e0f2fe',
                  padding: '2px 6px',
                  borderRadius: '4px',
                }}
              >
                Estancia: {formatStayDuration(bed.paciente.fechaIngresoIso)}
              </span>
              {isLongStay && (
                <span style={{ fontSize: '0.68rem', fontWeight: 800, color: '#b45309' }}>
                  +48h
                </span>
              )}
            </div>
          </div>
        ) : (
          <div style={{ fontSize: '0.76rem', color: '#6b8278' }}>
            {isOps ? 'Disponible · Clic para asignar o ver' : 'Disponible · Lista para ingreso'}
          </div>
        )}
      </button>
    );
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* PLANTA 3 - VIP */}
      {p3Rooms.length > 0 && (
        <section
          style={{
            background: '#ffffff',
            borderRadius: '14px',
            border: '1px solid #e2ece7',
            padding: '18px',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '14px',
              flexWrap: 'wrap',
              gap: '8px',
            }}
          >
            <div>
              <h2 style={{ margin: 0, fontSize: '1rem', fontWeight: 800, color: '#13221c' }}>
                Planta 3 — Área VIP (Habitaciones Individuales)
              </h2>
              <p style={{ margin: '2px 0 0', fontSize: '0.78rem', color: '#526b60' }}>
                5 suites individuales de aislamiento y recuperación (VIP-301 a VIP-305).
              </p>
            </div>
            <span
              style={{
                fontSize: '0.75rem',
                fontWeight: 700,
                padding: '4px 10px',
                borderRadius: '999px',
                background: '#f0f4f2',
                color: '#13221c',
              }}
            >
              {p3Rooms.flatMap((r) => r.camas).filter((c) => c.estado === 'OCUPADA').length} /{' '}
              {p3Rooms.flatMap((r) => r.camas).length} ocupadas
            </span>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
              gap: '12px',
            }}
          >
            {p3Rooms.map((room) =>
              room.camas.map((bed) => renderBedSlot(bed, bed.codigo))
            )}
          </div>
        </section>
      )}

      {/* PLANTA 2 - SEMIPRIVADAS */}
      {p2Rooms.length > 0 && (
        <section
          style={{
            background: '#ffffff',
            borderRadius: '14px',
            border: '1px solid #e2ece7',
            padding: '18px',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '14px',
              flexWrap: 'wrap',
              gap: '8px',
            }}
          >
            <div>
              <h2 style={{ margin: 0, fontSize: '1rem', fontWeight: 800, color: '#13221c' }}>
                Planta 2 — Área Semiprivada (Habitaciones Dobles)
              </h2>
              <p style={{ margin: '2px 0 0', fontSize: '0.78rem', color: '#526b60' }}>
                6 habitaciones compartidas (HAB-201 a HAB-206), 2 camas por habitación (A y B).
              </p>
            </div>
            <span
              style={{
                fontSize: '0.75rem',
                fontWeight: 700,
                padding: '4px 10px',
                borderRadius: '999px',
                background: '#f0f4f2',
                color: '#13221c',
              }}
            >
              {p2Rooms.flatMap((r) => r.camas).filter((c) => c.estado === 'OCUPADA').length} /{' '}
              {p2Rooms.flatMap((r) => r.camas).length} ocupadas
            </span>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))',
              gap: '14px',
            }}
          >
            {p2Rooms.map((room) => {
              const occCount = room.camas.filter((c) => c.estado === 'OCUPADA').length;
              const isPartial = occCount === 1 && room.camas.length === 2;
              const isFull = occCount === room.camas.length && room.camas.length > 0;

              return (
                <div
                  key={room.codigo}
                  style={{
                    borderRadius: '12px',
                    border: `1px solid ${isPartial ? '#bae6fd' : '#e2ece7'}`,
                    background: '#fcfdfd',
                    padding: '12px',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '10px',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span style={{ fontWeight: 800, fontSize: '0.86rem', color: '#13221c' }}>
                      Habitación {room.codigo}
                    </span>
                    <span
                      style={{
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        padding: '2px 8px',
                        borderRadius: '999px',
                        background: isFull ? '#fef3c7' : isPartial ? '#e0f2fe' : '#ecfdf5',
                        color: isFull ? '#b45309' : isPartial ? '#0369a1' : '#047857',
                      }}
                    >
                      {isFull ? 'Completa (2/2)' : isPartial ? 'Parcial (1/2)' : 'Disponible (0/2)'}
                    </span>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                    {room.camas.map((bed) =>
                      renderBedSlot(bed, bed.letra ? `Cama ${bed.letra}` : bed.codigo)
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}
    </div>
  );
};
