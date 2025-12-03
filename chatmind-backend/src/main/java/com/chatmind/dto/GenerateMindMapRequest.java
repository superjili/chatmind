package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * AI生成脑图请求DTO
 */
@Data
public class GenerateMindMapRequest {
    
    /**
     * 输入文本
     */
    @NotBlank(message = "输入文本不能为空")
    private String text;
    
    /**
     * 目标文档ID(可选,不提供则创建新文档)
     */
    private Long documentId;
    
    /**
     * 模板ID(可选)
     */
    private Long templateId;
    
    /**
     * 最大层级深度(默认3)
     */
    private Integer maxDepth = 4;
    
    /**
     * 每个父节点最大子节点数(默认6)
     */
    private Integer maxChildren = 6;
    
    /**
     * 用户ID
     */
    private Long userId;
}
