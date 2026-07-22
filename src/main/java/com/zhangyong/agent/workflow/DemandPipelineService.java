package com.zhangyong.agent.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangyong.agent.agent.ClassifierAgent;
import com.zhangyong.agent.agent.QuestionerAgent;
import com.zhangyong.agent.agent.StructurerAgent;
import com.zhangyong.agent.tool.WeComApiClient;
import com.zhangyong.agent.channel.WeComMessage;
import com.zhangyong.agent.config.WeComProperties;
import com.zhangyong.agent.storage.Demand;
import com.zhangyong.agent.storage.DemandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 需求处理 Pipeline 服务
 *
 * 核心编排：
 * 1. 第一轮消息：原始需求 → Classifier → Questioner → 保存会话 → 返回追问
 * 2. 中间轮次：用户回答 → 继续追问或进入结构化
 * 3. 最终轮次：所有回答完成 → Structurer → 归档 → 通知
 *
 * 会话状态存 Redis（key: session:{userId}，TTL 30 分钟）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandPipelineService {

    private static final String SESSION_KEY_PREFIX = "demand:session:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    /** 追问最大轮数（每轮 1 个问题，所以最大问题数） */
    private static final int MAX_QUESTIONS = 3;

    private final ClassifierAgent classifierAgent;
    private final QuestionerAgent questionerAgent;
    private final StructurerAgent structurerAgent;
    private final DemandRepository demandRepository;
    private final WeComApiClient weComApiClient;
    private final WeComProperties weComProperties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.agent.max-rounds:2}")
    private int maxRounds;

    /**
     * 处理企业微信消息
     */
    public String handle(WeComMessage msg) {
        String userId = msg.getFromUserName();
        String content = msg.getContent();

        DemandSession session = getSession(userId);

        // 如果用户发"重置"、"重新开始"，清空会话
        if (isResetCommand(content)) {
            clearSession(userId);
            return "✅ 会话已重置。请重新描述您的需求。";
        }

        // 如果会话已开始追问，把消息当作"回答"
        if (session != null && session.isQuestioning()) {
            return handleAnswer(session, userId, content);
        }

        // 否则当作新需求
        return handleNewRequest(userId, content);
    }

    /**
     * 处理新需求（第一轮）
     */
    private String handleNewRequest(String userId, String rawInput) {
        log.info("[Pipeline] 新需求: user={}, input={}", userId, rawInput);

        // 1. 分类
        Map<String, Object> classification = classifierAgent.classify(rawInput);
        String type = (String) classification.get("type");
        Double confidence = (Double) classification.get("confidence");

        List<String> questions = questionerAgent.generateAllQuestions(type, rawInput);
        if (questions.size() > MAX_QUESTIONS) {
            questions = questions.subList(0, MAX_QUESTIONS);
        }

        // 3. 保存会话
        DemandSession session = DemandSession.builder()
            .userId(userId)
            .rawInput(rawInput)
            .requirementType(type)
            .confidence(confidence)
            .questions(questions)
            .currentRound(1)
            .createdAt(System.currentTimeMillis())
            .build();
        saveSession(session);

        // 4. 构造回复（带分类结果 + 第一个问题）
        String typeName = getTypeName(type);
        StringBuilder reply = new StringBuilder();
        reply.append(String.format("📋 **需求分类**：%s\n", typeName));
        if (classification.get("reason") != null) {
            reply.append(String.format("💡 分类理由：%s\n\n", classification.get("reason")));
        }
        reply.append(String.format("我会逐个问您 %d 个问题，先回答第 1 个：\n\n", questions.size()));
        reply.append(String.format("1️⃣ %s", questions.get(0)));
        reply.append("\n\n（回复「重置」可重新开始）");
        return reply.toString();
    }

    /**
     * 处理用户回答（中间轮次）
     */
    private String handleAnswer(DemandSession session, String userId, String answer) {
        log.info("[Pipeline] 用户回答: round={}, answer={}", session.getCurrentRound(), answer);

        session.addAnswer(answer);
        session.nextRound();

        // 还有更多问题
        if (session.getCurrentRound() <= session.getQuestions().size()) {
            saveSession(session);
            int nextIdx = session.getCurrentRound() - 1;
            return String.format("✅ 已记录回答 %d/%d\n\n📝 问题 %d：%s",
                session.getCurrentRound() - 1,
                session.getQuestions().size(),
                session.getCurrentRound(),
                session.getQuestions().get(nextIdx));
        }

        // 所有问题已答完，进入结构化
        log.info("[Pipeline] 所有追问完成，进入结构化");
        return structureAndArchive(session);
    }

    /**
     * 结构化输出 + 归档 + 通知
     */
    private String structureAndArchive(DemandSession session) {
        // 1. 结构化
        StructurerAgent.StructuredDemand sd = structurerAgent.structure(
            session.getRequirementType(),
            session.getRawInput(),
            session.getQuestions(),
            session.getAnswers()
        );

        // 2. 归档到 MySQL
        Demand demand = archive(session, sd);

        // 3. 清除会话
        clearSession(session.getUserId());

        // 4. 构造回复
        StringBuilder reply = new StringBuilder();
        reply.append("🎉 **需求已结构化并归档！**\n\n");
        reply.append(String.format("📌 **标题**：%s\n\n", sd.title));
        reply.append(String.format("📖 **背景**：%s\n\n", sd.background));
        reply.append(String.format("👥 **使用对象**：%s\n\n", sd.user_role));
        reply.append(String.format("✅ **验收标准**：%s\n\n", sd.acceptance_criteria));
        reply.append(String.format("⭐ **优先级**：%s\n\n", sd.priority));
        reply.append(String.format("需求 ID: %d\n", demand.getId()));
        reply.append("（回复「重置」可开始下一个需求）");

        // 5. 主动通知产品经理（如果有配置）
        notifyProductManager(demand, sd);

        return reply.toString();
    }

    /**
     * 归档到 MySQL
     */
    private Demand archive(DemandSession session, StructurerAgent.StructuredDemand sd) {
        Demand demand = new Demand();
        demand.setUserId(session.getUserId());
        demand.setRawInput(session.getRawInput());
        demand.setRequirementType(session.getRequirementType());
        demand.setTitle(sd.title);
        demand.setBusinessContext(sd.background);
        demand.setUserRole(sd.user_role);
        demand.setAcceptanceCriteria(sd.acceptance_criteria);
        demand.setPriority(sd.priority);
        demand.setStatus("ARCHIVED");
        demand.setStructured(true);
        try {
            // 把问答历史存为 JSON
            StringBuilder qaJson = new StringBuilder("[");
            for (int i = 0; i < session.getQuestions().size(); i++) {
                if (i > 0) qaJson.append(",");
                qaJson.append(String.format("{\"q\":\"%s\",\"a\":\"%s\"}",
                    escape(session.getQuestions().get(i)),
                    i < session.getAnswers().size() ? escape(session.getAnswers().get(i)) : ""));
            }
            qaJson.append("]");
            demand.setQaHistory(qaJson.toString());
        } catch (Exception e) {
            log.warn("问答历史 JSON 序列化失败", e);
        }
        return demandRepository.save(demand);
    }

    /**
     * 通知产品经理（可选）
     */
    private void notifyProductManager(Demand demand, StructurerAgent.StructuredDemand sd) {
        try {
            // 简化处理：把 userId 当作 touser（实际生产环境应通知产品经理角色）
            String content = String.format(
                "📥 新需求待评审\n标题：%s\n提交人：%s\n类型：%s\n优先级：%s\n\n需求 ID: %d",
                sd.title,
                demand.getUserId(),
                getTypeName(sd.title),
                sd.priority,
                demand.getId()
            );
            // 实际场景应该发给产品经理而非业务人员
            // weComApiClient.sendTextMessage(productManagerIds, content);
            log.info("[通知产品经理] 需求 {} 已归档", demand.getId());
        } catch (Exception e) {
            log.warn("通知产品经理失败", e);
        }
    }

    // ============ 会话管理（Redis）============

    private DemandSession getSession(String userId) {
        try {
            String key = SESSION_KEY_PREFIX + userId;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, DemandSession.class);
        } catch (Exception e) {
            log.warn("读取会话失败: {}", e.getMessage());
            return null;
        }
    }

    private void saveSession(DemandSession session) {
        try {
            String key = SESSION_KEY_PREFIX + session.getUserId();
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key, json, SESSION_TTL);
        } catch (JsonProcessingException e) {
            log.error("保存会话失败", e);
        }
    }

    private void clearSession(String userId) {
        redisTemplate.delete(SESSION_KEY_PREFIX + userId);
    }

    private boolean isResetCommand(String content) {
        if (content == null) return false;
        String c = content.trim();
        return "重置".equals(c) || "重新开始".equals(c) || "reset".equalsIgnoreCase(c);
    }

    private String getTypeName(String type) {
        return switch (type) {
            case "DATA_REPORT" -> "数据报表";
            case "BUSINESS_FLOW" -> "业务流程";
            case "API_INTEGRATION" -> "接口对接";
            case "UI_CHANGE" -> "UI 调整";
            case "RULE_CHANGE" -> "规则变更";
            default -> "其他";
        };
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}