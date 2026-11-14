import React, { useState, useEffect, useMemo } from 'react';
import { CalendarIcon, PlusCircleIcon, RefreshCwIcon } from '../../components/Icons';
import { apiClient, ApiError } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { isJefeOperaciones, getRoleBadgeConfig } from '../../utils/roles';
import { PersonalOption, ShiftPrefill, ShiftViewMode, TurnoRaw, WeekGroupBy } from './types';
import { addDays, addMonths, CORE_AREAS, getMonthGridDays, getWeekDays, isStaffActiveOnDate, normalizeSpecialtyLabel } from './shiftUtils';
import { SuggestedShift } from './shiftSuggester';
import { ShiftToolbar } from './components/ShiftToolbar';
import { DayTimelineView } from './components/DayTimelineView';
import { WeekGridView } from './components/WeekGridView';
import { MonthCalendarView } from './components/MonthCalendarView';
import { ShiftDetailDrawer } from './components/ShiftDetailDrawer';
import { SpecialtyCoverageMonitor } from './components/SpecialtyCoverageMonitor';
import { SuggestCoverageModal } from './components/SuggestCoverageModal';

const FALLBACK_SHIFTS: TurnoRaw[] = [
  {
    id: 't-hoy-1',
    id_usuario: 'u-1',
    empleado: 'Dra. Ana Martínez',
    cargo: 'Medicina General',
    rol: 'MEDICO',
    area: 'Consulta Externa (C-101)',
    fecha: new Date().toISOString().split('T')[0],
    hora_inicio: '08:00',
    hora_fin: '16:00',
  },
  {
    id: 't-hoy-2',
    id_usuario: 'u-2',
    empleado: 'Dr. Carlos López',
    cargo: 'Emergencia y Trauma',
    rol: 'MEDICO',
    area: 'Servicio de Emergencias',
    fecha: new Date().toISOString().split('T')[0],
    hora_inicio: '08:00',
    hora_fin: '16:00',
  },
  {
    id: 't-hoy-3',
    id_usuario: 'u-3',
    empleado: 'Dra. Sofía Rivera',
    cargo: 'Alergología e Inmunología',
    rol: 'MEDICO',
    area: 'Consulta Externa (C-102)',
    fecha: new Date().toISOString().split('T')[0],
    hora_inicio: '16:00',
    hora_fin: '23:59',
  },
  {
    id: 't-hoy-4',
    id_usuario: 'u-4',
    empleado: 'José Ramírez',
    cargo: 'Enfermero de Turno',
    rol: 'RECEPCIONISTA',
    area: 'Triaje y Emergencias (Enfermería)',
    fecha: new Date().toISOString().split('T')[0],
    hora_inicio: '08:00',
    hora_fin: '16:00',
  },
  {
    id: 't-hoy-5',
    id_usuario: 'u-5',
    empleado: 'María Hernández',
    cargo: 'Recepcionista',
    rol: 'RECEPCIONISTA',
    area: 'Admisión Central y Recepción',
    fecha: new Date().toISOString().split('T')[0],
    hora_inicio: '08:00',
    hora_fin: '16:00',
  },
];

