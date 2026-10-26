/** 客户端请求错误可展示接口公开提示，服务端异常统一使用通用提示。 */
export function getHttpErrorMessage(status: number | undefined, data: unknown): string {
  const message =
    data && typeof data === 'object' && 'message' in data && typeof data.message === 'string'
      ? data.message.trim()
      : ''
  if (!status) return '连接失败，请检查网络后重试'
  if (status === 503) return message || '服务暂时不可用，请稍后重试'
  if (status === 507) return '存储空间不足，暂时无法上传图片'
  if (status >= 500) return '服务器异常，请稍后重试'
  if (message) return message
  if (status === 401) return '身份验证失败，请重新登录'
  if (status === 403) return '权限不足'
  if (status === 404) return '请求的资源不存在'
  if (status === 409) return '数据状态已变化，请刷新后重试'
  if (status === 413) return '上传文件过大'
  if (status === 429) return '操作过于频繁，请稍后重试'
  if (status === 400) return '请求参数有误，请检查后重试'
  return '请求失败，请稍后重试'
}

export function getRetryAfterSeconds(value: unknown): number {
  if (typeof value !== 'number' && typeof value !== 'string') return 0
  const seconds = Number(value)
  return Number.isFinite(seconds) && seconds > 0 ? Math.min(86_400, Math.ceil(seconds)) : 0
}
