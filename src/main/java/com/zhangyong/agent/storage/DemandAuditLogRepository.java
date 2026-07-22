package com.zhangyong.agent.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandAuditLogRepository extends JpaRepository<DemandAuditLog, Long> {
    List<DemandAuditLog> findByDemandIdOrderByCreatedAtDesc(Long demandId);
}
