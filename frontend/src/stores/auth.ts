import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api/axios'
import type { UserMe } from '../types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserMe | null>(null)
  const loading = ref(false)
  const initialized = ref(false)
  let _initPromise: Promise<void> | null = null

  const isLoggedIn = computed(() => user.value !== null)
  const displayName = computed(() => user.value?.displayName ?? '')

  async function fetchMe() {
    loading.value = true
    try {
      const { data } = await api.get<UserMe>('/auth/me')
      user.value = data
    } catch {
      user.value = null
    } finally {
      loading.value = false
      initialized.value = true
    }
  }

  function init() {
    if (!_initPromise) {
      _initPromise = fetchMe()
    }
    return _initPromise
  }

  async function login(email: string, password: string, rememberMe: boolean = false) {
    const { data } = await api.post('/auth/login', { email, password, rememberMe })
    user.value = data
    return data
  }

  async function register(displayName: string, email: string, password: string) {
    const { data } = await api.post('/auth/register', { displayName, email, password })
    return data
  }

  async function verifyEmail(email: string, code: string) {
    const { data } = await api.post('/auth/verify-email', { email, code })
    user.value = data
    return data
  }

  async function resendVerification(email: string) {
    const { data } = await api.post('/auth/resend-verification', { email })
    return data
  }

  async function forgotPassword(email: string) {
    const { data } = await api.post('/auth/forgot-password', { email })
    return data
  }

  async function resetPassword(token: string, newPassword: string) {
    const { data } = await api.post('/auth/reset-password', { token, newPassword })
    return data
  }

  async function logout() {
    try {
      await api.post('/auth/logout')
    } finally {
      user.value = null
    }
  }

  return {
    user, loading, isLoggedIn, displayName, initialized,
    fetchMe, init, login, register, verifyEmail, resendVerification,
    forgotPassword, resetPassword, logout,
  }
})

