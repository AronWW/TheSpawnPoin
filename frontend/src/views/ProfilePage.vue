<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useFriendStore } from '../stores/friends'
import { useChatStore } from '../stores/chat'
import { API_BASE_URL } from '../config'
import api from '../api/axios'
import type { Profile, Game, UserStats, ProfileComment } from '../types'
import { skillLabel, gameEmoji, timeAgo } from '../utils/helpers'
import { ALL_COUNTRIES } from '../utils/countries'
import ReportUserModal from '../components/ReportUserModal.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const friendStore = useFriendStore()
const chatStore = useChatStore()

const BANNER_PRESETS: Record<string, string> = {
  'banner-1': 'linear-gradient(135deg, #1a0a2e 0%, #3d1a78 50%, #1a0a2e 100%)',
  'banner-2': 'linear-gradient(135deg, #0a1a2e 0%, #0e4d6e 50%, #0a1a2e 100%)',
  'banner-3': 'linear-gradient(135deg, #2e0a0a 0%, #8b1a1a 50%, #2e0a0a 100%)',
  'banner-4': 'linear-gradient(135deg, #2e2a0a 0%, #8b7a1a 50%, #2e2a0a 100%)',
  'banner-5': 'linear-gradient(135deg, #0a2e1a 0%, #1a6e3d 50%, #0a2e1a 100%)',
}

const profile = ref<Profile | null>(null)
const favoriteGames = ref<Game[]>([])
const userStats = ref<UserStats | null>(null)
const loading = ref(false)
const error = ref('')
const addingFriend = ref(false)
const showReport = ref(false)
const showGamesCount = ref(6)

const bannerStyle = computed(() => {
  const key = profile.value?.bannerUrl
  if (key && BANNER_PRESETS[key]) {
    return { background: BANNER_PRESETS[key] }
  }
  return {}
})

