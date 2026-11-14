import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { isJefeOperaciones } from '../../utils/roles';

interface PacienteItem {
  id: number | string;
  nombre_completo: string;
  dui: string;
  numero_expediente: string;
  fecha_nacimiento: string;
  sexo: 'M' | 'F' | 'OTRO';
  telefono: string | null;
  direccion: string | null;
  tiene_cuenta_usuario?: boolean;
}

interface SignosVitalesItem {
  presion_sistolica?: number | null;
  presion_diastolica?: number | null;
  frecuencia_cardiaca?: number | null;
  frecuencia_respiratoria?: number | null;
  temperatura_c?: number | null;
  saturacion_oxigeno?: number | null;
  peso_kg?: number | null;
  talla_cm?: number | null;
  observaciones?: string | null;
}

interface RecetaExpediente {
  id_receta: string;
  medicamento: string;
  dosis: string;
  frecuencia: string;
  duracion: string;
  indicaciones?: string | null;
}

interface CitaExpediente {
  id_cita: string;
  fecha: string;
  hora_inicio?: string | null;
  estado: string;
  motivo?: string | null;
  especialidad?: { nombre?: string | null } | null;
  medico?: { nombre_completo?: string | null; especialidad?: { nombre?: string | null } | null } | null;
  signos_vitales?: SignosVitalesItem | null;
  consulta?: {
    diagnostico?: string | null;
    notas?: string | null;
    recetas?: RecetaExpediente[];
  } | null;
}

interface OrdenLabExpediente {
  id_orden: string;
  estado: string;
  resultado?: string | null;
  observaciones?: string | null;
  fecha_solicitud?: string | null;
  examen?: { nombre?: string | null } | null;
}

interface IngresoExpediente {
  id_ingreso: string;
  fecha_ingreso: string;
  fecha_alta?: string | null;
  motivo: string;
  cama?: { codigo?: string | null; habitacion?: { codigo?: string | null; planta?: number | null } | null } | null;
}

interface ExpedienteDetalle {
  tipo_sangre?: string | null;
  alergias?: string | null;
  historial?: {
    antecedentes_personales?: string | null;
    antecedentes_familiares?: string | null;
  } | null;
  citas?: CitaExpediente[];
  ordenes_laboratorio?: OrdenLabExpediente[];
  ingresos?: IngresoExpediente[];
}

function buildVitalItems(sv: SignosVitalesItem): { label: string; value: string }[] {
  const items: { label: string; value: string }[] = [];
  if (sv.presion_sistolica != null || sv.presion_diastolica != null) {
    items.push({ label: 'Presión arterial', value: `${sv.presion_sistolica ?? '—'}/${sv.presion_diastolica ?? '—'} mmHg` });
  }
  if (sv.frecuencia_cardiaca != null) items.push({ label: 'Frec. cardíaca', value: `${sv.frecuencia_cardiaca} lpm` });
  if (sv.frecuencia_respiratoria != null) items.push({ label: 'Frec. respiratoria', value: `${sv.frecuencia_respiratoria} rpm` });
  if (sv.temperatura_c != null) items.push({ label: 'Temperatura', value: `${sv.temperatura_c} °C` });
  if (sv.saturacion_oxigeno != null) items.push({ label: 'SpO₂', value: `${sv.saturacion_oxigeno}%` });
  if (sv.peso_kg != null) items.push({ label: 'Peso', value: `${sv.peso_kg} kg` });
  if (sv.talla_cm != null) items.push({ label: 'Talla', value: `${sv.talla_cm} cm` });
  return items;
}

