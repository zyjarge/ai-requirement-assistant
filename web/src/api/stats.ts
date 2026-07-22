import client from './client';
import type { DashboardSummary, StatByField, TrendDataPoint } from '@/types/stats';

export async function getSummary(): Promise<DashboardSummary> {
  const { data } = await client.get<DashboardSummary>('/admin/api/stats/summary');
  return data;
}

export async function getByType(): Promise<StatByField[]> {
  const { data } = await client.get<StatByField[]>('/admin/api/stats/by-type');
  return data;
}

export async function getByStatus(): Promise<StatByField[]> {
  const { data } = await client.get<StatByField[]>('/admin/api/stats/by-status');
  return data;
}

export async function getByDepartment(): Promise<StatByField[]> {
  const { data } = await client.get<StatByField[]>('/admin/api/stats/by-department');
  return data;
}

export async function getTrend(): Promise<TrendDataPoint[]> {
  const { data } = await client.get<TrendDataPoint[]>('/admin/api/stats/trend');
  return data;
}
