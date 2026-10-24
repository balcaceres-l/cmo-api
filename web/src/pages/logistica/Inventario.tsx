export default function Inventario() {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', margin: '0 0 6px 0' }}>Control de Recursos e Inventarios</h2>
          <p style={{ color: '#4a5c53', margin: 0 }}>
            Administración de stock para medicamentos, insumos médicos y equipos.
          </p>
        </div>
        <button
          style={{
            padding: '10px 18px',
            background: '#0b5d4b',
            color: '#fff',
            border: 'none',
            borderRadius: '6px',
            fontWeight: 600,
            cursor: 'pointer'
          }}
        >
          + Nuevo Recurso
        </button>
      </div>

      <div style={{ background: '#fff', borderRadius: '8px', border: '1px solid #d1dcd6', padding: '24px' }}>
        <p style={{ color: '#4a5c53', margin: 0 }}>
          Listado de medicamentos, insumos y equipos con alertas de stock bajo el mínimo.
        </p>
      </div>
    </div>
  );
}
