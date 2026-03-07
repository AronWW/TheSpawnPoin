<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useFriendStore } from '../stores/friends'
import { useChatStore } from '../stores/chat'
import { API_BASE_URL } from '../config'
import api from '../api/axios'
import type { Profile, Game } from '../types'
import { skillLabel, gameEmoji, genreColor } from '../utils/helpers'
import ReportUserModal from '../components/ReportUserModal.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const friendStore = useFriendStore()
const chatStore = useChatStore()

const profile = ref<Profile | null>(null)
const favoriteGames = ref<Game[]>([])
const loading = ref(false)
const error = ref('')
const addingFriend = ref(false)
const showReport = ref(false)

const REGION_LABELS: Record<string, string> = {
  EUROPE:        '🌍 Європа',
  CIS:           '🌏 СНД / Східна Європа',
  NORTH_AMERICA: '🌎 Північна Америка',
  SOUTH_AMERICA: '🌎 Південна Америка',
  ASIA:          '🌏 Азія',
  MIDDLE_EAST:   '🌍 Близький Схід',
  AFRICA:        '🌍 Африка',
  OCEANIA:       '🌏 Океанія',
}

const PLAY_STYLE_LABELS: Record<string, string> = {
  CASUAL:           'Казуальний',
  SEMI_COMPETITIVE: 'Напів-змагальний',
  COMPETITIVE:      'Змагальний',
}

const LANGUAGE_NAMES: Record<string, string> = {
  UA: 'Українська', EN: 'English',    PL: 'Polski',     DE: 'Deutsch',
  FR: 'Français',   ES: 'Español',    PT: 'Português',  TR: 'Türkçe',
  KO: '한국어',     ZH: '中文',       JA: '日本語',     IT: 'Italiano',
  NL: 'Nederlands', SV: 'Svenska',    NO: 'Norsk',      DA: 'Dansk',
  FI: 'Suomi',      CS: 'Čeština',    SK: 'Slovenčina', HU: 'Magyar',
  RO: 'Română',     BG: 'Български',  HR: 'Hrvatski',   SR: 'Srpski',
  AR: 'العربية',    HI: 'हिन्दी',    VI: 'Tiếng Việt', TH: 'ภาษาไทย',
  ID: 'Bahasa Indonesia',
}

const PLATFORM_LABELS: Record<string, string> = {
  PC: 'PC', PLAYSTATION: 'PlayStation', XBOX: 'Xbox',
  NINTENDO: 'Nintendo', MOBILE: 'Mobile', OTHER: 'Інше',
}

interface SocialConfig {
  key: keyof Profile
  label: string
  color: string
  buildUrl: (val: string) => string
  displayVal: (val: string) => string
  copyable?: boolean
}

