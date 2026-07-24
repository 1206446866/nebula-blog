// src/api/auth.ts
import request from '@/utils/request'
import type { Result } from '../types/result'
import type { Page } from '@/types/page.ts'

export interface UserProfile {
  id: number
  username: string
  role: string
  articleCount: number
  commentCount: number
  totalViewCount: number
  recentArticles: UserArticleVO
}

export interface UserArticleVO {
  id: number
  title: string
  viewCount: number
}

export interface UserVO {
  id: number
  nid: String
  username: string
  avatar: string
  roleIds: number[]
  status?: number
  createTime: String
}

/**
 * 编辑用户DTO
 */
export interface EditUserDTO {
  id: number
  username: string
  roleIds: number[]
}

// 新增：获取用户列表
export const getUsersApi = (role?: string, username?: string, current = 1, size = 10) => {
  return request.get<Result<Page<UserVO>>>('/users', {
    params: {
      role,
      username,
      current,
      size,
    },
  })
}

export const switchStatusByIdApi = (id: number, status: number) => {
  return request.put<Result<boolean>>(`/users/switchStatusById/${id}`, null, {
    params: { status },
  })
}

export const editUserApi = (data: EditUserDTO) => {
  return request.put<Result<boolean>>('/users/edit', data)
}

export const deleteUserByIdApi = (id: number) => {
  return request.delete<Result<boolean>>(`/users/deleteUserById/${id}`)
}

export const uploadAvatarApi = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<Result<string>>('/users/upload/avatar', formData)
}

/**
 * 修改当前用户用户名
 */
export const updateSelfNameApi = (username: string) => {
  return request.put<Result<boolean>>('/users/self/name', {
    username,
  })
}

export function getUserById(userId: number) {
  return request.get<Result<UserVO>>(`/users/${userId}`)
}

export const getUserByIdsApi = (ids: number[]) => {
  return request.get('/users/batch', {
    params: {
      ids,
    },
  })
}
