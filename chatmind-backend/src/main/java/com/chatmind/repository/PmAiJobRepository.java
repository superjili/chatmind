package com.chatmind.repository;

import com.chatmind.entity.PmAiJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI任务数据访问层
 */
@Repository
public interface PmAiJobRepository extends JpaRepository<PmAiJob, Long> {
    
    /**
     * 根据用户ID查询任务列表
     */
    List<PmAiJob> findByUserIdAndDeletedOrderByCreatedAtDesc(Long userId, Integer deleted);
    
    /**
     * 根据文档ID查询任务列表
     */
    List<PmAiJob> findByDocumentIdAndDeletedOrderByCreatedAtDesc(Long documentId, Integer deleted);
    
    /**
     * 根据状态查询任务
     */
    List<PmAiJob> findByStatusOrderByCreatedAtAsc(String status);
}
