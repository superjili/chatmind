package com.chatmind.repository;

import com.chatmind.entity.PmDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档数据访问层
 */
@Repository
public interface PmDocumentRepository extends JpaRepository<PmDocument, Long> {
    
    /**
     * 根据所有者ID查询文档列表
     */
    List<PmDocument> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);
    
    /**
     * 根据可见性查询文档
     */
    List<PmDocument> findByVisibilityAndDeleted(String visibility, Integer deleted);
}
