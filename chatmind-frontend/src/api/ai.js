import request from './request'

export const aiApi = {
  // AI生成脑图
  generateMindmap(data) {
    return request.post('/ai/generate', data, {
      timeout: 180000  // AI生成3分钟超时
    })
  },

  // AI扩展节点
  expandNode(data) {
    return request.post('/ai/expand', data, {
      timeout: 120000
    })
  },

  // AI总结
  summarize(data) {
    return request.post('/ai/summarize', data, {
      timeout: 120000
    })
  }
}
