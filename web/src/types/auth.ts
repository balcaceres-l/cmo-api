export type RolUsuario = 'ADMINISTRADOR' | 'MEDICO' | 'RECEPCIONISTA' | 'PACIENTE';

export interface EspecialidadItem {
  id_especialidad: string;
  nombre: string;
  descripcion?: string | null;
  es_sistema: boolean;
  activo: boolean;
  medicos_count?: number;
}

export interface User {
  id: number | string;
  nombre_completo: string;
  usuario: string;
  email?: string | null;
  rol: RolUsuario;
  cargo?: string | null;
  id_especialidad?: string | null;
  especialidad_nombre?: string | null;
  especialidad?: EspecialidadItem | null;
  telefono?: string | null;
  estado: 'ACTIVO' | 'INACTIVO';
}

export interface LoginCredentials {
  usuario: string;
  password: string;
}

export interface AuthResponseData {
  usuario: User;
  id_paciente?: number | string | null;
  token: string;
  token_type: string;
}

export interface ApiResponse<T = unknown> {
  success: boolean;
  message: string;
  data?: T;
  errors?: Record<string, string[]>;
}
