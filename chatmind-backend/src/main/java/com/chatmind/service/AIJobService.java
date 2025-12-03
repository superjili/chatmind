package com.chatmind.service;

import com.chatmind.dto.AIJobVO;
import com.chatmind.entity.PmAiJob;
import com.chatmind.repository.PmAiJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI任务流程管理服务
 * 管理LLM调用状态、成本估算、校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIJobService {
    
    private final PmAiJobRepository aiJobRepository;
    
    // GPT-4定价(每1K tokens)
    private static final double INPUT_COST_PER_1K = 0.03;   // $0.03/1K tokens
    private static final double OUTPUT_COST_PER_1K = 0.06;  // $0.06/1K tokens
    
    // 平均tokens估算
    private static final int AVG_CHINESE_CHAR_TOKENS = 2;   // 中文字符约2 tokens
    private static final int AVG_ENGLISH_WORD_TOKENS = 1;   // 英文单词约1 token
    
    /**
     * 创建AI任务
     */
    @Transactional
    public AIJobVO createJob(Long userId, Long documentId, String jobType, String inputData) {
        log.info("创建AI任务: 用户={}, 类型={}", userId, jobType);
        
        // 估算tokens和成本
        int estimatedTokens = estimateTokens(inputData);
        double estimatedCost = estimateCost(estimatedTokens);
        
        log.debug("预估消耗: tokens={}, 成本=${}", estimatedTokens, estimatedCost);
        
        PmAiJob job = new PmAiJob();
        job.setUserId(userId);
        job.setDocumentId(documentId);
        job.setJobType(jobType);
        job.setStatus("pending");
        job.setPrompt(inputData);
        job.setCostEstimate(estimatedTokens);
        
        job = aiJobRepository.save(job);
        
        return convertToVO(job);
    }
    
    /**
     * 开始执行任务
     */
    @Transactional
    public void startJob(Long jobId) {
        log.info("开始执行任务: {}", jobId);
        
        PmAiJob job = aiJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("任务不存在: " + jobId));
        
        job.setStatus("running");
        // 开始执行
        aiJobRepository.save(job);
    }
    
    /**
     * 更新任务进度
     */
    @Transactional
    public void updateProgress(Long jobId, Integer progress) {
        log.debug("更新任务进度: ID={}, 进度={}%", jobId, progress);
        
        PmAiJob job = aiJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("任务不存在: " + jobId));
        
        // 更新进度
        aiJobRepository.save(job);
    }
    
    /**
     * 完成任务
     */
    @Transactional
    public void completeJob(Long jobId, String outputData, Integer actualTokens) {
        log.info("任务完成: ID={}, tokens={}", jobId, actualTokens);
        
        PmAiJob job = aiJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("任务不存在: " + jobId));
        
        double actualCost = estimateCost(actualTokens);
        
        job.setStatus("completed");
        job.setResultData(outputData);
        job.setActualTokens(actualTokens);
        job.setCompletedAt(LocalDateTime.now());
        
        aiJobRepository.save(job);
        
        log.info("任务完成统计: 预估tokens={}, 实际tokens={}, 成本=${}", 
            job.getCostEstimate(), actualTokens, 0.0);
    }
    
    /**
     * 任务失败
     */
    @Transactional
    public void failJob(Long jobId, String errorMessage) {
        log.error("任务失败: ID={}, 错误={}", jobId, errorMessage);
        
        PmAiJob job = aiJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("任务不存在: " + jobId));
        
        job.setStatus("failed");
        job.setErrorMessage(errorMessage);
        job.setCompletedAt(LocalDateTime.now());
        
        aiJobRepository.save(job);
    }
    
    /**
     * 取消任务
     */
    @Transactional
    public void cancelJob(Long jobId) {
        log.info("取消任务: {}", jobId);
        
        PmAiJob job = aiJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("任务不存在: " + jobId));
        
        if ("completed".equals(job.getStatus()) || "failed".equals(job.getStatus())) {
            throw new RuntimeException("任务已结束,无法取消");
        }
        
        job.setStatus("cancelled");
        job.setCompletedAt(LocalDateTime.now());
        
        aiJobRepository.save(job);
    }
    
    /**
     * 获取任务详情
     */
    public AIJobVO getJob(Long jobId) {
        PmAiJob job = aiJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("任务不存在: " + jobId));
        
        return convertToVO(job);
    }
    
    /**
     * 获取用户任务列表
     */
    public List<AIJobVO> getUserJobs(Long userId, Integer limit) {
        List<PmAiJob> jobs = aiJobRepository.findByUserIdAndDeletedOrderByCreatedAtDesc(userId, 0);
        
        if (limit != null && jobs.size() > limit) {
            jobs = jobs.subList(0, limit);
        }
        
        return jobs.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取文档任务列表
     */
    public List<AIJobVO> getDocumentJobs(Long documentId) {
        List<PmAiJob> jobs = aiJobRepository.findByDocumentIdAndDeletedOrderByCreatedAtDesc(documentId, 0);
        
        return jobs.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 统计用户成本
     */
    public Double calculateUserCost(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        List<PmAiJob> jobs;
        
        if (startTime != null && endTime != null) {
            jobs = aiJobRepository.findByUserIdAndDeletedOrderByCreatedAtDesc(userId, 0);
        } else {
            jobs = aiJobRepository.findByUserIdAndDeletedOrderByCreatedAtDesc(userId, 0);
        }
        
        return jobs.stream()
            .filter(j -> j.getActualTokens() != null)
            .mapToDouble(j -> estimateCost(j.getActualTokens()))
            .sum();
    }
    
    /**
     * 估算tokens数量
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // 简单估算:中文字符*2 + 英文单词*1
        int chineseCount = 0;
        int englishCount = 0;
        
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FA5) {
                chineseCount++;
            } else if (Character.isLetter(c)) {
                englishCount++;
            }
        }
        
        int englishWords = englishCount > 0 ? englishCount / 5 : 0; // 假设平均5个字母一个单词
        
        return chineseCount * AVG_CHINESE_CHAR_TOKENS + englishWords * AVG_ENGLISH_WORD_TOKENS;
    }
    
    /**
     * 估算成本
     */
    private double estimateCost(int tokens) {
        if (tokens == 0) {
            return 0.0;
        }
        
        // 假设输入输出各占50%
        double inputCost = (tokens / 2.0 / 1000) * INPUT_COST_PER_1K;
        double outputCost = (tokens / 2.0 / 1000) * OUTPUT_COST_PER_1K;
        
        return inputCost + outputCost;
    }
    
    /**
     * 转换为VO
     */
    private AIJobVO convertToVO(PmAiJob job) {
        AIJobVO vo = new AIJobVO();
        vo.setId(job.getId());
        vo.setJobType(job.getJobType());
        vo.setStatus(job.getStatus());
        vo.setInputData(job.getPrompt());
        vo.setOutputData(job.getResultData());
        vo.setErrorMessage(job.getErrorMessage());
        vo.setProgress(50); // 简化进度
        vo.setEstimatedTokens(job.getCostEstimate());
        vo.setActualTokens(job.getActualTokens());
        vo.setEstimatedCost(estimateCost(job.getCostEstimate() != null ? job.getCostEstimate() : 0));
        vo.setActualCost(job.getActualTokens() != null ? estimateCost(job.getActualTokens()) : null);
        
        if (job.getCreatedAt() != null) {
            vo.setCreatedAt(job.getCreatedAt().atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli());
        }
        
        if (job.getCompletedAt() != null) {
            vo.setCompletedAt(job.getCompletedAt().atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli());
            
            if (job.getCreatedAt() != null) {
                vo.setDuration(vo.getCompletedAt() - vo.getCreatedAt());
            }
        }
        
        return vo;
    }
}
