package com.chatmind.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模板VO
 */
@Data
public class TemplateVO {
    
    /**
     * 模板ID
     */
    private Long id;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 模板类型
     */
    private String templateType;
    
    /**
     * 模板描述
     */
    private String description;
    
    /**
     * 模板内容(JSON)
     */
    private String templateContent;
    
    /**
     * 是否公开
     */
    private Integer isPublic;
    
    /**
     * 使用次数
     */
    private Integer useCount;
    
    /**
     * 创建者ID
     */
    private Long createdBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
