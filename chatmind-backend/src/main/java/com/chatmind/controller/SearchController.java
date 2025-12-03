package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.SearchRequest;
import com.chatmind.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 搜索控制器
 */
@Tag(name = "搜索功能", description = "全局搜索和文档内节点搜索接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {
    
    private final SearchService searchService;
    
    /**
     * 搜索
     */
    @Operation(summary = "搜索", description = "全局搜索或文档内搜索节点")
    @PostMapping
    public ApiResult<Map<String, Object>> search(@RequestBody SearchRequest request) {
        Map<String, Object> result = searchService.search(request);
        return ApiResult.ok(result);
    }
    
    /**
     * 快速搜索(GET方式)
     */
    @Operation(summary = "快速搜索", description = "通过GET方式快速搜索")
    @GetMapping
    public ApiResult<Map<String, Object>> quickSearch(
        @RequestParam String keyword,
        @RequestParam(required = false) Long documentId,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false, defaultValue = "1") Integer page,
        @RequestParam(required = false, defaultValue = "20") Integer pageSize
    ) {
        SearchRequest request = new SearchRequest();
        request.setKeyword(keyword);
        request.setDocumentId(documentId);
        request.setUserId(userId);
        request.setPage(page);
        request.setPageSize(pageSize);
        
        if (documentId != null) {
            request.setScope("document");
        }
        
        Map<String, Object> result = searchService.search(request);
        return ApiResult.ok(result);
    }
}
