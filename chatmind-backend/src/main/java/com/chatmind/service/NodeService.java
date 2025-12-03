package com.chatmind.service;

import com.chatmind.dto.CreateNodeRequest;
import com.chatmind.dto.NodeVO;
import com.chatmind.entity.PmNode;
import com.chatmind.repository.PmNodeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 节点业务服务
 * 处理节点的CRUD、拖拽、批量操作等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeService {
    
    private final PmNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 创建节点
     */
    @Transactional
    public NodeVO createNode(CreateNodeRequest request) {
        log.info("创建节点: {}, 父节点: {}", request.getContent(), request.getParentId());
        
        PmNode node = new PmNode();
        node.setDocumentId(request.getDocumentId());
        node.setParentId(request.getParentId());
        node.setContent(request.getContent());
        node.setColor(request.getColor());
        node.setIcon(request.getIcon());
        node.setDescription(request.getDescription());
        node.setLabels(request.getLabels());
        
        // 计算深度
        if (request.getParentId() != null) {
            PmNode parentNode = nodeRepository.findById(request.getParentId())
                .orElseThrow(() -> new RuntimeException("父节点不存在: " + request.getParentId()));
            node.setDepth(parentNode.getDepth() + 1);
            
            // 更新父节点的子节点排序
            updateParentChildrenOrder(parentNode, node);
        } else {
            node.setDepth(0);
        }
        
        node = nodeRepository.save(node);
        log.info("节点创建成功,ID: {}", node.getId());
        return convertToVO(node);
    }
    
    /**
     * 获取文档的所有节点
     */
    public List<NodeVO> getNodesByDocumentId(Long documentId) {
        log.debug("查询文档节点: {}", documentId);
        List<PmNode> nodes = nodeRepository.findByDocumentIdAndDeleted(documentId, 0);
        return nodes.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 根据ID获取节点
     */
    public NodeVO getNodeById(Long id) {
        log.debug("查询节点: {}", id);
        PmNode node = nodeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + id));
        return convertToVO(node);
    }
    
    /**
     * 获取子节点列表
     */
    public List<NodeVO> getChildNodes(Long parentId) {
        log.debug("查询子节点: {}", parentId);
        List<PmNode> nodes = nodeRepository.findByParentIdAndDeleted(parentId, 0);
        return nodes.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 更新节点
     */
    @Transactional
    public NodeVO updateNode(Long id, CreateNodeRequest request) {
        log.info("更新节点: {}", id);
        
        PmNode node = nodeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + id));
        
        if (request.getContent() != null) {
            node.setContent(request.getContent());
        }
        if (request.getColor() != null) {
            node.setColor(request.getColor());
        }
        if (request.getIcon() != null) {
            node.setIcon(request.getIcon());
        }
        if (request.getDescription() != null) {
            node.setDescription(request.getDescription());
        }
        if (request.getLabels() != null) {
            node.setLabels(request.getLabels());
        }
        
        node = nodeRepository.save(node);
        log.info("节点更新成功: {}", id);
        return convertToVO(node);
    }
    
    /**
     * 批量更新节点(用于批量设置颜色/标签等)
     */
    @Transactional
    public List<NodeVO> batchUpdateNodes(List<Long> nodeIds, String color, String labels) {
        log.info("批量更新节点,数量: {}", nodeIds.size());
        
        List<PmNode> nodes = nodeRepository.findAllById(nodeIds);
        
        for (PmNode node : nodes) {
            if (color != null) {
                node.setColor(color);
            }
            if (labels != null) {
                node.setLabels(labels);
            }
        }
        
        nodes = nodeRepository.saveAll(nodes);
        log.info("批量更新完成");
        
        return nodes.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 删除节点(逻辑删除)
     */
    @Transactional
    public void deleteNode(Long id) {
        log.info("删除节点: {}", id);
        
        PmNode node = nodeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + id));
        
        // 递归删除所有子节点
        deleteNodeRecursive(node);
        
        log.info("节点删除成功: {}", id);
    }
    
    /**
     * 切换折叠状态
     */
    @Transactional
    public NodeVO toggleCollapse(Long id) {
        log.info("切换节点折叠状态: {}", id);
        
        PmNode node = nodeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + id));
        
        node.setCollapsed(node.getCollapsed() == 1 ? 0 : 1);
        node = nodeRepository.save(node);
        
        log.info("折叠状态已切换: {} -> {}", id, node.getCollapsed());
        return convertToVO(node);
    }
    
    /**
     * 递归删除节点及其子节点
     */
    private void deleteNodeRecursive(PmNode node) {
        // 删除当前节点
        node.setDeleted(1);
        nodeRepository.save(node);
        
        // 递归删除子节点
        List<PmNode> children = nodeRepository.findByParentIdAndDeleted(node.getId(), 0);
        for (PmNode child : children) {
            deleteNodeRecursive(child);
        }
    }
    
    /**
     * 更新父节点的子节点排序
     */
    private void updateParentChildrenOrder(PmNode parentNode, PmNode newChild) {
        try {
            List<Long> childrenOrder;
            if (parentNode.getChildrenOrder() != null && !parentNode.getChildrenOrder().isEmpty()) {
                childrenOrder = objectMapper.readValue(
                    parentNode.getChildrenOrder(), 
                    new TypeReference<List<Long>>() {}
                );
            } else {
                childrenOrder = new ArrayList<>();
            }
            
            // 添加新子节点ID(先保存以获取ID,实际场景中可能需要调整)
            // 注意:这里newChild可能还没有ID,需要先保存
            
            parentNode.setChildrenOrder(objectMapper.writeValueAsString(childrenOrder));
            nodeRepository.save(parentNode);
        } catch (Exception e) {
            log.error("更新子节点排序失败", e);
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