const SOCIALS: SocialConfig[] = [
  {
    key: 'discord',
    label: 'Discord',
    color: '#5865F2',
    buildUrl: () => '',
    displayVal: (v) => v,
    copyable: true,
  },
  {
    key: 'steam',
    label: 'Steam',
    color: '#1b2838',
    buildUrl: (v) => v.startsWith('http') ? v : `https://steamcommunity.com/id/${v}`,
    displayVal: (v) => v.replace(/https?:\/\/(www\.)?steamcommunity\.com\/(id\/|profiles\/)?/, '').replace(/\/$/, ''),
  },
  {
    key: 'twitch',
    label: 'Twitch',
    color: '#9146FF',
    buildUrl: (v) => v.startsWith('http') ? v : `https://twitch.tv/${v}`,
    displayVal: (v) => v.replace(/https?:\/\/(www\.)?twitch\.tv\//, '').replace(/\/$/, ''),
  },
  {
    key: 'xbox',
    label: 'Xbox',
    color: '#107C10',
    buildUrl: (v) => `https://account.xbox.com/en-us/profile?gamertag=${encodeURIComponent(v)}`,
    displayVal: (v) => v,
  },
  {
    key: 'playstation',
    label: 'PlayStation',
    color: '#003791',
    buildUrl: (v) => `https://psnprofiles.com/${encodeURIComponent(v)}`,
    displayVal: (v) => v,
  },
  {
    key: 'nintendo',
    label: 'Nintendo',
    color: '#E4000F',
    buildUrl: () => '',
    displayVal: (v) => v,
    copyable: true,
  },
]

function resolveAvatar(url: string | null): string {
  if (!url) return API_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return API_BASE_URL + url
}

const copied = ref<string | null>(null)
async function copyToClipboard(val: string, key: string) {
  await navigator.clipboard.writeText(val)
  copied.value = key
  setTimeout(() => { copied.value = null }, 2000)
}

const isOwnProfile = computed(() =>
    auth.user && profile.value && auth.user.id === profile.value.userId
)
const isFriend = computed(() =>
    profile.value ? friendStore.friends.some(f => f.userId === profile.value!.userId) : false
)
const hasPendingRequest = computed(() =>
    profile.value
        ? friendStore.outgoingRequests.some(r => r.receiverId === profile.value!.userId)
        || friendStore.incomingRequests.some(r => r.senderId === profile.value!.userId)
        : false
)
const activeSocials = computed(() =>
    SOCIALS.filter(s => profile.value && profile.value[s.key])
)
const hasGameProfile = computed(() =>
        profile.value && (
            profile.value.skillLevel || profile.value.playStyle ||
            profile.value.platforms?.length || profile.value.languages?.length
        )
)

async function fetchProfile(userId: string | string[]) {
  const id = Array.isArray(userId) ? userId[0] : userId
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get<Profile>(`/profile/${id}`)
    profile.value = data
    try {
      const { data: games } = await api.get<Game[]>(`/users/${id}/games`)
      favoriteGames.value = games
    } catch {
      favoriteGames.value = []
    }
  } catch {
    error.value = 'Не вдалося завантажити профіль'
    profile.value = null
    favoriteGames.value = []
  } finally {
    loading.value = false
  }
}

async function sendFriendRequest() {
  if (!profile.value) return
  addingFriend.value = true
  try { await friendStore.sendRequest(profile.value.userId) }
  catch { } finally { addingFriend.value = false }
}

async function openDm() {
  if (!profile.value?.email) return
  await chatStore.openDm(profile.value.email)
  router.push('/chat')
}

function goToGame(gameId: number) {
  router.push({ path: '/search-parties', query: { gameId: String(gameId) } })
}

onMounted(async () => {
  if (route.params.userId) fetchProfile(route.params.userId)
  if (auth.isLoggedIn) {
    await Promise.all([
      friendStore.fetchFriends(),
      friendStore.fetchIncomingRequests(),
      friendStore.fetchOutgoingRequests(),
    ])
  }
})

watch(() => route.params.userId, (newId) => {
  if (newId) fetchProfile(newId)
})
</script>

