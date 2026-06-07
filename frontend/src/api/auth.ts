import request from '@/api/request'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
  email?: string
}

export interface AuthResponse {
  userId: number
  username: string
  token: string
}

export interface ResetPasswordParams {
  email: string
  newPassword: string
}

/** 登录 */
export function login(data: LoginParams) {
  return request.post<any, { code: number; data: AuthResponse }>('/api/auth/login', data)
}

/** 注册 */
export function register(data: RegisterParams) {
  return request.post<any, { code: number; data: AuthResponse }>('/api/auth/register', data)
}

/** 忘记密码 - 通过邮箱重置 */
export function resetPassword(data: ResetPasswordParams) {
  return request.post<any, { code: number; data: string }>('/api/auth/reset-password', data)
}
