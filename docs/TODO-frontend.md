# P0 Task: 前端重写 — 详细 TODO

> 框架：React 18 + TypeScript + Vite
> UI：Ant Design（PC）+ Ant Design Mobile（移动端）+ TipTap（富文本）
> 部署：Nginx 单独部署
> 后端：API 可重构

---

## 📊 总览

| 维度 | 目标 |
|---|---|
| 工程初始化 | 1 天 |
| PC 端功能实现 | 2-3 天 |
| 移动端适配 | 1 天 |
| 后端 API 对接 | 1 天 |
| Nginx 部署 | 0.5 天 |
| 联调测试 | 0.5 天 |
| **合计** | **6-8 天** |

---

## 阶段 1：工程初始化（1 天）

### 1.1 创建前端项目骨架

- [ ] 在项目根目录创建 `web/` 目录
- [ ] 使用 Vite 初始化 React + TypeScript 模板：
  ```bash
  npm create vite@latest web -- --template react-ts
  ```
- [ ] 删除模板里的 demo 文件
- [ ] 创建目录结构：
  ```
  web/
  ├── src/
  │   ├── api/              # API 客户端
  │   ├── components/       # 通用组件
  │   ├── pages/            # 页面
  │   ├── hooks/            # 自定义 hooks
  │   ├── utils/            # 工具函数
  │   ├── types/            # TypeScript 类型
  │   ├── store/            # 状态管理
  │   ├── styles/           # 全局样式
  │   ├── App.tsx
  │   ├── main.tsx
  │   └── router.tsx
  ├── public/
  ├── package.json
  ├── tsconfig.json
  ├── vite.config.ts
  └── index.html
  ```

### 1.2 依赖安装

- [ ] 核心：
  - `react@18` / `react-dom@18`
  - `react-router-dom@6`
  - `typescript@5`
- [ ] UI：
  - `antd@5`（PC 端）
  - `antd-mobile@5`（移动端）
  - `@ant-design/icons`
- [ ] 富文本：
  - `@tiptap/react`
  - `@tiptap/starter-kit`
  - `@tiptap/extension-link`
  - `@tiptap/extension-placeholder`
- [ ] 图表：
  - `echarts` + `echarts-for-react`
- [ ] HTTP：
  - `axios`
- [ ] 工具：
  - `dayjs`（日期处理）
  - `lodash-es`（工具函数）
  - `zustand`（轻量状态管理）
  - `ahooks`（React hooks 工具集）
- [ ] 开发：
  - `eslint` / `prettier`
  - `unplugin-auto-import`（自动导入）
  - `vite-plugin-imp`（按需加载）

### 1.3 Vite 配置

- [ ] 配置路径别名 `@` → `src/`
- [ ] 配置按需加载
- [ ] 配置开发代理（dev 环境下 /api 代理到 Spring Boot）
- [ ] 配置生产构建输出到 `web/dist/`
- [ ] 配置文件 `vite.config.ts`：
  ```ts
  export default defineConfig({
    plugins: [react()],
    resolve: {
      alias: { '@': '/src' }
    },
    server: {
      proxy: {
        '/admin/api': 'http://localhost:9080',
        '/wecom': 'http://localhost:9080'
      }
    },
    build: {
      outDir: 'dist',
      sourcemap: false
    }
  });
  ```

### 1.4 基础配置

- [ ] 配置 `tsconfig.json`（严格模式）
- [ ] 配置 ESLint + Prettier
- [ ] 配置 `.gitignore`（排除 `node_modules/` 和 `dist/`）
- [ ] 添加 `web/.gitignore`

---

## 阶段 2：基础设施（1 天）

### 2.1 路由系统

- [ ] 创建 `router.tsx`（使用 React Router 6）
- [ ] 路由结构：
  ```
  /login                 → 登录页
  /                      → 重定向到 /requirements
  /requirements          → 需求列表
  /requirements/:id      → 需求详情
  /requirements/new      → 新建需求
  /dashboard             → 数据看板
  /admin/audit           → 审计日志（管理员）
  /admin/settings        → 系统设置（管理员）
  ```
- [ ] 配置路由守卫（未登录跳登录）
- [ ] 配置按设备类型选择布局（PC/Mobile 布局组件）

### 2.2 API 客户端

- [ ] 创建 `src/api/client.ts`（axios 实例）
- [ ] 配置请求拦截器：
  - 自动带 cookie（`withCredentials: true`）
  - 自动带 `Content-Type: application/json`
  - 请求超时：30 秒
- [ ] 配置响应拦截器：
  - 401 → 跳转登录
  - 其他错误 → toast 提示
- [ ] 配置 baseURL：开发用 `/`，生产用域名

