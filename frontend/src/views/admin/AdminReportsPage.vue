<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useAdminStore } from '../../stores/admin'

const REASON_LABELS: Record<string, string> = {
  TOXIC_BEHAVIOR: 'Токсична поведінка',
  CHEATING: 'Читерство',
  SPAM: 'Спам',
  HARASSMENT: 'Переслідування',
  INAPPROPRIATE_CONTENT: 'Неприйнятний контент',
  OTHER: 'Інше',
}

const admin = useAdminStore()
const page = ref(0)
const statusFilter = ref('OPEN')

const reviewModal = ref<any>(null)
const reviewStatus = ref('REVIEWED')
const reviewComment = ref('')

function load() {
  admin.fetchReports(page.value, 20, statusFilter.value || undefined)
}

onMounted(load)
watch([page, statusFilter], load)

function openReview(r: any) {
  reviewModal.value = r
  reviewStatus.value = 'REVIEWED'
  reviewComment.value = ''
}
function closeReview() { reviewModal.value = null }
async function confirmReview() {
  if (!reviewModal.value) return
  await admin.reviewReport(reviewModal.value.id, reviewStatus.value, reviewComment.value || undefined)
  closeReview()
  load()
}

function getPageData() {
  if (!admin.reports) return { content: [], totalPages: 0 }
  return {
    content: admin.reports.content,
    totalPages: admin.reports.page?.totalPages ?? admin.reports.totalPages ?? 0,
  }
}
</script>

<template>
  <div class="admin-reports">
    <h1 class="admin-page-title">СКАРГИ</h1>

    <div class="filter-row">
      <select v-model="statusFilter" class="admin-select" @change="page = 0">
        <option value="OPEN">Відкриті</option>
        <option value="REVIEWED">Розглянуті</option>
        <option value="DISMISSED">Відхилені</option>
        <option value="">Усі</option>
      </select>
    </div>

    <table class="admin-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Автор скарги</th>
          <th>На кого</th>
          <th>Причина</th>
          <th>Опис</th>
          <th>Статус</th>
          <th>Дата</th>
          <th>Дії</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in getPageData().content" :key="r.id">
          <td>{{ r.id }}</td>
          <td>{{ r.reporterDisplayName }}</td>
          <td>{{ r.reportedUserDisplayName }}</td>
          <td>{{ REASON_LABELS[r.reason] || r.reason }}</td>
          <td class="desc-cell" :title="r.description || ''">{{ r.description ? r.description.substring(0, 40) + (r.description.length > 40 ? '...' : '') : '—' }}</td>
          <td>
            <span class="status-badge" :class="r.status.toLowerCase()">{{ r.status }}</span>
          </td>
          <td class="date-cell">{{ new Date(r.createdAt).toLocaleDateString('uk-UA') }}</td>
          <td>
            <button v-if="r.status === 'OPEN'" class="action-sm" @click="openReview(r)">Розглянути</button>
            <span v-else-if="r.adminComment" class="comment-hint" :title="r.adminComment">💬</span>
          </td>
        </tr>
        <tr v-if="!getPageData().content.length">
          <td colspan="8" class="empty-cell">Немає скарг</td>
        </tr>
      </tbody>
    </table>

    <div v-if="getPageData().totalPages > 1" class="pagination">
      <button :disabled="page === 0" @click="page--">←</button>
      <span>{{ page + 1 }} / {{ getPageData().totalPages }}</span>
      <button :disabled="page >= getPageData().totalPages - 1" @click="page++">→</button>
    </div>

    <div v-if="reviewModal" class="modal-overlay" @click.self="closeReview">
      <div class="modal-box">
        <h3>Скарга #{{ reviewModal.id }}</h3>
        <div class="review-info">
          <p><strong>Автор:</strong> {{ reviewModal.reporterDisplayName }}</p>
          <p><strong>На кого:</strong> {{ reviewModal.reportedUserDisplayName }}</p>
          <p><strong>Причина:</strong> {{ REASON_LABELS[reviewModal.reason] || reviewModal.reason }}</p>
          <p v-if="reviewModal.description"><strong>Опис:</strong> {{ reviewModal.description }}</p>
        </div>
        <label class="modal-label">Рішення</label>
        <select v-model="reviewStatus" class="admin-select">
          <option value="REVIEWED">Розглянуто</option>
          <option value="DISMISSED">Відхилено</option>
        </select>
        <label class="modal-label">Коментар (необов'язково)</label>
        <textarea v-model="reviewComment" class="admin-input admin-textarea" rows="3" placeholder="Коментар адміна..."></textarea>
        <div class="modal-actions">
          <button class="admin-btn primary" @click="confirmReview">Підтвердити</button>
          <button class="admin-btn outline" @click="closeReview">Скасувати</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-reports { padding: 0; }
.admin-page-title { font-family: var(--font-display); font-size: 2rem; color: var(--yellow); margin-bottom: 20px; }
.filter-row { margin-bottom: 16px; }
.desc-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.date-cell { white-space: nowrap; font-size: 0.85rem; color: var(--gray); }
.status-badge { font-size: 0.8rem; padding: 2px 10px; border: 1px solid var(--border); text-transform: uppercase; }
.status-badge.open { color: var(--yellow); border-color: var(--yellow-dim); }
.status-badge.reviewed { color: #4caf50; border-color: #388e3c; }
.status-badge.dismissed { color: var(--gray); border-color: var(--border); }
.comment-hint { cursor: help; }
.review-info { background: var(--panel-light); padding: 12px; margin-bottom: 16px; border: 1px solid var(--border); }
.review-info p { margin-bottom: 6px; font-size: 0.9rem; }
.review-info strong { color: var(--gray-light); }
.action-sm { padding: 4px 10px; font-size: 0.8rem; border: 1px solid var(--border); background: transparent; color: var(--gray-light); cursor: pointer; font-family: var(--font-body); transition: all 0.15s; }
.action-sm:hover { border-color: var(--yellow-dim); color: var(--yellow); }
.pagination { display: flex; align-items: center; gap: 12px; justify-content: center; margin-top: 16px; }
.pagination button { padding: 6px 14px; background: var(--panel); border: 1px solid var(--border); color: var(--gray-light); cursor: pointer; font-family: var(--font-body); }
.pagination button:disabled { opacity: 0.4; cursor: default; }
.pagination span { color: var(--gray); font-size: 0.9rem; }
.empty-cell { text-align: center; color: var(--gray); padding: 30px !important; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-box { background: var(--panel); border: 1px solid var(--border); padding: 24px; min-width: 420px; max-width: 520px; }
.modal-box h3 { font-family: var(--font-display); font-size: 1.4rem; color: var(--white); margin-bottom: 12px; }
.modal-label { display: block; color: var(--gray); font-size: 0.85rem; margin: 12px 0 4px; }
.modal-actions { display: flex; gap: 10px; margin-top: 16px; }
</style>

