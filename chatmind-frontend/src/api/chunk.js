import request from './request'

/**
 * 文档分片API
 */
export const chunkApi = {
  /**
   * 获取根分片(第一层节点)
   */
  getRootChunk(documentId) {
    return request.get(`/chunks/document/${documentId}/root`)
  },

  /**
   * 获取子树分片
   */
  getSubtreeChunk(documentId, nodeId) {
    return request.get(`/chunks/document/${documentId}/subtree/${nodeId}`)
  },

  /**
   * 按层级加载节点
   */
  loadNodesByLevel(documentId, fromLevel, toLevel) {
    return request.get(`/chunks/document/${documentId}/levels`, {
      params: { fromLevel, toLevel }
    })
  },

  /**
   * 清除文档分片缓存
   */
  clearCache(documentId) {
    return request.delete(`/chunks/document/${documentId}/cache`)
  }
}
