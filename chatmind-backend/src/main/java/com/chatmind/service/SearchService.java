package com.chatmind.service;

import com.chatmind.dto.SearchRequest;
import com.chatmind.dto.SearchResultDTO;
import com.chatmind.entity.PmDocument;
import com.chatmind.entity.PmNode;
import com.chatmind.repository.PmDocumentRepository;
import com.chatmind.repository.PmNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索服务
 * 支持全局搜索和文档内节点搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    
    private final PmNodeRepository nodeRepository;
    private final PmDocumentRepository documentRepository;
    
    /**
     * 搜索
     */
    public Map<String, Object> search(SearchRequest request) {
        log.info("搜索: 关键词={}, 范围={}, 文档ID={}", 
            request.getKeyword(), request.getScope(), request.getDocumentId());
        
        if (request.getKeyword() == null || request.getKeyword().trim().isEmpty()) {
            return buildEmptyResult();
        }
        
        List<SearchResultDTO> results;
        
        if ("document".equals(request.getScope()) && request.getDocumentId() != null) {
            // 文档内搜索
            results = searchInDocument(request);
        } else {
            // 全局搜索
            results = searchGlobal(request);
        }
        
        // 分页
        int total = results.size();
        int start = (request.getPage() - 1) * request.getPageSize();
        int end = Math.min(start + request.getPageSize(), total);
        
        List<SearchResultDTO> pagedResults = start < total ? 
            results.subList(start, end) : Collections.emptyList();
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", request.getPage());
        result.put("pageSize", request.getPageSize());
        result.put("totalPages", (int) Math.ceil((double) total / request.getPageSize()));
        result.put("results", pagedResults);
        
        log.info("搜索完成,找到{}条结果", total);
        return result;
    }
    
    /**
     * 文档内搜索
     */
    private List<SearchResultDTO> searchInDocument(SearchRequest request) {
        log.debug("文档内搜索: {}", request.getDocumentId());
        
        // 获取文档
        PmDocument document = documentRepository.findById(request.getDocumentId()).orElse(null);
        if (document == null) {
            return Collections.emptyList();
        }
        
        // 获取文档的所有节点
        List<PmNode> nodes = nodeRepository.findByDocumentIdAndDeleted(request.getDocumentId(), 0);
        
        // 搜索匹配的节点
        List<SearchResultDTO> results = new ArrayList<>();
        String keyword = request.getKeyword().toLowerCase();
        
        for (PmNode node : nodes) {
            SearchResultDTO result = checkNodeMatch(node, document, keyword, request);
            if (result != null) {
                results.add(result);
            }
        }
        
        return results;
    }
    
    /**
     * 全局搜索
     */
    private List<SearchResultDTO> searchGlobal(SearchRequest request) {
        log.debug("全局搜索");
        
        // 获取用户有权限的文档
        List<PmDocument> documents;
        if (request.getUserId() != null) {
            documents = documentRepository.findByOwnerIdAndDeleted(request.getUserId(), 0);
        } else {
            // 如果没有用户ID,只搜索公开文档
            documents = documentRepository.findByVisibilityAndDeleted("public", 0);
        }
        
        List<SearchResultDTO> results = new ArrayList<>();
        String keyword = request.getKeyword().toLowerCase();
        
        for (PmDocument document : documents) {
            List<PmNode> nodes = nodeRepository.findByDocumentIdAndDeleted(document.getId(), 0);
            
            for (PmNode node : nodes) {
                SearchResultDTO result = checkNodeMatch(node, document, keyword, request);
                if (result != null) {
                    results.add(result);
                }
            }
        }
        
        return results;
    }
    
    /**
     * 检查节点是否匹配
     */
    private SearchResultDTO checkNodeMatch(PmNode node, PmDocument document, 
                                           String keyword, SearchRequest request) {
        boolean matched = false;
        String matchType = null;
        String highlightSnippet = null;
        
        // 搜索内容
        if (request.getSearchContent() && node.getContent() != null) {
            String content = node.getContent().toLowerCase();
            if (content.contains(keyword)) {
                matched = true;
                matchType = "content";
                highlightSnippet = generateHighlight(node.getContent(), keyword);
            }
        }
        
        // 搜索描述
        if (!matched && request.getSearchDescription() && node.getDescription() != null) {
            String description = node.getDescription().toLowerCase();
            if (description.contains(keyword)) {
                matched = true;
                matchType = "description";
                highlightSnippet = generateHighlight(node.getDescription(), keyword);
            }
        }
        
        // 搜索标签
        if (!matched && request.getSearchLabels() && node.getLabels() != null) {
            String labels = node.getLabels().toLowerCase();
            if (labels.contains(keyword)) {
                matched = true;
                matchType = "label";
                highlightSnippet = node.getLabels();
            }
        }
        
        if (!matched) {
            return null;
        }
        
        // 构建搜索结果
        SearchResultDTO result = new SearchResultDTO();
        result.setNodeId(node.getId());
        result.setContent(node.getContent());
        result.setDescription(node.getDescription());
        result.setLabels(node.getLabels());
        result.setDocumentId(document.getId());
        result.setDocumentTitle(document.getTitle());
        result.setParentId(node.getParentId());
        result.setMatchType(matchType);
        result.setHighlightSnippet(highlightSnippet);
        result.setDepth(node.getDepth());
        result.setCollapsed(node.getCollapsed());
        
        // 生成面包屑导航
        result.setBreadcrumb(generateBreadcrumb(node));
        
        return result;
    }
    
    /**
     * 生成高亮片段
     */
    private String generateHighlight(String text, String keyword) {
        if (text == null || keyword == null) {
            return "";
        }
        
        int index = text.toLowerCase().indexOf(keyword.toLowerCase());
        if (index < 0) {
            return text.length() > 100 ? text.substring(0, 100) + "..." : text;
        }
        
        // 获取关键词前后的上下文
        int start = Math.max(0, index - 50);
        int end = Math.min(text.length(), index + keyword.length() + 50);
        
        String snippet = (start > 0 ? "..." : "") + 
                        text.substring(start, end) + 
                        (end < text.length() ? "..." : "");
        
        // 添加高亮标记
        String highlightedKeyword = "<mark>" + 
            text.substring(index, index + keyword.length()) + "</mark>";
        
        return snippet.replaceAll(
            "(?i)" + keyword, 
            highlightedKeyword
        );
    }
    
    /**
     * 生成面包屑导航
     */
    private List<String> generateBreadcrumb(PmNode node) {
        List<String> breadcrumb = new ArrayList<>();
        
        Long currentId = node.getParentId();
        int maxDepth = 10; // 防止无限循环
        int depth = 0;
        
        while (currentId != null && depth < maxDepth) {
            PmNode parent = nodeRepository.findById(currentId).orElse(null);
            if (parent == null) break;
            
            breadcrumb.add(0, parent.getContent());
            currentId = parent.getParentId();
            depth++;
        }
        
        breadcrumb.add(node.getContent());
        
        return breadcrumb;
    }
    
    /**
     * 构建空结果
     */
    private Map<String, Object> buildEmptyResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", 0);
        result.put("page", 1);
        result.put("pageSize", 20);
        result.put("totalPages", 0);
        result.put("results", Collections.emptyList());
        return result;
    }
}
