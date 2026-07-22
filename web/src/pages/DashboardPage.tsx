import { useState, useEffect } from 'react';
import { Card, Row, Col, Spin, Empty } from 'antd';
import ReactECharts from 'echarts-for-react';
import { getSummary, getByType, getByStatus, getByDepartment, getTrend } from '@/api/stats';
import type { DashboardSummary, StatByField, TrendDataPoint } from '@/types/stats';
import { useDeviceType } from '@/hooks/useDeviceType';

const COLORS = ['#1677ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2'];

export default function DashboardPage() {
  const deviceType = useDeviceType();
  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [byType, setByType] = useState<StatByField[]>([]);
  const [byStatus, setByStatus] = useState<StatByField[]>([]);
  const [byDept, setByDept] = useState<StatByField[]>([]);
  const [trend, setTrend] = useState<TrendDataPoint[]>([]);

  useEffect(() => {
    Promise.all([getSummary(), getByType(), getByStatus(), getByDepartment(), getTrend()])
      .then(([s, t, st, d, tr]) => {
        setSummary(s);
        setByType(t);
        setByStatus(st);
        setByDept(d);
        setTrend(tr);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 60 }}>
        <Spin size="large" />
      </div>
    );
  }

  const pieOption = (data: StatByField[], title: string) => ({
    title: { text: title, left: 'center', top: 10 },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 10 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        label: { show: false },
        data: data.map((d, i) => ({ value: d.count, name: d.label, itemStyle: { color: COLORS[i % COLORS.length] } })),
      },
    ],
  });

  const barOption = (data: StatByField[], title: string) => ({
    title: { text: title, left: 'center', top: 10 },
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 30, bottom: 40, top: 50 },
    xAxis: { type: 'category', data: data.map((d) => d.label), axisLabel: { rotate: data.length > 6 ? 30 : 0 } },
    yAxis: { type: 'value' },
    series: [{
      data: data.map((d, i) => ({ value: d.count, itemStyle: { color: COLORS[i % COLORS.length] } })),
      type: 'bar',
    }],
  });

  const lineOption = (data: TrendDataPoint[]) => ({
    title: { text: '新增趋势', left: 'center', top: 10 },
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 30, bottom: 40, top: 50 },
    xAxis: { type: 'category', data: data.map((d) => d.date) },
    yAxis: { type: 'value' },
    series: [{
      data: data.map((d) => d.count),
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      itemStyle: { color: '#1677ff' },
    }],
  });

  if (deviceType === 'mobile') {
    return (
      <div style={{ padding: 12 }}>
        <h2>数据看板</h2>
        <Row gutter={[8, 8]}>
          <Col span={12}><Card><div style={{ color: '#999', fontSize: 12 }}>总需求</div><div style={{ fontSize: 24, fontWeight: 600 }}>{summary?.total || 0}</div></Card></Col>
          <Col span={12}><Card><div style={{ color: '#999', fontSize: 12 }}>今日新增</div><div style={{ fontSize: 24, fontWeight: 600, color: '#52c41a' }}>{summary?.todayNew || 0}</div></Card></Col>
          <Col span={12}><Card><div style={{ color: '#999', fontSize: 12 }}>进行中</div><div style={{ fontSize: 24, fontWeight: 600, color: '#faad14' }}>{summary?.inProgress || 0}</div></Card></Col>
          <Col span={12}><Card><div style={{ color: '#999', fontSize: 12 }}>已完成</div><div style={{ fontSize: 24, fontWeight: 600, color: '#722ed1' }}>{summary?.completed || 0}</div></Card></Col>
        </Row>
        <div style={{ marginTop: 16, color: '#999', textAlign: 'center' }}>完整图表请在 PC 端查看</div>
      </div>
    );
  }

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>数据看板</h2>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <div style={{ color: '#999', fontSize: 12 }}>总需求</div>
            <div style={{ fontSize: 32, fontWeight: 600 }}>{summary?.total || 0}</div>
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <div style={{ color: '#999', fontSize: 12 }}>今日新增</div>
            <div style={{ fontSize: 32, fontWeight: 600, color: '#52c41a' }}>{summary?.todayNew || 0}</div>
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <div style={{ color: '#999', fontSize: 12 }}>进行中</div>
            <div style={{ fontSize: 32, fontWeight: 600, color: '#faad14' }}>{summary?.inProgress || 0}</div>
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <div style={{ color: '#999', fontSize: 12 }}>已完成</div>
            <div style={{ fontSize: 32, fontWeight: 600, color: '#722ed1' }}>{summary?.completed || 0}</div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Card>
            {byStatus.length > 0 ? (
              <ReactECharts option={pieOption(byStatus, '按状态分布')} style={{ height: 300 }} />
            ) : (
              <Empty description="暂无数据" />
            )}
          </Card>
        </Col>
        <Col span={12}>
          <Card>
            {byType.length > 0 ? (
              <ReactECharts option={pieOption(byType, '按类型分布')} style={{ height: 300 }} />
            ) : (
              <Empty description="暂无数据" />
            )}
          </Card>
        </Col>
        <Col span={12}>
          <Card>
            {byDept.length > 0 ? (
              <ReactECharts option={barOption(byDept, '按部门分布')} style={{ height: 300 }} />
            ) : (
              <Empty description="暂无数据" />
            )}
          </Card>
        </Col>
        <Col span={12}>
          <Card>
            {trend.length > 0 ? (
              <ReactECharts option={lineOption(trend)} style={{ height: 300 }} />
            ) : (
              <Empty description="暂无数据" />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}
