package com.chatmind.dto;

import lombok.Data;

/**
 * 限流配额VO
 */
@Data
public class RateLimitVO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 限流类型:ai_call/export/share
     */
    private String limitType;
    
    /**
     * 时间窗口:hour/day/month
     */
    private String timeWindow;
    
    /**
     * 配额上限
     */
    private Integer quota;
    
    /**
     * 已使用量
     */
    private Integer used;
    
    /**
     * 剩余量
     */
    private Integer remaining;
    
    /**
     * 是否被限流
     */
    private Boolean rateLimited;
    
    /**
     * 重置时间(时间戳)
     */
    private Long resetAt;
}
