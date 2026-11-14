import { PersonalOption, ShiftFranja, ShiftViewMode, StaffCategory, TurnoRaw } from './types';

const MONTHS_ES = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];

const MONTHS_SHORT_ES = [
  'Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
  'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic',
];

const DAYS_SHORT_ES = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
const DAYS_LETTER_ES = ['D', 'L', 'M', 'X', 'J', 'V', 'S'];
const DAYS_FULL_ES = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

export const CORE_AREAS = [
  'Servicio de Emergencias',
  'Consulta Externa',
  'Triaje y Enfermería',
  'Admisión y Recepción',
  'Hospitalización',
];

export const FRANJA_CONFIG: Record<
  ShiftFranja,
  { label: string; range: string; defaultStart: string; defaultEnd: string; badgeBg: string; badgeColor: string }
> = {
  MANANA: {
    label: 'Mañana (8h)',
    range: '08:00 – 16:00',
    defaultStart: '08:00',
    defaultEnd: '16:00',
    badgeBg: '#fef3c7',
    badgeColor: '#b45309',
  },
  TARDE: {
    label: 'Tarde (8h)',
    range: '16:00 – 23:59',
    defaultStart: '16:00',
    defaultEnd: '23:59',
    badgeBg: '#e0f2fe',
    badgeColor: '#0369a1',
  },
  NOCHE: {
    label: 'Noche (8h)',
    range: '00:00 – 08:00',
    defaultStart: '00:00',
    defaultEnd: '08:00',
    badgeBg: '#ede9fe',
    badgeColor: '#6d28d9',
  },
};

export function toISODate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export function parseISODate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, (m || 1) - 1, d || 1);
}

export function addDays(iso: string, delta: number): string {
  const d = parseISODate(iso);
  d.setDate(d.getDate() + delta);
  return toISODate(d);
}

export function addMonths(iso: string, delta: number): string {
  const d = parseISODate(iso);
  d.setMonth(d.getMonth() + delta);
  return toISODate(d);
}

export function startOfWeekMonday(iso: string): string {
  const d = parseISODate(iso);
  const day = d.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  return toISODate(d);
}

export function getWeekDays(anchorIso: string): string[] {
  const mon = startOfWeekMonday(anchorIso);
  return Array.from({ length: 7 }, (_, i) => addDays(mon, i));
}

/**
 * Verifica si el colaborador está activo y dentro de su periodo de rotación/contrato
 * en la fecha ISO (YYYY-MM-DD) indicada.
 */
export function isStaffActiveOnDate(person: PersonalOption, dateIso: string): boolean {
  if (person.estado && String(person.estado).toUpperCase() !== 'ACTIVO') {
    return false;
  }
  const day = dateIso.slice(0, 10);
  if (person.fecha_ingreso) {
    const start = String(person.fecha_ingreso).slice(0, 10);
    if (day < start) return false;
  }
  if (person.fecha_salida) {
    const end = String(person.fecha_salida).slice(0, 10);
    if (day > end) return false;
  }
  return true;
}

export function getMonthGridDays(anchorIso: string): { date: string; inMonth: boolean }[] {
  const d = parseISODate(anchorIso);
  const year = d.getFullYear();
  const month = d.getMonth();
  const firstOfMonth = new Date(year, month, 1);
  const startGrid = startOfWeekMonday(toISODate(firstOfMonth));

  const lastOfMonth = new Date(year, month + 1, 0);
  const endWeekMon = startOfWeekMonday(toISODate(lastOfMonth));
  const endGrid = addDays(endWeekMon, 6);

  const result: { date: string; inMonth: boolean }[] = [];
  let cursor = startGrid;
  while (cursor <= endGrid) {
    const cDate = parseISODate(cursor);
    result.push({
      date: cursor,
      inMonth: cDate.getMonth() === month,
    });
    cursor = addDays(cursor, 1);
  }
  return result;
}

export function formatDayHeader(iso: string): {
  dayName: string;
  dayLetter: string;
  dayNum: number;
  monthShort: string;
} {
  const d = parseISODate(iso);
  return {
    dayName: DAYS_SHORT_ES[d.getDay()],
    dayLetter: DAYS_LETTER_ES[d.getDay()],
    dayNum: d.getDate(),
    monthShort: MONTHS_SHORT_ES[d.getMonth()],
  };
}

