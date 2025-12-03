package com.chatmind.repository;

import com.chatmind.entity.PmOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作记录数据访问层
 */
@Repository
public interface PmOperationRepository extends JpaRepository<PmOperation, Long> {
    
    /**
     * 根据文档ID查询操作记录
     */
    List<PmOperation> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
    
    /**
     * 根据opId查询操作
     */
    PmOperation findByOpId(String opId);
    
    /**
     * 查询指定时间范围内的操作
     */
    List<PmOperation> findByDocumentIdAndCreatedAtBetweenOrderByCreatedAtAsc(
        Long documentId, LocalDateTime start, LocalDateTime end
    );
    
    /**
     * 按创建时间升序查询
     */
    List<PmOperation> findByDocumentIdOrderByCreatedAtAsc(Long documentId);
    
    /**
     * 根据因果时间戳范围查询
     */
    List<PmOperation> findByDocumentIdAndCausalityTimestampBetweenOrderByCausalityTimestampAsc(
        Long documentId, Long fromTimestamp, Long toTimestamp
    );
    
    /**
     * 查询指定时间之前的操作
     */
    List<PmOperation> findByDocumentIdAndCreatedAtBeforeOrderByCreatedAtAsc(
        Long documentId, LocalDateTime before
    );
}