<template>
  <div class="profile-page">
    <div class="profile-container">

      <div v-if="loading" class="empty-state"><p>Завантаження профілю...</p></div>

      <div v-else-if="error" class="empty-state">
        <div class="empty-icon">😕</div>
        <h3>{{ error }}</h3>
        <router-link to="/" class="action-btn">НА ГОЛОВНУ</router-link>
      </div>

      <div v-else-if="profile" class="profile-content">

        <div class="profile-header ink-panel">
          <img :src="resolveAvatar(profile.avatarUrl)" :alt="profile.displayName" class="profile-avatar" />
          <div class="profile-header-info">
            <h1 class="profile-display-name">{{ profile.displayName }}</h1>
            <div v-if="profile.fullName" class="profile-full-name">{{ profile.fullName }}</div>

            <div class="profile-meta">
              <span v-if="profile.country || profile.region" class="meta-item">
                <span v-if="profile.country">{{ profile.country }}</span>
                <span v-if="profile.country && profile.region"> · </span>
                <span v-if="profile.region">{{ REGION_LABELS[profile.region] ?? profile.region }}</span>
              </span>
              <span v-if="profile.birthDate" class="meta-item">
                {{ new Date(profile.birthDate).toLocaleDateString('uk-UA', { day: 'numeric', month: 'long', year: 'numeric' }) }}
              </span>
            </div>

            <div v-if="isOwnProfile" class="profile-edit-hint">
              <router-link to="/settings" class="edit-link">Редагувати профіль</router-link>
            </div>
            <div v-else-if="auth.isLoggedIn" class="profile-actions">
              <button class="profile-action-btn" @click="openDm">💬 Написати</button>
              <button v-if="isFriend" class="profile-action-btn friend-badge-btn" disabled>✓ Друзі</button>
              <button v-else-if="hasPendingRequest" class="profile-action-btn pending-badge-btn" disabled>⏳ Запит надіслано</button>
              <button v-else class="profile-action-btn add-friend-btn" :disabled="addingFriend" @click="sendFriendRequest">
                + Додати в друзі
              </button>
              <button class="profile-action-btn report-btn" @click="showReport = true">🚩 Скарга</button>
            </div>
          </div>
        </div>

        <ReportUserModal
          v-if="showReport && profile"
          :reported-user-id="profile.userId"
          :reported-user-name="profile.displayName"
          @close="showReport = false"
        />

        <div v-if="profile.bio" class="profile-section ink-panel">
          <h2 class="section-label">ПРО ГРАВЦЯ</h2>
          <p class="profile-bio">{{ profile.bio }}</p>
        </div>

        <div v-if="hasGameProfile" class="profile-section ink-panel">
          <h2 class="section-label">ІГРОВИЙ ПРОФІЛЬ</h2>
          <div class="profile-tags-grid">

            <div v-if="profile.skillLevel" class="profile-tag-group">
              <span class="tag-label">Рівень гри</span>
              <span class="tag-value">{{ skillLabel(profile.skillLevel) }}</span>
            </div>

            <div v-if="profile.playStyle" class="profile-tag-group">
              <span class="tag-label">Стиль гри</span>
              <span class="tag-value">{{ PLAY_STYLE_LABELS[profile.playStyle] ?? profile.playStyle }}</span>
            </div>

            <div v-if="profile.platforms?.length" class="profile-tag-group full-width">
              <span class="tag-label">Платформи</span>
              <div class="tag-chips">
                <span v-for="p in profile.platforms" :key="p" class="chip">{{ PLATFORM_LABELS[p] ?? p }}</span>
              </div>
            </div>

            <div v-if="profile.languages?.length" class="profile-tag-group full-width">
              <span class="tag-label">Мови спілкування</span>
              <div class="tag-chips">
                <span v-for="lang in profile.languages" :key="lang" class="chip lang-chip">
                  <span class="lang-code">{{ lang }}</span>
                  <span class="lang-name-text">{{ LANGUAGE_NAMES[lang] ?? lang }}</span>
                </span>
              </div>
            </div>

          </div>
        </div>

        <div v-if="activeSocials.length" class="profile-section ink-panel">
          <h2 class="section-label">ЗНАЙТИ В МЕРЕЖАХ</h2>
          <div class="social-grid">

            <template v-for="s in activeSocials" :key="s.key">
              <a
                  v-if="!s.copyable && s.buildUrl(profile[s.key] as string)"
                  :href="s.buildUrl(profile[s.key] as string)"
                  target="_blank"
                  rel="noopener"
                  class="social-badge"
                  :style="{ '--badge-color': s.color }"
              >
                <span class="badge-accent"></span>
                <div class="badge-content">
                  <span class="badge-label">{{ s.label }}</span>
                  <span class="badge-value">{{ s.displayVal(profile[s.key] as string) }}</span>
                </div>
                <span class="badge-arrow">↗</span>
              </a>

              <button
                  v-else
                  class="social-badge social-badge-copy"
                  :style="{ '--badge-color': s.color }"
                  @click="copyToClipboard(profile[s.key] as string, s.key as string)"
              >
                <span class="badge-accent"></span>
                <div class="badge-content">
                  <span class="badge-label">{{ s.label }}</span>
                  <span class="badge-value">{{ s.displayVal(profile[s.key] as string) }}</span>
                </div>
                <span class="badge-arrow">{{ copied === s.key ? '✓' : '⎘' }}</span>
              </button>
            </template>

          </div>
        </div>

        <div v-if="favoriteGames.length" class="profile-section ink-panel">
          <h2 class="section-label">УЛЮБЛЕНІ ІГРИ</h2>
          <div class="fav-games-grid">
            <div
                v-for="game in favoriteGames"
                :key="game.id"
                class="fav-game-card"
                @click="goToGame(game.id)"
            >
              <img
                  v-if="game.imageUrl"
                  :src="game.imageUrl"
                  :alt="game.name"
                  class="fav-game-img"
              />
              <div v-else class="fav-game-ph">{{ gameEmoji(game.genre) }}</div>
              <div class="fav-game-info">
                <span class="fav-game-name">{{ game.name }}</span>
                <span v-if="game.genre" class="fav-game-genre" :class="genreColor(game.genre)">{{ game.genre }}</span>
              </div>
            </div>
          </div>
          <div v-if="isOwnProfile" class="fav-games-add">
            <router-link to="/games" class="fav-add-link">+ Додати ще ігри</router-link>
          </div>
        </div>

        <div v-else-if="isOwnProfile" class="profile-section ink-panel">
          <h2 class="section-label">УЛЮБЛЕНІ ІГРИ</h2>
          <div class="fav-games-empty">
            <span class="fav-empty-icon">🎮</span>
            <p>Ти ще не додав улюблених ігор</p>
            <router-link to="/favorite-games" class="fav-add-link">Перейти до каталогу ігор →</router-link>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}

