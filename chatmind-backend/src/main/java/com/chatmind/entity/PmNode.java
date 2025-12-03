package com.chatmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 脑图节点实体类
 * 每个节点包含内容、样式、位置、层级关系等信息
 */
@Data
@Entity
@Table(name = "pm_node", indexes = {
    @Index(name = "idx_document_id", columnList = "document_id"),
    @Index(name = "idx_parent_id", columnList = "parent_id")
})
@Comment("脑图节点表")
public class PmNode {
    
    /**
     * 节点ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("节点ID")
    private Long id;
    
    /**
     * 所属文档ID
     */
    @Column(name = "document_id", nullable = false)
    @Comment("所属文档ID")
    private Long documentId;
    
    /**
     * 父节点ID（根节点为null）
     */
    @Column(name = "parent_id")
    @Comment("父节点ID")
    private Long parentId;
    
    /**
     * 节点内容
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    @Comment("节点内容")
    private String content;
    
    /**
     * 子节点排序（JSON数组，存储子节点ID的顺序）
     */
    @Column(name = "children_order", columnDefinition = "TEXT")
    @Comment("子节点排序JSON")
    private String childrenOrder;
    
    /**
     * 节点标签（JSON数组）
     */
    @Column(columnDefinition = "TEXT")
    @Comment("节点标签JSON")
    private String labels;
    
    /**
     * 节点颜色
     */
    @Column(length = 20)
    @Comment("节点颜色")
    private String color;
    
    /**
     * 节点图标
     */
    @Column(length = 50)
    @Comment("节点图标")
    private String icon;
    
    /**
     * 节点描述/备注
     */
    @Column(columnDefinition = "TEXT")
    @Comment("节点描述备注")
    private String description;
    
    /**
     * 是否折叠：0-展开，1-折叠
     */
    @Column(name = "collapsed")
    @Comment("是否折叠")
    private Integer collapsed = 0;
    
    /**
     * 节点位置信息（JSON格式，用于自由布局）
     */
    @Column(columnDefinition = "TEXT")
    @Comment("节点位置JSON")
    private String position;
    
    /**
     * 节点层级深度
     */
    @Column(name = "depth")
    @Comment("节点层级深度")
    private Integer depth = 0;
    
    /**
     * 元数据（JSON格式，包含AI生成标记等）
     */
    @Column(columnDefinition = "TEXT")
    @Comment("元数据JSON")
    private String metadata;
    
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
