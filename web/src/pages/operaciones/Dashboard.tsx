export default function Dashboard() {
  return (
    <div>
      <h2 style={{ fontSize: '1.5rem', marginBottom: '8px' }}>Dashboard Operativo</h2>
      <p style={{ color: '#4a5c53', marginBottom: '24px' }}>
        Monitoreo general de ocupación, citas, estado de ambulancia y recursos en tiempo real.
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
        <div style={{ background: '#fff', padding: '20px', borderRadius: '8px', border: '1px solid #d1dcd6' }}>
          <h4 style={{ margin: '0 0 8px 0', color: '#4a5c53', fontSize: '0.9rem' }}>Ocupación de Camas</h4>
          <p style={{ margin: 0, fontSize: '1.8rem', fontWeight: 700 }}>0 / 17</p>
          <span style={{ fontSize: '0.8rem', color: '#0b5d4b' }}>5 VIP · 12 Semiprivadas</span>
        </div>

        <div style={{ background: '#fff', padding: '20px', borderRadius: '8px', border: '1px solid #d1dcd6' }}>
          <h4 style={{ margin: '0 0 8px 0', color: '#4a5c53', fontSize: '0.9rem' }}>Estado de Ambulancia</h4>
          <p style={{ margin: 0, fontSize: '1.4rem', fontWeight: 700, color: '#1a7541' }}>DISPONIBLE</p>
          <span style={{ fontSize: '0.8rem', color: '#4a5c53' }}>Unidad 1 (Privada)</span>
        </div>

        <div style={{ background: '#fff', padding: '20px', borderRadius: '8px', border: '1px solid #d1dcd6' }}>
          <h4 style={{ margin: '0 0 8px 0', color: '#4a5c53', fontSize: '0.9rem' }}>Citas del Día</h4>
          <p style={{ margin: 0, fontSize: '1.8rem', fontWeight: 700 }}>0</p>
          <span style={{ fontSize: '0.8rem', color: '#4a5c53' }}>0 en atención · 0 en espera</span>
        </div>

        <div style={{ background: '#fff', padding: '20px', borderRadius: '8px', border: '1px solid #d1dcd6' }}>
          <h4 style={{ margin: '0 0 8px 0', color: '#4a5c53', fontSize: '0.9rem' }}>Personal en Turno</h4>
          <p style={{ margin: 0, fontSize: '1.8rem', fontWeight: 700 }}>0</p>
          <span style={{ fontSize: '0.8rem', color: '#4a5c53' }}>Médicos y enfermería activos</span>
        </div>
      </div>
    </div>
  );
}
