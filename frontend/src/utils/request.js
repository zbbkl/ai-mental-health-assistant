import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 请求的前缀
  timeout: 15000, // 请求的超时时间
})

// 登录失效的业务码。后端有两种返回形态，都要处理：
// 1) 过滤器直接返回 HTTP 401（token 缺失/非法/过期），包体里带 code=401
// 2) 业务层返回 HTTP 200，包体里 code 为下面的取值（A0230=token无效/过期/拉黑，A0231=token被禁止访问，A0301=未授权）
// 注意不要把 403（无权限访问该功能）算进来：普通用户访问管理端接口只该看到提示，不该被登出。
const AUTH_FAILURE_CODES = ['401', 'A0230', 'A0231', 'A0301']

// 清理登录态并跳转登录页
const handleAuthFailure = (message) => {
  ElMessage.error(message || '登录状态已过期，请重新登录')
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  if (!window.location.pathname.startsWith('/auth/login')) {
    window.location.href = '/auth/login'
  }
}

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
    const { data, config } = response

    // 处理业务状态码
    if (data.code === '200') {
      return data.data
    }

    // 参数校验失败时后端把具体字段提示放在 data 里，优先展示它
    const detail = typeof data.data === 'string' && data.data ? data.data : ''
    const message = detail || data.msg || '请求失败'

    // 登录失效（HTTP 200 + 业务码）也走统一处理，不依赖 HTTP 状态码
    if (AUTH_FAILURE_CODES.includes(data.code) && !config.url?.includes('/login')) {
      handleAuthFailure(message)
      return Promise.reject(new Error(message))
    }

    ElMessage.error(message)
    // 非成功响应必须走 reject，否则调用方的 .then 会把失败当成功处理
    return Promise.reject(new Error(message))
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data

    if (!error.response) {
      ElMessage.error('无法连接后端服务，请确认后端已启动')
      return Promise.reject(error)
    }

    if (status === 401 || AUTH_FAILURE_CODES.includes(body?.code)) {
      if (!error.config?.url?.includes('/login')) {
        handleAuthFailure(body?.msg)
        return Promise.reject(error)
      }
    }

    ElMessage.error(body?.msg || `请求失败（${status}）`)
    return Promise.reject(error)
  }
)

export default service
