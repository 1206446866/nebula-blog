import axios, { type AxiosInstance } from 'axios'

import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '../stores/auth.ts'
import { useLoadingStore } from '@/stores/loading.ts'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 👇 关键：声明返回 T（不是 AxiosResponse）
declare module 'axios' {
  export interface AxiosInstance {
    get<T = any>(url: string, config?: any): Promise<T>
    post<T = any>(url: string, data?: any, config?: any): Promise<T>
    put<T = any>(url: string, data?: any, config?: any): Promise<T>
    delete<T = any>(url: string, config?: any): Promise<T>
  }
}

/**
 * 请求拦截器
 */
service.interceptors.request.use((config) => {
  const userStore = useAuthStore()
  const loadingStore = useLoadingStore()
  loadingStore.start()
  const token = userStore.getToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 响应拦截器
 */
service.interceptors.response.use(
  (response) => {
    const loadingStore = useLoadingStore()
    loadingStore.end()
    const { code, message } = response.data
    if (code !== 200) {
      ElMessage.error(message || '请求失败')
      return Promise.reject(response.data)
    }
    return response.data
  },

  (error) => {
    const loadingStore = useLoadingStore()
    loadingStore.end()
    const status = error.response?.status
    /**
     * 登录过期
     */
    if (status === 401) {
      localStorage.removeItem('token')
      ElMessage.error('登录已过期')
      router.push('/login')
    }

    if (status === 403) {
      ElMessage.error('权限不足')
    }

    if (status >= 500) {
      ElMessage.error('服务器异常')
    }
    return Promise.reject(error)
  },
)

export default service
