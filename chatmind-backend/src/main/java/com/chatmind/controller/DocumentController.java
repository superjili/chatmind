package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.CreateDocumentRequest;
import com.chatmind.dto.UpdateDocumentRequest;
import com.chatmind.dto.DocumentVO;
import com.chatmind.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档管理控制器
 */
@Tag(name = "文档管理", description = "文档的创建、查询、更新、删除接口")
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    
    /**
     * 创建文档
     */
    @Operation(summary = "创建文档", description = "创建新的脑图文档")
    @PostMapping
    public ApiResult<DocumentVO> createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        DocumentVO document = documentService.createDocument(request);
        return ApiResult.ok(document);
    }
    
    /**
     * 获取文档详情
     */
    @Operation(summary = "获取文档详情", description = "根据ID获取文档详细信息")
    @GetMapping("/{id}")
    public ApiResult<DocumentVO> getDocument(@PathVariable Long id) {
        DocumentVO document = documentService.getDocumentById(id);
        return ApiResult.ok(document);
    }
    
    /**
     * 获取用户的文档列表
     */
    @Operation(summary = "获取用户文档列表", description = "获取指定用户的所有文档")
    @GetMapping("/user/{ownerId}")
    public ApiResult<List<DocumentVO>> getUserDocuments(@PathVariable Long ownerId) {
        List<DocumentVO> documents = documentService.getDocumentsByOwnerId(ownerId);
        return ApiResult.ok(documents);
    }
    
    /**
     * 更新文档
     */
    @Operation(summary = "更新文档", description = "更新文档基本信息")
    @PutMapping("/{id}")
    public ApiResult<DocumentVO> updateDocument(
        @PathVariable Long id,
        @Valid @RequestBody UpdateDocumentRequest request
    ) {
        DocumentVO document = documentService.updateDocument(id, request);
        return ApiResult.ok(document);
    }
    
    /**
     * 删除文档
     */
    @Operation(summary = "删除文档", description = "逻辑删除文档")
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ApiResult.ok();
    }
}
