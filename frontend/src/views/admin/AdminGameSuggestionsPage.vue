<script setup lang="ts">

import { ref, reactive, onMounted, watch } from 'vue'
import { useAdminStore } from '../../stores/admin'
import type { GameSuggestion } from '../../types'

const admin = useAdminStore()
const page = ref(0)
const statusFilter = ref('PENDING')


const selectedSuggestion = ref<GameSuggestion | null>(null)
const editForm = reactive({
  name: '',
  genre: '',
  releaseYear: null as number | null,
  imageUrl: '',
  maxPartySize: 5,
})

const showRejectForm = ref(false)
const rejectComment = ref('')

function load() {
  admin.fetchSuggestions(page.value, 20, statusFilter.value || undefined)
}

onMounted(load)
watch([page, statusFilter], load)

function openSuggestion(s: GameSuggestion) {
  selectedSuggestion.value = s
  editForm.name = s.name
  editForm.genre = s.genre || ''
  editForm.releaseYear = s.releaseYear
  editForm.imageUrl = s.imageUrl || ''
  editForm.maxPartySize = s.maxPartySize
  showRejectForm.value = false
  rejectComment.value = ''
}

function closeModal() {
  selectedSuggestion.value = null
  showRejectForm.value = false
}

async function confirmApprove() {
  if (!selectedSuggestion.value) return
  await admin.approveSuggestion(selectedSuggestion.value.id, {
    name: editForm.name,
    genre: editForm.genre || undefined,
    releaseYear: editForm.releaseYear || undefined,
    imageUrl: editForm.imageUrl || undefined,
    maxPartySize: editForm.maxPartySize,
  })
  closeModal()
  load()
}

function openRejectForm() {
  showRejectForm.value = true
}

async function confirmReject() {
  if (!selectedSuggestion.value) return
  await admin.rejectSuggestion(selectedSuggestion.value.id, rejectComment.value || undefined)
  closeModal()
  load()
}

function getPageData() {
  if (!admin.suggestions) return { content: [], totalPages: 0 }
  return {
    content: admin.suggestions.content,
    totalPages: admin.suggestions.page?.totalPages ?? admin.suggestions.totalPages ?? 0,
  }
}
</script>

<template>
  <div class="admin-suggestions">
    <h1 class="admin-page-title">ЗАЯВКИ НА ІГРИ</h1>

    <div class="filter-row">
      <select v-model="statusFilter" class="admin-select" @change="page = 0">
        <option value="PENDING">Очікують</option>
        <option value="APPROVED">Схвалені</option>
        <option value="REJECTED">Відхилені</option>
        <option value="">Усі</option>
      </select>
    </div>

    <table class="admin-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Назва</th>
          <th>Жанр</th>
          <th>Рік</th>
          <th>Max party</th>
          <th>Автор</th>
          <th>Статус</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="s in getPageData().content"
          :key="s.id"
          class="suggestion-row"
          :class="{ clickable: s.status === 'PENDING' }"
          @click="openSuggestion(s)"
        >
          <td>{{ s.id }}</td>
          <td>{{ s.name }}</td>
          <td>{{ s.genre || '—' }}</td>
          <td>{{ s.releaseYear || '—' }}</td>
          <td>{{ s.maxPartySize }}</td>
          <td>{{ s.suggestedByDisplayName }}</td>
          <td>
            <span class="status-badge" :class="s.status.toLowerCase()">{{ s.status }}</span>
          </td>
        </tr>
        <tr v-if="!getPageData().content.length">
          <td colspan="7" class="empty-cell">Немає заявок</td>
        </tr>
      </tbody>
    </table>

    <div v-if="getPageData().totalPages > 1" class="pagination">
      <button :disabled="page === 0" @click="page--">←</button>
      <span>{{ page + 1 }} / {{ getPageData().totalPages }}</span>
      <button :disabled="page >= getPageData().totalPages - 1" @click="page++">→</button>
    </div>

    <div v-if="selectedSuggestion" class="modal-overlay" @click.self="closeModal">
      <div class="modal-box">
        <h3>Заявка #{{ selectedSuggestion.id }}</h3>
        <p class="modal-meta">
          Автор: <strong>{{ selectedSuggestion.suggestedByDisplayName }}</strong>
        </p>

        <template v-if="selectedSuggestion.status === 'PENDING' && !showRejectForm">
          <div class="form-group">
            <label class="modal-label">Назва гри</label>
            <input v-model="editForm.name" class="admin-input" type="text" />
          </div>
          <div class="form-group">
            <label class="modal-label">Жанр</label>
            <input v-model="editForm.genre" class="admin-input" type="text" />
          </div>
          <div class="form-row">
            <div class="form-group half">
              <label class="modal-label">Рік випуску</label>
              <input v-model.number="editForm.releaseYear" class="admin-input" type="number" />
            </div>
            <div class="form-group half">
              <label class="modal-label">Max party size</label>
              <input v-model.number="editForm.maxPartySize" class="admin-input" type="number" min="1" />
            </div>
          </div>
          <div class="form-group">
            <label class="modal-label">URL зображення</label>
            <input v-model="editForm.imageUrl" class="admin-input" type="text" placeholder="https://..." />
          </div>
          <div class="modal-actions">
            <button class="admin-btn success" @click="confirmApprove">Схвалити</button>
            <button class="admin-btn danger" @click="openRejectForm">Відхилити</button>
            <button class="admin-btn outline" @click="closeModal">Скасувати</button>
          </div>
        </template>

        <template v-else-if="selectedSuggestion.status === 'PENDING' && showRejectForm">
          <p class="reject-title">Відхилити заявку "{{ selectedSuggestion.name }}"</p>
          <div class="form-group">
            <label class="modal-label">Повідомлення для користувача</label>
            <textarea
              v-model="rejectComment"
              class="admin-input admin-textarea"
              rows="4"
              placeholder="Напишіть причину відхилення — користувач отримає це повідомлення..."
            ></textarea>
          </div>
          <div class="modal-actions">
            <button class="admin-btn danger" @click="confirmReject">Підтвердити відхилення</button>
            <button class="admin-btn outline" @click="showRejectForm = false">Назад</button>
          </div>
        </template>

        <template v-else>
          <div class="info-row"><span class="info-label">Назва:</span> {{ selectedSuggestion.name }}</div>
          <div class="info-row"><span class="info-label">Жанр:</span> {{ selectedSuggestion.genre || '—' }}</div>
          <div class="info-row"><span class="info-label">Рік:</span> {{ selectedSuggestion.releaseYear || '—' }}</div>
          <div class="info-row"><span class="info-label">Max party:</span> {{ selectedSuggestion.maxPartySize }}</div>
          <div v-if="selectedSuggestion.adminComment" class="info-row">
            <span class="info-label">Коментар адміна:</span> {{ selectedSuggestion.adminComment }}
          </div>
          <div class="modal-actions">
            <button class="admin-btn outline" @click="closeModal">Закрити</button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-suggestions { padding: 0; }
