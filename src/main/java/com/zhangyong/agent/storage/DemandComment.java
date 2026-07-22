package com.zhangyong.agent.storage;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "demand_comment")
public class DemandComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demand_id", nullable = false)
    private Long demandId;

    @Column(name = "author_user_id", nullable = false, length = 64)
    private String authorUserId;

    @Column(name = "author_name", length = 128)
    private String authorName;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
