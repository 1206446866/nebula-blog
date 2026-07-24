import type { Result } from '@/types/result.ts'
import request from '@/utils/request.ts'
import type { Page } from '@/types/page.ts'

/**
 * 分类信息
 */
export interface CategoryVO {
  id: number
  name: string
  description?: string
  createTime?: string
  updateTime?: string
}

/**
 * 创建分类
 */
export interface CreateCategoryDTO {
  name: string
  description?: string
}

/**
 * 修改分类
 */
export interface UpdateCategoryDTO {
  id: number
  name: string
  description?: string
}

export function pageCategoryApi(params: { name?: string; currentPage?: number; size?: number }) {
  return request.get<Result<Page<CategoryVO>>>('/categories', {
    params,
  })
}

/**
 * 分类列表
 */
export function listCategoryApi() {
  return request.get<Result<CategoryVO[]>>('/categories/list')
}

export function createCategoryApi(data: CreateCategoryDTO) {
  return request.post<Result<boolean>>('/categories/create', data)
}

export function updateCategoryApi(data: UpdateCategoryDTO) {
  return request.put<Result<boolean>>('/categories/edit', data)
}

/**
 * 删除分类
 */
export function deleteCategoryApi(id: number) {
  return request.delete<Result<boolean>>(`/categories/delete/${id}`)
}
