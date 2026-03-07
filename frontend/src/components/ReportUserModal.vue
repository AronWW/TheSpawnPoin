<script setup lang="ts">
import { ref } from 'vue'
import api from '../api/axios'

const props = defineProps<{ reportedUserId: number; reportedUserName: string }>()
const emit = defineEmits<{ close: [] }>()

const REASONS = [
  { value: 'TOXIC_BEHAVIOR', label: 'Токсична поведінка' },
  { value: 'CHEATING', label: 'Читерство' },
  { value: 'SPAM', label: 'Спам' },
  { value: 'HARASSMENT', label: 'Переслідування' },
  { value: 'INAPPROPRIATE_CONTENT', label: 'Неприйнятний контент' },
  { value: 'OTHER', label: 'Інше' },
]

const reason = ref('')
const description = ref('')
const loading = ref(false)
const success = ref(false)
const error = ref('')

async function submit() {
  if (!reason.value) return
  loading.value = true
  error.value = ''
  try {
    await api.post('/reports', {
      reportedUserId: props.reportedUserId,
      reason: reason.value,
      description: description.value || null,
    })
    success.value = true
    setTimeout(() => emit('close'), 1500)
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Помилка надсилання скарги'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-box">
      <h3>Скарга на {{ reportedUserName }}</h3>

      <div v-if="success" class="success-msg">✓ Скарга надіслана. Дякуємо!</div>

      <template v-else>
        <label class="modal-label">Причина *</label>
        <div class="reasons-grid">
          <button
            v-for="r in REASONS" :key="r.value"
            class="reason-chip"
            :class="{ selected: reason === r.value }"
            @click="reason = r.value"
          >{{ r.label }}</button>
        </div>

        <label class="modal-label">Опис (необов'язково)</label>
        <textarea v-model="description" class="modal-input modal-textarea" rows="3" placeholder="Деталі..."></textarea>

        <div v-if="error" class="error-msg">{{ error }}</div>

        <div class="modal-actions">
          <button class="modal-btn danger" @click="submit" :disabled="!reason || loading">
            {{ loading ? 'Надсилання...' : 'Надіслати скаргу' }}
          </button>
          <button class="modal-btn" @click="emit('close')">Скасувати</button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-box { background: var(--panel); border: 1px solid var(--border); padding: 24px; min-width: 400px; max-width: 480px; }
.modal-box h3 { font-family: var(--font-display); font-size: 1.3rem; color: var(--white); margin-bottom: 16px; }
.modal-label { display: block; color: var(--gray); font-size: 0.85rem; margin: 14px 0 6px; }

.reasons-grid { display: flex; flex-wrap: wrap; gap: 6px; }
.reason-chip {
  padding: 6px 14px; border: 1px solid var(--border); background: transparent;
  color: var(--gray-light); cursor: pointer; font-family: var(--font-body); font-size: 0.85rem; transition: all 0.15s;
}
.reason-chip:hover { border-color: var(--yellow-dim); }
.reason-chip.selected { border-color: var(--yellow); color: var(--yellow); background: var(--yellow-glow); }

.modal-input {
  width: 100%; padding: 8px 12px;
  background: var(--panel-light); border: 1px solid var(--border);
  color: var(--white); font-family: var(--font-body); font-size: 0.95rem; outline: none;
}
.modal-input:focus { border-color: var(--yellow-dim); }
.modal-textarea { resize: vertical; }

.modal-actions { display: flex; gap: 10px; margin-top: 16px; }
.modal-btn {
  padding: 8px 18px; border: 1px solid var(--border); background: var(--panel); color: var(--gray-light);
  cursor: pointer; font-family: var(--font-body); font-size: 0.9rem; transition: all 0.15s;
}
.modal-btn:hover { border-color: var(--yellow-dim); color: var(--white); }
.modal-btn.danger { background: var(--red-dim); color: var(--white); border-color: var(--red); }
.modal-btn.danger:hover { background: var(--red); }
.modal-btn:disabled { opacity: 0.5; cursor: default; }

.success-msg { color: #4caf50; font-size: 1rem; padding: 20px 0; text-align: center; }
.error-msg { color: var(--red); font-size: 0.85rem; margin-top: 8px; }
</style>

