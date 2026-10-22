import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'

import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '../stores/auth.ts'
import { belongsToCurrentSession } from './auth-session'
import { getHttpErrorMessage } from './http-error'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 👇 关键：声明返回 T（不是 AxiosResponse）
declare module 'axios' {
  export interface AxiosRequestConfig {
    skipAuthRedirect?: boolean
  }
  export interface AxiosInstance {
    get<T = AxiosResponse['data']>(url: string, config?: AxiosRequestConfig): Promise<T>
    post<T = AxiosResponse['data']>(
      url: string,
      data?: unknown,
      config?: AxiosRequestConfig,
    ): Promise<T>
    put<T = AxiosResponse['data']>(
      url: string,
      data?: unknown,
      config?: AxiosRequestConfig,
    ): Promise<T>
    delete<T = AxiosResponse['data']>(url: string, config?: AxiosRequestConfig): Promise<T>
  }
}

/**
 * 请求拦截器
 */
service.interceptors.request.use((config) => {
  const userStore = useAuthStore()
  const token = userStore.getToken()
  if (token && !config.headers.Authorization) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function expireCurrentSession(config: AxiosRequestConfig) {
  const auth = useAuthStore()
  if (
    config.skipAuthRedirect ||
    !belongsToCurrentSession(config.headers?.Authorization, auth.getToken())
  )
    return
  auth.clearAuth()
  ElMessage.error('登录状态已失效，请重新登录')
  void router.replace('/login')
}

/**
 * 响应拦截器
 */
service.interceptors.response.use(
  (response) => {
    const { code, message } = response.data
    if (code !== 200) {
      if (code === 401 && !response.config.skipAuthRedirect) expireCurrentSession(response.config)
      else ElMessage.error(message || '请求失败')
      return Promise.reject(response.data)
    }
    return response.data
  },

  (error) => {
    const status = error.response?.status
    if (status === 401) {
      if (error.config?.skipAuthRedirect) {
        ElMessage.error(getHttpErrorMessage(status, error.response?.data))
      } else {
        expireCurrentSession(error.config || {})
      }
    } else {
      ElMessage.error(getHttpErrorMessage(status, error.response?.data))
    }
    return Promise.reject(error)
  },
)

export default service
