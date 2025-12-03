package com.chatmind.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分享VO
 */
@Data
public class ShareVO {
    
    /**
     * 分享ID
     */
    private Long id;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 分享码
     */
    private String shareCode;
    
    /**
     * 权限类型：read/edit
     */
    private String permission;
    
    /**
     * 是否需要密码
     */
    private Boolean requirePassword;
    
    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 访问次数
     */
    private Integer accessCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 是否过期
     */
    private Boolean expired;
}
