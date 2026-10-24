export default function Avisos() {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', margin: '0 0 6px 0' }}>Avisos y Comunicados</h2>
          <p style={{ color: '#4a5c53', margin: 0 }}>
            Publicaciones institucionales visibles para todo el personal y pacientes.
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
          + Nuevo Aviso
        </button>
      </div>

      <div style={{ background: '#fff', borderRadius: '8px', border: '1px solid #d1dcd6', padding: '24px' }}>
        <p style={{ color: '#4a5c53', margin: 0 }}>
          No hay avisos urgentes activos actualmente.
        </p>
      </div>
    </div>
  );
}