export const ShiftsPage: React.FC = () => {
  const { user } = useAuth();
  const isOps = isJefeOperaciones(user);

  const todayStr = new Date().toLocaleDateString('en-CA');
  const now = new Date();
  const nowHHMM = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

  const [shifts, setShifts] = useState<TurnoRaw[]>([]);
  const [personal, setPersonal] = useState<PersonalOption[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [filterRol, setFilterRol] = useState<string>('TODOS');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFechaGuardia, setSelectedFechaGuardia] = useState<string>(todayStr);
  const [isGuardiaOpen, setIsGuardiaOpen] = useState<boolean>(true);
  const [showModal, setShowModal] = useState<boolean>(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Vistas de Cuadrícula (Día / Semana / Mes / Lista)
  const [viewMode, setViewMode] = useState<ShiftViewMode>('week');
  const [anchorDate, setAnchorDate] = useState<string>(todayStr);
  const [weekGroupBy, setWeekGroupBy] = useState<WeekGroupBy>('area');
  const [drawerTurno, setDrawerTurno] = useState<TurnoRaw | null>(null);
  const [showSuggestModal, setShowSuggestModal] = useState<boolean>(false);
  const [modalSpecialtyFilter, setModalSpecialtyFilter] = useState<string | null>(null);
  const [showClearRangeModal, setShowClearRangeModal] = useState<boolean>(false);
  const [clearRange, setClearRange] = useState<{ from: string; to: string }>(() => {
    const week = getWeekDays(new Date().toISOString().split('T')[0]);
    return { from: week[0], to: addDays(week[6], 7) };
  });
  const [isClearingRange, setIsClearingRange] = useState<boolean>(false);

  // Modal Relevo de Guardia / Incapacidad
  const [selectedTurno, setSelectedTurno] = useState<TurnoRaw | null>(null);
  const [showReliefModal, setShowReliefModal] = useState<boolean>(false);
  const [relevoForm, setRelevoForm] = useState({
    id_sustituto: '',
    motivo: 'Incapacidad médica certificada',
    observaciones: '',
  });

  // Formulario de Turno Nuevo (Turno estándar de 8h: Mañana 08:00 – 16:00 por defecto)
  const [turnoForm, setTurnoForm] = useState({
    id_usuario: '',
    fecha: new Date().toISOString().split('T')[0],
    hora_inicio: '08:00',
    hora_fin: '16:00',
    area: 'Consulta Externa',
  });

  const notify = (msg: string) => {
    setFeedback(msg);
    setTimeout(() => setFeedback(null), 4500);
  };

  const fetchTurnos = async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const firstRes: any = await apiClient.get('/turnos?per_page=100&page=1');
      const extractList = (r: any) =>
        Array.isArray(r) ? r : Array.isArray(r?.data) ? r.data : r?.data?.data || [];

      let list: any[] = extractList(firstRes);
      const lastPage = Number(firstRes?.last_page || 1);

      if (lastPage > 1) {
        const restPages = await Promise.all(
          Array.from({ length: lastPage - 1 }, (_, i) =>
            apiClient.get(`/turnos?per_page=100&page=${i + 2}`)
          )
        );
        restPages.forEach((r) => {
          list = list.concat(extractList(r));
        });
      }

      const mapped: TurnoRaw[] = list.map((t: any) => ({
        id: t.id_turno || t.id,
        id_usuario: t.id_usuario,
        empleado: t.usuario?.nombre_completo || 'Personal',
        cargo:
          t.usuario?.rol === 'MEDICO'
            ? t.usuario?.especialidad_nombre || t.usuario?.especialidad?.nombre || t.usuario?.cargo || 'Medicina general'
            : t.usuario?.cargo || 'Personal Hospitalario',
        rol: t.usuario?.rol || 'MEDICO',
        area: t.area || 'Hospital General',
        fecha: t.fecha ? String(t.fecha).split('T')[0] : '',
        hora_inicio: t.hora_inicio ? String(t.hora_inicio).slice(0, 5) : '00:00',
        hora_fin: t.hora_fin ? String(t.hora_fin).slice(0, 5) : '00:00',
      }));
      setShifts(mapped);
    } catch {
      setShifts(FALLBACK_SHIFTS);
    } finally {
      setLoading(false);
    }
  };

  const fetchPersonal = async () => {
    try {
      const res: any = await apiClient.get('/usuarios?per_page=500');
      const list = Array.isArray(res)
        ? res
        : Array.isArray(res?.data)
        ? res.data
        : res?.data?.data || [];
      if (list.length > 0) {
        const activePersonal = list
          .filter(
            (u: any) =>
              u.rol !== 'PACIENTE' &&
              u.rol !== 'ADMINISTRADOR' &&
              (!u.estado || String(u.estado).toUpperCase() === 'ACTIVO')
          )
          .map((u: any) => ({
            ...u,
            cargo:
              u.rol === 'MEDICO'
                ? u.especialidad_nombre || u.especialidad?.nombre || u.cargo || 'Medicina general'
                : u.cargo,
          }));
        setPersonal(activePersonal);
        if (activePersonal.length > 0) {
          setTurnoForm((prev) => ({ ...prev, id_usuario: prev.id_usuario || activePersonal[0].id }));
        }
      }
    } catch {
      // Ignorar
    }
  };

  useEffect(() => {
    fetchTurnos();
    fetchPersonal();
  }, []);

  const handleCrearTurno = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!turnoForm.id_usuario || !turnoForm.fecha || !turnoForm.area.trim()) {
      alert('Completa empleado, fecha y área de atención.');
      return;
    }

    setIsSubmitting(true);
    try {
      await apiClient.post('/turnos', turnoForm);
      notify('Turno laboral asignado y guardado en la base de datos.');
      setShowModal(false);
      fetchTurnos();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al asignar turno: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEliminarTurno = async (turno: TurnoRaw) => {
    if (!confirm(`¿Deseas retirar el turno de ${turno.empleado} el ${turno.fecha}?`)) return;

    try {
      await apiClient.delete(`/turnos/${turno.id}`);
      notify('Turno eliminado correctamente.');
      fetchTurnos();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`No se pudo eliminar turno: ${e.message}`);
    }
  };

  // Abrir Modal de Relevo / Incapacidad
  const handleOpenRelief = (turno: TurnoRaw) => {
    setSelectedTurno(turno);
    const candidatos = personal.filter(
      (p) => String(p.id) !== String(turno.id_usuario) && p.rol === turno.rol
    );
    setRelevoForm({
      id_sustituto: candidatos[0]?.id || '',
      motivo: 'Incapacidad médica certificada',
      observaciones: '',
    });
    setShowReliefModal(true);
  };

  // Confirmar Relevo de Guardia
  const handleConfirmarRelevo = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTurno) return;
    if (!relevoForm.id_sustituto) {
      alert('Por favor selecciona un colaborador sustituto para cubrir el turno.');
      return;
    }

    const sustitutoObj = personal.find((p) => String(p.id) === String(relevoForm.id_sustituto));
    const nombreSustituto = sustitutoObj?.nombre_completo || 'Colaborador sustituto';

    setIsSubmitting(true);
    try {
      try {
        await apiClient.delete(`/turnos/${selectedTurno.id}`);
      } catch {
        // Ignorar
      }

      await apiClient.post('/turnos', {
        id_usuario: relevoForm.id_sustituto,
        fecha: selectedTurno.fecha,
        hora_inicio: selectedTurno.hora_inicio,
        hora_fin: selectedTurno.hora_fin,
        area: `${selectedTurno.area} [Relevo de ${selectedTurno.empleado}]`,
      });

      notify(
        `✓ Relevo de guardia registrado: ${nombreSustituto} cubrirá a ${selectedTurno.empleado} el ${selectedTurno.fecha} (${relevoForm.motivo}).`
      );
      setShowReliefModal(false);
      setSelectedTurno(null);
      fetchTurnos();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`No se pudo procesar el relevo: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Suspender Turno por Incapacidad sin Reemplazo
  const handleSuspenderPorIncapacidad = async () => {
    if (!selectedTurno) return;
    if (
      !confirm(
        `¿Deseas suspender la guardia de ${selectedTurno.empleado} por ${relevoForm.motivo} sin asignar reemplazo inmediato?`
      )
    ) {
      return;
    }

    setIsSubmitting(true);
    try {
      await apiClient.delete(`/turnos/${selectedTurno.id}`);
      notify(`✓ Guardia de ${selectedTurno.empleado} suspendida por ${relevoForm.motivo}.`);
      setShowReliefModal(false);
      setSelectedTurno(null);
      fetchTurnos();
    } catch (err: unknown) {
      const e = err as ApiError;
      alert(`Error al suspender guardia: ${e.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Turnos de la guardia de hoy (o de la fecha seleccionada)
  const turnosGuardiaHoy = shifts.filter((s) => s.fecha === selectedFechaGuardia);
  const medicosDeGuardiaHoy = turnosGuardiaHoy.filter((s) => s.rol === 'MEDICO');
  const enfermeriaDeGuardiaHoy = turnosGuardiaHoy.filter(
    (s) => s.rol === 'RECEPCIONISTA' || (s.cargo && /enferm|recep|triaje|admisi/i.test(s.cargo))
  );

  // Diagnóstico de Cobertura Hospitalaria (Evaluación de Hoy para Administrador)
  const turnosHoy = shifts.filter((s) => s.fecha === todayStr);

  // 1. Médicos de Guardia Hoy
  const medicosHoy = turnosHoy.filter((s) => s.rol === 'MEDICO');

  // 2. Enfermería & Personal de Apoyo Hoy
  const enfermeriaYApoyoHoy = turnosHoy.filter((s) => s.rol !== 'MEDICO');

  // 3. Cobertura del Servicio de Emergencias
  const medicoEmergencias = turnosHoy.find(
    (s) => s.rol === 'MEDICO' && (/emergenc|urgenc|trauma/i.test(s.area) || /emergenc|urgenc|trauma/i.test(s.cargo))
  );
  const enfermeriaEmergencias = turnosHoy.find(
    (s) => s.rol !== 'MEDICO' && (/emergenc|triaje|urgenc/i.test(s.area) || /enferm|triaje/i.test(s.cargo))
  );
  const emergenciasCubierto = !!medicoEmergencias;

  // 4. Relevos / Incapacidades
  const relevosHoy = turnosHoy.filter((s) => /relevo|incapacidad/i.test(s.area) || /relevo|incapacidad/i.test(s.cargo));

  // Función de presencia en tiempo real para las filas de la tabla
  const isTurnoActivoAhora = (t: TurnoRaw) => {
    if (t.fecha !== todayStr) return false;
    return nowHHMM >= t.hora_inicio && nowHHMM <= t.hora_fin;
  };

  // Semáforo de Cobertura Hospitalaria
  let estadoDiagnostico: 'OPTIMO' | 'ALERTA' | 'CRITICO' = 'OPTIMO';
  let mensajeDiagnostico =
    'Guardia hospitalaria completa: Médicos, Enfermería y Servicio de Emergencias con cobertura activa.';
  let tituloDiagnostico = 'COBERTURA HOSPITALARIA ÓPTIMA';

  if (!emergenciasCubierto && turnosHoy.length > 0) {
    estadoDiagnostico = 'CRITICO';
    tituloDiagnostico = 'ALERTA CRÍTICA: EMERGENCIAS DESPROTEGIDO';
    mensajeDiagnostico =
      'El Servicio de Emergencias no cuenta con médico de guardia asignado para la jornada de hoy. Requiere designación inmediata.';
  } else if (!enfermeriaEmergencias && turnosHoy.length > 0) {
    estadoDiagnostico = 'ALERTA';
    tituloDiagnostico = 'ALERTA: TRIAJE SIN ENFERMERÍA';
    mensajeDiagnostico =
      'El Servicio de Emergencias cuenta con médico pero falta personal de enfermería o triaje asignado hoy.';
  } else if (medicosHoy.length < 2 && turnosHoy.length > 0) {
    estadoDiagnostico = 'ALERTA';
    tituloDiagnostico = 'ATENCIÓN: DÉFICIT DE MÉDICOS';
    mensajeDiagnostico = `Solo hay ${medicosHoy.length} médico programado en el cuadrante de hoy (se sugiere un mínimo de 2 médicos).`;
  } else if (enfermeriaYApoyoHoy.length === 0 && turnosHoy.length > 0) {
    estadoDiagnostico = 'ALERTA';
    tituloDiagnostico = 'ATENCIÓN: SIN PERSONAL DE ENFERMERÍA';
    mensajeDiagnostico = 'No hay personal de enfermería ni apoyo registrado en el cuadrante de hoy.';
  } else if (turnosHoy.length === 0) {
    estadoDiagnostico = 'ALERTA';
    tituloDiagnostico = 'SIN CUADRANTE PROGRAMADO PARA HOY';
    mensajeDiagnostico =
      'No hay turnos registrados para la fecha de hoy en el sistema. Se sugiere coordinar con Operaciones.';
  }

  // Ordenar turnos de la tabla: fecha DESC (hoy y futuros primero), luego hora ASC
  const sortedShifts = [...shifts].sort((a, b) => {
    if (a.fecha !== b.fecha) {
      return b.fecha.localeCompare(a.fecha);
    }
    return a.hora_inicio.localeCompare(b.hora_inicio);
  });

  const filteredShifts = sortedShifts.filter((s) => {
    if (filterRol !== 'TODOS' && s.rol !== filterRol) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return (
        s.empleado.toLowerCase().includes(q) ||
        s.cargo.toLowerCase().includes(q) ||
        s.area.toLowerCase().includes(q) ||
        s.fecha.includes(q)
      );
    }
    return true;
  });

  const handleNavigateDate = (direction: 'prev' | 'next' | 'today') => {
    if (direction === 'today') {
      setAnchorDate(todayStr);
      return;
    }
    const sign = direction === 'next' ? 1 : -1;
    if (viewMode === 'day') {
      setAnchorDate((prev) => addDays(prev, sign));
    } else if (viewMode === 'week') {
      setAnchorDate((prev) => addDays(prev, sign * 7));
    } else if (viewMode === 'month') {
      setAnchorDate((prev) => addMonths(prev, sign));
    }
  };

  const getEligibleStaffForArea = (
    staffList: PersonalOption[],
    area: string,
    specialtyFilter: string | null,
    fecha?: string
  ): PersonalOption[] => {
    const base = staffList.filter(
      (p) =>
        p.rol !== 'PACIENTE' &&
        p.rol !== 'ADMINISTRADOR' &&
        (!fecha || isStaffActiveOnDate(p, fecha))
    );

    if (specialtyFilter) {
      return base.filter(
        (p) => p.rol === 'MEDICO' && normalizeSpecialtyLabel(p.cargo) === specialtyFilter
      );
    }

    if (!area) return base;

    const a = area.toLowerCase();
    if (/emergenc|urgenc|trauma/.test(a)) {
      return base.filter((p) => p.rol === 'MEDICO' && /emergenc|urgenc|trauma/i.test(p.cargo || ''));
    }
    if (/consulta externa/.test(a)) {
      return base.filter((p) => p.rol === 'MEDICO' && !/emergenc|urgenc|trauma/i.test(p.cargo || ''));
    }
    if (/enferm|triaje/.test(a)) {
      return base.filter((p) => p.rol === 'RECEPCIONISTA' && /enferm|triaje/i.test(p.cargo || ''));
    }
    if (/admisi|recepci/.test(a)) {
      return base.filter((p) => p.rol === 'RECEPCIONISTA' && !/enferm|triaje/i.test(p.cargo || ''));
    }
    if (/hospitaliz/.test(a)) {
      return base.filter(
        (p) => p.rol === 'MEDICO' || (p.rol === 'RECEPCIONISTA' && /enferm|triaje/i.test(p.cargo || ''))
      );
    }
    return base;
  };

  const handleQuickCreate = (prefill: ShiftPrefill) => {
    const requestedArea = prefill.area || 'Consulta Externa';
    const targetArea = CORE_AREAS.includes(requestedArea) ? requestedArea : 'Consulta Externa';
    const targetFecha = prefill.fecha || turnoForm.fecha;

    const nextSpecialty = prefill.specialtyFilter || null;
    setModalSpecialtyFilter(nextSpecialty);

    const eligible = getEligibleStaffForArea(personal, targetArea, nextSpecialty, targetFecha);
    const currentValid = eligible.some((p) => String(p.id) === String(turnoForm.id_usuario));
    const defaultUserId = currentValid
      ? turnoForm.id_usuario
      : eligible.length > 0
      ? String(eligible[0].id)
      : '';

    setTurnoForm((prev) => ({
      ...prev,
      id_usuario: defaultUserId,
      fecha: targetFecha,
      area: targetArea,
      hora_inicio: prefill.hora_inicio || '08:00',
      hora_fin: prefill.hora_fin || '16:00',
    }));
    setShowModal(true);
  };

  const handleClearRange = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!clearRange.from || !clearRange.to || clearRange.from > clearRange.to) {
      alert('Selecciona un rango de fechas válido (Desde <= Hasta).');
      return;
    }

    const targets = shifts.filter((s) => s.fecha >= clearRange.from && s.fecha <= clearRange.to);
    if (targets.length === 0) {
      notify('No hay turnos en ese rango de fechas.');
      setShowClearRangeModal(false);
      return;
    }

    if (!confirm(`¿Eliminar ${targets.length} turno(s) entre ${clearRange.from} y ${clearRange.to}?`)) {
      return;
    }

    setIsClearingRange(true);
    let deleted = 0;
    try {
      for (let i = 0; i < targets.length; i += 10) {
        const batch = targets.slice(i, i + 10);
        const results = await Promise.allSettled(
          batch.map((t) => apiClient.delete(`/turnos/${t.id}`))
        );
        deleted += results.filter((r) => r.status === 'fulfilled').length;
      }
      await fetchTurnos();
      setShowClearRangeModal(false);
      notify(`✓ Se eliminaron ${deleted} de ${targets.length} turno(s) (${clearRange.from} al ${clearRange.to}).`);
    } finally {
      setIsClearingRange(false);
    }
  };

  const handleConfirmSaveSuggestions = async (selected: SuggestedShift[]) => {
    if (selected.length === 0) return;
    setIsSubmitting(true);
    let created = 0;
    const errors: string[] = [];

    for (const s of selected) {
      try {
        await apiClient.post('/turnos', {
          id_usuario: s.id_usuario,
          fecha: s.fecha,
          hora_inicio: s.hora_inicio,
          hora_fin: s.hora_fin,
          area: s.area,
        });
        created++;
      } catch (err: unknown) {
        const e = err as ApiError;
        const details = e.errors ? Object.values(e.errors).flat().join(' ') : '';
        errors.push(`${s.empleado} (${s.fecha}): ${details || e.message}`);
      }
    }

    setIsSubmitting(false);
    setShowSuggestModal(false);
    await fetchTurnos();

    if (errors.length > 0) {
      alert(`Se guardaron ${created} turnos, pero ${errors.length} fallaron:\n${errors.join('\n')}`);
    } else {
      notify(`✓ Cobertura aplicada: se crearon ${created} turno(s) en la base de datos.`);
    }
  };

  const visibleRangeDates = useMemo(() => {
    if (viewMode === 'day') return [anchorDate];
    if (viewMode === 'week') return getWeekDays(anchorDate);
    if (viewMode === 'month') {
      return getMonthGridDays(anchorDate)
        .filter((d) => d.inMonth)
        .map((d) => d.date);
    }
    return [];
  }, [viewMode, anchorDate]);

  const visibleShiftsInRange = useMemo(() => {
    if (viewMode === 'list') return filteredShifts;
    const dateSet = new Set(visibleRangeDates);
    return filteredShifts.filter((s) => dateSet.has(s.fecha));
  }, [viewMode, visibleRangeDates, filteredShifts]);

  const emergenciasUncoveredDays = useMemo(() => {
    if (viewMode === 'list') return 0;
    return visibleRangeDates.filter((d) => {
      if (d < todayStr) return false;
      return !filteredShifts.some(
        (s) => s.fecha === d && s.rol === 'MEDICO' && /emergenc|urgenc|trauma/i.test(s.area)
      );
    }).length;
  }, [viewMode, visibleRangeDates, filteredShifts, todayStr]);

  return (
    <div style={{ display: 'grid', gap: '24px' }}>
      {/* Header */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: '10px',
              background: isOps ? '#e0f2fe' : '#ecfdf5',
              color: isOps ? '#0284c7' : '#059669',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <CalendarIcon size={24} />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span
                style={{
                  fontSize: '0.72rem',
                  fontWeight: 800,
                  background: isOps ? '#0284c7' : '#0b5d4b',
                  color: '#ffffff',
                  padding: '2px 8px',
                  borderRadius: '6px',
                  textTransform: 'uppercase',
                  letterSpacing: '0.04em',
                }}
              >
                {isOps ? 'Operación de Guardias' : 'Auditoría Institucional'}
              </span>
            </div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 800, margin: '2px 0 0', color: '#13221c' }}>
              {isOps ? 'Gestión Operativa de Turnos y Relevos' : 'Auditoría de Cuadrantes y Cobertura'}
            </h1>
            <p style={{ margin: 0, fontSize: '0.85rem', color: '#526b60' }}>
              {isOps
                ? 'Supervisión en vivo de guardias activas y gestión de relevos por incapacidad.'
                : 'Diagnóstico en tiempo real de cobertura médica, balance en áreas críticas y supervisión de relevos.'}
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <button
            onClick={() => {
              fetchTurnos();
              notify('Turnos de personal sincronizados con la BD.');
            }}
            disabled={loading}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
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
            <RefreshCwIcon size={14} /> {loading ? 'Cargando...' : 'Sincronizar'}
          </button>
          {isOps && (
            <button
              type="button"
              onClick={() => setShowClearRangeModal(true)}
              title="Eliminar turnos de un rango de fechas (útil para pruebas)"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                padding: '9px 14px',
                borderRadius: '8px',
                border: '1px solid #fecaca',
                background: '#fff5f5',
                color: '#b91c1c',
                fontSize: '0.84rem',
                fontWeight: 700,
                cursor: 'pointer',
                boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
              }}
            >
              🗑 Limpiar
            </button>
          )}
          {isOps ? (
            <button
              onClick={() =>
                handleQuickCreate({
                  fecha: anchorDate,
                  area: 'Consulta Externa',
                  hora_inicio: '08:00',
                  hora_fin: '16:00',
                })
              }
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                padding: '9px 18px',
                borderRadius: '8px',
                border: 'none',
                background: '#0b5d4b',
                color: '#ffffff',
                fontSize: '0.84rem',
                fontWeight: 700,
                cursor: 'pointer',
                boxShadow: '0 4px 12px rgba(11, 93, 75, 0.25)',
              }}
            >
              <PlusCircleIcon size={16} /> Asignar Turno
            </button>
          ) : (
            <span
              style={{
                fontSize: '0.78rem',
                color: '#073f33',
                fontWeight: 700,
                background: '#ecfdf5',
                padding: '8px 14px',
                borderRadius: '8px',
                border: '1px solid #a7f3d0',
              }}
            >
              Programación operada por Jefatura de Operaciones
            </span>
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
          }}
        >
          ✓ {feedback}
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

      {/* =========================================================================
          SECCIÓN OPERATIVA: PERSONAL DE GUARDIA HOY (JEFE DE OPERACIONES)
          ========================================================================= */}
      {isOps ? (
      <div
        style={{
          background: 'linear-gradient(180deg, #ffffff 0%, #f8faf9 100%)',
          borderRadius: '16px',
          padding: isGuardiaOpen ? '22px 24px 24px' : '16px 24px',
          border: '1px solid #d1dcd6',
          boxShadow: '0 4px 14px rgba(11, 93, 75, 0.06)',
          transition: 'all 0.2s ease',
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '12px',
            marginBottom: isGuardiaOpen ? '20px' : '0',
            borderBottom: isGuardiaOpen ? '1px solid #e2ece7' : 'none',
            paddingBottom: isGuardiaOpen ? '14px' : '0',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
              <span
                style={{
                  width: '10px',
                  height: '10px',
                  borderRadius: '50%',
                  background: '#10b981',
                  boxShadow: '0 0 0 3px rgba(16, 185, 129, 0.2)',
                }}
              />
              <h2 style={{ fontSize: '1.2rem', fontWeight: 800, color: '#073f33', margin: 0 }}>
                Equipo en Guardia Activa (Jornada Hoy)
              </h2>
              <div style={{ display: 'flex', gap: '6px' }}>
                <span
                  style={{
                    background: '#e0f2fe',
                    color: '#0369a1',
                    fontSize: '0.74rem',
                    fontWeight: 800,
                    padding: '2px 8px',
                    borderRadius: '999px',
                  }}
                >
                  🩺 {medicosDeGuardiaHoy.length} Médicos
                </span>
                <span
                  style={{
                    background: '#dcfce7',
                    color: '#15803d',
                    fontSize: '0.74rem',
                    fontWeight: 800,
                    padding: '2px 8px',
                    borderRadius: '999px',
                  }}
                >
                  💉 {enfermeriaDeGuardiaHoy.length} Enfermería / Recepción
                </span>
              </div>
            </div>
            {isGuardiaOpen && (
              <p style={{ margin: '4px 0 0', fontSize: '0.82rem', color: '#526b60' }}>
                Visualización en vivo de médicos y personal de enfermería en turno. Pulsa <strong>Relevo / Incapacidad</strong> si algún colaborador debe ser sustituido de emergencia.
              </p>
            )}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
            {isGuardiaOpen && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '0.78rem', fontWeight: 700, color: '#526b60' }}>Jornada:</span>
                <input
                  type="date"
                  value={selectedFechaGuardia}
                  onChange={(e) => setSelectedFechaGuardia(e.target.value)}
                  style={{
                    padding: '6px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.84rem',
                    fontWeight: 700,
                    color: '#073f33',
                    background: '#ffffff',
                  }}
                />
                {selectedFechaGuardia !== todayStr && (
                  <button
                    onClick={() => setSelectedFechaGuardia(todayStr)}
                    style={{
                      padding: '6px 10px',
                      borderRadius: '8px',
                      border: '1px solid #a7f3d0',
                      background: '#ecfdf5',
                      color: '#047857',
                      fontSize: '0.76rem',
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    Volver a Hoy
                  </button>
                )}
              </div>
            )}

            <button
              type="button"
              onClick={() => setIsGuardiaOpen(!isGuardiaOpen)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                padding: '6px 12px',
                borderRadius: '8px',
                border: '1px solid #cad8d1',
                background: isGuardiaOpen ? '#ffffff' : '#ecfdf5',
                color: isGuardiaOpen ? '#334155' : '#047857',
                fontSize: '0.78rem',
                fontWeight: 700,
                cursor: 'pointer',
                boxShadow: '0 1px 2px rgba(0,0,0,0.03)',
                transition: 'all 0.15s ease',
              }}
            >
              <span>{isGuardiaOpen ? 'Ocultar Guardia' : 'Desplegar Guardia'}</span>
              <svg
                width={14}
                height={14}
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                style={{
                  transition: 'transform 0.2s ease',
                  transform: isGuardiaOpen ? 'rotate(180deg)' : 'rotate(0deg)',
                }}
              >
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>
          </div>
        </div>

        {/* Columnas Divididas: Médicos vs. Enfermería/Recepción */}
        {isGuardiaOpen && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '20px' }}>
          {/* COLUMNA 1: MÉDICOS DE GUARDIA */}
          <div
            style={{
              background: '#ffffff',
              borderRadius: '14px',
              padding: '18px',
              border: '1px solid #bae6fd',
              boxShadow: '0 2px 8px rgba(2, 132, 199, 0.05)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '1.3rem' }}>🩺</span>
                <div>
                  <h3 style={{ margin: 0, fontSize: '0.98rem', fontWeight: 800, color: '#0369a1' }}>
                    Médicos de Guardia
                  </h3>
                  <span style={{ fontSize: '0.74rem', color: '#64748b' }}>
                    {medicosDeGuardiaHoy.length} médico(s) asignado(s)
                  </span>
                </div>
              </div>
              <span
                style={{
                  background: '#e0f2fe',
                  color: '#0369a1',
                  fontWeight: 800,
                  fontSize: '0.74rem',
                  padding: '3px 10px',
                  borderRadius: '999px',
                }}
              >
                {medicosDeGuardiaHoy.length} en servicio
              </span>
            </div>

            {medicosDeGuardiaHoy.length === 0 ? (
              <div style={{ padding: '24px', textAlign: 'center', background: '#f8faf9', borderRadius: '10px', color: '#849a90', fontSize: '0.82rem' }}>
                No hay médicos programados para esta jornada.
              </div>
            ) : (
              <div style={{ display: 'grid', gap: '10px' }}>
                {medicosDeGuardiaHoy.map((m) => (
                  <div
                    key={m.id}
                    style={{
                      padding: '12px 14px',
                      borderRadius: '10px',
                      border: '1px solid #e2ece7',
                      background: '#f8faf9',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      gap: '10px',
                    }}
                  >
                    <div>
                      <strong style={{ display: 'block', fontSize: '0.88rem', color: '#13221c' }}>
                        {m.empleado}
                      </strong>
                      <span style={{ fontSize: '0.75rem', color: '#0b5d4b', fontWeight: 600 }}>
                        {m.cargo} · 📍 {m.area}
                      </span>
                      <div style={{ fontSize: '0.73rem', color: '#64748b', marginTop: '2px' }}>
                        🕒 Horario: <strong>{m.hora_inicio} — {m.hora_fin}</strong>
                      </div>
                    </div>
                    <button
                      onClick={() => handleOpenRelief(m)}
                      style={{
                        background: '#0284c7',
                        color: '#ffffff',
                        border: 'none',
                        borderRadius: '6px',
                        padding: '6px 12px',
                        fontSize: '0.74rem',
                        fontWeight: 700,
                        cursor: 'pointer',
                        whiteSpace: 'nowrap',
                        boxShadow: '0 2px 6px rgba(2, 132, 199, 0.25)',
                      }}
                    >
                      Relevo / Incapacidad
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* COLUMNA 2: ENFERMERÍA Y PERSONAL OPERATIVO DE GUARDIA */}
          <div
            style={{
              background: '#ffffff',
              borderRadius: '14px',
              padding: '18px',
              border: '1px solid #bbf7d0',
              boxShadow: '0 2px 8px rgba(16, 185, 129, 0.05)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '1.3rem' }}>💉</span>
                <div>
                  <h3 style={{ margin: 0, fontSize: '0.98rem', fontWeight: 800, color: '#15803d' }}>
                    Enfermería & Recepción de Guardia
                  </h3>
                  <span style={{ fontSize: '0.74rem', color: '#64748b' }}>
                    {enfermeriaDeGuardiaHoy.length} personal operativo asignado
                  </span>
                </div>
              </div>
              <span
                style={{
                  background: '#dcfce7',
                  color: '#15803d',
                  fontWeight: 800,
                  fontSize: '0.74rem',
                  padding: '3px 10px',
                  borderRadius: '999px',
                }}
              >
                {enfermeriaDeGuardiaHoy.length} en servicio
              </span>
            </div>

            {enfermeriaDeGuardiaHoy.length === 0 ? (
              <div style={{ padding: '24px', textAlign: 'center', background: '#f8faf9', borderRadius: '10px', color: '#849a90', fontSize: '0.82rem' }}>
                No hay personal de enfermería registrado para esta jornada.
              </div>
            ) : (
              <div style={{ display: 'grid', gap: '10px' }}>
                {enfermeriaDeGuardiaHoy.map((e) => (
                  <div
                    key={e.id}
                    style={{
                      padding: '12px 14px',
                      borderRadius: '10px',
                      border: '1px solid #e2ece7',
                      background: '#f8faf9',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      gap: '10px',
                    }}
                  >
                    <div>
                      <strong style={{ display: 'block', fontSize: '0.88rem', color: '#13221c' }}>
                        {e.empleado}
                      </strong>
                      <span style={{ fontSize: '0.75rem', color: '#0b5d4b', fontWeight: 600 }}>
                        {e.cargo} · 📍 {e.area}
                      </span>
                      <div style={{ fontSize: '0.73rem', color: '#64748b', marginTop: '2px' }}>
                        🕒 Horario: <strong>{e.hora_inicio} — {e.hora_fin}</strong>
                      </div>
                    </div>
                    <button
                      onClick={() => handleOpenRelief(e)}
                      style={{
                        background: '#0b5d4b',
                        color: '#ffffff',
                        border: 'none',
                        borderRadius: '6px',
                        padding: '6px 12px',
                        fontSize: '0.74rem',
                        fontWeight: 700,
                        cursor: 'pointer',
                        whiteSpace: 'nowrap',
                        boxShadow: '0 2px 6px rgba(11, 93, 75, 0.25)',
                      }}
                    >
                      Relevo / Incapacidad
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
      </div>
      ) : (
        /* =========================================================================
            TABLERO EJECUTIVO DE AUDITORÍA Y DIAGNÓSTICO EN 5 SEGUNDOS (ADMIN GENERAL)
            ========================================================================= */
        <div style={{ display: 'grid', gap: '16px' }}>
          {/* SEMÁFORO DE COBERTURA (BANNER PRINCIPAL) */}
          <div
            style={{
              borderRadius: '14px',
              padding: '16px 20px',
              background:
                estadoDiagnostico === 'CRITICO'
                  ? '#fef2f2'
                  : estadoDiagnostico === 'ALERTA'
                  ? '#fffbeb'
                  : '#ecfdf5',
              border:
                estadoDiagnostico === 'CRITICO'
                  ? '1.5px solid #fecaca'
                  : estadoDiagnostico === 'ALERTA'
                  ? '1.5px solid #fde68a'
                  : '1.5px solid #a7f3d0',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: '12px',
              boxShadow: '0 2px 6px rgba(0,0,0,0.02)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
              <span style={{ fontSize: '1.8rem' }}>
                {estadoDiagnostico === 'CRITICO' ? '🚨' : estadoDiagnostico === 'ALERTA' ? '⚠️' : '🛡️'}
              </span>
              <div>
                <div
                  style={{
                    fontSize: '0.92rem',
                    fontWeight: 800,
                    letterSpacing: '0.02em',
                    color:
                      estadoDiagnostico === 'CRITICO'
                        ? '#991b1b'
                        : estadoDiagnostico === 'ALERTA'
                        ? '#92400e'
                        : '#065f46',
                  }}
                >
                  {tituloDiagnostico}
                </div>
                <div
                  style={{
                    fontSize: '0.82rem',
                    color:
                      estadoDiagnostico === 'CRITICO'
                        ? '#b91c1c'
                        : estadoDiagnostico === 'ALERTA'
                        ? '#b45309'
                        : '#047857',
                    marginTop: '2px',
                  }}
                >
                  {mensajeDiagnostico}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span
                style={{
                  padding: '4px 10px',
                  borderRadius: '999px',
                  fontSize: '0.74rem',
                  fontWeight: 800,
                  background: '#ffffff',
                  color:
                    estadoDiagnostico === 'CRITICO'
                      ? '#dc2626'
                      : estadoDiagnostico === 'ALERTA'
                      ? '#d97706'
                      : '#059669',
                  border: '1px solid currentColor',
                }}
              >
                Jornada de Hoy ({todayStr})
              </span>
            </div>
          </div>

          {/* 4 TARJETAS DE DIAGNÓSTICO RÁPIDO */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
              gap: '14px',
            }}
          >
            {/* Card 1: Médicos de Guardia */}
            <div
              style={{
                background: '#ffffff',
                padding: '18px',
                borderRadius: '12px',
                border: '1px solid #bae6fd',
                boxShadow: '0 2px 4px rgba(0,0,0,0.02)',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.72rem', fontWeight: 800, color: '#0369a1', textTransform: 'uppercase' }}>
                  Médicos de Guardia
                </span>
                <span style={{ fontSize: '1.2rem' }}>🩺</span>
              </div>
              <div
                style={{
                  fontSize: '1.45rem',
                  fontWeight: 800,
                  color: medicosHoy.length > 0 ? '#0369a1' : '#dc2626',
                  marginTop: '6px',
                }}
              >
                {medicosHoy.length} Médicos Asignados
              </div>
              <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#13221c', marginTop: '6px' }}>
                🩺 {medicosHoy.map((m) => m.empleado).join(', ') || 'Sin médicos en cuadrante'}
              </div>
              <div style={{ fontSize: '0.74rem', color: '#059669', fontWeight: 600, marginTop: '2px' }}>
                ✓ {medicosHoy.length >= 2 ? 'Cobertura médica completa' : 'Requiere refuerzo'}
              </div>
              <div style={{ fontSize: '0.69rem', color: '#849a90', marginTop: '6px', borderTop: '1px solid #f1f5f3', paddingTop: '4px' }}>
                Consultorios de consulta y emergencias
              </div>
            </div>

            {/* Card 2: Enfermería & Recepción */}
            <div
              style={{
                background: '#ffffff',
                padding: '18px',
                borderRadius: '12px',
                border: '1px solid #bbf7d0',
                boxShadow: '0 2px 4px rgba(0,0,0,0.02)',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.72rem', fontWeight: 800, color: '#15803d', textTransform: 'uppercase' }}>
                  Enfermería & Recepción
                </span>
                <span style={{ fontSize: '1.2rem' }}>💉</span>
              </div>
              <div
                style={{
                  fontSize: '1.45rem',
                  fontWeight: 800,
                  color: enfermeriaYApoyoHoy.length > 0 ? '#15803d' : '#dc2626',
                  marginTop: '6px',
                }}
              >
                {enfermeriaYApoyoHoy.length} Personal Operativo
              </div>
              <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#13221c', marginTop: '6px' }}>
                💉 {enfermeriaYApoyoHoy.map((e) => `${e.empleado} (${e.cargo || 'Apoyo'})`).join(', ') || 'Sin personal'}
              </div>
              <div style={{ fontSize: '0.74rem', color: '#059669', fontWeight: 600, marginTop: '2px' }}>
                ✓ Triaje y Admisión cubiertos
              </div>
              <div style={{ fontSize: '0.69rem', color: '#849a90', marginTop: '6px', borderTop: '1px solid #f1f5f3', paddingTop: '4px' }}>
                Cuidados de enfermería y ventanilla
              </div>
            </div>

            {/* Card 3: Servicio de Emergencias */}
            <div
              style={{
                background: '#ffffff',
                padding: '18px',
                borderRadius: '12px',
                border: `1.5px solid ${emergenciasCubierto ? '#bbf7d0' : '#fecaca'}`,
                boxShadow: '0 2px 4px rgba(0,0,0,0.02)',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span
                  style={{
                    fontSize: '0.72rem',
                    fontWeight: 800,
                    color: emergenciasCubierto ? '#15803d' : '#b91c1c',
                    textTransform: 'uppercase',
                  }}
                >
                  Servicio de Emergencias
                </span>
                <span style={{ fontSize: '1.2rem' }}>🚨</span>
              </div>
              <div
                style={{
                  fontSize: '1.45rem',
                  fontWeight: 800,
                  color: emergenciasCubierto ? '#15803d' : '#dc2626',
                  marginTop: '6px',
                }}
              >
                {emergenciasCubierto ? 'CUBIERTO ✓' : 'DESPROTEGIDO ⚠️'}
              </div>
              <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#13221c', marginTop: '6px' }}>
                👨‍⚕️ Médico: {medicoEmergencias ? medicoEmergencias.empleado : 'Sin asignar'}
              </div>
              <div style={{ fontSize: '0.74rem', color: '#526b60', marginTop: '2px' }}>
                💉 Triaje: {enfermeriaEmergencias ? enfermeriaEmergencias.empleado : 'Sin enfermería asignada'}
              </div>
              <div style={{ fontSize: '0.69rem', color: '#849a90', marginTop: '6px', borderTop: '1px solid #f1f5f3', paddingTop: '4px' }}>
                Guardia prioritaria de urgencias 24/7
              </div>
            </div>

            {/* Card 4: Relevos / Incapacidades */}
            <div
              style={{
                background: '#ffffff',
                padding: '18px',
                borderRadius: '12px',
                border: '1px solid #e2ece7',
                boxShadow: '0 2px 4px rgba(0,0,0,0.02)',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.72rem', fontWeight: 800, color: '#526b60', textTransform: 'uppercase' }}>
                  Relevos / Incapacidades
                </span>
                <span style={{ fontSize: '1.2rem' }}>🔄</span>
              </div>
              <div
                style={{
                  fontSize: '1.45rem',
                  fontWeight: 800,
                  color: relevosHoy.length > 0 ? '#d97706' : '#059669',
                  marginTop: '6px',
                }}
              >
                {relevosHoy.length > 0 ? `${relevosHoy.length} Relevo(s)` : '0 Incidencias'}
              </div>
              <div
                style={{
                  fontSize: '0.76rem',
                  fontWeight: 600,
                  color: relevosHoy.length > 0 ? '#d97706' : '#059669',
                  marginTop: '6px',
                }}
              >
                {relevosHoy.length > 0
                  ? '⚠️ Reemplazos activos hoy'
                  : '✓ Plantilla sin sustituciones hoy'}
              </div>
              <div style={{ fontSize: '0.74rem', color: '#526b60', marginTop: '2px' }}>
                {relevosHoy.length > 0
                  ? relevosHoy.map((r) => r.empleado).join(', ')
                  : 'Todas las áreas según cuadrante base'}
              </div>
              <div style={{ fontSize: '0.69rem', color: '#849a90', marginTop: '6px', borderTop: '1px solid #f1f5f3', paddingTop: '4px' }}>
                Control de ausencias e incidencias
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Monitor de Cobertura por Especialidad y Guardia Mínima 24/7 */}
      <SpecialtyCoverageMonitor
        anchorDate={anchorDate}
        todayStr={todayStr}
        shifts={shifts}
        personal={personal}
        isOps={isOps}
        onQuickCreate={handleQuickCreate}
        onOpenSuggestModal={() => setShowSuggestModal(true)}
      />

      {/* Toolbar Unificado: Día / Semana / Mes / Lista + Filtros + Distribución */}
      <ShiftToolbar
        viewMode={viewMode}
        onViewModeChange={setViewMode}
        anchorDate={anchorDate}
        todayStr={todayStr}
        onNavigate={handleNavigateDate}
        weekGroupBy={weekGroupBy}
        onWeekGroupByChange={setWeekGroupBy}
        filterRol={filterRol}
        onFilterRolChange={setFilterRol}
        searchQuery={searchQuery}
        onSearchQueryChange={setSearchQuery}
        visibleShifts={visibleShiftsInRange}
        emergenciasUncoveredDays={emergenciasUncoveredDays}
      />

      {/* Vista Activa del Cuadrante */}
      {viewMode === 'day' && (
        <DayTimelineView
          date={anchorDate}
          todayStr={todayStr}
          nowHHMM={nowHHMM}
          shifts={filteredShifts}
          isOps={isOps}
          onSelectShift={setDrawerTurno}
          onQuickCreate={handleQuickCreate}
        />
      )}

      {viewMode === 'week' && (
        <WeekGridView
          anchorDate={anchorDate}
          todayStr={todayStr}
          nowHHMM={nowHHMM}
          shifts={filteredShifts}
          personal={personal}
          groupBy={weekGroupBy}
          isOps={isOps}
          onSelectShift={setDrawerTurno}
          onSelectDay={(d) => {
            setAnchorDate(d);
            setViewMode('day');
          }}
          onQuickCreate={handleQuickCreate}
        />
      )}

      {viewMode === 'month' && (
        <MonthCalendarView
          anchorDate={anchorDate}
          todayStr={todayStr}
          shifts={filteredShifts}
          isOps={isOps}
          onSelectShift={setDrawerTurno}
          onSelectDay={(d) => {
            setAnchorDate(d);
            setViewMode('day');
          }}
          onQuickCreate={handleQuickCreate}
        />
      )}

      {viewMode === 'list' && (
        <div
          style={{
            background: '#ffffff',
            borderRadius: '16px',
            border: '1px solid #e2ece7',
            overflowX: 'auto',
            boxShadow: '0 2px 6px rgba(0,0,0,0.03)',
          }}
        >
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
          <thead>
            <tr style={{ background: '#f8faf9', borderBottom: '1px solid #e2ece7', color: '#526b60' }}>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Fecha / Horario</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Personal Asignado</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Rol / Cargo</th>
              <th style={{ padding: '14px 16px', fontWeight: 800 }}>Área de Asignación</th>
              <th style={{ padding: '14px 16px', fontWeight: 800, textAlign: 'right' }}>
                {isOps ? 'Gestión Operativa' : 'Estado / Auditoría'}
              </th>
            </tr>
          </thead>
          <tbody>
            {filteredShifts.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ padding: '32px', textAlign: 'center', color: '#849a90' }}>
                  {loading ? 'Cargando turnos...' : 'No hay turnos registrados en este período.'}
                </td>
              </tr>
            ) : (
              filteredShifts.map((s) => {
                const esHoy = s.fecha === selectedFechaGuardia;
                return (
                  <tr
                    key={s.id}
                    style={{
                      borderBottom: '1px solid #f0f4f2',
                      background: esHoy ? '#f0fdf4' : 'transparent',
                    }}
                  >
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <div style={{ fontWeight: 800, color: '#13221c' }}>{s.fecha}</div>
                        {esHoy && (
                          <span
                            style={{
                              background: '#dcfce7',
                              color: '#15803d',
                              fontSize: '0.68rem',
                              fontWeight: 800,
                              padding: '1px 6px',
                              borderRadius: '4px',
                            }}
                          >
                            HOY
                          </span>
                        )}
                      </div>
                      <div style={{ fontSize: '0.78rem', color: '#0b5d4b', fontWeight: 700 }}>
                        🕒 {s.hora_inicio} — {s.hora_fin}
                      </div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <div style={{ fontWeight: 700, color: '#13221c' }}>{s.empleado}</div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {(() => {
                        const badge = getRoleBadgeConfig(s.rol);
                        return (
                          <span
                            style={{
                              padding: '3px 8px',
                              borderRadius: '6px',
                              fontSize: '0.72rem',
                              fontWeight: 700,
                              background: badge.bg,
                              color: badge.color,
                              border: badge.border,
                            }}
                          >
                            {s.rol}
                          </span>
                        );
                      })()}
                      <div style={{ fontSize: '0.75rem', color: '#526b60', marginTop: '2px' }}>{s.cargo}</div>
                    </td>
                    <td style={{ padding: '14px 16px', color: '#13221c', fontWeight: 600 }}>
                      {s.area}
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      {isOps ? (
                        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                          <button
                            onClick={() => handleOpenRelief(s)}
                            title="Gestionar relevo de guardia o reportar incapacidad del colaborador"
                            style={{
                              padding: '6px 12px',
                              borderRadius: '6px',
                              border: '1px solid #bae6fd',
                              background: '#f0f9ff',
                              color: '#0369a1',
                              fontSize: '0.75rem',
                              fontWeight: 700,
                              cursor: 'pointer',
                            }}
                          >
                            Incapacidad / Relevo
                          </button>
                          <button
                            onClick={() => handleEliminarTurno(s)}
                            style={{
                              padding: '6px 10px',
                              borderRadius: '6px',
                              border: '1px solid #fecaca',
                              background: '#fff5f5',
                              color: '#b91c1c',
                              fontSize: '0.75rem',
                              fontWeight: 700,
                              cursor: 'pointer',
                            }}
                          >
                            Eliminar
                          </button>
                        </div>
                      ) : (
                        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                          {isTurnoActivoAhora(s) ? (
                            <span
                              style={{
                                padding: '4px 10px',
                                borderRadius: '999px',
                                fontSize: '0.74rem',
                                fontWeight: 800,
                                background: '#dcfce7',
                                color: '#15803d',
                              }}
                            >
                              ● En Turno Activo
                            </span>
                          ) : /relevo|incapacidad/i.test(s.area) ? (
                            <span
                              style={{
                                padding: '4px 10px',
                                borderRadius: '999px',
                                fontSize: '0.74rem',
                                fontWeight: 800,
                                background: '#fef3c7',
                                color: '#b45309',
                              }}
                            >
                              🔄 Relevo de Guardia
                            </span>
                          ) : s.fecha < todayStr ? (
                            <span
                              style={{
                                padding: '4px 10px',
                                borderRadius: '999px',
                                fontSize: '0.74rem',
                                fontWeight: 700,
                                background: '#f1f5f9',
                                color: '#64748b',
                              }}
                            >
                              ✓ Concluido
                            </span>
                          ) : (
                            <span
                              style={{
                                padding: '4px 10px',
                                borderRadius: '999px',
                                fontSize: '0.74rem',
                                fontWeight: 700,
                                background: '#e0f2fe',
                                color: '#0369a1',
                              }}
                            >
                              🕒 Programado
                            </span>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
      )}

      {/* Drawer Lateral de Detalle del Turno */}
      <ShiftDetailDrawer
        turno={drawerTurno}
        allShifts={shifts}
        todayStr={todayStr}
        nowHHMM={nowHHMM}
        isOps={isOps}
        onClose={() => setDrawerTurno(null)}
        onOpenRelief={handleOpenRelief}
        onDelete={handleEliminarTurno}
      />

      {/* Modal Algoritmo de Cobertura Semanal 24/7 */}
      <SuggestCoverageModal
        isOpen={showSuggestModal}
        anchorDate={anchorDate}
        shifts={shifts}
        personal={personal}
        isSubmitting={isSubmitting}
        onClose={() => setShowSuggestModal(false)}
        onConfirmSave={handleConfirmSaveSuggestions}
      />

      {/* MODAL RELEVO DE GUARDIA / INCAPACIDAD */}
      {showReliefModal && selectedTurno && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 60,
            padding: '16px',
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '16px',
              maxWidth: '520px',
              width: '100%',
              padding: '26px',
              boxShadow: '0 20px 30px rgba(0, 0, 0, 0.25)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <div>
                <span style={{ fontSize: '0.72rem', fontWeight: 800, color: '#0369a1', textTransform: 'uppercase' }}>
                  Gestión Operativa de Relevos
                </span>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '2px 0 0', color: '#13221c' }}>
                  Incapacidad / Relevo de Guardia
                </h3>
              </div>
              <button
                onClick={() => setShowReliefModal(false)}
                style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer', color: '#849a90' }}
              >
                ✕
              </button>
            </div>

            {/* Ficha del Turno Afectado */}
            <div style={{ background: '#f8faf9', padding: '14px', borderRadius: '10px', border: '1px solid #e2ece7', marginBottom: '16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                <strong style={{ fontSize: '0.9rem', color: '#13221c' }}>{selectedTurno.empleado}</strong>
                <span style={{ fontSize: '0.76rem', fontWeight: 700, color: '#0b5d4b' }}>{selectedTurno.rol}</span>
              </div>
              <p style={{ margin: 0, fontSize: '0.78rem', color: '#526b60' }}>
                📅 {selectedTurno.fecha} · 🕒 {selectedTurno.hora_inicio} — {selectedTurno.hora_fin}
              </p>
              <p style={{ margin: '4px 0 0', fontSize: '0.78rem', color: '#33483f' }}>
                📍 Área: <strong>{selectedTurno.area}</strong>
              </p>
            </div>

            <form onSubmit={handleConfirmarRelevo} style={{ display: 'grid', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Motivo de la Incapacidad / Ausencia *
                </label>
                <select
                  value={relevoForm.motivo}
                  onChange={(e) => setRelevoForm({ ...relevoForm, motivo: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                    background: '#ffffff',
                  }}
                >
                  <option value="Incapacidad médica certificada">Incapacidad médica certificada (enfermedad)</option>
                  <option value="Permiso de urgencia personal">Permiso de urgencia personal o familiar</option>
                  <option value="Suspensión temporal operativa">Suspensión temporal u orden directiva</option>
                  <option value="Reasignación de servicio hospitalario">Reasignación por refuerzo de área</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Colaborador Sustituto (Reemplazo de Guardia) *
                </label>
                <select
                  value={relevoForm.id_sustituto}
                  onChange={(e) => setRelevoForm({ ...relevoForm, id_sustituto: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                    background: '#ffffff',
                  }}
                  required
                >
                  <option value="">-- Seleccionar Reemplazo Disponible ({selectedTurno.rol}) --</option>
                  {personal
                    .filter((p) => String(p.id) !== String(selectedTurno.id_usuario) && p.rol === selectedTurno.rol)
                    .map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.nombre_completo} — {p.cargo || p.rol}
                      </option>
                    ))}
                </select>
                <span style={{ fontSize: '0.72rem', color: '#526b60', marginTop: '3px', display: 'block' }}>
                  Filtrado automáticamente por colaboradores con perfil compatible de {selectedTurno.rol}.
                </span>
              </div>

              <div style={{ display: 'flex', gap: '8px', marginTop: '14px', flexWrap: 'wrap' }}>
                <button
                  type="button"
                  onClick={handleSuspenderPorIncapacidad}
                  disabled={isSubmitting}
                  style={{
                    padding: '10px 14px',
                    borderRadius: '8px',
                    border: '1px solid #fecaca',
                    background: '#fef2f2',
                    color: '#991b1b',
                    fontWeight: 700,
                    fontSize: '0.8rem',
                    cursor: 'pointer',
                  }}
                >
                  Suspender sin Sustituto
                </button>
                <button
                  type="button"
                  onClick={() => setShowReliefModal(false)}
                  style={{
                    padding: '10px 16px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1',
                    background: '#ffffff',
                    fontWeight: 600,
                    fontSize: '0.82rem',
                    cursor: 'pointer',
                    marginLeft: 'auto',
                  }}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{
                    padding: '10px 20px',
                    borderRadius: '8px',
                    border: 'none',
                    background: '#0284c7',
                    color: '#ffffff',
                    fontWeight: 700,
                    fontSize: '0.82rem',
                    cursor: 'pointer',
                  }}
                >
                  {isSubmitting ? 'Procesando...' : 'Asignar Relevo'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL ASIGNAR TURNO NUEVO */}
      {showModal && (
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
              Asignar Turno de Guardia
            </h3>
            <p style={{ margin: '0 0 16px', fontSize: '0.82rem', color: '#526b60' }}>
              Define la fecha, horario y servicio de atención para el empleado.
            </p>

            <form onSubmit={handleCrearTurno} style={{ display: 'grid', gap: '14px' }}>
              {/* 1. Primero seleccionar el Área / Servicio */}
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  1. Área / Servicio de Atención
                </label>
                <select
                  value={turnoForm.area}
                  onChange={(e) => {
                    const val = e.target.value;
                    setModalSpecialtyFilter(null);
                    const eligible = getEligibleStaffForArea(personal, val, null, turnoForm.fecha);
                    const keepCurrent = eligible.some((p) => String(p.id) === String(turnoForm.id_usuario));
                    setTurnoForm({
                      ...turnoForm,
                      area: val,
                      id_usuario: keepCurrent ? turnoForm.id_usuario : eligible[0]?.id ? String(eligible[0].id) : '',
                    });
                  }}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                    background: '#ffffff',
                  }}
                  required
                >
                  {CORE_AREAS.map((areaName) => (
                    <option key={areaName} value={areaName}>
                      {areaName}
                    </option>
                  ))}
                </select>
              </div>

              {/* 2. Personal elegible según el área y fecha seleccionadas (sin administradores ni contratos vencidos) */}
              {(() => {
                const eligibleStaff = getEligibleStaffForArea(
                  personal,
                  turnoForm.area,
                  modalSpecialtyFilter,
                  turnoForm.fecha
                );
                return (
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '4px' }}>
                      <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c' }}>
                        2. Personal Asignado ({eligibleStaff.length} vigentes el {turnoForm.fecha})
                      </label>
                      {modalSpecialtyFilter && (
                        <button
                          type="button"
                          onClick={() => {
                            setModalSpecialtyFilter(null);
                            const eligible = getEligibleStaffForArea(personal, turnoForm.area, null, turnoForm.fecha);
                            setTurnoForm((prev) => ({
                              ...prev,
                              id_usuario: eligible[0]?.id ? String(eligible[0].id) : '',
                            }));
                          }}
                          style={{
                            border: 'none',
                            background: '#e0f2fe',
                            color: '#0369a1',
                            fontSize: '0.7rem',
                            fontWeight: 700,
                            padding: '2px 8px',
                            borderRadius: '999px',
                            cursor: 'pointer',
                          }}
                        >
                          Filtro: {modalSpecialtyFilter} ✕
                        </button>
                      )}
                    </div>
                    <select
                      value={turnoForm.id_usuario}
                      onChange={(e) => setTurnoForm({ ...turnoForm, id_usuario: e.target.value })}
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        borderRadius: '8px',
                        border: '1px solid #cad8d1',
                        fontSize: '0.85rem',
                      }}
                      required
                    >
                      <option value="">-- Seleccionar Personal Elegible --</option>
                      {eligibleStaff.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.nombre_completo} ({p.cargo || p.rol})
                          {p.fecha_salida ? ` · Rotación hasta ${String(p.fecha_salida).slice(0, 10)}` : ' · Planta Indefinido'}
                        </option>
                      ))}
                    </select>
                  </div>
                );
              })()}

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                  Fecha de la Guardia
                </label>
                <input
                  type="date"
                  value={turnoForm.fecha}
                  onChange={(e) => {
                    const nextFecha = e.target.value;
                    const eligible = getEligibleStaffForArea(
                      personal,
                      turnoForm.area,
                      modalSpecialtyFilter,
                      nextFecha
                    );
                    const stillValid = eligible.some((p) => String(p.id) === String(turnoForm.id_usuario));
                    setTurnoForm({
                      ...turnoForm,
                      fecha: nextFecha,
                      id_usuario: stillValid
                        ? turnoForm.id_usuario
                        : eligible[0]?.id
                        ? String(eligible[0].id)
                        : '',
                    });
                  }}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid #cad8d1',
                    fontSize: '0.85rem',
                    boxSizing: 'border-box',
                  }}
                  required
                />
              </div>

              {/* Atajos Turnos 8h (jornada desde 08:00) */}
              <div>
                <div style={{ display: 'flex', gap: '6px', marginBottom: '8px' }}>
                  <button
                    type="button"
                    onClick={() => setTurnoForm({ ...turnoForm, hora_inicio: '00:00', hora_fin: '08:00' })}
                    style={{
                      flex: 1,
                      padding: '6px 8px',
                      borderRadius: '7px',
                      border: turnoForm.hora_inicio === '00:00' && turnoForm.hora_fin === '08:00' ? '1px solid #6d28d9' : '1px solid #cad8d1',
                      background: turnoForm.hora_inicio === '00:00' && turnoForm.hora_fin === '08:00' ? '#ede9fe' : '#f8faf9',
                      color: '#5b21b6',
                      fontSize: '0.72rem',
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    🌙 Noche (00–08)
                  </button>
                  <button
                    type="button"
                    onClick={() => setTurnoForm({ ...turnoForm, hora_inicio: '08:00', hora_fin: '16:00' })}
                    style={{
                      flex: 1,
                      padding: '6px 8px',
                      borderRadius: '7px',
                      border: turnoForm.hora_inicio === '08:00' && turnoForm.hora_fin === '16:00' ? '1px solid #0b5d4b' : '1px solid #cad8d1',
                      background: turnoForm.hora_inicio === '08:00' && turnoForm.hora_fin === '16:00' ? '#eaf5f1' : '#f8faf9',
                      color: '#073f33',
                      fontSize: '0.72rem',
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    ☀ Mañana (08–16)
                  </button>
                  <button
                    type="button"
                    onClick={() => setTurnoForm({ ...turnoForm, hora_inicio: '16:00', hora_fin: '23:59' })}
                    style={{
                      flex: 1,
                      padding: '6px 8px',
                      borderRadius: '7px',
                      border: turnoForm.hora_inicio === '16:00' && turnoForm.hora_fin === '23:59' ? '1px solid #b45309' : '1px solid #cad8d1',
                      background: turnoForm.hora_inicio === '16:00' && turnoForm.hora_fin === '23:59' ? '#fef3c7' : '#f8faf9',
                      color: '#92400e',
                      fontSize: '0.72rem',
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    🌤 Tarde (16–24)
                  </button>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <div>
                    <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                      Hora Inicio
                    </label>
                    <input
                      type="time"
                      value={turnoForm.hora_inicio}
                      onChange={(e) => setTurnoForm({ ...turnoForm, hora_inicio: e.target.value })}
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        borderRadius: '8px',
                        border: '1px solid #cad8d1',
                        fontSize: '0.85rem',
                        boxSizing: 'border-box',
                      }}
                      required
                    />
                  </div>
                  <div>
                    <label style={{ fontSize: '0.8rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                      Hora Fin
                    </label>
                    <input
                      type="time"
                      value={turnoForm.hora_fin}
                      onChange={(e) => setTurnoForm({ ...turnoForm, hora_fin: e.target.value })}
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        borderRadius: '8px',
                        border: '1px solid #cad8d1',
                        fontSize: '0.85rem',
                        boxSizing: 'border-box',
                      }}
                      required
                    />
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
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
                  {isSubmitting ? 'Asignando...' : 'Guardar Turno'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL LIMPIAR TURNOS POR RANGO DE FECHAS (DEMO / PRUEBAS) */}
      {showClearRangeModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 55,
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
            <h3 style={{ fontSize: '1.15rem', fontWeight: 800, margin: '0 0 6px', color: '#991b1b' }}>
              🗑 Limpiar Turnos por Rango
            </h3>
            <p style={{ margin: '0 0 14px', fontSize: '0.82rem', color: '#526b60' }}>
              Elimina los turnos guardados entre dos fechas usando la API oficial (`DELETE /turnos/:id`).
            </p>

            {/* Atajos rápidos */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '14px' }}>
              <button
                type="button"
                onClick={() => {
                  const w = getWeekDays(anchorDate);
                  setClearRange({ from: w[0], to: w[6] });
                }}
                style={{
                  padding: '5px 10px',
                  borderRadius: '999px',
                  border: '1px solid #cad8d1',
                  background: '#f8faf9',
                  fontSize: '0.74rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                Semana visible
              </button>
              <button
                type="button"
                onClick={() => {
                  const w = getWeekDays(todayStr);
                  setClearRange({ from: w[0], to: addDays(w[6], 7) });
                }}
                style={{
                  padding: '5px 10px',
                  borderRadius: '999px',
                  border: '1px solid #cad8d1',
                  background: '#f8faf9',
                  fontSize: '0.74rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                Esta y próx. semana
              </button>
              <button
                type="button"
                onClick={() => {
                  setClearRange({ from: todayStr, to: addDays(todayStr, 30) });
                }}
                style={{
                  padding: '5px 10px',
                  borderRadius: '999px',
                  border: '1px solid #cad8d1',
                  background: '#f8faf9',
                  fontSize: '0.74rem',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                Desde hoy (+30d)
              </button>
            </div>

            <form onSubmit={handleClearRange} style={{ display: 'grid', gap: '12px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <div>
                  <label style={{ fontSize: '0.78rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                    Desde
                  </label>
                  <input
                    type="date"
                    value={clearRange.from}
                    onChange={(e) => setClearRange((r) => ({ ...r, from: e.target.value }))}
                    style={{
                      width: '100%',
                      padding: '8px 10px',
                      borderRadius: '8px',
                      border: '1px solid #cad8d1',
                      fontSize: '0.84rem',
                      boxSizing: 'border-box',
                    }}
                    required
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.78rem', fontWeight: 700, color: '#13221c', display: 'block', marginBottom: '4px' }}>
                    Hasta
                  </label>
                  <input
                    type="date"
                    value={clearRange.to}
                    onChange={(e) => setClearRange((r) => ({ ...r, to: e.target.value }))}
                    style={{
                      width: '100%',
                      padding: '8px 10px',
                      borderRadius: '8px',
                      border: '1px solid #cad8d1',
                      fontSize: '0.84rem',
                      boxSizing: 'border-box',
                    }}
                    required
                  />
                </div>
              </div>

              {(() => {
                const count = shifts.filter(
                  (s) => s.fecha >= clearRange.from && s.fecha <= clearRange.to
                ).length;
                return (
                  <div
                    style={{
                      padding: '10px 12px',
                      borderRadius: '8px',
                      background: count > 0 ? '#fff5f5' : '#f8faf9',
                      border: count > 0 ? '1px solid #fecaca' : '1px solid #e2ece7',
                      fontSize: '0.8rem',
                      fontWeight: 700,
                      color: count > 0 ? '#991b1b' : '#526b60',
                    }}
                  >
                    {count > 0
                      ? `Se encontraron ${count} turno(s) en este rango.`
                      : 'No hay turnos registrados en este rango.'}
                  </div>
                );
              })()}

              <div style={{ display: 'flex', gap: '10px', marginTop: '6px' }}>
                <button
                  type="button"
                  onClick={() => setShowClearRangeModal(false)}
                  disabled={isClearingRange}
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
                  disabled={isClearingRange}
                  style={{
                    flex: 1,
                    padding: '10px',
                    borderRadius: '8px',
                    border: 'none',
                    background: '#b91c1c',
                    color: '#ffffff',
                    fontWeight: 700,
                    cursor: 'pointer',
                  }}
                >
                  {isClearingRange ? 'Eliminando...' : 'Eliminar Turnos'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
