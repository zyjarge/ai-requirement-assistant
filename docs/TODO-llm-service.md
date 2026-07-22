# Task 1: 抽取 LlmService — 子计划

> 父任务：P0.1 抽取公共 LlmService
> 预计工作量：2 小时
> 涉及文件：1 个新增 + 3 个修改

---

## 🎯 目标

消除 3 个 Agent 中的 LLM 调用重复代码，统一管理 LLM 调用逻辑。

**当前问题**：
```java
// ClassifierAgent.java
private String callLlm(String systemPrompt, String userPrompt) throws Exception {
    String url = llmProperties.getBaseUrl() + llmProperties.getEndpointPath();
    Map<String, Object> body = new HashMap<>();
    body.put("model", llmProperties.getModelName());
    body.put("max_tokens", 200);
    body.put("system", systemPrompt);
    body.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-api-key", llmProperties.getApiKey());
    headers.set("anthropic-version", "2023-06-01");
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
    ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
    JsonNode root = objectMapper.readTree(response.getBody());
    return root.path("content").get(0).path("text").asText();
}
```

**重构后**：
```java
// ClassifierAgent.java
private static final String SYSTEM_PROMPT = "...";

public Map<String, Object> classify(String userInput) {
    return llmService.chatJson(SYSTEM_PROMPT, userInput, 200, Map.class);
}
```

---

## 📋 子 TODO

### 阶段 A：创建 LlmService 骨架（30 分钟）

#### A.1 创建类文件
- [ ] 新建 `src/main/java/com/zhangyong/agent/llm/LlmService.java`
- [ ] 包名：`com.zhangyong.agent.llm`（新建 `llm` 子包）
- [ ] 标注 `@Service` 交给 Spring 管理
- [ ] 注入 `LlmProperties` 和 `ObjectMapper`

#### A.2 实现核心方法
- [ ] 私有方法 `private String doChat(String system, String user, int maxTokens)` — 实际 HTTP 调用
- [ ] 公开方法 `public String chat(String system, String user, int maxTokens)` — 返回文本
- [ ] 公开方法 `public String chat(String system, String user)` — 默认 max_tokens=1000
- [ ] 公开方法 `public <T> T chatJson(String system, String user, int maxTokens, Class<T> clazz)` — 返回解析后的对象

#### A.3 关键参数
- [ ] 复用 `LlmProperties`：`baseUrl` / `endpointPath` / `modelName` / `apiKey`
- [ ] 请求头：`x-api-key` / `anthropic-version: 2023-06-01` / `Content-Type: application/json`
- [ ] 请求体字段：`model` / `max_tokens` / `system` / `messages`
- [ ] 响应解析路径：`content[0].text`

---

### 阶段 B：替换 Agent 调用（60 分钟）

#### B.1 ClassifierAgent
- [ ] 删除 `RestTemplate restTemplate = new RestTemplate()` 字段
- [ ] 删除 `private String callLlm(...)` 方法
- [ ] 删除内嵌的 HTTP 拼装代码
- [ ] 注入 `LlmService`（用 `@RequiredArgsConstructor`）
- [ ] 改 `callLlm` 调用为 `llmService.chatJson(SYSTEM_PROMPT, userInput, 200, new TypeReference<...>(){})`
- [ ] 保持 `parseJson` 逻辑（如果业务需要）

#### B.2 QuestionerAgent
- [ ] 同 B.1 改造 `generateSingleQuestion()` 方法
- [ ] 保留 `parseQuestion` 解析逻辑
- [ ] max_tokens 用 200

#### B.3 StructurerAgent
- [ ] 同 B.1 改造 `structure()` 方法
- [ ] 保留 `parseJson` 解析逻辑
- [ ] max_tokens 用 500

---

### 阶段 C：验证（30 分钟）

#### C.1 编译验证
- [ ] `mvn compile` 无错误
- [ ] 没有未使用 import

#### C.2 测试验证
- [ ] `mvn test` 全部 30 个测试通过
- [ ] 现有 `WeComMessageProcessorTest` 等集成测试不变

