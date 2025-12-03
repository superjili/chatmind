package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 节点拖拽请求DTO
 */
@Data
public class MoveNodeRequest {
    
    /**
     * 要移动的节点ID
     */
    @NotNull(message = "节点ID不能为空")
    private Long nodeId;
    
    /**
     * 新的父节点ID(移动到根节点时为null)
     */
    private Long newParentId;
    
    /**
     * 目标位置索引(在新父节点的子节点列表中的位置)
     */
    private Integer targetIndex;
    
    /**
     * 操作用户ID
     */
    private Long userId;
}
