import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig } from 'axios'

// 创建 axios 实例，基础地址指向后端
const request: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// 响应拦截：统一提取 data 字段
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API 请求错误:', error)
    return Promise.reject(error)
  }
)

export default request
