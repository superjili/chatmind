package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.RateLimitVO;
import com.chatmind.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 限流管理控制器
 */
@Tag(name = "限流管理", description = "防滥用限流,配额管理")
@RestController
@RequestMapping("/rate-limit")
@RequiredArgsConstructor
public class RateLimitController {
    
    private final RateLimitService rateLimitService;
    
    /**
     * 获取限流状态
     */
    @Operation(summary = "限流状态", description = "获取用户限流状态")
    @GetMapping("/user/{userId}")
    public ApiResult<RateLimitVO> getRateLimit(
        @PathVariable Long userId,
        @RequestParam String limitType,
        @RequestParam(defaultValue = "hour") String timeWindow
    ) {
        RateLimitVO limit = rateLimitService.getRateLimit(userId, limitType, timeWindow);
        return ApiResult.ok(limit);
    }
    
    /**
     * 检查是否被限流
     */
    @Operation(summary = "检查限流", description = "检查用户是否被限流")
    @GetMapping("/user/{userId}/check")
    public ApiResult<Boolean> isRateLimited(
        @PathVariable Long userId,
        @RequestParam String limitType,
        @RequestParam(defaultValue = "hour") String timeWindow
    ) {
        boolean limited = rateLimitService.isRateLimited(userId, limitType, timeWindow);
        return ApiResult.ok(limited);
    }
    
    /**
     * 重置限流
     */
    @Operation(summary = "重置限流", description = "重置用户限流计数(管理员)")
    @DeleteMapping("/user/{userId}")
    public ApiResult<Void> resetLimit(
        @PathVariable Long userId,
        @RequestParam String limitType,
        @RequestParam(defaultValue = "hour") String timeWindow
    ) {
        rateLimitService.resetLimit(userId, limitType, timeWindow);
        return ApiResult.ok();
    }
}
