package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.VersionDiffVO;
import com.chatmind.service.DiffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 版本差异对比控制器
 */
@Tag(name = "版本差异", description = "版本差异对比,查看变更内容")
@RestController
@RequestMapping("/diff")
@RequiredArgsConstructor
public class DiffController {
    
    private final DiffService diffService;
    
    /**
     * 对比两个版本
     */
    @Operation(summary = "对比版本", description = "对比两个历史版本的差异")
    @GetMapping("/versions")
    public ApiResult<VersionDiffVO> compareVersions(
        @RequestParam Long fromVersionId,
        @RequestParam Long toVersionId
    ) {
        VersionDiffVO diff = diffService.compareVersions(fromVersionId, toVersionId);
        return ApiResult.ok(diff);
    }
    
    /**
     * 对比当前版本与历史版本
     */
    @Operation(summary = "对比当前版本", description = "对比当前版本与历史版本的差异")
    @GetMapping("/current")
    public ApiResult<VersionDiffVO> compareWithCurrent(
        @RequestParam Long documentId,
        @RequestParam Long versionId
    ) {
        VersionDiffVO diff = diffService.compareWithCurrent(documentId, versionId);
        return ApiResult.ok(diff);
    }
}
