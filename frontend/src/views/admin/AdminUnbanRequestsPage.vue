<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useAdminStore } from '../../stores/admin'

const admin = useAdminStore()
const page = ref(0)
const statusFilter = ref('PENDING')

const reviewModal = ref<any>(null)
const reviewStatus = ref('APPROVED')
const reviewComment = ref('')
const historyOpen = ref(false)

function load() {
  admin.fetchUnbanRequests(page.value, 20, statusFilter.value || undefined)
}

onMounted(load)
watch([page, statusFilter], load)

function openReview(r: any) {
  reviewModal.value = r
  reviewStatus.value = 'APPROVED'
  reviewComment.value = ''
  historyOpen.value = false
}
function closeReview() { reviewModal.value = null }
async function confirmReview() {
  if (!reviewModal.value) return
  await admin.reviewUnbanRequest(reviewModal.value.id, reviewStatus.value, reviewComment.value || undefined)
  closeReview()
  load()
}

function getPageData() {
  if (!admin.unbanRequests) return { content: [], totalPages: 0 }
  return {
    content: admin.unbanRequests.content,
    totalPages: admin.unbanRequests.page?.totalPages ?? admin.unbanRequests.totalPages ?? 0,
  }
}

function formatDate(d: string) {
  return new Date(d).toLocaleDateString('uk-UA', { day: '2-digit', month: '2-digit', year: 'numeric' })
}
</script>

<template>
  <div class="admin-unban-requests">
    <h1 class="admin-page-title">ЗАПИТИ НА РОЗБАН</h1>

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
          <th>Користувач</th>
          <th>Email</th>
          <th>Причина запиту</th>
          <th>Статус</th>
          <th>Дата</th>
          <th>Дії</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in getPageData().content" :key="r.id">
          <td>{{ r.id }}</td>
          <td>{{ r.userDisplayName }}</td>
          <td>{{ r.userEmail }}</td>
          <td class="reason-cell" :title="r.reason">{{ r.reason.length > 50 ? r.reason.substring(0, 50) + '...' : r.reason }}</td>
          <td>
            <span class="status-badge" :class="r.status.toLowerCase()">{{ r.status }}</span>
          </td>
          <td class="date-cell">{{ new Date(r.createdAt).toLocaleDateString('uk-UA') }}</td>
          <td>
            <button v-if="r.status === 'PENDING'" class="action-sm" @click="openReview(r)">Розглянути</button>
            <span v-else-if="r.adminComment" class="comment-hint" :title="r.adminComment">💬</span>
          </td>
        </tr>
        <tr v-if="!getPageData().content.length">
          <td colspan="7" class="empty-cell">Немає запитів</td>
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
        <h3>Запит на розбан #{{ reviewModal.id }}</h3>
        <div class="review-info">
          <p><strong>Користувач:</strong> {{ reviewModal.userDisplayName }}</p>
          <p><strong>Email:</strong> {{ reviewModal.userEmail }}</p>
          <p><strong>Причина бану:</strong> {{ reviewModal.banReason || '—' }}</p>
          <p><strong>Причина запиту:</strong> {{ reviewModal.reason }}</p>
        </div>

        <div v-if="reviewModal.banHistory?.length > 1" class="ban-history">
          <button class="ban-history-toggle" @click="historyOpen = !historyOpen">
            {{ historyOpen ? '▾' : '▸' }} Історія банів ({{ reviewModal.banHistory.length }})
          </button>
          <div v-if="historyOpen" class="ban-history-list">
            <div v-for="(bh, i) in reviewModal.banHistory" :key="i" class="ban-history-item">
              <div class="bh-date">{{ formatDate(bh.createdAt) }}</div>
              <div class="bh-reason">Бан: {{ bh.banReason || 'Без причини' }}</div>
              <div class="bh-request">Запит: {{ bh.requestReason }}</div>
              <div class="bh-status">{{ bh.requestStatus }}</div>
            </div>
          </div>
        </div>

        <label class="modal-label">Рішення</label>
        <select v-model="reviewStatus" class="admin-select">
          <option value="APPROVED">Схвалити (розбанити)</option>
          <option value="REJECTED">Відхилити</option>
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
.admin-unban-requests { padding: 0; }
.admin-page-title { font-family: var(--font-display); font-size: 2rem; color: var(--yellow); margin-bottom: 20px; }
.filter-row { margin-bottom: 16px; }
.reason-cell { max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.date-cell { white-space: nowrap; font-size: 0.85rem; color: var(--gray); }
.status-badge { font-size: 0.8rem; padding: 2px 10px; border: 1px solid var(--border); text-transform: uppercase; }
.status-badge.pending { color: var(--yellow); border-color: var(--yellow-dim); }
.status-badge.approved { color: #4caf50; border-color: #388e3c; }
.status-badge.rejected { color: var(--red); border-color: var(--red-dim); }
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

.ban-history { margin-bottom: 12px; }
.ban-history-toggle {
  background: none;
  border: none;
  color: var(--yellow);
  font-family: var(--font-body);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s;
}
.ban-history-toggle:hover { color: var(--white); }
.ban-history-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 180px;
  overflow-y: auto;
}
.ban-history-item {
  background: var(--panel);
  border: 1px solid var(--border);
  border-left: 3px solid var(--red-dim);
  padding: 8px 12px;
  font-size: 0.85rem;
}
.bh-date { color: var(--gray); font-size: 0.78rem; margin-bottom: 2px; }
.bh-reason { color: var(--gray-light); }
.bh-request { color: var(--gray-light); font-size: 0.82rem; margin-top: 2px; }
.bh-status { color: var(--gray); font-size: 0.75rem; text-transform: uppercase; margin-top: 2px; }
</style>

