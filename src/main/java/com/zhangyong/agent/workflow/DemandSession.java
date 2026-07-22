package com.zhangyong.agent.workflow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 需求会话状态
 *
 * 存储在 Redis 中（key: session:{userId}）
 * 跟踪用户的多次消息：
 * - 原始需求
 * - 分类结果
 * - 追问列表
 * - 用户回答
 * - 当前轮次
 *
 * 序列化为 JSON 存储
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 原始一句话需求 */
    private String rawInput;

    /** 需求类型 */
    private String requirementType;

    /** 分类置信度 */
    private Double confidence;

    /** 追问列表 */
    @Builder.Default
    private List<String> questions = new ArrayList<>();

    /** 用户回答列表 */
    @Builder.Default
    private List<String> answers = new ArrayList<>();

    /** 当前追问轮次（0 表示刚开始，1 表示第 1 轮） */
    @Builder.Default
    private int currentRound = 0;

    /** 是否已结构化完成 */
    @Builder.Default
    private boolean structured = false;

    /** 已结构化的需求 JSON */
    private String structuredJson;

    /** 会话创建时间戳 */
    private long createdAt;

    /**
     * 当前是否在追问阶段（Jackson 反序列化时跳过）
     */
    @JsonIgnore
    public boolean isQuestioning() {
        return currentRound > 0 && currentRound <= questions.size() && !structured;
    }

    /**
     * 添加用户回答
     */
    public void addAnswer(String answer) {
        answers.add(answer);
    }

    /**
     * 进入下一轮
     */
    public void nextRound() {
        currentRound++;
    }
}