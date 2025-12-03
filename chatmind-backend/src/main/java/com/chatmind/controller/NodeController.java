package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.CreateNodeRequest;
import com.chatmind.dto.MoveNodeRequest;
import com.chatmind.dto.NodeVO;
import com.chatmind.service.NodeMoveService;
import com.chatmind.service.NodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 节点管理控制器
 */
@Tag(name = "节点管理", description = "节点的创建、查询、更新、删除、批量操作接口")
@RestController
@RequestMapping("/nodes")
@RequiredArgsConstructor
public class NodeController {
    
    private final NodeService nodeService;
    private final NodeMoveService nodeMoveService;
    
    /**
     * 创建节点
     */
    @Operation(summary = "创建节点", description = "创建新的脑图节点")
    @PostMapping
    public ApiResult<NodeVO> createNode(@Valid @RequestBody CreateNodeRequest request) {
        NodeVO node = nodeService.createNode(request);
        return ApiResult.ok(node);
    }
    
    /**
     * 获取节点详情
     */
    @Operation(summary = "获取节点详情", description = "根据ID获取节点详细信息")
    @GetMapping("/{id}")
    public ApiResult<NodeVO> getNode(@PathVariable Long id) {
        NodeVO node = nodeService.getNodeById(id);
        return ApiResult.ok(node);
    }
    
    /**
     * 获取文档的所有节点
     */
    @Operation(summary = "获取文档节点列表", description = "获取指定文档的所有节点")
    @GetMapping("/document/{documentId}")
    public ApiResult<List<NodeVO>> getDocumentNodes(@PathVariable Long documentId) {
        List<NodeVO> nodes = nodeService.getNodesByDocumentId(documentId);
        return ApiResult.ok(nodes);
    }
    
    /**
     * 获取子节点列表
     */
    @Operation(summary = "获取子节点列表", description = "获取指定节点的所有子节点")
    @GetMapping("/{parentId}/children")
    public ApiResult<List<NodeVO>> getChildNodes(@PathVariable Long parentId) {
        List<NodeVO> nodes = nodeService.getChildNodes(parentId);
        return ApiResult.ok(nodes);
    }
    
    /**
     * 更新节点
     */
    @Operation(summary = "更新节点", description = "更新节点内容和属性")
    @PutMapping("/{id}")
    public ApiResult<NodeVO> updateNode(
        @PathVariable Long id,
        @Valid @RequestBody CreateNodeRequest request
    ) {
        NodeVO node = nodeService.updateNode(id, request);
        return ApiResult.ok(node);
    }
    
    /**
     * 批量更新节点
     */
    @Operation(summary = "批量更新节点", description = "批量设置节点颜色或标签")
    @PutMapping("/batch")
    public ApiResult<List<NodeVO>> batchUpdateNodes(
        @RequestParam List<Long> nodeIds,
        @RequestParam(required = false) String color,
        @RequestParam(required = false) String labels
    ) {
        List<NodeVO> nodes = nodeService.batchUpdateNodes(nodeIds, color, labels);
        return ApiResult.ok(nodes);
    }
    
    /**
     * 删除节点
     */
    @Operation(summary = "删除节点", description = "逻辑删除节点及其子节点")
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteNode(@PathVariable Long id) {
        nodeService.deleteNode(id);
        return ApiResult.ok();
    }
    
    /**
     * 切换折叠状态
     */
    @Operation(summary = "切换折叠状态", description = "展开或折叠节点")
    @PutMapping("/{id}/toggle-collapse")
    public ApiResult<NodeVO> toggleCollapse(@PathVariable Long id) {
        NodeVO node = nodeService.toggleCollapse(id);
        return ApiResult.ok(node);
    }
    
    /**
     * 移动节点(拖拽)
     */
    @Operation(summary = "移动节点", description = "拖拽节点到新位置,支持改变父节点")
    @PutMapping("/move")
    public ApiResult<NodeVO> moveNode(@Valid @RequestBody MoveNodeRequest request) {
        NodeVO node = nodeMoveService.moveNode(request);
        return ApiResult.ok(node);
    }
    
    /**
     * 重新排序子节点
     */
    @Operation(summary = "重新排序子节点", description = "调整子节点的顺序")
    @PutMapping("/{parentId}/reorder")
    public ApiResult<Void> reorderChildren(
        @PathVariable Long parentId,
        @RequestBody List<Long> orderedChildrenIds
    ) {
        nodeMoveService.reorderChildren(parentId, orderedChildrenIds);
        return ApiResult.ok();
    }
}
