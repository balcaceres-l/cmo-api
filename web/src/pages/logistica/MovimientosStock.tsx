export default function MovimientosStock() {
  return (
    <div>
      <h2 style={{ fontSize: '1.5rem', marginBottom: '8px' }}>Movimientos de Inventario</h2>
      <p style={{ color: '#4a5c53', marginBottom: '24px' }}>
        Registro de entradas y salidas de almacén para trazabilidad de insumos.
      </p>

      <div style={{ background: '#fff', borderRadius: '8px', border: '1px solid #d1dcd6', padding: '24px' }}>
        <p style={{ color: '#4a5c53', margin: 0 }}>
          Historial de movimientos con motivo, fecha, usuario responsable y cantidad afectada.
        </p>
      </div>
    </div>
  );
}
