import { ApiResponse } from '../types/auth';

declare global {
  interface Window {

    __ENV__?: {
      VITE_API_URL?: string;
      VITE_REVERB_HOST?: string;
      VITE_REVERB_PORT?: string;
      VITE_REVERB_APP_KEY?: string;
      VITE_REVERB_SCHEME?: string;
    };
  }
}

const BASE_URL =
  window.__ENV__?.VITE_API_URL ||
  import.meta.env.VITE_API_URL ||
  'https://cmo-api.marioalabi.com/api/v1';



export class ApiError extends Error {
  status: number;
  errors?: Record<string, string[]>;

  constructor(message: string, status: number, errors?: Record<string, string[]>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.errors = errors;
  }
}

let onUnauthorizedCallback: (() => void) | null = null;

export function setOnUnauthorized(cb: () => void) {
  onUnauthorizedCallback = cb;
}

export function getToken(): string | null {
  return sessionStorage.getItem('cmo_auth_token') || localStorage.getItem('cmo_auth_token');
}

export function setToken(token: string, persist = false) {
  if (persist) {
    localStorage.setItem('cmo_auth_token', token);
  } else {
    sessionStorage.setItem('cmo_auth_token', token);
  }
}

export function removeToken() {
  sessionStorage.removeItem('cmo_auth_token');
  localStorage.removeItem('cmo_auth_token');
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = `${BASE_URL.replace(/\/$/, '')}/${endpoint.replace(/^\//, '')}`;
  const token = getToken();

  const headers: Record<string, string> = {
    'Accept': 'application/json',
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(url, {
      ...options,
      headers,
    });
  } catch (err) {
    throw new ApiError(
      'No se pudo conectar con el servidor. Verifica que la API esté corriendo.',
      0
    );
  }

  let body: ApiResponse<T>;
  try {
    body = await response.json();
  } catch {
    throw new ApiError(
      `Error de respuesta del servidor (${response.status})`,
      response.status
    );
  }

  if (response.status === 401) {
    removeToken();
    if (onUnauthorizedCallback) {
      onUnauthorizedCallback();
    }
    throw new ApiError(body.message || 'Sesión expirada o no autorizada.', 401);
  }

  if (!response.ok || body.success === false) {
    throw new ApiError(
      body.message || 'Error en la petición.',
      response.status,
      body.errors
    );
  }

  return (body.data !== undefined ? body.data : (body as unknown as T)) as T;
}

export const apiClient = {
  get: <T>(endpoint: string, options?: RequestInit) =>
    request<T>(endpoint, { ...options, method: 'GET' }),

  post: <T>(endpoint: string, data?: unknown, options?: RequestInit) =>
    request<T>(endpoint, {
      ...options,
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    }),

  put: <T>(endpoint: string, data?: unknown, options?: RequestInit) =>
    request<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    }),

  patch: <T>(endpoint: string, data?: unknown, options?: RequestInit) =>
    request<T>(endpoint, {
      ...options,
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined,
    }),

  delete: <T>(endpoint: string, options?: RequestInit) =>
    request<T>(endpoint, { ...options, method: 'DELETE' }),
};
