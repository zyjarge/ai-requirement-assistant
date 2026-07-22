import { useState, useEffect, useCallback } from 'react';
import { Table, Card, Input, Select, Button, Space, Tag, message, Skeleton, Empty } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { SearchOutlined, DownloadOutlined, PlusOutlined } from '@ant-design/icons';
import { listDemands, getAssignableUsers } from '@/api/requirements';
import { useDeviceType } from '@/hooks/useDeviceType';
import {
  STATUS_OPTIONS,
  PRIORITY_OPTIONS,
  TYPE_OPTIONS,
  SOURCE_OPTIONS,
  type DemandListItem,
  type DemandListParams,
  type DemandStatus,
  type Priority,
  type RequirementType,
} from '@/types/demand';
import { fmtTime } from '@/utils/time';

const TYPE_LABELS = Object.fromEntries(TYPE_OPTIONS.map((t) => [t.value, t.label]));
const STATUS_LABELS = Object.fromEntries(STATUS_OPTIONS.map((s) => [s.value, s.label]));
const PRIORITY_LABELS = Object.fromEntries(PRIORITY_OPTIONS.map((p) => [p.value, p.label]));
const SOURCE_LABELS = Object.fromEntries(SOURCE_OPTIONS.map((s) => [s.value, s.label]));

const STATUS_COLORS: Record<DemandStatus, string> = {
  SUBMITTED: 'red',
  QUESTIONING: 'orange',
  IN_PROGRESS: 'green',
  ARCHIVED: 'blue',
  DONE: 'purple',
};

const PRIORITY_COLORS: Record<Priority, string> = {
  P0: 'red',
  P1: 'orange',
  P2: 'blue',
  P3: 'default',
};

