<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '../stores/games'
import { useAuthStore } from '../stores/auth'
import { gameEmoji, genreColor } from '../utils/helpers'
import api from '../api/axios'
import type { Game, Page } from '../types'
import SuggestGameModal from '../components/SuggestGameModal.vue'

const router = useRouter()
const gameStore = useGameStore()
const auth = useAuthStore()

const search = ref('')
const selectedGenre = ref('')
const showSuggest = ref(false)

const games = ref<Game[]>([])
const loading = ref(false)
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = 20

const genres = ref<string[]>([])

const animatingHearts = ref<Set<number>>(new Set())

async function fetchPage(p = 0) {
  loading.value = true
  try {
    const params: Record<string, string | number> = { page: p, size: pageSize }
    if (search.value.trim()) params.q = search.value.trim()
    if (selectedGenre.value) params.genre = selectedGenre.value

    const { data } = await api.get<Page<Game>>('/games/search', { params })
    games.value = data.content
    page.value = data.page?.number ?? data.number ?? 0
    totalPages.value = data.page?.totalPages ?? data.totalPages ?? 0
    totalElements.value = data.page?.totalElements ?? data.totalElements ?? 0
  } catch {
    games.value = []
  } finally {
    loading.value = false
  }
}

function goToPage(p: number) {
  fetchPage(p)
}

function goToGame(gameId: number) {
  router.push({ path: '/search-parties', query: { gameId: String(gameId) } })
}

async function handleToggleFavorite(gameId: number) {
  animatingHearts.value = new Set([...animatingHearts.value, gameId])
  setTimeout(() => {
    const s = new Set(animatingHearts.value)
    s.delete(gameId)
    animatingHearts.value = s
  }, 400)
  await gameStore.toggleFavorite(gameId)
}

let searchTimeout: ReturnType<typeof setTimeout> | null = null
watch(search, () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => fetchPage(0), 350)
})

watch(selectedGenre, () => fetchPage(0))

onMounted(async () => {
  await gameStore.fetchGames()
  const set = new Set<string>()
  gameStore.games.forEach((g) => { if (g.genre) set.add(g.genre) })
  genres.value = Array.from(set).sort()

  fetchPage(0)

  if (auth.isLoggedIn) {
    gameStore.fetchFavorites()
  }
})
</script>

