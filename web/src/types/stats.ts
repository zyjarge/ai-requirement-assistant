// 数据统计
export interface DashboardSummary {
  total: number;
  todayNew: number;
  inProgress: number;
  completed: number;
}

export interface StatByField {
  field: string;
  label: string;
  count: number;
}

export interface TrendDataPoint {
  date: string;
  count: number;
}
