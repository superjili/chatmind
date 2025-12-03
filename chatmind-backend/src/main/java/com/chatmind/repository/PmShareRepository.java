package com.chatmind.repository;

import com.chatmind.entity.PmShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 分享链接数据访问层
 */
@Repository
public interface PmShareRepository extends JpaRepository<PmShare, Long> {
    
    /**
     * 根据分享码查询
     */
    PmShare findByShareCode(String shareCode);
    
    /**
     * 根据文档ID查询所有分享链接
     */
    List<PmShare> findByDocumentIdAndDeleted(Long documentId, Integer deleted);
    
    /**
     * 根据创建者查询分享链接
     */
    List<PmShare> findByCreatedByAndDeleted(Long createdBy, Integer deleted);
}
