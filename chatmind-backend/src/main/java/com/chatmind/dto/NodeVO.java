package com.chatmind.dto;

import lombok.Data;

/**
 * 节点响应DTO
 */
@Data
public class NodeVO {
    
    /**
     * 节点ID
     */
    private Long id;
    
    /**
     * 所属文档ID
     */
    private Long documentId;
    
    /**
     * 父节点ID
     */
    private Long parentId;
    
    /**
     * 节点内容
     */
    private String content;
    
    /**
     * 子节点排序
     */
    private String childrenOrder;
    
    /**
     * 标签
     */
    private String labels;
    
    /**
     * 颜色
     */
    private String color;
    
    /**
     * 图标
     */
    private String icon;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 是否折叠
     */
    private Integer collapsed;
    
    /**
     * 位置信息
     */
    private String position;
    
    /**
     * 层级深度
     */
    private Integer depth;
    
    /**
     * 元数据
     */
    private String metadata;
    
    /**
     * 创建时间(时间戳)
     */
    private Long createdAt;
    
    /**
     * 更新时间(时间戳)
     */
    private Long updatedAt;
}
