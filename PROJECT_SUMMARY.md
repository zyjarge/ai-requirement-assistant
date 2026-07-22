# AI 需求助理 - 项目完整记录

> 最后更新：2026-07-20
> 状态：MVP 端到端链路通，Spring Boot 处理完整，企业微信客户端显示仍需 IP 白名单配置

---

## 1. 项目背景

### 1.1 业务痛点

企业在业务开发中，业务人员提需求时往往只说一句话（如"我要做一个销售月报"），导致：
- 需求描述模糊，研发理解成本高
- 反复沟通确认，效率低
- 关键信息缺失（使用对象、验收标准、边界条件）
- 需求分散在微信群里，无法追溯

### 1.2 解决方案

**AI 需求助理** —— 在企业微信中提供 AI 助手：
- 业务人员发一句话需求
- AI 自动分类（5 种类型）
- AI 逐轮追问关键信息
- 生成结构化需求卡
- 归档到数据库供研发查看

### 1.3 MVP 目标

- ✅ 2 人试点使用
- ✅ 跑通"一句话需求 → AI 追问 → 结构化需求卡 → 归档"完整流程
- ✅ 在企业微信内一站式完成
- ✅ 0-1 验证业务价值

---

## 2. 方案设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│  企业微信（业务人员侧）                                    │
└─────────────────────────┬───────────────────────────────┘
                          │ HTTPS POST 回调
                          ↓
┌─────────────────────────────────────────────────────────┐
│  公网层                                                    │
│  Cloudflare DNS（zhang-yong.site）                          │
│  ↓                                                         │
│  阿里云 ECS（101.200.149.147）                              │
│  ├─ OpenResty（443 HTTPS）                                  │
│  └─ frps（8086 → WECOM 隧道）                              │
└─────────────────────────┬───────────────────────────────┘
                          │ frp 加密隧道
                          ↓
┌─────────────────────────────────────────────────────────┐
│  iStoreOS 软路由（192.168.3.4）                             │
│  └─ frpc 客户端                                              │
└─────────────────────────┬───────────────────────────────┘
                          │ 内网穿透
                          ↓
┌─────────────────────────────────────────────────────────┐
│  mac 工作站（192.168.3.105:9080）                          │
│  Spring Boot 3.4 + AgentScope Java v2.0 GA                │
│  ├─ WeComCallbackController（接收回调）                    │
│  ├─ DemandPipelineService（业务编排）                      │
│  ├─ ClassifierAgent / QuestionerAgent / StructurerAgent   │
│  ├─ WeComCryptoUtil（SHA1 + AES 加解密）                   │
│  └─ WeComApiClient（主动消息 API）                          │
└─────────────────────────┬───────────────────────────────┘
                          │ 调用 LLM
                          ↓
┌─────────────────────────────────────────────────────────┐
│  MiniMax API（api.minimaxi.com）                            │
│  Anthropic Messages API 兼容模式                            │
│  Model: MiniMax-M3                                         │
└─────────────────────────────────────────────────────────┘

