import axios from 'axios'
import type { AxiosInstance } from 'axios'

// 创建 axios 实例，基础地址指向后端
const request: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截：自动携带 Token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一提取 data 字段，处理 401
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      window.location.href = '/login'
    }
    console.error('API 请求错误:', error)
    return Promise.reject(error)
  }
)

export default request
