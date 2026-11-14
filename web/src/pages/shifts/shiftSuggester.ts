import { PersonalOption, ShiftFranja, TurnoRaw } from './types';
import {
  FRANJA_CONFIG,
  addDays,
  formatDayHeader,
  getMedicalSpecialties,
  getStaffCategory,
  getWeekDays,
  isStaffActiveOnDate,
  normalizeAreaGroup,
  normalizeSpecialtyLabel,
  parseISODate,
  shiftCoversBlock,
  startOfWeekMonday,
  timeToMinutes,
  toISODate,
} from './shiftUtils';

export interface SuggestedShift {
  tempId: string;
  selected: boolean;
  id_usuario: string;
  empleado: string;
  cargo: string;
  rol: string;
  fecha: string;
  hora_inicio: string;
  hora_fin: string;
  area: string;
  block: ShiftFranja;
  reason: string;
  isBackup: boolean;
}

export interface UnfilledGap {
  fecha: string;
  block: ShiftFranja;
  area: string;
  roleNeeded: string;
  reason: string;
}

export interface SuggesterOptions {
  maxWeeklyHours: number; // ej. 40h (5 turnos de 8h) o 48h (6 turnos)
  minRestHours: number; // ej. 16h mínimo de descanso entre turnos
  maxNightsPerWeek: number; // ej. 2 o 3 noches máx. por semana
  minSpecialtyDays: number; // ej. 1 día/semana mínimo por especialidad
  allowGeneralBackupForEmergencies: boolean;
}

export interface SuggesterResult {
  proposals: SuggestedShift[];
  unfilledGaps: UnfilledGap[];
}

interface VirtualShift {
  id_usuario: string;
  fecha: string;
  hora_inicio: string;
  hora_fin: string;
  area: string;
  cargo: string;
  rol: string;
}

function toEpochMinutes(isoDate: string, hhmm: string): number {
  const [y, m, d] = isoDate.split('-').map(Number);
  const mins = timeToMinutes(hhmm);
  return Math.floor(Date.UTC(y, (m || 1) - 1, d || 1) / 60000) + mins;
}

function getShiftInterval(fecha: string, horaInicio: string, horaFin: string): [number, number] {
  const start = toEpochMinutes(fecha, horaInicio);
  let end = toEpochMinutes(fecha, horaFin);
  if (end <= start) {
    end += 24 * 60;
  }
  return [start, end];
}

function isSplitContinuation(s: Pick<VirtualShift, 'hora_inicio' | 'hora_fin'>): boolean {
  return timeToMinutes(s.hora_inicio) === 0 && timeToMinutes(s.hora_fin) < 8 * 60;
}

function getRealDurationHours(s: Pick<VirtualShift, 'hora_inicio' | 'hora_fin'>): number {
  const start = timeToMinutes(s.hora_inicio);
  let end = timeToMinutes(s.hora_fin);
  if (end <= start) end += 24 * 60;
  // Si es la primera parte de un split Laravel (ej. 23:00–23:59), su parte 2 (00:00–07:00) suma el resto
  return (end - start) / 60;
}

/**
 * Garantiza:
 * 1) 0 solapes horarios
 * 2) Mínimo `minRestHours` (16 h) de descanso continuo entre el fin de un turno y el inicio del siguiente
 *    (lo que también impide >8 h por bloque de 24 h y prohíbe rotaciones inversas Tarde→Mañana o Noche→Tarde).
 */
function respectsRestAndNoOverlap(
  userId: string,
  fecha: string,
  horaInicio: string,
  horaFin: string,
  allShifts: VirtualShift[],
  minRestHours: number
): boolean {
  const [candStart, candEnd] = getShiftInterval(fecha, horaInicio, horaFin);
  const restMin = minRestHours * 60;

  for (const ex of allShifts) {
    if (String(ex.id_usuario) !== String(userId)) continue;
    const [exStart, exEnd] = getShiftInterval(ex.fecha, ex.hora_inicio, ex.hora_fin);

    const separatedAfterEx = candStart >= exEnd + restMin;
    const separatedBeforeEx = exStart >= candEnd + restMin;
    if (!separatedAfterEx && !separatedBeforeEx) {
      return false;
    }
  }
  return true;
}