const REGION_LABELS: Record<string, string> = {
  EUROPE:        '🌍 Європа',
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

function formatHours(hours: number): string {
  if (hours < 1) {
    const mins = Math.round(hours * 60)
    return mins + ' хв'
  }
  return hours.toFixed(1) + 'h'
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
computed(() =>
        profile.value && (
            profile.value.skillLevel || profile.value.playStyle ||
            profile.value.platforms?.length || profile.value.languages?.length
        )
);
const onlineFriendsCount = computed(() =>
    friendStore.friends.filter(f => f.status === 'ONLINE').length
)

const displayedFriends = computed(() =>
    [...friendStore.friends]
        .sort((a, b) => {
          if (a.status === 'ONLINE' && b.status !== 'ONLINE') return -1
          if (a.status !== 'ONLINE' && b.status === 'ONLINE') return 1
          return 0
        })
        .slice(0, 5)
)

const displayCountry = computed(() => {
  const c = profile.value?.country
  if (!c) return ''
  const found = ALL_COUNTRIES.find(x => x.en === c)
  return found ? found.native : c
})

const displayedGames = computed(() =>
    favoriteGames.value.slice(0, showGamesCount.value)
)

const statusClass = computed(() => {
  switch (profile.value?.status) {
    case 'ONLINE': return 'dot-online'
    case 'IN_LOBBY': return 'dot-lobby'
    case 'IN_GAME': return 'dot-ingame'
    default: return 'dot-offline'
  }
})

const statusText = computed(() => {
  switch (profile.value?.status) {
    case 'ONLINE': return '● Онлайн'
    case 'IN_LOBBY': return '● В лобі'
    case 'IN_GAME': return '● У грі'
    default:
      if (profile.value?.lastSeen) return '○ ' + timeAgo(profile.value.lastSeen)
      return '○ Офлайн'
  }
})

const memberSince = computed(() => {
  if (!profile.value?.createdAt) return ''
  const d = new Date(profile.value.createdAt)
  const months = ['січня','лютого','березня','квітня','травня','червня','липня','серпня','вересня','жовтня','листопада','грудня']
  return `Учасник з ${months[d.getMonth()]} ${d.getFullYear()}`
})

const comments = ref<ProfileComment[]>([])
const commentsLoading = ref(false)
const commentsPage = ref(0)
const commentsHasMore = ref(true)
const newComment = ref('')
const postingComment = ref(false)

async function fetchComments(reset = false) {
  if (!profile.value) return
  if (reset) {
    commentsPage.value = 0
    commentsHasMore.value = true
    comments.value = []
  }
  commentsLoading.value = true
  try {
    const { data } = await api.get(`/profile/${profile.value.userId}/comments`, {
      params: { page: commentsPage.value, size: 10 }
    })
    const page = data as any
    const content: ProfileComment[] = page.content
    if (reset) {
      comments.value = content
    } else {
      comments.value.push(...content)
    }
    commentsHasMore.value = !page.last
  } catch { }
  finally { commentsLoading.value = false }
}

async function postComment() {
  if (!newComment.value.trim() || !profile.value) return
  postingComment.value = true
  try {
    const { data } = await api.post<ProfileComment>(`/profile/${profile.value.userId}/comments`, {
      content: newComment.value.trim()
    })
    comments.value.unshift(data)
    newComment.value = ''
  } catch { }
  finally { postingComment.value = false }
}

async function deleteComment(commentId: number) {
  if (!profile.value) return
  try {
    await api.delete(`/profile/${profile.value.userId}/comments/${commentId}`)
    comments.value = comments.value.filter(c => c.id !== commentId)
  } catch { }
}

function loadMoreComments() {
  commentsPage.value++
  fetchComments()
}

async function fetchProfile(userId: string | string[]) {
  const id = Array.isArray(userId) ? userId[0] : userId
  loading.value = true
  error.value = ''
  showGamesCount.value = 6
  try {
    const { data } = await api.get<Profile>(`/profile/${id}`)
    profile.value = data
    fetchComments(true)
    try {
      const [gamesRes, statsRes] = await Promise.all([
        api.get<Game[]>(`/users/${id}/games`),
        api.get<UserStats>(`/profile/${id}/stats`),
      ])
      favoriteGames.value = gamesRes.data
      userStats.value = statsRes.data
    } catch {
      favoriteGames.value = []
      userStats.value = null
    }
  } catch {
    error.value = 'Не вдалося завантажити профіль'
    profile.value = null
    favoriteGames.value = []
    userStats.value = null
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

function goToFriendProfile(userId: number) {
  router.push(`/profile/${userId}`)
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
    <div v-if="loading" class="va-page">
      <div class="empty-state"><p>Завантаження профілю...</p></div>
    </div>

    <div v-else-if="error" class="va-page">
      <div class="empty-state">
        <div class="empty-icon">😕</div>
        <h3>{{ error }}</h3>
        <router-link to="/" class="action-btn">НА ГОЛОВНУ</router-link>
      </div>
    </div>

    <div v-else-if="profile" class="va-page">

      <div class="cinematic-banner" :class="{ 'has-banner': profile.bannerUrl }" :style="bannerStyle">
        <router-link v-if="isOwnProfile" :to="{ path: '/settings', query: { section: 'banner' } }" class="banner-edit-btn">✎ ЗМІНИТИ БАНЕР</router-link>
      </div>

      <div class="va-hero">
        <div class="va-ava-wrap">
          <img :src="resolveAvatar(profile.avatarUrl)" :alt="profile.displayName" class="va-ava-img" />
          <div class="online-dot" :class="statusClass"></div>
        </div>
        <div class="va-info">
          <div class="va-name">{{ profile.displayName }}</div>
          <div class="va-status-row">
            <span class="va-status-text" :class="statusClass">{{ statusText }}</span>
            <span v-if="memberSince" class="va-member-since">{{ memberSince }}</span>
          </div>
          <div v-if="profile.fullName || profile.country || profile.region" class="va-details">
            <div v-if="profile.fullName" class="va-detail-item">
              <span class="va-detail-icon">👤</span>
              <span class="va-detail-val">{{ profile.fullName }}</span>
            </div>
            <div v-if="profile.country" class="va-detail-item">
              <span class="va-detail-val">{{ displayCountry }}</span>
            </div>
            <div v-if="profile.region" class="va-detail-item">
              <span class="va-detail-val">{{ REGION_LABELS[profile.region] ?? profile.region }}</span>
            </div>
          </div>
          <div class="va-pills">
            <span v-if="profile.playStyle" class="va-pill hl"> {{ PLAY_STYLE_LABELS[profile.playStyle] ?? profile.playStyle }}</span>
            <span v-if="profile.skillLevel" class="va-pill hl">{{ skillLabel(profile.skillLevel) }}</span>
            <span v-for="p in profile.platforms" :key="p" class="va-pill">{{ PLATFORM_LABELS[p] ?? p }}</span>
            <span v-if="profile.languages?.length" class="va-pill">{{ profile.languages.join(' · ') }}</span>
          </div>
        </div>
        <div class="va-actions">
          <template v-if="isOwnProfile">
            <router-link to="/settings" class="va-btn p">✎ РЕДАГУВАТИ</router-link>
          </template>
          <template v-else-if="auth.isLoggedIn">
            <button class="va-btn p" @click="openDm">💬 НАПИСАТИ</button>
            <button v-if="isFriend" class="va-btn s" disabled>✓ ДРУЗІ</button>
            <button v-else-if="hasPendingRequest" class="va-btn s" disabled>⏳ ЗАПИТ НАДІСЛАНО</button>
            <button v-else class="va-btn s" :disabled="addingFriend" @click="sendFriendRequest">+ ДОДАТИ В ДРУЗІ</button>
            <button class="va-flag-btn" title="Поскаржитись" @click="showReport = true">🚩 скарга</button>
          </template>
        </div>
      </div>

      <ReportUserModal
        v-if="showReport && profile"
        :reported-user-id="profile.userId"
        :reported-user-name="profile.displayName"
        @close="showReport = false"
      />

      <div class="va-body">
        <div class="va-main">

          <div v-if="userStats && (userStats.completedGames || userStats.hoursPlayed)" class="va-panel">
            <div class="va-panel-title">ІГРОВА СТАТИСТИКА</div>
            <div class="va-stats-grid">
              <div class="va-stat">
                <div class="va-stat-val">{{ userStats.completedGames }}</div>
                <div class="va-stat-lbl">Завершено ігор</div>
              </div>
              <div class="va-stat">
                <div class="va-stat-val">{{ userStats.partiesCreated }}</div>
                <div class="va-stat-lbl">Лобі створено</div>
              </div>
              <div class="va-stat">
                <div class="va-stat-val">{{ userStats.partiesJoined }}</div>
                <div class="va-stat-lbl">Приєднань</div>
              </div>
              <div class="va-stat blue">
                <div class="va-stat-val">{{ formatHours(userStats.hoursPlayed) }}</div>
                <div class="va-stat-lbl">Годин у грі</div>
              </div>
            </div>
            <div
              v-if="userStats.favoriteGameName"
              class="va-fav-game"
              :class="{ 'va-fav-game--clickable': userStats.favoriteGameId }"
              @click="userStats.favoriteGameId && goToGame(userStats.favoriteGameId)"
              :style="userStats.favoriteGameId ? 'cursor:pointer' : ''"
            >
              <img
                v-if="userStats.favoriteGameImageUrl"
                :src="userStats.favoriteGameImageUrl"
                :alt="userStats.favoriteGameName"
                class="va-fav-img"
              />
              <div v-else class="va-fav-img">⭐</div>
              <div class="va-fav-info">
                <div class="va-fav-label">Улюблена гра</div>
                <div class="va-fav-name">{{ userStats.favoriteGameName }}</div>
              </div>
            </div>
          </div>


          <div v-if="favoriteGames.length" class="va-panel">
            <div class="va-panel-title">ОБРАНІ ІГРИ</div>
            <div class="va-games">
              <div
                  v-for="game in displayedGames"
                  :key="game.id"
                  class="va-game"
                  @click="goToGame(game.id)"
              >
                <img v-if="game.imageUrl" :src="game.imageUrl" :alt="game.name" class="va-game-cover-img" />
                <div v-else class="va-game-cover">{{ gameEmoji(game.genre) }}</div>
                <div class="va-game-name">{{ game.name }}</div>
              </div>
            </div>
            <button
              v-if="favoriteGames.length > showGamesCount"
              class="games-load-more"
              @click="showGamesCount += 6"
            >ПОКАЗАТИ ЩЕ</button>
            <div v-if="isOwnProfile" class="fav-games-add">
              <router-link to="/games" class="fav-add-link">+ Додати ще ігри</router-link>
            </div>
          </div>

          <div v-else-if="isOwnProfile" class="va-panel">
            <div class="va-panel-title">УЛЮБЛЕНІ ІГРИ</div>
            <div class="fav-games-empty">
              <span class="fav-empty-icon">🎮</span>
              <p>Ти ще не додав улюблених ігор</p>
              <router-link to="/favorite-games" class="fav-add-link">Перейти до каталогу ігор →</router-link>
            </div>
          </div>

          <div class="va-panel comments-panel">
            <div class="va-panel-title">
              КОМЕНТАРІ
              <span class="comments-count">{{ comments.length }}</span>
            </div>

            <div v-if="auth.isLoggedIn" class="comment-form">
              <img :src="resolveAvatar(auth.user?.avatarUrl ?? null)" alt="" class="comment-form-ava" />
              <div class="comment-input-wrap">
                <textarea
                  v-model="newComment"
                  class="comment-input"
                  placeholder="Написати коментар..."
                  maxlength="1000"
                  rows="2"
                ></textarea>
                <div class="comment-form-actions">
                  <span class="comment-char-count">{{ newComment.length }}/1000</span>
                  <button
                    class="comment-submit"
                    :disabled="!newComment.trim() || postingComment"
                    @click="postComment"
                  >{{ postingComment ? '...' : 'НАДІСЛАТИ' }}</button>
                </div>
              </div>
            </div>

            <div v-if="comments.length === 0 && !commentsLoading" class="comments-empty">
              <span>💬</span>
              <p>Коментарів поки немає. Будь першим!</p>
            </div>

            <div class="comments-list">
              <div v-for="c in comments" :key="c.id" class="comment-item">
                <img
                  :src="resolveAvatar(c.authorAvatarUrl)"
                  :alt="c.authorDisplayName"
                  class="comment-ava"
                  @click="$router.push(`/profile/${c.authorId}`)"
                />
                <div class="comment-body">
                  <div class="comment-header">
                    <router-link :to="`/profile/${c.authorId}`" class="comment-author">{{ c.authorDisplayName }}</router-link>
                    <span class="comment-time">{{ timeAgo(c.createdAt) }}</span>
                    <button
                      v-if="auth.user && (auth.user.id === c.authorId || (profile && auth.user.id === profile.userId))"
                      class="comment-delete"
                      @click="deleteComment(c.id)"
                      title="Видалити"
                    >✕</button>
                  </div>
                  <p class="comment-text">{{ c.content }}</p>
                </div>
              </div>
            </div>

            <div v-if="commentsLoading" class="comments-loading">Завантаження...</div>

            <button
              v-if="commentsHasMore && comments.length > 0 && !commentsLoading"
              class="comments-load-more"
              @click="loadMoreComments"
            >ПОКАЗАТИ ЩЕ</button>
          </div>

        </div>

        <div class="va-sidebar">

          <div v-if="isOwnProfile && friendStore.friends.length" class="va-side-panel">
            <div class="va-side-title">
              ДРУЗІ
              <span v-if="onlineFriendsCount" class="va-online-badge">{{ onlineFriendsCount }} онлайн</span>
            </div>
            <div class="va-friends">
              <div
                v-for="friend in displayedFriends"
                :key="friend.userId"
                class="va-friend"
                @click="goToFriendProfile(friend.userId)"
              >
                <div class="va-friend-ava-wrap">
                  <img :src="resolveAvatar(friend.avatarUrl)" :alt="friend.displayName" class="va-friend-ava-img" />
                  <div class="friend-dot" :class="friend.status === 'ONLINE' ? 'online' : 'offline'"></div>
                </div>
                <div class="va-friend-info">
                  <div class="va-friend-name">{{ friend.displayName }}</div>
                  <div class="va-friend-st" :class="{ on: friend.status === 'ONLINE' }">
                    <template v-if="friend.status === 'ONLINE'">● Online</template>
                    <template v-else-if="friend.lastSeen">○ {{ timeAgo(friend.lastSeen) }}</template>
                    <template v-else>○ Офлайн</template>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="friendStore.friends.length > 5" class="va-side-more">
              <router-link to="/friends" class="fav-add-link">Усі друзі →</router-link>
            </div>
          </div>

          <div v-if="profile.bio" class="va-side-panel">
            <div class="va-side-title">ПРО ГРАВЦЯ</div>
            <p class="va-bio-text">{{ profile.bio }}</p>
          </div>

          <div v-if="activeSocials.length" class="va-side-panel">
            <div class="va-side-title">СОЦІАЛЬНІ МЕРЕЖІ</div>
            <div class="side-social-list">
              <template v-for="s in activeSocials" :key="s.key">
                <a
                    v-if="!s.copyable && s.buildUrl(profile[s.key] as string)"
                    :href="s.buildUrl(profile[s.key] as string)"
                    target="_blank"
                    rel="noopener"
                    class="side-social-item"
                    :style="{ '--badge-color': s.color }"
                >
                  <span class="side-social-accent"></span>
                  <span class="side-social-label">{{ s.label }}</span>
                  <span class="side-social-val">{{ s.displayVal(profile[s.key] as string) }}</span>
                  <span class="side-social-arrow">↗</span>
                </a>
                <button
                    v-else
                    class="side-social-item side-social-copy"
                    :style="{ '--badge-color': s.color }"
                    @click="copyToClipboard(profile[s.key] as string, s.key as string)"
                >
                  <span class="side-social-accent"></span>
                  <span class="side-social-label">{{ s.label }}</span>
                  <span class="side-social-val">{{ s.displayVal(profile[s.key] as string) }}</span>
                  <span class="side-social-arrow">{{ copied === s.key ? '✓' : '⎘' }}</span>
                </button>
              </template>
            </div>
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

.va-page {
  max-width: 1320px;
  margin: 0 auto;
  padding: 0 24px;
}

.cinematic-banner {
  height: 200px;
  position: relative;
  overflow: hidden;
  border: 2px solid var(--border);
  border-bottom: none;
  background: var(--black);
  background-image: radial-gradient(circle, rgba(255,255,255,0.015) 1px, transparent 1px);
  background-size: 4px 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cinematic-banner.has-banner {
  background-image: none;
}
.cinematic-banner::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(245,197,24,0.03) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 100%, rgba(245,197,24,0.06) 0%, transparent 60%);
  pointer-events: none;
}
.cinematic-banner::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--yellow-dim), transparent);
  opacity: 0.6;
}
.banner-edit-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  background: rgba(10,10,11,0.8);
  border: 1px solid var(--border);
  color: var(--gray);
  font-size: 10px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  padding: 5px 12px;
  cursor: pointer;
  font-family: var(--font-body);
  font-weight: 600;
  transition: all 0.15s;
  text-decoration: none;
}
.banner-edit-btn:hover {
  color: var(--yellow);
  border-color: var(--yellow-dim);
}

