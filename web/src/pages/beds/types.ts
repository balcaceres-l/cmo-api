export interface BedPatientInfo {
  id_paciente?: string;
  nombre: string;
  expediente: string;
  dui?: string | null;
  diagnostico?: string;
  medico?: string;
  fechaIngresoIso?: string;
  fechaIngreso?: string;
  registradoPor?: string;
}

export interface BedData {
  id: string;
  codigo: string;
  planta: number;
  tipo: 'VIP' | 'SEMIPRIVADA';
  letra?: 'A' | 'B';
  habitacion: string;
  estado: 'OCUPADA' | 'DISPONIBLE' | 'MANTENIMIENTO';
  id_ingreso?: string;
  paciente?: BedPatientInfo;
}

export interface RoomGroup {
  codigo: string;
  planta: number;
  tipo: 'VIP' | 'SEMIPRIVADA';
  camas: BedData[];
}

export interface IngresoRecord {
  id_ingreso: string;
  id_cama?: string;
  fecha_ingreso: string;
  fecha_alta?: string | null;
  motivo: string;
  paciente?: {
    id_paciente: string;
    nombre_completo: string;
    numero_expediente: string;
    dui?: string | null;
  };
  cama?: {
    id_cama: string;
    codigo: string;
    habitacion?: {
      codigo: string;
      planta: number;
      tipo: 'VIP' | 'SEMIPRIVADA';
    };
  };
  medico?: {
    id: string;
    nombre_completo: string;
  };
  registrado_por?: {
    id: string;
    nombre_completo: string;
    cargo?: string | null;
  } | null;
}

export interface PacienteOption {
  id_paciente: string;
  nombre_completo: string;
  numero_expediente: string;
  dui: string | null;
}

export interface MedicoOption {
  id: string;
  nombre_completo: string;
  cargo: string | null;
  especialidad_nombre?: string | null;
}

export type ModalType = 'INGRESO' | 'ALTA' | 'TRASLADO' | null;

export function getStayHours(isoDate?: string | null): number {
  if (!isoDate) return 0;
  const start = new Date(isoDate).getTime();
  if (Number.isNaN(start)) return 0;
  return Math.max(0, (Date.now() - start) / (1000 * 60 * 60));
}

export function formatStayDuration(startIso?: string | null, endIso?: string | null): string {
  if (!startIso) return '—';
  const start = new Date(startIso).getTime();
  const end = endIso ? new Date(endIso).getTime() : Date.now();
  if (Number.isNaN(start) || Number.isNaN(end)) return '—';

  const totalMinutes = Math.max(0, Math.floor((end - start) / 60000));
  const days = Math.floor(totalMinutes / (60 * 24));
  const hours = Math.floor((totalMinutes % (60 * 24)) / 60);
  const mins = totalMinutes % 60;

  if (days > 0) return `${days}d ${hours}h`;
  if (hours > 0) return `${hours}h ${mins}m`;
  return `${mins}m`;
}