.profile-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 32px;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-header {
  display: flex;
  gap: 28px;
  padding: 28px;
  align-items: flex-start;
}

.profile-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--yellow-dim);
  box-shadow: 0 0 20px rgba(245, 197, 24, 0.15);
  flex-shrink: 0;
}

.profile-header-info {
  flex: 1;
}

.profile-display-name {
  font-family: var(--font-display), sans-serif;
  font-size: 36px;
  letter-spacing: 2px;
  color: var(--yellow);
  line-height: 1;
  margin-bottom: 4px;
}

.profile-full-name {
  font-size: 15px;
  color: var(--gray-light);
  margin-bottom: 8px;
}

.profile-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 10px;
}

.meta-item {
  font-size: 13px;
  color: var(--gray);
}

.edit-link {
  font-size: 12px;
  color: var(--gray);
  letter-spacing: 1px;
  text-transform: uppercase;
  transition: color 0.15s;
  display: inline-block;
}
.edit-link:hover {
  color: var(--yellow);
}

.profile-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.profile-action-btn {
  font-family: var(--font-body), sans-serif;
  font-weight: 600;
  font-size: 12px;
  letter-spacing: 1px;
  padding: 8px 16px;
  border: 2px solid var(--border);
  background: var(--panel-light);
  color: var(--white);
  cursor: pointer;
  transition: all 0.15s;
}
.profile-action-btn:hover:not(:disabled) {
  border-color: var(--yellow-dim);
  background: var(--yellow-glow);
  color: var(--yellow);
}
.profile-action-btn:disabled {
  opacity: 0.7;
  cursor: default;
}
.add-friend-btn    { border-color: var(--yellow-dim); color: var(--yellow); }
.friend-badge-btn  { border-color: rgba(46,204,113,0.3); color: #2ecc71; background: rgba(46,204,113,0.08); }
.pending-badge-btn { color: var(--gray); }
.report-btn        { border-color: var(--red-dim); color: var(--red); font-size: 11px; }
.report-btn:hover  { background: rgba(192, 57, 43, 0.12); border-color: var(--red); }

.profile-section {
  padding: 24px 28px;
}

.section-label {
  font-family: var(--font-display), sans-serif;
  font-size: 18px;
  letter-spacing: 3px;
  color: var(--yellow);
  margin-bottom: 16px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 8px;
}

.profile-bio {
  color: var(--gray-light);
  font-size: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.profile-tags-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.profile-tag-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.full-width {
  grid-column: 1 / -1;
}

.tag-label {
  font-size: 11px;
  color: var(--gray);
  letter-spacing: 2px;
  text-transform: uppercase;
}

.tag-value {
  font-size: 15px;
  color: var(--white);
  font-weight: 600;
}

.tag-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  font-size: 12px;
  padding: 4px 10px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  color: var(--gray-light);
}

.lang-chip {
  display: flex;
  align-items: center;
  gap: 6px;
}

.lang-code {
  font-family: var(--font-display), sans-serif;
  font-size: 10px;
  letter-spacing: 1px;
  color: var(--yellow);
  opacity: 0.8;
}

.lang-name-text {
  font-size: 12px;
  color: var(--gray-light);
}

.social-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
}