同时：
- MySQL 8（Docker 容器，存需求数据）
- Redis 7（Docker 容器，存会话状态）
```

### 2.2 技术选型

| 层级 | 技术 | 选型理由 |
|---|---|---|
| 后端框架 | Spring Boot 3.4 | Java 生态成熟，团队熟悉 |
| AI 框架 | 直接调 MiniMax API | AgentScope Java v2.0 抽象度太高，REST 调用更直接可控 |
| 持久化 | MySQL 8 + JPA | 结构化需求数据，事务支持 |
| 会话存储 | Redis 7 | 高性能，自动过期 |
| 部署 | Docker Compose | 单机 MVP 简化部署 |
| 链路穿透 | FRP | 已有 iStoreOS frpc + 阿里云 frps |
| HTTPS | Cloudflare + OpenResty | 已有 Cloudflare Origin 证书复用 |

### 2.3 关键设计决策

1. **企业微信集成方式**：自建应用（不是智能机器人）
   - 原因：可固化入口、纳入工作台、与内部工作流整合

2. **入口链路**：公网 HTTPS → 阿里云 → FRP → mac
   - 原因：用户本地有 mac + 软路由 + 阿里云，复用现有 FRP 基础设施

3. **LLM 调用方式**：Anthropic Messages API（不是 OpenAI 兼容）
   - 原因：MiniMax 官方支持，且支持更高级特性（thinking blocks 等）

4. **追问逻辑**：一次只问 1 个问题（不是一次性问 3 个）
   - 原因：用户已明确反馈一次性问 3 个体验差

5. **响应模式**：5 秒内立即返回 + 异步处理 + 主动消息推送
   - 原因：企业微信回调 5 秒超时，必须异步；Spring Boot 不主动发消息用户看不到

---

## 3. 实施过程

### 3.1 时间线（2026-07-20 单日完成 MVP）

| 时段 | 完成项 |
|---|---|
| 上午 1 | 需求分析 + 方案设计（与企业微信集成链路） |
| 上午 2 | 链路搭建：FRP 隧道、OpenResty 反代、Cloudflare DNS |
| 上午 3 | Python 临时验证服务跑通 URL 验证 |
| 上午 4 | Spring Boot 项目脚手架 + pom.xml + application.yml |
| 上午 5 | 16 个 Java 类实现（Controller、Agent、Pipeline、Entity） |
| 下午 1 | mvn package 成功 + Spring Boot 启动 + GET URL 验证端到端跑通 |
| 下午 2 | 修复 Lombok 字段缺失 + 修复 BadPaddingException（手动 PKCS7 unpad） |
| 下午 3 | 修复 Jackson 反序列化（@JsonIgnoreProperties） |
| 下午 4 | 修复 Pipeline 逻辑（一次只问 1 个问题） |
| 下午 5 | 修复 handleMessage 同步阻塞（立即返回 + @Async 异步 + 主动发消息） |
| 下午 6 | 诊断发现企业微信 errcode 60020（IP 白名单问题）|

### 3.2 关键里程碑

| 里程碑 | 验证证据 |
|---|---|
| 公网 HTTPS 端到端 | `https://wecom.zhang-yong.site/wecom/callback` 可访问 |
| Spring Boot 启动 | Started AgentApplication in 25-35 seconds |
| GET URL 验证 | 加密字符串能被解密并原样返回 |
| POST 业务消息 | 多次 LLM 调用 + 数据库归档 |
| MySQL 入库 | `SELECT COUNT(*) FROM demand` = 3 条 |
| Redis 会话 | `GET demand:session:ZhangYong` 返回完整 JSON |

---

## 4. 完整链路搭建

### 4.1 DNS + HTTPS 链路

```
域名：wecom.zhang-yong.site（Cloudflare DNS）
   ↓
阿里云 ECS 101.200.149.147:443（HTTPS）
   ↓
OpenResty 容器（1Panel-openresty-byxY）
   ↓
复用 nb.zhang-yong.site 的 Cloudflare Origin 证书
```

### 4.2 FRP 隧道链路

```
iStoreOS 软路由（192.168.3.4）
   ↓
frpc 客户端 → 阿里云 frps（101.200.149.147:5443）
   ↓
新增隧道 [WECOM]：101.200.149.147:8086 → 192.168.3.105:9080
```

### 4.3 Spring Boot 应用

```
位置：/Users/zhangyong/ai-requirement-assistant/
构建：mvn package（成功，~10 秒）
启动：nohup java -jar target/ai-requirement-assistant.jar
监听：0.0.0.0:9080
健康：curl http://localhost:9080/wecom/health → {"status":"UP"}
```

### 4.4 数据存储

```
MySQL 容器：ai-req-mysql
  - 端口 3306
  - 数据库 ai_req
  - 表 demand（自动创建）
  - 已有 3 条需求记录

Redis 容器：ai-req-redis
  - 端口 6379
  - key: demand:session:{userId}
  - TTL 30 分钟
```

---

## 5. 遇到的问题与解决（11 个关键 bug）

### 问题 1：FRP 端口冲突
- **症状**：WECOM 隧道报 connection refused
- **根因**：iStoreOS frpc 配置的 `[WECOM]` 段端口 8086 已被 QM 隧道占用
- **解决**：在阿里云 ECS 监听 8086 端口 + iStoreOS frpc 配 `[WECOM] remote_port=8086`

### 问题 2：企业微信 URL 验证失败
- **症状**：openapi 回调地址请求不通过
- **根因**：本地没有运行任何服务
- **解决**：写 Python 临时验证服务（35 行）+ Spring Boot 重写

### 问题 3：Maven 依赖下载失败（MySQL 认证）
- **症状**：Public Key Retrieval is not allowed
- **根因**：MySQL 8.0 默认 caching_sha2_password
- **解决**：JDBC URL 加 `allowPublicKeyRetrieval=true`

### 问题 4：AgentScope Java API 不可用
- **症状**：chatModel.call() 找不到方法
- **根因**：v2.0 GA 文档与实际 API 有差异
- **解决**：移除 AgentScope Starter，直接 RestTemplate 调 LLM

