package com.chatmind.dto;

import lombok.Data;

/**
 * 移动节点VO
 */
@Data
public class MovedNodeVO {
    
    /**
     * 节点ID
     */
    private Long nodeId;
    
    /**
     * 节点内容
     */
    private String content;
    
    /**
     * 旧父节点ID
     */
    private Long oldParentId;
    
    /**
     * 新父节点ID
     */
    private Long newParentId;
    
    /**
     * 旧路径
     */
    private String oldPath;
    
    /**
     * 新路径
     */
    private String newPath;
}
