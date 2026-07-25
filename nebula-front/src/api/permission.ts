import request from '@/utils/request'
import type { Result } from '@/types/result.ts'
import type { Page } from '@/types/page.ts'

export interface PermissionVO {
  id: number
  name: string
  description: string
  createTime: string
  updateTime: string
}

export interface PermissionGroupMap {
  [module: string]: PermissionVO[]
}

export interface PermissionPageDto {
  name: string
  current: number
  size: number
}

export interface CreatePermissionDto {
  name: string
  description: string
}
export interface UpdatePermissionDto {
  name: string
  description: string
}

export const getPermissionsApi = () => {
  return request.get<Result<PermissionGroupMap>>('/permissions')
}

/** 角色已有权限 */
export const getRolePermissionsApi = (roleId: number) => {
  return request.get<Result<PermissionGroupMap>>(`/permissions/${roleId}`)
}

/** 保存角色权限 */
export const saveRolePermissionsApi = (data: { roleId?: number; permissionIds: number[] }) => {
  return request.post<Result<Boolean>>('/permissions/save', data)
}

export const getPermissionPage = (params: PermissionPageDto) => {
  return request.get<Result<Page<PermissionVO>>>('/permissions/page', { params })
}

export const createPermissionApi = (params: CreatePermissionDto) => {
  return request.post<boolean>('/permissions/create', params)
}

export const updatePermissionApi = (params: UpdatePermissionDto) => {
  return request.put<boolean>('/permissions/update', params)
}

export const removePermissionApi = (id: number) => {
  return request.delete<boolean>(`/permissions/remove/${id}`)
}
