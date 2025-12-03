import request from './request'

/**
 * 导入导出API
 */
export const exportApi = {
  /**
   * 导出文档(文本格式)
   */
  exportDocument(documentId, format) {
    return request.post('/export', {
      documentId,
      format
    }, {
      responseType: 'arraybuffer'  // 使用arraybuffer接收二进制数据
    })
  },

  /**
   * 导出为PNG图片
   */
  exportPNG(documentId, width = 1920, height = 1080) {
    return request.post('/export', {
      documentId,
      format: 'png',
      imageWidth: width,
      imageHeight: height
    })
  },

  /**
   * 导出为SVG图片
   */
  exportSVG(documentId) {
    return request.post('/export', {
      documentId,
      format: 'svg'
    })
  },

  /**
   * 导入文档
   */
  importDocument(data) {
    return request.post('/export/import', data)
  }
}

/**
 * 搜索API
 */
export const searchApi = {
  /**
   * 搜索(POST)
   */
  search(data) {
    return request.post('/search', data)
  },

  /**
   * 快速搜索(GET)
   */
  quickSearch(keyword, documentId, userId, page = 1, pageSize = 20) {
    return request.get('/search', {
      params: { keyword, documentId, userId, page, pageSize }
    })
  }
}
