package com.chatmind.dto;

import lombok.Data;

/**
 * 搜索请求DTO
 */
@Data
public class SearchRequest {
    
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 搜索范围：global-全局搜索，document-文档内搜索
     */
    private String scope = "global";
    
    /**
     * 文档ID(文档内搜索时必填)
     */
    private Long documentId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 是否搜索标签
     */
    private Boolean searchLabels = true;
    
    /**
     * 是否搜索内容
     */
    private Boolean searchContent = true;
    
    /**
     * 是否搜索描述
     */
    private Boolean searchDescription = true;
    
    /**
     * 页码(从1开始)
     */
    private Integer page = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 20;
}
