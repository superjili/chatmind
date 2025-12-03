package com.chatmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 操作记录实体类
 * 记录所有编辑操作,用于实时协作、回放、CRDT合并等
 */
@Data
@Entity
@Table(name = "pm_operation", indexes = {
    @Index(name = "idx_document_id", columnList = "document_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Comment("操作记录表")
public class PmOperation {
    
    /**
     * 操作ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("操作ID")
    private Long id;
    
    @Column(name = "op_id", unique = true, length = 100, nullable = false)
    @Comment("操作唯一标识")
    private String opId;
    
    @Column(name = "document_id", nullable = false)
    @Comment("所属文档ID")
    private Long documentId;
    
    @Column(name = "node_id")
    @Comment("操作节点ID")
    private Long nodeId;
    
    @Column(name = "user_id", nullable = false)
    @Comment("操作用户ID")
    private Long userId;
    
    @Column(name = "op_type", length = 20, nullable = false)
    @Comment("操作类型")
    private String opType;
    
    @Column(columnDefinition = "TEXT")
    @Comment("操作负载JSON")
    private String payload;
    
    @Column(name = "causality_timestamp")
    @Comment("因果时间戳")
    private Long causalityTimestamp;
    
    @Column(name = "applied")
    @Comment("是否已应用")
    private Integer applied = 1;
    
    @Column(name = "created_at", updatable = false, nullable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (causalityTimestamp == null) {
            causalityTimestamp = System.currentTimeMillis();
        }
    }
}