export function formatRangeTitle(mode: ShiftViewMode, anchorIso: string): string {
  const d = parseISODate(anchorIso);
  if (mode === 'day') {
    return `${DAYS_FULL_ES[d.getDay()]}, ${d.getDate()} de ${MONTHS_ES[d.getMonth()]} ${d.getFullYear()}`;
  }
  if (mode === 'week') {
    const days = getWeekDays(anchorIso);
    const first = parseISODate(days[0]);
    const last = parseISODate(days[6]);
    if (first.getMonth() === last.getMonth()) {
      return `${first.getDate()} – ${last.getDate()} ${MONTHS_ES[first.getMonth()]} ${first.getFullYear()}`;
    }
    return `${first.getDate()} ${MONTHS_SHORT_ES[first.getMonth()]} – ${last.getDate()} ${MONTHS_SHORT_ES[last.getMonth()]} ${last.getFullYear()}`;
  }
  if (mode === 'month') {
    return `${MONTHS_ES[d.getMonth()]} ${d.getFullYear()}`;
  }
  return 'Todos los Turnos';
}

export function timeToMinutes(hhmm: string): number {
  const [h, m] = hhmm.split(':').map(Number);
  return (h || 0) * 60 + (m || 0);
}

export function isOvernightShift(t: Pick<TurnoRaw, 'hora_inicio' | 'hora_fin'>): boolean {
  return timeToMinutes(t.hora_fin) <= timeToMinutes(t.hora_inicio);
}

export function getShiftDurationHours(t: TurnoRaw): number {
  const start = timeToMinutes(t.hora_inicio);
  let end = timeToMinutes(t.hora_fin);
  if (end <= start) end += 24 * 60;
  return Math.round(((end - start) / 60) * 10) / 10;
}

export function isShiftActiveNow(t: TurnoRaw, todayStr: string, nowHHMM: string): boolean {
  const nowMin = timeToMinutes(nowHHMM);
  const startMin = timeToMinutes(t.hora_inicio);
  const endMin = timeToMinutes(t.hora_fin);

  if (!isOvernightShift(t)) {
    return t.fecha === todayStr && nowMin >= startMin && nowMin <= endMin;
  }
  if (t.fecha === todayStr && nowMin >= startMin) return true;
  if (addDays(t.fecha, 1) === todayStr && nowMin <= endMin) return true;
  return false;
}

export function getShiftFranja(horaInicio: string): ShiftFranja {
  const mins = timeToMinutes(horaInicio);
  if (mins >= 7 * 60 && mins < 15 * 60) return 'MANANA';
  if (mins >= 15 * 60 && mins < 23 * 60) return 'TARDE';
  return 'NOCHE';
}

export function isRelevoShift(t: TurnoRaw): boolean {
  return /relevo|incapacidad/i.test(t.area) || /relevo|incapacidad/i.test(t.cargo);
}

export function getStaffCategory(rol: string, cargo?: string | null): StaffCategory {
  const r = (rol || '').toUpperCase();
  const c = (cargo || '').toLowerCase();
  if (r === 'MEDICO') {
    if (/emergenc|urgenc|trauma/.test(c)) return 'EMERGENCIAS';
    return 'MEDICO_ESPECIALIDAD';
  }
  if (r === 'RECEPCIONISTA') {
    if (/enferm|triaje/.test(c)) return 'ENFERMERIA';
    return 'RECEPCION';
  }
  return 'ADMIN';
}

export function normalizeSpecialtyLabel(cargo?: string | null): string {
  const clean = (cargo || '').trim();
  if (!clean) return 'Medicina General';
  if (/emergenc|urgenc|trauma/i.test(clean)) return 'Emergencias';
  if (/medicina\s*general|general/i.test(clean)) return 'Medicina General';
  if (/alerg/i.test(clean)) return 'Alergología';
  return clean;
}

export function getShiftSpecialtyGroup(t: TurnoRaw): string {
  const cat = getStaffCategory(t.rol, t.cargo);
  if (cat === 'EMERGENCIAS') return 'Emergencias';
  if (cat === 'MEDICO_ESPECIALIDAD') return normalizeSpecialtyLabel(t.cargo);
  if (cat === 'ENFERMERIA') return 'Enfermería';
  if (cat === 'RECEPCION') return 'Recepción';
  return 'Administración';
}

