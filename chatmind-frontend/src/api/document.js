import request from './request'

export const documentApi = {
  // 创建文档
  createDocument(data) {
    return request.post('/documents', data)
  },

  // 获取文档详情
  getDocument(id) {
    return request.get(`/documents/${id}`)
  },

  // 更新文档
  updateDocument(id, data) {
    return request.put(`/documents/${id}`, data)
  },

  // 删除文档
  deleteDocument(id) {
    return request.delete(`/documents/${id}`)
  },

  // 获取用户文档列表
  getUserDocuments(userId) {
    return request.get(`/documents/user/${userId}`)
  }
}