### 问题 5：Lombok 字段缺失
- **症状**：properties cannot be resolved
- **根因**：重写 WeComCryptoUtil.java 时漏写 `private final WeComProperties properties`
- **解决**：补上 final 字段声明

### 问题 6：BadPaddingException
- **症状**：AES 解密时 padding 校验失败
- **根因**：Java 的 `Cipher.getInstance("AES/CBC/PKCS5Padding")` 对中文 UTF-8 密文边界处理有 bug
- **解决**：改用 `NoPadding` + 手动 PKCS#7 unpad

### 问题 7：Jackson 反序列化失败
- **症状**：Unrecognized field "questioning" (class DemandSession)
- **根因**：`isQuestioning()` getter 方法被 Jackson 当字段
- **解决**：加 `@JsonIgnoreProperties(ignoreUnknown = true)` 和 `@JsonIgnore`

### 问题 8：Pipeline 一次性问 3 个问题
- **症状**：用户体验差，不符合自然对话
- **根因**：需求初次实现时直接列出所有问题
- **解决**：QuestionerAgent 改 prompt 一次只问 1 个 + Pipeline 启动时一次生成所有问题列表，逐轮推进

### 问题 9：Spring Boot 同步阻塞
- **症状**：企业微信 5 秒超时，Spring Boot 5-10 秒才返回
- **根因**：handleMessage 同步等所有 LLM 调用完成
- **解决**：立即返回 "success" + @Async 异步处理 + 主动消息 API

### 问题 10：环境变量覆盖 yml 配置
- **症状**：CorpID 一直是占位符 `ww_dummy_corp_id`
- **根因**：我每次重启时 `export WECOM_CORP_ID=ww_dummy_corp_id` 覆盖了 yml 默认值
- **解决**：重启前 `unset WECOM_CORP_ID`，让 yml 默认值生效

### 问题 11：企业微信 IP 白名单限制
- **症状**：errcode 60020 "not allow to access from your ip, from ip: 123.112.50.120"
- **根因**：Spring Boot 在 mac 上（公网 IP 123.112.50.120），不在企业可信 IP 白名单
- **状态**：🔴 **未解决**（需要在企业微信后台添加 IP 或部署阿里云代理）

---

## 6. 当前状态

### 6.1 已完成（链路层 + 应用层）

| 组件 | 状态 | 验证方式 |
|---|---|---|
| 公网 HTTPS | ✅ | Cloudflare DNS + OpenResty 443 |
| FRP 隧道 | ✅ | iStoreOS frpc + 阿里云 frps |
| Spring Boot 启动 | ✅ | 25-35 秒启动完成 |
| GET URL 验证 | ✅ | 加密字符串解密返回正确 |
| POST 业务消息接收 | ✅ | 多轮追问日志完整 |
| AES 加解密 | ✅ | 手动 PKCS7 unpad 修复 |
| Jackson 反序列化 | ✅ | @JsonIgnoreProperties 修复 |
| LLM 集成 | ✅ | MiniMax M3 通过 Anthropic API 调用 |
| MySQL 归档 | ✅ | 已有 3 条需求记录 |
| Redis 会话 | ✅ | 会话状态正确保存 |
| Pipeline 逻辑 | ✅ | 一次只问 1 个问题 |

### 6.2 未完成 / 阻塞中

| 组件 | 状态 | 影响 |
|---|---|---|
| **企业微信 IP 白名单** | 🔴 未配置 | 用户收不到 AI 回复（errcode 60020） |
| **应用可见范围** | ⚠️ 未确认 | 用户可能看不到应用 |
| **LLM Key 安全性** | ⚠️ 明文泄露 | 之前在聊天里明文发了 Coding Plan Key |
| **生产化部署** | ❌ 未做 | 没有 Dockerfile、systemd、监控告警 |
| **企业可信 IP** | ❌ 未配置 | 应用主动发消息需要 |

### 6.3 当前 Spring Boot 日志状态

```
✅ 收到企业微信消息                       [POST] 收到企业微信消息
✅ 签名验证 + AES 解密                   extractEncrypt 成功
✅ Pipeline 处理                          [Pipeline] 用户回答: round=N
✅ 异步处理完成                          [Async] 处理完成
✅ access_token 获取                     access_token 刷新成功
❌ 主动发消息 API 调用                   errcode 60020 (IP 被拒)
```

---

## 7. 立即要做的（解锁 MVP）

### 7.1 企业微信后台（2 分钟）

1. **我的企业 → 安全与管理 → 网络配置**
2. **企业可信 IP** → 添加：
   - `101.200.149.147`（阿里云 ECS IP），或
   - `123.112.50.120`（mac 当前公网 IP）

