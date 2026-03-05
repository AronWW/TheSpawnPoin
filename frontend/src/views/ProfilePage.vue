<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useFriendStore } from '../stores/friends'
import { useChatStore } from '../stores/chat'
import api from '../api/axios'
import type { Profile } from '../types'
import { skillLabel } from '../utils/helpers'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const friendStore = useFriendStore()
const chatStore = useChatStore()

const profile = ref<Profile | null>(null)
const loading = ref(false)
const error = ref('')
const addingFriend = ref(false)

const avatarBase = 'http://localhost:8080'

function resolveAvatar(url: string | null): string {
  if (!url) return avatarBase + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return avatarBase + url
}

const isOwnProfile = computed(() => {
  return auth.user && profile.value && auth.user.id === profile.value.userId
})

const isFriend = computed(() => {
  if (!profile.value) return false
  return friendStore.friends.some((f) => f.userId === profile.value!.userId)
})

const hasPendingRequest = computed(() => {
  if (!profile.value) return false
  return friendStore.outgoingRequests.some((r) => r.receiverId === profile.value!.userId)
    || friendStore.incomingRequests.some((r) => r.senderId === profile.value!.userId)
})

async function fetchProfile(userId: string | string[]) {
  const id = Array.isArray(userId) ? userId[0] : userId
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get<Profile>(`/profile/${id}`)
    profile.value = data
  } catch {
    error.value = 'Не вдалося завантажити профіль'
    profile.value = null
  } finally {
    loading.value = false
  }
}

async function sendFriendRequest() {
  if (!profile.value) return
  addingFriend.value = true
  try {
    await friendStore.sendRequest(profile.value.userId)
  } catch {  }
  finally { addingFriend.value = false }
}

async function openDm() {
  if (!profile.value?.email) return
  await chatStore.openDm(profile.value.email)
  router.push('/chat')
}

onMounted(async () => {
  if (route.params.userId) {
    fetchProfile(route.params.userId)
  }
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
      <div v-if="loading" class="empty-state">
        <p>Завантаження профілю...</p>
      </div>

      <div v-else-if="error" class="empty-state">
        <div class="empty-icon">😕</div>
        <h3>{{ error }}</h3>
        <router-link to="/" class="action-btn">НА ГОЛОВНУ</router-link>
      </div>

      <div v-else-if="profile" class="profile-content">
        <div class="profile-header ink-panel">
          <div class="profile-avatar-wrapper">
            <img :src="resolveAvatar(profile.avatarUrl)" :alt="profile.displayName" class="profile-avatar" />
          </div>
          <div class="profile-header-info">
            <h1 class="profile-display-name">{{ profile.displayName }}</h1>
            <div v-if="profile.fullName" class="profile-full-name">{{ profile.fullName }}</div>
            <div v-if="profile.country" class="profile-location">
              📍 {{ profile.country }}<span v-if="profile.region">, {{ profile.region }}</span>
            </div>
            <div v-if="isOwnProfile" class="profile-edit-hint">
              <router-link to="/settings" class="edit-link">✏️ Редагувати профіль</router-link>
            </div>
            <div v-else-if="auth.isLoggedIn" class="profile-actions">
              <button class="profile-action-btn" @click="openDm">💬 Написати</button>
              <button
                v-if="isFriend"
                class="profile-action-btn friend-badge-btn"
                disabled
              >✓ Друзі</button>
              <button
                v-else-if="hasPendingRequest"
                class="profile-action-btn pending-badge-btn"
                disabled
              >⏳ Запит надіслано</button>
              <button
                v-else
                class="profile-action-btn add-friend-btn"
                :disabled="addingFriend"
                @click="sendFriendRequest"
              >+ Додати в друзі</button>
            </div>
          </div>
        </div>

        <div v-if="profile.bio" class="profile-section ink-panel">
          <h2 class="section-label">ПРО ГРАВЦЯ</h2>
          <p class="profile-bio">{{ profile.bio }}</p>
        </div>

        <div class="profile-section ink-panel">
          <h2 class="section-label">ІГРОВИЙ ПРОФІЛЬ</h2>
          <div class="profile-tags-grid">
            <div v-if="profile.skillLevel" class="profile-tag-group">
              <span class="tag-label">Рівень</span>
              <span class="tag-value">{{ skillLabel(profile.skillLevel) }}</span>
            </div>
            <div v-if="profile.playStyle" class="profile-tag-group">
              <span class="tag-label">Стиль гри</span>
              <span class="tag-value">{{ profile.playStyle }}</span>
            </div>
            <div v-if="profile.platforms && profile.platforms.length" class="profile-tag-group">
              <span class="tag-label">Платформи</span>
              <div class="tag-chips">
                <span v-for="p in profile.platforms" :key="p" class="chip">{{ p }}</span>
              </div>
            </div>
            <div v-if="profile.languages && profile.languages.length" class="profile-tag-group">
              <span class="tag-label">Мови</span>
              <div class="tag-chips">
                <span v-for="lang in profile.languages" :key="lang" class="chip">{{ lang }}</span>
              </div>
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
  align-items: center;
}

.profile-avatar-wrapper {
  flex-shrink: 0;
}

.profile-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--yellow-dim);
  box-shadow: 0 0 20px rgba(245, 197, 24, 0.15);
}

.profile-header-info {
  flex: 1;
}

.profile-display-name {
  font-family: var(--font-display);
  font-size: 36px;
  letter-spacing: 2px;
  color: var(--yellow);
  line-height: 1;
  margin-bottom: 4px;
}

.profile-full-name {
  font-size: 15px;
  color: var(--gray-light);
  margin-bottom: 4px;
}

.profile-location {
  font-size: 13px;
  color: var(--gray);
}

.edit-link {
  font-size: 12px;
  color: var(--gray);
  letter-spacing: 1px;
  text-transform: uppercase;
  transition: color 0.15s;
  margin-top: 8px;
  display: inline-block;
}
.edit-link:hover {
  color: var(--yellow);
}

.profile-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.profile-action-btn {
  font-family: var(--font-body);
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

.add-friend-btn {
  border-color: var(--yellow-dim);
  color: var(--yellow);
}

.friend-badge-btn {
  border-color: rgba(46, 204, 113, 0.3);
  color: #2ecc71;
  background: rgba(46, 204, 113, 0.08);
}

.pending-badge-btn {
  color: var(--gray);
}

.profile-section {
  padding: 24px 28px;
}

.section-label {
  font-family: var(--font-display);
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
  gap: 4px;
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
  padding: 3px 10px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  color: var(--gray-light);
  letter-spacing: 0.5px;
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

@media (max-width: 600px) {
  .profile-header {
    flex-direction: column;
    text-align: center;
  }
  .profile-tags-grid {
    grid-template-columns: 1fr;
  }
}
</style>




