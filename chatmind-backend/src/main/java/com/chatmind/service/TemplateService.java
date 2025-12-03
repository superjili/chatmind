package com.chatmind.service;

import com.chatmind.dto.CreateTemplateRequest;
import com.chatmind.dto.DocumentVO;
import com.chatmind.dto.TemplateVO;
import com.chatmind.entity.PmDocument;
import com.chatmind.entity.PmNode;
import com.chatmind.entity.PmTemplate;
import com.chatmind.repository.PmDocumentRepository;
import com.chatmind.repository.PmNodeRepository;
import com.chatmind.repository.PmTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模板服务
 * 管理会议、SWOT、产品规划等预定义模板
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {
    
    private final PmTemplateRepository templateRepository;
    private final PmDocumentRepository documentRepository;
    private final PmNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 创建模板
     */
    @Transactional
    public TemplateVO createTemplate(CreateTemplateRequest request) {
        log.info("创建模板: {}, 类型={}", request.getTemplateName(), request.getTemplateType());
        
        PmTemplate template = new PmTemplate();
        template.setTemplateName(request.getTemplateName());
        template.setTemplateType(request.getTemplateType());
        template.setDescription(request.getDescription());
        template.setTemplateContent(request.getTemplateContent());
        template.setIsPublic(request.getIsPublic());
        template.setUseCount(0);
        template.setCreatedBy(request.getCreatedBy());
        
        template = templateRepository.save(template);
        log.info("模板创建成功: ID={}", template.getId());
        
        return convertToVO(template);
    }
    
    /**
     * 获取模板列表
     */
    public List<TemplateVO> getTemplates(String templateType, Long userId) {
        log.debug("查询模板列表: 类型={}, 用户={}", templateType, userId);
        
        List<PmTemplate> templates;
        
        if (templateType != null) {
            // 按类型查询
            templates = templateRepository.findByTemplateTypeAndDeleted(templateType, 0);
        } else {
            // 查询所有公开模板
            templates = templateRepository.findByIsPublicAndDeleted(1, 0);
        }
        
        // 如果有用户ID,追加用户私有模板
        if (userId != null) {
            List<PmTemplate> userTemplates = templateRepository.findByCreatedByAndDeleted(userId, 0);
            templates.addAll(userTemplates);
        }
        
        return templates.stream()
            .distinct()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取模板详情
     */
    public TemplateVO getTemplate(Long templateId) {
        log.debug("查询模板详情: {}", templateId);
        
        PmTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        return convertToVO(template);
    }
    
    /**
     * 从模板创建文档
     */
    @Transactional
    public DocumentVO createDocumentFromTemplate(Long templateId, String title, Long userId) {
        log.info("从模板创建文档: 模板={}, 用户={}", templateId, userId);
        
        PmTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        // 增加使用次数
        template.setUseCount(template.getUseCount() + 1);
        templateRepository.save(template);
        
        // 创建文档
        PmDocument document = new PmDocument();
        document.setTitle(title != null ? title : template.getTemplateName());
        document.setOwnerId(userId);
        document.setTheme("default");
        document.setVisibility("private");
        document = documentRepository.save(document);
        
        // 解析模板内容并创建节点
        try {
            Map<String, Object> templateData = objectMapper.readValue(
                template.getTemplateContent(), Map.class);
            
            if (templateData.containsKey("nodes")) {
                Map<String, Object> rootNodeData = (Map<String, Object>) templateData.get("nodes");
                createNodesFromTemplate(document.getId(), rootNodeData, null, 0);
            }
        } catch (Exception e) {
            log.error("解析模板内容失败", e);
            throw new RuntimeException("解析模板内容失败: " + e.getMessage(), e);
        }
        
        log.info("文档创建成功: ID={}", document.getId());
        
        // 构建返回值
        DocumentVO vo = new DocumentVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setOwnerId(document.getOwnerId());
        vo.setTheme(document.getTheme());
        vo.setVisibility(document.getVisibility());
        vo.setCreatedAt(document.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        vo.setUpdatedAt(document.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        
        return vo;
    }
    
    /**
     * 删除模板
     */
    @Transactional
    public void deleteTemplate(Long templateId) {
        log.info("删除模板: {}", templateId);
        
        PmTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));
        
        template.setDeleted(1);
        templateRepository.save(template);
        
        log.info("模板已删除");
    }
    
    /**
     * 从模板数据创建节点
     */
    private void createNodesFromTemplate(Long documentId, Map<String, Object> nodeData,
                                         Long parentId, int depth) {
        PmNode node = new PmNode();
        node.setDocumentId(documentId);
        node.setParentId(parentId);
        node.setDepth(depth);
        
        if (nodeData.get("content") != null) {
            node.setContent((String) nodeData.get("content"));
        }
        if (nodeData.get("color") != null) {
            node.setColor((String) nodeData.get("color"));
        }
        if (nodeData.get("icon") != null) {
            node.setIcon((String) nodeData.get("icon"));
        }
        if (nodeData.get("description") != null) {
            node.setDescription((String) nodeData.get("description"));
        }
        if (nodeData.get("labels") != null) {
            node.setLabels((String) nodeData.get("labels"));
        }
        
        node = nodeRepository.save(node);
        
        // 递归创建子节点
        List<Map<String, Object>> children = (List<Map<String, Object>>) nodeData.get("children");
        if (children != null && !children.isEmpty()) {
            for (Map<String, Object> child : children) {
                createNodesFromTemplate(documentId, child, node.getId(), depth + 1);
            }
        }
    }
    
    /**
     * 转换为VO
     */
    private TemplateVO convertToVO(PmTemplate template) {
        TemplateVO vo = new TemplateVO();
        vo.setId(template.getId());
        vo.setTemplateName(template.getTemplateName());
        vo.setTemplateType(template.getTemplateType());
        vo.setDescription(template.getDescription());
        vo.setTemplateContent(template.getTemplateContent());
        vo.setIsPublic(template.getIsPublic());
        vo.setUseCount(template.getUseCount());
        vo.setCreatedBy(template.getCreatedBy());
        vo.setCreatedAt(template.getCreatedAt());
        return vo;
    }
}
