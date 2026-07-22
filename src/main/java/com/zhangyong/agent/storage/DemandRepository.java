package com.zhangyong.agent.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Long>, JpaSpecificationExecutor<Demand> {

    /** 根据用户 ID 查询所有需求（按时间倒序） */
    List<Demand> findByUserIdOrderByCreatedAtDesc(String userId);

    /** 根据状态查询 */
    List<Demand> findByStatusOrderByCreatedAtDesc(String status);

    /** 根据用户 + 状态查询（用于"追问中"的会话恢复） */
    List<Demand> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);
}