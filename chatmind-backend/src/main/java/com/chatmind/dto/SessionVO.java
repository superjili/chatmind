package com.chatmind.dto;

import lombok.Data;
import java.util.List;

/**
 * 协作会话VO
 */
@Data
public class SessionVO {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 当前在线用户列表
     */
    private List<PresenceVO> onlineUsers;
    
    /**
     * 活跃连接数
     */
    private Integer activeConnections;
    
    /**
     * 会话创建时间
     */
    private Long createdAt;
    
    /**
     * 最后活跃时间
     */
    private Long lastActiveAt;
}
