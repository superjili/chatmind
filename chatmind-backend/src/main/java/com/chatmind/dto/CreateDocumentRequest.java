package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建文档请求DTO
 */
@Data
public class CreateDocumentRequest {
    
    /**
     * 文档标题
     */
    @NotBlank(message = "文档标题不能为空")
    private String title;
    
    /**
     * 所有者用户ID
     */
    private Long ownerId;
    
    /**
     * 模板ID(可选)
     */
    private Long templateId;
    
    /**
     * 主题/皮肤
     */
    private String theme;
    
    /**
     * 可见性：private/shared/public
     */
    private String visibility;
    
    /**
     * 文档元数据（JSON格式，存储视图状态等配置信息）
     */
    private String metadata;
}
