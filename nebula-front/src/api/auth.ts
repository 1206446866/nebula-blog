import request from '@/utils/request'
import type { Result } from '@/types/result.ts'
import type { UserVO } from '@/api/user.ts'

export interface LoginVO {
  token: string
  user: UserVO
  roles: string[]
  permissions: string[]
}

/**
 * 邮箱注册验证
 */
export interface EmailVerifyDTO {
  email: string
  code: string
  password: string
}

export const loginApi = (data: { account: string; password: string }) => {
  return request.post<Result<LoginVO>>('/auth/login', data)
}

/**
 * 发送邮箱验证码
 */
export const sendEmailCaptchaApi = (data: { email: string }) => {
  return request.post<Result<number>>('/auth/email/captcha', data)
}

/**
 * 邮箱注册验证
 */
export const verifyEmailCaptchaApi = (data: EmailVerifyDTO) => {
  return request.post<Result<string>>('/auth/email/verify', data)
}

export interface RegisterDTO {
  token: string
}

export const registerApi = (data: RegisterDTO) => {
  return request.post<Result<boolean>>('/auth/register', data)
}

export const changePasswordApi = (data: {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}) => {
  return request.post<Result<Boolean>>('/auth/change-password', data)
}
