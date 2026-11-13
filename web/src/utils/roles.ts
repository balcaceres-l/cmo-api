import { User } from '../types/auth';

export const isJefeOperaciones = (user?: User | null): boolean => {
  if (!user) return false;
  const cargo = (user.cargo || '').toLowerCase();
  const usuario = (user.usuario || '').toLowerCase();
  return cargo.includes('operaciones') || usuario === 'operaciones';
};

export const isAdminGeneral = (user?: User | null): boolean => {
  if (!user) return false;
  return user.rol === 'ADMINISTRADOR' && !isJefeOperaciones(user);
};

export interface RoleBadgeConfig {
  bg: string;
  color: string;
  border: string;
  label: string;
}

/**
 * Estándar institucional de colores por rol (Figma Hospital CMO):
 * - MÉDICO: Azul cielo / petróleo (#e0f2fe / #0369a1)
 * - ENFERMERÍA / RECEPCIONISTA: Verde esmeralda (#dcfce7 / #15803d)
 * - ADMINISTRADOR: Ámbar / Dorado directivo (#fef3c7 / #92400e)
 * - OTROS / PACIENTE: Slate suave (#f1f5f9 / #475569)
 */
export const getRoleBadgeConfig = (rol?: string | null): RoleBadgeConfig => {
  const r = (rol || '').toUpperCase();
  if (r === 'MEDICO') {
    return {
      bg: '#e0f2fe',
      color: '#0369a1',
      border: '1px solid #bae6fd',
      label: 'MÉDICO',
    };
  }
  if (r === 'RECEPCIONISTA' || r === 'ENFERMERO' || r === 'ENFERMERIA') {
    return {
      bg: '#dcfce7',
      color: '#15803d',
      border: '1px solid #bbf7d0',
      label: 'RECEPCIÓN / ENFERMERÍA',
    };
  }
  if (r === 'ADMINISTRADOR') {
    return {
      bg: '#fef3c7',
      color: '#92400e',
      border: '1px solid #fde68a',
      label: 'ADMINISTRADOR',
    };
  }
  return {
    bg: '#f1f5f9',
    color: '#475569',
    border: '1px solid #cbd5e1',
    label: r || 'USUARIO',
  };
};

