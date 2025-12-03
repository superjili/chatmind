package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 创建版本请求DTO
 */
@Data
public class CreateVersionRequest {
    
    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long documentId;
    
    /**
     * 版本类型：autosave/explicit
     */
    private String versionType = "explicit";
    
    /**
     * 版本名称
     */
    private String versionName;
    
    /**
     * 版本描述
     */
    private String description;
    
    /**
     * 创建者ID
     */
    private Long createdBy;
}
