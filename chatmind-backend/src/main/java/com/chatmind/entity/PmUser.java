package com.chatmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 管理用户基本信息、角色、配额等
 */
@Data
@Entity
@Table(name = "pm_user")
@Comment("用户表")
public class PmUser {
    
    /**
     * 用户ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("用户ID")
    private Long id;
    
    /**
     * 用户名
     */
    @Column(nullable = false, length = 100)
    @Comment("用户名")
    private String name;
    
    /**
     * 头像URL
     */
    @Column(length = 500)
    @Comment("头像URL")
    private String avatar;
    
    /**
     * 用户角色：owner/editor/viewer
     */
    @Column(length = 20)
    @Comment("用户角色")
    private String role;
    
    /**
     * AI调用日配额
     */
    @Column(name = "daily_ai_quota")
    @Comment("AI调用日配额")
    private Integer dailyAiQuota;
    
    /**
     * AI调用已使用配额
     */
    @Column(name = "used_ai_quota")
    @Comment("AI调用已使用配额")
    private Integer usedAiQuota;
    
    /**
     * 配额重置时间
     */
    @Column(name = "quota_reset_time")
    @Comment("配额重置时间")
    private LocalDateTime quotaResetTime;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", updatable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    @Comment("更新时间")
    private LocalDateTime updatedAt;
    
    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    @Column(name = "deleted")
    @Comment("逻辑删除标记")
    private Integer deleted = 0;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (dailyAiQuota == null) {
            dailyAiQuota = 100; // 默认配额
        }
        if (usedAiQuota == null) {
            usedAiQuota = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
