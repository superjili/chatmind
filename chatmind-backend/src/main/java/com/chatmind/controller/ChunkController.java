package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.ChunkVO;
import com.chatmind.dto.NodeVO;
import com.chatmind.service.ChunkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档分片控制器
 */
@Tag(name = "文档分片", description = "文档分片存储,支持懒加载")
@RestController
@RequestMapping("/chunks")
@RequiredArgsConstructor
public class ChunkController {
    
    private final ChunkService chunkService;
    
    /**
     * 获取根分片
     */
    @Operation(summary = "根分片", description = "获取文档根分片(第一层)")
    @GetMapping("/document/{documentId}/root")
    public ApiResult<ChunkVO> getRootChunk(@PathVariable Long documentId) {
        ChunkVO chunk = chunkService.getRootChunk(documentId);
        return ApiResult.ok(chunk);
    }
    
    /**
     * 获取子树分片
     */
    @Operation(summary = "子树分片", description = "获取指定节点的子树分片")
    @GetMapping("/document/{documentId}/subtree/{nodeId}")
    public ApiResult<ChunkVO> getSubtreeChunk(
        @PathVariable Long documentId,
        @PathVariable Long nodeId
    ) {
        ChunkVO chunk = chunkService.getSubtreeChunk(documentId, nodeId);
        return ApiResult.ok(chunk);
    }
    
    /**
     * 按层级加载
     */
    @Operation(summary = "按层级加载", description = "按层级范围懒加载节点")
    @GetMapping("/document/{documentId}/levels")
    public ApiResult<List<NodeVO>> loadNodesByLevel(
        @PathVariable Long documentId,
        @RequestParam Integer fromLevel,
        @RequestParam Integer toLevel
    ) {
        List<NodeVO> nodes = chunkService.loadNodesByLevel(documentId, fromLevel, toLevel);
        return ApiResult.ok(nodes);
    }
    
    /**
     * 清除缓存
     */
    @Operation(summary = "清除缓存", description = "清除文档分片缓存")
    @DeleteMapping("/document/{documentId}/cache")
    public ApiResult<Void> clearCache(@PathVariable Long documentId) {
        chunkService.clearCache(documentId);
        return ApiResult.ok();
    }
}
