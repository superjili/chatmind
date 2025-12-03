package com.chatmind.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作记录VO
 */
@Data
public class OperationVO {
    
    /**
     * 操作记录ID
     */
    private Long id;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 操作ID
     */
    private String opId;
    
    /**
     * 操作类型
     */
    private String opType;
    
    /**
     * 节点ID
     */
    private Long nodeId;
    
    /**
     * 操作数据
     */
    private String opData;
    
    /**
     * 操作者用户ID
     */
    private Long userId;
    
    /**
     * 因果时间戳
     */
    private Long causalityTimestamp;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
