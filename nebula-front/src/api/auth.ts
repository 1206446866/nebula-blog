import request from '@/utils/request'
import type { Result } from '@/types/result.ts'
import type { UserVO } from '@/api/user.ts'

export interface LoginVO {
  token: string
  user: UserVO
  roles: string[]
  permissions: string[]
}

export const loginApi = (data: { nid: string; password: string }) => {
  return request.post<Result<LoginVO>>('/auth/login', data)
}

export const changePasswordApi = (data: {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}) => {
  return request.post('/auth/change-password', data)
}
