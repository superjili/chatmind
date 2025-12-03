package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.CreateVersionRequest;
import com.chatmind.dto.DocumentVO;
import com.chatmind.dto.RestoreVersionRequest;
import com.chatmind.dto.VersionVO;
import com.chatmind.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 版本管理控制器
 */
@Tag(name = "版本管理", description = "文档版本快照和历史管理接口")
@RestController
@RequestMapping("/versions")
@RequiredArgsConstructor
public class VersionController {
    
    private final VersionService versionService;
    
    /**
     * 创建版本快照
     */
    @Operation(summary = "创建版本", description = "创建文档版本快照")
    @PostMapping
    public ApiResult<VersionVO> createVersion(@Valid @RequestBody CreateVersionRequest request) {
        VersionVO version = versionService.createVersion(request);
        return ApiResult.ok(version);
    }
    
    /**
     * 获取文档版本列表
     */
    @Operation(summary = "版本列表", description = "获取文档的所有版本")
    @GetMapping("/document/{documentId}")
    public ApiResult<List<VersionVO>> getVersions(
        @PathVariable Long documentId,
        @RequestParam(required = false) String versionType
    ) {
        List<VersionVO> versions = versionService.getVersions(documentId, versionType);
        return ApiResult.ok(versions);
    }
    
    /**
     * 获取版本详情
     */
    @Operation(summary = "版本详情", description = "获取指定版本的详细信息")
    @GetMapping("/{versionId}")
    public ApiResult<VersionVO> getVersion(@PathVariable Long versionId) {
        VersionVO version = versionService.getVersion(versionId);
        return ApiResult.ok(version);
    }
    
    /**
     * 删除版本
     */
    @Operation(summary = "删除版本", description = "删除指定版本(不能删除最新版本)")
    @DeleteMapping("/{versionId}")
    public ApiResult<Void> deleteVersion(@PathVariable Long versionId) {
        versionService.deleteVersion(versionId);
        return ApiResult.ok();
    }
    
    /**
     * 手动触发自动保存
     */
    @Operation(summary = "自动保存", description = "手动触发文档自动保存")
    @PostMapping("/autosave")
    public ApiResult<Void> autoSave(
        @RequestParam Long documentId,
        @RequestParam Long userId
    ) {
        versionService.autoSave(documentId, userId);
        return ApiResult.ok();
    }
    
    /**
     * 恢复到指定版本
     */
    @Operation(summary = "恢复版本", description = "将文档恢复到指定版本")
    @PostMapping("/restore")
    public ApiResult<DocumentVO> restoreVersion(@Valid @RequestBody RestoreVersionRequest request) {
        DocumentVO document = versionService.restoreVersion(request);
        return ApiResult.ok(document);
    }
}
