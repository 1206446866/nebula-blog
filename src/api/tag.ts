// 标签相关接口。

import request from '@/utils/request'
import type { Result } from '@/types/result'
import type { Page } from '@/types/page'

/**
 * 标签
 */
export interface TagVO {
  id: number
  name: string
  description?: string
  createTime: string
  updateTime: string
  articleCount?: number
}

/**
 * 新增标签
 */
export interface CreateTagDTO {
  name: string
  description?: string
}

/**
 * 修改标签
 */
export interface UpdateTagDTO {
  id: number
  name: string
  description?: string
}

/**
 * 标签分页
 */
export function pageTagApi(name?: string, current = 1, size = 10, signal?: AbortSignal) {
  return request.get<Result<Page<TagVO>>>('/tags', {
    publicRead: true,
    signal,
    params: {
      name,
      current,
      size,
    },
  })
}

/**
 * 标签列表
 */
export function listTagApi(signal?: AbortSignal) {
  return request.get<Result<TagVO[]>>('/tags/list', { signal, publicRead: true })
}

export function getTagHome(signal?: AbortSignal) {
  return request.get<Result<TagVO[]>>('/api/tag', { signal, publicRead: true })
}

/**
 * 新增标签
 */
export function createTagApi(data: CreateTagDTO) {
  return request.post<Result<boolean>>('/tags/create', data)
}

/**
 * 修改标签
 */
export function updateTagApi(data: UpdateTagDTO) {
  return request.put<Result<boolean>>('/tags/edit', data)
}

/**
 * 删除标签
 */
export function deleteTagApi(id: number) {
  return request.delete<Result<boolean>>(`/tags/delete/${id}`)
}
