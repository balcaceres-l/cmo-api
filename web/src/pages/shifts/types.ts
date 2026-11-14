export interface TurnoRaw {
  id: string;
  id_usuario: string;
  empleado: string;
  cargo: string;
  rol: 'MEDICO' | 'RECEPCIONISTA' | 'ADMINISTRADOR';
  area: string;
  fecha: string;
  hora_inicio: string;
  hora_fin: string;
}

export interface PersonalOption {
  id: string;
  nombre_completo: string;
  rol: string;
  cargo: string | null;
  estado?: string;
  fecha_ingreso?: string | null;
  fecha_salida?: string | null;
}

export type ShiftViewMode = 'day' | 'week' | 'month' | 'list';
export type WeekGroupBy = 'area' | 'especialidad' | 'franja';
export type ShiftFranja = 'MANANA' | 'TARDE' | 'NOCHE';

export type StaffCategory =
  | 'EMERGENCIAS'
  | 'MEDICO_ESPECIALIDAD'
  | 'ENFERMERIA'
  | 'RECEPCION'
  | 'ADMIN';

export interface ShiftPrefill {
  fecha?: string;
  area?: string;
  hora_inicio?: string;
  hora_fin?: string;
  specialtyFilter?: string;
}
