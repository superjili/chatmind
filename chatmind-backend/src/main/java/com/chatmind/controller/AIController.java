package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.DocumentVO;
import com.chatmind.dto.GenerateMindMapRequest;
import com.chatmind.dto.ExpandNodeRequest;
import com.chatmind.dto.SummarizeRequest;
import com.chatmind.dto.NodeVO;
import com.chatmind.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI功能控制器
 */
@Tag(name = "AI功能", description = "AI生成脑图、智能扩展、自动总结等接口")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {
    
    private final AIService aiService;
    
    /**
     * 从文字生成脑图
     */
    @Operation(summary = "生成脑图", description = "从文本自动生成脑图结构")
    @PostMapping("/generate")
    public ApiResult<DocumentVO> generateMindMap(@Valid @RequestBody GenerateMindMapRequest request) {
        DocumentVO document = aiService.generateMindMap(request);
        return ApiResult.ok(document);
    }
    
    /**
     * 智能扩展节点
     */
    @Operation(summary = "智能扩展节点", description = "基于上下文为节点生成子节点")
    @PostMapping("/expand")
    public ApiResult<List<NodeVO>> expandNode(@Valid @RequestBody ExpandNodeRequest request) {
        List<NodeVO> nodes = aiService.expandNode(request.getNodeId(), request.getUserId(), 
            request.getCount() != null ? request.getCount() : 5);
        return ApiResult.ok(nodes);
    }
    
    /**
     * 自动总结
     */
    @Operation(summary = "自动总结", description = "对文档或子树进行智能总结")
    @PostMapping("/summarize")
    public ApiResult<String> summarize(@Valid @RequestBody SummarizeRequest request) {
        String summary = aiService.summarize(request.getDocumentId(), request.getNodeId(), 
            request.getUserId(), request.getSummaryType() != null ? request.getSummaryType() : "brief");
        return ApiResult.ok(summary);
    }
}
