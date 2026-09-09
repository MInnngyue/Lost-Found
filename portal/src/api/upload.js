import axios from 'axios'
import { ElMessage } from 'element-plus'
import { apiBaseUrl } from '@/utils/url'

const uploadClient = axios.create({
  baseURL: apiBaseUrl(),
  timeout: 30000,
})

uploadClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

uploadClient.interceptors.response.use(
  (res) => {
    if (res.data.code && res.data.code !== 200) {
      ElMessage.error(res.data.msg || res.data.message || '上传失败')
      return Promise.reject(new Error(res.data.msg || res.data.message))
    }
    return res.data
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
    } else if (error.response) {
      ElMessage.error(error.response.data?.msg || error.response.data?.message || '上传失败')
    } else {
      ElMessage.error('网络错误，无法连接服务器')
    }
    return Promise.reject(error)
  },
)

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('avatarfile', file)
  return uploadClient.post('/system/user/profile/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
