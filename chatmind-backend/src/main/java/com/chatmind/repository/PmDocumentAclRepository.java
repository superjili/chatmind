package com.chatmind.repository;

import com.chatmind.entity.PmDocumentAcl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档权限ACL数据访问层
 */
@Repository
public interface PmDocumentAclRepository extends JpaRepository<PmDocumentAcl, Long> {
    
    /**
     * 根据文档ID查询ACL列表
     */
    List<PmDocumentAcl> findByDocumentIdAndDeleted(Long documentId, Integer deleted);
    
    /**
     * 根据用户ID查询ACL列表
     */
    List<PmDocumentAcl> findByUserIdAndDeleted(Long userId, Integer deleted);
    
    /**
     * 查询用户对文档的权限
     */
    PmDocumentAcl findByDocumentIdAndUserIdAndDeleted(Long documentId, Long userId, Integer deleted);
}