.va-hero {
  background: var(--panel);
  border: 2px solid var(--border);
  border-top: none;
  padding: 28px 32px 32px;
  display: flex;
  align-items: flex-start;
  gap: 28px;
  position: relative;
  z-index: 5;
  margin-top: -2px;
}
.va-hero::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--yellow), rgba(245,197,24,0.3) 60%, transparent);
}

.va-ava-wrap {
  position: relative;
  flex-shrink: 0;
  margin-top: -80px;
  z-index: 20;
}
.va-ava-img {
  width: 240px;
  height: 240px;
  border-radius: 50%;
  object-fit: cover;
  border: 5px solid var(--panel);
  outline: 2px solid var(--yellow-dim);
  box-shadow: 0 -4px 20px rgba(0,0,0,0.5), 0 8px 36px rgba(0,0,0,0.9), 0 0 28px rgba(245,197,24,0.1);
}
.va-ava-wrap .online-dot {
  position: absolute;
  width: 26px;
  height: 26px;
  bottom: 16px;
  right: 16px;
  border-radius: 50%;
  border: 4px solid var(--panel);
  background: var(--gray);
  transition: background 0.2s, box-shadow 0.2s;
}
.va-ava-wrap .online-dot.dot-online {
  background: var(--green);
  box-shadow: 0 0 8px rgba(46,204,113,0.5);
}
.va-ava-wrap .online-dot.dot-lobby {
  background: var(--yellow);
  box-shadow: 0 0 8px rgba(245,197,24,0.5);
}
.va-ava-wrap .online-dot.dot-ingame {
  background: #5dade2;
  box-shadow: 0 0 8px rgba(93,173,226,0.5);
}
.va-ava-wrap .online-dot.dot-offline {
  background: var(--gray);
  box-shadow: none;
}

