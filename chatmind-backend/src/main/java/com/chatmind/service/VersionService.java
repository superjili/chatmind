package com.chatmind.service;

import com.chatmind.dto.CreateVersionRequest;
import com.chatmind.dto.DocumentVO;
import com.chatmind.dto.RestoreVersionRequest;
import com.chatmind.dto.VersionVO;
import com.chatmind.entity.PmDocument;
import com.chatmind.entity.PmNode;
import com.chatmind.entity.PmVersion;
import com.chatmind.repository.PmDocumentRepository;
import com.chatmind.repository.PmNodeRepository;
import com.chatmind.repository.PmVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;  
import java.util.stream.Collectors;

/**
 * 版本管理服务
 * 支持自动保存(autosave)和显式版本(explicit)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VersionService {
    
    private final PmVersionRepository versionRepository;
    private final PmDocumentRepository documentRepository;
    private final PmNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 创建版本快照
     */
    @Transactional
    public VersionVO createVersion(CreateVersionRequest request) {
        log.info("创建版本快照: 文档={}, 类型={}", request.getDocumentId(), request.getVersionType());
        
        // 验证文档
        PmDocument document = documentRepository.findById(request.getDocumentId())
            .orElseThrow(() -> new RuntimeException("文档不存在: " + request.getDocumentId()));
        
        // 获取当前文档的所有节点
        List<PmNode> nodes = nodeRepository.findByDocumentIdAndDeleted(request.getDocumentId(), 0);
        
        // 生成快照数据
        String snapshotData = generateSnapshot(document, nodes);
        
        // 获取下一个版本号
        Integer maxVersion = versionRepository.findMaxVersionNumberByDocumentId(request.getDocumentId());
        int versionNumber = (maxVersion == null) ? 1 : maxVersion + 1;
        
        // 创建版本记录
        PmVersion version = new PmVersion();
        version.setDocumentId(request.getDocumentId());
        version.setVersionNumber(versionNumber);
        version.setVersionType(request.getVersionType());
        version.setSnapshotData(snapshotData);
        version.setNodeCount(nodes.size());
        version.setSnapshotSize((long) snapshotData.length());
        version.setCreatedBy(request.getCreatedBy());
        
        if ("explicit".equals(request.getVersionType())) {
            version.setVersionName(request.getVersionName() != null ? 
                request.getVersionName() : "版本 " + versionNumber);
            version.setDescription(request.getDescription());
        } else {
            version.setVersionName("自动保存 " + versionNumber);
        }
        
        version = versionRepository.save(version);
        
        // 自动清理旧的autosave版本(保留最近10个)
        if ("autosave".equals(request.getVersionType())) {
            cleanupOldAutosaveVersions(request.getDocumentId());
        }
        
        log.info("版本创建成功: ID={}, 版本号={}", version.getId(), version.getVersionNumber());
        return convertToVO(version);
    }
    
    /**
     * 获取文档的版本列表
     */
    public List<VersionVO> getVersions(Long documentId, String versionType) {
        log.debug("查询版本列表: 文档={}, 类型={}", documentId, versionType);
        
        List<PmVersion> versions;
        if (versionType != null) {
            versions = versionRepository.findByDocumentIdAndVersionTypeAndDeletedOrderByVersionNumberDesc(
                documentId, versionType, 0);
        } else {
            versions = versionRepository.findByDocumentIdAndDeletedOrderByVersionNumberDesc(
                documentId, 0);
        }
        
        return versions.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取版本详情
     */
    public VersionVO getVersion(Long versionId) {
        log.debug("查询版本详情: {}", versionId);
        
        PmVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("版本不存在: " + versionId));
        
        return convertToVO(version);
    }
    
    /**
     * 删除版本
     */
    @Transactional
    public void deleteVersion(Long versionId) {
        log.info("删除版本: {}", versionId);
        
        PmVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("版本不存在: " + versionId));
        
        // 不允许删除最新的显式版本
        if ("explicit".equals(version.getVersionType())) {
            Integer maxVersion = versionRepository.findMaxVersionNumberByDocumentId(version.getDocumentId());
            if (version.getVersionNumber().equals(maxVersion)) {
                throw new RuntimeException("不能删除最新版本");
            }
        }
        
        version.setDeleted(1);
        versionRepository.save(version);
        
        log.info("版本已删除");
    }
    
    /**
     * 自动保存(后台定时调用)
     */
    @Transactional
    public void autoSave(Long documentId, Long userId) {
        log.info("自动保存: 文档={}", documentId);
        
        CreateVersionRequest request = new CreateVersionRequest();
        request.setDocumentId(documentId);
        request.setVersionType("autosave");
        request.setCreatedBy(userId);
        
        createVersion(request);
    }
    
    /**
     * 生成快照数据
     */
    private String generateSnapshot(PmDocument document, List<PmNode> nodes) {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            
            // 文档信息
            Map<String, Object> docInfo = new HashMap<>();
            docInfo.put("id", document.getId());
            docInfo.put("title", document.getTitle());
            docInfo.put("theme", document.getTheme());
            docInfo.put("visibility", document.getVisibility());
            snapshot.put("document", docInfo);
            
            // 节点树
            Map<Long, List<PmNode>> childrenMap = buildChildrenMap(nodes);
            PmNode rootNode = nodes.stream()
                .filter(n -> n.getParentId() == null)
                .findFirst()
                .orElse(null);
            
            if (rootNode != null) {
                snapshot.put("nodes", buildNodeTree(rootNode, childrenMap));
            } else {
                snapshot.put("nodes", Collections.emptyList());
            }
            
            snapshot.put("nodeCount", nodes.size());
            snapshot.put("snapshotTime", LocalDateTime.now().toString());
            
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.error("生成快照失败", e);
            throw new RuntimeException("生成快照失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建节点树
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
        
        for (PmNode node : nodes) {
            if (node.getParentId() != null) {
                childrenMap.computeIfAbsent(node.getParentId(), k -> new ArrayList<>())
                    .add(node);
            }
        }
        
        return childrenMap;
    }
    
    /**
     * 清理旧的自动保存版本(保留最近10个)
     */
    private void cleanupOldAutosaveVersions(Long documentId) {
        List<PmVersion> autosaveVersions = versionRepository
            .findByDocumentIdAndVersionTypeAndDeletedOrderByVersionNumberDesc(
                documentId, "autosave", 0);
        
        if (autosaveVersions.size() > 10) {
            List<PmVersion> toDelete = autosaveVersions.subList(10, autosaveVersions.size());
            for (PmVersion version : toDelete) {
                version.setDeleted(1);
                versionRepository.save(version);
            }
            log.info("清理旧版本: 删除{}个自动保存版本", toDelete.size());
        }
    }
    
    /**
     * 恢复到指定版本(回滚)
     */
    @Transactional
    public DocumentVO restoreVersion(RestoreVersionRequest request) {
        log.info("恢复版本: 版本={}, 用户={}", request.getVersionId(), request.getUserId());
        
        // 获取要恢复的版本
        PmVersion version = versionRepository.findById(request.getVersionId())
            .orElseThrow(() -> new RuntimeException("版本不存在: " + request.getVersionId()));
        
        Long documentId = version.getDocumentId();
        
        // 如果需要,创建当前状态的快照(作为备份)
        if (request.getCreateNewVersion()) {
            CreateVersionRequest backupRequest = new CreateVersionRequest();
            backupRequest.setDocumentId(documentId);
            backupRequest.setVersionType("explicit");
            backupRequest.setVersionName("回滚前备份");
            backupRequest.setDescription("回滚到版本" + version.getVersionNumber() + "前的备份");
            backupRequest.setCreatedBy(request.getUserId());
            createVersion(backupRequest);
        }
        
        // 解析快照数据
        Map<String, Object> snapshot = parseSnapshot(version.getSnapshotData());
        
        // 删除当前文档的所有节点
        List<PmNode> currentNodes = nodeRepository.findByDocumentIdAndDeleted(documentId, 0);
        for (PmNode node : currentNodes) {
            node.setDeleted(1);
        }
        nodeRepository.saveAll(currentNodes);
        
        // 从快照恢复节点
        Map<String, Object> nodesData = (Map<String, Object>) snapshot.get("nodes");
        if (nodesData != null) {
            restoreNodesFromSnapshot(documentId, nodesData, null, 0);
        }
        
        // 更新文档信息
        PmDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("文档不存在: " + documentId));
        
        Map<String, Object> docInfo = (Map<String, Object>) snapshot.get("document");
        if (docInfo != null) {
            if (docInfo.get("title") != null) {
                document.setTitle((String) docInfo.get("title"));
            }
            if (docInfo.get("theme") != null) {
                document.setTheme((String) docInfo.get("theme"));
            }
        }
        documentRepository.save(document);
        
        log.info("版本恢复成功: 文档={}", documentId);
        
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
     * 解析快照数据
     */
    private Map<String, Object> parseSnapshot(String snapshotData) {
        try {
            return objectMapper.readValue(snapshotData, Map.class);
        } catch (Exception e) {
            log.error("解析快照数据失败", e);
            throw new RuntimeException("解析快照数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从快照数据恢复节点
     */
    private void restoreNodesFromSnapshot(Long documentId, Map<String, Object> nodeData, 
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
        if (nodeData.get("collapsed") != null) {
            node.setCollapsed((Integer) nodeData.get("collapsed"));
        }
        if (nodeData.get("position") != null) {
            node.setPosition((String) nodeData.get("position"));
        }
        
        node = nodeRepository.save(node);
        
        // 递归恢复子节点
        List<Map<String, Object>> children = (List<Map<String, Object>>) nodeData.get("children");
        if (children != null && !children.isEmpty()) {
            for (Map<String, Object> child : children) {
                restoreNodesFromSnapshot(documentId, child, node.getId(), depth + 1);
            }
        }
    }
    
    /**
     * 转换为VO
     */
    private VersionVO convertToVO(PmVersion version) {
        VersionVO vo = new VersionVO();
        vo.setId(version.getId());
        vo.setDocumentId(version.getDocumentId());
        vo.setVersionNumber(version.getVersionNumber());
        vo.setVersionName(version.getVersionName());
        vo.setVersionType(version.getVersionType());
        vo.setDescription(version.getDescription());
        vo.setSnapshotData(version.getSnapshotData());
        vo.setNodeCount(version.getNodeCount());
        vo.setSnapshotSize(version.getSnapshotSize());
        vo.setCreatedBy(version.getCreatedBy());
        vo.setCreatedAt(version.getCreatedAt());
        return vo;
    }
}
