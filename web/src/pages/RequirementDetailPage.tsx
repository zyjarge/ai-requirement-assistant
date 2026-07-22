import { useState, useEffect } from 'react';
import {
  Card, Descriptions, Tag, Button, Space, Drawer, Form, Select, Input, DatePicker, InputNumber, Spin,
  Tabs, List, Empty, message,
} from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeftOutlined, EditOutlined, DownloadOutlined, CommentOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { getDemand, patchDemand, addComment, getAssignableUsers, getExportUrl, type DemandPatch } from '@/api/requirements';
import { useDeviceType } from '@/hooks/useDeviceType';
import {
  STATUS_OPTIONS, PRIORITY_OPTIONS, TYPE_OPTIONS,
  type Demand,
} from '@/types/demand';
import { fmtTime } from '@/utils/time';

const TYPE_LABELS = Object.fromEntries(TYPE_OPTIONS.map((t) => [t.value, t.label]));
const STATUS_LABELS = Object.fromEntries(STATUS_OPTIONS.map((s) => [s.value, s.label]));
const PRIORITY_LABELS = Object.fromEntries(PRIORITY_OPTIONS.map((p) => [p.value, p.label]));

const STATUS_COLORS: Record<string, string> = {
  SUBMITTED: 'red', QUESTIONING: 'orange', IN_PROGRESS: 'green', ARCHIVED: 'blue', DONE: 'purple',
};
const PRIORITY_COLORS: Record<string, string> = {
  P0: 'red', P1: 'orange', P2: 'blue', P3: 'default',
};

