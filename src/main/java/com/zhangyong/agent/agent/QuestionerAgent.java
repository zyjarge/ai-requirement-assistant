package com.zhangyong.agent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangyong.agent.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 追问 Agent - 每次只生成 1 个最关键问题
 *
 * 配合 Pipeline 多轮追问：用户回答 1 个 → AI 问下 1 个 → 直到问完所有问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionerAgent {

    private static final String SYSTEM_PROMPT = """
        你是资深产品经理。用户已经给你一个需求类型和一句话需求。

        你的任务：**只问 1 个**当前最关键的问题，帮用户完善需求描述。

        追问原则：
        1. 问题要具体、可回答（避免"请详细描述"这种开放问题）
        2. 一次只问 1 个问题，不要列出多个
        3. 按"使用对象 → 核心细节 → 边界场景"顺序，先问最重要的
        4. 问题不超过 30 字

        严格按以下 JSON 格式输出（不要任何解释）：
        {"question": "<单个问题>"}
        """;

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    /**
     * 生成单个问题（不是列表）
     */
    public String generateSingleQuestion(String requirementType, String userInput) {
        log.info("[QuestionerAgent] type={}", requirementType);
        try {
            String userPrompt = String.format("需求类型：%s\n用户需求：%s\n请只问 1 个最关键的问题。", requirementType, userInput);
            String content = llmService.chat(SYSTEM_PROMPT, userPrompt, 200);
            return parseQuestion(content, requirementType);
        } catch (Exception e) {
            log.error("生成追问失败", e);
            return getFallbackQuestions(requirementType)[0];
        }
    }

    /**
     * 兼容旧接口：返回 1 个问题的列表（Pipeline 一次只 push 1 个）
     */
    public List<String> generateQuestions(String requirementType, String userInput) {
        return List.of(generateSingleQuestion(requirementType, userInput));
    }

    /**
     * 根据需求类型生成完整的追问模板（用于初始化 questions 列表）
     */
    public List<String> generateAllQuestions(String requirementType, String userInput) {
        return switch (requirementType) {
            case "DATA_REPORT" -> Arrays.asList("这个报表给谁看？", "需要展示哪些核心指标？", "数据更新频率是？");
            case "BUSINESS_FLOW" -> Arrays.asList("哪些角色会参与这个流程？", "流程的触发节点是？", "异常情况如何处理？");
            case "API_INTEGRATION" -> Arrays.asList("对接哪个系统？", "数据推送方向是？", "调用频率大概多少？");
            case "UI_CHANGE" -> Arrays.asList("要改哪个页面？", "主要给哪些用户用？", "期望的效果是什么？");
            case "RULE_CHANGE" -> Arrays.asList("现在的规则是怎样的？", "想改成什么规则？", "会影响哪些业务场景？");
            default -> Arrays.asList("这个需求的背景是什么？", "成功的标准是什么？", "需要什么时候完成？");
        };
    }

    private String parseQuestion(String content, String type) {
        try {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            String json = (start >= 0 && end > start) ? content.substring(start, end + 1) : content;
            JsonNode node = objectMapper.readTree(json);
            String q = node.path("question").asText("");
            if (!q.isEmpty()) return q;
        } catch (Exception e) {
            log.warn("解析单个问题失败: {}", e.getMessage());
        }
        return getFallbackQuestions(type)[0];
    }

    private String[] getFallbackQuestions(String type) {
        return switch (type) {
            case "DATA_REPORT" -> new String[]{"这个报表给谁看？", "需要展示哪些核心指标？", "数据更新频率是？"};
            case "BUSINESS_FLOW" -> new String[]{"哪些角色会参与这个流程？", "流程的触发节点是？", "异常情况如何处理？"};
            case "API_INTEGRATION" -> new String[]{"对接哪个系统？", "数据推送方向是？", "调用频率大概多少？"};
            case "UI_CHANGE" -> new String[]{"要改哪个页面？", "主要给哪些用户用？", "期望的效果是什么？"};
            case "RULE_CHANGE" -> new String[]{"现在的规则是怎样的？", "想改成什么规则？", "会影响哪些业务场景？"};
            default -> new String[]{"这个需求的背景是什么？", "成功的标准是什么？", "需要什么时候完成？"};
        };
    }
}