### 2.3 API 模块拆分

- [ ] `src/api/auth.ts` — 登录、登出、获取当前用户
- [ ] `src/api/requirements.ts` — 需求 CRUD
- [ ] `src/api/comments.ts` — 评论
- [ ] `src/api/users.ts` — 企微成员
- [ ] `src/api/stats.ts` — 数据统计

### 2.4 TypeScript 类型定义

- [ ] `src/types/demand.ts`：
  ```ts
  export interface Demand {
    id: number;
    userId: string;
    department?: string;
    source: string;
    type: string;
    title: string;
    rawInput?: string;
    businessContext?: string;
    userRole?: string;
    acceptanceCriteria?: string;
    priority?: 'P0' | 'P1' | 'P2' | 'P3';
    status: string;
    assigneeUserId?: string;
    sprintVersion?: string;
    estimatedHours?: number;
    deadline?: string;
    categoryTags?: string[];
    relatedDemandIds?: number[];
    attachments?: Attachment[];
    closedAt?: string;
    feedbackRating?: number;
    notes?: string;
    qaHistory?: QAItem[];
    comments?: Comment[];
    audit?: AuditLog[];
    createdAt: string;
    updatedAt: string;
  }
  ```
- [ ] `src/types/user.ts`：User, SessionInfo
- [ ] `src/types/api.ts`：ApiResponse<T>, PagedResult<T>

### 2.5 布局组件

- [ ] `src/components/layout/PCLayout.tsx`（PC 端后台布局）
  - Sider（左侧导航）+ Header（顶部用户信息）+ Content（页面内容）
  - 导航菜单：需求列表 / 数据看板 / 审计 / 设置
- [ ] `src/components/layout/MobileLayout.tsx`（移动端布局）
  - 顶部 Header + TabBar 底部导航
- [ ] `src/components/layout/ResponsiveLayout.tsx`
  - 根据屏幕宽度自动切换 PC/Mobile 布局
  - 断点：< 768px 移动端，≥ 768px PC 端
- [ ] `src/components/layout/AuthGuard.tsx`（鉴权守卫）

---

## 阶段 3：核心页面 — PC 端（2 天）

### 3.1 需求列表页 `/requirements`

- [ ] 页面标题：需求管理
- [ ] 工具栏（与当前 admin/index.html 类似）：
  - 搜索框（按标题/原始内容）
  - 状态下拉筛选
  - 优先级筛选
  - 类型筛选
  - 部门筛选（新增）
  - 指派人筛选
  - 来源筛选（新增）
  - Sprint 筛选（新增）
  - 查询按钮
  - 导出按钮（导出全部为 CSV）
- [ ] 需求表格（用 Ant Design Table）：
  - 列：ID / 提交人 / 部门 / 类型 / 标题 / 优先级 / 状态 / 指派人 / Sprint / 截止日期 / 创建时间
  - 排序
  - 分页（与后端 offset/size 协议一致）
  - 行点击 → 跳详情
- [ ] 空状态、数据加载状态、错误状态
- [ ] 移动端用 Card 列表（用 Ant Design Mobile 的 Card）

### 3.2 需求详情页 `/requirements/:id`

- [ ] 页面布局：左主右辅（PC 端），上下单栏（移动端）
- [ ] 主区域：
  - 标题
  - 结构化字段（类型、原始需求、背景、使用对象、验收标准）
  - 问答历史（Q&A 列表）
  - 评论列表 + 新增评论
  - 审计日志
- [ ] 侧边栏（PC 端 Drawer，固定右侧）：
  - 管理字段编辑（状态、优先级、指派人、Sprint、截止日期、预估工时等）
  - 保存按钮
  - 导出 CSV 按钮
- [ ] 富文本编辑器（TipTap）：
  - 验收标准、原始需求、备注等可编辑
  - 工具栏：加粗、斜体、列表、链接、撤销/重做
  - 移动端适配（移动端用更简洁的工具栏）
- [ ] 移动端用 Ant Design Mobile 的表单组件
- [ ] 操作按钮区：保存、导出、删除

### 3.3 新建需求页 `/requirements/new`

- [ ] 表单字段：
  - 原始需求（textarea）
  - 需求类型（选择）
  - 优先级（选择）
  - 部门（自动从当前用户获取，可改）
  - 截止日期
  - Sprint
  - 预估工时
  - 备注
- [ ] 提交按钮（保存为草稿 / 提交）
- [ ] 移动端友好的表单布局

### 3.4 数据看板页 `/dashboard`

- [ ] 顶部 KPI 卡片：
  - 总需求数
  - 今日新增
  - 进行中
  - 已完成
