import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'

import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '../stores/auth.ts'
import { belongsToCurrentSession } from './auth-session'
import { getHttpErrorMessage } from './http-error'
import { loginLocation } from '@/router/policy'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 👇 关键：声明返回 T（不是 AxiosResponse）
declare module 'axios' {
  export interface AxiosRequestConfig {
    skipAuthRedirect?: boolean
    silentError?: boolean
    sessionToken?: string
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
  const authorization = config.headers.Authorization
  config.sessionToken =
    typeof authorization === 'string' && authorization.startsWith('Bearer ')
      ? authorization.substring(7)
      : token
  return config
})

function sessionChanged(config?: AxiosRequestConfig) {
  return (
    typeof config?.sessionToken === 'string' && config.sessionToken !== useAuthStore().getToken()
  )
}

function expireCurrentSession(config: AxiosRequestConfig) {
  const auth = useAuthStore()
  if (
    config.skipAuthRedirect ||
    !belongsToCurrentSession(config.headers?.Authorization, auth.getToken())
  )
    return
  auth.clearAuth()
  ElMessage.error('登录状态已失效，请重新登录')
  void router.replace(
    router.currentRoute.value.meta.public
      ? '/login'
      : loginLocation(router.currentRoute.value.fullPath),
  )
}

/**
 * 响应拦截器
 */
service.interceptors.response.use(
  (response) => {
    if (sessionChanged(response.config))
      return Promise.reject(new axios.CanceledError('登录状态已变化'))
    if (response.config.responseType === 'blob') return response.data
    const { code, message } = response.data
    if (code !== 200) {
      if (code === 401 && !response.config.skipAuthRedirect) expireCurrentSession(response.config)
      else if (!response.config.silentError) ElMessage.error(message || '请求失败')
      return Promise.reject(response.data)
    }
    return response.data
  },

  (error) => {
    if (sessionChanged(error.config))
      return Promise.reject(new axios.CanceledError('登录状态已变化'))
    const status = error.response?.status
    if (status === 401) {
      if (error.config?.skipAuthRedirect) {
        if (!error.config?.silentError)
          ElMessage.error(getHttpErrorMessage(status, error.response?.data))
      } else {
        expireCurrentSession(error.config || {})
      }
    } else if (!error.config?.silentError && error.code !== 'ERR_CANCELED') {
      ElMessage.error(getHttpErrorMessage(status, error.response?.data))
    }
    return Promise.reject(error)
  },
)

export default service
