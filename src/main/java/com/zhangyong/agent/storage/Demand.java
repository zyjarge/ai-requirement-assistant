package com.zhangyong.agent.storage;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 需求实体
 *
 * 需求类型：
 * - DATA_REPORT: 数据报表
 * - BUSINESS_FLOW: 业务流程
 * - API_INTEGRATION: 接口对接
 * - UI_CHANGE: UI 调整
 * - RULE_CHANGE: 规则变更
 * - OTHER: 其他
 *
 * 状态：
 * - SUBMITTED: 已提交
 * - QUESTIONING: 追问中
 * - STRUCTURED: 已结构化
 * - ARCHIVED: 已归档
 */
@Data
@Entity
@Table(name = "demand")
public class Demand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务人员 ID（企业微信 UserID） */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /** 需求类型 */
    @Column(name = "requirement_type", length = 32)
    private String requirementType;

    /** 标题 */
    @Column(name = "title", length = 256)
    private String title;

    /** 原始一句话需求 */
    @Column(name = "raw_input", columnDefinition = "TEXT")
    private String rawInput;

    /** 业务背景 */
    @Column(name = "business_context", columnDefinition = "TEXT")
    private String businessContext;

    /** 用户角色 */
    @Column(name = "user_role", columnDefinition = "TEXT")
    private String userRole;

    /** 验收标准 */
    @Column(name = "acceptance_criteria", columnDefinition = "TEXT")
    private String acceptanceCriteria;

    /** 优先级 */
    @Column(name = "priority", length = 16)
    private String priority;

    /** 追问历史（JSON） */
    @Column(name = "qa_history", columnDefinition = "TEXT")
    private String qaHistory;

    /** 状态 */
    @Column(name = "status", length = 16)
    private String status = "SUBMITTED";

    /** 是否已结构化完成 */
    @Column(name = "structured")
    private Boolean structured = false;

    /** 被指派人 userid（NULL = 未派单） */
    @Column(name = "assignee_user_id", length = 64)
    private String assigneeUserId;

    /** 内部备注（一段纯文本） */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 反射式 set，AdminApiController 用，避免写一堆 if/else */
    public void setByString(String field, String value) {
        switch (field) {
            case "status" -> setStatus(value);
            case "priority" -> setPriority(value);
            case "assigneeUserId" -> setAssigneeUserId(value);
            case "notes" -> setNotes(value);
            default -> throw new IllegalArgumentException("unknown field: " + field);
        }
    }
}