.admin-page-title { font-family: var(--font-display); font-size: 2rem; color: var(--yellow); margin-bottom: 20px; }
.filter-row { margin-bottom: 16px; }

.suggestion-row { transition: background 0.15s; }
.suggestion-row.clickable { cursor: pointer; }
.suggestion-row.clickable:hover { background: rgba(255,255,255,0.03); }

.status-badge { font-size: 0.8rem; padding: 2px 10px; border: 1px solid var(--border); text-transform: uppercase; }
.status-badge.pending { color: var(--yellow); border-color: var(--yellow-dim); }
.status-badge.approved { color: #4caf50; border-color: #388e3c; }
.status-badge.rejected { color: var(--red); border-color: var(--red-dim); }

.pagination { display: flex; align-items: center; gap: 12px; justify-content: center; margin-top: 16px; }
.pagination button { padding: 6px 14px; background: var(--panel); border: 1px solid var(--border); color: var(--gray-light); cursor: pointer; font-family: var(--font-body); }
.pagination button:disabled { opacity: 0.4; cursor: default; }
.pagination span { color: var(--gray); font-size: 0.9rem; }
.empty-cell { text-align: center; color: var(--gray); padding: 30px !important; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-box { background: var(--panel); border: 1px solid var(--border); padding: 28px; min-width: 420px; max-width: 520px; width: 100%; }
.modal-box h3 { font-family: var(--font-display); font-size: 1.5rem; color: var(--white); margin-bottom: 6px; }
.modal-meta { color: var(--gray); font-size: 0.85rem; margin-bottom: 18px; }
.modal-meta strong { color: var(--gray-light); }

.form-group { margin-bottom: 14px; }
.form-row { display: flex; gap: 12px; }
.form-group.half { flex: 1; }
.modal-label { display: block; color: var(--gray); font-size: 0.85rem; margin-bottom: 6px; }

.admin-input {
  width: 100%; padding: 8px 12px; background: var(--black); border: 1px solid var(--border);
  color: var(--white); font-family: var(--font-body); font-size: 0.9rem; transition: border-color 0.15s;
  box-sizing: border-box;
}
.admin-input:focus { outline: none; border-color: var(--yellow-dim); }
.admin-textarea { resize: vertical; min-height: 60px; }

.modal-actions { display: flex; gap: 10px; margin-top: 20px; }

.admin-btn {
  padding: 8px 18px; font-size: 0.9rem; border: 1px solid var(--border);
  background: transparent; cursor: pointer; font-family: var(--font-body); transition: all 0.15s;
}
.admin-btn.success { color: #4caf50; border-color: #388e3c; }
.admin-btn.success:hover { background: #388e3c; color: var(--white); }
.admin-btn.danger { color: var(--red); border-color: var(--red-dim); }
.admin-btn.danger:hover { background: var(--red-dim); color: var(--white); }
.admin-btn.outline { color: var(--gray); border-color: var(--border); }
.admin-btn.outline:hover { color: var(--white); border-color: var(--gray); }

.reject-title { color: var(--red); font-size: 1rem; margin-bottom: 14px; font-weight: 600; }

.info-row { color: var(--gray-light); font-size: 0.9rem; margin-bottom: 8px; }
.info-label { color: var(--gray); margin-right: 6px; }
</style>

