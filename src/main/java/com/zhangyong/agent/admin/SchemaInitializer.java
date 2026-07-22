package com.zhangyong.agent.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时检查并创建管理端需要的 schema：
 *   - demand 表加 assignee_user_id 列
 *   - demand 表加 notes 列
 *   - demand_comment 新表
 *   - demand_audit_log 新表
 *
 * 全部幂等，重启不会破坏数据。
 */
@Slf4j
@Component
@Order(0)  // 在 JPA 启动后跑，确保 INFORMATION_SCHEMA 稳定
@RequiredArgsConstructor
public class SchemaInitializer {

    private final JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        addColumnIfMissing("demand", "assignee_user_id", "VARCHAR(64) NULL");
        addColumnIfMissing("demand", "notes", "TEXT NULL");
        createTableIfMissing("demand_comment", """
            CREATE TABLE demand_comment (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              demand_id BIGINT NOT NULL,
              author_user_id VARCHAR(64) NOT NULL,
              author_name VARCHAR(128) NULL,
              content TEXT NOT NULL,
              created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
              KEY idx_demand_id_created (demand_id, created_at DESC)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        createTableIfMissing("demand_audit_log", """
            CREATE TABLE demand_audit_log (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              demand_id BIGINT NOT NULL,
              actor_user_id VARCHAR(64) NOT NULL,
              actor_name VARCHAR(128) NULL,
              field_name VARCHAR(64) NOT NULL,
              before_value TEXT NULL,
              after_value TEXT NULL,
              created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
              KEY idx_demand_id_created (demand_id, created_at DESC)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        log.info("SchemaInitializer: admin schema ready");
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
            Integer.class, table, column);
        if (count != null && count > 0) {
            log.debug("column {}.{} already exists", table, column);
            return;
        }
        log.info("adding column {}.{}", table, column);
        jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl);
    }

    private void createTableIfMissing(String table, String ddl) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ?",
            Integer.class, table);
        if (count != null && count > 0) {
            log.debug("table {} already exists", table);
            return;
        }
        log.info("creating table {}", table);
        jdbc.execute(ddl);
    }
}
