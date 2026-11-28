import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'

import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '../stores/auth.ts'
import { belongsToCurrentSession } from './auth-session'
import { getHttpErrorMessage, getRetryAfterSeconds } from './http-error'
import { isGuestRoute, loginLocation } from '@/router/policy'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 👇 关键：声明返回 T（不是 AxiosResponse）
declare module 'axios' {
  export interface AxiosRequestConfig {
    skipAuthRedirect?: boolean
    publicRead?: boolean
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
}, undefined, { synchronous: true })

function sessionChanged(config?: AxiosRequestConfig) {
  return (
    typeof config?.sessionToken === 'string' && config.sessionToken !== useAuthStore().getToken()
  )
}

function expireCurrentSession(config: AxiosRequestConfig) {
  const auth = useAuthStore()
  if (
    config.skipAuthRedirect ||
    config.publicRead ||
    !belongsToCurrentSession(config.headers?.Authorization, auth.getToken())
  )
    return false
  auth.clearAuth()
  const route = router.currentRoute.value
  if (isGuestRoute(route)) {
    ElMessage.warning('登录状态已失效，当前可继续公开阅读')
  } else {
    ElMessage.error('登录状态已失效，请重新登录')
    void router.replace(route.meta.public ? '/login' : loginLocation(route.fullPath))
  }
  return true
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
      const expired = code === 401 && expireCurrentSession(response.config)
      if (!expired && !response.config.silentError) ElMessage.error(message || '请求失败')
      return Promise.reject(response.data)
    }
    return response.data
  },

  (error) => {
    if (sessionChanged(error.config))
      return Promise.reject(new axios.CanceledError('登录状态已变化'))
    const status = error.response?.status
    if (status === 401) {
      const expired = expireCurrentSession(error.config || {})
      const authorization = error.config?.headers?.Authorization
      const wasCurrent = !authorization || belongsToCurrentSession(authorization, useAuthStore().getToken())
      if (!expired && !error.config?.silentError && wasCurrent)
        ElMessage.error(getHttpErrorMessage(status, error.response?.data))
    } else if (!error.config?.silentError && error.code !== 'ERR_CANCELED') {
      const retry = getRetryAfterSeconds(error.response?.headers?.['retry-after'])
      const message = getHttpErrorMessage(status, error.response?.data)
      ElMessage.error(error.config?.publicRead && retry && (status === 429 || status === 503)
        ? `${message}（${retry} 秒后可重试）` : message)
    }
    return Promise.reject(error)
  },
)

export default service
