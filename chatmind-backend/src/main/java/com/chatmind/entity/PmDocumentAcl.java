package com.chatmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 文档权限ACL实体类
 * 管理文档的访问控制列表
 */
@Data
@Entity
@Table(name = "pm_document_acl", indexes = {
    @Index(name = "idx_document_id", columnList = "document_id"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Comment("文档权限ACL表")
public class PmDocumentAcl {
    
    /**
     * ACL记录ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("ACL记录ID")
    private Long id;
    
    @Column(name = "document_id", nullable = false)
    @Comment("所属文档ID")
    private Long documentId;
    
    @Column(name = "user_id", nullable = false)
    @Comment("用户ID")
    private Long userId;
    
    @Column(length = 20, nullable = false)
    @Comment("权限级别")
    private String permission;
    
    @Column(name = "granted_by")
    @Comment("授予者ID")
    private Long grantedBy;
    
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
