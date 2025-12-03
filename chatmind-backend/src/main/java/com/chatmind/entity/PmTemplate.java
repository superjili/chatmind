package com.chatmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 模板实体类
 * 管理脑图模板库(会议、SWOT、产品规划等)
 */
@Data
@Entity
@Table(name = "pm_template")
@Comment("模板表")
public class PmTemplate {
    
    /**
     * 模板ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("模板ID")
    private Long id;
    
    @Column(name = "template_name", nullable = false, length = 100)
    @Comment("模板名称")
    private String templateName;
    
    @Column(columnDefinition = "TEXT")
    @Comment("模板描述")
    private String description;
    
    @Column(name = "template_type", length = 20)
    @Comment("模板类型")
    private String templateType;
    
    @Column(name = "template_content", columnDefinition = "LONGTEXT")
    @Comment("模板内容JSON")
    private String templateContent;
    
    @Column(name = "default_style", columnDefinition = "TEXT")
    @Comment("默认样式JSON")
    private String defaultStyle;
    
    @Column(name = "prompt_skeleton", columnDefinition = "TEXT")
    @Comment("Prompt骨架")
    private String promptSkeleton;
    
    @Column(name = "output_schema", columnDefinition = "TEXT")
    @Comment("输出SchemaJSON")
    private String outputSchema;
    
    @Column(columnDefinition = "TEXT")
    @Comment("示例数据")
    private String example;
    
    @Column(name = "is_public")
    @Comment("是否公开")
    private Integer isPublic = 0;
    
    @Column(name = "created_by")
    @Comment("创建者ID")
    private Long createdBy;
    
    @Column(name = "use_count")
    @Comment("使用次数")
    private Integer useCount = 0;
    
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
