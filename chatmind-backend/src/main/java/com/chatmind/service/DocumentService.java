package com.chatmind.service;

import com.chatmind.dto.CreateDocumentRequest;
import com.chatmind.dto.UpdateDocumentRequest;
import com.chatmind.dto.DocumentVO;
import com.chatmind.entity.PmDocument;
import com.chatmind.entity.PmNode;
import com.chatmind.repository.PmDocumentRepository;
import com.chatmind.repository.PmNodeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档业务服务
 * 处理文档的CRUD操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final PmDocumentRepository documentRepository;
    private final PmNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 创建文档
     * 同时创建根节点
     */
    @Transactional
    public DocumentVO createDocument(CreateDocumentRequest request) {
        log.info("创建文档: {}", request.getTitle());
        
        // 创建文档实体
        PmDocument document = new PmDocument();
        document.setTitle(request.getTitle());
        document.setOwnerId(request.getOwnerId() != null ? request.getOwnerId() : 1L);
        document.setVisibility(request.getVisibility() != null ? request.getVisibility() : "private");
        document.setTheme(request.getTheme() != null ? request.getTheme() : "default");
        document.setTemplateId(request.getTemplateId());
        
        // 保存文档
        document = documentRepository.save(document);
        
        // 创建根节点
        PmNode rootNode = new PmNode();
        rootNode.setDocumentId(document.getId());
        rootNode.setContent(request.getTitle());
        rootNode.setParentId(null);
        rootNode.setDepth(0);
        rootNode = nodeRepository.save(rootNode);
        
        // 更新文档的根节点ID
        document.setRootNodeId(rootNode.getId());
        document = documentRepository.save(document);
        
        log.info("文档创建成功,ID: {}, 根节点ID: {}", document.getId(), rootNode.getId());
        return convertToVO(document);
    }
    
    /**
     * 根据ID获取文档
     */
    public DocumentVO getDocumentById(Long id) {
        log.debug("查询文档: {}", id);
        PmDocument document = documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("文档不存在: " + id));
        return convertToVO(document);
    }
    
    /**
     * 根据所有者ID获取文档列表
     */
    public List<DocumentVO> getDocumentsByOwnerId(Long ownerId) {
        log.debug("查询用户文档列表: {}", ownerId);
        List<PmDocument> documents = documentRepository.findByOwnerIdAndDeleted(ownerId, 0);
        return documents.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 更新文档
     */
    @Transactional
    public DocumentVO updateDocument(Long id, UpdateDocumentRequest request) {
        log.info("更新文档: {}", id);
        
        PmDocument document = documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("文档不存在: " + id));
        
        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }
        if (request.getVisibility() != null) {
            document.setVisibility(request.getVisibility());
        }
        if (request.getTheme() != null) {
            document.setTheme(request.getTheme());
        }
        if (request.getMetadata() != null) {
            document.setMetadata(request.getMetadata());
        }
        
        document = documentRepository.save(document);
        log.info("文档更新成功: {}", id);
        return convertToVO(document);
    }
    
    /**
     * 删除文档(逻辑删除)
     */
    @Transactional
    public void deleteDocument(Long id) {
        log.info("删除文档: {}", id);
        
        PmDocument document = documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("文档不存在: " + id));
        
        document.setDeleted(1);
        documentRepository.save(document);
        
        // 同时逻辑删除所有节点
        List<PmNode> nodes = nodeRepository.findByDocumentIdAndDeleted(id, 0);
        nodes.forEach(node -> node.setDeleted(1));
        nodeRepository.saveAll(nodes);
        
        log.info("文档删除成功: {}", id);
    }
    
    /**
     * 转换为VO对象
     */
    private DocumentVO convertToVO(PmDocument document) {
        DocumentVO vo = new DocumentVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setOwnerId(document.getOwnerId());
        vo.setRootNodeId(document.getRootNodeId());
        vo.setVisibility(document.getVisibility());
        vo.setTheme(document.getTheme());
        vo.setTemplateId(document.getTemplateId());
        vo.setMetadata(document.getMetadata());
        vo.setCurrentVersion(document.getCurrentVersion());
        vo.setLastEditorId(document.getLastEditorId());
        vo.setCreatedAt(document.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        vo.setUpdatedAt(document.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return vo;
    }
}