<template>
  <div class="games-page">
    <div class="games-container">
      <div class="section-head">
        <div class="section-title-row">
          <div class="section-title">
            ВСІ ІГРИ
            <span class="section-count">{{ totalElements }} ігор</span>
          </div>
          <router-link v-if="auth.isLoggedIn" to="/favorite-games" class="fav-page-btn">
            ❤ УЛЮБЛЕНІ
            <span v-if="gameStore.favoriteGames.length" class="fav-count">{{ gameStore.favoriteGames.length }}</span>
          </router-link>
        </div>
      </div>

      <div class="games-filters">
        <input
          v-model="search"
          class="filter-search"
          placeholder="🔍 Пошук гри..."
        />
        <select v-model="selectedGenre" class="filter-select">
          <option value="">Всі жанри</option>
          <option v-for="genre in genres" :key="genre" :value="genre">
            {{ genre }}
          </option>
        </select>
      </div>

      <div v-if="loading" class="empty-state">
        <div class="empty-icon">⏳</div>
        <h3>ЗАВАНТАЖЕННЯ...</h3>
      </div>

      <template v-else>
        <div class="games-grid">
          <div
            v-for="game in games"
            :key="game.id"
            class="g-card"
            @click="goToGame(game.id)"
          >
            <button
              v-if="auth.isLoggedIn"
              class="heart-btn"
              :class="{ filled: gameStore.isFavorite(game.id), animating: animatingHearts.has(game.id) }"
              :title="gameStore.isFavorite(game.id) ? 'Видалити з улюблених' : 'Додати в улюблені'"
              @click.stop="handleToggleFavorite(game.id)"
            >
              <svg viewBox="0 0 24 24" class="heart-svg">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5
                  2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09
                  C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5
                  c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" />
              </svg>
            </button>

            <div class="g-card-left">
              <img
                v-if="game.imageUrl"
                :src="game.imageUrl"
                :alt="game.name"
                class="g-cover"
              />
              <div v-else class="g-cover-ph">{{ gameEmoji(game.genre) }}</div>
            </div>

            <div class="g-card-right">
              <div class="g-name">{{ game.name }}</div>

              <div class="g-meta-row">
                <span v-if="game.genre" class="g-genre" :class="genreColor(game.genre)">
                  {{ game.genre }}
                </span>
                <span v-if="game.releaseYear" class="g-year">{{ game.releaseYear }}</span>
              </div>

              <div class="g-bottom">
                <span class="g-party-size">👥 до {{ game.maxPartySize }} гравців</span>
                <span class="g-find-btn">ЗНАЙТИ ЛОБІ →</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="totalPages > 1" class="pagination">
          <button
            class="page-btn"
            :disabled="page === 0"
            @click="goToPage(page - 1)"
          >← НАЗАД</button>

          <template v-for="p in totalPages" :key="p">
            <button
              class="page-btn"
              :class="{ active: page === p - 1 }"
              @click="goToPage(p - 1)"
            >{{ p }}</button>
          </template>

          <button
            class="page-btn"
            :disabled="page >= totalPages - 1"
            @click="goToPage(page + 1)"
          >ДАЛІ →</button>
        </div>

        <div v-if="!games.length" class="empty-state">
          <div class="empty-icon">🎮</div>
          <h3>ІГОР НЕ ЗНАЙДЕНО</h3>
          <p>Спробуй змінити фільтри або пошуковий запит</p>
        </div>
      </template>

      <div v-if="auth.isLoggedIn" class="suggest-game-footer">
        Не знайшли свою гру?
        <button class="suggest-game-link" @click="showSuggest = true">Запропонувати нову</button>
      </div>

      <SuggestGameModal v-if="showSuggest" @close="showSuggest = false" @submitted="showSuggest = false" />
    </div>
  </div>
</template>

<style scoped>
.games-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}

.games-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 64px 80px;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 8px;
}

.fav-page-btn {
  font-family: var(--font-display), sans-serif;
  font-size: 14px;
  letter-spacing: 2px;
  padding: 8px 20px;
  background: transparent;
  border: 2px solid var(--yellow-dim);
  color: var(--yellow);
  cursor: pointer;
  transition: all 0.15s;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.fav-page-btn:hover {
  background: var(--yellow);
  color: var(--black);
  border-color: var(--yellow);
}
.fav-count {
  font-family: var(--font-body), sans-serif;
  font-size: 12px;
  font-weight: 700;
  background: var(--yellow);
  color: var(--black);
  padding: 1px 7px;
  border-radius: 10px;
  line-height: 1.3;
}
.fav-page-btn:hover .fav-count {
  background: var(--black);
  color: var(--yellow);
}

.suggest-game-footer {
  text-align: center;
  padding: 24px 0 8px;
  color: var(--gray);
  font-size: 0.85rem;
}
.suggest-game-link {
  background: none;
  border: none;
  color: var(--gray);
  cursor: pointer;
  font-family: var(--font-body), sans-serif;
  font-size: 0.85rem;
  text-decoration: underline;
  text-underline-offset: 3px;
  transition: color 0.15s;
  padding: 0;
}
.suggest-game-link:hover {
  color: var(--yellow);
}

.heart-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  width: 36px;
  height: 36px;
  border: none;
  background: rgba(10, 10, 11, 0.7);
  backdrop-filter: blur(4px);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: transform 0.2s, background 0.2s;
  opacity: 0;
}
.g-card:hover .heart-btn,
.heart-btn.filled {
  opacity: 1;
}
.heart-btn:hover {
  transform: scale(1.15);
  background: rgba(10, 10, 11, 0.9);
}

