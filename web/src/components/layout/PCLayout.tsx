import { useState, useEffect } from 'react';
import { Layout, Menu, Avatar, Dropdown, Typography, Spin } from 'antd';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  UnorderedListOutlined,
  BarChartOutlined,
  AuditOutlined,
  SettingOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { getMe } from '@/api/auth';
import type { SessionInfo } from '@/types/user';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

export default function PCLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [user, setUser] = useState<SessionInfo | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMe()
      .then((me) => setUser(me))
      .catch(() => {
        // 401 已自动跳走
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    );
  }

  const menuItems = [
    { key: '/requirements', icon: <UnorderedListOutlined />, label: '需求列表' },
    { key: '/dashboard', icon: <BarChartOutlined />, label: '数据看板' },
    { key: '/admin/audit', icon: <AuditOutlined />, label: '审计日志' },
    { key: '/admin/settings', icon: <SettingOutlined />, label: '系统设置' },
  ];

  const selectedKey = '/' + location.pathname.split('/')[1];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        width={220}
        theme="dark"
        breakpoint="lg"
        collapsedWidth={0}
        style={{ position: 'sticky', top: 0, height: '100vh' }}
      >
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff' }}>
          <span style={{ fontSize: 16, fontWeight: 600 }}>AI 需求管理</span>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            borderBottom: '1px solid #f0f0f0',
            position: 'sticky',
            top: 0,
            zIndex: 10,
          }}
        >
          <Dropdown
            menu={{
              items: [
                {
                  key: 'logout',
                  icon: <LogoutOutlined />,
                  label: '登出',
                  onClick: () => (window.location.href = '/admin/oauth/logout'),
                },
              ],
            }}
          >
            <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
              <Avatar size="small" icon={<UserOutlined />} />
              <Text>{user?.displayName || user?.userId || '未登录'}</Text>
            </div>
          </Dropdown>
        </Header>
        <Content style={{ padding: 24, background: '#f5f5f5' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
