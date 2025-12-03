package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建模板请求DTO
 */
@Data
public class CreateTemplateRequest {
    
    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    
    /**
     * 模板类型：meeting/swot/product/mindmap/custom
     */
    private String templateType = "custom";
    
    /**
     * 模板描述
     */
    private String description;
    
    /**
     * 模板内容(JSON格式)
     */
    @NotBlank(message = "模板内容不能为空")
    private String templateContent;
    
    /**
     * 是否公开：0-私有，1-公开
     */
    private Integer isPublic = 0;
    
    /**
     * 创建者ID
     */
    private Long createdBy;
}
