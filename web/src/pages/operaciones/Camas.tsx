export default function Camas() {
  return (
    <div>
      <h2 style={{ fontSize: '1.5rem', marginBottom: '8px' }}>Gestión de Camas y Habitaciones</h2>
      <p style={{ color: '#4a5c53', marginBottom: '24px' }}>
        Control de ocupación hospitalaria sin sobrecupo ni doble asignación.
      </p>

      <section style={{ marginBottom: '24px' }}>
        <h3 style={{ fontSize: '1.2rem', marginBottom: '12px' }}>3ª Planta — Habitaciones VIP (5 individuales)</h3>
        <p style={{ fontSize: '0.85rem', color: '#4a5c53', marginBottom: '12px' }}>Habitaciones VIP-301 a VIP-305 (Capacidad: 1 paciente por habitación).</p>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px' }}>
          {['VIP-301', 'VIP-302', 'VIP-303', 'VIP-304', 'VIP-305'].map((hab) => (
            <div key={hab} style={{ background: '#fff', padding: '16px', borderRadius: '8px', border: '1px solid #d1dcd6' }}>
              <b>{hab}</b>
              <p style={{ fontSize: '0.85rem', color: '#1a7541', margin: '4px 0 0 0' }}>Disponible</p>
            </div>
          ))}
        </div>
      </section>

      <section>
        <h3 style={{ fontSize: '1.2rem', marginBottom: '12px' }}>2ª Planta — Habitaciones Semiprivadas (6 compartidas)</h3>
        <p style={{ fontSize: '0.85rem', color: '#4a5c53', marginBottom: '12px' }}>Habitaciones HAB-201 a HAB-206 (Capacidad: 2 pacientes por habitación, 12 camas total).</p>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px' }}>
          {['HAB-201', 'HAB-202', 'HAB-203', 'HAB-204', 'HAB-205', 'HAB-206'].map((hab) => (
            <div key={hab} style={{ background: '#fff', padding: '16px', borderRadius: '8px', border: '1px solid #d1dcd6' }}>
              <b>{hab}</b>
              <p style={{ fontSize: '0.85rem', color: '#1a7541', margin: '4px 0 0 0' }}>2 camas disponibles</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
