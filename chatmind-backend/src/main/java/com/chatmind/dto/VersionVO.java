package com.chatmind.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 版本VO
 */
@Data
public class VersionVO {
    
    /**
     * 版本ID
     */
    private Long id;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 版本号
     */
    private Integer versionNumber;
    
    /**
     * 版本名称
     */
    private String versionName;
    
    /**
     * 版本类型：autosave/explicit
     */
    private String versionType;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 快照数据(JSON)
     */
    private String snapshotData;
    
    /**
     * 节点数量
     */
    private Integer nodeCount;
    
    /**
     * 快照大小(字节)
     */
    private Long snapshotSize;
    
    /**
     * 创建者ID
     */
    private Long createdBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
