package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 记录操作请求DTO
 */
@Data
public class RecordOperationRequest {
    
    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long documentId;
    
    /**
     * 操作ID(幂等性保证)
     */
    @NotBlank(message = "操作ID不能为空")
    private String opId;
    
    /**
     * 操作类型：create/update/delete/move/collapse/batch
     */
    @NotBlank(message = "操作类型不能为空")
    private String opType;
    
    /**
     * 节点ID
     */
    private Long nodeId;
    
    /**
     * 操作数据(JSON格式)
     */
    private String opData;
    
    /**
     * 操作者用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 因果时间戳(用于CRDT排序)
     */
    private Long causalityTimestamp;
}
