package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * AI总结请求DTO
 */
@Data
public class SummarizeRequest {
    
    /**
     * 文档ID(可选)
     */
    private Long documentId;
    
    /**
     * 节点ID(可选)
     */
    private Long nodeId;
    
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 总结类型: brief/detailed(默认brief)
     */
    private String summaryType;
}