.va-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  padding-top: 8px;
  padding-bottom: 2px;
}
.va-name {
  font-family: var(--font-display);
  font-size: 52px;
  letter-spacing: 3px;
  color: var(--yellow);
  line-height: 1;
  margin-bottom: 8px;
}
.va-status-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.va-status-text {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
}
.va-status-text.dot-online { color: var(--green); }
.va-status-text.dot-lobby { color: var(--yellow); }
.va-status-text.dot-ingame { color: #5dade2; }
.va-status-text.dot-offline { color: var(--gray); }
.va-member-since {
  font-size: 11px;
  color: var(--gray);
  letter-spacing: 0.5px;
}
.va-details {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}
.va-detail-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px 5px 10px;
  background: var(--dark);
  border: 1px solid var(--border);
  transition: border-color 0.15s;
}
.va-detail-item:hover {
  border-color: var(--yellow-dim);
}
.va-detail-icon {
  font-size: 12px;
  flex-shrink: 0;
}
.va-detail-val {
  font-size: 12px;
  color: var(--gray-light);
  letter-spacing: 0.5px;
  font-weight: 500;
}
.va-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.va-pill {
  font-size: 10px;
  letter-spacing: 2px;
  text-transform: uppercase;
  padding: 4px 12px;
  border: 1px solid var(--border);
  color: var(--gray);
}
.va-pill.hl {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  background: var(--yellow-glow);
}
.va-pill.gr {
  border-color: rgba(46,204,113,0.35);
  color: var(--green);
  background: var(--green-dim);
}

