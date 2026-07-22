package com.zhangyong.agent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangyong.agent.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 需求分类 Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassifierAgent {

    private static final String SYSTEM_PROMPT = """
        你是需求分类专家。根据用户输入的一句话需求，判断属于以下哪种类型：

        - DATA_REPORT: 数据报表（月报、销量统计、业绩报表等）
        - BUSINESS_FLOW: 业务流程（审批流、订单流程、状态流转等）
        - API_INTEGRATION: 接口对接（对接第三方系统、数据同步等）
        - UI_CHANGE: UI 调整（页面改版、按钮样式、布局调整等）
        - RULE_CHANGE: 规则变更（修改计算逻辑、调整权限、改变校验规则等）
        - OTHER: 不属于上述任何类型

        严格按 JSON 格式输出，不要任何解释：
        {"type": "<类型>", "confidence": <0-1数字>, "reason": "<一句话理由>"}
        """;

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public Map<String, Object> classify(String userInput) {
        log.info("[ClassifierAgent] 分类: {}", userInput);
        try {
            String content = llmService.chat(SYSTEM_PROMPT, userInput, 200);
            return parseJson(content);
        } catch (Exception e) {
            log.error("分类失败", e);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("type", "OTHER");
            fallback.put("confidence", 0.0);
            fallback.put("reason", "分类失败: " + e.getMessage());
            return fallback;
        }
    }

    private Map<String, Object> parseJson(String content) {
        Map<String, Object> result = new HashMap<>();
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        String json = (start >= 0 && end > start) ? content.substring(start, end + 1) : content;
        try {
            JsonNode node = objectMapper.readTree(json);
            result.put("type", node.path("type").asText("OTHER"));
            result.put("confidence", node.path("confidence").asDouble(0.85));
            result.put("reason", node.path("reason").asText(""));
        } catch (Exception e) {
            log.warn("解析分类 JSON 失败: {}", e.getMessage());
            result.put("type", "OTHER");
            result.put("confidence", 0.0);
            result.put("reason", "解析失败");
        }
        return result;
    }
}
