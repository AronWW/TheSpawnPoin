<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useFriendStore } from '../stores/friends'
import { useChatStore } from '../stores/chat'
import { timeAgo } from '../utils/helpers'
import { API_BASE_URL } from '../config'
import api from '../api/axios'

const router = useRouter()
const auth = useAuthStore()
const friendStore = useFriendStore()
const chatStore = useChatStore()

type Tab = 'friends' | 'incoming' | 'outgoing' | 'search'
const activeTab = ref<Tab>('friends')

function resolveAvatar(url: string | null): string {
  if (!url) return API_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return API_BASE_URL + url
}

onMounted(async () => {
  if (!auth.isLoggedIn) {
    router.push('/login')
    return
  }
  await Promise.all([
    friendStore.fetchFriends(),
    friendStore.fetchIncomingRequests(),
    friendStore.fetchOutgoingRequests(),
  ])
})

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    ONLINE: 'Онлайн',
    OFFLINE: 'Офлайн',
    IN_GAME: 'У грі',
  }
  return map[status] ?? status
}

function statusClass(status: string): string {
  return status?.toLowerCase() ?? 'offline'
}

interface SearchResult {
  id: number
  email: string
  displayName: string
  avatarUrl: string | null
  status: string
  lastSeen: string | null
}

const searchQuery = ref('')
const searchResults = ref<SearchResult[]>([])
const searchLoading = ref(false)
const searchError = ref('')
let searchDebounce: ReturnType<typeof setTimeout> | null = null

function onSearchInput() {
  if (searchDebounce) clearTimeout(searchDebounce)
  searchError.value = ''
  const q = searchQuery.value.trim()
  if (q.length < 2) {
    searchResults.value = []
    return
  }
  searchDebounce = setTimeout(() => doSearch(q), 400)
}

