package com.chatmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 文档/脑图实体类
 * 文档是脑图的顶层容器,包含根节点、权限、可见性等信息
 */
@Data
@Entity
@Table(name = "pm_document")
@Comment("文档脑图表")
public class PmDocument {
    
    /**
     * 文档ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("文档ID")
    private Long id;
    
    /**
     * 文档标题
     */
    @Column(nullable = false, length = 200)
    @Comment("文档标题")
    private String title;
    
    /**
     * 所有者用户ID
     */
    @Column(name = "owner_id", nullable = false)
    @Comment("所有者用户ID")
    private Long ownerId;
    
    /**
     * 根节点ID
     */
    @Column(name = "root_node_id")
    @Comment("根节点ID")
    private Long rootNodeId;
    
    /**
     * 文档可见性：private/shared/public
     */
    @Column(length = 20)
    @Comment("文档可见性")
    private String visibility = "private";
    
    /**
     * 主题/皮肤
     */
    @Column(length = 50)
    @Comment("主题皮肤")
    private String theme;
    
    /**
     * 模板ID（如果基于模板创建）
     */
    @Column(name = "template_id")
    @Comment("模板ID")
    private Long templateId;
    
    /**
     * 文档元数据（JSON格式，存储其他配置信息）
     */
    @Column(columnDefinition = "TEXT")
    @Comment("文档元数据JSON")
    private String metadata;
    
    /**
     * 当前版本号
     */
    @Column(name = "current_version")
    @Comment("当前版本号")
    private Integer currentVersion = 1;
    
    /**
     * 最后编辑者ID
     */
    @Column(name = "last_editor_id")
    @Comment("最后编辑者ID")
    private Long lastEditorId;
    
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
     * 逻辑删除标记
     */
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
