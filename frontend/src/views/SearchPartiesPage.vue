<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { usePartyStore } from '../stores/parties'
import { useGameStore } from '../stores/games'
import { useAuthStore } from '../stores/auth'
import PartyCard from '../components/PartyCard.vue'
import CreatePartyModal from '../components/CreatePartyModal.vue'
import type { Party } from '../types'

const router = useRouter()
const route = useRoute()
const partyStore = usePartyStore()
const gameStore = useGameStore()
const auth = useAuthStore()

const modalOpen = ref(false)
let searchDebounce: ReturnType<typeof setTimeout> | null = null

function selectParty(party: Party) {
  router.push(`/party/${party.id}`)
}

function goToPage(page: number) {
  partyStore.fetchSearchParties(page)
}

onMounted(async () => {
  await gameStore.fetchGames()

  if (route.query.gameId) {
    partyStore.filterGameId = Number(route.query.gameId)
  }

  await partyStore.fetchSearchParties(0)
})

watch(
    [
      () => partyStore.filterGameId,
      () => partyStore.filterPlatform,
      () => partyStore.filterSkillLevel,
      () => partyStore.filterLanguage,
    ],
    () => {
      partyStore.fetchSearchParties(0)
    },
)

watch(
    () => partyStore.search,
    () => {
      if (searchDebounce) clearTimeout(searchDebounce)
      searchDebounce = setTimeout(() => {
        partyStore.fetchSearchParties(0)
      }, 400)
    },
)
</script>

<template>
  <div class="search-page">
    <div class="search-container">
      <div class="section-head">
        <div class="section-title">
          ПОШУК ЛОБІ
          <span class="section-count">{{ partyStore.searchTotalElements }} знайдено</span>
        </div>
        <button v-if="auth.isLoggedIn" class="create-party-btn" @click="modalOpen = true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          СТВОРИТИ ЛОБІ
        </button>
      </div>

      <div class="filters-bar">
        <span class="filter-label">Фільтри:</span>

        <input
            class="filter-search"
            v-model="partyStore.search"
            placeholder="🔍 Пошук по грі або опису..."
        />

        <select class="filter-select" v-model="partyStore.filterGameId">
          <option :value="null">Всі ігри</option>
          <option v-for="game in gameStore.games" :key="game.id" :value="game.id">
            {{ game.name }}
          </option>
        </select>

        <select class="filter-select" v-model="partyStore.filterSkillLevel">
          <option value="">Будь-який рівень</option>
          <option value="BEGINNER">Початківець</option>
          <option value="INTERMEDIATE">Середній</option>
          <option value="ADVANCED">Просунутий</option>
          <option value="EXPERT">Експерт</option>
        </select>

        <select class="filter-select" v-model="partyStore.filterPlatform">
          <option value="">Всі платформи</option>
          <option value="PC">PC</option>
          <option value="PLAYSTATION">PlayStation</option>
          <option value="XBOX">Xbox</option>
          <option value="NINTENDO">Nintendo</option>
          <option value="MOBILE">Mobile</option>
        </select>
      </div>

      <div v-if="partyStore.searchLoading" class="empty-state">
        <div class="empty-state-icon">⏳</div>
        <div class="empty-state-title">ЗАВАНТАЖЕННЯ...</div>
      </div>

      <div v-else-if="partyStore.searchParties.length === 0" class="empty-state">
        <div class="empty-state-icon">🎮</div>
        <div class="empty-state-title">ЛОБІ НЕ ЗНАЙДЕНО</div>
        <div class="empty-state-sub">Спробуй змінити фільтри або створи власне лобі</div>
      </div>

      <template v-else>
        <div class="party-grid">
          <PartyCard
              v-for="party in partyStore.searchParties"
              :key="party.id"
              :party="party"
              @select="selectParty"
          />
        </div>

        <div v-if="partyStore.searchTotalPages > 1" class="pagination">
          <button
              class="page-btn"
              :disabled="partyStore.searchPage === 0"
              @click="goToPage(partyStore.searchPage - 1)"
          >
            ← НАЗАД
          </button>

          <button
              v-for="p in partyStore.searchTotalPages"
              :key="p"
              class="page-btn"
              :class="{ active: partyStore.searchPage === p - 1 }"
              @click="goToPage(p - 1)"
          >
            {{ p }}
          </button>

          <button
              class="page-btn"
              :disabled="partyStore.searchPage >= partyStore.searchTotalPages - 1"
              @click="goToPage(partyStore.searchPage + 1)"
          >
            ДАЛІ →
          </button>
        </div>
      </template>
    </div>
  </div>

  <CreatePartyModal :visible="modalOpen" @close="modalOpen = false" />
</template>

<style scoped>
.search-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}

.search-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 64px 80px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 40px;
  padding-top: 28px;
  border-top: 2px solid var(--border);
}

.page-btn {
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 2px;
  padding: 8px 16px;
  background: var(--panel);
  border: 2px solid var(--border);
  color: var(--gray-light);
  cursor: pointer;
  transition: all 0.15s;
}
.page-btn:hover:not(:disabled) {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  background: var(--yellow-glow);
}
.page-btn.active {
  border-color: var(--yellow);
  color: var(--black);
  background: var(--yellow);
}
.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .search-container {
    padding: 24px 20px 60px;
  }
}
</style>