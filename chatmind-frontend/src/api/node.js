import request from './request'

export const nodeApi = {
  // 获取文档节点
  getDocumentNodes(documentId) {
    return request.get(`/nodes/document/${documentId}`)
  },

  // 创建节点
  createNode(data) {
    return request.post('/nodes', data)
  },

  // 更新节点
  updateNode(id, data) {
    return request.put(`/nodes/${id}`, data)
  },

  // 删除节点
  deleteNode(id) {
    return request.delete(`/nodes/${id}`)
  }
}
