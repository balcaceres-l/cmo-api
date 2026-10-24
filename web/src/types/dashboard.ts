export interface OcupacionItem {
  planta: number;
  estado: string; // 'DISPONIBLE' | 'OCUPADA' | 'MANTENIMIENTO'
  total: number;
}

export interface CitaEstadoItem {
  estado: string; // 'SOLICITADA' | 'AGENDADA' | 'EN_ESPERA' | 'EN_ATENCION' | 'ATENDIDA' | 'CANCELADA'
  total: number;
}

export interface LaboratorioItem {
  estado: string; // 'PENDIENTE' | 'EN_PROCESO' | 'COMPLETADO'
  total: number;
}

export interface AmbulanciaMetricas {
  estado: 'DISPONIBLE' | 'EN_SERVICIO' | 'EN_MANTENIMIENTO' | string;
  servicios_mes: number;
}

export interface DashboardMetricasData {
  ocupacion: OcupacionItem[];
  citas_hoy: CitaEstadoItem[];
  laboratorio: LaboratorioItem[];
  recursos_bajo_minimo: number;
  ambulancia: AmbulanciaMetricas;
  pacientes: number;
  personal_activo: number;
}
