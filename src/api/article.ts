import request from '@/utils/request'
import type { Result } from '../types/result.ts'
import type { Page } from '@/types/page.ts'

export interface ArticleVO {
  id: number
  userId: number
  title: string
  content: string
  author: string
  viewCount: number
  likeCount: number
  createTime: string
  status: number
  categoryId?: number
  tagIds?: number[]
  liked?: boolean
  scheduledPublishAt?: string | null
  scheduleError?: string | null
}

export interface ArticlePageDTO {
  userId?: number
  title?: string
  author?: string
  tagIds?: number[]
  categoryIds?: number[]
  current?: number
  size?: number
}

/**
 * 创建文章DTO
 */
export interface CreateArticleDto {
  title: string
  content: string
  categoryId?: number
  tagIds?: number[]
}

/**
 * 修改文章DTO
 */
export interface UpdateArticleDto {
  id: number
  title: string
  content: string
  categoryId?: number
  tagIds?: number[]
}

/**
 * 切换状态DTO
 */
export interface ChangeArticleStatusDto {
  id: number
  status: number
}

export interface ArticleLikeDto {
  articleId: number
}

export const getArticlePageApi = (params: ArticlePageDTO, signal?: AbortSignal) => {
  return request.get<Result<Page<ArticleVO>>>('/articles', { params, signal })
}
/**
 * 根据ID查询文章详情
 */
export const getArticleByIdApi = (id: number, signal?: AbortSignal) => {
  return request.get<Result<ArticleVO>>(`/articles/${id}`, { signal })
}

export const createDraftApi = (data: CreateArticleDto) => {
  return request.post<Result<number>>('/articles/drafts', data)
}

/** 编辑器读取文章时不增加阅读量。 */
export const getArticleForEditApi = (id: number, signal?: AbortSignal) => {
  return request.get<Result<ArticleVO>>(`/articles/${id}/edit`, { signal })
}

/**
 * 修改文章
 */
export const updateArticleApi = (data: UpdateArticleDto) => {
  return request.put<Result<boolean>>('/articles/edit', data)
}

/**
 * 删除文章
 */
export const deleteArticleApi = (id: number) => {
  return request.delete(`/articles/${id}`)
}

/**
 * 切换状态
 */
export const changeArticleStatusApi = (data: ChangeArticleStatusDto) => {
  return request.put<Result<boolean>>('/articles/status', data)
}

export const scheduleArticleApi = (id: number, publishAt: string) =>
  request.put<Result<string>>(`/articles/${id}/publication`, { publishAt })

export const cancelArticleScheduleApi = (id: number) =>
  request.delete<Result<boolean>>(`/articles/${id}/publication`)

export const getPublishedArticlePageApi = (params: ArticlePageDTO, signal?: AbortSignal) => {
  return request.get<Result<Page<ArticleVO>>>('/articles/published', {
    params,
    signal,
  })
}

/**
 * 点赞文章
 */
export const likeArticleApi = (data: ArticleLikeDto) => {
  return request.put<Result<boolean>>('/articles/like', data)
}
