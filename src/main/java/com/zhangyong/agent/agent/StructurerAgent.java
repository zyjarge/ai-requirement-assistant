package com.zhangyong.agent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangyong.agent.llm.LlmService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 结构化输出 Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StructurerAgent {

    private static final String SYSTEM_PROMPT = """
        你是产品经理。用户给你一个需求类型 + 原始需求 + 追问与回答，请整理成结构化需求。

        严格按 JSON 输出（不要任何解释）：
        {
          "title": "<需求标题，20字以内>",
          "background": "<业务背景，1-2句>",
          "user_role": "<使用对象，如：销售总监>",
          "acceptance_criteria": "<验收标准，要可验证>",
          "priority": "<P0/P1/P2/P3>"
        }
        """;

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public StructuredDemand structure(String requirementType, String rawInput,
                                      List<String> questions, List<String> answers) {
        log.info("[StructurerAgent] 结构化");
        try {
            StringBuilder qa = new StringBuilder();
            for (int i = 0; i < questions.size(); i++) {
                qa.append(String.format("Q%d: %s\nA%d: %s\n", i + 1, questions.get(i),
                    i + 1, i < answers.size() ? answers.get(i) : "(未回答)"));
            }
            String userPrompt = String.format("""
                需求类型：%s
                原始需求：%s
                追问与回答：
                %s
                """, requirementType, rawInput, qa);

            String content = llmService.chat(SYSTEM_PROMPT, userPrompt, 500);
            return parseJson(content, rawInput);
        } catch (Exception e) {
            log.error("结构化失败", e);
            StructuredDemand fallback = new StructuredDemand();
            fallback.title = rawInput.length() > 20 ? rawInput.substring(0, 20) : rawInput;
            fallback.background = rawInput;
            fallback.user_role = "未知";
            fallback.acceptance_criteria = "请补充";
            fallback.priority = "P1";
            return fallback;
        }
    }

    private StructuredDemand parseJson(String content, String rawInput) throws Exception {
        StructuredDemand sd = new StructuredDemand();
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        String json = (start >= 0 && end > start) ? content.substring(start, end + 1) : content;
        JsonNode node = objectMapper.readTree(json);
        sd.title = node.path("title").asText(rawInput.length() > 20 ? rawInput.substring(0, 20) : rawInput);
        sd.background = node.path("background").asText(rawInput);
        sd.user_role = node.path("user_role").asText("未知");
        sd.acceptance_criteria = node.path("acceptance_criteria").asText("请补充");
        sd.priority = node.path("priority").asText("P1");
        return sd;
    }

    @Data
    public static class StructuredDemand {
        public String title;
        public String background;
        public String user_role;
        public String acceptance_criteria;
        public String priority;
    }
}
