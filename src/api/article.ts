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
  userId: number
}

export const getArticlePageApi = (params: ArticlePageDTO) => {
  return request.get<Result<Page<ArticleVO>>>('/articles', { params })
}
/**
 * 根据ID查询文章详情
 */
export const getArticleByIdApi = (id: number) => {
  return request.get<Result<ArticleVO>>(`/articles/${id}`)
}

/**
 * 新增文章
 */
export const createArticleApi = (data: CreateArticleDto) => {
  return request.post('/articles/create', data)
}

/**
 * 修改文章
 */
export const updateArticleApi = (data: UpdateArticleDto) => {
  return request.put('/articles/edit', data)
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

export const getPublishedArticlePageApi = (params: ArticlePageDTO) => {
  return request.get<Result<Page<ArticleVO>>>('/articles/published', {
    params,
  })
}

/**
 * 点赞文章
 */
export const likeArticleApi = (data: ArticleLikeDto) => {
  return request.put<Result<boolean>>('/articles/like', data)
}
