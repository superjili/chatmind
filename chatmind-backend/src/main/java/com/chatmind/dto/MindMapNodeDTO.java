package com.chatmind.dto;

import lombok.Data;
import java.util.List;

/**
 * AI生成的脑图节点结构DTO
 */
@Data
public class MindMapNodeDTO {
    
    /**
     * 节点文本内容
     */
    private String text;
    
    /**
     * 节点标签
     */
    private List<String> labels;
    
    /**
     * 建议的颜色
     */
    private String suggestedColor;
    
    /**
     * 子节点列表
     */
    private List<MindMapNodeDTO> children;
    
    /**
     * 是否包含敏感内容
     */
    private Boolean sensitive = false;
}
