import React, { useState, useEffect } from 'react';
import { BedIcon, SearchIcon, PlusCircleIcon, RefreshCwIcon } from '../../components/Icons';
import { apiClient, ApiError } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { isJefeOperaciones } from '../../utils/roles';

interface BedData {
  id: string; // id_cama o código
  codigo: string;
  planta: number;
  tipo: 'VIP' | 'SEMIPRIVADA';
  letra?: 'A' | 'B';
  habitacion: string;
  estado: 'OCUPADA' | 'DISPONIBLE' | 'MANTENIMIENTO';
  id_ingreso?: string;
  paciente?: {
    id_paciente?: string;
    nombre: string;
    expediente: string;
    dui?: string;
    diagnostico?: string;
    medico?: string;
    fechaIngreso?: string;
  };
}

interface PacienteOption {
  id_paciente: string;
  nombre_completo: string;
  numero_expediente: string;
  dui: string | null;
}

interface MedicoOption {
  id: string;
  nombre_completo: string;
  cargo: string | null;
}

const FALLBACK_BEDS: BedData[] = [
  // Planta 3 - VIP
  { id: 'b-301', codigo: 'VIP-301', planta: 3, tipo: 'VIP', habitacion: 'VIP-301', estado: 'DISPONIBLE' },
  { id: 'b-302', codigo: 'VIP-302', planta: 3, tipo: 'VIP', habitacion: 'VIP-302', estado: 'DISPONIBLE' },
  { id: 'b-303', codigo: 'VIP-303', planta: 3, tipo: 'VIP', habitacion: 'VIP-303', estado: 'DISPONIBLE' },
  { id: 'b-304', codigo: 'VIP-304', planta: 3, tipo: 'VIP', habitacion: 'VIP-304', estado: 'DISPONIBLE' },
  { id: 'b-305', codigo: 'VIP-305', planta: 3, tipo: 'VIP', habitacion: 'VIP-305', estado: 'DISPONIBLE' },
  // Planta 2 - Semiprivadas
  { id: 'b-201a', codigo: 'HAB-201-A', planta: 2, tipo: 'SEMIPRIVADA', letra: 'A', habitacion: 'HAB-201', estado: 'DISPONIBLE' },
  { id: 'b-201b', codigo: 'HAB-201-B', planta: 2, tipo: 'SEMIPRIVADA', letra: 'B', habitacion: 'HAB-201', estado: 'DISPONIBLE' },
  { id: 'b-202a', codigo: 'HAB-202-A', planta: 2, tipo: 'SEMIPRIVADA', letra: 'A', habitacion: 'HAB-202', estado: 'DISPONIBLE' },
  { id: 'b-202b', codigo: 'HAB-202-B', planta: 2, tipo: 'SEMIPRIVADA', letra: 'B', habitacion: 'HAB-202', estado: 'DISPONIBLE' },
  { id: 'b-203a', codigo: 'HAB-203-A', planta: 2, tipo: 'SEMIPRIVADA', letra: 'A', habitacion: 'HAB-203', estado: 'DISPONIBLE' },
  { id: 'b-203b', codigo: 'HAB-203-B', planta: 2, tipo: 'SEMIPRIVADA', letra: 'B', habitacion: 'HAB-203', estado: 'DISPONIBLE' },
  { id: 'b-204a', codigo: 'HAB-204-A', planta: 2, tipo: 'SEMIPRIVADA', letra: 'A', habitacion: 'HAB-204', estado: 'DISPONIBLE' },
  { id: 'b-204b', codigo: 'HAB-204-B', planta: 2, tipo: 'SEMIPRIVADA', letra: 'B', habitacion: 'HAB-204', estado: 'DISPONIBLE' },
  { id: 'b-205a', codigo: 'HAB-205-A', planta: 2, tipo: 'SEMIPRIVADA', letra: 'A', habitacion: 'HAB-205', estado: 'DISPONIBLE' },
  { id: 'b-205b', codigo: 'HAB-205-B', planta: 2, tipo: 'SEMIPRIVADA', letra: 'B', habitacion: 'HAB-205', estado: 'DISPONIBLE' },
  { id: 'b-206a', codigo: 'HAB-206-A', planta: 2, tipo: 'SEMIPRIVADA', letra: 'A', habitacion: 'HAB-206', estado: 'DISPONIBLE' },
  { id: 'b-206b', codigo: 'HAB-206-B', planta: 2, tipo: 'SEMIPRIVADA', letra: 'B', habitacion: 'HAB-206', estado: 'DISPONIBLE' },
];

