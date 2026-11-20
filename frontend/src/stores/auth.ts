import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '../api/auth'
import { TOKEN_KEY, USER_KEY } from '../api/http'
import type { LoginPayload, RegisterPayload, User } from '../api/types'

function readStoredUser(): User | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    const parsed: unknown = JSON.parse(raw)
    if (parsed && typeof parsed === 'object') return parsed as User
  } catch {
    /* 本地缓存损坏时忽略，重新登录即可恢复 */
  }
  return null
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  const user = ref<User | null>(readStoredUser())
  const loading = ref(false)

  const isLoggedIn = computed<boolean>(() => Boolean(token.value))
  const displayName = computed<string>(
    () => user.value?.nickname || user.value?.username || '访客',
  )
  const avatarText = computed<string>(() => displayName.value.slice(0, 1).toUpperCase())

  function persist(nextToken: string, nextUser: User): void {
    token.value = nextToken
    user.value = nextUser
    localStorage.setItem(TOKEN_KEY, nextToken)
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
  }

  async function login(payload: LoginPayload): Promise<void> {
    loading.value = true
    try {
      const result = await authApi.login(payload)
      persist(result.token, result.user)
    } finally {
      loading.value = false
    }
  }

  async function register(payload: RegisterPayload): Promise<void> {
    loading.value = true
    try {
      const result = await authApi.register(payload)
      persist(result.token, result.user)
    } finally {
      loading.value = false
    }
  }

  /** 用 /auth/me 校正本地缓存的用户信息（失败时保持本地数据） */
  async function refreshMe(): Promise<void> {
    if (!token.value) return
    try {
      const me = await authApi.fetchMe()
      user.value = me
      localStorage.setItem(USER_KEY, JSON.stringify(me))
    } catch {
      /* 拦截器已提示，这里保持静默 */
    }
  }

  function logout(): void {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    user,
    loading,
    isLoggedIn,
    displayName,
    avatarText,
    login,
    register,
    refreshMe,
    logout,
  }
})
