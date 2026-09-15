import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 请求的前缀
  timeout: 5000, // 请求的超时时间
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 在发送请求之前做些什么
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['token'] = token
    }
    return config
  },
  (error) => {
    // 对请求错误做些什么
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    // 对响应数据做点什么
    const { data, config } = response
    // 处理业务状态码
    if (data.code === '200') {
        return data.data
    }

    // 参数校验失败时后端把具体字段提示放在 data 里，优先展示它
    const detail = typeof data.data === 'string' && data.data ? data.data : ''
    const message = detail || data.msg || '请求失败'

    if (data.code === '-1' && !config.url?.includes('/login')) {
      ElMessage.error(message)
      // 清除登录信息
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/auth/login'
      return Promise.reject(new Error(message))
    }

    ElMessage.error(message)
    // 非成功响应必须走 reject，否则调用方的 .then 会把失败当成功处理
    return Promise.reject(new Error(message))
  },
  (error) => {
    // 对响应错误做点什么
    const status = error.response?.status
    if (status === 401) {
      ElMessage.error('登录状态已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/auth/login'
    } else if (!error.response) {
      ElMessage.error('无法连接后端服务，请确认后端已启动')
    } else {
      ElMessage.error(error.response.data?.msg || `请求失败（${status}）`)
    }
    return Promise.reject(error)
  }
)

export default service