export default function Usuarios() {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', margin: '0 0 6px 0' }}>Gestión de Personal</h2>
          <p style={{ color: '#4a5c53', margin: 0 }}>
            CRUD de doctores, recepcionistas, asistentes, enfermeros y administradores.
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
          + Nuevo Usuario
        </button>
      </div>

      <div style={{ background: '#fff', borderRadius: '8px', border: '1px solid #d1dcd6', overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
          <thead style={{ background: '#e8efeb', color: '#13221c' }}>
            <tr>
              <th style={{ padding: '12px 16px' }}>Nombre</th>
              <th style={{ padding: '12px 16px' }}>Usuario</th>
              <th style={{ padding: '12px 16px' }}>Rol</th>
              <th style={{ padding: '12px 16px' }}>Cargo / Especialidad</th>
              <th style={{ padding: '12px 16px' }}>Estado</th>
              <th style={{ padding: '12px 16px' }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td style={{ padding: '12px 16px', borderTop: '1px solid #d1dcd6' }}>Administrador General</td>
              <td style={{ padding: '12px 16px', borderTop: '1px solid #d1dcd6' }}>admin</td>
              <td style={{ padding: '12px 16px', borderTop: '1px solid #d1dcd6' }}><span style={{ padding: '3px 8px', borderRadius: '4px', background: '#e8efeb', fontSize: '0.8rem' }}>ADMINISTRADOR</span></td>
              <td style={{ padding: '12px 16px', borderTop: '1px solid #d1dcd6' }}>Administración</td>
              <td style={{ padding: '12px 16px', borderTop: '1px solid #d1dcd6', color: '#1a7541', fontWeight: 600 }}>ACTIVO</td>
              <td style={{ padding: '12px 16px', borderTop: '1px solid #d1dcd6' }}><button style={{ background: 'none', border: 'none', color: '#0e5c86', cursor: 'pointer' }}>Editar</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
