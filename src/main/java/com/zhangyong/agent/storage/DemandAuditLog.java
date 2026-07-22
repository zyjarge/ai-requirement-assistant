package com.zhangyong.agent.storage;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "demand_audit_log")
public class DemandAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demand_id", nullable = false)
    private Long demandId;

    @Column(name = "actor_user_id", nullable = false, length = 64)
    private String actorUserId;

    @Column(name = "actor_name", length = 128)
    private String actorName;

    @Column(name = "field_name", nullable = false, length = 64)
    private String fieldName;

    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue;

    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
