import { apiClient, setToken, removeToken } from './client';
import { AuthResponseData, LoginCredentials, User } from '../types/auth';

export async function loginApi(credentials: LoginCredentials, remember = false): Promise<AuthResponseData> {
  const data = await apiClient.post<AuthResponseData>('auth/login', credentials);
  if (data?.token) {
    setToken(data.token, remember);
  }
  return data;
}

export async function getProfileApi(): Promise<{ usuario: User; id_paciente?: number | string | null }> {
  return apiClient.get<{ usuario: User; id_paciente?: number | string | null }>('auth/me');
}

export async function logoutApi(): Promise<void> {
  try {
    await apiClient.post('auth/logout');
  } finally {
    removeToken();
  }
}
