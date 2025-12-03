package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * AI扩展节点请求DTO
 */
@Data
public class ExpandNodeRequest {
    
    /**
     * 节点ID
     */
    @NotNull(message = "节点ID不能为空")
    private Long nodeId;
    
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 生成子节点数量(默认5)
     */
    private Integer count;
}
