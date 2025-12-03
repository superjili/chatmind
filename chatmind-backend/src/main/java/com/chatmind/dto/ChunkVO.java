package com.chatmind.dto;

import lombok.Data;
import java.util.List;

/**
 * 文档分片VO
 */
@Data
public class ChunkVO {
    
    /**
     * 分片键(根节点ID)
     */
    private String chunkKey;
    
    /**
     * 分片类型:root/subtree
     */
    private String chunkType;
    
    /**
     * 根节点信息
     */
    private NodeVO rootNode;
    
    /**
     * 子节点列表
     */
    private List<NodeVO> childNodes;
    
    /**
     * 节点总数
     */
    private Integer nodeCount;
    
    /**
     * 最大层级深度
     */
    private Integer maxDepth;
    
    /**
     * 是否有更多子节点(懒加载标记)
     */
    private Boolean hasMore;
}
