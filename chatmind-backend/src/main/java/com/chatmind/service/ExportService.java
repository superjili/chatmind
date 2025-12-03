package com.chatmind.service;

import com.chatmind.dto.ExportRequest;
import com.chatmind.entity.PmDocument;
import com.chatmind.entity.PmNode;
import com.chatmind.repository.PmDocumentRepository;
import com.chatmind.repository.PmNodeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 导出服务
 * 支持导出为Markdown、OPML、JSON、PNG、SVG、PDF格式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {
    
    private final PmDocumentRepository documentRepository;
    private final PmNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 导出文档
     */
    public String exportDocument(ExportRequest request) {
        log.info("导出文档: {}, 格式: {}", request.getDocumentId(), request.getFormat());
        
        PmDocument document = documentRepository.findById(request.getDocumentId())
            .orElseThrow(() -> new RuntimeException("文档不存在: " + request.getDocumentId()));
        
        // 查询所有未删除的节点
        List<PmNode> nodes = nodeRepository.findByDocumentIdAndDeleted(request.getDocumentId(), 0);
        log.info("查询到节点数量: {}", nodes.size());
        
        if (nodes.isEmpty()) {
            log.warn("文档没有节点数据: {}", request.getDocumentId());
            return "文档没有节点数据";
        }
        
        // 输出前10个节点详情用于调试
        log.info("=== 节点数据详情 ===");
        for (int i = 0; i < Math.min(10, nodes.size()); i++) {
            PmNode n = nodes.get(i);
            log.info("节点[{}]: ID={}, parentId={}, content='{}', deleted={}", 
                i, n.getId(), n.getParentId(), n.getContent(), n.getDeleted());
        }
        log.info("====================");
        
        switch (request.getFormat().toLowerCase()) {
            case "markdown":
                return exportMarkdown(document, nodes);
            case "opml":
                return exportOPML(document, nodes);
            case "json":
                return exportJSON(document, nodes);
            case "png":
            case "svg":
            case "pdf":
                // 图片格式需要前端渲染,这里返回JSON供前端使用
                return exportJSON(document, nodes);
            default:
                throw new RuntimeException("不支持的导出格式: " + request.getFormat());
        }
    }
    
    /**
     * 导出为Markdown格式
     */
    private String exportMarkdown(PmDocument document, List<PmNode> nodes) {
        log.debug("导出Markdown格式");
        
        StringBuilder md = new StringBuilder();
        md.append("# ").append(document.getTitle()).append("\n\n");
        
        // 构建节点层级关系
        Map<Long, List<PmNode>> childrenMap = buildChildrenMap(nodes);
        
        // 找到所有根节点
        List<PmNode> rootNodes = nodes.stream()
            .filter(n -> n.getParentId() == null)
            .sorted((n1, n2) -> n1.getId().compareTo(n2.getId()))
            .toList();
        
        log.info("找到 {} 个根节点", rootNodes.size());
        
        // 导出所有根节点
        for (PmNode rootNode : rootNodes) {
            exportMarkdownRecursive(rootNode, childrenMap, md, 1);
        }
        
        return md.toString();
    }
    
    /**
     * 递归导出Markdown
     */
    private void exportMarkdownRecursive(PmNode node, Map<Long, List<PmNode>> childrenMap, 
                                         StringBuilder md, int level) {
        // 添加标题
        md.append("#".repeat(Math.min(level + 1, 6))).append(" ");
        md.append(node.getContent()).append("\n");
        
        // 添加描述
        if (node.getDescription() != null && !node.getDescription().isEmpty()) {
            md.append("\n").append(node.getDescription()).append("\n");
        }
        
        md.append("\n");
        
        // 递归处理子节点
        List<PmNode> children = childrenMap.getOrDefault(node.getId(), Collections.emptyList());
        log.debug("节点 {} (内容:{}) 有 {} 个子节点", 
            node.getId(), node.getContent(), children.size());
        
        for (PmNode child : children) {
            exportMarkdownRecursive(child, childrenMap, md, level + 1);
        }
    }
    
    /**
     * 导出为OPML格式
     */
    private String exportOPML(PmDocument document, List<PmNode> nodes) {
        log.debug("导出OPML格式");
        
        StringBuilder opml = new StringBuilder();
        opml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        opml.append("<opml version=\"2.0\">\n");
        opml.append("  <head>\n");
        opml.append("    <title>").append(escapeXml(document.getTitle())).append("</title>\n");
        opml.append("  </head>\n");
        opml.append("  <body>\n");
        
        Map<Long, List<PmNode>> childrenMap = buildChildrenMap(nodes);
        
        // 找到所有根节点
        List<PmNode> rootNodes = nodes.stream()
            .filter(n -> n.getParentId() == null)
            .sorted((n1, n2) -> n1.getId().compareTo(n2.getId()))
            .toList();
        
        // 导出所有根节点
        for (PmNode rootNode : rootNodes) {
            exportOPMLRecursive(rootNode, childrenMap, opml, 2);
        }
        
        opml.append("  </body>\n");
        opml.append("</opml>");
        
        return opml.toString();
    }
    
    /**
     * 递归导出OPML
     */
    private void exportOPMLRecursive(PmNode node, Map<Long, List<PmNode>> childrenMap, 
                                     StringBuilder opml, int indent) {
        String indentStr = "  ".repeat(indent);
        
        List<PmNode> children = childrenMap.getOrDefault(node.getId(), Collections.emptyList());
        
        if (children.isEmpty()) {
            opml.append(indentStr).append("<outline text=\"")
                .append(escapeXml(node.getContent())).append("\" />\n");
        } else {
            opml.append(indentStr).append("<outline text=\"")
                .append(escapeXml(node.getContent())).append("\">\n");
            
            for (PmNode child : children) {
                exportOPMLRecursive(child, childrenMap, opml, indent + 1);
            }
            
            opml.append(indentStr).append("</outline>\n");
        }
    }
    
    /**
     * 导出为JSON格式
     */
    private String exportJSON(PmDocument document, List<PmNode> nodes) {
        log.debug("导出JSON格式");
        
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("documentId", document.getId());
            result.put("title", document.getTitle());
            result.put("theme", document.getTheme());
            result.put("visibility", document.getVisibility());
            
            Map<Long, List<PmNode>> childrenMap = buildChildrenMap(nodes);
            
            // 找到所有根节点
            List<PmNode> rootNodes = nodes.stream()
                .filter(n -> n.getParentId() == null)
                .sorted((n1, n2) -> n1.getId().compareTo(n2.getId()))
                .toList();
            
            // 导出所有根节点
            List<Map<String, Object>> rootNodesList = new ArrayList<>();
            for (PmNode rootNode : rootNodes) {
                rootNodesList.add(buildNodeTree(rootNode, childrenMap));
            }
            result.put("nodes", rootNodesList);
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            log.error("导出JSON失败", e);
            throw new RuntimeException("导出JSON失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建节点树(用于JSON导出)
     */
    private Map<String, Object> buildNodeTree(PmNode node, Map<Long, List<PmNode>> childrenMap) {
        Map<String, Object> nodeMap = new HashMap<>();
        nodeMap.put("id", node.getId());
        nodeMap.put("content", node.getContent());
        nodeMap.put("color", node.getColor());
        nodeMap.put("icon", node.getIcon());
        nodeMap.put("description", node.getDescription());
        nodeMap.put("labels", node.getLabels());
        nodeMap.put("collapsed", node.getCollapsed());
        nodeMap.put("position", node.getPosition());
        nodeMap.put("depth", node.getDepth());
        
        List<PmNode> children = childrenMap.getOrDefault(node.getId(), Collections.emptyList());
        if (!children.isEmpty()) {
            List<Map<String, Object>> childrenList = new ArrayList<>();
            for (PmNode child : children) {
                childrenList.add(buildNodeTree(child, childrenMap));
            }
            nodeMap.put("children", childrenList);
        }
        
        return nodeMap;
    }
    
    /**
     * 构建子节点映射
     */
    private Map<Long, List<PmNode>> buildChildrenMap(List<PmNode> nodes) {
        Map<Long, List<PmNode>> childrenMap = new HashMap<>();
        
        log.info("=== 开始构建子节点映射，节点总数: {} ===", nodes.size());
        
        int rootCount = 0;
        int childCount = 0;
        
        for (PmNode node : nodes) {
            if (node.getParentId() != null) {
                childrenMap.computeIfAbsent(node.getParentId(), k -> new ArrayList<>())
                    .add(node);
                childCount++;
                log.info("子节点: ID={}, parentId={}, content='{}'", 
                    node.getId(), node.getParentId(), node.getContent());
            } else {
                rootCount++;
                log.info("根节点: ID={}, content='{}'", node.getId(), node.getContent());
            }
        }
        
        log.info("=== 映射构建完成: 根节点{}个, 子节点{}个, 父节点数{} ===", 
            rootCount, childCount, childrenMap.size());
        
        // 输出每个父节点的子节点数量
        for (Map.Entry<Long, List<PmNode>> entry : childrenMap.entrySet()) {
            log.info("父节点[{}] -> {} 个子节点", entry.getKey(), entry.getValue().size());
        }
        
        // 按ID排序子节点
        for (List<PmNode> children : childrenMap.values()) {
            children.sort((n1, n2) -> n1.getId().compareTo(n2.getId()));
        }
        
        return childrenMap;
    }
    
    /**
     * XML转义
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