export function getMedicalSpecialties(personal: PersonalOption[], shifts: TurnoRaw[]): string[] {
  const set = new Set<string>(['Emergencias']);
  personal.forEach((p) => {
    if (p.rol === 'MEDICO') {
      set.add(normalizeSpecialtyLabel(p.cargo));
    }
  });
  shifts.forEach((s) => {
    if (s.rol === 'MEDICO') {
      set.add(normalizeSpecialtyLabel(s.cargo));
    }
  });
  return Array.from(set).sort((a, b) => {
    if (a === 'Emergencias') return -1;
    if (b === 'Emergencias') return 1;
    return a.localeCompare(b);
  });
}

export function getSpecialtyGroupRows(personal: PersonalOption[], shifts: TurnoRaw[]): string[] {
  const medSpecs = getMedicalSpecialties(personal, shifts);
  return [...medSpecs, 'Enfermería', 'Recepción'];
}

export function normalizeAreaGroup(area: string): string {
  const clean = area.replace(/\[Relevo.*?\]/gi, '').trim();
  const lower = clean.toLowerCase();
  if (/triaje|enfermer/.test(lower)) return 'Triaje y Enfermería';
  if (/admisi|recepci/.test(lower)) return 'Admisión y Recepción';
  if (/emergenc|urgenc|trauma/.test(lower)) return 'Servicio de Emergencias';
  if (/consulta/.test(lower)) return 'Consulta Externa';
  if (/hospitaliz|cama|piso/.test(lower)) return 'Hospitalización';
  return clean || 'Hospital General';
}

/**
 * Mantiene siempre las áreas clínicas principales y solo agrega áreas extra
 * si existen turnos activos en el período visible (evita filas vacías de prueba).
 */
export function getAreaGroupsForShifts(visibleShifts: TurnoRaw[]): string[] {
  const set = new Set<string>(CORE_AREAS);
  visibleShifts.forEach((s) => {
    set.add(normalizeAreaGroup(s.area));
  });
  return Array.from(set);
}

/**
 * Detecta si un turno cubre alguno de los 3 bloques de 8h (jornada desde las 08:00):
 * - NOCHE:  00:00–08:00
 * - MANANA: 08:00–16:00
 * - TARDE:  16:00–23:59
 */
export function shiftCoversBlock(t: TurnoRaw, date: string, block: ShiftFranja): boolean {
  if (t.fecha !== date) return false;
  const start = timeToMinutes(t.hora_inicio);
  const end = timeToMinutes(t.hora_fin);
  const overnight = end <= start;

  if (block === 'NOCHE') {
    return (start < 8 * 60 && (overnight || end > 0)) || start >= 23 * 60;
  }
  if (block === 'MANANA') {
    return start < 16 * 60 && (overnight || end > 8 * 60);
  }
  return start < 24 * 60 && (overnight || end > 16 * 60);
}

export function getShiftVisualStyle(t: TurnoRaw): {
  bg: string;
  border: string;
  accent: string;
  text: string;
  roleLabel: string;
} {
  if (isRelevoShift(t)) {
    return {
      bg: '#fffbeb',
      border: '#fde68a',
      accent: '#d97706',
      text: '#92400e',
      roleLabel: t.rol === 'MEDICO' ? 'Médico (Relevo)' : 'Apoyo (Relevo)',
    };
  }
  if (t.rol === 'MEDICO') {
    return {
      bg: '#f0f9ff',
      border: '#bae6fd',
      accent: '#0284c7',
      text: '#075985',
      roleLabel: normalizeSpecialtyLabel(t.cargo),
    };
  }
  if (t.rol === 'ADMINISTRADOR') {
    return {
      bg: '#f5f3ff',
      border: '#ddd6fe',
      accent: '#7c3aed',
      text: '#5b21b6',
      roleLabel: 'Admin',
    };
  }
  return {
    bg: '#eaf5f1',
    border: '#a7f3d0',
    accent: '#0b5d4b',
    text: '#073f33',
    roleLabel: getStaffCategory(t.rol, t.cargo) === 'ENFERMERIA' ? 'Enfermería' : 'Recepción',
  };
}
