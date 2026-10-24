export default function Pacientes() {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', margin: '0 0 6px 0' }}>Directorio de Pacientes</h2>
          <p style={{ color: '#4a5c53', margin: 0 }}>
            Búsqueda, registro y consulta de pacientes con expediente clínico y DUI.
          </p>
        </div>
      </div>

      <div style={{ background: '#fff', borderRadius: '8px', border: '1px solid #d1dcd6', padding: '20px', marginBottom: '16px' }}>
        <input
          type="text"
          placeholder="Buscar paciente por nombre, DUI o número de expediente..."
          style={{ width: '100%', padding: '10px 14px', borderRadius: '6px', border: '1px solid #d1dcd6', fontSize: '0.95rem' }}
        />
      </div>

      <div style={{ background: '#fff', borderRadius: '8px', border: '1px solid #d1dcd6', overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
          <thead style={{ background: '#e8efeb', color: '#13221c' }}>
            <tr>
              <th style={{ padding: '12px 16px' }}>Expediente</th>
              <th style={{ padding: '12px 16px' }}>Nombre Completo</th>
              <th style={{ padding: '12px 16px' }}>DUI</th>
              <th style={{ padding: '12px 16px' }}>Sexo</th>
              <th style={{ padding: '12px 16px' }}>Acción</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td style={{ padding: '12px 16px', borderTop: '1px solid #d1dcd6', fontWeight: 600 }}>EXP-2026-000001</td>
              <td style={{ padding: '12px 16px', borderTop: '1px solid #d1dcd6' }}>Paciente Uno</td>
              <td style={{ padding: '12px 16px', borderTop: '1px solid #d1dcd6' }}>01234567-8</td>
              <td style={{ padding: '12px 16px', borderTop: '1px solid #d1dcd6' }}>Femenino</td>
              <td style={{ padding: '12px 16px', borderTop: '1px solid #d1dcd6' }}>
                <button style={{ background: 'none', border: 'none', color: '#0e5c86', cursor: 'pointer', fontWeight: 600 }}>
                  Ver Expediente
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
