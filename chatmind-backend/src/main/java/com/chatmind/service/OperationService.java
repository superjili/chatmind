package com.chatmind.service;

import com.chatmind.dto.OperationVO;
import com.chatmind.dto.RecordOperationRequest;
import com.chatmind.entity.PmOperation;
import com.chatmind.repository.PmOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作记录服务
 * 记录所有编辑操作,支持CRDT实时协作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationService {
    
    private final PmOperationRepository operationRepository;
    
    /**
     * 记录操作
     */
    @Transactional
    public OperationVO recordOperation(RecordOperationRequest request) {
        log.debug("记录操作: opId={}, type={}", request.getOpId(), request.getOpType());
        
        // 检查opId是否已存在(幂等性)
        PmOperation existing = operationRepository.findByOpId(request.getOpId());
        if (existing != null) {
            log.debug("操作已存在,跳过: {}", request.getOpId());
            return convertToVO(existing);
        }
        
        PmOperation operation = new PmOperation();
        operation.setDocumentId(request.getDocumentId());
        operation.setOpId(request.getOpId());
        operation.setOpType(request.getOpType());
        operation.setNodeId(request.getNodeId());
        operation.setPayload(request.getOpData());
        operation.setUserId(request.getUserId());
        
        // 设置因果时间戳(如果未提供则使用当前时间)
        if (request.getCausalityTimestamp() != null) {
            operation.setCausalityTimestamp(request.getCausalityTimestamp());
        } else {
            operation.setCausalityTimestamp(System.currentTimeMillis());
        }
        
        operation = operationRepository.save(operation);
        log.debug("操作记录成功: ID={}", operation.getId());
        
        return convertToVO(operation);
    }
    
    /**
     * 获取文档的操作历史
     */
    public List<OperationVO> getOperations(Long documentId, LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("查询操作历史: 文档={}, 时间范围={} ~ {}", documentId, startTime, endTime);
        
        List<PmOperation> operations;
        
        if (startTime != null && endTime != null) {
            operations = operationRepository.findByDocumentIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                documentId, startTime, endTime);
        } else {
            operations = operationRepository.findByDocumentIdOrderByCreatedAtAsc(documentId);
        }
        
        return operations.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取最近的操作
     */
    public List<OperationVO> getRecentOperations(Long documentId, Integer limit) {
        log.debug("查询最近操作: 文档={}, 数量={}", documentId, limit);
        
        List<PmOperation> operations = operationRepository
            .findByDocumentIdOrderByCreatedAtDesc(documentId);
        
        if (limit != null && operations.size() > limit) {
            operations = operations.subList(0, limit);
        }
        
        return operations.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 回放操作(用于版本恢复)
     */
    public List<OperationVO> getOperationsForReplay(Long documentId, Long fromTimestamp, Long toTimestamp) {
        log.info("获取回放操作: 文档={}, 时间戳范围={} ~ {}", documentId, fromTimestamp, toTimestamp);
        
        List<PmOperation> operations = operationRepository
            .findByDocumentIdAndCausalityTimestampBetweenOrderByCausalityTimestampAsc(
                documentId, fromTimestamp, toTimestamp);
        
        return operations.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 清理旧操作记录(保留最近30天)
     */
    @Transactional
    public void cleanupOldOperations(Long documentId) {
        log.info("清理旧操作记录: 文档={}", documentId);
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
        List<PmOperation> oldOperations = operationRepository
            .findByDocumentIdAndCreatedAtBeforeOrderByCreatedAtAsc(documentId, cutoffTime);
        
        if (!oldOperations.isEmpty()) {
            // 保留最近1000条,删除更早的
            if (oldOperations.size() > 1000) {
                List<PmOperation> toDelete = oldOperations.subList(1000, oldOperations.size());
                operationRepository.deleteAll(toDelete);
                log.info("清理完成: 删除{}条旧记录", toDelete.size());
            }
        }
    }
    
    /**
     * 转换为VO
     */
    private OperationVO convertToVO(PmOperation operation) {
        OperationVO vo = new OperationVO();
        vo.setId(operation.getId());
        vo.setDocumentId(operation.getDocumentId());
        vo.setOpId(operation.getOpId());
        vo.setOpType(operation.getOpType());
        vo.setNodeId(operation.getNodeId());
        vo.setOpData(operation.getPayload());
        vo.setUserId(operation.getUserId());
        vo.setCausalityTimestamp(operation.getCausalityTimestamp());
        vo.setCreatedAt(operation.getCreatedAt());
        return vo;
    }
}
