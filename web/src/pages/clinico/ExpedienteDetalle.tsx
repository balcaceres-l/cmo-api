export default function ExpedienteDetalle() {
  return (
    <div>
      <div style={{ marginBottom: '20px' }}>
        <button style={{ background: 'none', border: 'none', color: '#0e5c86', cursor: 'pointer', fontSize: '0.9rem', marginBottom: '8px' }}>
          ← Volver a Pacientes
        </button>
        <h2 style={{ fontSize: '1.5rem', margin: '0 0 6px 0' }}>Expediente Clínico: EXP-2026-000001</h2>
        <p style={{ color: '#4a5c53', margin: 0 }}>
          Paciente Uno · Modo de lectura protegido para administración y médico tratante.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '16px' }}>
        <div style={{ background: '#fff', borderRadius: '8px', border: '1px solid #d1dcd6', padding: '20px' }}>
          <h3 style={{ fontSize: '1.1rem', margin: '0 0 12px 0' }}>Datos Personales</h3>
          <p style={{ fontSize: '0.9rem', color: '#4a5c53', margin: '4px 0' }}><b>DUI:</b> 01234567-8</p>
          <p style={{ fontSize: '0.9rem', color: '#4a5c53', margin: '4px 0' }}><b>Fecha de Nacimiento:</b> 1990-01-01</p>
          <p style={{ fontSize: '0.9rem', color: '#4a5c53', margin: '4px 0' }}><b>Sexo:</b> Femenino</p>
        </div>

        <div style={{ background: '#fff', borderRadius: '8px', border: '1px solid #d1dcd6', padding: '20px' }}>
          <h3 style={{ fontSize: '1.1rem', margin: '0 0 12px 0' }}>Historial Clínico</h3>
          <p style={{ fontSize: '0.9rem', color: '#4a5c53', margin: '4px 0' }}><b>Tipo de Sangre:</b> O+</p>
          <p style={{ fontSize: '0.9rem', color: '#4a5c53', margin: '4px 0' }}><b>Alergias:</b> Penicilina</p>
          <p style={{ fontSize: '0.9rem', color: '#4a5c53', margin: '4px 0' }}><b>Padecimientos:</b> Ninguno</p>
        </div>
      </div>
    </div>
  );
}