function getWeeklyHours(userId: string, weekDaysSet: Set<string>, allShifts: VirtualShift[]): number {
  let total = 0;
  for (const s of allShifts) {
    if (String(s.id_usuario) !== String(userId)) continue;
    if (!weekDaysSet.has(s.fecha)) continue;
    total += getRealDurationHours(s);
  }
  return Math.round(total * 10) / 10;
}

function getWeeklyNightsCount(userId: string, weekDaysSet: Set<string>, allShifts: VirtualShift[]): number {
  return allShifts.filter(
    (s) =>
      String(s.id_usuario) === String(userId) &&
      weekDaysSet.has(s.fecha) &&
      timeToMinutes(s.hora_inicio) >= 22 * 60
  ).length;
}

function getOccupiedWeekDaysCount(
  userId: string,
  weekDays: string[],
  allShifts: VirtualShift[],
  candidateFecha?: string
): number {
  const occupied = new Set<string>();
  for (const s of allShifts) {
    if (String(s.id_usuario) !== String(userId)) continue;
    if (!isSplitContinuation(s) && weekDays.includes(s.fecha)) {
      occupied.add(s.fecha);
    }
  }
  if (candidateFecha) occupied.add(candidateFecha);
  return occupied.size;
}

function hasShiftStartingOnDate(userId: string, fecha: string, allShifts: VirtualShift[]): boolean {
  return allShifts.some(
    (s) => String(s.id_usuario) === String(userId) && s.fecha === fecha && !isSplitContinuation(s)
  );
}

function canAssign(
  person: PersonalOption,
  fecha: string,
  block: ShiftFranja,
  weekDays: string[],
  weekDaysSet: Set<string>,
  allShifts: VirtualShift[],
  options: SuggesterOptions
): boolean {
  // 0. Vigencia contractual / rotación universitaria en la fecha exacta del turno
  if (!isStaffActiveOnDate(person, fecha)) {
    return false;
  }

  const cfg = FRANJA_CONFIG[block];
  const horaInicio = cfg.defaultStart;
  const horaFin = cfg.defaultEnd;

  // 1. Tope de horas semanales (ej. 40 h/semana)
  if (getWeeklyHours(person.id, weekDaysSet, allShifts) + 8 > options.maxWeeklyHours) {
    return false;
  }

  // 2. Tope de noches por semana (ej. máx 2 o 3 noches)
  if (block === 'NOCHE' && getWeeklyNightsCount(person.id, weekDaysSet, allShifts) >= options.maxNightsPerWeek) {
    return false;
  }

  // 3. Al menos 1 día libre completo a la semana (máx 6 días con turno de 7)
  if (getOccupiedWeekDaysCount(person.id, weekDays, allShifts, fecha) > 6) {
    return false;
  }

  // 4. Descanso mínimo de 16 horas entre turnos y 0 solapes (y máx. 1 turno por día)
  if (!respectsRestAndNoOverlap(person.id, fecha, horaInicio, horaFin, allShifts, options.minRestHours)) {
    return false;
  }

  return true;
}

function formatDayShortLabel(fecha: string): string {
  const { dayName, dayNum, monthShort } = formatDayHeader(fecha);
  return `${dayName} ${dayNum} ${monthShort}`;
}

