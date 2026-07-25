import request from '@/utils/request.ts'
import type { Result } from '@/types/result.ts'
import type { Page } from '@/types/page.ts'

export interface RoleVO {
  id: number
  name: string
  description: string
  createTime: string
}

export interface RolePageDto {
  description?: string
  current: number
  size: number
}
export interface CreateRoleDto {
  name: string
  description: string
}
export interface UpdateRoleDto {
  name: string
  description: string
}

/**
 * 获取所有角色
 */
export const getAllRolesApi = () => {
  return request.get<Result<RoleVO[]>>('/roles')
}
/**
 * 查询用户角色
 */
export const getRolesByUserIdApi = (userId: number) => {
  return request.get<Result<RoleVO[]>>(`/roles/${userId}`)
}
/**
 * 分页查询
 */
export const getRolePageApi = (params: RolePageDto) => {
  return request.get<Result<Page<RoleVO>>>('/roles/page', { params })
}
export const createRole = (params: CreateRoleDto) => {
  return request.post<Result<Boolean>>(`/roles/create`, params)
}
export const updateRole = (params: UpdateRoleDto) => {
  return request.put<Result<Boolean>>(`/roles/update`, params)
}
export const removeRole = (roleId: number) => {
  return request.delete<Result<Boolean>>(`/roles/remove/${roleId}`)
}
