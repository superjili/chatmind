package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建节点请求DTO
 */
@Data
public class CreateNodeRequest {
    
    /**
     * 所属文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long documentId;
    
    /**
     * 父节点ID(创建根节点时为null)
     */
    private Long parentId;
    
    /**
     * 节点内容
     */
    @NotBlank(message = "节点内容不能为空")
    private String content;
    
    /**
     * 节点颜色
     */
    private String color;
    
    /**
     * 节点图标
     */
    private String icon;
    
    /**
     * 节点描述
     */
    private String description;
    
    /**
     * 标签(JSON数组字符串)
     */
    private String labels;
}
