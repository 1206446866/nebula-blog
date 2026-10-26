import request from '@/utils/request'
import type { Result } from '@/types/result'
import type { Page } from '@/types/page'
import { createMediaQueue } from '@/utils/media-queue'
import { useAuthStore } from '@/stores/auth'
import { CanceledError } from 'axios'

const queue = createMediaQueue()
/** 排队前记录当前会话，避免旧任务在切换账号后以新账号身份执行。 */
function mediaWork<T>(task: (session: string) => Promise<T>, signal?: AbortSignal): Promise<T> {
  const session = useAuthStore().getToken()
  return queue(() => {
    if (!session || session !== useAuthStore().getToken()) throw new CanceledError('登录状态已变化')
    return task(session)
  }, signal)
}

export interface MediaAsset {
  id: number
  userId: number
  business: 'article' | 'avatar'
  path: string
  originalName: string
  contentType: string
  size: number
  width: number
  height: number
  createTime: string
}
export interface MediaEntry {
  asset: MediaAsset
  references: { business: string; id: number | null; title: string; readable: boolean }[]
}
export const uploadArticleImageApi = (file: File, signal?: AbortSignal) => {
  const data = new FormData()
  data.append('file', file)
  return mediaWork(
    (session) =>
      request.post<Result<MediaAsset>>('/media/images', data, {
        signal,
        timeout: 60000,
        headers: { Authorization: `Bearer ${session}` },
      }),
    signal,
  )
}
export const listMediaApi = (
  params: { current: number; size: number; business?: string; name?: string; all?: boolean },
  signal?: AbortSignal,
) => request.get<Result<Page<MediaEntry>>>('/media', { params, signal })
export const deleteMediaApi = (id: number) => request.delete<Result<boolean>>(`/media/${id}`)
export const mediaFileApi = (id: number, signal: AbortSignal) =>
  mediaWork(
    (session) =>
      request.get<Blob>(`/media/${id}/file`, {
        headers: { Authorization: `Bearer ${session}` },
        params: { thumbnail: true },
        responseType: 'blob',
        signal,
        silentError: true,
      }),
    signal,
  )
export const mediaImageApi = (path: string, signal: AbortSignal) =>
  mediaWork(
    (session) =>
      request.get<Blob>('/media/image', {
        headers: { Authorization: `Bearer ${session}` },
        params: { path },
        responseType: 'blob',
        signal,
        silentError: true,
      }),
    signal,
  )
