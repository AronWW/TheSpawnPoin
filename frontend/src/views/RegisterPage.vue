<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const displayName = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const error = ref('')
const loading = ref(false)

async function handleRegister() {
  error.value = ''

  if (!displayName.value || !email.value || !password.value || !confirmPassword.value) {
    error.value = 'Заповніть всі поля'
    return
  }
  if (displayName.value.length < 2) {
    error.value = 'Ім\'я має бути мінімум 2 символи'
    return
  }
  if (password.value.length < 6) {
    error.value = 'Пароль має бути мінімум 6 символів'
    return
  }
  if (!/(?=.*[A-Za-z])(?=.*\d)/.test(password.value)) {
    error.value = 'Пароль має містити літери та цифри'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = 'Паролі не співпадають'
    return
  }

  loading.value = true
  try {
    await auth.register(displayName.value, email.value, password.value)
    router.push({ name: 'verify-email', query: { email: email.value } })
  } catch (e: any) {
    const msg = e.response?.data?.message || e.response?.data?.error
    error.value = msg || 'Помилка при реєстрації'
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
          <div class="auth-side-tagline">ПРИЄДНУЙСЯ ДО КОМАНДИ</div>
          <p class="auth-side-desc">
            Створи профіль гравця, знайди свою команду та починай грати
            з тими, хто розділяє твій стиль.
          </p>
          <div class="auth-side-art">🎮</div>
        </div>
      </div>

      <div class="auth-form-panel">
        <div class="auth-form-header">
          <h1 class="auth-form-title">РЕЄСТРАЦІЯ</h1>
          <p class="auth-form-sub">Створи свій акаунт воїна</p>
        </div>

        <form class="auth-form" @submit.prevent="handleRegister">
          <div v-if="error" class="auth-error">{{ error }}</div>

          <div class="form-group">
            <label class="form-label">Ім'я гравця</label>
            <input
              class="form-input"
              type="text"
              v-model="displayName"
              placeholder="ShadowKnight"
              autocomplete="username"
            />
          </div>

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
              placeholder="Мін. 6 символів, літери + цифри"
              autocomplete="new-password"
            />
          </div>

          <div class="form-group">
            <label class="form-label">Підтвердження пароля</label>
            <input
              class="form-input"
              type="password"
              v-model="confirmPassword"
              placeholder="Повторіть пароль"
              autocomplete="new-password"
            />
          </div>

          <button type="submit" class="btn-submit auth-submit" :disabled="loading">
            {{ loading ? 'РЕЄСТРАЦІЯ...' : '⚡ СТВОРИТИ АКАУНТ' }}
          </button>
        </form>

        <div class="auth-footer-text">
          Вже є акаунт?
          <router-link to="/login" class="auth-link accent">Увійти</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

