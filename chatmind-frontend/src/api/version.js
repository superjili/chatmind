import request from './request'

export const versionApi = {
  // 获取文档版本列表
  getDocumentVersions(documentId, versionType) {
    return request.get(`/versions/document/${documentId}`, {
      params: versionType ? { versionType } : {}
    })
  },

  // 获取版本详情
  getVersion(versionId) {
    return request.get(`/versions/${versionId}`)
  },

  // 恢复到指定版本
  restoreVersion(data) {
    return request.post('/versions/restore', data)
  }
}
