package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.AIJobVO;
import com.chatmind.service.AIJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI任务管理控制器
 */
@Tag(name = "AI任务", description = "AI任务流程管理,成本追踪")
@RestController
@RequestMapping("/ai-jobs")
@RequiredArgsConstructor
public class AIJobController {
    
    private final AIJobService aiJobService;
    
    /**
     * 获取任务详情
     */
    @Operation(summary = "任务详情", description = "获取AI任务详情")
    @GetMapping("/{jobId}")
    public ApiResult<AIJobVO> getJob(@PathVariable Long jobId) {
        AIJobVO job = aiJobService.getJob(jobId);
        return ApiResult.ok(job);
    }
    
    /**
     * 获取用户任务列表
     */
    @Operation(summary = "用户任务", description = "获取用户的AI任务列表")
    @GetMapping("/user/{userId}")
    public ApiResult<List<AIJobVO>> getUserJobs(
        @PathVariable Long userId,
        @RequestParam(required = false, defaultValue = "20") Integer limit
    ) {
        List<AIJobVO> jobs = aiJobService.getUserJobs(userId, limit);
        return ApiResult.ok(jobs);
    }
    
    /**
     * 获取文档任务列表
     */
    @Operation(summary = "文档任务", description = "获取文档的AI任务列表")
    @GetMapping("/document/{documentId}")
    public ApiResult<List<AIJobVO>> getDocumentJobs(@PathVariable Long documentId) {
        List<AIJobVO> jobs = aiJobService.getDocumentJobs(documentId);
        return ApiResult.ok(jobs);
    }
    
    /**
     * 取消任务
     */
    @Operation(summary = "取消任务", description = "取消执行中的AI任务")
    @PostMapping("/{jobId}/cancel")
    public ApiResult<Void> cancelJob(@PathVariable Long jobId) {
        aiJobService.cancelJob(jobId);
        return ApiResult.ok();
    }
    
    /**
     * 统计用户成本
     */
    @Operation(summary = "成本统计", description = "统计用户AI调用成本")
    @GetMapping("/user/{userId}/cost")
    public ApiResult<Double> calculateUserCost(
        @PathVariable Long userId,
        @RequestParam(required = false) LocalDateTime startTime,
        @RequestParam(required = false) LocalDateTime endTime
    ) {
        Double cost = aiJobService.calculateUserCost(userId, startTime, endTime);
        return ApiResult.ok(cost);
    }
}
