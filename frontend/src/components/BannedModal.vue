<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api/axios'
import type { UnbanRequest } from '../types'

defineProps<{
  banReason: string | null
}>()

const myRequest = ref<UnbanRequest | null>(null)
const requestReason = ref('')
const submitting = ref(false)
const errorMsg = ref('')
const loadingRequest = ref(true)

onMounted(async () => {
  try {
    const { data } = await api.get<UnbanRequest | null>('/unban-requests/my')
    myRequest.value = data
  } catch {

  } finally {
    loadingRequest.value = false
  }
})

async function submitRequest() {
  if (!requestReason.value.trim()) return
  submitting.value = true
  errorMsg.value = ''
  try {
    const { data } = await api.post<UnbanRequest>('/unban-requests', { reason: requestReason.value.trim() })
    myRequest.value = data
    requestReason.value = ''
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || 'Помилка при надсиланні запиту'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="banned-overlay">
    <div class="banned-modal">
      <h1 class="banned-title">ВАС ЗАБАНЕНО</h1>

      <div class="banned-image-wrapper">
        <img src="../assets/banned-placeholder.png" alt="Banned" class="banned-image" />
      </div>

      <div v-if="banReason" class="banned-reason">
        <span class="reason-label">Причина</span>
        <p class="reason-text">{{ banReason }}</p>
      </div>

      <div class="divider"></div>

      <div class="unban-section">
        <div v-if="loadingRequest" class="unban-loading">Завантаження...</div>

        <div v-else-if="myRequest && myRequest.status === 'PENDING'" class="status-block pending">
          <span class="status-label">⏳ Ваш запит розглядається</span>
          <p class="status-text">{{ myRequest.reason }}</p>
        </div>

        <template v-else-if="myRequest && myRequest.status === 'REJECTED'">
          <div class="status-block rejected">
            <span class="status-label">Запит відхилено</span>
            <p v-if="myRequest.adminComment" class="status-text">{{ myRequest.adminComment }}</p>
          </div>
          <div class="unban-form">
            <textarea
              v-model="requestReason"
              class="unban-textarea"
              rows="3"
              placeholder="Опишіть чому вас слід розбанити..."
              :disabled="submitting"
            ></textarea>
            <p v-if="errorMsg" class="unban-error">{{ errorMsg }}</p>
            <button class="unban-btn" :disabled="submitting || !requestReason.trim()" @click="submitRequest">
              {{ submitting ? 'Надсилається...' : 'Надіслати повторно' }}
            </button>
          </div>
        </template>

        <div v-else class="unban-form">
          <textarea
            v-model="requestReason"
            class="unban-textarea"
            rows="3"
            placeholder="Опишіть чому вас слід розбанити..."
            :disabled="submitting"
          ></textarea>
          <p v-if="errorMsg" class="unban-error">{{ errorMsg }}</p>
          <button class="unban-btn" :disabled="submitting || !requestReason.trim()" @click="submitRequest">
            {{ submitting ? 'Надсилається...' : 'Надіслати запит на розбан' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.banned-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.82);
  backdrop-filter: blur(3px);
}

.banned-modal {
  width: 90%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
  background: var(--panel);
  border: 2px solid var(--red);
  padding: 32px 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  box-shadow: 0 0 50px rgba(192, 57, 43, 0.2);
}

.banned-title {
  font-family: var(--font-display);
  font-size: 2.8rem;
  color: var(--red);
  letter-spacing: 6px;
  line-height: 1;
  text-align: center;
}

.banned-image-wrapper {
  width: 100%;
}

.banned-image {
  width: 100%;
  height: auto;
  object-fit: contain;
  display: block;
  border: 1px solid var(--border);
}

.banned-reason {
  width: 100%;
  text-align: center;
}

.reason-label {
  font-family: var(--font-display);
  font-size: 0.85rem;
  color: var(--gray);
  letter-spacing: 2px;
  text-transform: uppercase;
}

.reason-text {
  margin-top: 4px;
  font-size: 1rem;
  color: var(--white);
  line-height: 1.4;
}

.divider {
  width: 100%;
  height: 1px;
  background: var(--border);
}

.unban-section {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.unban-loading {
  text-align: center;
  color: var(--gray);
  font-size: 0.9rem;
}

.status-block {
  padding: 12px 14px;
  border-left: 3px solid var(--border);
  background: var(--panel-light);
}
.status-block.pending {
  border-left-color: var(--yellow);
}
.status-block.rejected {
  border-left-color: var(--red);
}

.status-label {
  font-family: var(--font-display);
  font-size: 0.95rem;
  letter-spacing: 1px;
}
.status-block.pending .status-label { color: var(--yellow); }
.status-block.rejected .status-label { color: var(--red); }

.status-text {
  margin-top: 4px;
  font-size: 0.88rem;
  color: var(--gray-light);
  line-height: 1.4;
}

.unban-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.unban-textarea {
  width: 100%;
  padding: 10px 12px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  color: var(--white);
  font-family: var(--font-body);
  font-size: 0.95rem;
  resize: vertical;
  outline: none;
  transition: border-color 0.15s;
}
.unban-textarea:focus {
  border-color: var(--yellow-dim);
}
.unban-textarea:disabled {
  opacity: 0.4;
}

.unban-error {
  color: var(--red);
  font-size: 0.85rem;
}

.unban-btn {
  align-self: flex-start;
  padding: 8px 18px;
  background: transparent;
  color: var(--yellow);
  border: 1px solid var(--yellow-dim);
  font-family: var(--font-body);
  font-size: 0.9rem;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.unban-btn:hover:not(:disabled) {
  background: var(--yellow);
  color: var(--black);
}
.unban-btn:disabled {
  opacity: 0.35;
  cursor: default;
}

@media (max-width: 480px) {
  .banned-modal {
    padding: 24px 18px;
  }
  .banned-title {
    font-size: 2.2rem;
    letter-spacing: 3px;
  }
}
</style>
