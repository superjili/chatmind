package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.CreateShareRequest;
import com.chatmind.dto.GrantAclRequest;
import com.chatmind.dto.ShareVO;
import com.chatmind.entity.PmDocumentAcl;
import com.chatmind.service.PermissionService;
import com.chatmind.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限与分享控制器
 */
@Tag(name = "权限与分享", description = "文档权限管理和分享链接接口")
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {
    
    private final ShareService shareService;
    private final PermissionService permissionService;
    
    /**
     * 创建分享链接
     */
    @Operation(summary = "创建分享", description = "创建文档分享链接")
    @PostMapping
    public ApiResult<ShareVO> createShare(@Valid @RequestBody CreateShareRequest request) {
        ShareVO share = shareService.createShare(request);
        return ApiResult.ok(share);
    }
    
    /**
     * 获取分享信息(访问分享链接)
     */
    @Operation(summary = "访问分享", description = "通过分享码访问文档")
    @GetMapping("/{shareCode}")
    public ApiResult<ShareVO> getShare(
        @PathVariable String shareCode,
        @RequestParam(required = false) String password
    ) {
        ShareVO share = shareService.getShareByCode(shareCode, password);
        return ApiResult.ok(share);
    }
    
    /**
     * 获取文档的分享列表
     */
    @Operation(summary = "分享列表", description = "获取文档的所有分享链接")
    @GetMapping("/document/{documentId}")
    public ApiResult<List<ShareVO>> getDocumentShares(@PathVariable Long documentId) {
        List<ShareVO> shares = shareService.getSharesByDocumentId(documentId);
        return ApiResult.ok(shares);
    }
    
    /**
     * 删除分享
     */
    @Operation(summary = "删除分享", description = "删除分享链接")
    @DeleteMapping("/{shareId}")
    public ApiResult<Void> deleteShare(@PathVariable Long shareId) {
        shareService.deleteShare(shareId);
        return ApiResult.ok();
    }
    
    /**
     * 更新分享
     */
    @Operation(summary = "更新分享", description = "更新分享权限或过期时间")
    @PutMapping("/{shareId}")
    public ApiResult<ShareVO> updateShare(
        @PathVariable Long shareId,
        @RequestParam(required = false) String permission,
        @RequestParam(required = false) Integer expireDays
    ) {
        ShareVO share = shareService.updateShare(shareId, permission, expireDays);
        return ApiResult.ok(share);
    }
    
    /**
     * 授予权限
     */
    @Operation(summary = "授予权限", description = "授予用户文档访问权限")
    @PostMapping("/acl/grant")
    public ApiResult<Void> grantPermission(@Valid @RequestBody GrantAclRequest request) {
        permissionService.grantPermission(request);
        return ApiResult.ok();
    }
    
    /**
     * 撤销权限
     */
    @Operation(summary = "撤销权限", description = "撤销用户文档访问权限")
    @DeleteMapping("/acl/{documentId}/{userId}")
    public ApiResult<Void> revokePermission(
        @PathVariable Long documentId,
        @PathVariable Long userId
    ) {
        permissionService.revokePermission(documentId, userId);
        return ApiResult.ok();
    }
    
    /**
     * 获取文档ACL列表
     */
    @Operation(summary = "ACL列表", description = "获取文档的权限列表")
    @GetMapping("/acl/document/{documentId}")
    public ApiResult<List<PmDocumentAcl>> getDocumentAcls(@PathVariable Long documentId) {
        List<PmDocumentAcl> acls = permissionService.getDocumentAcls(documentId);
        return ApiResult.ok(acls);
    }
    
    /**
     * 检查权限
     */
    @Operation(summary = "检查权限", description = "检查用户是否有权限")
    @GetMapping("/acl/check")
    public ApiResult<Boolean> checkPermission(
        @RequestParam Long documentId,
        @RequestParam Long userId,
        @RequestParam String permission
    ) {
        boolean hasPermission = permissionService.hasPermission(documentId, userId, permission);
        return ApiResult.ok(hasPermission);
    }
}
