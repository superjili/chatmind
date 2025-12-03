import request from './request'

/**
 * 操作记录API
 */
export const operationApi = {
  /**
   * 获取文档操作历史
   */
  getOperations(documentId, startTime, endTime) {
    return request.get(`/operations/document/${documentId}`, {
      params: { startTime, endTime }
    })
  },

  /**
   * 获取最近操作
   */
  getRecentOperations(documentId, limit = 100) {
    return request.get(`/operations/document/${documentId}/recent`, {
      params: { limit }
    })
  },

  /**
   * 获取回放操作(用于版本恢复)
   */
  getOperationsForReplay(documentId, fromTimestamp, toTimestamp) {
    return request.get(`/operations/document/${documentId}/replay`, {
      params: { fromTimestamp, toTimestamp }
    })
  },

  /**
   * 清理旧操作记录
   */
  cleanupOldOperations(documentId) {
    return request.delete(`/operations/document/${documentId}/cleanup`)
  }
}

/**
 * 差异对比API
 */
export const diffApi = {
  /**
   * 对比两个版本
   */
  compareVersions(fromVersionId, toVersionId) {
    return request.get('/diff/versions', {
      params: { fromVersionId, toVersionId }
    })
  },

  /**
   * 对比当前版本与历史版本
   */
  compareWithCurrent(documentId, versionId) {
    return request.get('/diff/current', {
      params: { documentId, versionId }
    })
  }
}
