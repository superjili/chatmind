package com.chatmind.dto;

import lombok.Data;

/**
 * 差异节点VO
 */
@Data
public class DiffNodeVO {
    
    /**
     * 节点ID
     */
    private Long nodeId;
    
    /**
     * 节点内容
     */
    private String content;
    
    /**
     * 父节点ID
     */
    private Long parentId;
    
    /**
     * 节点层级
     */
    private Integer level;
    
    /**
     * 节点路径(用于展示面包屑)
     */
    private String path;
    
    /**
     * 变更类型(对于更新节点):content/style/position
     */
    private String changeType;
    
    /**
     * 旧值(对于更新节点)
     */
    private String oldValue;
    
    /**
     * 新值(对于更新节点)
     */
    private String newValue;
}