.social-badge {
  --badge-color: var(--yellow);
  display: flex;
  align-items: stretch;
  background: var(--panel-light);
  border: 2px solid var(--border);
  text-decoration: none;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.1s;
  overflow: hidden;
  position: relative;
  font-family: inherit;
}

.social-badge:hover {
  border-color: var(--badge-color, var(--yellow-dim));
  box-shadow: 0 0 12px color-mix(in srgb, var(--badge-color, #f5c518) 25%, transparent);
  transform: translateY(-1px);
}

.badge-accent {
  display: block;
  width: 4px;
  min-height: 100%;
  background: var(--badge-color, var(--yellow));
  align-self: stretch;
  flex-shrink: 0;
}

.badge-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 8px 8px 8px 10px;
  min-width: 0;
}

.badge-label {
  font-size: 10px;
  font-family: var(--font-display), sans-serif;
  letter-spacing: 1.5px;
  color: var(--gray);
  text-transform: uppercase;
  line-height: 1;
  margin-bottom: 4px;
  white-space: nowrap;
}

.badge-value {
  font-size: 13px;
  color: var(--white);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.badge-arrow {
  font-size: 14px;
  color: var(--gray);
  padding: 0 12px 0 6px;
  align-self: center;
  flex-shrink: 0;
  transition: color 0.15s;
}

.social-badge:hover .badge-arrow {
  color: var(--badge-color, var(--yellow));
}

.social-badge-copy {
  font-family: inherit;
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
  font-size: 24px;
  letter-spacing: 2px;
  color: var(--yellow);
  margin-bottom: 8px;
}

.action-btn {
  font-family: var(--font-display), sans-serif;
  letter-spacing: 2px;
  font-size: 15px;
  padding: 10px 28px;
  border: 2px solid var(--yellow);
  background: var(--yellow);
  color: var(--black);
  text-transform: uppercase;
  transition: background 0.15s;
  display: inline-block;
  margin-top: 16px;
}

.action-btn:hover {
  background: var(--yellow-dim);
}

.fav-games-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.fav-game-card {
  background: var(--panel-light);
  border: 2px solid var(--border);
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s, transform 0.15s, box-shadow 0.2s;
  position: relative;
}

.fav-game-card:hover {
  border-color: var(--yellow-dim);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.35);
}

.fav-game-img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  display: block;
  transition: transform 0.3s;
}

.fav-game-card:hover .fav-game-img {
  transform: scale(1.05);
}

.fav-game-ph {
  width: 100%;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  background: var(--panel);
}

.fav-game-info {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.fav-game-name {
  font-family: var(--font-display), sans-serif;
  font-size: 15px;
  letter-spacing: 1px;
  color: var(--white);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.15s;
}

.fav-game-card:hover .fav-game-name {
  color: var(--yellow);
}

.fav-game-genre {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: var(--gray);
}

.fav-game-genre.blue  { color: #5dade2; }
.fav-game-genre.purple { color: #bb8fce; }
.fav-game-genre.red   { color: #e57373; }
.fav-game-genre.green { color: #58d68d; }

.fav-games-add {
  margin-top: 14px;
  text-align: center;
}

.fav-add-link {
  font-family: var(--font-display), sans-serif;
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--gray);
  transition: color 0.15s;
}

.fav-add-link:hover {
  color: var(--yellow);
}

.fav-games-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px 0;
}

.fav-empty-icon {
  font-size: 36px;
}

.fav-games-empty p {
  color: var(--gray);
  font-size: 14px;
}

@media (max-width: 600px) {
  .profile-container {
    padding: 24px 16px;
  }

  .profile-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .profile-meta {
    justify-content: center;
  }

  .profile-actions {
    justify-content: center;
  }

  .profile-tags-grid {
    grid-template-columns: 1fr;
  }

  .social-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>