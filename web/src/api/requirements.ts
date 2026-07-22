import client from './client';
import type { Demand, DemandListItem, DemandListParams, PagedResult, QAItem, Comment } from '@/types/demand';

// 列表
export async function listDemands(params: DemandListParams): Promise<PagedResult<DemandListItem>> {
  const { data } = await client.get<PagedResult<DemandListItem>>('/admin/api/requirements', { params });
  return data;
}

// 详情
export async function getDemand(id: number): Promise<Demand> {
  const { data } = await client.get<Demand>(`/admin/api/requirements/${id}`);
  return data;
}

// 更新（部分字段）
export interface DemandPatch {
  status?: string;
  priority?: string;
  assigneeUserId?: string;
  notes?: string;
  department?: string;
  source?: string;
  categoryTags?: string;
  sprintVersion?: string;
  relatedDemandIds?: string;
  estimatedHours?: number;
  deadline?: string;
  feedbackRating?: number;
  closedAt?: string;
}

export async function patchDemand(id: number, body: DemandPatch): Promise<DemandListItem> {
  const { data } = await client.patch<DemandListItem>(`/admin/api/requirements/${id}`, body);
  return data;
}

// 新增（手动创建）
export async function createDemand(body: Partial<Demand>): Promise<DemandListItem> {
  const { data } = await client.post<DemandListItem>('/admin/api/requirements', body);
  return data;
}

// 评论
export async function addComment(demandId: number, content: string): Promise<Comment> {
  const { data } = await client.post<Comment>(`/admin/api/requirements/${demandId}/comments`, { content });
  return data;
}

// 导出 CSV
export function getExportUrl(id: number): string {
  return `/admin/api/requirements/${id}/export`;
}

// 可指派用户
export async function getAssignableUsers(): Promise<{ userid: string; name: string }[]> {
  const { data } = await client.get<{ userid: string; name: string }[]>('/admin/api/assignable-users');
  return data;
}
