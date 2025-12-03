package com.chatmind.service;

import com.chatmind.dto.*;
import com.chatmind.entity.PmNode;
import com.chatmind.entity.PmVersion;
import com.chatmind.repository.PmNodeRepository;
import com.chatmind.repository.PmVersionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 版本差异对比服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiffService {
    
    private final PmVersionRepository versionRepository;
    private final PmNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 对比两个版本的差异
     */
    public VersionDiffVO compareVersions(Long fromVersionId, Long toVersionId) {
        log.info("对比版本差异: {} -> {}", fromVersionId, toVersionId);
        
        PmVersion fromVersion = versionRepository.findById(fromVersionId)
            .orElseThrow(() -> new RuntimeException("源版本不存在: " + fromVersionId));
        PmVersion toVersion = versionRepository.findById(toVersionId)
            .orElseThrow(() -> new RuntimeException("目标版本不存在: " + toVersionId));
        
        if (!fromVersion.getDocumentId().equals(toVersion.getDocumentId())) {
            throw new RuntimeException("版本不属于同一文档");
        }
        
        // 解析快照数据
        Map<Long, NodeSnapshot> fromNodes = parseSnapshot(fromVersion.getSnapshotData());
        Map<Long, NodeSnapshot> toNodes = parseSnapshot(toVersion.getSnapshotData());
        
        // 计算差异
        VersionDiffVO diff = new VersionDiffVO();
        diff.setFromVersionId(fromVersionId);
        diff.setToVersionId(toVersionId);
        
        List<DiffNodeVO> addedNodes = new ArrayList<>();
        List<DiffNodeVO> removedNodes = new ArrayList<>();
        List<DiffNodeVO> updatedNodes = new ArrayList<>();
        List<MovedNodeVO> movedNodes = new ArrayList<>();
        
        // 查找新增和更新的节点
        for (Map.Entry<Long, NodeSnapshot> entry : toNodes.entrySet()) {
            Long nodeId = entry.getKey();
            NodeSnapshot toNode = entry.getValue();
            
            if (!fromNodes.containsKey(nodeId)) {
                // 新增节点
                DiffNodeVO added = createDiffNode(toNode);
                addedNodes.add(added);
            } else {
                NodeSnapshot fromNode = fromNodes.get(nodeId);
                
                // 检查是否移动
                if (!Objects.equals(fromNode.getParentId(), toNode.getParentId())) {
                    MovedNodeVO moved = new MovedNodeVO();
                    moved.setNodeId(nodeId);
                    moved.setContent(toNode.getContent());
                    moved.setOldParentId(fromNode.getParentId());
                    moved.setNewParentId(toNode.getParentId());
                    moved.setOldPath(buildNodePath(fromNode, fromNodes));
                    moved.setNewPath(buildNodePath(toNode, toNodes));
                    movedNodes.add(moved);
                }
                
                // 检查内容是否更新
                if (!Objects.equals(fromNode.getContent(), toNode.getContent())) {
                    DiffNodeVO updated = createDiffNode(toNode);
                    updated.setChangeType("content");
                    updated.setOldValue(fromNode.getContent());
                    updated.setNewValue(toNode.getContent());
                    updatedNodes.add(updated);
                }
                
                // 检查样式是否更新
                if (!Objects.equals(fromNode.getStyle(), toNode.getStyle())) {
                    DiffNodeVO updated = createDiffNode(toNode);
                    updated.setChangeType("style");
                    updated.setOldValue(fromNode.getStyle());
                    updated.setNewValue(toNode.getStyle());
                    updatedNodes.add(updated);
                }
            }
        }
        
        // 查找删除的节点
        for (Map.Entry<Long, NodeSnapshot> entry : fromNodes.entrySet()) {
            Long nodeId = entry.getKey();
            NodeSnapshot fromNode = entry.getValue();
            
            if (!toNodes.containsKey(nodeId)) {
                // 删除节点
                DiffNodeVO removed = createDiffNode(fromNode);
                removedNodes.add(removed);
            }
        }
        
        diff.setAddedNodes(addedNodes);
        diff.setRemovedNodes(removedNodes);
        diff.setUpdatedNodes(updatedNodes);
        diff.setMovedNodes(movedNodes);
        
        // 统计
        VersionDiffVO.DiffStats stats = new VersionDiffVO.DiffStats();
        stats.setAddedCount(addedNodes.size());
        stats.setRemovedCount(removedNodes.size());
        stats.setUpdatedCount(updatedNodes.size());
        stats.setMovedCount(movedNodes.size());
        diff.setStats(stats);
        
        log.info("差异对比完成: 新增{}, 删除{}, 更新{}, 移动{}", 
            addedNodes.size(), removedNodes.size(), updatedNodes.size(), movedNodes.size());
        
        return diff;
    }
    
    /**
     * 对比当前版本与历史版本
     */
    public VersionDiffVO compareWithCurrent(Long documentId, Long versionId) {
        log.info("对比当前版本与历史版本: 文档={}, 版本={}", documentId, versionId);
        
        // 获取当前节点
        List<PmNode> currentNodes = nodeRepository.findByDocumentIdAndDeleted(documentId, 0);
        
        // 获取历史版本
        PmVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("版本不存在: " + versionId));
        
        if (!version.getDocumentId().equals(documentId)) {
            throw new RuntimeException("版本不属于该文档");
        }
        
        // 构建当前节点快照
        Map<Long, NodeSnapshot> currentNodeMap = new HashMap<>();
        for (PmNode node : currentNodes) {
            NodeSnapshot snapshot = new NodeSnapshot();
            snapshot.setNodeId(node.getId());
            snapshot.setContent(node.getContent());
            snapshot.setParentId(node.getParentId());
            snapshot.setLevel(node.getDepth());
            // 组合样式信息
            String style = String.format("{\"color\":\"%s\",\"icon\":\"%s\"}", 
                node.getColor() != null ? node.getColor() : "", 
                node.getIcon() != null ? node.getIcon() : "");
            snapshot.setStyle(style);
            currentNodeMap.put(node.getId(), snapshot);
        }
        
        // 解析历史快照
        Map<Long, NodeSnapshot> historyNodeMap = parseSnapshot(version.getSnapshotData());
        
        // 使用相同逻辑计算差异
        return compareMaps(versionId, null, historyNodeMap, currentNodeMap);
    }
    
    /**
     * 解析快照数据
     */
    private Map<Long, NodeSnapshot> parseSnapshot(String snapshotData) {
        try {
            Map<String, Object> snapshot = objectMapper.readValue(snapshotData, 
                new TypeReference<Map<String, Object>>() {});
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodesList = (List<Map<String, Object>>) snapshot.get("nodes");
            
            Map<Long, NodeSnapshot> nodeMap = new HashMap<>();
            
            if (nodesList != null) {
                for (Map<String, Object> nodeData : nodesList) {
                    NodeSnapshot node = objectMapper.convertValue(nodeData, NodeSnapshot.class);
                    nodeMap.put(node.getNodeId(), node);
                }
            }
            
            return nodeMap;
        } catch (Exception e) {
            log.error("解析快照失败", e);
            return new HashMap<>();
        }
    }
    
    /**
     * 对比两个节点Map
     */
    private VersionDiffVO compareMaps(Long fromVersionId, Long toVersionId,
                                      Map<Long, NodeSnapshot> fromNodes,
                                      Map<Long, NodeSnapshot> toNodes) {
        VersionDiffVO diff = new VersionDiffVO();
        diff.setFromVersionId(fromVersionId);
        diff.setToVersionId(toVersionId);
        
        List<DiffNodeVO> addedNodes = new ArrayList<>();
        List<DiffNodeVO> removedNodes = new ArrayList<>();
        List<DiffNodeVO> updatedNodes = new ArrayList<>();
        List<MovedNodeVO> movedNodes = new ArrayList<>();
        
        // 查找新增和更新
        for (Map.Entry<Long, NodeSnapshot> entry : toNodes.entrySet()) {
            Long nodeId = entry.getKey();
            NodeSnapshot toNode = entry.getValue();
            
            if (!fromNodes.containsKey(nodeId)) {
                addedNodes.add(createDiffNode(toNode));
            } else {
                NodeSnapshot fromNode = fromNodes.get(nodeId);
                
                if (!Objects.equals(fromNode.getParentId(), toNode.getParentId())) {
                    MovedNodeVO moved = new MovedNodeVO();
                    moved.setNodeId(nodeId);
                    moved.setContent(toNode.getContent());
                    moved.setOldParentId(fromNode.getParentId());
                    moved.setNewParentId(toNode.getParentId());
                    movedNodes.add(moved);
                }
                
                if (!Objects.equals(fromNode.getContent(), toNode.getContent())) {
                    DiffNodeVO updated = createDiffNode(toNode);
                    updated.setChangeType("content");
                    updated.setOldValue(fromNode.getContent());
                    updated.setNewValue(toNode.getContent());
                    updatedNodes.add(updated);
                }
            }
        }
        
        // 查找删除
        for (Map.Entry<Long, NodeSnapshot> entry : fromNodes.entrySet()) {
            if (!toNodes.containsKey(entry.getKey())) {
                removedNodes.add(createDiffNode(entry.getValue()));
            }
        }
        
        diff.setAddedNodes(addedNodes);
        diff.setRemovedNodes(removedNodes);
        diff.setUpdatedNodes(updatedNodes);
        diff.setMovedNodes(movedNodes);
        
        VersionDiffVO.DiffStats stats = new VersionDiffVO.DiffStats();
        stats.setAddedCount(addedNodes.size());
        stats.setRemovedCount(removedNodes.size());
        stats.setUpdatedCount(updatedNodes.size());
        stats.setMovedCount(movedNodes.size());
        diff.setStats(stats);
        
        return diff;
    }
    
    /**
     * 创建差异节点
     */
    private DiffNodeVO createDiffNode(NodeSnapshot snapshot) {
        DiffNodeVO node = new DiffNodeVO();
        node.setNodeId(snapshot.getNodeId());
        node.setContent(snapshot.getContent());
        node.setParentId(snapshot.getParentId());
        node.setLevel(snapshot.getLevel());
        return node;
    }
    
    /**
     * 构建节点路径
     */
    private String buildNodePath(NodeSnapshot node, Map<Long, NodeSnapshot> allNodes) {
        List<String> path = new ArrayList<>();
        NodeSnapshot current = node;
        
        while (current != null) {
            path.add(0, current.getContent());
            if (current.getParentId() == null) {
                break;
            }
            current = allNodes.get(current.getParentId());
        }
        
        return String.join(" > ", path);
    }
    
    /**
     * 节点快照内部类
     */
    @lombok.Data
    private static class NodeSnapshot {
        private Long nodeId;
        private String content;
        private Long parentId;
        private Integer level;
        private String style;
    }
}
