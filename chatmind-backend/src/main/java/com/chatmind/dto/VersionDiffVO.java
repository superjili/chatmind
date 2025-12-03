package com.chatmind.dto;

import lombok.Data;
import java.util.List;

/**
 * 版本差异VO
 */
@Data
public class VersionDiffVO {
    
    /**
     * 源版本ID
     */
    private Long fromVersionId;
    
    /**
     * 目标版本ID
     */
    private Long toVersionId;
    
    /**
     * 新增的节点
     */
    private List<DiffNodeVO> addedNodes;
    
    /**
     * 删除的节点
     */
    private List<DiffNodeVO> removedNodes;
    
    /**
     * 更新的节点
     */
    private List<DiffNodeVO> updatedNodes;
    
    /**
     * 移动的节点
     */
    private List<MovedNodeVO> movedNodes;
    
    /**
     * 差异节点数统计
     */
    @Data
    public static class DiffStats {
        private Integer addedCount;
        private Integer removedCount;
        private Integer updatedCount;
        private Integer movedCount;
    }
    
    /**
     * 差异统计
     */
    private DiffStats stats;
}
