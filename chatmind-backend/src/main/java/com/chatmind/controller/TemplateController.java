package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.CreateTemplateRequest;
import com.chatmind.dto.DocumentVO;
import com.chatmind.dto.TemplateVO;
import com.chatmind.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板控制器
 */
@Tag(name = "模板管理", description = "会议、SWOT、产品规划等预定义模板接口")
@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {
    
    private final TemplateService templateService;
    
    /**
     * 创建模板
     */
    @Operation(summary = "创建模板", description = "创建新的文档模板")
    @PostMapping
    public ApiResult<TemplateVO> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        TemplateVO template = templateService.createTemplate(request);
        return ApiResult.ok(template);
    }
    
    /**
     * 获取模板列表
     */
    @Operation(summary = "模板列表", description = "获取所有可用模板")
    @GetMapping
    public ApiResult<List<TemplateVO>> getTemplates(
        @RequestParam(required = false) String templateType,
        @RequestParam(required = false) Long userId
    ) {
        List<TemplateVO> templates = templateService.getTemplates(templateType, userId);
        return ApiResult.ok(templates);
    }
    
    /**
     * 获取模板详情
     */
    @Operation(summary = "模板详情", description = "获取指定模板的详细信息")
    @GetMapping("/{templateId}")
    public ApiResult<TemplateVO> getTemplate(@PathVariable Long templateId) {
        TemplateVO template = templateService.getTemplate(templateId);
        return ApiResult.ok(template);
    }
    
    /**
     * 从模板创建文档
     */
    @Operation(summary = "从模板创建", description = "使用模板创建新文档")
    @PostMapping("/{templateId}/create")
    public ApiResult<DocumentVO> createFromTemplate(
        @PathVariable Long templateId,
        @RequestParam(required = false) String title,
        @RequestParam Long userId
    ) {
        DocumentVO document = templateService.createDocumentFromTemplate(templateId, title, userId);
        return ApiResult.ok(document);
    }
    
    /**
     * 删除模板
     */
    @Operation(summary = "删除模板", description = "删除指定模板")
    @DeleteMapping("/{templateId}")
    public ApiResult<Void> deleteTemplate(@PathVariable Long templateId) {
        templateService.deleteTemplate(templateId);
        return ApiResult.ok();
    }
}
