import client from './client';
import type { SessionInfo } from '@/types/user';

export async function getMe(): Promise<SessionInfo> {
  const { data } = await client.get<SessionInfo>('/admin/oauth/me');
  return data;
}
