package com.chatmind.dto;

import lombok.Data;

/**
 * 用户在线状态VO
 */
@Data
public class PresenceVO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户头像
     */
    private String avatar;
    
    /**
     * 用户颜色(用于光标标识)
     */
    private String color;
    
    /**
     * 当前焦点节点ID
     */
    private Long focusNodeId;
    
    /**
     * 当前选中节点IDs
     */
    private String selectedNodeIds;
    
    /**
     * 在线状态:online/away/busy
     */
    private String status;
    
    /**
     * 加入时间
     */
    private Long joinedAt;
    
    /**
     * 最后活跃时间
     */
    private Long lastActiveAt;
}