**推荐**：两个都加，避免 mac IP 变化时失联。

### 7.2 应用可见范围（1 分钟）

1. **应用管理 → 自建 → AI 需求助理**
2. **可见范围** → 修改
3. 搜索并勾选 ZhangYong + 另一个试点员工
4. 保存

### 7.3 重新登录企业微信

修改可见范围后，业务人员必须**重新登录**企业微信才能看到应用。

---

## 8. 中期行动

### 8.1 修复 mac 公网 IP 不稳定问题（30 分钟）

方案：让 Spring Boot 通过 FRP/阿里云代理发消息

```
mac Spring Boot
   ↓ HTTPS
阿里云 ECS（101.200.149.147）
   ↓ Python 代理服务（监听 9081）
   ↓
企业微信 API（看到 IP 101.200.149.147）
```

### 8.2 安全改进（5 分钟）

- 撤销已泄露的 MiniMax Coding Plan Key
- 重新生成新 Key，用环境变量注入
- 修改 yml 权限为 600

### 8.3 可靠性改进（1 天）

- LLM 调用超时控制（30 秒）
- LLM 调用重试机制（3 次）
- 会话清理定时任务
- 死循环保护
- 完整的错误处理和 fallback

---

## 9. 长期行动（生产化）

### 9.1 容器化部署（4 小时）

- 写 Dockerfile
- 完善 docker-compose.yml
- 添加健康检查
- 部署到阿里云 ECS

### 9.2 监控告警（4 小时）

- Spring Boot Actuator
- Micrometer + Prometheus
- Grafana 仪表盘
- 告警规则（异常率、响应时间）

### 9.3 CI/CD（1 天）

- GitHub Actions
- 自动化测试
- 自动部署

### 9.4 安全审计（1 天）

- 撤销所有泄露的 Key
- Key 轮换策略
- 访问日志审计

---

## 10. 关键文件清单

```
/Users/zhangyong/ai-requirement-assistant/
├── pom.xml                           Spring Boot 3 + 16 个依赖
├── docker-compose.yml                MySQL + Redis（已直接用 docker run 启动）
├── docs/
│   └── PROJECT_SUMMARY.md            本文档
├── src/main/resources/
│   └── application.yml                含真实 CorpID / Secret / Token / AES Key
└── src/main/java/com/zhangyong/agent/
    ├── AgentApplication.java          入口
    ├── channel/
    │   ├── WeComCryptoUtil.java       SHA1 + AES 手动 PKCS7 unpad
    │   ├── WeComMessage.java          XML 解析
    │   └── WeComCallbackController.java  GET 验证 + POST 异步处理 + 主动发消息
    ├── config/
    │   ├── WeComProperties.java       企业微信配置
    │   └── LlmProperties.java          LLM 配置
    ├── agent/
    │   ├── ClassifierAgent.java       需求分类
    │   ├── QuestionerAgent.java       追问生成（一次 1 个）
    │   └── StructurerAgent.java       结构化输出
    ├── workflow/
    │   ├── DemandSession.java         会话状态
    │   └── DemandPipelineService.java  业务编排
    ├── storage/
    │   ├── Demand.java                JPA Entity
    │   └── DemandRepository.java      JPA Repository
    └── tool/
        ├── AccessTokenManager.java    access_token 缓存
        └── WeComApiClient.java        主动消息发送
```

---

## 11. 联系与协作

- **项目目录**：`/Users/zhangyong/ai-requirement-assistant/`
- **本机 IP**：192.168.3.105（mac）
- **iStoreOS IP**：192.168.3.4
- **阿里云 ECS**：101.200.149.147（公网）/ 6226（SSH）
- **企业微信域名**：wecom.zhang-yong.site
- **企业 CorpID**：ww0865b2d53c86a0c8
- **企业 AgentID**：1000003
- **MiniMax 模型**：MiniMax-M3（通过 Coding Plan）

---

## 12. 给下一个接手的人

如果你（或别人）将来需要继续这个项目：
1. 看这份文档第 1-4 节了解背景和架构
2. 看第 5 节了解已经踩过的坑（避免重复）
3. 看第 6 节了解当前阻塞点
4. 看第 7 节立即解锁 MVP（加 IP 白名单）
5. 看第 8-9 节做生产化

**关键警告**：
- mac 公网 IP `123.112.50.120` 是动态的
- MiniMax Coding Plan Key 已在聊天明文泄露（建议立即撤销）
- 阿里云 SSH 偶尔会断（Connection closed），需要 iStoreOS 跳板

