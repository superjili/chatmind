package com.chatmind.service;

import com.chatmind.dto.CreateShareRequest;
import com.chatmind.dto.ShareVO;
import com.chatmind.entity.PmDocument;
import com.chatmind.entity.PmShare;
import com.chatmind.repository.PmDocumentRepository;
import com.chatmind.repository.PmShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 分享服务
 * 管理文档分享链接
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareService {
    
    private final PmShareRepository shareRepository;
    private final PmDocumentRepository documentRepository;
    
    /**
     * 创建分享链接
     */
    @Transactional
    public ShareVO createShare(CreateShareRequest request) {
        log.info("创建分享链接: 文档={}, 权限={}", request.getDocumentId(), request.getPermission());
        
        // 验证文档存在
        PmDocument document = documentRepository.findById(request.getDocumentId())
            .orElseThrow(() -> new RuntimeException("文档不存在: " + request.getDocumentId()));
        
        PmShare share = new PmShare();
        share.setDocumentId(request.getDocumentId());
        share.setShareCode(generateShareCode());
        share.setPermission(request.getPermission());
        share.setRequirePassword(request.getRequirePassword() ? 1 : 0);
        
        if (request.getRequirePassword() && request.getPassword() != null) {
            share.setPassword(request.getPassword());
        }
        
        if (request.getExpireDays() != null) {
            share.setExpiresAt(LocalDateTime.now().plusDays(request.getExpireDays()));
        }
        
        share.setAccessCount(0);
        share.setCreatedBy(request.getCreatedBy());
        
        share = shareRepository.save(share);
        log.info("分享链接创建成功: {}", share.getShareCode());
        
        return convertToVO(share);
    }
    
    /**
     * 根据分享码获取分享信息
     */
    public ShareVO getShareByCode(String shareCode, String password) {
        log.info("访问分享链接: {}", shareCode);
        
        PmShare share = shareRepository.findByShareCode(shareCode);
        if (share == null || share.getDeleted() == 1) {
            throw new RuntimeException("分享链接不存在或已失效");
        }
        
        // 检查过期
        if (share.getExpiresAt() != null && LocalDateTime.now().isAfter(share.getExpiresAt())) {
            throw new RuntimeException("分享链接已过期");
        }
        
        // 检查密码
        if (share.getRequirePassword() == 1) {
            if (password == null || !password.equals(share.getPassword())) {
                throw new RuntimeException("密码错误");
            }
        }
        
        // 增加访问次数
        share.setAccessCount(share.getAccessCount() + 1);
        shareRepository.save(share);
        
        return convertToVO(share);
    }
    
    /**
     * 获取文档的所有分享链接
     */
    public List<ShareVO> getSharesByDocumentId(Long documentId) {
        log.debug("查询文档分享链接: {}", documentId);
        
        List<PmShare> shares = shareRepository.findByDocumentIdAndDeleted(documentId, 0);
        return shares.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 删除分享链接
     */
    @Transactional
    public void deleteShare(Long shareId) {
        log.info("删除分享链接: {}", shareId);
        
        PmShare share = shareRepository.findById(shareId)
            .orElseThrow(() -> new RuntimeException("分享链接不存在: " + shareId));
        
        share.setDeleted(1);
        shareRepository.save(share);
        
        log.info("分享链接已删除");
    }
    
    /**
     * 更新分享权限
     */
    @Transactional
    public ShareVO updateShare(Long shareId, String permission, Integer expireDays) {
        log.info("更新分享链接: {}", shareId);
        
        PmShare share = shareRepository.findById(shareId)
            .orElseThrow(() -> new RuntimeException("分享链接不存在: " + shareId));
        
        if (permission != null) {
            share.setPermission(permission);
        }
        
        if (expireDays != null) {
            if (expireDays > 0) {
                share.setExpiresAt(LocalDateTime.now().plusDays(expireDays));
            } else {
                share.setExpiresAt(null);
            }
        }
        
        share = shareRepository.save(share);
        log.info("分享链接已更新");
        
        return convertToVO(share);
    }
    
    /**
     * 生成分享码
     */
    private String generateShareCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // 检查是否重复
        if (shareRepository.findByShareCode(code.toString()) != null) {
            return generateShareCode(); // 递归重试
        }
        
        return code.toString();
    }
    
    /**
     * 转换为VO
     */
    private ShareVO convertToVO(PmShare share) {
        ShareVO vo = new ShareVO();
        vo.setId(share.getId());
        vo.setDocumentId(share.getDocumentId());
        vo.setShareCode(share.getShareCode());
        vo.setPermission(share.getPermission());
        vo.setRequirePassword(share.getRequirePassword() == 1);
        vo.setExpiresAt(share.getExpiresAt());
        vo.setAccessCount(share.getAccessCount());
        vo.setCreatedAt(share.getCreatedAt());
        
        // 判断是否过期
        boolean expired = false;
        if (share.getExpiresAt() != null) {
            expired = LocalDateTime.now().isAfter(share.getExpiresAt());
        }
        vo.setExpired(expired);
        
        return vo;
    }
}
