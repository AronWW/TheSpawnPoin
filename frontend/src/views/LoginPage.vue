<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const email = ref('')
const password = ref('')
const rememberMe = ref(false)
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  if (!email.value || !password.value) {
    error.value = 'Заповніть всі поля'
    return
  }
  loading.value = true
  try {
    await auth.login(email.value, password.value, rememberMe.value)
    router.push('/')
  } catch (e: any) {
    const msg = e.response?.data?.message || e.response?.data?.error
    if (msg) {
      error.value = msg
    } else {
      error.value = 'Невірний email або пароль'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-bg"></div>
    <div class="auth-container">
      <div class="auth-side">
        <div class="auth-side-inner">
          <div class="auth-side-logo">THE<span>SPAWN</span>POINT</div>
          <div class="auth-side-tagline">ЗНАЙДИ СВІЙ ЗАГІН</div>
          <p class="auth-side-desc">
            Платформа для гравців, які хочуть більше ніж просто грати.
            Знаходь тімейтів, організовуй пати, грай на своєму рівні.
          </p>
          <div class="auth-side-art">⚔️</div>
        </div>
      </div>

      <div class="auth-form-panel">
        <div class="auth-form-header">
          <h1 class="auth-form-title">ВХІД</h1>
          <p class="auth-form-sub">Ласкаво просимо назад, воїне</p>
        </div>

        <form class="auth-form" @submit.prevent="handleLogin">
          <div v-if="error" class="auth-error">{{ error }}</div>

          <div class="form-group">
            <label class="form-label">Email</label>
            <input
              class="form-input"
              type="email"
              v-model="email"
              placeholder="gamer@example.com"
              autocomplete="email"
            />
          </div>

          <div class="form-group">
            <label class="form-label">Пароль</label>
            <input
              class="form-input"
              type="password"
              v-model="password"
              placeholder="••••••••"
              autocomplete="current-password"
            />
          </div>

          <div class="auth-row">
            <label class="auth-checkbox">
              <input type="checkbox" v-model="rememberMe" />
              <span>Запам'ятати мене</span>
            </label>
            <router-link to="/forgot-password" class="auth-link">Забули пароль?</router-link>
          </div>

          <button type="submit" class="btn-submit auth-submit" :disabled="loading">
            {{ loading ? 'ВХІД...' : '⚡ УВІЙТИ' }}
          </button>
        </form>

        <div class="auth-footer-text">
          Немає акаунту?
          <router-link to="/register" class="auth-link accent">Зареєструватись</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

