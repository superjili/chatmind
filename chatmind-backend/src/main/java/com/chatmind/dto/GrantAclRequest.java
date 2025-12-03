package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 授权ACL请求DTO
 */
@Data
public class GrantAclRequest {
    
    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long documentId;
    
    /**
     * 被授权用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 权限类型：owner/edit/read
     */
    private String permission = "read";
    
    /**
     * 授权者ID
     */
    private Long grantedBy;
}
