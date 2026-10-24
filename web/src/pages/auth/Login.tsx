export default function Login() {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f3f6f4' }}>
      <div style={{ background: '#fff', padding: '32px', borderRadius: '8px', border: '1px solid #d1dcd6', width: '100%', maxWidth: '400px' }}>
        <h2 style={{ margin: '0 0 8px 0', fontSize: '1.5rem', color: '#13221c' }}>Iniciar Sesión</h2>
        <p style={{ color: '#4a5c53', marginBottom: '24px', fontSize: '0.9rem' }}>
          Panel Administrativo — Hospital Universitario
        </p>

        <form onSubmit={(e) => e.preventDefault()} style={{ display: 'grid', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '6px' }}>Usuario</label>
            <input
              type="text"
              placeholder="Ej. admin"
              style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #d1dcd6' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '6px' }}>Contraseña</label>
            <input
              type="password"
              placeholder="••••••••"
              style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #d1dcd6' }}
            />
          </div>

          <button
            type="submit"
            style={{
              marginTop: '8px',
              padding: '12px',
              background: '#0b5d4b',
              color: '#fff',
              border: 'none',
              borderRadius: '6px',
              fontWeight: 600,
              cursor: 'pointer'
            }}
          >
            Acceder al Panel
          </button>
        </form>
      </div>
    </div>
  );
}
