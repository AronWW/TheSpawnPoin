<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const email = ref((route.query.email as string) || '')
const code = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)
const resending = ref(false)

async function handleVerify() {
  error.value = ''
  success.value = ''

  if (!email.value || !code.value) {
    error.value = 'Введіть email та код підтвердження'
    return
  }
  if (!/^\d{6}$/.test(code.value)) {
    error.value = 'Код має бути 6 цифр'
    return
  }

  loading.value = true
  try {
    await auth.verifyEmail(email.value, code.value)
    router.push('/')
  } catch (e: any) {
    const msg = e.response?.data?.message || e.response?.data?.error
    error.value = msg || 'Невірний код підтвердження'
  } finally {
    loading.value = false
  }
}

async function handleResend() {
  if (!email.value) {
    error.value = 'Введіть email'
    return
  }
  error.value = ''
  success.value = ''
  resending.value = true
  try {
    await auth.resendVerification(email.value)
    success.value = 'Код надіслано повторно. Перевірте пошту.'
  } catch (e: any) {
    const msg = e.response?.data?.message || e.response?.data?.error
    error.value = msg || 'Помилка при повторному надсиланні'
  } finally {
    resending.value = false
  }
}

onMounted(() => {
  if (!email.value) {
  }
})
</script>

<template>
  <div class="auth-page">
    <div class="auth-bg"></div>
    <div class="auth-container auth-container--narrow">
      <div class="auth-form-panel auth-form-panel--full">
        <div class="auth-form-header">
          <div class="auth-form-icon">📧</div>
          <h1 class="auth-form-title">ПІДТВЕРДЖЕННЯ EMAIL</h1>
          <p class="auth-form-sub">
            Ми надіслали 6-значний код на <strong>{{ email || 'вашу пошту' }}</strong>
          </p>
        </div>

        <form class="auth-form" @submit.prevent="handleVerify">
          <div v-if="error" class="auth-error">{{ error }}</div>
          <div v-if="success" class="auth-success">{{ success }}</div>

          <div v-if="!email" class="form-group">
            <label class="form-label">Email</label>
            <input
              class="form-input"
              type="email"
              v-model="email"
              placeholder="gamer@example.com"
            />
          </div>

          <div class="form-group">
            <label class="form-label">Код підтвердження</label>
            <input
              class="form-input auth-code-input"
              type="text"
              v-model="code"
              placeholder="000000"
              maxlength="6"
              inputmode="numeric"
              autocomplete="one-time-code"
            />
          </div>

          <button type="submit" class="btn-submit auth-submit" :disabled="loading">
            {{ loading ? 'ПЕРЕВІРКА...' : '⚡ ПІДТВЕРДИТИ' }}
          </button>
        </form>

        <div class="auth-footer-text">
          Не отримали код?
          <button class="auth-link accent auth-link-btn" @click="handleResend" :disabled="resending">
            {{ resending ? 'Надсилання...' : 'Надіслати повторно' }}
          </button>
        </div>

        <div class="auth-footer-text" style="margin-top: 8px;">
          <router-link to="/login" class="auth-link">← Повернутись до входу</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

