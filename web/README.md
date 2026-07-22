# AI 需求管理 - Web 前端

基于 React 18 + TypeScript + Vite 的现代化管理端。

## 技术栈

- **React 18** + **TypeScript** + **Vite**
- **Ant Design 5**（PC 端）
- **Ant Design Mobile**（移动端）
- **TipTap**（富文本编辑器，备 P1 使用）
- **ECharts**（数据可视化）
- **Axios**（HTTP 客户端）
- **React Router 6**（路由）

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器（带后端 API 代理）
npm run dev
# 默认 http://localhost:5173
```

开发服务器的 API 代理自动转发到 `http://localhost:9080`（Spring Boot）。

## 构建

```bash
npm run build
# 产物在 dist/ 目录
```

## 目录结构

```
web/
├── src/
│   ├── api/             # API 客户端（按模块拆分）
│   ├── components/
│   │   ├── common/      # 通用组件（RichEditor 等）
│   │   └── layout/      # 布局（PCLayout/MobileLayout/ResponsiveLayout）
│   ├── hooks/           # 自定义 hooks
│   ├── pages/           # 页面
│   ├── styles/          # 全局样式
│   ├── types/           # TypeScript 类型
│   ├── utils/           # 工具函数
│   ├── App.tsx
│   ├── main.tsx
│   └── router.tsx
├── deploy/
│   └── nginx.conf       # Nginx 部署配置
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 响应式设计

- **断点**：`< 768px` 移动端，`>= 768px` PC 端
- `useDeviceType` hook 自动检测屏幕宽度
- `ResponsiveLayout` 组件自动切换布局

## 移动端适配

- 用 Ant Design Mobile 组件重写关键页面
- 触屏友好（最小点击区域 44px × 44px）
- 列表用 Card 替代 Table
- 详情用 Tabs 折叠不同模块

## 部署

参见 `deploy/nginx.conf`

## 后续计划

- [ ] P0.5: LLM 调用超时 + 重试
- [ ] P1: 状态机引擎（替换当前简单状态）
- [ ] P1: 群内审批卡片
- [ ] P1: Meilisearch 全文搜索
- [ ] P1: 通知体系
- [ ] P2: 完整 TipTap 富文本
- [ ] P2: 附件上传（MinIO）
- [ ] P2: 变更管理
