package com.chatmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * 版本快照实体类
 * 记录文档的历史版本,支持回滚和版本对比
 */
@Data
@Entity
@Table(name = "pm_version", indexes = {
    @Index(name = "idx_document_id", columnList = "document_id")
})
@Comment("版本快照表")
public class PmVersion {
    
    /**
     * 版本ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("版本ID")
    private Long id;
    
    @Column(name = "document_id", nullable = false)
    @Comment("所属文档ID")
    private Long documentId;
    
    @Column(name = "version_number", nullable = false)
    @Comment("版本号")
    private Integer versionNumber;
    
    @Column(name = "version_name", length = 100)
    @Comment("版本名称")
    private String versionName;
    
    @Column(columnDefinition = "TEXT")
    @Comment("版本描述")
    private String description;
    
    @Column(name = "version_type", length = 20)
    @Comment("版本类型")
    private String versionType;
    
    @Column(name = "node_count")
    @Comment("节点数量")
    private Integer nodeCount;
    
    @Column(name = "snapshot_size")
    @Comment("快照大小")
    private Long snapshotSize;
    
    @Column(name = "root_hash", length = 100)
    @Comment("根节点哈希")
    private String rootHash;
    
    @Column(name = "snapshot_data", columnDefinition = "LONGTEXT")
    @Comment("快照数据JSON")
    private String snapshotData;
    
    @Column(name = "created_by")
    @Comment("创建者ID")
    private Long createdBy;
    
    @Column(name = "created_at", updatable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;
    
    @Column(name = "deleted")
    @Comment("逻辑删除标记")
    private Integer deleted = 0;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
