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
     * 调用 LLM，解析为指定 Class
     */
    public <T> T chatJson(String systemPrompt, String userPrompt, int maxTokens, Class<T> clazz) {
        String text = chat(systemPrompt, userPrompt, maxTokens);
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
