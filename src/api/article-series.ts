import request from '@/utils/request'
import type { Result } from '@/types/result'
export interface ArticleSeries {
  id: number
  userId: number
  title: string
  description: string
  publicVisible: boolean
  articleCount: number
}
export interface SeriesArticle {
  id: number
  title: string
  status: number
}
export interface SeriesDetail {
  series: ArticleSeries
  articles: SeriesArticle[]
}
export interface SeriesInput {
  title: string
  description: string
  publicVisible: boolean
  articleIds: number[]
}
export const getArticleSeriesApi = (mine: boolean, current: number, signal?: AbortSignal) =>
  request.get<Result<{ records: ArticleSeries[]; total: number }>>('/series', {
    params: { mine, current },
    signal,
  })
export const getSeriesDetailApi = (id: number, editing = false, signal?: AbortSignal) =>
  request.get<Result<SeriesDetail>>(`/series/${id}${editing ? '/edit' : ''}`, { signal })
export const saveArticleSeriesApi = (id: number | undefined, data: SeriesInput) =>
  id
    ? request.put<Result<number>>(`/series/${id}`, data)
    : request.post<Result<number>>('/series', data)
export const deleteArticleSeriesApi = (id: number) =>
  request.delete<Result<boolean>>(`/series/${id}`)
export const getArticleSeriesLinksApi = (id: number, signal?: AbortSignal) =>
  request.get<Result<ArticleSeries[]>>(`/series/for-article/${id}`, { signal, silentError: true })
