package com.chatmind.service;

import com.chatmind.dto.PresenceVO;
import com.chatmind.dto.SessionVO;
import com.chatmind.dto.UpdatePresenceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 实时协作会话服务
 * 管理在线用户presence信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String SESSION_PREFIX = "session:";
    private static final String PRESENCE_PREFIX = "presence:";
    private static final int SESSION_TTL = 3600; // 会话过期时间1小时
    private static final int PRESENCE_TTL = 300; // 在线状态过期时间5分钟
    
    // 用户颜色池
    private static final String[] USER_COLORS = {
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A", "#98D8C8",
        "#F7DC6F", "#BB8FCE", "#85C1E2", "#F8B195", "#C06C84"
    };
    
    /**
     * 加入会话
     */
    public SessionVO joinSession(Long documentId, Long userId, String username, String avatar) {
        log.info("用户加入会话: 文档={}, 用户={}", documentId, userId);
        
        String sessionKey = SESSION_PREFIX + documentId;
        String presenceKey = PRESENCE_PREFIX + documentId + ":" + userId;
        
        // 创建或更新presence
        PresenceVO presence = new PresenceVO();
        presence.setUserId(userId);
        presence.setUsername(username);
        presence.setAvatar(avatar);
        presence.setColor(assignUserColor(userId));
        presence.setStatus("online");
        presence.setJoinedAt(System.currentTimeMillis());
        presence.setLastActiveAt(System.currentTimeMillis());
        
        // 存储presence
        redisTemplate.opsForValue().set(presenceKey, presence, PRESENCE_TTL, TimeUnit.SECONDS);
        
        // 更新会话
        @SuppressWarnings("unchecked")
        SessionVO session = (SessionVO) redisTemplate.opsForValue().get(sessionKey);
        
        if (session == null) {
            session = new SessionVO();
            session.setSessionId(UUID.randomUUID().toString());
            session.setDocumentId(documentId);
            session.setCreatedAt(System.currentTimeMillis());
        }
        
        session.setLastActiveAt(System.currentTimeMillis());
        session.setOnlineUsers(getOnlineUsers(documentId));
        session.setActiveConnections(session.getOnlineUsers().size());
        
        redisTemplate.opsForValue().set(sessionKey, session, SESSION_TTL, TimeUnit.SECONDS);
        
        log.info("用户加入成功: 当前在线{}", session.getActiveConnections());
        return session;
    }
    
    /**
     * 离开会话
     */
    public void leaveSession(Long documentId, Long userId) {
        log.info("用户离开会话: 文档={}, 用户={}", documentId, userId);
        
        String presenceKey = PRESENCE_PREFIX + documentId + ":" + userId;
        redisTemplate.delete(presenceKey);
        
        // 更新会话
        updateSessionActivity(documentId);
    }
    
    /**
     * 更新用户在线状态
     */
    public PresenceVO updatePresence(Long documentId, UpdatePresenceRequest request) {
        log.debug("更新在线状态: 文档={}, 用户={}", documentId, request.getUserId());
        
        String presenceKey = PRESENCE_PREFIX + documentId + ":" + request.getUserId();
        
        @SuppressWarnings("unchecked")
        PresenceVO presence = (PresenceVO) redisTemplate.opsForValue().get(presenceKey);
        
        if (presence == null) {
            log.warn("用户未加入会话: {}", request.getUserId());
            return null;
        }
        
        // 更新状态
        if (request.getFocusNodeId() != null) {
            presence.setFocusNodeId(request.getFocusNodeId());
        }
        if (request.getSelectedNodeIds() != null) {
            presence.setSelectedNodeIds(request.getSelectedNodeIds());
        }
        if (request.getStatus() != null) {
            presence.setStatus(request.getStatus());
        }
        
        presence.setLastActiveAt(System.currentTimeMillis());
        
        // 刷新过期时间
        redisTemplate.opsForValue().set(presenceKey, presence, PRESENCE_TTL, TimeUnit.SECONDS);
        
        // 更新会话活跃时间
        updateSessionActivity(documentId);
        
        return presence;
    }
    
    /**
     * 获取会话信息
     */
    public SessionVO getSession(Long documentId) {
        log.debug("获取会话信息: {}", documentId);
        
        String sessionKey = SESSION_PREFIX + documentId;
        
        @SuppressWarnings("unchecked")
        SessionVO session = (SessionVO) redisTemplate.opsForValue().get(sessionKey);
        
        if (session != null) {
            session.setOnlineUsers(getOnlineUsers(documentId));
            session.setActiveConnections(session.getOnlineUsers().size());
        }
        
        return session;
    }
    
    /**
     * 获取在线用户列表
     */
    public List<PresenceVO> getOnlineUsers(Long documentId) {
        log.debug("获取在线用户: {}", documentId);
        
        String pattern = PRESENCE_PREFIX + documentId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<PresenceVO> users = new ArrayList<>();
        
        for (String key : keys) {
            @SuppressWarnings("unchecked")
            PresenceVO presence = (PresenceVO) redisTemplate.opsForValue().get(key);
            if (presence != null) {
                users.add(presence);
            }
        }
        
        // 按加入时间排序
        users.sort(Comparator.comparing(PresenceVO::getJoinedAt));
        
        return users;
    }
    
    /**
     * 心跳保持
     */
    public void heartbeat(Long documentId, Long userId) {
        log.debug("心跳: 文档={}, 用户={}", documentId, userId);
        
        String presenceKey = PRESENCE_PREFIX + documentId + ":" + userId;
        
        @SuppressWarnings("unchecked")
        PresenceVO presence = (PresenceVO) redisTemplate.opsForValue().get(presenceKey);
        
        if (presence != null) {
            presence.setLastActiveAt(System.currentTimeMillis());
            redisTemplate.opsForValue().set(presenceKey, presence, PRESENCE_TTL, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 清理过期会话
     */
    public void cleanupExpiredSessions() {
        log.info("清理过期会话");
        
        String pattern = SESSION_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys == null || keys.isEmpty()) {
            return;
        }
        
        int cleaned = 0;
        
        for (String key : keys) {
            @SuppressWarnings("unchecked")
            SessionVO session = (SessionVO) redisTemplate.opsForValue().get(key);
            
            if (session != null) {
                long inactiveTime = System.currentTimeMillis() - session.getLastActiveAt();
                
                // 超过1小时无活动则清理
                if (inactiveTime > SESSION_TTL * 1000) {
                    redisTemplate.delete(key);
                    cleaned++;
                }
            }
        }
        
        log.info("清理完成: 清理{}个过期会话", cleaned);
    }
    
    /**
     * 更新会话活跃时间
     */
    private void updateSessionActivity(Long documentId) {
        String sessionKey = SESSION_PREFIX + documentId;
        
        @SuppressWarnings("unchecked")
        SessionVO session = (SessionVO) redisTemplate.opsForValue().get(sessionKey);
        
        if (session != null) {
            session.setLastActiveAt(System.currentTimeMillis());
            session.setOnlineUsers(getOnlineUsers(documentId));
            session.setActiveConnections(session.getOnlineUsers().size());
            redisTemplate.opsForValue().set(sessionKey, session, SESSION_TTL, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 分配用户颜色
     */
    private String assignUserColor(Long userId) {
        int index = (int) (userId % USER_COLORS.length);
        return USER_COLORS[index];
    }
}
