export default function Turnos() {
  return (
    <div>
      <h2 style={{ fontSize: '1.5rem', marginBottom: '8px' }}>Control de Turnos del Personal</h2>
      <p style={{ color: '#4a5c53', marginBottom: '24px' }}>
        Registro y visualización de horarios semanales para doctores, enfermeras, asistentes y recepcionistas.
      </p>

      <div style={{ background: '#fff', padding: '24px', borderRadius: '8px', border: '1px solid #d1dcd6' }}>
        <h3 style={{ margin: '0 0 16px 0', fontSize: '1.2rem' }}>Grilla Semanal de Horarios</h3>
        <p style={{ fontSize: '0.85rem', color: '#4a5c53' }}>Validación automática de solapes de turno por médico y área.</p>
        <div style={{ marginTop: '20px', padding: '40px', textAlign: 'center', background: '#f3f6f4', borderRadius: '6px', color: '#4a5c53' }}>
          Selecciona una semana para cargar la programación de turnos.
        </div>
      </div>
    </div>
  );
}
