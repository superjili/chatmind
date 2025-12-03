package com.chatmind.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 恢复版本请求DTO
 */
@Data
public class RestoreVersionRequest {
    
    /**
     * 版本ID
     */
    @NotNull(message = "版本ID不能为空")
    private Long versionId;
    
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 是否创建新版本(默认true)
     */
    private Boolean createNewVersion = true;
}
