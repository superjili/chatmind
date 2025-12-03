package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 导入请求DTO
 */
@Data
public class ImportRequest {
    
    /**
     * 导入内容
     */
    @NotBlank(message = "导入内容不能为空")
    private String content;
    
    /**
     * 导入格式：markdown/opml
     */
    @NotBlank(message = "导入格式不能为空")
    private String format;
    
    /**
     * 文档标题(可选,从内容自动提取)
     */
    private String title;
    
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 主题
     */
    private String theme = "default";
    
    /**
     * 可见性
     */
    private String visibility = "private";
}
