package com.chatmind.controller;

import com.chatmind.common.ApiResult;
import com.chatmind.dto.PresenceVO;
import com.chatmind.dto.SessionVO;
import com.chatmind.dto.UpdatePresenceRequest;
import com.chatmind.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 实时协作会话控制器
 */
@Tag(name = "协作会话", description = "实时协作会话管理,用户在线状态")
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {
    
    private final SessionService sessionService;
    
    /**
     * 加入会话
     */
    @Operation(summary = "加入会话", description = "用户加入文档协作会话")
    @PostMapping("/document/{documentId}/join")
    public ApiResult<SessionVO> joinSession(
        @PathVariable Long documentId,
        @RequestParam Long userId,
        @RequestParam String username,
        @RequestParam(required = false) String avatar
    ) {
        SessionVO session = sessionService.joinSession(documentId, userId, username, avatar);
        return ApiResult.ok(session);
    }
    
    /**
     * 离开会话
     */
    @Operation(summary = "离开会话", description = "用户离开协作会话")
    @PostMapping("/document/{documentId}/leave")
    public ApiResult<Void> leaveSession(
        @PathVariable Long documentId,
        @RequestParam Long userId
    ) {
        sessionService.leaveSession(documentId, userId);
        return ApiResult.ok();
    }
    
    /**
     * 更新在线状态
     */
    @Operation(summary = "更新状态", description = "更新用户在线状态(焦点节点、选中等)")
    @PutMapping("/document/{documentId}/presence")
    public ApiResult<PresenceVO> updatePresence(
        @PathVariable Long documentId,
        @Valid @RequestBody UpdatePresenceRequest request
    ) {
        PresenceVO presence = sessionService.updatePresence(documentId, request);
        return ApiResult.ok(presence);
    }
    
    /**
     * 获取会话信息
     */
    @Operation(summary = "会话信息", description = "获取文档的协作会话信息")
    @GetMapping("/document/{documentId}")
    public ApiResult<SessionVO> getSession(@PathVariable Long documentId) {
        SessionVO session = sessionService.getSession(documentId);
        return ApiResult.ok(session);
    }
    
    /**
     * 获取在线用户
     */
    @Operation(summary = "在线用户", description = "获取当前在线用户列表")
    @GetMapping("/document/{documentId}/users")
    public ApiResult<List<PresenceVO>> getOnlineUsers(@PathVariable Long documentId) {
        List<PresenceVO> users = sessionService.getOnlineUsers(documentId);
        return ApiResult.ok(users);
    }
    
    /**
     * 心跳保持
     */
    @Operation(summary = "心跳", description = "保持在线状态心跳")
    @PostMapping("/document/{documentId}/heartbeat")
    public ApiResult<Void> heartbeat(
        @PathVariable Long documentId,
        @RequestParam Long userId
    ) {
        sessionService.heartbeat(documentId, userId);
        return ApiResult.ok();
    }
}
