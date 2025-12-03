package com.chatmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 分享链接实体类
 * 管理文档的分享链接及权限
 */
@Data
@Entity
@Table(name = "pm_share", indexes = {
    @Index(name = "idx_document_id", columnList = "document_id"),
    @Index(name = "idx_share_code", columnList = "share_code")
})
@Comment("分享链接表")
public class PmShare {
    
    /**
     * 分享ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("分享ID")
    private Long id;
    
    @Column(name = "document_id", nullable = false)
    @Comment("所属文档ID")
    private Long documentId;
    
    @Column(name = "share_code", unique = true, length = 50, nullable = false)
    @Comment("分享码")
    private String shareCode;
    
    @Column(length = 10, nullable = false)
    @Comment("分享权限")
    private String permission = "read";
    
    @Column(name = "require_password")
    @Comment("是否需要密码")
    private Integer requirePassword = 0;
    
    @Column(length = 50)
    @Comment("访问密码")
    private String password;
    
    @Column(name = "expires_at")
    @Comment("过期时间")
    private LocalDateTime expiresAt;
    
    @Column(name = "allow_download")
    @Comment("允许下载")
    private Integer allowDownload = 0;
    
    @Column(name = "access_count")
    @Comment("访问次数")
    private Integer accessCount = 0;
    
    @Column(name = "enabled")
    @Comment("是否启用")
    private Integer enabled = 1;
    
    @Column(name = "created_by", nullable = false)
    @Comment("创建者ID")
    private Long createdBy;
    
    @Column(name = "created_at", updatable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @Comment("更新时间")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted")
    @Comment("逻辑删除标记")
    private Integer deleted = 0;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
