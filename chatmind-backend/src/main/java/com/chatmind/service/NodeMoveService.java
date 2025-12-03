package com.chatmind.service;

import com.chatmind.dto.MoveNodeRequest;
import com.chatmind.dto.NodeVO;
import com.chatmind.entity.PmNode;
import com.chatmind.entity.PmOperation;
import com.chatmind.repository.PmNodeRepository;
import com.chatmind.repository.PmOperationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.*;

/**
 * 节点拖拽与重排序服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeMoveService {
    
    private final PmNodeRepository nodeRepository;
    private final PmOperationRepository operationRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 移动节点(拖拽)
     * 支持改变parentId和childrenOrder,防止循环引用
     */
    @Transactional
    public NodeVO moveNode(MoveNodeRequest request) {
        log.info("移动节点: {} -> 新父节点: {}, 位置: {}", 
            request.getNodeId(), request.getNewParentId(), request.getTargetIndex());
        
        // 1. 获取要移动的节点
        PmNode node = nodeRepository.findById(request.getNodeId())
            .orElseThrow(() -> new RuntimeException("节点不存在: " + request.getNodeId()));
        
        Long oldParentId = node.getParentId();
        Long newParentId = request.getNewParentId();
        
        // 2. 防止循环引用检查
        if (newParentId != null) {
            checkCircularReference(node.getId(), newParentId);
        }
        
        // 3. 从原父节点的子节点列表中移除
        if (oldParentId != null) {
            removeFromParentChildren(oldParentId, node.getId());
        }
        
        // 4. 添加到新父节点的子节点列表
        if (newParentId != null) {
            addToParentChildren(newParentId, node.getId(), request.getTargetIndex());
            
            // 更新深度
            PmNode newParent = nodeRepository.findById(newParentId)
                .orElseThrow(() -> new RuntimeException("新父节点不存在: " + newParentId));
            updateNodeDepth(node, newParent.getDepth() + 1);
        } else {
            // 移动到根节点
            updateNodeDepth(node, 0);
        }
        
        // 5. 更新节点的父节点ID
        node.setParentId(newParentId);
        node = nodeRepository.save(node);
        
        // 6. 记录操作
        recordMoveOperation(request, oldParentId, newParentId);
        
        log.info("节点移动成功: {}", node.getId());
        return convertToVO(node);
    }
    
    /**
     * 重新排序子节点
     */
    @Transactional
    public void reorderChildren(Long parentId, List<Long> orderedChildrenIds) {
        log.info("重新排序子节点,父节点: {}, 新顺序: {}", parentId, orderedChildrenIds);
        
        PmNode parent = nodeRepository.findById(parentId)
            .orElseThrow(() -> new RuntimeException("父节点不存在: " + parentId));
        
        try {
            parent.setChildrenOrder(objectMapper.writeValueAsString(orderedChildrenIds));
            nodeRepository.save(parent);
            log.info("子节点排序更新成功");
        } catch (Exception e) {
            log.error("更新子节点排序失败", e);
            throw new RuntimeException("更新排序失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查循环引用
     * 确保不会将节点移动到它自己的子节点下
     */
    private void checkCircularReference(Long nodeId, Long targetParentId) {
        if (nodeId.equals(targetParentId)) {
            throw new RuntimeException("不能将节点移动到自己下面");
        }
        
        Set<Long> visited = new HashSet<>();
        Long currentId = targetParentId;
        
        while (currentId != null) {
            if (visited.contains(currentId)) {
                throw new RuntimeException("检测到循环引用");
            }
            
            if (currentId.equals(nodeId)) {
                throw new RuntimeException("不能将节点移动到其子节点下");
            }
            
            visited.add(currentId);
            
            PmNode current = nodeRepository.findById(currentId).orElse(null);
            if (current == null) break;
            
            currentId = current.getParentId();
            
            // 防止无限循环
            if (visited.size() > 100) {
                throw new RuntimeException("节点层级过深,可能存在数据问题");
            }
        }
    }
    
    /**
     * 从父节点的子节点列表中移除
     */
    private void removeFromParentChildren(Long parentId, Long childId) {
        PmNode parent = nodeRepository.findById(parentId).orElse(null);
        if (parent == null || parent.getChildrenOrder() == null) {
            return;
        }
        
        try {
            List<Long> children = objectMapper.readValue(
                parent.getChildrenOrder(), 
                new TypeReference<List<Long>>() {}
            );
            
            children.remove(childId);
            
            parent.setChildrenOrder(objectMapper.writeValueAsString(children));
            nodeRepository.save(parent);
        } catch (Exception e) {
            log.error("从父节点移除子节点失败", e);
        }
    }
    
    /**
     * 添加到父节点的子节点列表
     */
    private void addToParentChildren(Long parentId, Long childId, Integer targetIndex) {
        PmNode parent = nodeRepository.findById(parentId)
            .orElseThrow(() -> new RuntimeException("父节点不存在: " + parentId));
        
        try {
            List<Long> children;
            if (parent.getChildrenOrder() != null && !parent.getChildrenOrder().isEmpty()) {
                children = objectMapper.readValue(
                    parent.getChildrenOrder(), 
                    new TypeReference<List<Long>>() {}
                );
            } else {
                children = new ArrayList<>();
            }
            
            // 插入到指定位置
            if (targetIndex != null && targetIndex >= 0 && targetIndex <= children.size()) {
                children.add(targetIndex, childId);
            } else {
                children.add(childId);
            }
            
            parent.setChildrenOrder(objectMapper.writeValueAsString(children));
            nodeRepository.save(parent);
        } catch (Exception e) {
            log.error("添加子节点到父节点失败", e);
            throw new RuntimeException("添加子节点失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 递归更新节点深度
     */
    private void updateNodeDepth(PmNode node, int newDepth) {
        node.setDepth(newDepth);
        nodeRepository.save(node);
        
        // 递归更新所有子节点的深度
        List<PmNode> children = nodeRepository.findByParentIdAndDeleted(node.getId(), 0);
        for (PmNode child : children) {
            updateNodeDepth(child, newDepth + 1);
        }
    }
    
    /**
     * 记录移动操作
     */
    private void recordMoveOperation(MoveNodeRequest request, Long oldParentId, Long newParentId) {
        try {
            PmNode node = nodeRepository.findById(request.getNodeId()).orElse(null);
            if (node == null) return;
            
            PmOperation operation = new PmOperation();
            operation.setOpId(UUID.randomUUID().toString());
            operation.setDocumentId(node.getDocumentId());
            operation.setNodeId(request.getNodeId());
            operation.setUserId(request.getUserId() != null ? request.getUserId() : 1L);
            operation.setOpType("move");
            
            // 构建操作负载
            Map<String, Object> payload = new HashMap<>();
            payload.put("oldParentId", oldParentId);
            payload.put("newParentId", newParentId);
            payload.put("targetIndex", request.getTargetIndex());
            
            operation.setPayload(objectMapper.writeValueAsString(payload));
            operationRepository.save(operation);
            
            log.debug("移动操作已记录,opId: {}", operation.getOpId());
        } catch (Exception e) {
            log.error("记录移动操作失败", e);
        }
    }
    
    /**
     * 转换为VO对象
     */
    private NodeVO convertToVO(PmNode node) {
        NodeVO vo = new NodeVO();
        vo.setId(node.getId());
        vo.setDocumentId(node.getDocumentId());
        vo.setParentId(node.getParentId());
        vo.setContent(node.getContent());
        vo.setChildrenOrder(node.getChildrenOrder());
        vo.setLabels(node.getLabels());
        vo.setColor(node.getColor());
        vo.setIcon(node.getIcon());
        vo.setDescription(node.getDescription());
        vo.setCollapsed(node.getCollapsed());
        vo.setPosition(node.getPosition());
        vo.setDepth(node.getDepth());
        vo.setMetadata(node.getMetadata());
        vo.setCreatedAt(node.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        vo.setUpdatedAt(node.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return vo;
    }
}
