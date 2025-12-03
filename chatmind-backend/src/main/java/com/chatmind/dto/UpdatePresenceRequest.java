package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 更新在线状态请求
 */
@Data
public class UpdatePresenceRequest {
    
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 焦点节点ID
     */
    private Long focusNodeId;
    
    /**
     * 选中节点IDs(JSON数组)
     */
    private String selectedNodeIds;
    
    /**
     * 状态
     */
    private String status;
}
