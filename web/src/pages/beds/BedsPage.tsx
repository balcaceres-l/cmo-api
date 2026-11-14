import React, { useState, useEffect, useMemo } from 'react';
import {
  BedIcon,
  SearchIcon,
  PlusCircleIcon,
  RefreshCwIcon,
  ActivityIcon,
  FileTextIcon,
} from '../../components/Icons';
import { apiClient, ApiError } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { isJefeOperaciones } from '../../utils/roles';
import {
  BedData,
  IngresoRecord,
  MedicoOption,
  ModalType,
  PacienteOption,
  RoomGroup,
  getStayHours,
} from './types';
import { BedsKpiHeader } from './components/BedsKpiHeader';
import { FloorMapView } from './components/FloorMapView';
import { BedDetailDrawer } from './components/BedDetailDrawer';
import { AdmissionsAuditTable } from './components/AdmissionsAuditTable';
import { BedModals } from './components/BedModals';

const FALLBACK_BEDS: BedData[] = [
  { id: 'b-301', codigo: 'VIP-301', planta: 3, tipo: 'VIP', habitacion: 'VIP-301', estado: 'DISPONIBLE' },
  { id: 'b-302', codigo: 'VIP-302', planta: 3, tipo: 'VIP', habitacion: 'VIP-302', estado: 'DISPONIBLE' },
  { id: 'b-303', codigo: 'VIP-303', planta: 3, tipo: 'VIP', habitacion: 'VIP-303', estado: 'DISPONIBLE' },
  { id: 'b-304', codigo: 'VIP-304', planta: 3, tipo: 'VIP', habitacion: 'VIP-304', estado: 'DISPONIBLE' },
  { id: 'b-305', codigo: 'VIP-305', planta: 3, tipo: 'VIP', habitacion: 'VIP-305', estado: 'DISPONIBLE' },
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
  const [ingresosHistory, setIngresosHistory] = useState<IngresoRecord[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [viewMode, setViewMode] = useState<'MAP' | 'AUDIT'>('MAP');
  const [floorFilter, setFloorFilter] = useState<'ALL' | 'P3' | 'P2'>('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'DISPONIBLE' | 'OCUPADA'>('ALL');
  const [filterQuery, setFilterQuery] = useState('');

  const [drawerBed, setDrawerBed] = useState<BedData | null>(null);
  const [modalBed, setModalBed] = useState<BedData | null>(null);
  const [modalType, setModalType] = useState<ModalType>(null);

  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // Auxiliares para modales operativos (Jefe de Operaciones)
  const [pacientes, setPacientes] = useState<PacienteOption[]>([]);
  const [medicos, setMedicos] = useState<MedicoOption[]>([]);
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
        apiClient.get('/ingresos?per_page=100'),
      ]);

      const allIngresos: IngresoRecord[] = Array.isArray(ingresosRes)
        ? ingresosRes
        : ingresosRes?.data || [];
      setIngresosHistory(allIngresos);

      const activeIngresos = allIngresos.filter((i) => !i.fecha_alta);
      const ingresoPorCama = new Map<string, IngresoRecord>();
      activeIngresos.forEach((ing) => {
        if (ing.cama?.codigo) ingresoPorCama.set(ing.cama.codigo, ing);
        if (ing.id_cama) ingresoPorCama.set(ing.id_cama, ing);
      });

      const flatBeds: BedData[] = [];
      const processHabitacion = (hab: any, plantaNum: number) => {
        if (!hab.camas || !Array.isArray(hab.camas)) return;
        hab.camas.forEach((c: any) => {
          const matchingIngreso = ingresoPorCama.get(c.codigo) || (c.id_cama ? ingresoPorCama.get(c.id_cama) : undefined);
          const isOcupada = c.estado === 'OCUPADA' || !!matchingIngreso || !!c.ingreso;
          const letMatch = c.codigo.endsWith('-A') ? 'A' : c.codigo.endsWith('-B') ? 'B' : undefined;

          const fechaIso = c.ingreso?.fecha_ingreso || matchingIngreso?.fecha_ingreso;
          const pacienteInfo = isOcupada
            ? {
                id_paciente: c.paciente?.id_paciente || matchingIngreso?.paciente?.id_paciente,
                nombre: c.paciente?.nombre_completo || matchingIngreso?.paciente?.nombre_completo || 'Paciente Hospitalizado',
                expediente: c.paciente?.numero_expediente || matchingIngreso?.paciente?.numero_expediente || 'EXP-2026',
                dui: c.paciente?.dui || matchingIngreso?.paciente?.dui,
                diagnostico: c.ingreso?.motivo || matchingIngreso?.motivo || 'Observación clínica',
                medico: c.ingreso?.medico || matchingIngreso?.medico?.nombre_completo || 'Médico de Turno',
                fechaIngresoIso: fechaIso,
                fechaIngreso: fechaIso ? new Date(fechaIso).toLocaleString() : 'Reciente',
                registradoPor:
                  c.ingreso?.registrado_por || matchingIngreso?.registrado_por?.nombre_completo,
              }
            : undefined;

          flatBeds.push({
            id: c.id_cama || c.codigo,
            codigo: c.codigo,
            planta: plantaNum,
            tipo: hab.tipo || (plantaNum === 3 ? 'VIP' : 'SEMIPRIVADA'),
            letra: letMatch as 'A' | 'B' | undefined,
            habitacion: hab.codigo,
            estado: isOcupada ? 'OCUPADA' : 'DISPONIBLE',
            id_ingreso: c.ingreso?.id_ingreso || matchingIngreso?.id_ingreso,
            paciente: pacienteInfo,
          });
        });
      };

      const rawPlants = habsRes;
      if (rawPlants && typeof rawPlants === 'object') {
        if (Array.isArray(rawPlants['3'])) rawPlants['3'].forEach((h: any) => processHabitacion(h, 3));
        if (Array.isArray(rawPlants['2'])) rawPlants['2'].forEach((h: any) => processHabitacion(h, 2));
        if (Array.isArray(rawPlants)) {
          rawPlants.forEach((h: any) => processHabitacion(h, h.planta || (h.tipo === 'VIP' ? 3 : 2)));
        }
      }

      const resolvedBeds = flatBeds.length > 0 ? flatBeds : FALLBACK_BEDS;
      setBeds(resolvedBeds);

      // Si el drawer está abierto, actualizar su referencia con los datos frescos
      setDrawerBed((prev) => (prev ? resolvedBeds.find((b) => b.id === prev.id || b.codigo === prev.codigo) || null : null));
    } catch {
      setErrorMsg('No se pudo conectar con la API de habitaciones. Mostrando mapa base local.');
      setBeds(FALLBACK_BEDS);
    } finally {
      setLoading(false);
    }
  };

  const fetchAuxiliares = async () => {
    if (!isOps) return;
    try {
      const [pacRes, medRes]: any[] = await Promise.all([
        apiClient.get('/pacientes?per_page=100'),
        apiClient.get('/usuarios?rol=MEDICO&per_page=100'),
      ]);
      const pacList = Array.isArray(pacRes) ? pacRes : pacRes?.data || [];
      const medList = Array.isArray(medRes) ? medRes : medRes?.data || [];
      setPacientes(pacList);
      setMedicos(medList);
      if (pacList.length > 0) setIngresoForm((p) => ({ ...p, id_paciente: pacList[0].id_paciente }));
      if (medList.length > 0) setIngresoForm((p) => ({ ...p, id_medico: medList[0].id }));
    } catch {
      // Silencioso si no hay conexión auxiliar
    }
  };

  useEffect(() => {
    fetchHospitalizacion();
    fetchAuxiliares();
  }, [isOps]);

  const handleDarDeAlta = async () => {
    if (!modalBed || !isOps) return;
    setIsSubmitting(true);
    try {
      let ingresoId = modalBed.id_ingreso;
      if (!ingresoId) {
        const res: any = await apiClient.get('/ingresos?activos=1');
        const list = Array.isArray(res) ? res : res?.data || [];
        const match = list.find((i: any) => i.cama?.codigo === modalBed.codigo || i.id_cama === modalBed.id);
        ingresoId = match?.id_ingreso || match?.id;
      }
      if (!ingresoId) throw new Error('No se encontró el ingreso activo de esta cama.');
      await apiClient.patch(`/ingresos/${ingresoId}/alta`);
      showNotification(`Alta procesada exitosamente para la cama ${modalBed.codigo}.`);
      setModalType(null);
      setModalBed(null);
      setDrawerBed(null);
      fetchHospitalizacion();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al procesar alta: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleAsignarIngreso = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isOps) return;
    if (!ingresoForm.id_paciente || !ingresoForm.id_medico || !ingresoForm.motivo.trim()) {
      alert('Por favor completa todos los campos del ingreso.');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload: Record<string, any> = {
        id_paciente: ingresoForm.id_paciente,
        tipo: modalBed ? modalBed.tipo : ingresoForm.tipo,
        id_medico: ingresoForm.id_medico,
        motivo: ingresoForm.motivo.trim(),
      };
      if (modalBed && modalBed.estado === 'DISPONIBLE') {
        payload.id_cama = modalBed.id;
      }

      await apiClient.post('/ingresos', payload);
      showNotification('Ingreso hospitalario registrado exitosamente.');
      setModalType(null);
      setModalBed(null);
      setDrawerBed(null);
      setIngresoForm((prev) => ({ ...prev, motivo: '' }));
      fetchHospitalizacion();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al asignar cama: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleTrasladar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isOps || !modalBed || !trasladoIdCama) return;

    setIsSubmitting(true);
    try {
      if (!modalBed.id_ingreso) {
        throw new Error('No se encontró el ID de ingreso activo para esta cama.');
      }
      await apiClient.patch(`/ingresos/${modalBed.id_ingreso}/trasladar`, {
        id_cama: trasladoIdCama,
      });
      showNotification('Traslado hospitalario completado sin generar alta ficticia.');
      setModalType(null);
      setModalBed(null);
      setDrawerBed(null);
      setTrasladoIdCama('');
      fetchHospitalizacion();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error en traslado: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Agrupación por habitación y filtrado
  const filteredBeds = useMemo(() => {
    return beds.filter((b) => {
      if (floorFilter === 'P3' && b.planta !== 3) return false;
      if (floorFilter === 'P2' && b.planta !== 2) return false;
      if (statusFilter !== 'ALL' && b.estado !== statusFilter) return false;
      if (filterQuery.trim()) {
        const q = filterQuery.toLowerCase();
        const matchCode = b.codigo.toLowerCase().includes(q) || b.habitacion.toLowerCase().includes(q);
        const matchPatient = b.paciente?.nombre.toLowerCase().includes(q) || b.paciente?.expediente.toLowerCase().includes(q);
        const matchDoc = b.paciente?.medico?.toLowerCase().includes(q);
        return matchCode || matchPatient || matchDoc;
      }
      return true;
    });
  }, [beds, floorFilter, statusFilter, filterQuery]);

  const rooms = useMemo<RoomGroup[]>(() => {
    const map = new Map<string, RoomGroup>();
    filteredBeds.forEach((bed) => {
      const key = `${bed.planta}-${bed.habitacion}`;
      if (!map.has(key)) {
        map.set(key, {
          codigo: bed.habitacion,
          planta: bed.planta,
          tipo: bed.tipo,
          camas: [],
        });
      }
      map.get(key)!.camas.push(bed);
    });
    return Array.from(map.values()).sort((a, b) => b.planta - a.planta || a.codigo.localeCompare(b.codigo));
  }, [filteredBeds]);

  // Habitaciones semiprivadas parcialmente ocupadas (1/2) sobre el total sin filtrar
  const halfOccupiedRoomsCount = useMemo(() => {
    const byRoom = new Map<string, BedData[]>();
    beds
      .filter((b) => b.planta === 2)
      .forEach((b) => {
        const arr = byRoom.get(b.habitacion) || [];
        arr.push(b);
        byRoom.set(b.habitacion, arr);
      });
    let count = 0;
    byRoom.forEach((arr) => {
      const occ = arr.filter((c) => c.estado === 'OCUPADA').length;
      if (occ === 1 && arr.length === 2) count += 1;
    });
    return count;
  }, [beds]);

  const longStayBeds = useMemo(
    () => beds.filter((b) => b.estado === 'OCUPADA' && getStayHours(b.paciente?.fechaIngresoIso) >= 48),
    [beds]
  );

  const availableBedsForTransfer = useMemo(
    () => beds.filter((b) => b.estado === 'DISPONIBLE' && b.id !== modalBed?.id),
    [beds, modalBed]
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
      {/* Cabecera */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: '11px',
              background: isOps ? '#e0f2fe' : '#fef3c7',
              color: isOps ? '#0369a1' : '#92400e',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <BedIcon size={24} />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
              <h1 style={{ fontSize: '1.45rem', fontWeight: 800, margin: 0, color: '#13221c' }}>
                {isOps ? 'Control Operativo de Camas' : 'Supervisión y Censo de Camas'}
              </h1>
              <span
                style={{
                  fontSize: '0.7rem',
                  fontWeight: 800,
                  padding: '3px 9px',
                  borderRadius: '999px',
                  background: isOps ? '#e0f2fe' : '#fef3c7',
                  color: isOps ? '#0369a1' : '#92400e',
                  border: `1px solid ${isOps ? '#bae6fd' : '#fde68a'}`,
                }}
              >
                {isOps ? 'OPERACIONES · INTERVENCIÓN HABILITADA' : 'DIRECCIÓN · SOLO LECTURA'}
              </span>
            </div>
            <p style={{ margin: '2px 0 0', fontSize: '0.84rem', color: '#526b60' }}>
              {isOps
                ? 'Monitoreo en tiempo real, detección de cuellos de botella y resolución de traslados o ingresos.'
                : 'Supervisión institucional de capacidad (17 camas), ocupación por tipología y auditoría de movimientos.'}
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <button
            type="button"
            onClick={() => {
              fetchHospitalizacion();
              showNotification('Censo de camas sincronizado.');
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
            <RefreshCwIcon size={14} /> {loading ? 'Sincronizando...' : 'Sincronizar'}
          </button>

          {isOps && (
            <button
              type="button"
              onClick={() => {
                setModalBed(null);
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

      {/* Notificaciones */}
      {feedback && (
        <div
          style={{
            padding: '11px 16px',
            background: '#ecfdf5',
            color: '#065f46',
            borderRadius: '10px',
            border: '1px solid #a7f3d0',
            fontWeight: 600,
            fontSize: '0.86rem',
          }}
        >
          ✓ {feedback}
        </div>
      )}
      {errorMsg && (
        <div
          style={{
            padding: '11px 16px',
            background: '#fef3c7',
            color: '#92400e',
            borderRadius: '10px',
            border: '1px solid #fde68a',
            fontWeight: 600,
            fontSize: '0.84rem',
          }}
        >
          ⚠ {errorMsg}
        </div>
      )}

      {/* KPIs */}
      <BedsKpiHeader
        isOps={isOps}
        beds={beds}
        ingresosHistory={ingresosHistory}
        halfOccupiedRoomsCount={halfOccupiedRoomsCount}
      />

      {/* Franja de alertas operativas para Jefe de Operaciones */}
      {isOps && (longStayBeds.length > 0 || halfOccupiedRoomsCount > 0) && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '10px',
            padding: '10px 16px',
            borderRadius: '10px',
            background: '#f0f9ff',
            border: '1px solid #bae6fd',
            color: '#0369a1',
            fontSize: '0.82rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 600 }}>
            <ActivityIcon size={16} />
            <span>
              <strong>Focos operativos:</strong>{' '}
              {longStayBeds.length > 0
                ? `${longStayBeds.length} paciente(s) con estancia ≥ 48h (${longStayBeds.map((b) => b.codigo).join(', ')}). `
                : ''}
              {halfOccupiedRoomsCount > 0
                ? `${halfOccupiedRoomsCount} habitación(es) semiprivada(s) ocupada(s) al 50% (1/2).`
                : ''}
            </span>
          </div>
          <span style={{ fontSize: '0.75rem', fontWeight: 700 }}>
            Haz clic en cualquier cama para ver detalles o trasladar
          </span>
        </div>
      )}

      {/* Barra de navegación: Mapa vs Auditoría + Filtros */}
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
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
          <button
            type="button"
            onClick={() => setViewMode('MAP')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '7px 14px',
              borderRadius: '8px',
              border: 'none',
              fontSize: '0.82rem',
              fontWeight: 700,
              cursor: 'pointer',
              background: viewMode === 'MAP' ? '#13221c' : '#f0f4f2',
              color: viewMode === 'MAP' ? '#ffffff' : '#526b60',
            }}
          >
            <BedIcon size={15} /> Mapa por Planta
          </button>
          <button
            type="button"
            onClick={() => setViewMode('AUDIT')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '7px 14px',
              borderRadius: '8px',
              border: 'none',
              fontSize: '0.82rem',
              fontWeight: 700,
              cursor: 'pointer',
              background: viewMode === 'AUDIT' ? '#13221c' : '#f0f4f2',
              color: viewMode === 'AUDIT' ? '#ffffff' : '#526b60',
            }}
          >
            <FileTextIcon size={15} /> {isOps ? 'Historial Operativo' : 'Auditoría e Historial'}
          </button>

          {viewMode === 'MAP' && (
            <>
              <span style={{ color: '#cbd5e1', margin: '0 4px' }}>|</span>
              {[
                { id: 'ALL', label: 'Todas' },
                { id: 'P3', label: 'Planta 3 (VIP)' },
                { id: 'P2', label: 'Planta 2 (Semi)' },
              ].map((tab) => (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setFloorFilter(tab.id as any)}
                  style={{
                    padding: '6px 12px',
                    borderRadius: '6px',
                    border: 'none',
                    fontSize: '0.78rem',
                    fontWeight: 700,
                    cursor: 'pointer',
                    background: floorFilter === tab.id ? '#0b5d4b' : '#f0f4f2',
                    color: floorFilter === tab.id ? '#ffffff' : '#526b60',
                  }}
                >
                  {tab.label}
                </button>
              ))}

              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as any)}
                style={{
                  padding: '6px 10px',
                  borderRadius: '6px',
                  border: '1px solid #cad8d1',
                  fontSize: '0.78rem',
                  fontWeight: 600,
                  color: '#13221c',
                  background: '#ffffff',
                }}
              >
                <option value="ALL">Estado: Todos</option>
                <option value="DISPONIBLE">Solo Libres</option>
                <option value="OCUPADA">Solo Ocupadas</option>
              </select>
            </>
          )}
        </div>

        {viewMode === 'MAP' && (
          <div style={{ position: 'relative', minWidth: '250px' }}>
            <div style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#849a90' }}>
              <SearchIcon size={15} />
            </div>
            <input
              type="text"
              placeholder="Buscar cama, habitación, paciente o médico..."
              value={filterQuery}
              onChange={(e) => setFilterQuery(e.target.value)}
              style={{
                width: '100%',
                padding: '7px 12px 7px 32px',
                borderRadius: '8px',
                border: '1px solid #cad8d1',
                fontSize: '0.82rem',
              }}
            />
          </div>
        )}
      </div>

      {/* Vista principal */}
      {viewMode === 'MAP' ? (
        <FloorMapView
          rooms={rooms}
          selectedBedId={drawerBed?.id}
          onSelectBed={(bed) => setDrawerBed(bed)}
          isOps={isOps}
        />
      ) : (
        <AdmissionsAuditTable ingresos={ingresosHistory} isOps={isOps} />
      )}

      {/* Drawer de detalle */}
      <BedDetailDrawer
        bed={drawerBed}
        isOps={isOps}
        onClose={() => setDrawerBed(null)}
        onOpenIngreso={(bed) => {
          setModalBed(bed);
          setIngresoForm((prev) => ({ ...prev, tipo: bed.tipo }));
          setModalType('INGRESO');
        }}
        onOpenTraslado={(bed) => {
          setModalBed(bed);
          const firstAvail = beds.find((b) => b.estado === 'DISPONIBLE' && b.id !== bed.id);
          setTrasladoIdCama(firstAvail?.id || '');
          setModalType('TRASLADO');
        }}
        onOpenAlta={(bed) => {
          setModalBed(bed);
          setModalType('ALTA');
        }}
      />

      {/* Modales operativos — exclusivos de Jefe de Operaciones */}
      {isOps && (
        <BedModals
          modalType={modalType}
          selectedBed={modalBed}
          pacientes={pacientes}
          medicos={medicos}
          availableBedsForTransfer={availableBedsForTransfer}
          ingresoForm={ingresoForm}
          trasladoIdCama={trasladoIdCama}
          isSubmitting={isSubmitting}
          onChangeIngresoForm={setIngresoForm}
          onChangeTrasladoIdCama={setTrasladoIdCama}
          onClose={() => {
            setModalType(null);
            setModalBed(null);
          }}
          onSubmitIngreso={handleAsignarIngreso}
          onConfirmAlta={handleDarDeAlta}
          onSubmitTraslado={handleTrasladar}
        />
      )}
    </div>
  );
};
