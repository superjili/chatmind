package com.chatmind.service;

import com.chatmind.dto.*;
import com.chatmind.entity.PmAiJob;
import com.chatmind.entity.PmDocument;
import com.chatmind.entity.PmNode;
import com.chatmind.repository.PmAiJobRepository;
import com.chatmind.repository.PmDocumentRepository;
import com.chatmind.repository.PmNodeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AI业务服务
 * 处理AI生成脑图、智能扩展、自动总结等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {
    
    private final ChatModelService chatModelService;
    private final PmAiJobRepository aiJobRepository;
    private final PmDocumentRepository documentRepository;
    private final PmNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 从文字生成脑图
     */
    @Transactional
    public DocumentVO generateMindMap(GenerateMindMapRequest request) {
        log.info("开始生成脑图,用户: {}, 文本长度: {}", request.getUserId(), request.getText().length());
        
        // 1. 创建AI任务记录
        PmAiJob job = createAIJob(request);
        
        try {
            // 2. 构建Prompt
            String prompt = buildGeneratePrompt(request);
            log.debug("Prompt: {}", prompt);
            
            // 3. 调用LLM生成
            job.setStatus("processing");
            aiJobRepository.save(job);
            
            String response = chatModelService.generateText(prompt);
            log.debug("LLM响应: {}", response);
            
            // 4. 解析LLM返回的JSON
            List<MindMapNodeDTO> nodeTree = parseLLMResponse(response);
            
            // 5. 校验生成的节点
            validateNodes(nodeTree, request.getMaxDepth(), request.getMaxChildren());
            
            // 6. 保存结果到AI Job
            job.setResultData(response);
            job.setResultSummary("成功生成" + countNodes(nodeTree) + "个节点");
            job.setValidationStatus("passed");
            job.setStatus("completed");
            job.setCompletedAt(LocalDateTime.now());
            aiJobRepository.save(job);
            
            // 7. 创建或更新文档和节点
            DocumentVO document = createDocumentWithNodes(request, nodeTree);
            
            log.info("脑图生成成功,文档ID: {}, 节点数: {}", document.getId(), countNodes(nodeTree));
            return document;
            
        } catch (Exception e) {
            log.error("生成脑图失败", e);
            job.setStatus("failed");
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            aiJobRepository.save(job);
            throw new RuntimeException("AI生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * AI智能扩展节点
     */
    @Transactional
    public List<NodeVO> expandNode(Long nodeId, Long userId, Integer count) {
        log.info("智能扩展节点: {}, 用户: {}", nodeId, userId);
        
        PmNode node = nodeRepository.findById(nodeId)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));
        
        // 创建AI任务
        PmAiJob job = new PmAiJob();
        job.setUserId(userId);
        job.setDocumentId(node.getDocumentId());
        job.setNodeId(nodeId);
        job.setJobType("expand");
        job.setStatus("processing");
        aiJobRepository.save(job);
        
        try {
            // 构建扩展Prompt
            String prompt = buildExpandPrompt(node, count != null ? count : 5);
            
            String response = chatModelService.generateText(prompt);
            log.debug("扩展节点LLM响应: {}", response);
            
            // 解析返回的子节点
            List<MindMapNodeDTO> expandedNodes = parseLLMResponse(response);
            
            // 保存AI任务结果
            job.setPrompt(prompt);
            job.setResultData(response);
            job.setResultSummary("扩展生成" + expandedNodes.size() + "个子节点");
            job.setValidationStatus("passed");
            job.setStatus("completed");
            job.setCompletedAt(LocalDateTime.now());
            aiJobRepository.save(job);
            
            // 创建子节点
            List<NodeVO> createdNodes = createChildNodes(node, expandedNodes);
            
            log.info("节点扩展成功,新增: {}个子节点", createdNodes.size());
            return createdNodes;
            
        } catch (Exception e) {
            log.error("扩展节点失败", e);
            job.setStatus("failed");
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            aiJobRepository.save(job);
            throw new RuntimeException("智能扩展失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * AI自动总结
     */
    public String summarize(Long documentId, Long nodeId, Long userId, String summaryType) {
        log.info("自动总结,文档: {}, 节点: {}, 类型: {}", documentId, nodeId, summaryType);
        
        // 创建AI任务
        PmAiJob job = new PmAiJob();
        job.setUserId(userId);
        job.setDocumentId(documentId);
        job.setNodeId(nodeId);
        job.setJobType("summarize");
        job.setStatus("processing");
        aiJobRepository.save(job);
        
        try {
            // 收集要总结的节点文本
            List<String> nodeTexts = collectNodeTexts(documentId, nodeId);
            
            // 构建总结Prompt
            String prompt = buildSummarizePrompt(nodeTexts, summaryType);
            
            String summary = chatModelService.generateText(prompt);
            
            // 保存结果
            job.setPrompt(prompt);
            job.setResultData(summary);
            job.setResultSummary("总结完成");
            job.setStatus("completed");
            job.setCompletedAt(LocalDateTime.now());
            aiJobRepository.save(job);
            
            log.info("总结完成,长度: {}", summary.length());
            return summary;
            
        } catch (Exception e) {
            log.error("总结失败", e);
            job.setStatus("failed");
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            aiJobRepository.save(job);
            throw new RuntimeException("自动总结失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建生成脑图的Prompt
     */
    private String buildGeneratePrompt(GenerateMindMapRequest request) {
        return String.format(
            "你是一个专注结构化梳理的助手。目标：把以下文本拆解为层级清晰的脑图节点。\n\n" +
            "返回JSON数组格式，每项包含：\n" +
            "{ \"text\": \"节点文本\", \"labels\": [\"标签1\", \"标签2\"], \"suggestedColor\": \"颜色\", \"children\": [...] }\n\n" +
            "要求：\n" +
            "1. 最大层级深度：%d\n" +
            "2. 每个父节点最多子节点数：%d\n" +
            "3. 不要使用循环引用\n" +
            "4. 颜色使用常见颜色名(如red, blue, green等)\n" +
            "5. 如果文本含有敏感内容，请标注 \"sensitive\": true\n\n" +
            "输入文本：\n%s\n\n" +
            "请直接返回JSON数组，不要其他说明文字。",
            request.getMaxDepth(), request.getMaxChildren(), request.getText()
        );
    }
    
    /**
     * 构建扩展节点的Prompt
     */
    private String buildExpandPrompt(PmNode node, int count) {
        String context = collectParentContext(node);
        return String.format(
            "给定节点文本：%s\n\n" +
            "上级上下文：%s\n\n" +
            "请生成%d个可落地的子节点，每个子节点包含文本和标签。\n" +
            "返回JSON数组格式：[{ \"text\":\"节点文本\", \"labels\": [\"标签\"] }]\n\n" +
            "请直接返回JSON数组，不要其他说明文字。",
            node.getContent(), context, count
        );
    }
    
    /**
     * 构建总结的Prompt
     */
    private String buildSummarizePrompt(List<String> nodeTexts, String summaryType) {
        String typeDesc = "detailed".equals(summaryType) ? "详细总结" : "扼要总结";
        return String.format(
            "你要基于以下节点列表做%s：\n\n" +
            "节点文本：\n%s\n\n" +
            "请返回：\n" +
            "1. 短摘要（1-2句）\n" +
            "2. 关键结论（要点列表）\n" +
            "3. 行动项（可执行、包含责任人/优先级）\n\n" +
            "请以清晰的文本格式返回。",
            typeDesc, String.join("\n", nodeTexts)
        );
    }
    
    /**
     * 解析LLM返回的JSON
     */
    private List<MindMapNodeDTO> parseLLMResponse(String response) {
        try {
            // 提取JSON部分(去除可能的前后文字)
            String json = extractJSON(response);
            return objectMapper.readValue(json, new TypeReference<List<MindMapNodeDTO>>() {});
        } catch (Exception e) {
            log.error("解析LLM响应失败: {}", response, e);
            throw new RuntimeException("解析AI响应失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从文本中提取JSON
     */
    private String extractJSON(String text) {
        // 查找JSON数组的开始和结束
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        // 如果没找到数组，尝试查找对象
        start = text.indexOf('{');
        end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return "[" + text.substring(start, end + 1) + "]";
        }
        return text;
    }
    
    /**
     * 校验生成的节点
     */
    private void validateNodes(List<MindMapNodeDTO> nodes, int maxDepth, int maxChildren) {
        int totalNodes = countNodes(nodes);
        if (totalNodes > 100) {
            throw new RuntimeException("生成的节点数超过限制: " + totalNodes);
        }
        
        validateNodeDepth(nodes, 0, maxDepth);
        validateChildrenCount(nodes, maxChildren);
    }
    
    /**
     * 递归验证深度
     */
    private void validateNodeDepth(List<MindMapNodeDTO> nodes, int currentDepth, int maxDepth) {
        if (currentDepth > maxDepth) {
            throw new RuntimeException("节点深度超过限制: " + currentDepth);
        }
        for (MindMapNodeDTO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                validateNodeDepth(node.getChildren(), currentDepth + 1, maxDepth);
            }
        }
    }
    
    /**
     * 验证子节点数量
     */
    private void validateChildrenCount(List<MindMapNodeDTO> nodes, int maxChildren) {
        for (MindMapNodeDTO node : nodes) {
            if (node.getChildren() != null && node.getChildren().size() > maxChildren) {
                throw new RuntimeException("子节点数超过限制: " + node.getChildren().size());
            }
            if (node.getChildren() != null) {
                validateChildrenCount(node.getChildren(), maxChildren);
            }
        }
    }
    
    /**
     * 计算总节点数
     */
    private int countNodes(List<MindMapNodeDTO> nodes) {
        int count = nodes.size();
        for (MindMapNodeDTO node : nodes) {
            if (node.getChildren() != null) {
                count += countNodes(node.getChildren());
            }
        }
        return count;
    }
    
    /**
     * 创建文档和节点
     */
    private DocumentVO createDocumentWithNodes(GenerateMindMapRequest request, List<MindMapNodeDTO> nodeTree) {
        // 创建或获取文档
        PmDocument document;
        if (request.getDocumentId() != null) {
            document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new RuntimeException("文档不存在"));
        } else {
            document = new PmDocument();
            document.setTitle("AI生成的脑图");
            document.setOwnerId(request.getUserId() != null ? request.getUserId() : 1L);
            document.setTemplateId(request.getTemplateId());
            document = documentRepository.save(document);
        }
        
        // 创建根节点
        if (nodeTree != null && !nodeTree.isEmpty()) {
            MindMapNodeDTO rootDTO = nodeTree.get(0);
            PmNode rootNode = createNodeRecursive(document.getId(), null, rootDTO, 0);
            
            // 更新文档的根节点ID
            document.setRootNodeId(rootNode.getId());
            documentRepository.save(document);
        }
        
        // 转换为VO
        DocumentVO vo = new DocumentVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setOwnerId(document.getOwnerId());
        vo.setRootNodeId(document.getRootNodeId());
        return vo;
    }
    
    /**
     * 递归创建节点
     */
    private PmNode createNodeRecursive(Long documentId, Long parentId, MindMapNodeDTO dto, int depth) {
        PmNode node = new PmNode();
        node.setDocumentId(documentId);
        node.setParentId(parentId);
        node.setContent(dto.getText());
        node.setDepth(depth);
        node.setColor(dto.getSuggestedColor());
        
        if (dto.getLabels() != null && !dto.getLabels().isEmpty()) {
            try {
                node.setLabels(objectMapper.writeValueAsString(dto.getLabels()));
            } catch (Exception e) {
                log.warn("序列化标签失败", e);
            }
        }
        
        node = nodeRepository.save(node);
        
        // 递归创建子节点
        if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
            List<Long> childrenIds = new ArrayList<>();
            for (MindMapNodeDTO child : dto.getChildren()) {
                PmNode childNode = createNodeRecursive(documentId, node.getId(), child, depth + 1);
                childrenIds.add(childNode.getId());
            }
            
            // 更新子节点排序
            try {
                node.setChildrenOrder(objectMapper.writeValueAsString(childrenIds));
                nodeRepository.save(node);
            } catch (Exception e) {
                log.warn("更新子节点排序失败", e);
            }
        }
        
        return node;
    }
    
    /**
     * 创建子节点
     */
    private List<NodeVO> createChildNodes(PmNode parentNode, List<MindMapNodeDTO> nodeDTOs) {
        List<NodeVO> result = new ArrayList<>();
        
        for (MindMapNodeDTO dto : nodeDTOs) {
            PmNode node = new PmNode();
            node.setDocumentId(parentNode.getDocumentId());
            node.setParentId(parentNode.getId());
            node.setContent(dto.getText());
            node.setDepth(parentNode.getDepth() + 1);
            node.setColor(dto.getSuggestedColor());
            
            if (dto.getLabels() != null) {
                try {
                    node.setLabels(objectMapper.writeValueAsString(dto.getLabels()));
                } catch (Exception e) {
                    log.warn("序列化标签失败", e);
                }
            }
            
            node = nodeRepository.save(node);
            
            NodeVO vo = new NodeVO();
            vo.setId(node.getId());
            vo.setContent(node.getContent());
            vo.setColor(node.getColor());
            vo.setLabels(node.getLabels());
            result.add(vo);
        }
        
        return result;
    }
    
    /**
     * 创建AI任务记录
     */
    private PmAiJob createAIJob(GenerateMindMapRequest request) {
        PmAiJob job = new PmAiJob();
        job.setUserId(request.getUserId() != null ? request.getUserId() : 1L);
        job.setDocumentId(request.getDocumentId());
        job.setJobType("generate");
        job.setStatus("pending");
        return aiJobRepository.save(job);
    }
    
    /**
     * 收集节点文本用于总结
     */
    private List<String> collectNodeTexts(Long documentId, Long nodeId) {
        List<String> texts = new ArrayList<>();
        
        if (nodeId != null) {
            // 总结指定节点及其子树
            PmNode node = nodeRepository.findById(nodeId).orElse(null);
            if (node != null) {
                collectNodeTextsRecursive(node, texts);
            }
        } else if (documentId != null) {
            // 总结整个文档
            List<PmNode> nodes = nodeRepository.findByDocumentIdAndDeleted(documentId, 0);
            for (PmNode node : nodes) {
                texts.add(node.getContent());
            }
        }
        
        return texts;
    }
    
    /**
     * 递归收集节点文本
     */
    private void collectNodeTextsRecursive(PmNode node, List<String> texts) {
        texts.add(node.getContent());
        List<PmNode> children = nodeRepository.findByParentIdAndDeleted(node.getId(), 0);
        for (PmNode child : children) {
            collectNodeTextsRecursive(child, texts);
        }
    }
    
    /**
     * 收集父节点上下文
     */
    private String collectParentContext(PmNode node) {
        StringBuilder context = new StringBuilder();
        Long parentId = node.getParentId();
        int level = 0;
        
        while (parentId != null && level < 3) {
            PmNode parent = nodeRepository.findById(parentId).orElse(null);
            if (parent == null) break;
            
            context.insert(0, parent.getContent() + " > ");
            parentId = parent.getParentId();
            level++;
        }
        
        return context.toString();
    }
}
