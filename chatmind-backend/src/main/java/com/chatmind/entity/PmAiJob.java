package com.chatmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import java.time.LocalDateTime;

/**
 * AI任务实体类
 * 记录LLM调用请求,管理AI生成任务的状态和结果
 */
@Data
@Entity
@Table(name = "pm_ai_job", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_document_id", columnList = "document_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Comment("AI任务表")
public class PmAiJob {
    
    /**
     * 任务ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("任务ID")
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @Comment("用户ID")
    private Long userId;
    
    @Column(name = "document_id")
    @Comment("文档ID")
    private Long documentId;
    
    @Column(name = "node_id")
    @Comment("节点ID")
    private Long nodeId;
    
    @Column(name = "job_type", length = 20, nullable = false)
    @Comment("任务类型")
    private String jobType;
    
    @Column(name = "prompt_template_id")
    @Comment("Prompt模板ID")
    private Long promptTemplateId;
    
    @Column(columnDefinition = "TEXT")
    @Comment("实际Prompt")
    private String prompt;
    
    @Column(length = 20, nullable = false)
    @Comment("任务状态")
    private String status = "pending";
    
    @Column(name = "result_data", columnDefinition = "LONGTEXT")
    @Comment("返回结果JSON")
    private String resultData;
    
    @Column(name = "result_summary", columnDefinition = "TEXT")
    @Comment("结果摘要")
    private String resultSummary;
    
    @Column(name = "validation_status", length = 20)
    @Comment("校验状态")
    private String validationStatus;
    
    @Column(name = "validation_error", columnDefinition = "TEXT")
    @Comment("校验错误")
    private String validationError;
    
    @Column(name = "cost_estimate")
    @Comment("成本估算")
    private Integer costEstimate;
    
    @Column(name = "actual_tokens")
    @Comment("实际Token")
    private Integer actualTokens;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    @Comment("错误信息")
    private String errorMessage;
    
    @Column(name = "retry_count")
    @Comment("重试次数")
    private Integer retryCount = 0;
    
    @Column(name = "created_at", updatable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    @Comment("完成时间")
    private LocalDateTime completedAt;
    
    @Column(name = "deleted")
    @Comment("逻辑删除标记")
    private Integer deleted = 0;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