export default function RequirementDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const deviceType = useDeviceType();
  const [demand, setDemand] = useState<Demand | null>(null);
  const [loading, setLoading] = useState(true);
  const [editOpen, setEditOpen] = useState(false);
  const [users, setUsers] = useState<{ userid: string; name: string }[]>([]);
  const [commentText, setCommentText] = useState('');
  const [form] = Form.useForm();

  const load = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const d = await getDemand(Number(id));
      setDemand(d);
      form.setFieldsValue({
        status: d.status,
        priority: d.priority,
        assigneeUserId: d.assigneeUserId,
        sprintVersion: d.sprintVersion,
        estimatedHours: d.estimatedHours,
        deadline: d.deadline ? dayjs(d.deadline) : null,
        notes: d.notes,
      });
    } catch {
      // 拦截器已处理
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [id]);

  useEffect(() => {
    getAssignableUsers().then(setUsers).catch(() => {});
  }, []);

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const body: DemandPatch = {
        status: values.status,
        priority: values.priority,
        assigneeUserId: values.assigneeUserId,
        sprintVersion: values.sprintVersion,
        estimatedHours: values.estimatedHours,
        deadline: values.deadline ? values.deadline.format('YYYY-MM-DD') : undefined,
        notes: values.notes,
      };
      await patchDemand(Number(id), body);
      message.success('已保存');
      setEditOpen(false);
      load();
    } catch {
      // 拦截器已处理
    }
  };

  const handleAddComment = async () => {
    if (!commentText.trim()) {
      message.warning('评论内容不能为空');
      return;
    }
    try {
      await addComment(Number(id), commentText.trim());
      setCommentText('');
      message.success('评论已添加');
      load();
    } catch {
      // 拦截器已处理
    }
  };

  if (loading || !demand) {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    );
  }

  // ============ 移动端布局 ============
  if (deviceType === 'mobile') {
    return (
      <div style={{ padding: 12 }}>
        <div style={{ marginBottom: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>返回</Button>
          <h3 style={{ margin: 0, flex: 1 }}>#{demand.id} {demand.title || '(无标题)'}</h3>
        </div>

        <Card size="small" style={{ marginBottom: 12 }}>
          <Space size={4} wrap>
            <Tag color={STATUS_COLORS[demand.status]}>{STATUS_LABELS[demand.status]}</Tag>
            {demand.priority && <Tag color={PRIORITY_COLORS[demand.priority]}>{PRIORITY_LABELS[demand.priority]}</Tag>}
            {demand.type && <Tag>{TYPE_LABELS[demand.type]}</Tag>}
          </Space>
        </Card>

        <Tabs
          items={[
            {
              key: 'info',
              label: '详情',
              children: (
                <>
                  <Card title="结构化" size="small" style={{ marginBottom: 12 }}>
                    <Descriptions column={1} size="small">
                      <Descriptions.Item label="原始需求">{demand.rawInput || '-'}</Descriptions.Item>
                      <Descriptions.Item label="业务背景">{demand.businessContext || '-'}</Descriptions.Item>
                      <Descriptions.Item label="使用对象">{demand.userRole || '-'}</Descriptions.Item>
                      <Descriptions.Item label="验收标准">{demand.acceptanceCriteria || '-'}</Descriptions.Item>
                    </Descriptions>
                  </Card>
                  {demand.qaHistory && demand.qaHistory.length > 0 && (
                    <Card title="问答历史" size="small" style={{ marginBottom: 12 }}>
                      <List
                        size="small"
                        dataSource={demand.qaHistory}
                        renderItem={(item) => (
                          <List.Item>
                            <div style={{ width: '100%' }}>
                              <div style={{ color: '#1677ff' }}>Q: {item.q}</div>
                              <div>A: {item.a || '(空)'}</div>
                            </div>
                          </List.Item>
                        )}
                      />
                    </Card>
                  )}
                </>
              ),
            },
            {
              key: 'manage',
              label: '管理',
              children: (
                <Card size="small">
                  <Form layout="vertical" form={form}>
                    <Form.Item label="状态" name="status">
                      <Select options={STATUS_OPTIONS} />
                    </Form.Item>
                    <Form.Item label="优先级" name="priority">
                      <Select options={PRIORITY_OPTIONS} allowClear />
                    </Form.Item>
                    <Form.Item label="指派给" name="assigneeUserId">
                      <Select
                        options={users.map((u) => ({ value: u.userid, label: u.name || u.userid }))}
                        allowClear
                        placeholder="未派单"
                      />
                    </Form.Item>
                    <Form.Item label="Sprint" name="sprintVersion">
                      <Input />
                    </Form.Item>
                    <Form.Item label="截止日期" name="deadline">
                      <DatePicker style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item label="预估工时" name="estimatedHours">
                      <InputNumber min={0} step={0.5} style={{ width: '100%' }} addonAfter="人天" />
                    </Form.Item>
                    <Form.Item label="备注" name="notes">
                      <Input.TextArea rows={3} />
                    </Form.Item>
                    <Button type="primary" onClick={handleSave} block>保存</Button>
                  </Form>
                </Card>
              ),
            },
            {
              key: 'comments',
              label: `评论 ${demand.comments?.length || 0}`,
              children: (
                <>
                  <Card size="small" style={{ marginBottom: 12 }}>
                    <Input.TextArea
                      rows={3}
                      value={commentText}
                      onChange={(e) => setCommentText(e.target.value)}
                      placeholder="说点什么..."
                    />
                    <Button type="primary" onClick={handleAddComment} style={{ marginTop: 8 }} block>
                      提交评论
                    </Button>
                  </Card>
                  {demand.comments && demand.comments.length > 0 ? (
                    <List
                      size="small"
                      dataSource={demand.comments}
                      renderItem={(c) => (
                        <List.Item>
                          <div style={{ width: '100%' }}>
                            <div>{c.content}</div>
                            <div style={{ fontSize: 11, color: '#999' }}>
                              {c.authorName || c.authorUserId} · {fmtTime(c.createdAt)}
                            </div>
                          </div>
                        </List.Item>
                      )}
                    />
                  ) : (
                    <Empty description="暂无评论" />
                  )}
                </>
              ),
            },
            {
              key: 'audit',
              label: `审计 ${demand.audit?.length || 0}`,
              children: demand.audit && demand.audit.length > 0 ? (
                <List
                  size="small"
                  dataSource={demand.audit}
                  renderItem={(a) => (
                    <List.Item>
                      <div style={{ width: '100%', fontSize: 12 }}>
                        <div>
                          <strong>{a.fieldName}</strong>: <span style={{ color: '#999' }}>{a.beforeValue || '∅'}</span> → <strong>{a.afterValue || '∅'}</strong>
                        </div>
                        <div style={{ fontSize: 11, color: '#999' }}>
                          {a.actorName || a.actorUserId} · {fmtTime(a.createdAt)}
                        </div>
                      </div>
                    </List.Item>
                  )}
                />
              ) : (
                <Empty description="暂无变更记录" />
              ),
            },
          ]}
        />

        <div style={{ marginTop: 16, textAlign: 'center' }}>
          <Button icon={<DownloadOutlined />} onClick={() => window.open(getExportUrl(demand.id))}>
            导出 CSV
          </Button>
        </div>
      </div>
    );
  }

  // ============ PC 端布局 ============
  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/requirements')}>返回列表</Button>
        <h2 style={{ margin: 0, flex: 1 }}>#{demand.id} {demand.title || '(无标题)'}</h2>
        <Space>
          <Button icon={<EditOutlined />} type="primary" onClick={() => setEditOpen(true)}>编辑</Button>
          <Button icon={<DownloadOutlined />} onClick={() => window.open(getExportUrl(demand.id))}>导出 CSV</Button>
        </Space>
      </div>

      <Card title="结构化" size="small" style={{ marginBottom: 16 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="类型">{demand.type ? TYPE_LABELS[demand.type] : '-'}</Descriptions.Item>
          <Descriptions.Item label="提交人">
            {users.find((u) => u.userid === demand.userId)?.name || demand.userId}
          </Descriptions.Item>
          <Descriptions.Item label="部门">{demand.department || '-'}</Descriptions.Item>
          <Descriptions.Item label="来源">{demand.source || '-'}</Descriptions.Item>
          <Descriptions.Item label="原始需求" span={2}>{demand.rawInput || '-'}</Descriptions.Item>
          <Descriptions.Item label="业务背景" span={2}>{demand.businessContext || '-'}</Descriptions.Item>
          <Descriptions.Item label="使用对象" span={2}>{demand.userRole || '-'}</Descriptions.Item>
          <Descriptions.Item label="验收标准" span={2}>{demand.acceptanceCriteria || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      {demand.qaHistory && demand.qaHistory.length > 0 && (
        <Card title="问答历史" size="small" style={{ marginBottom: 16 }}>
          <List
            size="small"
            dataSource={demand.qaHistory}
            renderItem={(item) => (
              <List.Item>
                <div style={{ width: '100%' }}>
                  <div style={{ color: '#1677ff' }}>Q: {item.q}</div>
                  <div>A: {item.a || '(空)'}</div>
                </div>
              </List.Item>
            )}
          />
        </Card>
      )}

      <Card
        title={
          <Space>
            <CommentOutlined />
            <span>评论 ({demand.comments?.length || 0})</span>
          </Space>
        }
        size="small"
        style={{ marginBottom: 16 }}
      >
        <Input.TextArea
          rows={3}
          value={commentText}
          onChange={(e) => setCommentText(e.target.value)}
          placeholder="说点什么..."
        />
        <Button type="primary" onClick={handleAddComment} style={{ marginTop: 8 }}>
          提交评论
        </Button>
        {demand.comments && demand.comments.length > 0 && (
          <List
            size="small"
            dataSource={demand.comments}
            style={{ marginTop: 16 }}
            renderItem={(c) => (
              <List.Item>
                <div style={{ width: '100%' }}>
                  <div>{c.content}</div>
                  <div style={{ fontSize: 11, color: '#999' }}>
                    {c.authorName || c.authorUserId} · {fmtTime(c.createdAt)}
                  </div>
                </div>
              </List.Item>
            )}
          />
        )}
      </Card>

      {demand.audit && demand.audit.length > 0 && (
        <Card title={`审计日志 (${demand.audit.length})`} size="small">
          <List
            size="small"
            dataSource={demand.audit}
            renderItem={(a) => (
              <List.Item>
                <div style={{ width: '100%', fontSize: 12 }}>
                  <div>
                    <strong>{a.fieldName}</strong>: <span style={{ color: '#999' }}>{a.beforeValue || '∅'}</span> → <strong>{a.afterValue || '∅'}</strong>
                  </div>
                  <div style={{ fontSize: 11, color: '#999' }}>
                    {a.actorName || a.actorUserId} · {fmtTime(a.createdAt)}
                  </div>
                </div>
              </List.Item>
            )}
          />
        </Card>
      )}

      <Drawer
        title={`编辑 #${demand.id}`}
        open={editOpen}
        onClose={() => setEditOpen(false)}
        width={520}
        extra={
          <Space>
            <Button onClick={() => setEditOpen(false)}>取消</Button>
            <Button type="primary" onClick={handleSave}>保存</Button>
          </Space>
        }
      >
        <Form layout="vertical" form={form}>
          <Form.Item label="状态" name="status">
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
          <Form.Item label="优先级" name="priority">
            <Select options={PRIORITY_OPTIONS} allowClear placeholder="未设置" />
          </Form.Item>
          <Form.Item label="指派给" name="assigneeUserId">
            <Select
              options={users.map((u) => ({ value: u.userid, label: u.name || u.userid }))}
              allowClear
              placeholder="未派单"
            />
          </Form.Item>
          <Form.Item label="Sprint" name="sprintVersion">
            <Input placeholder="如 v1.0" />
          </Form.Item>
          <Form.Item label="截止日期" name="deadline">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="预估工时" name="estimatedHours">
            <InputNumber min={0} step={0.5} style={{ width: '100%' }} addonAfter="人天" />
          </Form.Item>
          <Form.Item label="备注" name="notes">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Drawer>
    </div>
  );
}
