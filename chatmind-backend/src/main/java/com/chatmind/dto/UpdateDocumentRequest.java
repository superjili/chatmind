package com.chatmind.dto;

import lombok.Data;

/**
 * 更新文档请求DTO
 */
@Data
public class UpdateDocumentRequest {
    
    /**
     * 文档标题
     */
    private String title;
    
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
