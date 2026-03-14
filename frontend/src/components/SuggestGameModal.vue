<script setup lang="ts">
import { ref } from 'vue'
import api from '../api/axios'
import { useGameStore } from '../stores/games'

const emit = defineEmits<{ close: []; submitted: [] }>()
const gameStore = useGameStore()
const name = ref('')
const genre = ref('')
const releaseYear = ref<number | null>(null)
const imageUrl = ref('')
const maxPartySize = ref(5)

const loading = ref(false)
const success = ref(false)
const error = ref('')

async function submit() {
  if (!name.value.trim()) return
  loading.value = true
  error.value = ''
  try {
    await api.post('/game-suggestions', {
      name: name.value.trim(),
      genre: genre.value.trim() || null,
      releaseYear: releaseYear.value || null,
      imageUrl: imageUrl.value.trim() || null,
      maxPartySize: maxPartySize.value,
    })
    success.value = true
    emit('submitted')
    gameStore.fetchMySuggestionsCount()
    setTimeout(() => emit('close'), 1500)
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Помилка надсилання заявки'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-box">
      <h3>ЗАПРОПОНУВАТИ ГРУ</h3>

      <div v-if="success" class="success-msg">
        ✓ Заявку надіслано! Адмін розгляне її найближчим часом.
      </div>

      <template v-else>
        <p class="modal-hint">
          Не знайшли свою гру? Запропонуйте її — і після перевірки адміністратором вона з'явиться в каталозі.
        </p>

        <label class="modal-label">Назва гри *</label>
        <input v-model="name" class="modal-input" placeholder="Наприклад: Palworld" maxlength="100" />

        <label class="modal-label">Жанр</label>
        <input v-model="genre" class="modal-input" placeholder="Shooter, RPG, MOBA..." maxlength="50" />

        <div class="row-2">
          <div class="field">
            <label class="modal-label">Рік випуску</label>
            <input v-model.number="releaseYear" class="modal-input" type="number" placeholder="2024" min="1970" max="2030" />
          </div>
          <div class="field">
            <label class="modal-label">Макс. гравців у пати</label>
            <input v-model.number="maxPartySize" class="modal-input" type="number" min="2" max="100" />
          </div>
        </div>

        <label class="modal-label">URL зображення (обкладинка)</label>
        <input v-model="imageUrl" class="modal-input" placeholder="https://..." maxlength="500" />

        <div v-if="imageUrl && imageUrl.startsWith('http')" class="preview-wrap">
          <img :src="imageUrl" class="preview-img" alt="Preview" @error="($event.target as HTMLImageElement).style.display='none'" />
        </div>

        <div v-if="error" class="error-msg">{{ error }}</div>

        <div class="modal-actions">
          <button class="modal-btn primary" @click="submit" :disabled="!name.trim() || loading">
            {{ loading ? 'Надсилання...' : 'Надіслати заявку' }}
          </button>
          <button class="modal-btn" @click="emit('close')">Скасувати</button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000;
}
.modal-box {
  background: var(--panel);
  border: 2px solid var(--border);
  padding: 28px 32px;
  width: 100%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-box h3 {
  font-family: var(--font-display), sans-serif;
  font-size: 1.5rem;
  letter-spacing: 2px;
  color: var(--yellow);
  margin-bottom: 8px;
}
.modal-hint {
  color: var(--gray);
  font-size: 0.9rem;
  margin-bottom: 16px;
  line-height: 1.4;
}
.modal-label {
  display: block;
  color: var(--gray);
  font-size: 0.85rem;
  margin: 14px 0 5px;
  letter-spacing: 0.5px;
}
.modal-input {
  width: 100%;
  padding: 10px 14px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  color: var(--white);
  font-family: var(--font-body), sans-serif;
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.15s;
}
.modal-input:focus { border-color: var(--yellow-dim); }

.row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: flex; flex-direction: column; }

.preview-wrap { margin-top: 10px; }
.preview-img {
  max-width: 100%;
  max-height: 160px;
  border: 1px solid var(--border);
  object-fit: contain;
}

.error-msg { color: var(--red); font-size: 0.85rem; margin-top: 10px; }

.modal-actions { display: flex; gap: 10px; margin-top: 20px; }
.modal-btn {
  padding: 10px 22px;
  border: 1px solid var(--border);
  background: var(--panel);
  color: var(--gray-light);
  cursor: pointer;
  font-family: var(--font-body), sans-serif;
  font-size: 0.95rem;
  letter-spacing: 0.5px;
  transition: all 0.15s;
}
.modal-btn:hover { border-color: var(--yellow-dim); color: var(--white); }
.modal-btn.primary {
  background: var(--yellow-dim);
  color: var(--black);
  border-color: var(--yellow);
  font-weight: 600;
}
.modal-btn.primary:hover { background: var(--yellow); }
.modal-btn:disabled { opacity: 0.5; cursor: default; }

.success-msg {
  color: #4caf50;
  font-size: 1rem;
  padding: 30px 0;
  text-align: center;
}
</style>

