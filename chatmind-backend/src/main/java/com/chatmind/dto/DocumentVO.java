package com.chatmind.dto;

import lombok.Data;

/**
 * 文档响应DTO
 */
@Data
public class DocumentVO {
    
    /**
     * 文档ID
     */
    private Long id;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 所有者用户ID
     */
    private Long ownerId;
    
    /**
     * 根节点ID
     */
    private Long rootNodeId;
    
    /**
     * 可见性
     */
    private String visibility;
    
    /**
     * 主题
     */
    private String theme;
    
    /**
     * 模板ID
     */
    private Long templateId;
    
    /**
     * 文档元数据（JSON格式）
     */
    private String metadata;
    
    /**
     * 当前版本号
     */
    private Integer currentVersion;
    
    /**
     * 最后编辑者ID
     */
    private Long lastEditorId;
    
    /**
     * 创建时间(时间戳)
     */
    private Long createdAt;
    
    /**
     * 更新时间(时间戳)
     */
    private Long updatedAt;
}
