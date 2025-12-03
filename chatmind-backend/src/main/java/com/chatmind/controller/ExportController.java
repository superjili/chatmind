package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.DocumentVO;
import com.chatmind.dto.ExportRequest;
import com.chatmind.dto.ImportRequest;
import com.chatmind.service.ExportService;
import com.chatmind.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 导入导出控制器
 */
@Tag(name = "导入导出", description = "文档的导入导出功能接口")
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {
    
    private final ExportService exportService;
    private final ImportService importService;
    
    /**
     * 导出文档
     */
    @Operation(summary = "导出文档", description = "导出文档为Markdown/OPML/JSON等格式")
    @PostMapping
    public ResponseEntity<byte[]> exportDocument(@Valid @RequestBody ExportRequest request) {
        String content = exportService.exportDocument(request);
        
        // 根据格式设置响应头
        String filename = "document_" + request.getDocumentId();
        String contentType = "text/plain";
        
        switch (request.getFormat().toLowerCase()) {
            case "markdown":
                filename += ".md";
                contentType = "text/markdown";
                break;
            case "opml":
                filename += ".opml";
                contentType = "text/xml";
                break;
            case "json":
                filename += ".json";
                contentType = "application/json";
                break;
        }
        
        // 返回文件流
        byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .header(HttpHeaders.CONTENT_TYPE, contentType + "; charset=UTF-8")
            .contentLength(bytes.length)
            .body(bytes);
    }
    
    /**
     * 导入文档
     */
    @Operation(summary = "导入文档", description = "从Markdown/OPML导入文档")
    @PostMapping("/import")
    public ApiResult<DocumentVO> importDocument(@Valid @RequestBody ImportRequest request) {
        DocumentVO document = importService.importDocument(request);
        return ApiResult.ok(document);
    }
}
