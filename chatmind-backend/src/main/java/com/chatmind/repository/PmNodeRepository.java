package com.chatmind.repository;

import com.chatmind.entity.PmNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 节点数据访问层
 */
@Repository
public interface PmNodeRepository extends JpaRepository<PmNode, Long> {
    
    /**
     * 根据文档ID查询所有节点
     */
    List<PmNode> findByDocumentIdAndDeleted(Long documentId, Integer deleted);
    
    /**
     * 根据父节点ID查询子节点
     */
    List<PmNode> findByParentIdAndDeleted(Long parentId, Integer deleted);
    
    /**
     * 根据文档ID和父节点ID查询
     */
    List<PmNode> findByDocumentIdAndParentIdAndDeleted(Long documentId, Long parentId, Integer deleted);
    
    /**
     * 查询根节点
     */
    @Query("SELECT n FROM PmNode n WHERE n.documentId = :documentId AND n.parentId IS NULL AND n.deleted = 0")
    PmNode findRootNode(@Param("documentId") Long documentId);
}