async function doSearch(q: string) {
  searchLoading.value = true
  try {
    const { data } = await api.get<SearchResult[]>('/users/search', { params: { q } })
    searchResults.value = data
  } catch {
    searchError.value = 'Помилка пошуку'
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

function isFriend(userId: number): boolean {
  return friendStore.friends.some((f) => f.userId === userId)
}

function hasPendingRequest(userId: number): boolean {
  return friendStore.outgoingRequests.some((r) => r.receiverId === userId)
      || friendStore.incomingRequests.some((r) => r.senderId === userId)
}

async function addFriend(userId: number) {
  try {
    await friendStore.sendRequest(userId)
  } catch {  }
}

async function openDm(email: string) {
  await chatStore.openDm(email)
  router.push('/chat')
}
</script>

<template>
  <div class="friends-page">
    <div class="friends-container">
      <div class="section-head">
        <div class="section-title">ДРУЗІ</div>
      </div>

      <div class="friends-tabs">
        <button
            class="friends-tab"
            :class="{ active: activeTab === 'friends' }"
            @click="activeTab = 'friends'"
        >
          Мої друзі
          <span class="tab-count">{{ friendStore.friendCount }}</span>
        </button>
        <button
            class="friends-tab"
            :class="{ active: activeTab === 'incoming' }"
            @click="activeTab = 'incoming'"
        >
          Вхідні запити
          <span v-if="friendStore.pendingCount > 0" class="tab-count pending">{{ friendStore.pendingCount }}</span>
        </button>
        <button
            class="friends-tab"
            :class="{ active: activeTab === 'outgoing' }"
            @click="activeTab = 'outgoing'"
        >
          Надіслані
          <span class="tab-count">{{ friendStore.outgoingRequests.length }}</span>
        </button>
        <button
            class="friends-tab"
            :class="{ active: activeTab === 'search' }"
            @click="activeTab = 'search'"
        >
          🔍 Знайти гравців
        </button>
      </div>

      <div v-if="activeTab === 'friends'" class="friends-list">
        <div v-if="friendStore.loading" class="empty-state">
          <p>Завантаження...</p>
        </div>
        <div v-else-if="friendStore.friends.length === 0" class="empty-state">
          <div class="empty-icon">👥</div>
          <h3>Друзів поки немає</h3>
          <p>Знайди гравців у лобі та надішли запит у друзі!</p>
        </div>
        <div v-else class="friend-cards">
          <div v-for="friend in friendStore.friends" :key="friend.userId" class="friend-card ink-panel">
            <router-link :to="'/profile/' + friend.userId" class="friend-avatar-link">
              <img :src="resolveAvatar(friend.avatarUrl)" :alt="friend.displayName" class="friend-avatar" />
              <span class="status-dot" :class="statusClass(friend.status)"></span>
            </router-link>
            <div class="friend-info">
              <router-link :to="'/profile/' + friend.userId" class="friend-name">{{ friend.displayName }}</router-link>
              <div class="friend-status">{{ statusLabel(friend.status) }}</div>
              <div class="friend-since">Друзі з {{ timeAgo(friend.friendsSince) }}</div>
            </div>
            <div class="friend-actions">
              <button class="btn-message" @click="openDm(friend.email)" title="Написати">💬</button>
              <button class="btn-danger" @click="friendStore.unfriend(friend.userId)" title="Видалити з друзів">✕</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'incoming'" class="friends-list">
        <div v-if="friendStore.incomingRequests.length === 0" class="empty-state">
          <div class="empty-icon">📬</div>
          <h3>Немає вхідних запитів</h3>
        </div>
        <div v-else class="friend-cards">
          <div v-for="req in friendStore.incomingRequests" :key="req.inviteId" class="friend-card ink-panel">
            <router-link :to="'/profile/' + req.senderId" class="friend-avatar-link">
              <img :src="resolveAvatar(req.senderAvatarUrl)" :alt="req.senderDisplayName" class="friend-avatar" />
            </router-link>
            <div class="friend-info">
              <router-link :to="'/profile/' + req.senderId" class="friend-name">{{ req.senderDisplayName }}</router-link>
              <div class="friend-since">{{ timeAgo(req.createdAt) }}</div>
            </div>
            <div class="friend-actions">
              <button class="btn-accept" @click="friendStore.acceptRequest(req.inviteId)">✓ Прийняти</button>
              <button class="btn-danger" @click="friendStore.declineRequest(req.inviteId)">✕</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'outgoing'" class="friends-list">
        <div v-if="friendStore.outgoingRequests.length === 0" class="empty-state">
          <div class="empty-icon">📤</div>
          <h3>Немає надісланих запитів</h3>
        </div>
        <div v-else class="friend-cards">
          <div v-for="req in friendStore.outgoingRequests" :key="req.inviteId" class="friend-card ink-panel">
            <router-link :to="'/profile/' + req.receiverId" class="friend-avatar-link">
              <img :src="resolveAvatar(req.receiverAvatarUrl)" :alt="req.receiverDisplayName" class="friend-avatar" />
            </router-link>
            <div class="friend-info">
              <router-link :to="'/profile/' + req.receiverId" class="friend-name">{{ req.receiverDisplayName }}</router-link>
              <div class="friend-since">{{ timeAgo(req.createdAt) }}</div>
            </div>
            <div class="friend-actions">
              <button class="btn-danger" @click="friendStore.cancelRequest(req.inviteId)">Скасувати</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'search'" class="friends-list">
        <div class="search-bar">
          <input
              v-model="searchQuery"
              @input="onSearchInput"
              type="text"
              class="search-input"
              placeholder="Пошук за іменем або email..."
          />
        </div>

        <div v-if="searchLoading" class="empty-state" style="padding: 30px;">
          <p>Пошук...</p>
        </div>
        <div v-else-if="searchError" class="empty-state" style="padding: 30px;">
          <p style="color: var(--red);">{{ searchError }}</p>
        </div>
        <div v-else-if="searchQuery.trim().length >= 2 && searchResults.length === 0" class="empty-state" style="padding: 30px;">
          <div class="empty-icon">🔍</div>
          <h3>Нікого не знайдено</h3>
          <p>Спробуйте інший запит</p>
        </div>
        <div v-else-if="searchQuery.trim().length < 2 && searchResults.length === 0" class="empty-state" style="padding: 30px;">
          <div class="empty-icon">🔍</div>
          <h3>Знайти гравців</h3>
          <p>Введіть мінімум 2 символи для пошуку</p>
        </div>
        <div v-else class="friend-cards">
          <div v-for="user in searchResults" :key="user.id" class="friend-card ink-panel">
            <router-link :to="'/profile/' + user.id" class="friend-avatar-link">
              <img :src="resolveAvatar(user.avatarUrl)" :alt="user.displayName" class="friend-avatar" />
            </router-link>
            <div class="friend-info">
              <router-link :to="'/profile/' + user.id" class="friend-name">{{ user.displayName }}</router-link>
              <div class="friend-status">{{ statusLabel(user.status) }}</div>
            </div>
            <div class="friend-actions">
              <button class="btn-message" @click="openDm(user.email)" title="Написати повідомлення">💬</button>
              <template v-if="isFriend(user.id)">
                <span class="friend-badge">✓ Друзі</span>
              </template>
              <template v-else-if="hasPendingRequest(user.id)">
                <span class="pending-badge">⏳ Запит</span>
              </template>
              <template v-else>
                <button class="btn-accept" @click="addFriend(user.id)">+ Додати</button>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.friends-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}

.friends-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 32px;
}


