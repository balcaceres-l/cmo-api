export default function Ambulancia() {
  return (
    <div>
      <h2 style={{ fontSize: '1.5rem', marginBottom: '8px' }}>Recurso Crítico — Ambulancia</h2>
      <p style={{ color: '#4a5c53', marginBottom: '24px' }}>
        Despacho en tiempo real, monitoreo de estado y bitácora histórica de emergencias.
      </p>

      <div style={{ background: '#fff', padding: '24px', borderRadius: '8px', border: '1px solid #d1dcd6', marginBottom: '24px' }}>
        <h3 style={{ margin: '0 0 12px 0', fontSize: '1.2rem' }}>Estado Actual de la Unidad Única</h3>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <span style={{ padding: '6px 14px', borderRadius: '16px', background: '#d9efe1', color: '#1a7541', fontWeight: 600 }}>
            DISPONIBLE
          </span>
          <span style={{ color: '#4a5c53', fontSize: '0.9rem' }}>Placa: AMB-01 · Lista para despacho</span>
        </div>
      </div>

      <section style={{ background: '#fff', padding: '24px', borderRadius: '8px', border: '1px solid #d1dcd6' }}>
        <h3 style={{ margin: '0 0 16px 0', fontSize: '1.2rem' }}>Bitácora de Emergencias y Servicios</h3>
        <p style={{ fontSize: '0.85rem', color: '#4a5c53' }}>Filtro por fecha, estado y prioridad.</p>
        <div style={{ marginTop: '16px', color: '#4a5c53', fontStyle: 'italic' }}>
          No hay servicios en curso en este momento.
        </div>
      </section>
    </div>
  );
}