- [ ] 图表（用 ECharts）：
  - 按状态分布（饼图）
  - 按类型分布（饼图）
  - 按部门分布（柱状图）
  - 按优先级分布（饼图）
  - 趋势（按周/月新增需求数 - 折线图）
- [ ] 移动端：图表简化或替换为列表

### 3.5 登录页 `/login`

- [ ] 检测登录状态：调 `/admin/oauth/me`，如果 200 直接跳需求列表
- [ ] 未登录：调 `/admin/oauth/browser-helper?redirect=/`，后端会重定向到企信 OAuth
- [ ] 实际上登录页主要是二维码页（由后端 OAuthBrowserHelperController 提供）
- [ ] 前端判断如果是从企信内访问，自动调 `/admin/oauth/login`

---

## 阶段 4：核心页面 — 移动端适配（1 天）

### 4.1 设备检测

- [ ] 创建 `useDeviceType` hook
- [ ] 根据 `window.innerWidth` 切换
- [ ] 监听 `resize` 事件
- [ ] 也可以用 CSS 媒体查询（不切换组件，只切换样式）

### 4.2 移动端组件

- [ ] 用 Ant Design Mobile 组件重写关键页面
- [ ] 列表页用 `Card` 替代 Table
- [ ] 详情页用 `Tabs` 折叠不同模块
- [ ] 表单用 `Form` + `Input` 等
- [ ] 操作按钮用 `Button` + `ActionSheet`

### 4.3 移动端 UX 优化

- [ ] 触屏友好的点击区域（最小 44px × 44px）
- [ ] 表单输入避免 zoom in（iOS）
- [ ] 列表下拉刷新
- [ ] 滑动返回（可选）
- [ ] 关键操作二次确认（弹窗）

### 4.4 移动端特定场景

- [ ] **场景 1**：在企信里收到需求通知 → 点开 → 进入详情（PC 风格可能不便）
  - 移动端布局
- [ ] **场景 2**：在企信工作台点应用 → 进入管理端
  - 默认用 PC 端布局（企信内置浏览器有足够宽度）
  - 但用户可以手动切换移动端视图
- [ ] **场景 3**：手机浏览器（Safari/Chrome）扫码登录后
  - 自动用移动端布局

---

## 阶段 5：后端 API 对接（1 天）

### 5.1 后端 API 调整

- [ ] 改造 `/admin/api/requirements` 返回更标准的 RESTful 格式
- [ ] 新增 `PATCH /admin/api/requirements/{id}/status` 单独改状态
- [ ] 新增 `PATCH /admin/api/requirements/{id}/assignee` 单独改指派人
- [ ] 新增 `GET /admin/api/stats` 数据统计接口
- [ ] 新增 `GET /admin/api/stats/by-type` 按类型统计
- [ ] 新增 `GET /admin/api/stats/by-status` 按状态统计
- [ ] 新增 `GET /admin/api/stats/by-department` 按部门统计
- [ ] 新增 `GET /admin/api/stats/trend` 趋势数据
- [ ] 错误响应标准化：`{"code": 400, "message": "...", "data": null}`

### 5.2 前端联调

- [ ] 用真实 API 替换 mock 数据
- [ ] 处理 401 → 跳登录
- [ ] 处理 403 → 提示无权限
- [ ] 处理 500 → 全局错误提示
- [ ] Loading 状态管理
- [ ] 错误重试机制（关键操作）

### 5.3 鉴权

- [ ] 前端调 `/admin/oauth/me` 检查登录状态
- [ ] 401 响应自动跳登录
- [ ] 登录回调处理（从 URL 中读 code，调后端换 cookie）

---

## 阶段 6：测试 & 优化（0.5 天）

### 6.1 单元测试

- [ ] 关键组件：表格、表单、详情页
- [ ] 工具函数：日期格式化、CSV 导出
- [ ] API 客户端：拦截器
- [ ] 使用 Vitest + React Testing Library

### 6.2 E2E 测试（可选）

- [ ] 用 Playwright 写关键流程：
  - 登录 → 列表 → 详情 → 改字段
  - 新建需求
  - 加评论
- [ ] 跑 PC + 移动端两种视口

### 6.3 性能优化

- [ ] 路由懒加载（`React.lazy`）
- [ ] 组件按需引入
- [ ] 列表虚拟滚动（如需求数 > 100）
- [ ] 防抖/节流（搜索框、滚动）
- [ ] 图片懒加载（如有附件）
- [ ] bundle size 报告（`vite-plugin-bundle-analyzer`）

### 6.4 兼容性

- [ ] 企信内嵌浏览器测试
- [ ] Chrome / Safari / Edge 测试
- [ ] 移动端浏览器测试
- [ ] iPad 适配（断点 768-1024）

