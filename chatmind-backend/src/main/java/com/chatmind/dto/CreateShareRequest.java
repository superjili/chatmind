package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建分享请求DTO
 */
@Data
public class CreateShareRequest {
    
    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long documentId;
    
    /**
     * 权限类型：read/edit
     */
    @NotBlank(message = "权限类型不能为空")
    private String permission = "read";
    
    /**
     * 是否需要密码
     */
    private Boolean requirePassword = false;
    
    /**
     * 分享密码
     */
    private String password;
    
    /**
     * 过期天数(null表示永久)
     */
    private Integer expireDays;
    
    /**
     * 创建者ID
     */
    private Long createdBy;
}
