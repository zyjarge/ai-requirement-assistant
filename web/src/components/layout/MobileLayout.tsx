import { useState, useEffect } from 'react';
import { TabBar, SpinLoading } from 'antd-mobile';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  UnorderedListOutline,
  AppOutline,
  UserOutline,
} from 'antd-mobile-icons';
import { getMe } from '@/api/auth';
import type { SessionInfo } from '@/types/user';

const tabs = [
  { key: '/requirements', title: '需求', icon: <UnorderedListOutline /> },
  { key: '/dashboard', title: '看板', icon: <AppOutline /> },
  { key: '/me', title: '我的', icon: <UserOutline /> },
];

export default function MobileLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [user, setUser] = useState<SessionInfo | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <SpinLoading />
      </div>
    );
  }

  // 简化路由：只展示需求列表（详情走全屏）
  if (location.pathname === '/me') {
    return (
      <div style={{ minHeight: '100vh', background: '#f5f5f5', paddingTop: 56 }}>
        <div
          style={{
            position: 'sticky',
            top: 0,
            background: '#fff',
            padding: '16px',
            borderBottom: '1px solid #eee',
            zIndex: 10,
            textAlign: 'center',
          }}
        >
          <strong style={{ fontSize: 16 }}>我的</strong>
        </div>
        <div style={{ padding: 16 }}>
          <div style={{ background: '#fff', padding: 16, borderRadius: 8, marginBottom: 12 }}>
            <div style={{ color: '#999', fontSize: 12, marginBottom: 4 }}>当前用户</div>
            <div style={{ fontSize: 16 }}>{user?.displayName || user?.userId || '未登录'}</div>
          </div>
          <div
            style={{ background: '#fff', padding: 16, borderRadius: 8, textAlign: 'center', color: '#1677ff' }}
            onClick={() => (window.location.href = '/admin/oauth/logout')}
          >
            登出
          </div>
        </div>
        <div style={{ height: 50 }} />
        <TabBar
          activeKey={tabs.find((t) => location.pathname.startsWith(t.key))?.key || '/requirements'}
          onChange={(key) => navigate(key)}
          style={{ position: 'fixed', bottom: 0, left: 0, right: 0, borderTop: '1px solid #eee' }}
        >
          {tabs.map((t) => (
            <TabBar.Item key={t.key} title={t.title} icon={t.icon} />
          ))}
        </TabBar>
      </div>
    );
  }

  // 其他路由：展示 Outlet + 底部 TabBar
  return (
    <div style={{ minHeight: '100vh', background: '#f5f5f5', paddingBottom: 50 }}>
      <div
        style={{
          position: 'sticky',
          top: 0,
          background: '#1677ff',
          color: '#fff',
          padding: '12px 16px',
          zIndex: 10,
          fontSize: 16,
          fontWeight: 600,
        }}
      >
        AI 需求管理
      </div>
      <Outlet />
      <TabBar
        activeKey={tabs.find((t) => location.pathname.startsWith(t.key))?.key || '/requirements'}
        onChange={(key) => navigate(key)}
        style={{ position: 'fixed', bottom: 0, left: 0, right: 0, borderTop: '1px solid #eee' }}
      >
        {tabs.map((t) => (
          <TabBar.Item key={t.key} title={t.title} icon={t.icon} />
        ))}
      </TabBar>
    </div>
  );
}