export default function RequirementsListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const deviceType = useDeviceType();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<DemandListItem[]>([]);
  const [total, setTotal] = useState(0);
  const [users, setUsers] = useState<{ userid: string; name: string }[]>([]);
  const [page, setPage] = useState(() => Number(searchParams.get('offset') || 0) / 20);
  const [pageSize] = useState(20);
  const [filters, setFilters] = useState({
    q: searchParams.get('q') || '',
    status: searchParams.get('status') || '',
    priority: searchParams.get('priority') || '',
    type: searchParams.get('type') || '',
    assignee: searchParams.get('assignee') || '',
    source: searchParams.get('source') || '',
  });

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const params: DemandListParams = {
        offset: page * pageSize,
        size: pageSize,
        ...filters,
      };
      const result = await listDemands(params);
      setData(result.items);
      setTotal(result.total);
    } catch {
      // 拦截器已处理
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, filters]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    getAssignableUsers().then(setUsers).catch(() => {});
  }, []);

  const handleSearch = () => {
    setPage(0);
    const sp = new URLSearchParams();
    Object.entries(filters).forEach(([k, v]) => v && sp.set(k, v));
    setSearchParams(sp);
  };

  const handleExport = async () => {
    try {
      const params = new URLSearchParams({ ...filters } as Record<string, string>);
      params.delete('offset');
      params.delete('size');
      const url = `/admin/api/requirements/export?${params.toString()}`;
      const res = await fetch(url, { credentials: 'include' });
      if (!res.ok) {
        message.error('导出失败');
        return;
      }
      const blob = await res.blob();
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `requirements-${Date.now()}.csv`;
      a.click();
      URL.revokeObjectURL(a.href);
    } catch {
      message.error('导出失败');
    }
  };

  // 移动端：Card 列表
  if (deviceType === 'mobile') {
    return (
      <div style={{ padding: 12 }}>
        <Card size="small" style={{ marginBottom: 12 }}>
          <Space direction="vertical" size="small" style={{ width: '100%' }}>
            <Input
              placeholder="搜索标题或原始内容"
              prefix={<SearchOutlined />}
              value={filters.q}
              onChange={(e) => setFilters({ ...filters, q: e.target.value })}
              onPressEnter={handleSearch}
              allowClear
            />
            <Space wrap>
              <Select
                placeholder="状态"
                value={filters.status || undefined}
                onChange={(v) => setFilters({ ...filters, status: v || '' })}
                style={{ minWidth: 100 }}
                allowClear
                options={STATUS_OPTIONS}
              />
              <Select
                placeholder="优先级"
                value={filters.priority || undefined}
                onChange={(v) => setFilters({ ...filters, priority: v || '' })}
                style={{ minWidth: 100 }}
                allowClear
                options={PRIORITY_OPTIONS}
              />
              <Select
                placeholder="类型"
                value={filters.type || undefined}
                onChange={(v) => setFilters({ ...filters, type: v || '' })}
                style={{ minWidth: 100 }}
                allowClear
                options={TYPE_OPTIONS}
              />
            </Space>
            <Button type="primary" onClick={handleSearch} block>
              查询
            </Button>
          </Space>
        </Card>

        {loading ? (
          <Skeleton active />
        ) : data.length === 0 ? (
          <Empty description="暂无需求" />
        ) : (
          data.map((d) => (
            <Card
              key={d.id}
              size="small"
              style={{ marginBottom: 8 }}
              onClick={() => navigate(`/requirements/${d.id}`)}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                <strong>#{d.id} {d.title || '(无标题)'}</strong>
              </div>
              <div style={{ fontSize: 12, color: '#666', marginBottom: 6 }}>
                {d.userId} {d.assigneeUserId && `→ ${d.assigneeUserId}`} · {TYPE_LABELS[d.type as RequirementType] || d.type}
              </div>
              <Space size={4} wrap>
                {d.priority && <Tag color={PRIORITY_COLORS[d.priority]}>{PRIORITY_LABELS[d.priority]}</Tag>}
                <Tag color={STATUS_COLORS[d.status]}>{STATUS_LABELS[d.status]}</Tag>
                {d.sprintVersion && <Tag>Sprint {d.sprintVersion}</Tag>}
              </Space>
              <div style={{ fontSize: 11, color: '#999', marginTop: 6 }}>{fmtTime(d.createdAt)}</div>
            </Card>
          ))
        )}

        <div style={{ textAlign: 'center', margin: '12px 0', color: '#999' }}>
          共 {total} 条 · 第 {page * pageSize + 1}-{Math.min((page + 1) * pageSize, total)} 条
        </div>
        <Space>
          <Button disabled={page === 0} onClick={() => setPage(page - 1)}>上一页</Button>
          <Button disabled={(page + 1) * pageSize >= total} onClick={() => setPage(page + 1)}>下一页</Button>
        </Space>
      </div>
    );
  }

  // PC 端：Table
  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70, fixed: 'left' as const },
    { title: '提交人', dataIndex: 'userId', width: 120, render: (v: string) => users.find((u) => u.userid === v)?.name || v },
    { title: '部门', dataIndex: 'department', width: 120, render: (v?: string) => v || '-' },
    { title: '类型', dataIndex: 'type', width: 100, render: (v: RequirementType) => TYPE_LABELS[v] || v },
    {
      title: '标题',
      dataIndex: 'title',
      ellipsis: true,
      render: (v: string) => v || <span style={{ color: '#999' }}>(无标题)</span>,
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      width: 100,
      render: (v?: Priority) => (v ? <Tag color={PRIORITY_COLORS[v]}>{PRIORITY_LABELS[v]}</Tag> : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (v: DemandStatus) => <Tag color={STATUS_COLORS[v]}>{STATUS_LABELS[v]}</Tag>,
    },
    { title: '指派给', dataIndex: 'assigneeUserId', width: 120, render: (v?: string) => v || '-' },
    { title: 'Sprint', dataIndex: 'sprintVersion', width: 100, render: (v?: string) => v || '-' },
    { title: '截止', dataIndex: 'deadline', width: 110, render: (v?: string) => v || '-' },
    { title: '来源', dataIndex: 'source', width: 80, render: (v?: string) => (v ? SOURCE_LABELS[v as keyof typeof SOURCE_LABELS] || v : '-') },
    { title: '创建时间', dataIndex: 'createdAt', width: 170, render: (v: string) => fmtTime(v) },
  ];

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="搜索标题或原始内容"
            prefix={<SearchOutlined />}
            value={filters.q}
            onChange={(e) => setFilters({ ...filters, q: e.target.value })}
            onPressEnter={handleSearch}
            allowClear
            style={{ width: 220 }}
          />
          <Select placeholder="状态" value={filters.status || undefined} onChange={(v) => setFilters({ ...filters, status: v || '' })} style={{ width: 110 }} allowClear options={STATUS_OPTIONS} />
          <Select placeholder="优先级" value={filters.priority || undefined} onChange={(v) => setFilters({ ...filters, priority: v || '' })} style={{ width: 110 }} allowClear options={PRIORITY_OPTIONS} />
          <Select placeholder="类型" value={filters.type || undefined} onChange={(v) => setFilters({ ...filters, type: v || '' })} style={{ width: 110 }} allowClear options={TYPE_OPTIONS} />
          <Select placeholder="指派人" value={filters.assignee || undefined} onChange={(v) => setFilters({ ...filters, assignee: v || '' })} style={{ width: 120 }} allowClear options={users.map((u) => ({ value: u.userid, label: u.name || u.userid }))} />
          <Select placeholder="来源" value={filters.source || undefined} onChange={(v) => setFilters({ ...filters, source: v || '' })} style={{ width: 100 }} allowClear options={SOURCE_OPTIONS} />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>查询</Button>
          <Button icon={<DownloadOutlined />} onClick={handleExport}>导出 CSV</Button>
          <Button icon={<PlusOutlined />} onClick={() => navigate('/requirements/new')}>新建</Button>
        </Space>
      </Card>

      <Table<DemandListItem>
        rowKey="id"
        loading={loading}
        dataSource={data}
        columns={columns}
        scroll={{ x: 1300 }}
        onRow={(record) => ({ onClick: () => navigate(`/requirements/${record.id}`) })}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          showSizeChanger: false,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p) => setPage(p - 1),
        }}
        rowClassName={() => 'clickable-row'}
      />
    </div>
  );
}
