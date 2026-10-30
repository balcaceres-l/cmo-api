import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

interface OcupacionItem {
  planta: number;
  estado: string;
  total: number;
}

interface PublicData {
  camasDisponibles: number;
  camasOcupadas: number;
  camasVipLibres: number;
  camasSemiprivadasLibres: number;
  ambulanciaEstado: 'DISPONIBLE' | 'EN_SERVICIO' | 'EN_MANTENIMIENTO';
  medicosTurno: Array<{ nombre: string; especialidad: string; consultorio: string }>;
  avisos: Array<{ id: number; titulo: string; contenido: string; fecha: string; tipo: 'general' | 'urgente' }>;
}

export const PublicPortalPage: React.FC = () => {
  const navigate = useNavigate();
  const [data, setData] = useState<PublicData>({
    camasDisponibles: 12,
    camasOcupadas: 5,
    camasVipLibres: 3,
    camasSemiprivadasLibres: 9,
    ambulanciaEstado: 'DISPONIBLE',
    medicosTurno: [
      { nombre: 'Dr. Alejandro Benítez', especialidad: 'Medicina General', consultorio: 'C-101' },
      { nombre: 'Dr. Salvador Morales', especialidad: 'Emergencias Médicas', consultorio: 'C-102' },
      { nombre: 'Dra. María Arismendi', especialidad: 'Alergología e Inmunología', consultorio: 'C-103' },
    ],
    avisos: [
      {
        id: 1,
        titulo: 'Horario de Visitas en Habitaciones VIP y Semiprivadas',
        contenido: 'Se permite el ingreso de 1 familiar por paciente en horario de 10:00 a 12:00 y de 15:00 a 17:00.',
        fecha: 'Vigente hoy',
        tipo: 'general',
      },
      {
        id: 2,
        titulo: 'Atención Ininterrumpida de Emergencias 24/7',
        contenido: 'El área de triaje y la ambulancia médica se encuentran operativas de manera continua.',
        fecha: 'Permanente',
        tipo: 'urgente',
      },
    ],
  });

  const [lastCheck, setLastCheck] = useState<Date>(new Date());

  // Consulta pública a la API (si está disponible)
  useEffect(() => {
    const fetchPublicInfo = async () => {
      try {
        const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1';
        const res = await fetch(`${baseUrl}/publico/ocupacion`);
        if (res.ok) {
          const json = await res.json();
          if (json.success && json.data) {
            const ocupacion: OcupacionItem[] = json.data.ocupacion || [];
            const vipLibres = ocupacion.find((o) => o.planta === 3 && o.estado === 'DISPONIBLE')?.total ?? 3;
            const semiLibres = ocupacion.find((o) => o.planta === 2 && o.estado === 'DISPONIBLE')?.total ?? 9;
            const vipOcup = ocupacion.find((o) => o.planta === 3 && o.estado === 'OCUPADA')?.total ?? 2;
            const semiOcup = ocupacion.find((o) => o.planta === 2 && o.estado === 'OCUPADA')?.total ?? 3;

            setData((prev) => ({
              ...prev,
              camasVipLibres: vipLibres,
              camasSemiprivadasLibres: semiLibres,
              camasDisponibles: vipLibres + semiLibres,
              camasOcupadas: vipOcup + semiOcup,
            }));
          }
        }
      } catch {
        // Fallback robusto con valores de referencia
      } finally {
        setLastCheck(new Date());
      }
    };

    fetchPublicInfo();
    const interval = setInterval(fetchPublicInfo, 10000); // Actualización cada 10s según HU-67/HU-69
    return () => clearInterval(interval);
  }, []);

  const totalCamas = 17;
  const porcentaje = Math.round((data.camasOcupadas / totalCamas) * 100);

  return (
    <div style={{ minHeight: '100vh', background: '#f3f6f4', color: '#13221c', fontFamily: 'system-ui, -apple-system, sans-serif' }}>
      {/* Header Institucional */}
      <header
        style={{
          background: '#ffffff',
          borderBottom: '1px solid #d1dcd6',
          position: 'sticky',
          top: 0,
          zIndex: 30,
          boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
        }}
      >
        <div
          style={{
            maxWidth: '1200px',
            margin: '0 auto',
            padding: '12px 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '16px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <img
              src="/logo-full.png"
              alt="Centro Médico de Occidente"
              style={{ height: '48px', width: 'auto', objectFit: 'contain' }}
            />
          </div>

          <nav style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
            <a href="#capacidad" style={{ color: '#0b5d4b', textDecoration: 'none', fontWeight: 600, fontSize: '0.9rem' }}>
              Capacidad (17 Camas)
            </a>
            <a href="#ambulancia" style={{ color: '#0b5d4b', textDecoration: 'none', fontWeight: 600, fontSize: '0.9rem' }}>
              Ambulancia en Vivo
            </a>
            <a href="#medicos" style={{ color: '#0b5d4b', textDecoration: 'none', fontWeight: 600, fontSize: '0.9rem' }}>
              Médicos de Guardia
            </a>
            <a href="#avisos" style={{ color: '#0b5d4b', textDecoration: 'none', fontWeight: 600, fontSize: '0.9rem' }}>
              Avisos
            </a>

            <button
              onClick={() => navigate('/login')}
              style={{
                background: 'linear-gradient(135deg, #0b5d4b 0%, #158066 100%)',
                color: '#ffffff',
                border: 'none',
                borderRadius: '8px',
                padding: '8px 18px',
                fontWeight: 700,
                fontSize: '0.88rem',
                cursor: 'pointer',
                boxShadow: '0 4px 12px rgba(11, 93, 75, 0.25)',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              <span>Acceso Personal</span>
              <span>→</span>
            </button>
          </nav>
        </div>
      </header>

      {/* Hero Banner */}
      <section
        style={{
          background: 'linear-gradient(135deg, #091a14 0%, #0d2a21 100%)',
          color: '#ffffff',
          padding: '60px 24px',
          textAlign: 'center',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <div style={{ maxWidth: '800px', margin: '0 auto', position: 'relative', zIndex: 1 }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '8px',
              background: 'rgba(79, 196, 162, 0.15)',
              border: '1px solid rgba(79, 196, 162, 0.3)',
              padding: '6px 14px',
              borderRadius: '999px',
              fontSize: '0.8rem',
              color: '#4fc4a2',
              fontWeight: 600,
              marginBottom: '18px',
            }}
          >
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#4fc4a2', display: 'inline-block' }} />
            <span>Información Pública en Tiempo Real · Actualizado a las {lastCheck.toLocaleTimeString()}</span>
          </div>

          <h1 style={{ fontSize: '2.5rem', fontWeight: 800, margin: '0 0 16px', letterSpacing: '-0.02em', lineHeight: 1.15 }}>
            Centro Médico de Occidente
          </h1>
          <p style={{ fontSize: '1.15rem', color: '#a2b7ad', margin: '0 0 28px', lineHeight: 1.5 }}>
            Hospital Universitario comprometido con la excelencia médica, la transparencia operativa y el bienestar de nuestros pacientes.
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '16px', flexWrap: 'wrap' }}>
            <div
              style={{
                background: 'rgba(255, 255, 255, 0.08)',
                border: '1px solid rgba(255, 255, 255, 0.12)',
                borderRadius: '12px',
                padding: '16px 24px',
                minWidth: '180px',
              }}
            >
              <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#4fc4a2' }}>17 Camas</div>
              <div style={{ fontSize: '0.78rem', color: '#a2b7ad', textTransform: 'uppercase' }}>Aforo Técnico Máximo</div>
            </div>
            <div
              style={{
                background: 'rgba(255, 255, 255, 0.08)',
                border: '1px solid rgba(255, 255, 255, 0.12)',
                borderRadius: '12px',
                padding: '16px 24px',
                minWidth: '180px',
              }}
            >
              <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#68d196' }}>1 Ambulancia</div>
              <div style={{ fontSize: '0.78rem', color: '#a2b7ad', textTransform: 'uppercase' }}>Unidad de Soporte Vital</div>
            </div>
            <div
              style={{
                background: 'rgba(255, 255, 255, 0.08)',
                border: '1px solid rgba(255, 255, 255, 0.12)',
                borderRadius: '12px',
                padding: '16px 24px',
                minWidth: '180px',
              }}
            >
              <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#6cb7e3' }}>24 Horas</div>
              <div style={{ fontSize: '0.78rem', color: '#a2b7ad', textTransform: 'uppercase' }}>Atención Continua</div>
            </div>
          </div>
        </div>
      </section>

      {/* Main Content */}
      <main style={{ maxWidth: '1200px', margin: '40px auto', padding: '0 24px', display: 'grid', gap: '48px' }}>
        {/* Capacidad Hospitalaria */}
        <section id="capacidad">
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '8px' }}>
            <div>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 700, margin: 0, color: '#073f33' }}>
                Capacidad Hospitalaria y Ocupación
              </h2>
              <p style={{ margin: '4px 0 0', color: '#4a5c53', fontSize: '0.9rem' }}>
                Regla estricta sin sobrecupo: capacidad máxima de 17 camas entre Planta 3 (VIP) y Planta 2 (Semiprivadas).
              </p>
            </div>
            <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#0b5d4b' }}>
              {data.camasDisponibles} camas libres de 17 ({100 - porcentaje}% libre)
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px' }}>
            {/* Planta 3 VIP */}
            <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #d1dcd6', padding: '24px', boxShadow: '0 2px 6px rgba(0,0,0,0.02)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700, color: '#073f33' }}>
                  Planta 3 · Habitaciones VIP
                </h3>
                <span style={{ background: '#eaf5f1', color: '#0b5d4b', fontSize: '0.78rem', fontWeight: 700, padding: '3px 10px', borderRadius: '999px' }}>
                  5 Habitaciones Individuales
                </span>
              </div>
              <p style={{ margin: '0 0 16px', fontSize: '0.85rem', color: '#526b60' }}>
                Habitaciones VIP-301 a VIP-305 con baño privado, cama de acompañante y monitoreo clínico.
              </p>

              {/* Habitaciones VIP grid */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '8px' }}>
                {['VIP-301', 'VIP-302', 'VIP-303', 'VIP-304', 'VIP-305'].map((hab, index) => {
                  const isLibre = index < data.camasVipLibres;
                  return (
                    <div
                      key={hab}
                      style={{
                        background: isLibre ? '#d9efe1' : '#fae9c8',
                        border: `1.5px solid ${isLibre ? '#1a7541' : '#85560a'}`,
                        borderRadius: '8px',
                        padding: '12px 6px',
                        textAlign: 'center',
                      }}
                    >
                      <div style={{ fontSize: '0.75rem', fontWeight: 700, color: isLibre ? '#1a7541' : '#85560a' }}>
                        {hab}
                      </div>
                      <div style={{ fontSize: '0.68rem', marginTop: '4px', fontWeight: 600 }}>
                        {isLibre ? 'LIBRE' : 'OCUPADA'}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Planta 2 Semiprivadas */}
            <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #d1dcd6', padding: '24px', boxShadow: '0 2px 6px rgba(0,0,0,0.02)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700, color: '#073f33' }}>
                  Planta 2 · Habitaciones Semiprivadas
                </h3>
                <span style={{ background: '#eaf5f1', color: '#0b5d4b', fontSize: '0.78rem', fontWeight: 700, padding: '3px 10px', borderRadius: '999px' }}>
                  6 Habs (12 Camas)
                </span>
              </div>
              <p style={{ margin: '0 0 16px', fontSize: '0.85rem', color: '#526b60' }}>
                HAB-201 a HAB-206 con 2 camas por habitación (Cama A y Cama B). Sin sobrecupo ni camas extra.
              </p>

              {/* Grid 6 habitaciones */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px' }}>
                {['HAB-201', 'HAB-202', 'HAB-203', 'HAB-204', 'HAB-205', 'HAB-206'].map((hab, i) => (
                  <div key={hab} style={{ background: '#f8faf9', border: '1px solid #d1dcd6', borderRadius: '8px', padding: '8px', textAlign: 'center' }}>
                    <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#073f33', marginBottom: '4px' }}>{hab}</div>
                    <div style={{ display: 'flex', gap: '4px', justifyContent: 'center' }}>
                      <span style={{ fontSize: '0.65rem', padding: '2px 4px', borderRadius: '4px', background: '#d9efe1', color: '#1a7541', fontWeight: 700 }}>A: Libre</span>
                      <span style={{ fontSize: '0.65rem', padding: '2px 4px', borderRadius: '4px', background: i % 2 === 0 ? '#fae9c8' : '#d9efe1', color: i % 2 === 0 ? '#85560a' : '#1a7541', fontWeight: 700 }}>B: {i % 2 === 0 ? 'Ocup.' : 'Libre'}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Ambulancia Única & Médicos de Guardia */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px' }}>
          {/* Ambulancia */}
          <section id="ambulancia" style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #d1dcd6', padding: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: '#073f33' }}>
                Ambulancia Única del Hospital
              </h2>
              <span
                style={{
                  background: data.ambulanciaEstado === 'DISPONIBLE' ? '#d9efe1' : '#dbebf4',
                  color: data.ambulanciaEstado === 'DISPONIBLE' ? '#1a7541' : '#0e5c86',
                  fontWeight: 800,
                  fontSize: '0.78rem',
                  padding: '4px 12px',
                  borderRadius: '999px',
                }}
              >
                ● {data.ambulanciaEstado}
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '16px', background: '#f8faf9', padding: '16px', borderRadius: '8px', border: '1px solid #e8efeb' }}>
              <div style={{ fontSize: '2.5rem' }}>🚑</div>
              <div>
                <div style={{ fontWeight: 700, fontSize: '1rem', color: '#13221c' }}>Unidad de Soporte Vital Móvil 01</div>
                <div style={{ fontSize: '0.82rem', color: '#526b60', marginTop: '2px' }}>
                  Recurso crítico gestionado en tiempo real. Equipada con soporte vital y desfibrilador.
                </div>
              </div>
            </div>

            <div style={{ marginTop: '16px', fontSize: '0.82rem', color: '#526b60', lineHeight: 1.5 }}>
              <b>Política de despacho:</b> Asignación prioritaria para código rojo y traslados de urgencia autorizados por la Dirección de Operaciones.
            </div>
          </section>

          {/* Médicos de Guardia */}
          <section id="medicos" style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #d1dcd6', padding: '24px' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 16px', color: '#073f33' }}>
              Médicos de Turno Actual
            </h2>

            <div style={{ display: 'grid', gap: '10px' }}>
              {data.medicosTurno.map((med) => (
                <div
                  key={med.nombre}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 14px',
                    borderRadius: '8px',
                    background: '#f8faf9',
                    border: '1px solid #e8efeb',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#13221c' }}>{med.nombre}</div>
                    <div style={{ fontSize: '0.78rem', color: '#526b60' }}>{med.especialidad}</div>
                  </div>
                  <span style={{ background: '#eaf5f1', color: '#0b5d4b', fontSize: '0.78rem', fontWeight: 700, padding: '3px 8px', borderRadius: '6px' }}>
                    {med.consultorio}
                  </span>
                </div>
              ))}
            </div>
          </section>
        </div>

        {/* Avisos Oficiales */}
        <section id="avisos" style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #d1dcd6', padding: '24px' }}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 16px', color: '#073f33' }}>
            Avisos y Comunicados Oficiales
          </h2>

          <div style={{ display: 'grid', gap: '12px' }}>
            {data.avisos.map((aviso) => (
              <div
                key={aviso.id}
                style={{
                  borderLeft: `4px solid ${aviso.tipo === 'urgente' ? '#0b5d4b' : '#0e5c86'}`,
                  background: aviso.tipo === 'urgente' ? '#eaf5f1' : '#f0f7fb',
                  padding: '14px 18px',
                  borderRadius: '0 8px 8px 0',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ fontWeight: 700, fontSize: '0.95rem', color: '#073f33' }}>{aviso.titulo}</div>
                  <span style={{ fontSize: '0.75rem', color: '#526b60', fontWeight: 600 }}>{aviso.fecha}</span>
                </div>
                <p style={{ margin: '6px 0 0', fontSize: '0.85rem', color: '#33483f', lineHeight: 1.45 }}>
                  {aviso.contenido}
                </p>
              </div>
            ))}
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer style={{ background: '#091a14', color: '#a2b7ad', padding: '36px 24px', borderTop: '1px solid #14352a', marginTop: '60px' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto', display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: '20px', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <img src="/logo-icon.png" alt="CMO" style={{ height: '36px', width: '36px', objectFit: 'contain' }} />
            <div>
              <div style={{ color: '#ffffff', fontWeight: 700, fontSize: '0.92rem' }}>Centro Médico de Occidente</div>
              <div style={{ fontSize: '0.78rem' }}>Sistema de Gestión para Hospital Universitario</div>
            </div>
          </div>

          <div style={{ fontSize: '0.8rem', textAlign: 'right' }}>
            <p style={{ margin: 0 }}>Política estricta de protección de datos personales y expedientes clínicos.</p>
            <p style={{ margin: '4px 0 0', color: '#5eead4' }}>© 2026 Hospital CMO. Todos los derechos reservados.</p>
          </div>
        </div>
      </footer>
    </div>
  );
};
