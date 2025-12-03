package com.chatmind.service;

import com.chatmind.dto.DocumentVO;
import com.chatmind.dto.ImportRequest;
import com.chatmind.entity.PmDocument;
import com.chatmind.entity.PmNode;
import com.chatmind.repository.PmDocumentRepository;
import com.chatmind.repository.PmNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 导入服务
 * 支持从Markdown、OPML导入文档
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {
    
    private final PmDocumentRepository documentRepository;
    private final PmNodeRepository nodeRepository;
    
    /**
     * 导入文档
     */
    @Transactional
    public DocumentVO importDocument(ImportRequest request) {
        log.info("导入文档: 格式={}, 用户={}", request.getFormat(), request.getUserId());
        
        switch (request.getFormat().toLowerCase()) {
            case "markdown":
                return importMarkdown(request);
            case "opml":
                return importOPML(request);
            default:
                throw new RuntimeException("不支持的导入格式: " + request.getFormat());
        }
    }
    
    /**
     * 导入Markdown
     */
    private DocumentVO importMarkdown(ImportRequest request) {
        log.debug("导入Markdown格式");
        
        String content = request.getContent();
        String[] lines = content.split("\\r?\\n");
        
        // 提取标题(第一个一级标题)
        String title = request.getTitle();
        if (title == null) {
            for (String line : lines) {
                if (line.startsWith("# ")) {
                    title = line.substring(2).trim();
                    break;
                }
            }
        }
        if (title == null) {
            title = "导入的文档";
        }
        
        // 创建文档
        PmDocument document = new PmDocument();
        document.setTitle(title);
        document.setOwnerId(request.getUserId());
        document.setTheme(request.getTheme());
        document.setVisibility(request.getVisibility());
        document = documentRepository.save(document);
        
        // 解析节点层级
        List<NodeInfo> nodeInfos = parseMarkdownNodes(lines);
        
        // 创建节点树
        PmNode rootNode = createNodesFromMarkdown(document.getId(), nodeInfos);
        
        // 构建返回值
        DocumentVO vo = new DocumentVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setOwnerId(document.getOwnerId());
        vo.setTheme(document.getTheme());
        vo.setVisibility(document.getVisibility());
        vo.setCreatedAt(document.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        vo.setUpdatedAt(document.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        
        log.info("Markdown导入成功,文档ID: {}", document.getId());
        return vo;
    }
    
    /**
     * 解析Markdown节点
     */
    private List<NodeInfo> parseMarkdownNodes(String[] lines) {
        List<NodeInfo> nodeInfos = new ArrayList<>();
        Pattern headerPattern = Pattern.compile("^(#{1,6})\\s+(.+)$");
        
        StringBuilder contentBuilder = null;
        NodeInfo currentNode = null;
        
        for (String line : lines) {
            Matcher matcher = headerPattern.matcher(line);
            
            if (matcher.matches()) {
                // 保存前一个节点的描述
                if (currentNode != null && contentBuilder != null) {
                    currentNode.setDescription(contentBuilder.toString().trim());
                }
                
                // 创建新节点
                int level = matcher.group(1).length();
                String text = matcher.group(2).trim();
                
                currentNode = new NodeInfo();
                currentNode.setLevel(level);
                currentNode.setContent(text);
                nodeInfos.add(currentNode);
                
                contentBuilder = new StringBuilder();
            } else if (currentNode != null && !line.trim().isEmpty()) {
                // 累积描述内容
                if (contentBuilder.length() > 0) {
                    contentBuilder.append("\n");
                }
                contentBuilder.append(line);
            }
        }
        
        // 保存最后一个节点的描述
        if (currentNode != null && contentBuilder != null) {
            currentNode.setDescription(contentBuilder.toString().trim());
        }
        
        return nodeInfos;
    }
    
    /**
     * 从Markdown节点信息创建数据库节点
     */
    private PmNode createNodesFromMarkdown(Long documentId, List<NodeInfo> nodeInfos) {
        if (nodeInfos.isEmpty()) {
            // 创建默认根节点
            PmNode root = new PmNode();
            root.setDocumentId(documentId);
            root.setContent("根节点");
            root.setDepth(0);
            return nodeRepository.save(root);
        }
        
        // 用于跟踪每个层级的最后一个节点
        PmNode[] levelNodes = new PmNode[10];
        PmNode rootNode = null;
        
        for (NodeInfo nodeInfo : nodeInfos) {
            PmNode node = new PmNode();
            node.setDocumentId(documentId);
            node.setContent(nodeInfo.getContent());
            node.setDescription(nodeInfo.getDescription());
            node.setDepth(nodeInfo.getLevel() - 1);
            
            // 确定父节点
            if (nodeInfo.getLevel() == 1) {
                // 一级标题作为根节点
                node.setParentId(null);
                rootNode = nodeRepository.save(node);
                levelNodes[0] = rootNode;
            } else {
                // 找到父节点(上一层级的最后一个节点)
                PmNode parent = levelNodes[nodeInfo.getLevel() - 2];
                if (parent != null) {
                    node.setParentId(parent.getId());
                    node = nodeRepository.save(node);
                    levelNodes[nodeInfo.getLevel() - 1] = node;
                }
            }
        }
        
        return rootNode;
    }
    
    /**
     * 导入OPML
     */
    private DocumentVO importOPML(ImportRequest request) {
        log.debug("导入OPML格式");
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(request.getContent().getBytes()));
            
            // 提取标题
            String title = request.getTitle();
            NodeList titleNodes = doc.getElementsByTagName("title");
            if (title == null && titleNodes.getLength() > 0) {
                title = titleNodes.item(0).getTextContent();
            }
            if (title == null) {
                title = "导入的文档";
            }
            
            // 创建文档
            PmDocument document = new PmDocument();
            document.setTitle(title);
            document.setOwnerId(request.getUserId());
            document.setTheme(request.getTheme());
            document.setVisibility(request.getVisibility());
            document = documentRepository.save(document);
            
            // 解析outline节点
            NodeList bodyNodes = doc.getElementsByTagName("body");
            if (bodyNodes.getLength() > 0) {
                Element body = (Element) bodyNodes.item(0);
                NodeList outlines = body.getElementsByTagName("outline");
                
                if (outlines.getLength() > 0) {
                    Element firstOutline = (Element) outlines.item(0);
                    createNodesFromOPML(document.getId(), firstOutline, null, 0);
                }
            }
            
            // 构建返回值
            DocumentVO vo = new DocumentVO();
            vo.setId(document.getId());
            vo.setTitle(document.getTitle());
            vo.setOwnerId(document.getOwnerId());
            vo.setTheme(document.getTheme());
            vo.setVisibility(document.getVisibility());
            vo.setCreatedAt(document.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            vo.setUpdatedAt(document.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            
            log.info("OPML导入成功,文档ID: {}", document.getId());
            return vo;
        } catch (Exception e) {
            log.error("OPML导入失败", e);
            throw new RuntimeException("OPML导入失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从OPML outline递归创建节点
     */
    private PmNode createNodesFromOPML(Long documentId, Element outline, Long parentId, int depth) {
        String text = outline.getAttribute("text");
        
        PmNode node = new PmNode();
        node.setDocumentId(documentId);
        node.setContent(text);
        node.setParentId(parentId);
        node.setDepth(depth);
        node = nodeRepository.save(node);
        
        // 递归处理子节点
        NodeList children = outline.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element child = (Element) children.item(i);
                if ("outline".equals(child.getTagName())) {
                    createNodesFromOPML(documentId, child, node.getId(), depth + 1);
                }
            }
        }
        
        return node;
    }
    
    /**
     * 节点信息临时类
     */
    private static class NodeInfo {
        private int level;
        private String content;
        private String description;
        
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