#### C.3 代码质量检查
- [ ] 3 个 Agent 类的代码量明显减少
- [ ] 没有重复的 `HttpHeaders` / `Map body` 拼装代码
- [ ] 异常处理在 LlmService 内部统一

---

## 📁 涉及文件

### 新增（1 个）

```
src/main/java/com/zhangyong/agent/llm/
└── LlmService.java          ← 约 80-100 行
```

### 修改（3 个）

```
src/main/java/com/zhangyong/agent/agent/
├── ClassifierAgent.java      ← 减少约 30 行
├── QuestionerAgent.java      ← 减少约 30 行
└── StructurerAgent.java      ← 减少约 30 行
```

---

## 🔧 LlmService 草稿代码

```java
package com.zhangyong.agent.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangyong.agent.config.LlmProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 统一 LLM 调用服务
 * 所有 Agent 通过此类调用 LLM，避免代码重复
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    /** 默认 max_tokens */
    private static final int DEFAULT_MAX_TOKENS = 1000;

    /**
     * 调用 LLM，返回文本
     */
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, DEFAULT_MAX_TOKENS);
    }

    /**
     * 调用 LLM，指定 max_tokens，返回文本
     */
    public String chat(String systemPrompt, String userPrompt, int maxTokens) {
        try {
            return doChat(systemPrompt, userPrompt, maxTokens);
        } catch (Exception e) {
            log.error("LLM 调用失败", e);
            throw new RuntimeException("LLM 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用 LLM，解析为指定类型
     */
    public <T> T chatJson(String systemPrompt, String userPrompt, Class<T> clazz) {
        String text = chat(systemPrompt, userPrompt);
        try {
            return objectMapper.readValue(extractJson(text), clazz);
        } catch (Exception e) {
            log.error("LLM 响应 JSON 解析失败: {}", text);
            throw new RuntimeException("LLM 响应 JSON 解析失败", e);
        }
    }

    /**
     * 调用 LLM，解析为泛型类型
     */
    public <T> T chatJson(String systemPrompt, String userPrompt, int maxTokens, TypeReference<T> typeRef) {
        String text = chat(systemPrompt, userPrompt, maxTokens);
        try {
            return objectMapper.readValue(extractJson(text), typeRef);
        } catch (Exception e) {
            log.error("LLM 响应 JSON 解析失败: {}", text);
            throw new RuntimeException("LLM 响应 JSON 解析失败", e);
        }
    }

    /**
     * 实际 HTTP 调用
     */
    private String doChat(String systemPrompt, String userPrompt, int maxTokens) throws Exception {
        String url = llmProperties.getBaseUrl() + llmProperties.getEndpointPath();

        Map<String, Object> body = Map.of(
            "model", llmProperties.getModelName(),
            "max_tokens", maxTokens,
            "system", systemPrompt,
            "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", llmProperties.getApiKey());
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("content").get(0).path("text").asText();
    }

    /**
     * 从 LLM 响应中提取 JSON 字符串（处理可能包含非 JSON 内容的情况）
     */
    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
    }
}
```

---

## ✅ 验收清单

### 必须达成
- [ ] 编译通过
- [ ] 30 个测试全部通过
- [ ] 3 个 Agent 各自删除了至少 25 行重复代码
- [ ] Agent 类只保留业务逻辑（系统提示词、解析、fallback）
- [ ] HTTP 调用、LlmService 负责，不再有 RestTemplate 字段

### 期望达成
- [ ] 错误处理统一（Agent 不再各自 catch 异常）
- [ ] 后续 P0.5 任务（超时+重试）只需在 LlmService 中加
- [ ] 后续接新 LLM（如 OpenAI、Gemini）只需在 LlmService 改

---

## ⚠️ 风险与注意

| 风险 | 缓解 |
|---|---|
| 调用方依赖具体方法签名 | 先跑通所有测试再合并 |
| JSON 解析行为变化 | 保持 `extractJson` 行为兼容 |
| 异常类型变化 | 仍抛 RuntimeException，Agent 现有 catch 兼容 |

---

*最后更新：2026-07-22*
