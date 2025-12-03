import request from './request'

/**
 * 协作会话API
 */
export const sessionApi = {
  /**
   * 加入会话
   */
  joinSession(documentId, userId, username, avatar) {
    return request.post(`/sessions/document/${documentId}/join`, null, {
      params: { userId, username, avatar }
    })
  },

  /**
   * 离开会话
   */
  leaveSession(documentId, userId) {
    return request.post(`/sessions/document/${documentId}/leave`, null, {
      params: { userId }
    })
  },

  /**
   * 更新在线状态
   */
  updatePresence(documentId, data) {
    return request.put(`/sessions/document/${documentId}/presence`, data)
  },

  /**
   * 获取会话信息
   */
  getSession(documentId) {
    return request.get(`/sessions/document/${documentId}`)
  },

  /**
   * 获取在线用户列表
   */
  getOnlineUsers(documentId) {
    return request.get(`/sessions/document/${documentId}/users`)
  },

  /**
   * 心跳保持
   */
  heartbeat(documentId, userId) {
    return request.post(`/sessions/document/${documentId}/heartbeat`, null, {
      params: { userId }
    })
  }
}
