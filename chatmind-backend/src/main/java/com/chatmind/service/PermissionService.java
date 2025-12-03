package com.chatmind.service;

import com.chatmind.dto.GrantAclRequest;
import com.chatmind.entity.PmDocument;
import com.chatmind.entity.PmDocumentAcl;
import com.chatmind.repository.PmDocumentAclRepository;
import com.chatmind.repository.PmDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 权限服务
 * 管理文档的ACL权限
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {
    
    private final PmDocumentAclRepository aclRepository;
    private final PmDocumentRepository documentRepository;
    
    /**
     * 授予权限
     */
    @Transactional
    public void grantPermission(GrantAclRequest request) {
        log.info("授予权限: 文档={}, 用户={}, 权限={}", 
            request.getDocumentId(), request.getUserId(), request.getPermission());
        
        // 验证文档存在
        PmDocument document = documentRepository.findById(request.getDocumentId())
            .orElseThrow(() -> new RuntimeException("文档不存在: " + request.getDocumentId()));
        
        // 检查是否已有ACL
        PmDocumentAcl existingAcl = aclRepository.findByDocumentIdAndUserIdAndDeleted(
            request.getDocumentId(), request.getUserId(), 0);
        
        if (existingAcl != null) {
            // 更新现有权限
            existingAcl.setPermission(request.getPermission());
            aclRepository.save(existingAcl);
            log.info("权限已更新");
        } else {
            // 创建新ACL
            PmDocumentAcl acl = new PmDocumentAcl();
            acl.setDocumentId(request.getDocumentId());
            acl.setUserId(request.getUserId());
            acl.setPermission(request.getPermission());
            acl.setGrantedBy(request.getGrantedBy());
            aclRepository.save(acl);
            log.info("权限已授予");
        }
    }
    
    /**
     * 撤销权限
     */
    @Transactional
    public void revokePermission(Long documentId, Long userId) {
        log.info("撤销权限: 文档={}, 用户={}", documentId, userId);
        
        PmDocumentAcl acl = aclRepository.findByDocumentIdAndUserIdAndDeleted(documentId, userId, 0);
        if (acl != null) {
            acl.setDeleted(1);
            aclRepository.save(acl);
            log.info("权限已撤销");
        }
    }
    
    /**
     * 检查用户是否有权限
     */
    public boolean hasPermission(Long documentId, Long userId, String requiredPermission) {
        log.debug("检查权限: 文档={}, 用户={}, 所需权限={}", documentId, userId, requiredPermission);
        
        // 检查文档所有者
        PmDocument document = documentRepository.findById(documentId).orElse(null);
        if (document != null && document.getOwnerId().equals(userId)) {
            return true; // 所有者拥有所有权限
        }
        
        // 检查公开文档的读权限
        if ("read".equals(requiredPermission) && document != null && "public".equals(document.getVisibility())) {
            return true;
        }
        
        // 检查ACL
        PmDocumentAcl acl = aclRepository.findByDocumentIdAndUserIdAndDeleted(documentId, userId, 0);
        if (acl == null) {
            return false;
        }
        
        // 权限层级: owner > edit > read
        String userPermission = acl.getPermission();
        
        if ("owner".equals(userPermission)) {
            return true;
        }
        
        if ("edit".equals(userPermission)) {
            return "edit".equals(requiredPermission) || "read".equals(requiredPermission);
        }
        
        if ("read".equals(userPermission)) {
            return "read".equals(requiredPermission);
        }
        
        return false;
    }
    
    /**
     * 获取文档的所有ACL
     */
    public List<PmDocumentAcl> getDocumentAcls(Long documentId) {
        log.debug("查询文档ACL: {}", documentId);
        return aclRepository.findByDocumentIdAndDeleted(documentId, 0);
    }
    
    /**
     * 获取用户有权限的文档列表
     */
    public List<PmDocumentAcl> getUserAcls(Long userId) {
        log.debug("查询用户ACL: {}", userId);
        return aclRepository.findByUserIdAndDeleted(userId, 0);
    }
}
