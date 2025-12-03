package com.chatmind.dto;

import lombok.Data;

/**
 * AI任务VO
 */
@Data
public class AIJobVO {
    
    /**
     * 任务ID
     */
    private Long id;
    
    /**
     * 任务类型:generate/expand/summarize
     */
    private String jobType;
    
    /**
     * 任务状态:pending/running/completed/failed/cancelled
     */
    private String status;
    
    /**
     * 输入数据
     */
    private String inputData;
    
    /**
     * 输出结果
     */
    private String outputData;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 进度百分比(0-100)
     */
    private Integer progress;
    
    /**
     * 预估token数
     */
    private Integer estimatedTokens;
    
    /**
     * 实际使用token数
     */
    private Integer actualTokens;
    
    /**
     * 预估成本(美元)
     */
    private Double estimatedCost;
    
    /**
     * 实际成本(美元)
     */
    private Double actualCost;
    
    /**
     * 创建时间
     */
    private Long createdAt;
    
    /**
     * 完成时间
     */
    private Long completedAt;
    
    /**
     * 执行时长(毫秒)
     */
    private Long duration;
}
