package com.chatmind.repository;

import com.chatmind.entity.PmTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模板数据访问层
 */
@Repository
public interface PmTemplateRepository extends JpaRepository<PmTemplate, Long> {
    
    /**
     * 根据类型查询模板
     */
    List<PmTemplate> findByTemplateTypeAndDeleted(String templateType, Integer deleted);
    
    /**
     * 查询公开模板
     */
    List<PmTemplate> findByIsPublicAndDeleted(Integer isPublic, Integer deleted);
    
    /**
     * 根据创建者查询模板
     */
    List<PmTemplate> findByCreatedByAndDeleted(Long createdBy, Integer deleted);
}