---

## 阶段 7：Nginx 部署（0.5 天）

### 7.1 Nginx 配置

- [ ] 创建 `web/deploy/nginx.conf`（参考配置）：
  ```nginx
  server {
    listen 443 ssl;
    server_name wecom.zhang-yong.site;
    # SSL 证书
    
    # 前端静态文件
    location / {
      root /opt/ai-req/web/dist;
      try_files $uri $uri/ /index.html;
    }
    
    # 后端 API 反代
    location /admin/api/ {
      proxy_pass http://127.0.0.1:9080;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    # 企信回调
    location /wecom/ {
      proxy_pass http://127.0.0.1:9080;
      ...
    }
  }
  ```
- [ ] 配置 SSL 证书（Cloudflare Origin 或 Let's Encrypt）
- [ ] 配置 gzip 压缩
- [ ] 配置静态资源缓存

### 7.2 构建脚本

- [ ] `web/package.json` 添加 `build` 脚本
- [ ] 构建后产物：`web/dist/`
- [ ] 部署到 ECS（与 OpenResty 共存或独立）

### 7.3 后端配合调整

- [ ] Spring Boot CORS 配置：允许前端域名
- [ ] 静态资源路径调整：不再服务 `admin/index.html`（由 Nginx 提供）

---

## 阶段 8：联调 & 上线（0.5 天）

### 8.1 联调

- [ ] 后端 CORS 配置验证
- [ ] 前端 API 调用链路验证
- [ ] 企信内嵌浏览器实测
- [ ] 移动浏览器实测
- [ ] 网络异常场景（断网、超时、500）

### 8.2 上线检查清单

- [ ] 生产构建无 warning
- [ ] 前端 bundle 体积 < 500KB（gzip 后）
- [ ] 关键页面首屏 < 2 秒
- [ ] 所有外部资源 HTTPS
- [ ] API 错误都有用户提示
- [ ] 移动端没有横向滚动
- [ ] 打印 console 无错误

### 8.3 回滚方案

- [ ] 保留旧版 `admin/index.html` 作为 fallback
- [ ] 保留原 Spring Boot 静态资源服务
- [ ] Nginx 配置可一键切回

---

## 9. 风险与注意

| 风险 | 缓解 |
|---|---|
| 前后端联调出问题 | 阶段 5 单独安排时间联调 |
| 企信内置浏览器兼容 | 阶段 8 重点测试 |
| Ant Design 在企信内核下样式异常 | 走 `babel-plugin-import` 按需引入 |
| 移动端富文本编辑体验差 | 移动端可只读 + PC 端可编辑 |
| 旧版 `admin/index.html` 依赖方 | 保持一段时间，作为 fallback |

---

## 10. 验收标准

### 功能性
- [ ] PC 端：现有管理端所有功能 100% 复刻
- [ ] 移动端：列表、详情、编辑、评论全部能用
- [ ] 登录：企信内 + 外部浏览器扫码两种方式正常
- [ ] 性能：1000 条需求列表首屏 < 2 秒

### 兼容性
- [ ] 企信 PC 版内嵌浏览器 ✅
- [ ] 企信 iOS 内嵌浏览器 ✅
- [ ] 企信 Android 内嵌浏览器 ✅
- [ ] Chrome / Safari / Edge（PC + 移动）✅
- [ ] iPad 平板（768-1024 断点）✅

### 工程质量
- [ ] TypeScript 严格模式
- [ ] ESLint + Prettier 0 warning
- [ ] 单元测试覆盖率 > 50%
- [ ] 生产构建无 error

### 部署
- [ ] Nginx 部署配置完成
- [ ] HTTPS 正常工作
- [ ] 与后端 API 联调通过
- [ ] 旧版可回滚

---

## 11. 时间线

```
第 1 天   阶段 1 + 阶段 2（工程初始化 + 基础设施）
第 2-3 天 阶段 3（PC 端核心页面）
第 4 天   阶段 4（移动端适配）
第 5 天   阶段 5（后端 API 对接）
第 6 天   阶段 6（测试 + 优化）
第 7 天   阶段 7（Nginx 部署）
第 8 天   阶段 8（联调 + 上线）
```

---

## 12. 后续 P1 衔接

- [ ] 状态机引擎接入
- [ ] 群内审批卡片
- [ ] Meilisearch 全文搜索
- [ ] 通知体系
- [ ] P2 富文本深度应用（TipTap 完整扩展）
- [ ] P2 附件上传（对接 MinIO）
- [ ] P2 变更管理（内容指纹、版本对比）

---

*最后更新：2026-07-22*
*预计总工作量：6-8 天*
