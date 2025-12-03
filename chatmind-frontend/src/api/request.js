import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 120000  // 2分钟超时
})

request.interceptors.request.use(
  config => {
    // TODO: 添加token
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    // 如果是arraybuffer或blob类型，直接返回原始响应
    if (response.config.responseType === 'arraybuffer' || response.config.responseType === 'blob') {
      return response
    }
    
    const res = response.data
    if (res.code !== 0) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    return Promise.reject(error)
  }
)

export default request
