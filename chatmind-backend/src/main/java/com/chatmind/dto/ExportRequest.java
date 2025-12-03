package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 导出文档请求DTO
 */
@Data
public class ExportRequest {
    
    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long documentId;
    
    /**
     * 导出格式：markdown/opml/json/png/svg/pdf
     */
    @NotBlank(message = "导出格式不能为空")
    private String format;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 是否包含样式信息(用于图片导出)
     */
    private Boolean includeStyle = true;
    
    /**
     * 图片宽度(像素)
     */
    private Integer imageWidth = 1920;
    
    /**
     * 图片高度(像素)
     */
    private Integer imageHeight = 1080;
}
