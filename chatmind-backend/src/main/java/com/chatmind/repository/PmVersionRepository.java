package com.chatmind.repository;

import com.chatmind.entity.PmVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 版本快照数据访问层
 */
@Repository
public interface PmVersionRepository extends JpaRepository<PmVersion, Long> {
    
    /**
     * 根据文档ID查询所有版本
     */
    List<PmVersion> findByDocumentIdAndDeletedOrderByVersionNumberDesc(Long documentId, Integer deleted);
    
    /**
     * 根据文档ID和版本类型查询
     */
    List<PmVersion> findByDocumentIdAndVersionTypeAndDeletedOrderByVersionNumberDesc(
        Long documentId, String versionType, Integer deleted);
    
    /**
     * 根据文档ID和版本号查询
     */
    PmVersion findByDocumentIdAndVersionNumber(Long documentId, Integer versionNumber);
    
    /**
     * 查询最新版本
     */
    PmVersion findFirstByDocumentIdAndDeletedOrderByVersionNumberDesc(Long documentId, Integer deleted);
    
    /**
     * 获取文档的最大版本号
     */
    @Query("SELECT MAX(v.versionNumber) FROM PmVersion v WHERE v.documentId = ?1")
    Integer findMaxVersionNumberByDocumentId(Long documentId);
}
