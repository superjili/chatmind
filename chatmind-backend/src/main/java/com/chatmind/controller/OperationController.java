package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.OperationVO;
import com.chatmind.dto.RecordOperationRequest;
import com.chatmind.service.OperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作记录控制器
 */
@Tag(name = "操作记录", description = "文档编辑操作记录,支持CRDT实时协作")
@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationController {
    
    private final OperationService operationService;
    
    /**
     * 记录操作
     */
    @Operation(summary = "记录操作", description = "记录文档编辑操作(幂等)")
    @PostMapping
    public ApiResult<OperationVO> recordOperation(@Valid @RequestBody RecordOperationRequest request) {
        OperationVO operation = operationService.recordOperation(request);
        return ApiResult.ok(operation);
    }
    
    /**
     * 获取操作历史
     */
    @Operation(summary = "操作历史", description = "获取文档的操作历史记录")
    @GetMapping("/document/{documentId}")
    public ApiResult<List<OperationVO>> getOperations(
        @PathVariable Long documentId,
        @RequestParam(required = false) LocalDateTime startTime,
        @RequestParam(required = false) LocalDateTime endTime
    ) {
        List<OperationVO> operations = operationService.getOperations(documentId, startTime, endTime);
        return ApiResult.ok(operations);
    }
    
    /**
     * 获取最近操作
     */
    @Operation(summary = "最近操作", description = "获取文档的最近操作记录")
    @GetMapping("/document/{documentId}/recent")
    public ApiResult<List<OperationVO>> getRecentOperations(
        @PathVariable Long documentId,
        @RequestParam(required = false, defaultValue = "100") Integer limit
    ) {
        List<OperationVO> operations = operationService.getRecentOperations(documentId, limit);
        return ApiResult.ok(operations);
    }
    
    /**
     * 获取回放操作
     */
    @Operation(summary = "回放操作", description = "获取指定时间戳范围的操作(用于版本恢复)")
    @GetMapping("/document/{documentId}/replay")
    public ApiResult<List<OperationVO>> getOperationsForReplay(
        @PathVariable Long documentId,
        @RequestParam Long fromTimestamp,
        @RequestParam Long toTimestamp
    ) {
        List<OperationVO> operations = operationService.getOperationsForReplay(
            documentId, fromTimestamp, toTimestamp);
        return ApiResult.ok(operations);
    }
    
    /**
     * 清理旧操作
     */
    @Operation(summary = "清理旧操作", description = "清理30天前的旧操作记录")
    @DeleteMapping("/document/{documentId}/cleanup")
    public ApiResult<Void> cleanupOldOperations(@PathVariable Long documentId) {
        operationService.cleanupOldOperations(documentId);
        return ApiResult.ok();
    }
}
