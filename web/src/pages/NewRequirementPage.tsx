import { useState } from 'react';
import { Form, Input, Select, DatePicker, InputNumber, Button, Card, message, Space } from 'antd';
import { ArrowLeftOutlined, SaveOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { createDemand } from '@/api/requirements';
import { STATUS_OPTIONS, PRIORITY_OPTIONS, TYPE_OPTIONS, type Demand, type RequirementType, type Priority } from '@/types/demand';

export default function NewRequirementPage() {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [description, setDescription] = useState('');

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      const body: Partial<Demand> = {
        title: values.title,
        rawInput: values.rawInput,
        type: values.type,
        priority: values.priority,
        status: 'SUBMITTED',
        acceptanceCriteria: description || undefined,
        deadline: values.deadline ? values.deadline.format('YYYY-MM-DD') : undefined,
        estimatedHours: values.estimatedHours,
        notes: values.notes,
      };
      await createDemand(body);
      message.success('需求创建成功');
      navigate('/requirements');
    } catch {
      // 拦截器已处理
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', gap: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>返回</Button>
        <h2 style={{ margin: 0 }}>新建需求</h2>
      </div>

      <Card>
        <Form layout="vertical" form={form} initialValues={{ type: 'OTHER', priority: 'P2' }}>
          <Form.Item label="标题" name="title" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="一句话描述需求" maxLength={100} showCount />
          </Form.Item>

          <Form.Item label="原始需求描述" name="rawInput" rules={[{ required: true, message: '请输入需求描述' }]}>
            <Input.TextArea rows={4} placeholder="详细描述这个需求是什么" />
          </Form.Item>

          <Form.Item label="类型" name="type">
            <Select options={TYPE_OPTIONS} />
          </Form.Item>

          <Form.Item label="优先级" name="priority">
            <Select options={PRIORITY_OPTIONS} />
          </Form.Item>

          <Form.Item label="截止日期" name="deadline">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item label="预估工时" name="estimatedHours">
            <InputNumber min={0} step={0.5} style={{ width: '100%' }} addonAfter="人天" />
          </Form.Item>

          <Form.Item label="验收标准（富文本）">
            <Input.TextArea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="什么情况下认为这个需求完成了？（占位，后续 P1 接 TipTap）"
            />
          </Form.Item>

          <Form.Item label="备注" name="notes">
            <Input.TextArea rows={2} placeholder="其他说明" />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" icon={<SaveOutlined />} onClick={handleSubmit} loading={saving}>
                创建
              </Button>
              <Button onClick={() => navigate(-1)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
