package com.zhangyong.agent.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandCommentRepository extends JpaRepository<DemandComment, Long> {
    List<DemandComment> findByDemandIdOrderByCreatedAtDesc(Long demandId);
}
