package com.chatmind.dto;

import lombok.Data;
import java.util.List;

/**
 * 搜索结果DTO
 */
@Data
public class SearchResultDTO {
    
    /**
     * 节点ID
     */
    private Long nodeId;
    
    /**
     * 节点内容
     */
    private String content;
    
    /**
     * 节点描述
     */
    private String description;
    
    /**
     * 节点标签
     */
    private String labels;
    
    /**
     * 所属文档ID
     */
    private Long documentId;
    
    /**
     * 文档标题
     */
    private String documentTitle;
    
    /**
     * 父节点ID
     */
    private Long parentId;
    
    /**
     * 节点路径(面包屑导航)
     */
    private List<String> breadcrumb;
    
    /**
     * 匹配类型：content/description/label
     */
    private String matchType;
    
    /**
     * 高亮的内容片段
     */
    private String highlightSnippet;
    
    /**
     * 节点深度
     */
    private Integer depth;
    
    /**
     * 是否折叠
     */
    private Integer collapsed;
}
