import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, publicApi } from '../api/axios'
import type { UserMe, Profile } from '../types'

type MeGuestResponse = {
  authenticated: false
  user: null
}

type MeAuthResponse = UserMe & {
  authenticated: true
}

type MeResponse = MeGuestResponse | MeAuthResponse

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserMe | null>(null)
  const loading = ref(false)
  const initialized = ref(false)
  const onlineCount = ref(0)
  const myProfile = ref<Profile | null>(null)
  let initPromise: Promise<void> | null = null

  const isLoggedIn = computed(() => user.value !== null)
  const displayName = computed(() => user.value?.displayName ?? '')

  function clearAuthState() {
    user.value = null
    myProfile.value = null
  }

  function clearRefreshHintCookie() {
    if (typeof document === 'undefined') return
    document.cookie = 'has_refresh_token=; Max-Age=0; Path=/'
  }

  async function requestMe(): Promise<MeResponse> {
    const { data } = await authApi.get<MeResponse>('/auth/me')
    return data
  }

  function applyMeResponse(data: MeResponse): UserMe | null {
    if (!data.authenticated) {
      return null
    }

    const { authenticated: _authenticated, ...userData } = data
    user.value = userData as UserMe
    return user.value
  }

  async function fetchMe() {
    loading.value = true

    try {
      const meData = await requestMe()
      const currentUser = applyMeResponse(meData)

      if (currentUser) {
        return currentUser
      }

      try {
        await authApi.post('/auth/refresh')
        const refreshedMeData = await requestMe()
        const restoredUser = applyMeResponse(refreshedMeData)

        if (restoredUser) {
          return restoredUser
        }
      } catch {
        clearRefreshHintCookie()

      }

      clearAuthState()
      return null
    } catch (error: unknown) {
      clearAuthState()
      return null
    } finally {
      loading.value = false
      initialized.value = true
    }
  }

  async function refreshUser() {
    await fetchMe()
  }

  function markBanned(reason?: string | null) {
    if (!user.value) return

    user.value = {
      ...user.value,
      banned: true,
      banReason: reason ?? user.value.banReason ?? null,
    }
  }

  function init() {
    if (!initPromise) {
      initPromise = fetchMe().then(() => {})
    }
    return initPromise
  }

  async function login(email: string, password: string, rememberMe: boolean = false) {
    const { data } = await authApi.post<UserMe>('/auth/login', { email, password, rememberMe })
    user.value = data
    initialized.value = true
    return data
  }

  async function register(displayName: string, email: string, password: string) {
    const { data } = await publicApi.post('/auth/register', { displayName, email, password })
    return data
  }

  async function verifyEmail(email: string, code: string) {
    const { data } = await publicApi.post<UserMe>('/auth/verify-email', { email, code })
    user.value = data
    initialized.value = true
    return data
  }

  async function resendVerification(email: string) {
    const { data } = await publicApi.post('/auth/resend-verification', { email })
    return data
  }

  async function forgotPassword(email: string) {
    const { data } = await publicApi.post('/auth/forgot-password', { email })
    return data
  }

  async function resetPassword(token: string, newPassword: string) {
    const { data } = await publicApi.post('/auth/reset-password', { token, newPassword })
    return data
  }

  async function changePassword(currentPassword: string, newPassword: string) {
    const { data } = await authApi.put('/auth/change-password', { currentPassword, newPassword })
    return data
  }

  async function logout() {
    try {
      await authApi.post('/auth/logout')
    } finally {
      user.value = null
      myProfile.value = null
      initialized.value = true
    }
  }

  async function fetchOnlineCount() {
    try {
      const { data } = await publicApi.get<{ count: number }>('/users/online-count')
      onlineCount.value = data.count
    } catch {

    }
  }

  async function fetchMyProfile() {
    if (myProfile.value) return myProfile.value

    try {
      const { data } = await authApi.get<Profile>('/profile/me')
      myProfile.value = data
      return data
    } catch {
      return null
    }
  }

  return {
    user,
    loading,
    isLoggedIn,
    displayName,
    initialized,
    onlineCount,
    myProfile,
    fetchMe,
    init,
    login,
    register,
    verifyEmail,
    resendVerification,
    forgotPassword,
    resetPassword,
    changePassword,
    logout,
    refreshUser,
    markBanned,
    fetchOnlineCount,
    fetchMyProfile,
  }
})