export function suggestWeeklyCoverage(
  weekDays: string[],
  existingShifts: TurnoRaw[],
  personal: PersonalOption[],
  options: SuggesterOptions,
  allowedDays?: Set<string>
): SuggesterResult {
  const weekDaysSet = new Set(weekDays);
  const proposals: SuggestedShift[] = [];
  const unfilledGaps: UnfilledGap[] = [];

  const allShifts: VirtualShift[] = existingShifts.map((s) => ({
    id_usuario: String(s.id_usuario),
    fecha: s.fecha,
    hora_inicio: s.hora_inicio,
    hora_fin: s.hora_fin,
    area: s.area,
    cargo: s.cargo,
    rol: s.rol,
  }));

  // Garantía dentro del algoritmo: nunca pacientes ni personal desactivado
  const activeStaff = personal.filter(
    (p) =>
      String(p.rol || '').toUpperCase() !== 'PACIENTE' &&
      (!p.estado || String(p.estado).toUpperCase() === 'ACTIVO')
  );

  const emergencyDocs = activeStaff.filter((p) => getStaffCategory(p.rol, p.cargo) === 'EMERGENCIAS');
  const generalDocs = activeStaff.filter(
    (p) =>
      getStaffCategory(p.rol, p.cargo) === 'MEDICO_ESPECIALIDAD' &&
      normalizeSpecialtyLabel(p.cargo) === 'Medicina General'
  );
  const otherDocs = activeStaff.filter((p) => getStaffCategory(p.rol, p.cargo) === 'MEDICO_ESPECIALIDAD');
  const nurses = activeStaff.filter((p) => getStaffCategory(p.rol, p.cargo) === 'ENFERMERIA');
  const receptionists = activeStaff.filter((p) => getStaffCategory(p.rol, p.cargo) === 'RECEPCION');

  const pickBestCandidate = (
    candidates: PersonalOption[],
    fecha: string,
    block: ShiftFranja
  ): PersonalOption | null => {
    const eligible = candidates.filter((c) =>
      canAssign(c, fecha, block, weekDays, weekDaysSet, allShifts, options)
    );
    if (eligible.length === 0) return null;

    eligible.sort((a, b) => {
      const hoursA = getWeeklyHours(a.id, weekDaysSet, allShifts);
      const hoursB = getWeeklyHours(b.id, weekDaysSet, allShifts);
      if (hoursA !== hoursB) return hoursA - hoursB;

      if (block === 'NOCHE') {
        const nightsA = getWeeklyNightsCount(a.id, weekDaysSet, allShifts);
        const nightsB = getWeeklyNightsCount(b.id, weekDaysSet, allShifts);
        if (nightsA !== nightsB) return nightsA - nightsB;
      }

      const prevA = hasShiftStartingOnDate(a.id, addDays(fecha, -1), allShifts) ? 1 : 0;
      const prevB = hasShiftStartingOnDate(b.id, addDays(fecha, -1), allShifts) ? 1 : 0;
      return prevA - prevB;
    });

    return eligible[0];
  };

  const addProposal = (
    person: PersonalOption,
    fecha: string,
    block: ShiftFranja,
    area: string,
    reason: string,
    isBackup = false
  ) => {
    const cfg = FRANJA_CONFIG[block];
    const hora_inicio = cfg.defaultStart;
    const hora_fin = cfg.defaultEnd;

    proposals.push({
      tempId: `prop-${proposals.length + 1}-${person.id}-${fecha}-${block}`,
      selected: true,
      id_usuario: String(person.id),
      empleado: person.nombre_completo,
      cargo: person.cargo || person.rol,
      rol: person.rol,
      fecha,
      hora_inicio,
      hora_fin,
      area,
      block,
      reason,
      isBackup,
    });
    allShifts.push({
      id_usuario: String(person.id),
      fecha,
      hora_inicio,
      hora_fin,
      area,
      cargo: person.cargo || person.rol,
      rol: person.rol,
    });
  };

  const isCovered = (fecha: string, block: ShiftFranja, matcher: (s: VirtualShift) => boolean): boolean => {
    return allShifts.some((s) => {
      const dummy: TurnoRaw = {
        id: '',
        id_usuario: s.id_usuario,
        empleado: '',
        cargo: s.cargo,
        rol: s.rol as TurnoRaw['rol'],
        area: s.area,
        fecha: s.fecha,
        hora_inicio: s.hora_inicio,
        hora_fin: s.hora_fin,
      };
      if (!shiftCoversBlock(dummy, fecha, block)) return false;
      return matcher(s);
    });
  };

  const now = new Date();
  const todayIso = toISODate(now);

  // Solo planificar a partir de las 00:00 de mañana y dentro de allowedDays si aplica
  const isAssignableDay = (fecha: string): boolean => {
    if (fecha <= todayIso) return false;
    return !allowedDays || allowedDays.has(fecha);
  };

  // =========================================================================
  // FASE 1: GUARDIA MÍNIMA 24/7 EN 3 TURNOS DE 8 HORAS (00-08, 08-16, 16-24)
  // =========================================================================
  const blocks: { id: ShiftFranja; label: string }[] = [
    { id: 'NOCHE', label: 'Noche (00–08)' },
    { id: 'MANANA', label: 'Mañana (08–16)' },
    { id: 'TARDE', label: 'Tarde (16–24)' },
  ];

  for (const fecha of weekDays) {
    for (const block of blocks) {
      if (!isAssignableDay(fecha)) continue;
      // 1A. Médico de Emergencias 24/7
      const hasEmergMed = isCovered(
        fecha,
        block.id,
        (s) => s.rol === 'MEDICO' && /emergenc|urgenc|trauma/i.test(s.area)
      );
      if (!hasEmergMed) {
        const primary = pickBestCandidate(emergencyDocs, fecha, block.id);
        if (primary) {
          addProposal(primary, fecha, block.id, 'Servicio de Emergencias', `Emergencias 24/7 · ${block.label}`);
        } else if (options.allowGeneralBackupForEmergencies) {
          const backupPool = generalDocs.length > 0 ? generalDocs : otherDocs;
          const backup = pickBestCandidate(backupPool, fecha, block.id);
          if (backup) {
            addProposal(
              backup,
              fecha,
              block.id,
              'Servicio de Emergencias',
              `Refuerzo Emergencias · ${block.label}`,
              true
            );
          } else {
            unfilledGaps.push({
              fecha,
              block: block.id,
              area: 'Servicio de Emergencias',
              roleNeeded: 'Médico de Emergencias',
              reason: `Sin médico disponible (${formatDayShortLabel(fecha)} ${block.label}) respetando 16h de descanso y ${options.maxWeeklyHours}h/sem.`,
            });
          }
        } else {
          unfilledGaps.push({
            fecha,
            block: block.id,
            area: 'Servicio de Emergencias',
            roleNeeded: 'Médico de Emergencias',
            reason: `Sin médico de Emergencias disponible (${formatDayShortLabel(fecha)} ${block.label}).`,
          });
        }
      }

      // 1B. Enfermera de Triaje y Enfermería 24/7
      const hasNurse = isCovered(
        fecha,
        block.id,
        (s) => getStaffCategory(s.rol, s.cargo) === 'ENFERMERIA'
      );
      if (!hasNurse) {
        const nurse = pickBestCandidate(nurses, fecha, block.id);
        if (nurse) {
          addProposal(nurse, fecha, block.id, 'Triaje y Enfermería', `Mínimo Enfermería 24/7 · ${block.label}`);
        } else {
          unfilledGaps.push({
            fecha,
            block: block.id,
            area: 'Triaje y Enfermería',
            roleNeeded: 'Enfermera',
            reason: `Sin enfermera disponible (${formatDayShortLabel(fecha)} ${block.label}) respetando 16h de descanso y ${options.maxWeeklyHours}h/sem.`,
          });
        }
      }

      // 1C. Recepcionista de Admisión 24/7
      const hasRecep = isCovered(
        fecha,
        block.id,
        (s) => getStaffCategory(s.rol, s.cargo) === 'RECEPCION'
      );
      if (!hasRecep) {
        const recep = pickBestCandidate(receptionists, fecha, block.id);
        if (recep) {
          addProposal(recep, fecha, block.id, 'Admisión y Recepción', `Mínimo Recepción 24/7 · ${block.label}`);
        } else {
          unfilledGaps.push({
            fecha,
            block: block.id,
            area: 'Admisión y Recepción',
            roleNeeded: 'Recepcionista',
            reason: `Sin recepcionista disponible (${formatDayShortLabel(fecha)} ${block.label}) respetando 16h de descanso y ${options.maxWeeklyHours}h/sem.`,
          });
        }
      }
    }
  }

  // =========================================================================
  // FASE 2: ESPECIALIDADES MÉDICAS EN HORARIO REGULAR DIURNO (08:00–16:00)
  // Etapa 2A: Garantizar presencia mínima por especialidad distribuida en la semana
  // =========================================================================
  const allMedSpecialties = getMedicalSpecialties(activeStaff, existingShifts).filter(
    (spec) => spec !== 'Emergencias'
  );

  const assignableDaysCount = weekDays.filter(isAssignableDay).length;
  const weekSpanLabel = `${formatDayShortLabel(weekDays[0])}–${formatDayShortLabel(weekDays[weekDays.length - 1])}`;

  allMedSpecialties.forEach((specialty, specIndex) => {
    if (assignableDaysCount === 0) return;
    const specDoctors = otherDocs.filter((d) => normalizeSpecialtyLabel(d.cargo) === specialty);
    if (specDoctors.length === 0) return;

    let currentCount = weekDays.filter((day) =>
      isCovered(
        day,
        'MANANA',
        (s) => s.rol === 'MEDICO' && normalizeSpecialtyLabel(s.cargo) === specialty
      )
    ).length;

    const targetSpecDays = Math.min(options.minSpecialtyDays, assignableDaysCount);
    while (currentCount < targetSpecDays) {
      let bestChoice: { doctor: PersonalOption; day: string; score: number } | null = null;
      const offset = (specIndex * 2 + currentCount) % weekDays.length;
      const rotatedDays = weekDays.slice(offset).concat(weekDays.slice(0, offset));

      for (const day of rotatedDays) {
        if (!isAssignableDay(day)) continue;
        const alreadyThatDay = isCovered(
          day,
          'MANANA',
          (s) => s.rol === 'MEDICO' && normalizeSpecialtyLabel(s.cargo) === specialty
        );
        if (alreadyThatDay) continue;

        const dayConsultaLoad = allShifts.filter(
          (s) =>
            s.fecha === day &&
            s.rol === 'MEDICO' &&
            normalizeAreaGroup(s.area) === 'Consulta Externa' &&
            !isSplitContinuation(s)
        ).length;

        for (const doc of specDoctors) {
          if (!canAssign(doc, day, 'MANANA', weekDays, weekDaysSet, allShifts, options)) {
            continue;
          }
          const docHours = getWeeklyHours(doc.id, weekDaysSet, allShifts);
          const workedPrev = hasShiftStartingOnDate(doc.id, addDays(day, -1), allShifts);

          const score =
            (20 - dayConsultaLoad * 4) +
            ((options.maxWeeklyHours - docHours) / 8) * 2 +
            (workedPrev ? 0 : 2);
          if (!bestChoice || score > bestChoice.score) {
            bestChoice = { doctor: doc, day, score };
          }
        }
      }

      if (!bestChoice) {
        unfilledGaps.push({
          fecha: weekDays.find(isAssignableDay) || weekDays[0],
          block: 'MANANA',
          area: 'Consulta Externa',
          roleNeeded: `Médico (${specialty})`,
          reason: `Semana ${weekSpanLabel}: sin personal vigente/disponible de ${specialty} respetando descansos y ${options.maxWeeklyHours}h/sem.`,
        });
        break;
      }

      addProposal(
        bestChoice.doctor,
        bestChoice.day,
        'MANANA',
        'Consulta Externa',
        `Cobertura Especialidad: ${specialty} (08–16)`
      );
      currentCount++;
    }
  });

  // =========================================================================
  // Etapa 2B: Completar jornada semanal de cada médico de especialidad (hasta maxWeeklyHours)
  // repartiendo los turnos equitativamente de Lunes a Domingo en horario 08:00–16:00.
  // =========================================================================
  const docIndexById = new Map<string, number>();
  otherDocs.forEach((d, idx) => docIndexById.set(String(d.id), idx));

  let round = 0;
  const maxIterations = Math.max(1, otherDocs.length * 7);
  while (round < maxIterations) {
    round++;

    const candidates = otherDocs
      .map((doc) => {
        const hours = getWeeklyHours(doc.id, weekDaysSet, allShifts);
        if (hours + 8 > options.maxWeeklyHours) return null;

        const docIdx = docIndexById.get(String(doc.id)) ?? 0;
        const offset = (docIdx + round) % weekDays.length;
        const rotatedDays = weekDays.slice(offset).concat(weekDays.slice(0, offset));
        const specialty = normalizeSpecialtyLabel(doc.cargo);

        let bestDay: { day: string; score: number } | null = null;
        for (const day of rotatedDays) {
          if (!isAssignableDay(day)) continue;
          if (!canAssign(doc, day, 'MANANA', weekDays, weekDaysSet, allShifts, options)) continue;

          const dayConsultaLoad = allShifts.filter(
            (s) =>
              s.fecha === day &&
              s.rol === 'MEDICO' &&
              normalizeAreaGroup(s.area) === 'Consulta Externa' &&
              !isSplitContinuation(s)
          ).length;

          const sameSpecThatDay = isCovered(
            day,
            'MANANA',
            (s) => s.rol === 'MEDICO' && normalizeSpecialtyLabel(s.cargo) === specialty
          );

          const workedPrev = hasShiftStartingOnDate(doc.id, addDays(day, -1), allShifts);

          const score =
            (40 - dayConsultaLoad * 6) +
            (sameSpecThatDay ? 0 : 10) +
            (workedPrev ? 0 : 2);

          if (!bestDay || score > bestDay.score) {
            bestDay = { day, score };
          }
        }

        if (!bestDay) return null;
        return { doc, hours, bestDay, docIdx };
      })
      .filter((item): item is NonNullable<typeof item> => item !== null);

    if (candidates.length === 0) break;

    candidates.sort((a, b) => {
      if (a.hours !== b.hours) return a.hours - b.hours;
      return a.docIdx - b.docIdx;
    });

    const chosen = candidates[0];
    const specialty = normalizeSpecialtyLabel(chosen.doc.cargo);
    addProposal(
      chosen.doc,
      chosen.bestDay.day,
      'MANANA',
      'Consulta Externa',
      `Jornada Especialidad: ${specialty} (08–16)`
    );
  }

  const blockOrder: Record<ShiftFranja, number> = { MANANA: 0, TARDE: 1, NOCHE: 2 };
  proposals.sort((a, b) => {
    if (a.fecha !== b.fecha) return a.fecha.localeCompare(b.fecha);
    if (a.block !== b.block) return blockOrder[a.block] - blockOrder[b.block];
    return a.area.localeCompare(b.area);
  });

  return { proposals, unfilledGaps };
}

