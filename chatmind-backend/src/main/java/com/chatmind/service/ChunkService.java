package com.chatmind.service;

import com.chatmind.dto.ChunkVO;
import com.chatmind.dto.NodeVO;
import com.chatmind.entity.PmNode;
import com.chatmind.repository.PmNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文档分片存储服务
 * 支持按层级/子树懒加载,优化大文档性能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkService {
    
    private final PmNodeRepository nodeRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String CHUNK_PREFIX = "chunk:";
    private static final int CHUNK_SIZE = 100; // 每个分片最多100个节点
    private static final int MAX_DEPTH_PER_CHUNK = 3; // 每个分片最多3层深度
    
    /**
     * 获取文档根分片(第一层)
     */
    public ChunkVO getRootChunk(Long documentId) {
        log.debug("获取文档根分片: {}", documentId);
        
        String cacheKey = CHUNK_PREFIX + documentId + ":root";
        
        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        ChunkVO cached = (ChunkVO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("命中缓存: {}", cacheKey);
            return cached;
        }
        
        // 获取根节点及其直接子节点
        List<PmNode> rootNodes = nodeRepository.findByDocumentIdAndParentIdAndDeleted(documentId, null, 0);
        
        if (rootNodes.isEmpty()) {
            log.warn("文档无根节点: {}", documentId);
            return null;
        }
        
        PmNode root = rootNodes.get(0);
        
        // 获取子节点(最多2层深度)
        List<PmNode> children = loadChildrenByDepth(documentId, root.getId(), root.getDepth(), MAX_DEPTH_PER_CHUNK);
        
        ChunkVO chunk = new ChunkVO();
        chunk.setChunkKey("root");
        chunk.setChunkType("root");
        chunk.setRootNode(convertToVO(root));
        chunk.setChildNodes(children.stream().map(this::convertToVO).collect(Collectors.toList()));
        chunk.setNodeCount(children.size() + 1);
        chunk.setMaxDepth(calculateMaxDepth(children));
        chunk.setHasMore(hasMoreChildren(children));
        
        // 缓存5分钟
        redisTemplate.opsForValue().set(cacheKey, chunk, 5, TimeUnit.MINUTES);
        
        log.debug("根分片加载完成: 节点数={}", chunk.getNodeCount());
        return chunk;
    }
    
    /**
     * 获取子树分片
     */
    public ChunkVO getSubtreeChunk(Long documentId, Long nodeId) {
        log.debug("获取子树分片: 文档={}, 节点={}", documentId, nodeId);
        
        String cacheKey = CHUNK_PREFIX + documentId + ":subtree:" + nodeId;
        
        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        ChunkVO cached = (ChunkVO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        PmNode rootNode = nodeRepository.findById(nodeId)
            .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));
        
        if (!rootNode.getDocumentId().equals(documentId)) {
            throw new RuntimeException("节点不属于该文档");
        }
        
        // 加载子树(限制深度)
        List<PmNode> children = loadChildrenByDepth(documentId, nodeId, rootNode.getDepth(), MAX_DEPTH_PER_CHUNK);
        
        ChunkVO chunk = new ChunkVO();
        chunk.setChunkKey("subtree:" + nodeId);
        chunk.setChunkType("subtree");
        chunk.setRootNode(convertToVO(rootNode));
        chunk.setChildNodes(children.stream().map(this::convertToVO).collect(Collectors.toList()));
        chunk.setNodeCount(children.size() + 1);
        chunk.setMaxDepth(calculateMaxDepth(children));
        chunk.setHasMore(hasMoreChildren(children));
        
        // 缓存5分钟
        redisTemplate.opsForValue().set(cacheKey, chunk, 5, TimeUnit.MINUTES);
        
        return chunk;
    }
    
    /**
     * 按层级加载节点(懒加载)
     */
    public List<NodeVO> loadNodesByLevel(Long documentId, Integer fromLevel, Integer toLevel) {
        log.debug("按层级加载: 文档={}, 层级范围={}-{}", documentId, fromLevel, toLevel);
        
        List<PmNode> nodes = nodeRepository.findByDocumentIdAndDeleted(documentId, 0);
        
        return nodes.stream()
            .filter(n -> n.getDepth() >= fromLevel && n.getDepth() <= toLevel)
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 清除文档分片缓存
     */
    public void clearCache(Long documentId) {
        log.info("清除文档分片缓存: {}", documentId);
        
        String pattern = CHUNK_PREFIX + documentId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("清除{}个缓存", keys.size());
        }
    }
    
    /**
     * 清除节点子树缓存
     */
    public void clearSubtreeCache(Long documentId, Long nodeId) {
        log.debug("清除子树缓存: 文档={}, 节点={}", documentId, nodeId);
        
        String cacheKey = CHUNK_PREFIX + documentId + ":subtree:" + nodeId;
        redisTemplate.delete(cacheKey);
    }
    
    /**
     * 按深度限制加载子节点
     */
    private List<PmNode> loadChildrenByDepth(Long documentId, Long parentId, Integer parentDepth, Integer maxDepth) {
        List<PmNode> result = new ArrayList<>();
        Queue<Long> queue = new LinkedList<>();
        queue.offer(parentId);
        
        int currentMaxDepth = parentDepth + maxDepth;
        
        while (!queue.isEmpty() && result.size() < CHUNK_SIZE) {
            Long currentId = queue.poll();
            List<PmNode> children = nodeRepository.findByDocumentIdAndParentIdAndDeleted(documentId, currentId, 0);
            
            for (PmNode child : children) {
                if (result.size() >= CHUNK_SIZE) {
                    break;
                }
                
                result.add(child);
                
                // 如果未达到最大深度,继续加载下一层
                if (child.getDepth() < currentMaxDepth) {
                    queue.offer(child.getId());
                }
            }
        }
        
        return result;
    }
    
    /**
     * 计算最大深度
     */
    private Integer calculateMaxDepth(List<PmNode> nodes) {
        return nodes.stream()
            .map(PmNode::getDepth)
            .max(Integer::compare)
            .orElse(0);
    }
    
    /**
     * 检查是否还有更多子节点
     */
    private Boolean hasMoreChildren(List<PmNode> nodes) {
        if (nodes.isEmpty()) {
            return false;
        }
        
        // 检查最后一层节点是否还有子节点
        Integer maxDepth = calculateMaxDepth(nodes);
        
        for (PmNode node : nodes) {
            if (node.getDepth().equals(maxDepth)) {
                List<PmNode> children = nodeRepository.findByDocumentIdAndParentIdAndDeleted(
                    node.getDocumentId(), node.getId(), 0);
                if (!children.isEmpty()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 转换为VO
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
        vo.setCreatedAt(node.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        vo.setUpdatedAt(node.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        return vo;
    }
}
