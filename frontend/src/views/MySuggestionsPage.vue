<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api/axios'
import type { GameSuggestion } from '../types'

const suggestions = ref<GameSuggestion[]>([])
const loading = ref(false)

const STATUS_LABELS: Record<string, string> = {
  PENDING: '⏳ На розгляді',
  APPROVED: '✅ Схвалено',
  REJECTED: '❌ Відхилено',
}

async function fetchMySuggestions() {
  loading.value = true
  try {
    const { data } = await api.get<GameSuggestion[]>('/game-suggestions/my')
    suggestions.value = data
  } catch {
    suggestions.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchMySuggestions)
</script>

<template>
  <div class="suggestions-page">
    <div class="suggestions-container">
      <div class="page-header">
        <h1 class="page-title">МОЇ ЗАЯВКИ НА ІГРИ</h1>
        <router-link to="/games" class="back-link">← Назад до ігор</router-link>
      </div>

      <div v-if="loading" class="empty-state">
        <div class="empty-icon">⏳</div>
        <h3>ЗАВАНТАЖЕННЯ...</h3>
      </div>

      <div v-else-if="!suggestions.length" class="empty-state">
        <div class="empty-icon">💡</div>
        <h3>ЗАЯВОК ПОКИ НЕМАЄ</h3>
        <p>Ви ще не пропонували жодної гри. Зробити це можна на сторінці <router-link to="/games" class="inline-link">Ігри</router-link>.</p>
      </div>

      <div v-else class="suggestions-list">
        <div v-for="s in suggestions" :key="s.id" class="suggestion-card" :class="s.status.toLowerCase()">
          <div class="sg-left">
            <img v-if="s.imageUrl" :src="s.imageUrl" :alt="s.name" class="sg-cover" />
            <div v-else class="sg-cover-ph">🎮</div>
          </div>
          <div class="sg-info">
            <div class="sg-name">{{ s.name }}</div>
            <div class="sg-meta">
              <span v-if="s.genre" class="sg-genre">{{ s.genre }}</span>
              <span v-if="s.releaseYear" class="sg-year">{{ s.releaseYear }}</span>
              <span class="sg-party">👥 до {{ s.maxPartySize }}</span>
            </div>
            <div class="sg-date">Подано: {{ new Date(s.createdAt).toLocaleDateString('uk-UA') }}</div>
          </div>
          <div class="sg-status-col">
            <span class="sg-status" :class="s.status.toLowerCase()">{{ STATUS_LABELS[s.status] || s.status }}</span>
            <div v-if="s.adminComment" class="sg-comment">
              <span class="sg-comment-label">Коментар адміна:</span>
              {{ s.adminComment }}
            </div>
            <div v-if="s.reviewedAt" class="sg-reviewed">
              Розглянуто: {{ new Date(s.reviewedAt).toLocaleDateString('uk-UA') }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.suggestions-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}
.suggestions-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 32px 80px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-title {
  font-family: var(--font-display), sans-serif;
  font-size: 2rem;
  letter-spacing: 3px;
  color: var(--yellow);
}
.back-link {
  color: var(--gray);
  font-size: 0.9rem;
  text-decoration: none;
  transition: color 0.15s;
}
.back-link:hover { color: var(--yellow); }

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.suggestion-card {
  display: flex;
  align-items: stretch;
  background: var(--panel);
  border: 2px solid var(--border);
  overflow: hidden;
  transition: border-color 0.2s;
}
.suggestion-card.approved { border-left: 3px solid #4caf50; }
.suggestion-card.rejected { border-left: 3px solid var(--red); }
.suggestion-card.pending  { border-left: 3px solid var(--yellow); }

.sg-left {
  width: 80px;
  flex-shrink: 0;
  background: var(--dark);
}
.sg-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.sg-cover-ph {
  width: 100%;
  height: 100%;
  min-height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: var(--panel-light);
}

.sg-info {
  flex: 1;
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.sg-name {
  font-family: var(--font-display), sans-serif;
  font-size: 1.2rem;
  letter-spacing: 1px;
  color: var(--white);
}
.sg-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.8rem;
  color: var(--gray);
}
.sg-genre {
  padding: 1px 8px;
  border: 1px solid var(--border);
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}
.sg-date { font-size: 0.8rem; color: var(--gray); margin-top: 2px; }

.sg-status-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  padding: 14px 18px;
  min-width: 170px;
  gap: 6px;
}
.sg-status {
  font-size: 0.8rem;
  padding: 3px 12px;
  border: 1px solid var(--border);
  white-space: nowrap;
}
.sg-status.pending  { color: var(--yellow); border-color: var(--yellow-dim); }
.sg-status.approved { color: #4caf50; border-color: #388e3c; }
.sg-status.rejected { color: var(--red); border-color: var(--red-dim); }

.sg-comment {
  font-size: 0.8rem;
  color: var(--gray);
  text-align: right;
  max-width: 200px;
}
.sg-comment-label { color: var(--gray-light); font-weight: 600; display: block; }
.sg-reviewed { font-size: 0.75rem; color: var(--gray); }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-state h3 {
  font-family: var(--font-display), sans-serif;
  font-size: 1.5rem;
  letter-spacing: 2px;
  color: var(--yellow);
  margin-bottom: 8px;
}
.empty-state p { color: var(--gray); font-size: 0.95rem; }
.inline-link { color: var(--yellow); text-decoration: underline; }

@media (max-width: 640px) {
  .suggestion-card { flex-direction: column; }
  .sg-left { width: 100%; height: 100px; }
  .sg-status-col { align-items: flex-start; padding: 10px 18px; min-width: auto; }
}
</style>