.friends-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  border-bottom: 2px solid var(--border);
  padding-bottom: 0;
}

.friends-tab {
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: var(--gray-light);
  padding: 10px 20px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: color 0.15s, border-color 0.15s;
  display: flex;
  align-items: center;
  gap: 8px;
}
.friends-tab:hover {
  color: var(--yellow);
}
.friends-tab.active {
  color: var(--yellow);
  border-bottom-color: var(--yellow);
}

.tab-count {
  font-size: 11px;
  background: var(--panel-light);
  padding: 1px 6px;
  border-radius: 2px;
  color: var(--gray);
}
.tab-count.pending {
  background: var(--red);
  color: #fff;
}

.friend-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.friend-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
}

.friend-avatar-link {
  position: relative;
  flex-shrink: 0;
}

.friend-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
  transition: border-color 0.15s;
}
.friend-avatar-link:hover .friend-avatar {
  border-color: var(--yellow);
}

.status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid var(--panel);
}
.status-dot.online { background: #27ae60; }
.status-dot.offline { background: var(--gray); }
.status-dot.in_game { background: var(--yellow); }

.friend-info {
  flex: 1;
  min-width: 0;
}

.friend-name {
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 15px;
  color: var(--white);
  letter-spacing: 0.5px;
  transition: color 0.15s;
}
a.friend-name:hover {
  color: var(--yellow);
}

.friend-status {
  font-size: 12px;
  color: var(--gray);
  letter-spacing: 0.5px;
}

.friend-since {
  font-size: 11px;
  color: var(--gray);
  margin-top: 2px;
}

.friend-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.btn-accept {
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 12px;
  letter-spacing: 1px;
  padding: 6px 14px;
  background: var(--yellow);
  color: var(--black);
  border: none;
  text-transform: uppercase;
  transition: background 0.15s;
}
.btn-accept:hover {
  background: var(--yellow-dim);
}

.btn-danger {
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 12px;
  padding: 6px 12px;
  background: none;
  border: 2px solid var(--red-dim);
  color: var(--red);
  transition: background 0.15s, color 0.15s;
}
.btn-danger:hover {
  background: var(--red);
  color: #fff;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
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

.empty-state p {
  color: var(--gray);
  font-size: 14px;
}

.search-bar {
  margin-bottom: 16px;
}

.search-input {
  width: 100%;
  background: var(--dark);
  border: 2px solid var(--border);
  padding: 12px 16px;
  font-size: 14px;
  color: var(--white);
  font-family: var(--font-body);
  letter-spacing: 0.5px;
  outline: none;
  transition: border-color 0.15s;
}
.search-input:focus {
  border-color: var(--yellow-dim);
}
.search-input::placeholder {
  color: var(--gray);
}


.btn-message {
  font-size: 18px;
  padding: 4px 10px;
  background: none;
  border: 2px solid var(--border);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
  line-height: 1;
}
.btn-message:hover {
  border-color: var(--yellow-dim);
  background: var(--yellow-glow);
}

.friend-badge {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #2ecc71;
  padding: 4px 10px;
  border: 1px solid rgba(46, 204, 113, 0.3);
  background: rgba(46, 204, 113, 0.08);
}

.pending-badge {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--gray);
  padding: 4px 10px;
  border: 1px solid var(--border);
}
</style>