export const MAX_RANGE_DAYS = 366;

export function countRangeDays(startDate: string, endDate: string): number {
  if (!startDate || !endDate || startDate > endDate) return 0;
  const startMs = parseISODate(startDate).getTime();
  const endMs = parseISODate(endDate).getTime();
  return Math.round((endMs - startMs) / 86400000) + 1;
}

/**
 * Planifica cobertura para cualquier rango de fechas (semana, mes o personalizado)
 * iterando por semanas calendario para respetar topes semanales y descansos continuos
 * entre el domingo y el lunes siguiente, filtrando vigencia exacta en cada fecha.
 */
export function suggestCoverageForRange(
  startDate: string,
  endDate: string,
  existingShifts: TurnoRaw[],
  personal: PersonalOption[],
  options: SuggesterOptions
): SuggesterResult {
  const totalDays = countRangeDays(startDate, endDate);
  if (totalDays <= 0 || totalDays > MAX_RANGE_DAYS) {
    return { proposals: [], unfilledGaps: [] };
  }

  const allowedDays = new Set<string>();
  let cursor = startDate;
  while (cursor <= endDate) {
    allowedDays.add(cursor);
    cursor = addDays(cursor, 1);
  }

  const accumulatedShifts: TurnoRaw[] = [...existingShifts];
  const allProposals: SuggestedShift[] = [];
  const allGaps: UnfilledGap[] = [];

  let weekMonday = startOfWeekMonday(startDate);
  const lastWeekMonday = startOfWeekMonday(endDate);

  while (weekMonday <= lastWeekMonday) {
    const weekDays = getWeekDays(weekMonday);
    const weekResult = suggestWeeklyCoverage(
      weekDays,
      accumulatedShifts,
      personal,
      options,
      allowedDays
    );

    weekResult.proposals.forEach((p) => {
      const idx = allProposals.length + 1;
      allProposals.push({ ...p, tempId: `prop-${idx}-${p.id_usuario}-${p.fecha}-${p.block}` });
      accumulatedShifts.push({
        id: p.tempId,
        id_usuario: p.id_usuario,
        empleado: p.empleado,
        cargo: p.cargo,
        rol: p.rol as TurnoRaw['rol'],
        area: p.area,
        fecha: p.fecha,
        hora_inicio: p.hora_inicio,
        hora_fin: p.hora_fin,
      });
    });

    allGaps.push(...weekResult.unfilledGaps);
    weekMonday = addDays(weekMonday, 7);
  }

  const blockOrder: Record<ShiftFranja, number> = { MANANA: 0, TARDE: 1, NOCHE: 2 };
  allProposals.sort((a, b) => {
    if (a.fecha !== b.fecha) return a.fecha.localeCompare(b.fecha);
    if (a.block !== b.block) return blockOrder[a.block] - blockOrder[b.block];
    return a.area.localeCompare(b.area);
  });

  return { proposals: allProposals, unfilledGaps: allGaps };
}
