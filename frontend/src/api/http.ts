import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from './types'

export const TOKEN_KEY = 'agentdesk_token'
export const USER_KEY = 'agentdesk_user'

/** 清除本地登录态（401 或主动退出时使用） */
export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

/** 跳转登录页；已经在登录页时不重复跳转，避免刷新循环 */
export function redirectToLogin(): void {
  if (!window.location.pathname.startsWith('/login')) {
    window.location.replace('/login')
  }
}

const http = axios.create({
  baseURL: '/api',
  timeout: 60_000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown> | null
    if (body && typeof body === 'object' && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status
    const message =
      error.response?.data?.message ||
      (status ? `请求失败（HTTP ${status}）` : '网络异常，请确认后端服务已启动')

    if (status === 401) {
      clearSession()
      redirectToLogin()
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  },
)

/**
 * 统一请求入口：返回契约中的 data 字段。
 * 业务错误已在拦截器中提示，调用方只需处理 loading / 状态回滚。
 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config)
  return response.data.data
}

export default http