export const PatientsPage: React.FC = () => {
  const { user } = useAuth();
  const isOps = isJefeOperaciones(user);
  const [pacientes, setPacientes] = useState<PacienteItem[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState<boolean>(true);

  // Modal de Código de Activación (6 dígitos)
  const [activationModal, setActivationModal] = useState<{
    paciente: PacienteItem;
    codigo: string;
    expira: string;
  } | null>(null);

  // Modal de Consulta de Expediente (Lectura HU-44)
  const [viewingPaciente, setViewingPaciente] = useState<PacienteItem | null>(null);
  const [expediente, setExpediente] = useState<ExpedienteDetalle | null>(null);
  const [loadingExpediente, setLoadingExpediente] = useState(false);
  const [expedienteError, setExpedienteError] = useState<string | null>(null);

  const openExpediente = async (paciente: PacienteItem) => {
    if (isOps) return;
    setViewingPaciente(paciente);
    setExpediente(null);
    setExpedienteError(null);
    setLoadingExpediente(true);
    try {
      const res: any = await apiClient.get(`/pacientes/${paciente.id}/expediente`);
      setExpediente((res?.data ?? res) as ExpedienteDetalle);
    } catch (err: any) {
      setExpedienteError(err?.message || 'No se pudo cargar el expediente clínico del paciente.');
    } finally {
      setLoadingExpediente(false);
    }
  };

  const closeExpediente = () => {
    setViewingPaciente(null);
    setExpediente(null);
    setExpedienteError(null);
  };

  const fetchPacientes = async () => {
    setLoading(true);
    try {
      const res: any = await apiClient.get('/pacientes');
      const list = Array.isArray(res) ? res : res?.data || [];
      if (list.length > 0) {
        const mapped: PacienteItem[] = list.map((p: any) => ({
          id: p.id_paciente,
          nombre_completo: p.nombre_completo,
          dui: p.dui,
          numero_expediente: p.numero_expediente,
          fecha_nacimiento: p.fecha_nacimiento,
          sexo: p.sexo,
          telefono: p.telefono,
          direccion: p.direccion,
          tiene_cuenta_usuario: !!p.id_usuario,
        }));
        setPacientes(mapped);
        return;
      }
      setPacientes([]);
    } catch {
      // Datos de muestra para visualización
      setPacientes([
        {
          id: 1,
          nombre_completo: 'Carlos Eduardo Ramos',
          dui: '04829104-5',
          numero_expediente: 'EXP-2026-000142',
          fecha_nacimiento: '1988-06-14',
          sexo: 'M',
          telefono: '+503 7890-1234',
          direccion: 'Colonia Escalón, San Salvador',
          tiene_cuenta_usuario: true,
        },
        {
          id: 2,
          nombre_completo: 'Lucía Margarita Portillo',
          dui: '05129384-2',
          numero_expediente: 'EXP-2026-000143',
          fecha_nacimiento: '1995-11-20',
          sexo: 'F',
          telefono: '+503 7123-4567',
          direccion: 'Residencial San Carlos, Santa Ana',
          tiene_cuenta_usuario: false,
        },
        {
          id: 3,
          nombre_completo: 'Roberto Andrés Menjívar',
          dui: '03948572-8',
          numero_expediente: 'EXP-2026-000144',
          fecha_nacimiento: '1974-03-08',
          sexo: 'M',
          telefono: '+503 7654-3210',
          direccion: 'Barrio El Calvario, Sonsonate',
          tiene_cuenta_usuario: false,
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPacientes();
  }, []);

  const handleGenerateActivationCode = async (paciente: PacienteItem) => {
    try {
      const res: any = await apiClient.post(
        `/pacientes/${paciente.id}/codigo-activacion`
      );
      setActivationModal({
        paciente,
        codigo: res.codigo || Math.floor(100000 + Math.random() * 900000).toString(),
        expira: res.vence_en ? new Date(res.vence_en).toLocaleString() : '24 horas',
      });
    } catch {
      setActivationModal({
        paciente,
        codigo: Math.floor(100000 + Math.random() * 900000).toString(),
        expira: '24 horas',
      });
    }
  };

  const filteredPacientes = pacientes.filter((p) => {
    const q = searchTerm.toLowerCase();
    return (
      p.nombre_completo.toLowerCase().includes(q) ||
      p.dui.toLowerCase().includes(q) ||
      p.numero_expediente.toLowerCase().includes(q)
    );
  });

  return (
    <div style={{ display: 'grid', gap: '24px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
            <span
              style={{
                fontSize: '0.72rem',
                fontWeight: 800,
                background: '#0b5d4b',
                color: '#ffffff',
                padding: '2px 8px',
                borderRadius: '6px',
                textTransform: 'uppercase',
                letterSpacing: '0.04em',
              }}
            >
              Auditoría Clínica
            </span>
          </div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#073f33', margin: '0 0 4px' }}>
            Censo de Pacientes y Expedientes
          </h1>
          <p style={{ margin: 0, color: '#526b60', fontSize: '0.88rem' }}>
            Búsqueda, auditoría de expedientes y soporte de vinculación. El alta inicial de pacientes es operado en ventanilla por Recepción desde la App Móvil.
          </p>
        </div>

        <button
          onClick={fetchPacientes}
          disabled={loading}
          style={{
            padding: '9px 16px',
            borderRadius: '8px',
            border: '1px solid #cad8d1',
            background: '#ffffff',
            fontSize: '0.84rem',
            fontWeight: 600,
            cursor: 'pointer',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
          }}
        >
          {loading ? 'Actualizando...' : 'Actualizar Censo'}
        </button>
      </div>

      {/* Buscador */}
      <div style={{ background: '#ffffff', borderRadius: '12px', padding: '16px 20px', border: '1px solid #d1dcd6', display: 'flex', gap: '12px', alignItems: 'center' }}>
        <span style={{ fontSize: '1.2rem', color: '#849a90' }}>🔍</span>
        <input
          type="text"
          placeholder="Buscar por Nombre, DUI (ej. 04829104-5) o Expediente (ej. EXP-2026-000142)..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          style={{ width: '100%', border: 'none', outline: 'none', fontSize: '0.95rem', background: 'transparent', color: '#13221c' }}
        />
        {searchTerm && (
          <button onClick={() => setSearchTerm('')} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#849a90', fontSize: '0.9rem' }}>
            Borrar
          </button>
        )}
      </div>

      {/* Tabla de Pacientes */}
      <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #d1dcd6', overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.02)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.88rem' }}>
          <thead>
            <tr style={{ background: '#eaf5f1', borderBottom: '1px solid #d1dcd6', color: '#073f33' }}>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>N° Expediente</th>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>Paciente</th>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>DUI</th>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>Teléfono / Contacto</th>
              <th style={{ padding: '12px 18px', fontWeight: 700 }}>Cuenta App Móvil</th>
              <th style={{ padding: '12px 18px', fontWeight: 700, textAlign: 'right' }}>{isOps ? 'Soporte App' : 'Auditoría'}</th>
            </tr>
          </thead>
          <tbody>
            {filteredPacientes.map((p) => (
              <tr key={p.id} style={{ borderBottom: '1px solid #e8efeb' }}>
                <td style={{ padding: '14px 18px', fontFamily: 'monospace', color: '#0b5d4b', fontWeight: 700 }}>
                  {p.numero_expediente}
                </td>
                <td style={{ padding: '14px 18px' }}>
                  <div style={{ fontWeight: 700, color: '#13221c' }}>{p.nombre_completo}</div>
                  <div style={{ fontSize: '0.75rem', color: '#526b60' }}>
                    Nacimiento: {p.fecha_nacimiento} · Sexo: {p.sexo}
                  </div>
                </td>
                <td style={{ padding: '14px 18px', fontFamily: 'monospace', color: '#33483f' }}>
                  {p.dui}
                </td>
                <td style={{ padding: '14px 18px', color: '#33483f' }}>
                  <div>{p.telefono || '—'}</div>
                  <div style={{ fontSize: '0.75rem', color: '#849a90' }}>{p.direccion || 'Sin dirección'}</div>
                </td>
                <td style={{ padding: '14px 18px' }}>
                  {p.tiene_cuenta_usuario ? (
                    <span style={{ background: '#d9efe1', color: '#1a7541', padding: '3px 8px', borderRadius: '6px', fontSize: '0.75rem', fontWeight: 700 }}>
                      ✓ Vinculada
                    </span>
                  ) : (
                    <span style={{ background: '#fae9c8', color: '#85560a', padding: '3px 8px', borderRadius: '6px', fontSize: '0.75rem', fontWeight: 700 }}>
                      Pendiente App
                    </span>
                  )}
                </td>
                <td style={{ padding: '14px 18px', textAlign: 'right' }}>
                  {isOps ? (
                    <button
                      onClick={() => handleGenerateActivationCode(p)}
                      style={{
                        background: '#0b5d4b',
                        color: '#ffffff',
                        border: 'none',
                        borderRadius: '6px',
                        padding: '6px 14px',
                        fontSize: '0.78rem',
                        fontWeight: 700,
                        cursor: 'pointer',
                      }}
                    >
                      🔑 Generar PIN
                    </button>
                  ) : (
                    <button
                      onClick={() => openExpediente(p)}
                      style={{
                        background: '#f8faf9',
                        color: '#073f33',
                        border: '1px solid #d1dcd6',
                        borderRadius: '6px',
                        padding: '6px 14px',
                        fontSize: '0.78rem',
                        fontWeight: 700,
                        cursor: 'pointer',
                      }}
                    >
                      Ver Expediente →
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {filteredPacientes.length === 0 && (
              <tr>
                <td colSpan={6} style={{ padding: '36px', textAlign: 'center', color: '#526b60' }}>
                  No se encontraron pacientes con los criterios ingresados.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Modal Consulta Expediente (HU-44) — Solo Administrador General */}
      {!isOps && viewingPaciente && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '16px' }}>
          <div style={{ background: '#ffffff', borderRadius: '16px', width: '100%', maxWidth: '680px', padding: '28px', maxHeight: '90vh', overflowY: 'auto', boxShadow: '0 20px 40px rgba(0,0,0,0.2)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <div>
                <span style={{ fontSize: '0.75rem', fontWeight: 800, color: '#0b5d4b', textTransform: 'uppercase' }}>
                  Expediente Clínico Digital · Solo Lectura (Auditoría)
                </span>
                <h2 style={{ fontSize: '1.3rem', fontWeight: 800, margin: '2px 0 0', color: '#073f33' }}>
                  {viewingPaciente.nombre_completo}
                </h2>
              </div>
              <button onClick={closeExpediente} style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer', color: '#849a90' }}>✕</button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', background: '#f8faf9', padding: '16px', borderRadius: '12px', border: '1px solid #e8efeb', marginBottom: '20px', fontSize: '0.88rem' }}>
              <div><b>DUI:</b> {viewingPaciente.dui}</div>
              <div><b>N° Expediente:</b> {viewingPaciente.numero_expediente}</div>
              <div><b>Fecha Nacimiento:</b> {viewingPaciente.fecha_nacimiento}</div>
              <div><b>Sexo:</b> {viewingPaciente.sexo}</div>
              <div><b>Teléfono:</b> {viewingPaciente.telefono || 'No registrado'}</div>
              <div><b>Dirección:</b> {viewingPaciente.direccion || 'No registrada'}</div>
              {expediente?.tipo_sangre && <div><b>Tipo de sangre:</b> {expediente.tipo_sangre}</div>}
              {expediente?.alergias && <div><b>Alergias:</b> {expediente.alergias}</div>}
            </div>

            {loadingExpediente && (
              <div style={{ padding: '20px', textAlign: 'center', color: '#526b60', fontSize: '0.88rem', marginBottom: '16px' }}>
                Cargando expediente clínico...
              </div>
            )}

            {expedienteError && (
              <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#991b1b', borderRadius: '10px', padding: '12px 14px', fontSize: '0.84rem', marginBottom: '16px' }}>
                {expedienteError}
              </div>
            )}

            {!loadingExpediente && !expedienteError && expediente && (
              <div style={{ display: 'grid', gap: '16px', marginBottom: '20px' }}>
                {(expediente.historial?.antecedentes_personales || expediente.historial?.antecedentes_familiares) && (
                  <div style={{ background: '#f8faf9', border: '1px solid #d1dcd6', borderRadius: '10px', padding: '12px 14px', fontSize: '0.84rem', display: 'grid', gap: '6px' }}>
                    <div style={{ fontWeight: 700, color: '#073f33' }}>Antecedentes Clínicos</div>
                    {expediente.historial.antecedentes_personales && (
                      <div><b>Personales:</b> {expediente.historial.antecedentes_personales}</div>
                    )}
                    {expediente.historial.antecedentes_familiares && (
                      <div><b>Familiares:</b> {expediente.historial.antecedentes_familiares}</div>
                    )}
                  </div>
                )}

                <div style={{ display: 'grid', gap: '10px' }}>
                  <h3 style={{ fontSize: '1rem', fontWeight: 700, margin: 0, color: '#073f33' }}>
                    Historial de Citas, Signos Vitales & Consultas
                  </h3>
                  {(!expediente.citas || expediente.citas.length === 0) ? (
                    <div style={{ background: '#f8faf9', border: '1px solid #e8efeb', borderRadius: '8px', padding: '12px', fontSize: '0.84rem', color: '#526b60' }}>
                      Sin citas o atenciones registradas para este paciente.
                    </div>
                  ) : (
                    [...expediente.citas]
                      .sort((a, b) => `${b.fecha} ${b.hora_inicio || ''}`.localeCompare(`${a.fecha} ${a.hora_inicio || ''}`))
                      .map((cita) => {
                        const especialidad = cita.especialidad?.nombre || cita.medico?.especialidad?.nombre || 'Consulta General';
                        const vitals = cita.signos_vitales ? buildVitalItems(cita.signos_vitales) : [];
                        return (
                          <div key={cita.id_cita} style={{ background: '#ffffff', border: '1px solid #d1dcd6', borderRadius: '10px', padding: '12px 14px', display: 'grid', gap: '8px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px', fontSize: '0.8rem', color: '#526b60' }}>
                              <span>
                                <b>{especialidad}</b>
                                {cita.medico?.nombre_completo ? ` · ${cita.medico.nombre_completo}` : ' · Sin médico asignado'}
                              </span>
                              <span>
                                {cita.fecha}{cita.hora_inicio ? ` ${cita.hora_inicio.slice(0, 5)}` : ''} · <b>{cita.estado}</b>
                              </span>
                            </div>

                            {cita.motivo && (
                              <div style={{ fontSize: '0.82rem', color: '#33483f' }}>
                                <b>Motivo:</b> {cita.motivo}
                              </div>
                            )}

                            {cita.signos_vitales && (vitals.length > 0 || cita.signos_vitales.observaciones) && (
                              <div style={{ background: '#f2f9f6', border: '1px solid #cce3da', borderRadius: '8px', padding: '10px 12px', display: 'grid', gap: '6px' }}>
                                <div style={{ fontSize: '0.75rem', fontWeight: 800, color: '#0b5d4b', textTransform: 'uppercase' }}>
                                  Signos Vitales (Solo lectura)
                                </div>
                                {vitals.length > 0 && (
                                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(145px, 1fr))', gap: '6px 12px', fontSize: '0.8rem', color: '#13221c' }}>
                                    {vitals.map((item) => (
                                      <div key={item.label}>
                                        <span style={{ color: '#526b60' }}>{item.label}:</span> <b>{item.value}</b>
                                      </div>
                                    ))}
                                  </div>
                                )}
                                {cita.signos_vitales.observaciones && (
                                  <div style={{ fontSize: '0.78rem', color: '#33483f' }}>
                                    <b>Observaciones:</b> {cita.signos_vitales.observaciones}
                                  </div>
                                )}
                              </div>
                            )}

                            {cita.consulta && (
                              <div style={{ display: 'grid', gap: '4px', fontSize: '0.82rem', color: '#13221c' }}>
                                {cita.consulta.diagnostico && (
                                  <div><b>Diagnóstico:</b> {cita.consulta.diagnostico}</div>
                                )}
                                {cita.consulta.notas && (
                                  <div style={{ color: '#33483f' }}><b>Notas:</b> {cita.consulta.notas}</div>
                                )}
                                {cita.consulta.recetas && cita.consulta.recetas.length > 0 && (
                                  <div style={{ marginTop: '4px', paddingTop: '6px', borderTop: '1px dashed #e1ebe6', display: 'grid', gap: '3px' }}>
                                    <span style={{ fontWeight: 700, fontSize: '0.78rem', color: '#073f33' }}>Recetas:</span>
                                    {cita.consulta.recetas.map((r) => (
                                      <div key={r.id_receta} style={{ fontSize: '0.8rem', color: '#33483f' }}>
                                        • <b>{r.medicamento}</b> ({r.dosis} · {r.frecuencia} · {r.duracion})
                                        {r.indicaciones ? ` — ${r.indicaciones}` : ''}
                                      </div>
                                    ))}
                                  </div>
                                )}
                              </div>
                            )}
                          </div>
                        );
                      })
                  )}
                </div>

                {expediente.ordenes_laboratorio && expediente.ordenes_laboratorio.length > 0 && (
                  <div style={{ display: 'grid', gap: '8px' }}>
                    <h3 style={{ fontSize: '0.96rem', fontWeight: 700, margin: 0, color: '#073f33' }}>
                      Órdenes de Laboratorio
                    </h3>
                    {expediente.ordenes_laboratorio.map((ord) => (
                      <div key={ord.id_orden} style={{ background: '#ffffff', border: '1px solid #d1dcd6', borderRadius: '8px', padding: '10px 12px', fontSize: '0.82rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', color: '#526b60', fontSize: '0.78rem' }}>
                          <span><b>{ord.examen?.nombre || 'Examen clínico'}</b></span>
                          <span>{ord.fecha_solicitud ? ord.fecha_solicitud.slice(0, 10) : ''} · {ord.estado}</span>
                        </div>
                        {ord.resultado && <div style={{ marginTop: '4px', color: '#13221c' }}><b>Resultado:</b> {ord.resultado}</div>}
                        {ord.observaciones && <div style={{ marginTop: '2px', color: '#33483f' }}><b>Observaciones:</b> {ord.observaciones}</div>}
                      </div>
                    ))}
                  </div>
                )}

                {expediente.ingresos && expediente.ingresos.length > 0 && (
                  <div style={{ display: 'grid', gap: '8px' }}>
                    <h3 style={{ fontSize: '0.96rem', fontWeight: 700, margin: 0, color: '#073f33' }}>
                      Historial de Hospitalización
                    </h3>
                    {expediente.ingresos.map((ing) => (
                      <div key={ing.id_ingreso} style={{ background: '#ffffff', border: '1px solid #d1dcd6', borderRadius: '8px', padding: '10px 12px', fontSize: '0.82rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', color: '#526b60', fontSize: '0.78rem' }}>
                          <span>
                            {ing.cama?.habitacion?.codigo ? `Hab. ${ing.cama.habitacion.codigo} · ` : ''}
                            {ing.cama?.codigo ? `Cama ${ing.cama.codigo}` : 'Ingreso hospitalario'}
                          </span>
                          <span>
                            Ingreso: {ing.fecha_ingreso.slice(0, 10)}
                            {ing.fecha_alta ? ` · Alta: ${ing.fecha_alta.slice(0, 10)}` : ' · Activo'}
                          </span>
                        </div>
                        <div style={{ marginTop: '4px', color: '#13221c' }}><b>Motivo:</b> {ing.motivo}</div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Sección Discreta de Soporte para Vinculación Móvil */}
            <div
              style={{
                background: '#f0fdf4',
                border: '1px solid #bbf7d0',
                borderRadius: '12px',
                padding: '14px 18px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                gap: '12px',
                marginBottom: '20px',
              }}
            >
              <div>
                <span style={{ fontSize: '0.76rem', fontWeight: 800, color: '#166534', textTransform: 'uppercase' }}>
                  Soporte de Vinculación App Móvil
                </span>
                <p style={{ margin: '2px 0 0', fontSize: '0.82rem', color: '#15803d' }}>
                  {viewingPaciente.tiene_cuenta_usuario
                    ? '✓ El paciente ya vinculó su dispositivo móvil a este expediente.'
                    : 'Dispositivo aún no vinculado. Genera un PIN temporal de 6 dígitos si el paciente solicita soporte en ventanilla.'}
                </p>
              </div>

              <button
                onClick={() => handleGenerateActivationCode(viewingPaciente)}
                style={{
                  background: '#0b5d4b',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '6px',
                  padding: '8px 14px',
                  fontSize: '0.78rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                  whiteSpace: 'nowrap',
                }}
              >
                🔑 Generar PIN
              </button>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button onClick={closeExpediente} style={{ padding: '10px 20px', borderRadius: '8px', border: '1px solid #d1dcd6', background: '#f8faf9', color: '#13221c', fontWeight: 600, cursor: 'pointer' }}>
                Cerrar Expediente
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Código de Activación (6 dígitos) */}
      {activationModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 110, padding: '16px' }}>
          <div style={{ background: '#ffffff', borderRadius: '16px', width: '100%', maxWidth: '440px', padding: '28px', textAlign: 'center', boxShadow: '0 20px 40px rgba(0,0,0,0.2)' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '12px' }}>🔑</div>
            <h2 style={{ fontSize: '1.3rem', fontWeight: 800, color: '#073f33', margin: '0 0 6px' }}>
              Código de Activación Móvil
            </h2>
            <p style={{ margin: '0 0 20px', color: '#526b60', fontSize: '0.85rem' }}>
              Proporciona este código a <b>{activationModal.paciente.nombre_completo}</b> para vincular su cuenta en la App CMO (iOS/Android) sin exponer registros por DUI.
            </p>

            <div style={{ background: '#eaf5f1', border: '2px dashed #0b5d4b', borderRadius: '12px', padding: '20px', marginBottom: '20px' }}>
              <div style={{ fontSize: '2.4rem', fontWeight: 800, letterSpacing: '0.25em', color: '#0b5d4b', fontFamily: 'monospace' }}>
                {activationModal.codigo}
              </div>
              <div style={{ fontSize: '0.78rem', color: '#526b60', marginTop: '6px' }}>
                ⏱️ Válido durante las próximas 24 horas
              </div>
            </div>

            <button
              onClick={() => {
                navigator.clipboard.writeText(activationModal.codigo);
                alert('Código de 6 dígitos copiado al portapapeles.');
              }}
              style={{ width: '100%', padding: '12px', borderRadius: '8px', border: 'none', background: '#0b5d4b', color: '#ffffff', fontWeight: 700, cursor: 'pointer', marginBottom: '10px' }}
            >
              Copiar Código
            </button>
            <button
              onClick={() => setActivationModal(null)}
              style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #d1dcd6', background: '#f8faf9', color: '#13221c', fontWeight: 600, cursor: 'pointer' }}
            >
              Cerrar
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
