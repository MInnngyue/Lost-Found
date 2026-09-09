import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/utils/error'
import { apiBaseUrl } from '@/utils/url'

// 创建 axios 实例
const request = axios.create({
  baseURL: apiBaseUrl(),
  timeout: 10000,
})

// 请求拦截器：自动携带 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token && !config.skipAuth && !config.headers.Authorization) {
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 响应拦截器：统一处理 Result 结构
request.interceptors.response.use(
  (response) => {
    const res = response.data
    const handleErrorLocally = response.config.handleErrorLocally

    // 后端统一成功码
    if (res.code === 200) {
      return res
    }

    // token 无效或未登录
    if (res.code === 401) {
      if (!handleErrorLocally) {
        ElMessage.error(res.msg || res.message || '登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
      }
      return Promise.reject(res)
    }

    // 无权限
    if (res.code === 403) {
      if (!handleErrorLocally) ElMessage.error(res.msg || res.message || '没有权限访问')
      return Promise.reject(res)
    }

    if (!handleErrorLocally) ElMessage.error(res.msg || res.message || '请求失败')
    return Promise.reject(res)
  },
  (error) => {
    const handleErrorLocally = error.config?.handleErrorLocally
    const status = error.response?.status

    if (!handleErrorLocally) {
      ElMessage.error(getErrorMessage(error))

      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  },
)

export default request
