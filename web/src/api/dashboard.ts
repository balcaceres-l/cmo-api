import { apiClient } from './client';
import { DashboardMetricasData } from '../types/dashboard';

export async function getDashboardMetricas(): Promise<DashboardMetricasData> {
  return apiClient.get<DashboardMetricasData>('dashboard/metricas');
}