.va-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 8px;
  flex-shrink: 0;
  align-self: flex-start;
}
.va-btn {
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 2px;
  padding: 9px 22px;
  border: 2px solid;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  background: transparent;
  text-decoration: none;
  text-align: center;
  color: inherit;
  display: inline-block;
}
.va-btn.p {
  border-color: var(--yellow);
  color: var(--yellow);
}
.va-btn.p:hover {
  background: var(--yellow);
  color: var(--black);
}
.va-btn.s {
  border-color: var(--border);
  color: var(--gray);
}
.va-btn.s:hover:not(:disabled) {
  border-color: var(--white);
  color: var(--white);
}
.va-btn:disabled {
  opacity: 0.7;
  cursor: default;
}
.va-flag-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: rgba(192,57,43,0.35);
  font-size: 13px;
  letter-spacing: 1px;
  padding: 4px 0;
  text-align: right;
  font-family: var(--font-body);
  font-weight: 600;
  transition: color 0.15s;
  align-self: flex-end;
  display: flex;
  align-items: center;
  gap: 5px;
}
.va-flag-btn:hover {
  color: var(--red);
}

.va-body {
  display: grid;
  grid-template-columns: 1fr 260px;
  gap: 20px;
  margin-top: 20px;
  padding-bottom: 60px;
}
.va-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.va-panel {
  background: var(--panel);
  border: 2px solid var(--border);
  padding: 28px 28px;
  position: relative;
  overflow: hidden;
  transition: border-color 0.2s;
}
.va-panel:hover {
  border-color: rgba(245,197,24,0.25);
}
.va-panel::after {
  content: '';
  position: absolute;
  inset: 3px;
  border: 1px solid rgba(245,197,24,0.04);
  pointer-events: none;
}
.va-panel-title {
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 3px;
  color: var(--yellow);
  margin-bottom: 22px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 10px;
}