export const BedsPage: React.FC = () => {
  const { user } = useAuth();
  const isOps = isJefeOperaciones(user);

  const [beds, setBeds] = useState<BedData[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [activeTab, setActiveTab] = useState<'ALL' | 'P3' | 'P2' | 'LIST'>('ALL');
  const [filterQuery, setFilterQuery] = useState('');
  const [selectedBed, setSelectedBed] = useState<BedData | null>(null);
  const [modalType, setModalType] = useState<'INGRESO' | 'ALTA' | 'TRASLADO' | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // Auxiliares
  const [pacientes, setPacientes] = useState<PacienteOption[]>([]);
  const [medicos, setMedicos] = useState<MedicoOption[]>([]);

  // Formularios
  const [ingresoForm, setIngresoForm] = useState({
    id_paciente: '',
    tipo: 'SEMIPRIVADA' as 'VIP' | 'SEMIPRIVADA',
    id_medico: '',
    motivo: '',
  });

  const [trasladoIdCama, setTrasladoIdCama] = useState('');

  const showNotification = (msg: string) => {
    setFeedback(msg);
    setTimeout(() => setFeedback(null), 4500);
  };

  const fetchHospitalizacion = async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const [habsRes, ingresosRes]: any[] = await Promise.all([
        apiClient.get('/habitaciones'),
        apiClient.get('/ingresos?activos=1'),
      ]);

      const activeIngresos = Array.isArray(ingresosRes)
        ? ingresosRes
        : ingresosRes?.data || [];

      // Mapear ingresos activos por código o id de cama
      const ingresoPorCama = new Map<string, any>();
      activeIngresos.forEach((ing: any) => {
        if (ing.cama?.codigo) {
          ingresoPorCama.set(ing.cama.codigo, ing);
        }
        if (ing.id_cama) {
          ingresoPorCama.set(ing.id_cama, ing);
        }
      });

      // El endpoint /habitaciones devuelve un mapa o arreglo por plantas
      const rawPlants = habsRes;
      const flatBeds: BedData[] = [];

      const processHabitacion = (hab: any, plantaNum: number) => {
        if (!hab.camas || !Array.isArray(hab.camas)) return;
        hab.camas.forEach((c: any) => {
          const matchingIngreso = ingresoPorCama.get(c.codigo) || ingresoPorCama.get(c.id_cama);
          const isOcupada = c.estado === 'OCUPADA' || !!matchingIngreso;

          const letMatch = c.codigo.endsWith('-A') ? 'A' : c.codigo.endsWith('-B') ? 'B' : undefined;

          flatBeds.push({
            id: c.id_cama || c.codigo,
            codigo: c.codigo,
            planta: plantaNum,
            tipo: hab.tipo || (plantaNum === 3 ? 'VIP' : 'SEMIPRIVADA'),
            letra: letMatch as 'A' | 'B' | undefined,
            habitacion: hab.codigo,
            estado: isOcupada ? 'OCUPADA' : 'DISPONIBLE',
            id_ingreso: matchingIngreso?.id_ingreso || matchingIngreso?.id,
            paciente: isOcupada
              ? {
                  id_paciente: matchingIngreso?.paciente?.id_paciente || c.paciente?.id_paciente,
                  nombre: matchingIngreso?.paciente?.nombre_completo || c.paciente?.nombre_completo || 'Paciente Hospitalizado',
                  expediente: matchingIngreso?.paciente?.numero_expediente || c.paciente?.numero_expediente || 'EXP-2026',
                  dui: matchingIngreso?.paciente?.dui,
                  diagnostico: matchingIngreso?.motivo || 'Observación clínica',
                  medico: matchingIngreso?.medico?.nombre_completo || 'Médico de Turno',
                  fechaIngreso: matchingIngreso?.fecha_ingreso ? new Date(matchingIngreso.fecha_ingreso).toLocaleString() : 'Reciente',
                }
              : undefined,
          });
        });
      };

      if (Array.isArray(rawPlants)) {
        rawPlants.forEach((item: any) => {
          if (Array.isArray(item)) {
            // item es un arreglo de habitaciones de una planta
            const sampleHab = item[0];
            const pNum = sampleHab?.tipo === 'VIP' || sampleHab?.codigo?.startsWith('VIP') ? 3 : 2;
            item.forEach((hab) => processHabitacion(hab, pNum));
          } else if (item && typeof item === 'object') {
            // Puede ser objeto de habitación directamente
            const pNum = item.tipo === 'VIP' ? 3 : 2;
            processHabitacion(item, pNum);
          }
        });
      } else if (rawPlants && typeof rawPlants === 'object') {
        // En caso que venga como { "2": [...], "3": [...] }
        Object.entries(rawPlants).forEach(([plantaStr, habs]: [string, any]) => {
          const pNum = parseInt(plantaStr, 10) || 2;
          if (Array.isArray(habs)) {
            habs.forEach((hab) => processHabitacion(hab, pNum));
          }
        });
      }

      if (flatBeds.length > 0) {
        setBeds(flatBeds);
      } else {
        setBeds(FALLBACK_BEDS);
      }
    } catch {
      setBeds(FALLBACK_BEDS);
    } finally {
      setLoading(false);
    }
  };

  const fetchAuxiliares = async () => {
    try {
      const [pacsRes, medsRes]: any[] = await Promise.allSettled([
        apiClient.get('/pacientes'),
        apiClient.get('/medicos'),
      ]);

      if (pacsRes.status === 'fulfilled') {
        const pacs = Array.isArray(pacsRes.value) ? pacsRes.value : pacsRes.value?.data || [];
        setPacientes(pacs);
        if (pacs.length > 0) {
          setIngresoForm((prev) => ({ ...prev, id_paciente: pacs[0].id_paciente }));
        }
      }

      if (medsRes.status === 'fulfilled') {
        const meds = Array.isArray(medsRes.value) ? medsRes.value : medsRes.value?.data || [];
        setMedicos(meds);
        if (meds.length > 0) {
          setIngresoForm((prev) => ({ ...prev, id_medico: meds[0].id }));
        }
      }
    } catch {
      // Ignorar
    }
  };

  useEffect(() => {
    fetchHospitalizacion();
    fetchAuxiliares();
  }, []);

  // Dar de Alta
  const handleDarDeAlta = async () => {
    if (!selectedBed) return;
    setIsSubmitting(true);
    try {
      if (selectedBed.id_ingreso) {
        await apiClient.patch(`/ingresos/${selectedBed.id_ingreso}/alta`);
      } else {
        // Si no tenemos id_ingreso directo, buscar en ingresos
        const res: any = await apiClient.get('/ingresos?activos=1');
        const list = Array.isArray(res) ? res : res?.data || [];
        const match = list.find((i: any) => i.cama?.codigo === selectedBed.codigo || i.id_cama === selectedBed.id);
        if (match) {
          await apiClient.patch(`/ingresos/${match.id_ingreso || match.id}/alta`);
        }
      }
      showNotification(`Alta procesada exitosamente para la cama ${selectedBed.codigo}.`);
      setModalType(null);
      setSelectedBed(null);
      fetchHospitalizacion();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al procesar alta: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Asignar Ingreso
  const handleAsignarIngreso = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!ingresoForm.id_paciente || !ingresoForm.id_medico || !ingresoForm.motivo) {
      alert('Por favor completa todos los campos del ingreso.');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload: any = {
        id_paciente: ingresoForm.id_paciente,
        tipo: selectedBed ? selectedBed.tipo : ingresoForm.tipo,
        id_medico: ingresoForm.id_medico,
        motivo: ingresoForm.motivo,
      };

      // Si seleccionamos una cama disponible concreta
      if (selectedBed && selectedBed.estado === 'DISPONIBLE') {
        payload.id_cama = selectedBed.id;
      }

      await apiClient.post('/ingresos', payload);
      showNotification('Ingreso hospitalario registrado exitosamente en la base de datos.');
      setModalType(null);
      setSelectedBed(null);
      setIngresoForm({
        id_paciente: pacientes[0]?.id_paciente || '',
        tipo: 'SEMIPRIVADA',
        id_medico: medicos[0]?.id || '',
        motivo: '',
      });
      fetchHospitalizacion();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al asignar cama: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Trasladar
  const handleTrasladar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBed || !trasladoIdCama) {
      alert('Selecciona la cama de destino.');
      return;
    }

    setIsSubmitting(true);
    try {
      const ingresoId = selectedBed.id_ingreso;
      if (!ingresoId) {
        throw new Error('No se encontró el ID de ingreso activo para esta cama.');
      }
      await apiClient.patch(`/ingresos/${ingresoId}/trasladar`, {
        id_cama: trasladoIdCama,
      });
      showNotification(`Traslado hospitalario completado con éxito hacia la nueva cama.`);
      setModalType(null);
      setSelectedBed(null);
      setTrasladoIdCama('');
      fetchHospitalizacion();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error en traslado: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Totales
  const totalCamas = beds.length || 17;
  const ocupadas = beds.filter((b) => b.estado === 'OCUPADA').length;
  const disponibles = totalCamas - ocupadas;
  const p3Ocupadas = beds.filter((b) => b.planta === 3 && b.estado === 'OCUPADA').length;
  const p2Ocupadas = beds.filter((b) => b.planta === 2 && b.estado === 'OCUPADA').length;

  const filteredBeds = beds.filter((b) => {
    if (activeTab === 'P3' && b.planta !== 3) return false;
    if (activeTab === 'P2' && b.planta !== 2) return false;
    if (filterQuery) {
      const q = filterQuery.toLowerCase();
      const codeMatches = b.codigo.toLowerCase().includes(q);
      const patientMatches = b.paciente?.nombre.toLowerCase().includes(q);
      const doctorMatches = b.paciente?.medico?.toLowerCase().includes(q);
      return codeMatches || patientMatches || doctorMatches;
    }
    return true;
  });

  const availableBedsForTransfer = beds.filter((b) => b.estado === 'DISPONIBLE');

  return (
    <div style={{ display: 'grid', gap: '24px' }}>
      {/* Top Header */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: '40px',
                height: '40px',
                borderRadius: '10px',
                background: '#e6f4ea',
                color: '#0b5d4b',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <BedIcon size={24} />
            </div>
            <div>
              <h1 style={{ fontSize: '1.5rem', fontWeight: 800, margin: 0, color: '#13221c' }}>
                Mapa de Camas e Ingresos
              </h1>
              <p style={{ margin: 0, fontSize: '0.85rem', color: '#526b60' }}>
                {isOps
                  ? 'Control operativo de camas: asignación de ingresos hospitalarios, traslados entre salas y altas médicas.'
                  : 'Supervisión institucional de capacidad hospitalaria, censo de camas VIP/Semiprivadas y auditoría de estancias.'}
              </p>
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <button
            onClick={() => {
              fetchHospitalizacion();
              showNotification('Camas y ocupación sincronizadas con la base de datos.');
            }}
            disabled={loading}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '8px 14px',
              borderRadius: '8px',
              border: '1px solid #cad8d1',
              background: '#ffffff',
              fontSize: '0.82rem',
              fontWeight: 600,
              cursor: 'pointer',
              color: '#13221c',
            }}
          >
            <RefreshCwIcon size={14} /> {loading ? 'Cargando...' : 'Sincronizar'}
          </button>
          {isOps && (
            <button
              onClick={() => {
                setSelectedBed(null);
                setModalType('INGRESO');
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                padding: '8px 16px',
                borderRadius: '8px',
                border: 'none',
                background: '#0b5d4b',
                color: '#ffffff',
                fontSize: '0.82rem',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              <PlusCircleIcon size={16} /> Asignar Ingreso
            </button>
          )}
        </div>
      </div>

      {feedback && (
        <div
          style={{
            padding: '12px 18px',
            background: '#ecfdf5',
            color: '#065f46',
            borderRadius: '10px',
            border: '1px solid #a7f3d0',
            fontWeight: 600,
            fontSize: '0.88rem',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
          }}
        >
          <span>✓</span> {feedback}
        </div>
      )}

      {errorMsg && (
        <div
          style={{
            padding: '12px 18px',
            background: '#fee2e2',
            color: '#b91c1c',
            borderRadius: '10px',
            border: '1px solid #fecaca',
            fontWeight: 600,
            fontSize: '0.88rem',
          }}
        >
          ⚠ {errorMsg}
        </div>
      )}

      {/* KPI Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '14px',
        }}
      >
        <div style={{ background: '#ffffff', padding: '16px', borderRadius: '12px', border: '1px solid #e2ece7' }}>
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#526b60', textTransform: 'uppercase' }}>
            Aforo Máximo Legal
          </span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#13221c', marginTop: '4px' }}>
            {totalCamas} Camas
          </div>
          <span style={{ fontSize: '0.75rem', color: '#849a90' }}>Planta 3 (5) + Planta 2 (12)</span>
        </div>

        <div style={{ background: '#ffffff', padding: '16px', borderRadius: '12px', border: '1px solid #e2ece7' }}>
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#0b5d4b', textTransform: 'uppercase' }}>
            Camas Disponibles
          </span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#0b5d4b', marginTop: '4px' }}>
            {disponibles} Libres
          </div>
          <span style={{ fontSize: '0.75rem', color: '#526b60' }}>
            {Math.round((disponibles / totalCamas) * 100)}% de margen hospitalario
          </span>
        </div>

        <div style={{ background: '#ffffff', padding: '16px', borderRadius: '12px', border: '1px solid #e2ece7' }}>
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#b45309', textTransform: 'uppercase' }}>
            Camas Ocupadas
          </span>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#d97706', marginTop: '4px' }}>
            {ocupadas} Camas
          </div>
          <span style={{ fontSize: '0.75rem', color: '#526b60' }}>Pacientes ingresados</span>
        </div>

        <div style={{ background: '#ffffff', padding: '16px', borderRadius: '12px', border: '1px solid #e2ece7' }}>
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#0369a1', textTransform: 'uppercase' }}>
            Distribución
          </span>
          <div style={{ fontSize: '1.1rem', fontWeight: 800, color: '#0369a1', marginTop: '6px' }}>
            P3 VIP: {p3Ocupadas}/5 · P2 Semi: {p2Ocupadas}/12
          </div>
          <span style={{ fontSize: '0.75rem', color: '#849a90' }}>Control por piso y tipología</span>
        </div>
      </div>

      {/* Tabs and Search */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '12px',
          background: '#ffffff',
          padding: '12px 16px',
          borderRadius: '12px',
          border: '1px solid #e2ece7',
        }}
      >
        <div style={{ display: 'flex', gap: '8px' }}>
          {[
            { id: 'ALL', label: 'Todas las Camas' },
            { id: 'P3', label: 'Planta 3 (5 VIP)' },
            { id: 'P2', label: 'Planta 2 (12 Semiprivadas)' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              style={{
                padding: '6px 14px',
                borderRadius: '6px',
                border: 'none',
                fontSize: '0.8rem',
                fontWeight: 700,
                cursor: 'pointer',
                background: activeTab === tab.id ? '#0b5d4b' : '#f0f4f2',
                color: activeTab === tab.id ? '#ffffff' : '#526b60',
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div style={{ position: 'relative', minWidth: '260px' }}>
          <div style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#849a90' }}>
            <SearchIcon size={16} />
          </div>
          <input
            type="text"
            placeholder="Buscar por código, paciente o médico..."
            value={filterQuery}
            onChange={(e) => setFilterQuery(e.target.value)}
            style={{
              width: '100%',
              padding: '8px 12px 8px 34px',
              borderRadius: '8px',
              border: '1px solid #cad8d1',
              fontSize: '0.85rem',
            }}
          />
        </div>
      </div>

      {/* Grid de Camas */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
          gap: '16px',
        }}
      >
        {filteredBeds.map((bed) => {
          const isOcupada = bed.estado === 'OCUPADA';
          return (
            <div
              key={bed.id}
              style={{
                background: '#ffffff',
                borderRadius: '14px',
                border: `1.5px solid ${isOcupada ? '#fde68a' : '#d1fae5'}`,
                padding: '18px',
                boxShadow: '0 2px 8px rgba(0,0,0,0.03)',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                gap: '14px',
              }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span
                      style={{
                        padding: '4px 10px',
                        borderRadius: '6px',
                        fontWeight: 800,
                        fontSize: '0.85rem',
                        background: bed.tipo === 'VIP' ? '#f0fdf4' : '#eff6ff',
                        color: bed.tipo === 'VIP' ? '#15803d' : '#1d4ed8',
                      }}
                    >
                      {bed.codigo}
                    </span>
                    <span style={{ fontSize: '0.75rem', color: '#849a90', fontWeight: 600 }}>
                      Planta {bed.planta} · {bed.tipo}
                    </span>
                  </div>

                  <span
                    style={{
                      padding: '3px 8px',
                      borderRadius: '999px',
                      fontSize: '0.72rem',
                      fontWeight: 800,
                      background: isOcupada ? '#fef3c7' : '#ecfdf5',
                      color: isOcupada ? '#b45309' : '#047857',
                      border: `1px solid ${isOcupada ? '#fde68a' : '#a7f3d0'}`,
                    }}
                  >
                    {isOcupada ? '● OCUPADA' : '○ DISPONIBLE'}
                  </span>
                </div>

                {isOcupada && bed.paciente ? (
                  <div style={{ marginTop: '14px', borderTop: '1px solid #f0f4f2', paddingTop: '12px' }}>
                    <div style={{ fontWeight: 800, fontSize: '0.92rem', color: '#13221c' }}>
                      {bed.paciente.nombre}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: '#849a90', marginTop: '2px' }}>
                      Exp: {bed.paciente.expediente} {bed.paciente.dui && `· DUI: ${bed.paciente.dui}`}
                    </div>
                    <div style={{ fontSize: '0.78rem', color: '#526b60', marginTop: '6px' }}>
                      <strong>Motivo:</strong> {bed.paciente.diagnostico}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: '#0b5d4b', marginTop: '4px', fontWeight: 600 }}>
                      Tratante: {bed.paciente.medico}
                    </div>
                  </div>
                ) : (
                  <div style={{ marginTop: '16px', color: '#849a90', fontSize: '0.82rem', fontStyle: 'italic' }}>
                    Cama lista y esterilizada para ingreso inmediato.
                  </div>
                )}
              </div>

              {/* Botones de acción */}
              <div style={{ display: 'flex', gap: '8px', borderTop: '1px solid #f0f4f2', paddingTop: '12px' }}>
                {isOps ? (
                  isOcupada ? (
                    <>
                      <button
                        onClick={() => {
                          setSelectedBed(bed);
                          setModalType('ALTA');
                        }}
                        style={{
                          flex: 1,
                          padding: '6px 10px',
                          borderRadius: '6px',
                          border: '1px solid #cbd5e1',
                          background: '#ffffff',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          color: '#0b5d4b',
                          cursor: 'pointer',
                        }}
                      >
                        Dar de Alta
                      </button>
                      <button
                        onClick={() => {
                          setSelectedBed(bed);
                          setTrasladoIdCama(availableBedsForTransfer[0]?.id || '');
                          setModalType('TRASLADO');
                        }}
                        style={{
                          flex: 1,
                          padding: '6px 10px',
                          borderRadius: '6px',
                          border: '1px solid #d1d5db',
                          background: '#f8faf9',
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          color: '#374151',
                          cursor: 'pointer',
                        }}
                      >
                        Trasladar
                      </button>
                    </>
                  ) : (
                    <button
                      onClick={() => {
                        setSelectedBed(bed);
                        setIngresoForm((prev) => ({ ...prev, tipo: bed.tipo }));
                        setModalType('INGRESO');
                      }}
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        borderRadius: '6px',
                        border: 'none',
                        background: '#0b5d4b',
                        color: '#ffffff',
                        fontSize: '0.78rem',
                        fontWeight: 700,
                        cursor: 'pointer',
                      }}
                    >
                      Asignar Cama a Paciente
                    </button>
                  )
                ) : (
                  <div
                    style={{
                      width: '100%',
                      padding: '7px 10px',
                      borderRadius: '6px',
                      background: isOcupada ? '#fef3c7' : '#ecfdf5',
                      color: isOcupada ? '#92400e' : '#047857',
                      fontSize: '0.74rem',
                      fontWeight: 700,
                      textAlign: 'center',
                    }}
                  >
                    {isOcupada ? '• Paciente Hospitalizado (Auditoría)' : '✓ Cama Disponible'}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* MODAL INGRESO */}
      {modalType === 'INGRESO' && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 50,
            padding: '16px',
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              maxWidth: '480px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
            }}
          >
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
              Asignar Ingreso Hospitalario
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.82rem', color: '#526b60' }}>
              {selectedBed
                ? `Asignación directa para la cama ${selectedBed.codigo} (${selectedBed.tipo} - Planta ${selectedBed.planta}).`
                : 'Asignación hospitalaria según capacidad disponible.'}
            </p>

            <form onSubmit={handleAsignarIngreso} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Paciente
                </label>
                <select
                  value={ingresoForm.id_paciente}
                  onChange={(e) => setIngresoForm({ ...ingresoForm, id_paciente: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                  required
                >
                  <option value="">-- Seleccionar Paciente --</option>
                  {pacientes.map((p) => (
                    <option key={p.id_paciente} value={p.id_paciente}>
                      {p.nombre_completo} ({p.numero_expediente})
                    </option>
                  ))}
                </select>
              </div>

              {!selectedBed && (
                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                    Tipo de Habitación Requerida
                  </label>
                  <select
                    value={ingresoForm.tipo}
                    onChange={(e) => setIngresoForm({ ...ingresoForm, tipo: e.target.value as any })}
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      borderRadius: '8px',
                      border: '1px solid #cad8d1',
                      fontSize: '0.85rem',
                    }}
                  >
                    <option value="SEMIPRIVADA">Semiprivada (Planta 2 - 12 Camas)</option>
                    <option value="VIP">VIP Individual (Planta 3 - 5 Camas)</option>
                  </select>
                </div>
              )}

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Médico Tratante a Cargo
                </label>
                <select
                  value={ingresoForm.id_medico}
                  onChange={(e) => setIngresoForm({ ...ingresoForm, id_medico: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                  required
                >
                  <option value="">-- Seleccionar Médico --</option>
                  {medicos.map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.nombre_completo} ({m.cargo || 'Médico'})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Motivo o Diagnóstico de Ingreso
                </label>
                <textarea
                  value={ingresoForm.motivo}
                  onChange={(e) => setIngresoForm({ ...ingresoForm, motivo: e.target.value })}
                  placeholder="Ej. Postoperatorio inmediato de colecistectomía..."
                  rows={3}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                  required
                />
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  onClick={() => {
                    setModalType(null);
                    setSelectedBed(null);
                  }}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    background: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: 'none',
                    background: '#0b5d4b',
                    color: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isSubmitting ? 'Asignando...' : 'Confirmar Ingreso'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL ALTA */}
      {modalType === 'ALTA' && selectedBed && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 50,
            padding: '16px',
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              maxWidth: '440px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
            }}
          >
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
              Confirmar Alta Hospitalaria
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.85rem', color: '#526b60' }}>
              ¿Confirmas el alta médica de <strong>{selectedBed.paciente?.nombre}</strong>? La cama{' '}
              <strong>{selectedBed.codigo}</strong> quedará liberada y disponible inmediatamente.
            </p>

            <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
              <button
                type="button"
                onClick={() => {
                  setModalType(null);
                  setSelectedBed(null);
                }}
                style={{
                  flex: 1,
                  padding: '10px',
                  borderRadius: '8px',
                  border: '1px solid #cbd5e1',
                  background: '#ffffff',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                Volver
              </button>
              <button
                type="button"
                onClick={handleDarDeAlta}
                disabled={isSubmitting}
                style={{
                  flex: 1,
                  padding: '10px',
                  borderRadius: '8px',
                  border: 'none',
                  background: '#0b5d4b',
                  color: '#ffffff',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                {isSubmitting ? 'Procesando...' : 'Confirmar Alta'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL TRASLADO */}
      {modalType === 'TRASLADO' && selectedBed && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 50,
            padding: '16px',
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              maxWidth: '460px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
            }}
          >
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: '0 0 6px', color: '#13221c' }}>
              Trasladar Paciente de Cama
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.82rem', color: '#526b60' }}>
              Paciente: <strong>{selectedBed.paciente?.nombre}</strong> (Origen: {selectedBed.codigo})
            </p>

            <form onSubmit={handleTrasladar} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Cama de Destino Disponible
                </label>
                <select
                  value={trasladoIdCama}
                  onChange={(e) => setTrasladoIdCama(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                  }}
                  required
                >
                  <option value="">-- Seleccionar Cama Disponible --</option>
                  {availableBedsForTransfer.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.codigo} ({b.tipo} - Planta {b.planta})
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  onClick={() => {
                    setModalType(null);
                    setSelectedBed(null);
                  }}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    background: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting || !trasladoIdCama}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: 'none',
                    background: '#0b5d4b',
                    color: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isSubmitting ? 'Trasladando...' : 'Ejecutar Traslado'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
