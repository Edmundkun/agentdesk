import { request } from './http'
import type { AuthResult, LoginPayload, RegisterPayload, User } from './types'

/** POST /auth/login */
export function login(payload: LoginPayload): Promise<AuthResult> {
  return request<AuthResult>({
    url: '/auth/login',
    method: 'post',
    data: payload,
  })
}

/** POST /auth/register */
export function register(payload: RegisterPayload): Promise<AuthResult> {
  return request<AuthResult>({
    url: '/auth/register',
    method: 'post',
    data: payload,
  })
}

/** GET /auth/me */
export function fetchMe(): Promise<User> {
  return request<User>({ url: '/auth/me', method: 'get' })
}
