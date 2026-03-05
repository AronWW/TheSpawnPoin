<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const token = ref((route.query.token as string) || '')
const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)

async function handleReset() {
  error.value = ''
  success.value = ''

  if (!token.value) {
    error.value = 'Токен відсутній. Перейдіть за посиланням з email.'
    return
  }
  if (!newPassword.value || !confirmPassword.value) {
    error.value = 'Заповніть всі поля'
    return
  }
  if (newPassword.value.length < 6) {
    error.value = 'Пароль має бути мінімум 6 символів'
    return
  }
  if (!/(?=.*[A-Za-z])(?=.*\d)/.test(newPassword.value)) {
    error.value = 'Пароль має містити літери та цифри'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    error.value = 'Паролі не співпадають'
    return
  }

  loading.value = true
  try {
    await auth.resetPassword(token.value, newPassword.value)
    success.value = 'Пароль успішно змінено!'
    setTimeout(() => router.push('/login'), 2000)
  } catch (e: any) {
    const msg = e.response?.data?.message || e.response?.data?.error
    error.value = msg || 'Помилка при зміні пароля. Можливо, посилання застаріло.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-bg"></div>
    <div class="auth-container auth-container--narrow">
      <div class="auth-form-panel auth-form-panel--full">
        <div class="auth-form-header">
          <div class="auth-form-icon">🔐</div>
          <h1 class="auth-form-title">НОВИЙ ПАРОЛЬ</h1>
          <p class="auth-form-sub">Введіть новий пароль для вашого акаунту</p>
        </div>

        <form class="auth-form" @submit.prevent="handleReset">
          <div v-if="error" class="auth-error">{{ error }}</div>
          <div v-if="success" class="auth-success">{{ success }}</div>

          <div class="form-group">
            <label class="form-label">Новий пароль</label>
            <input
              class="form-input"
              type="password"
              v-model="newPassword"
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

          <button type="submit" class="btn-submit auth-submit" :disabled="loading || !!success">
            {{ loading ? 'ЗБЕРЕЖЕННЯ...' : '⚡ ЗБЕРЕГТИ ПАРОЛЬ' }}
          </button>
        </form>

        <div class="auth-footer-text">
          <router-link to="/login" class="auth-link">← Повернутись до входу</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