.heart-svg {
  width: 20px;
  height: 20px;
  fill: transparent;
  stroke: var(--gray);
  stroke-width: 2;
  transition: fill 0.25s, stroke 0.25s;
}
.heart-btn.filled .heart-svg {
  fill: var(--yellow);
  stroke: var(--yellow);
}
.heart-btn:not(.filled):hover .heart-svg {
  stroke: var(--yellow-dim);
}

.heart-btn.animating {
  animation: heart-pulse 0.4s ease;
}
@keyframes heart-pulse {
  0%   { transform: scale(1); }
  30%  { transform: scale(1.35); }
  60%  { transform: scale(0.95); }
  100% { transform: scale(1); }
}

.games-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.games-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
  gap: 16px;
}

.g-card {
  display: flex;
  background: var(--panel);
  border: 2px solid var(--border);
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s, transform 0.15s, box-shadow 0.2s;
  position: relative;
}
.g-card::after {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 3px; height: 100%;
  background: var(--yellow);
  transform: scaleY(0);
  transform-origin: top;
  transition: transform 0.25s;
}
.g-card:hover {
  border-color: var(--yellow-dim);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.35);
}
.g-card:hover::after {
  transform: scaleY(1);
}

.g-card-left {
  width: 120px;
  flex-shrink: 0;
  overflow: hidden;
  background: var(--dark);
}

.g-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
  display: block;
}
.g-card:hover .g-cover {
  transform: scale(1.08);
}

.g-cover-ph {
  width: 100%;
  height: 100%;
  min-height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  background: var(--panel-light);
}

.g-card-right {
  flex: 1;
  padding: 18px 22px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.g-name {
  font-family: var(--font-display), sans-serif;
  font-size: 22px;
  letter-spacing: 2px;
  color: var(--white);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.15s;
}
.g-card:hover .g-name {
  color: var(--yellow);
}

.g-meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.g-genre {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
  text-transform: uppercase;
  padding: 2px 10px;
  border: 1px solid var(--border);
  color: var(--gray-light);
  background: rgba(255, 255, 255, 0.03);
}
.g-genre.blue {
  border-color: #3498db44;
  color: #5dade2;
  background: rgba(52, 152, 219, 0.08);
}
.g-genre.purple {
  border-color: #9b59b644;
  color: #bb8fce;
  background: rgba(155, 89, 182, 0.08);
}
.g-genre.red {
  border-color: #c0392b44;
  color: #e57373;
  background: rgba(192, 57, 43, 0.08);
}
.g-genre.green {
  border-color: #27ae6044;
  color: #58d68d;
  background: rgba(39, 174, 96, 0.08);
}

.g-year {
  font-size: 12px;
  color: var(--gray);
  letter-spacing: 1px;
}

.g-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 6px;
}

.g-party-size {
  font-size: 12px;
  color: var(--gray);
  letter-spacing: 0.5px;
}

.g-find-btn {
  font-family: var(--font-display), sans-serif;
  font-size: 11px;
  letter-spacing: 2px;
  color: var(--yellow-dim);
  opacity: 0;
  transition: opacity 0.2s, color 0.2s;
}
.g-card:hover .g-find-btn {
  opacity: 1;
  color: var(--yellow);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
.empty-state h3 {
  font-family: var(--font-display), sans-serif;
  font-size: 28px;
  letter-spacing: 2px;
  color: var(--yellow);
  margin-bottom: 8px;
}
.empty-state p {
  color: var(--gray);
  font-size: 15px;
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
  font-family: var(--font-display), sans-serif;
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

@media (max-width: 900px) {
  .games-grid {
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  }
}
@media (max-width: 600px) {
  .games-grid {
    grid-template-columns: 1fr;
  }
  .games-container {
    padding: 24px 20px 60px;
  }
  .section-title-row {
    flex-direction: column;
    align-items: flex-start;
  }
  .fav-page-btn {
    width: 100%;
    justify-content: center;
    font-size: 12px;
    padding: 8px 12px;
  }
  .heart-btn {
    opacity: 1;
  }
}
</style>
