import request from '@/utils/request'
import type { Result } from '@/types/result'

export interface WritingDraft {
  title: string
  content: string
  categoryId?: number | null
  tagIds: number[]
  sourceVersion: number
}
export interface WritingBackup {
  revision: number
  draft: WritingDraft | null
  updatedAt: string | null
}

export const getWritingBackupApi = (key: string, signal?: AbortSignal) =>
  request.get<Result<WritingBackup>>(`/article-workspaces/${key}`, { signal, silentError: true })

export const saveWritingBackupApi = (key: string, revision: number, draft: WritingDraft | null) =>
  request.put<Result<WritingBackup>>(
    `/article-workspaces/${key}`,
    { revision, draft },
    { silentError: true },
  )
