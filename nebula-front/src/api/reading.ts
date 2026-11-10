import request from '@/utils/request'
import type { Result } from '@/types/result'

export interface ReadingState {
  favorite: boolean
  later: boolean
  progress: number
  anchor: string
  readAt: string | null
}
export type LibraryKind = 'favorite' | 'later' | 'history'
export interface ReadingItem {
  articleId: number
  title: string
  author: string
  state: ReadingState
}
export interface ReadingLibrary {
  records: ReadingItem[]
  total: number
  current: number
  size: number
}

export const getReadingApi = (id: number, signal?: AbortSignal) =>
  request.get<Result<ReadingState>>(`/reading/${id}`, { signal, silentError: true })
export const setReadingFlagsApi = (id: number, flags: Pick<ReadingState, 'favorite' | 'later'>) =>
  request.put<Result<ReadingState>>(`/reading/${id}/flags`, flags)
export const saveReadingProgressApi = (id: number, progress: number, anchor: string) =>
  request.put<Result<ReadingState>>(
    `/reading/${id}/progress`,
    { progress, anchor },
    { silentError: true },
  )
export const getReadingLibraryApi = (kind: LibraryKind, current: number, signal?: AbortSignal) =>
  request.get<Result<ReadingLibrary>>('/reading', { params: { kind, current, size: 20 }, signal })
export const clearReadingHistoryApi = () => request.delete<Result<boolean>>('/reading/history')
