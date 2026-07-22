// 需求类型枚举
export const TYPE_OPTIONS = [
  { value: 'DATA_REPORT', label: '数据报表' },
  { value: 'BUSINESS_FLOW', label: '业务流程' },
  { value: 'API_INTEGRATION', label: '接口对接' },
  { value: 'UI_CHANGE', label: 'UI 调整' },
  { value: 'RULE_CHANGE', label: '规则变更' },
  { value: 'OTHER', label: '其他' },
] as const;

export type RequirementType = typeof TYPE_OPTIONS[number]['value'];

// 状态枚举
export const STATUS_OPTIONS = [
  { value: 'SUBMITTED', label: '已提交' },
  { value: 'QUESTIONING', label: '追问中' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'ARCHIVED', label: '已归档' },
  { value: 'DONE', label: '完成' },
] as const;

export type DemandStatus = typeof STATUS_OPTIONS[number]['value'];

// 优先级枚举
export const PRIORITY_OPTIONS = [
  { value: 'P0', label: 'P0 紧急' },
  { value: 'P1', label: 'P1 高' },
  { value: 'P2', label: 'P2 中' },
  { value: 'P3', label: 'P3 低' },
] as const;

export type Priority = typeof PRIORITY_OPTIONS[number]['value'];

// 来源
export const SOURCE_OPTIONS = [
  { value: 'wecom', label: '企信' },
  { value: 'web', label: 'Web' },
  { value: 'import', label: '导入' },
] as const;

export type Source = typeof SOURCE_OPTIONS[number]['value'];

// 问答历史项
export interface QAItem {
  q: string;
  a: string;
}

// 评论
export interface Comment {
  id: number;
  authorUserId: string;
  authorName: string;
  content: string;
  createdAt: string;
}

// 审计日志
export interface AuditLog {
  id: number;
  actorUserId: string;
  actorName: string;
  fieldName: string;
  beforeValue?: string;
  afterValue?: string;
  createdAt: string;
}

// 需求实体
export interface Demand {
  id: number;
  userId: string;
  department?: string;
  source?: string;
  type?: RequirementType;
  title: string;
  rawInput?: string;
  businessContext?: string;
  userRole?: string;
  acceptanceCriteria?: string;
  priority?: Priority;
  status: DemandStatus;
  assigneeUserId?: string;
  sprintVersion?: string;
  estimatedHours?: number;
  deadline?: string;
  categoryTags?: string[];
  relatedDemandIds?: number[];
  attachments?: string[];
  closedAt?: string;
  feedbackRating?: number;
  notes?: string;
  qaHistory?: QAItem[];
  comments?: Comment[];
  audit?: AuditLog[];
  createdAt: string;
  updatedAt: string;
}

// 列表项（精简版）
export interface DemandListItem {
  id: number;
  userId: string;
  department?: string;
  source?: string;
  type?: RequirementType;
  title: string;
  priority?: Priority;
  status: DemandStatus;
  assigneeUserId?: string;
  sprintVersion?: string;
  estimatedHours?: number;
  deadline?: string;
  createdAt: string;
  updatedAt: string;
}

// 分页结果
export interface PagedResult<T> {
  total: number;
  size: number;
  offset: number;
  items: T[];
}

// 列表查询参数
export interface DemandListParams {
  offset?: number;
  size?: number;
  q?: string;
  status?: string;
  priority?: string;
  type?: string;
  assignee?: string;
  source?: string;
  department?: string;
  sprintVersion?: string;
  sort?: string;
  order?: 'asc' | 'desc';
}