.va-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.va-stat {
  background: var(--dark);
  border: 1px solid var(--border);
  padding: 28px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  transition: border-color 0.2s;
}
.va-stat:hover {
  border-color: var(--yellow-dim);
}
.va-stat.blue {
  border-color: rgba(93,173,226,0.3);
}
.va-stat.blue:hover {
  border-color: #5dade2;
}
.va-stat-val {
  font-family: var(--font-display);
  font-size: 30px;
  color: var(--yellow);
  letter-spacing: 2px;
  line-height: 1;
}
.va-stat.blue .va-stat-val {
  color: #5dade2;
}
.va-stat-lbl {
  font-size: 10px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--gray);
  text-align: center;
}

.va-fav-game {
  background: var(--dark);
  border: 1px solid rgba(245,197,24,0.2);
  padding: 18px 22px;
  display: flex;
  align-items: center;
  gap: 18px;
  transition: border-color 0.2s;
  cursor: default;
}
.va-fav-game--clickable {
  cursor: pointer;
}
.va-fav-game--clickable:hover {
  border-color: var(--yellow);
}
.va-fav-game:hover {
  border-color: var(--yellow-dim);
}
.va-fav-img {
  width: 50px;
  height: 68px;
  object-fit: cover;
  background: linear-gradient(135deg, #1a1a2e, #2d1b69);
  border: 2px solid var(--border);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}
.va-fav-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.va-fav-label {
  font-size: 10px;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: var(--gray);
}
.va-fav-name {
  font-family: var(--font-display);
  font-size: 20px;
  letter-spacing: 1px;
  color: var(--yellow);
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
  font-family: var(--font-display);
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

.va-games {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.va-game {
  background: var(--panel-light);
  border: 1px solid var(--border);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}
.va-game:hover {
  border-color: var(--yellow-dim);
  transform: translateY(-2px);
}
.va-game-cover {
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  background: linear-gradient(135deg, #1a1a2e, #0d1117);
}
.va-game-cover-img {
  width: 100%;
  height: 140px;
  object-fit: cover;
  display: block;
  transition: transform 0.3s;
}
.va-game:hover .va-game-cover-img {
  transform: scale(1.05);
}
.va-game-name {
  font-size: 12px;
  font-weight: 600;
  padding: 10px 12px;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.15s;
}
.va-game:hover .va-game-name {
  color: var(--yellow);
}

.games-load-more {
  display: block;
  width: 100%;
  margin-top: 14px;
  padding: 10px;
  background: none;
  border: 2px solid var(--border);
  color: var(--gray);
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.15s;
}
.games-load-more:hover {
  border-color: var(--yellow-dim);
  color: var(--yellow);
}

.fav-games-add {
  margin-top: 14px;
  text-align: center;
}
.fav-add-link {
  font-family: var(--font-display);
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

.va-sidebar {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.va-side-panel {
  background: var(--panel-light);
  border: 2px solid var(--border-glow);
  padding: 22px 22px;
  transition: border-color 0.2s;
}
.va-side-panel:hover {
  border-color: rgba(245,197,24,0.25);
}
.va-side-title {
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 2.5px;
  color: var(--yellow);
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--border-glow);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.va-online-badge {
  font-size: 10px;
  padding: 2px 7px;
  background: var(--green-dim);
  border: 1px solid rgba(46,204,113,0.35);
  color: var(--green);
  font-family: var(--font-body);
  font-weight: 600;
  letter-spacing: 1px;
}

.va-friends {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.va-friend {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 9px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
}
.va-friend:hover {
  background: var(--panel);
  border-color: var(--border);
}
.va-friend-ava-wrap {
  position: relative;
  flex-shrink: 0;
}
.va-friend-ava-img {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
  background: var(--dark);
  border: 2px solid var(--border-glow);
  display: block;
}
.friend-dot {
  position: absolute;
  bottom: -1px;
  right: -1px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
}
.friend-dot.online {
  background: var(--green);
  border: 2px solid var(--panel-light);
}
.friend-dot.offline {
  background: var(--gray);
  border: 2px solid var(--panel-light);
}
.va-friend-info {
  flex: 1;
  min-width: 0;
}
.va-friend-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--white);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.va-friend-st {
  font-size: 10px;
  color: var(--gray);
  letter-spacing: 0.5px;
}
.va-friend-st.on {
  color: var(--green);
}
.va-side-more {
  margin-top: 10px;
  text-align: center;
}

.va-bio-text {
  font-size: 13px;
  color: var(--gray-light);
  line-height: 1.6;
  font-style: italic;
  border-left: 2px solid var(--yellow-dim);
  padding-left: 10px;
  white-space: pre-wrap;
}

.tag-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.chip {
  font-size: 12px;
  padding: 4px 10px;
  background: var(--panel);
  border: 1px solid var(--border);
  color: var(--gray-light);
}
.lang-chip {
  display: flex;
  align-items: center;
  gap: 6px;
}
.lang-code {
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 1px;
  color: var(--yellow);
  opacity: 0.8;
}
.lang-name-text {
  font-size: 12px;
  color: var(--gray-light);
}

.side-social-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.side-social-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: var(--dark);
  border: 1px solid var(--border);
  text-decoration: none;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
  overflow: hidden;
}
.side-social-item:hover {
  border-color: var(--badge-color, var(--yellow-dim));
  background: rgba(245, 197, 24, 0.03);
}
.side-social-accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--badge-color, var(--yellow));
  opacity: 0.5;
  transition: opacity 0.15s;
}
.side-social-item:hover .side-social-accent {
  opacity: 1;
}
.side-social-label {
  font-family: var(--font-display);
  font-size: 9px;
  letter-spacing: 1.5px;
  color: var(--badge-color, var(--yellow));
  text-transform: uppercase;
  min-width: 55px;
  flex-shrink: 0;
}
.side-social-val {
  font-size: 12px;
  color: var(--gray-light);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
}
.side-social-item:hover .side-social-val {
  color: var(--white);
}
.side-social-arrow {
  font-size: 11px;
  color: var(--gray);
  flex-shrink: 0;
  transition: color 0.15s;
}
.side-social-item:hover .side-social-arrow {
  color: var(--badge-color, var(--yellow));
}
.side-social-copy {
  border: 1px solid var(--border);
  width: 100%;
  text-align: left;
  font-family: inherit;
}

.comments-panel .va-panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
}
.comments-count {
  font-family: var(--font-body);
  font-size: 11px;
  color: var(--gray);
  font-weight: 600;
  padding: 1px 8px;
  background: var(--dark);
  border: 1px solid var(--border);
  letter-spacing: 0;
}

.comment-form {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--border);
}
.comment-form-ava {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
  flex-shrink: 0;
  margin-top: 2px;
}
.comment-input-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.comment-input {
  width: 100%;
  background: var(--dark);
  border: 2px solid var(--border);
  padding: 10px 14px;
  font-size: 13px;
  color: var(--white);
  font-family: var(--font-body);
  resize: vertical;
  min-height: 50px;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}
.comment-input:focus {
  border-color: var(--yellow-dim);
}
.comment-input::placeholder {
  color: var(--gray);
}
.comment-form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.comment-char-count {
  font-size: 10px;
  color: var(--gray);
  letter-spacing: 0.5px;
}
.comment-submit {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 2px;
  padding: 7px 20px;
  background: var(--yellow);
  border: 2px solid var(--yellow);
  color: var(--black);
  cursor: pointer;
  transition: all 0.15s;
}
.comment-submit:hover:not(:disabled) {
  box-shadow: 0 0 16px rgba(245,197,24,0.3);
}
.comment-submit:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.comments-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 28px 0;
  color: var(--gray);
  font-size: 13px;
}
.comments-empty span {
  font-size: 28px;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.comment-item {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(255,255,255,0.03);
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-ava {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
  flex-shrink: 0;
  cursor: pointer;
  transition: border-color 0.15s;
}
.comment-ava:hover {
  border-color: var(--yellow-dim);
}
.comment-body {
  flex: 1;
  min-width: 0;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.comment-author {
  font-size: 12px;
  font-weight: 700;
  color: var(--yellow);
  text-decoration: none;
  letter-spacing: 0.5px;
  transition: color 0.15s;
}
.comment-author:hover {
  color: var(--white);
}
.comment-time {
  font-size: 10px;
  color: var(--gray);
  letter-spacing: 0.5px;
}
.comment-delete {
  margin-left: auto;
  background: none;
  border: none;
  color: var(--gray);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 6px;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s;
}
.comment-item:hover .comment-delete {
  opacity: 1;
}
.comment-delete:hover {
  color: var(--red);
}
.comment-text {
  font-size: 13px;
  color: var(--gray-light);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.comments-loading {
  text-align: center;
  padding: 16px 0;
  color: var(--gray);
  font-size: 12px;
  letter-spacing: 1px;
}

.comments-load-more {
  display: block;
  width: 100%;
  margin-top: 12px;
  padding: 10px;
  background: none;
  border: 2px solid var(--border);
  color: var(--gray);
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.15s;
}
.comments-load-more:hover {
  border-color: var(--yellow-dim);
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
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 2px;
  color: var(--yellow);
  margin-bottom: 8px;
}
.action-btn {
  font-family: var(--font-display);
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

@media (max-width: 768px) {
  .va-page {
    margin: 0 8px;
  }

  .cinematic-banner {
    height: 130px;
  }

  .va-hero {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 20px 16px 24px;
  }

  .va-ava-wrap {
    margin-top: -80px;
  }

  .va-ava-img {
    width: 130px;
    height: 130px;
  }

  .va-ava-wrap .online-dot {
    width: 20px;
    height: 20px;
    bottom: 6px;
    right: 6px;
    border-width: 3px;
  }

  .va-name {
    font-size: 30px;
  }

  .va-status-row {
    justify-content: center;
  }

  .va-details {
    justify-content: center;
  }

  .va-info {
    align-items: center;
    min-height: auto;
  }

  .va-pills {
    justify-content: center;
  }

  .va-actions {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    width: 100%;
  }

  .va-action-btn {
    font-size: 11px;
    padding: 7px 16px;
    letter-spacing: 1.5px;
  }

  .va-flag-btn {
    align-self: center;
  }

  .va-body {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .va-panel {
    padding: 20px 18px;
  }

  .va-stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .va-stat {
    padding: 20px 10px;
  }

  .va-stat-val {
    font-size: 22px;
  }

  .va-stat-lbl {
    font-size: 9px;
    letter-spacing: 1.5px;
  }

  .va-games {
    grid-template-columns: repeat(2, 1fr);
  }

  .va-game-cover, .va-game-cover-img {
    height: 100px;
  }

  .va-fav-game {
    padding: 10px;
  }

  .va-game-name {
    font-size: 10px;
  }

  .social-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .va-sidebar {
    order: -1;
  }

  .va-side-panel {
    padding: 18px 16px;
  }

  .va-bio-text {
    font-size: 13px;
  }

  .banner-edit-btn {
    font-size: 9px;
    padding: 4px 10px;
  }

  .comment-form-ava {
    display: none;
  }
}

@media (max-width: 480px) {
  .cinematic-banner {
    height: 90px;
  }

  .va-ava-wrap {
    margin-top: -55px;
  }

  .va-ava-img {
    width: 100px;
    height: 100px;
    border-width: 3px;
  }

  .va-ava-wrap .online-dot {
    width: 16px;
    height: 16px;
    bottom: 4px;
    right: 4px;
    border-width: 2px;
  }

  .va-name {
    font-size: 24px;
    letter-spacing: 2px;
  }

  .va-status-row {
    flex-direction: column;
    gap: 2px;
  }

  .va-details {
    flex-direction: column;
    align-items: center;
  }

  .va-detail-item {
    padding: 4px 10px;
  }

  .va-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .va-flag-btn {
    align-self: center;
  }

  .social-grid {
    grid-template-columns: 1fr;
  }

  .va-games {
    grid-template-columns: 1fr;
  }

  .va-game-cover, .va-game-cover-img {
    height: 120px;
  }

  .va-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .va-panel {
    padding: 20px 14px;
  }

  .va-panel-title {
    font-size: 12px;
    letter-spacing: 2px;
  }

  .comment-submit {
    font-size: 10px;
    padding: 6px 14px;
  }
}
</style>
