# P0 TODO 清单

> MVP → 产品化基础，1-2 周完成
> 任务关联：详见 `ROADMAP.md` 第二节

---

## 🔴 优先级最高

### [ ] 1. 抽取公共 LlmService（2 小时）

**目标**：消除 3 个 Agent 中的重复代码

**当前问题**：
- `ClassifierAgent.callLlm()`
- `QuestionerAgent.callLlm()`
- `StructurerAgent.callLlm()`
- 三处几乎一样的 `RestTemplate` + `HttpHeaders` + JSON 解析

**实现**：
```java
@Service
public class LlmService {
    String chat(String systemPrompt, String userPrompt);
    String chat(String systemPrompt, String userPrompt, int maxTokens);
    <T> T chatJson(String system, String user, Class<T> clazz);
}
```

**验收**：
- [ ] 新建 `LlmService` 类
- [ ] 3 个 Agent 改用 `LlmService`
- [ ] 删除重复的 `RestTemplate` 字段
- [ ] 现有功能不变，所有测试通过
- [ ] LLM 调用统一加上 30 秒超时

**涉及文件**：
- 新增：`src/main/java/com/zhangyong/agent/llm/LlmService.java`
- 改：`ClassifierAgent.java` / `QuestionerAgent.java` / `StructurerAgent.java`

---

### [ ] 2. 安全加固：Key 全部环境变量化（30 分钟）

**目标**：消除明文 Key 风险

**当前问题**：
- `application.yml` 包含明文 Secret、API Key
- 已提交到 git（虽然有 `.gitignore`，但 `application.yml.example` 默认值是真实值）

**实现**：
- [ ] `application.yml` 移除所有硬编码默认值
- [ ] `application.yml.example` 默认值改为 `your_xxx` 占位符
- [ ] 强制要求环境变量注入，否则启动失败
- [ ] yml 文件权限 600

**验收**：
- [ ] 没有环境变量时启动报错并提示
- [ ] git diff 确认无敏感信息
- [ ] 历史提交中的敏感信息评估（考虑清理）

**涉及文件**：
- `application.yml`
- `application.yml.example`

---

### [ ] 3. 前端重写为 React/Vue（2-3 天）

**目标**：从单文件 HTML 升级为现代前端框架

**当前问题**：
- 625 行单文件 HTML，JS/CSS/HTML 全耦合
- 后续富文本编辑器、状态机面板、审批卡片无法承载

**技术选型**：
- **推荐**：React 18 + TypeScript + Vite
- **备选**：Vue 3 + Vite

**实现**：
- [ ] 初始化前端项目（`web/` 目录）
- [ ] 路由：需求列表 / 需求详情 / 登录
- [ ] 组件拆分：Table / Card / Drawer / Form
- [ ] API 客户端封装（与后端 /admin/api 对接）
- [ ] 保持当前响应式设计（PC 表格 + 移动端卡片）
- [ ] 引入 TipTap（为 P1 状态机和 P2 富文本铺路）

**验收**：
- [ ] PC 浏览器和移动浏览器均能正常访问
- [ ] 现有管理端功能 100% 复刻
- [ ] OAuth 扫码登录流程正常
- [ ] `mvn package` 不影响前端构建

**涉及文件**：
- 新增：`web/`（整个前端项目）
- 改：`AdminApiController.java`（如果需要兼容 CORS）

**备注**：
- QM 项目的前端也是 React，保持一致
- 现阶段可以暂时把 admin SPA 留在原位置，渐进迁移

---

## 🟡 P0 必做

### [ ] 4. 容器化：应用服务 Docker 化（半天）

**目标**：Spring Boot 应用容器化部署

**当前问题**：
- MySQL / Redis 已容器化
- Spring Boot 通过 `nohup java -jar` 裸机运行

**实现**：
- [ ] 写 `Dockerfile`（多阶段构建：Maven 编译 → JRE 运行环境）
- [ ] 完善 `docker-compose.yml`，加入 `app` 服务
- [ ] 配置 health check
- [ ] 环境变量从 `.env` 注入

**验收**：
- [ ] `docker compose up -d` 一键启动全栈
- [ ] 不再依赖 mac 本地环境
- [ ] 镜像大小合理（< 300MB）

**涉及文件**：
- 新增：`Dockerfile`、`.dockerignore`
- 改：`docker-compose.yml`

---

### [ ] 5. LLM 调用超时 + 重试机制（半天）

**目标**：网络异常时优雅降级

**当前问题**：
- `RestTemplate` 默认无超时，可能永久挂死
- 任何网络抖动都导致用户收不到回复

**实现**：
- [ ] `LlmService` 内部用带超时的 `HttpClient`
- [ ] 超时设置：30 秒
- [ ] 失败重试：3 次指数退避（1s → 2s → 4s）
- [ ] 重试仅对网络异常生效，不对业务错误重试
- [ ] 关键路径加日志（重试次数、最终结果）

**验收**：
- [ ] 模拟网络断开，3 次后返回 fallback
- [ ] 正常调用无感知变化
- [ ] 单元测试覆盖重试逻辑

**涉及文件**：
- 改：`LlmService.java`（从任务 1 抽取出来的）

---

## ✅ 完成标准（P0 验收）

### 功能性
- [ ] 所有 P0 任务完成
- [ ] 现有 30 个测试全部通过
- [ ] LLM 调用无超时挂死
- [ ] OAuth 登录流程在企信内外均正常
- [ ] 管理端 UI 完整迁移到新前端

### 安全性
- [ ] 仓库中无明文密钥
- [ ] 启动时强制要求环境变量
- [ ] LLM Key 撤销并重新生成

### 部署
- [ ] `docker compose up` 一键启动
- [ ] 不依赖 mac 本地环境
- [ ] 健康检查通过

### 文档
- [ ] 文档全部在 `docs/` 目录
- [ ] 部署文档更新
- [ ] 环境变量清单完整

---

## 📊 工作量与时间估算

| 任务 | 工作量 | 累计 |
|---|---|---|
| 1. 抽 LlmService | 2h | 2h |
| 2. 安全加固 | 0.5h | 2.5h |
| 3. 前端重写 | 2-3d | 2.5d ~ 3.5d |
| 4. 容器化 | 0.5d | 3d ~ 4d |
| 5. LLM 超时重试 | 0.5d | 3.5d ~ 4.5d |
| **合计** | | **~1 周** |

---

## 🎯 推荐执行顺序

```
1. LlmService (2h)            ← 改动最小，收益最大
   ↓
2. 安全加固 (0.5h)            ← 一并清理
   ↓
3. LLM 超时重试 (0.5d)        ← 在 LlmService 框架内做
   ↓
4. 前端重写 (2-3d)            ← 投入最大
   ↓
5. 容器化 (0.5d)              ← 收尾
```

---

*最后更新：2026-07-22*
