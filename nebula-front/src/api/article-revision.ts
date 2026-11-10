import request from '@/utils/request'
import type { Result } from '@/types/result'
import type { WritingDraft } from './article-workspace'

export interface ArticleRevision {
  id: number
  version: number
  title: string
  createdAt: string
}
export const getArticleRevisionsApi = (id: number, signal?: AbortSignal) =>
  request.get<Result<ArticleRevision[]>>(`/articles/${id}/revisions`, { signal })
export const getArticleRevisionApi = (id: number, revision: number, signal?: AbortSignal) =>
  request.get<Result<{ id: number; version: number; draft: WritingDraft }>>(
    `/articles/${id}/revisions/${revision}`,
    { signal },
  )
