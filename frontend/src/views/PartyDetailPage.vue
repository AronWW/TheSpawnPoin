<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { usePartyStore } from '../stores/parties'
import type { Party } from '../types'
import { skillLabel, timeAgo, gameEmoji } from '../utils/helpers'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const partyStore = usePartyStore()

const party = ref<Party | null>(null)
const loading = ref(false)
const error = ref('')
const actionLoading = ref(false)
const actionError = ref('')

const avatarBase = 'http://localhost:8080'

function resolveAvatar(url: string | null): string {
  if (!url) return avatarBase + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return avatarBase + url
}

const isCreator = computed(() =>
  auth.user && party.value && auth.user.id === party.value.creatorId
)

const isMember = computed(() => {
  if (!auth.user || !party.value?.members) return false
  return party.value.members.some((m) => m.userId === auth.user!.id)
})

const canJoin = computed(() =>
  auth.isLoggedIn &&
  party.value?.isOpen &&
  !isMember.value &&
  party.value.currentMembers < party.value.maxMembers
)

const isFull = computed(() =>
  party.value ? party.value.currentMembers >= party.value.maxMembers : false
)

async function loadParty() {
  const id = route.params.id as string
  loading.value = true
  error.value = ''
  try {
    party.value = await partyStore.fetchParty(Number(id))
  } catch {
    error.value = 'Не вдалося завантажити лобі'
    party.value = null
  } finally {
    loading.value = false
  }
}

async function handleJoin() {
  if (!party.value) return
  actionLoading.value = true
  actionError.value = ''
  try {
    party.value = await partyStore.joinParty(party.value.id)
  } catch (e: any) {
    actionError.value = e.message || 'Помилка'
  } finally {
    actionLoading.value = false
  }
}

async function handleLeave() {
  if (!party.value) return
  actionLoading.value = true
  actionError.value = ''
  try {
    await partyStore.leaveParty(party.value.id)
    router.push('/my-parties')
  } catch (e: any) {
    actionError.value = e.message || 'Помилка'
  } finally {
    actionLoading.value = false
  }
}

async function handleClose() {
  if (!party.value) return
  actionLoading.value = true
  actionError.value = ''
  try {
    await partyStore.closeParty(party.value.id)
    await loadParty()
  } catch (e: any) {
    actionError.value = e.message || 'Помилка'
  } finally {
    actionLoading.value = false
  }
}

function goToChat() {
  if (party.value?.chatId) {
    router.push(`/chat?groupId=${party.value.chatId}`)
  }
}

function playStyleLabel(style: string | null): string {
  if (!style) return ''
  const map: Record<string, string> = {
    CASUAL: 'Casual',
    SEMI_COMPETITIVE: 'Semi-competitive',
    COMPETITIVE: 'Competitive',
  }
  return map[style] ?? style
}

onMounted(loadParty)

watch(() => route.params.id, () => {
  loadParty()
})
</script>

<template>
  <div class="party-detail-page">
    <div class="party-detail-container">
      <div v-if="loading" class="empty-state">
        <div class="empty-icon">⏳</div>
        <p>Завантаження лобі...</p>
      </div>

      <div v-else-if="error" class="empty-state">
        <div class="empty-icon">😕</div>
        <h3>{{ error }}</h3>
        <router-link to="/" class="action-btn">НА ГОЛОВНУ</router-link>
      </div>

      <div v-else-if="party" class="party-detail">
        <button class="back-link" @click="router.back()">← Назад</button>

        <div class="detail-header ink-panel">
          <div class="detail-header-top">
            <div class="detail-game-cover">
              <img v-if="party.gameImageUrl" :src="party.gameImageUrl" :alt="party.gameName" />
              <div v-else class="cover-placeholder">{{ gameEmoji(party.gameName) }}</div>
            </div>
            <div class="detail-header-info">
              <h1 class="detail-game-name">{{ party.gameName }}</h1>
              <div class="detail-host">
                Хост:
                <router-link :to="'/profile/' + party.creatorId" class="host-link">
                  <img :src="resolveAvatar(party.creatorAvatarUrl)" :alt="party.creatorDisplayName" class="host-avatar" />
                  {{ party.creatorDisplayName }}
                </router-link>
              </div>
              <div class="detail-time">{{ timeAgo(party.createdAt) }}</div>
            </div>
            <div class="detail-status">
              <span class="status-badge" :class="party.isOpen ? 'open' : 'closed'">
                {{ party.isOpen ? 'ВІДКРИТО' : 'ЗАКРИТО' }}
              </span>
            </div>
          </div>

          <div v-if="party.description" class="detail-description">
            <p>{{ party.description }}</p>
          </div>

          <div class="detail-tags">
            <span v-for="p in party.platform" :key="p" class="platform-tag">{{ p }}</span>
            <span v-if="party.language" class="tag">{{ party.language }}</span>
            <span v-if="party.skillLevel" class="skill-badge" :class="party.skillLevel.toLowerCase()">
              {{ skillLabel(party.skillLevel) }}
            </span>
            <span v-if="party.playStyle" class="tag">{{ playStyleLabel(party.playStyle) }}</span>
          </div>
        </div>

        <div class="detail-section ink-panel">
          <h2 class="section-label">
            ГРАВЦІ
            <span class="member-count">
              <span>{{ party.currentMembers }}</span>/{{ party.maxMembers }}
            </span>
          </h2>

          <div class="slots-visual">
            <div v-for="i in party.maxMembers" :key="i" class="member-slot" :class="{ filled: i <= party.currentMembers }">
              <template v-if="i <= party.currentMembers && party.members && party.members[i - 1]">
                <router-link :to="'/profile/' + party.members[i - 1].userId" class="member-card">
                  <img :src="resolveAvatar(party.members[i - 1].avatarUrl)" :alt="party.members[i - 1].displayName" class="member-avatar" />
                  <div class="member-info">
                    <span class="member-name">{{ party.members[i - 1].displayName }}</span>
                    <span v-if="party.members[i - 1].isCreator" class="creator-tag">ХОСТ</span>
                  </div>
                </router-link>
              </template>
              <template v-else>
                <div class="empty-slot-content">
                  <span class="empty-slot-icon">+</span>
                  <span class="empty-slot-text">Вільне місце</span>
                </div>
              </template>
            </div>
          </div>
        </div>

        <div v-if="actionError" class="action-error">
          {{ actionError }}
        </div>

        <div class="detail-actions">
          <button
            v-if="canJoin"
            class="btn-primary"
            :disabled="actionLoading"
            @click="handleJoin"
          >
            {{ actionLoading ? 'ПРИЄДНАННЯ...' : '⚡ ПРИЄДНАТИСЯ' }}
          </button>

          <div v-else-if="!isMember && isFull && party.isOpen" class="full-label">
            Лобі заповнене
          </div>

          <router-link v-else-if="!auth.isLoggedIn && party.isOpen" to="/login" class="btn-primary">
            УВІЙТИ ДЛЯ ПРИЄДНАННЯ
          </router-link>

          <button
            v-if="isMember && party.chatId"
            class="btn-secondary"
            @click="goToChat"
          >
            💬 ГРУПОВИЙ ЧАТ
          </button>

          <button
            v-if="isMember && !isCreator"
            class="btn-danger"
            :disabled="actionLoading"
            @click="handleLeave"
          >
            {{ actionLoading ? '...' : 'ПОКИНУТИ ЛОБІ' }}
          </button>

          <button
            v-if="isCreator && party.isOpen"
            class="btn-danger-outline"
            :disabled="actionLoading"
            @click="handleClose"
          >
            {{ actionLoading ? '...' : 'ЗАКРИТИ ЛОБІ' }}
          </button>

          <button
            v-if="isCreator"
            class="btn-danger"
            :disabled="actionLoading"
            @click="handleLeave"
          >
            {{ actionLoading ? '...' : 'ПОКИНУТИ ЛОБІ' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.party-detail-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}

.party-detail-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 32px;
}

.party-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.back-link {
  background: none;
  border: none;
  color: var(--gray);
  font-family: var(--font-body);
  font-size: 13px;
  letter-spacing: 1px;
  padding: 4px 0;
  transition: color 0.15s;
  align-self: flex-start;
}
.back-link:hover {
  color: var(--yellow);
}

.detail-header {
  padding: 28px;
}

.detail-header-top {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.detail-game-cover img {
  width: 70px;
  height: 94px;
  object-fit: cover;
  border: 2px solid var(--border);
  flex-shrink: 0;
}
.cover-placeholder {
  width: 70px;
  height: 94px;
  background: var(--panel-light);
  border: 2px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}

.detail-header-info {
  flex: 1;
}

.detail-game-name {
  font-family: var(--font-display);
  font-size: 32px;
  letter-spacing: 2px;
  color: var(--yellow);
  line-height: 1;
  margin-bottom: 8px;
}

.detail-host {
  font-size: 13px;
  color: var(--gray);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.host-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--gray-light);
  transition: color 0.15s;
}
.host-link:hover {
  color: var(--yellow);
}

.host-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--yellow-dim);
}

.detail-time {
  font-size: 11px;
  color: var(--gray);
}

.detail-status {
  flex-shrink: 0;
}

.status-badge {
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 2px;
  padding: 5px 14px;
  border: 2px solid;
}
.status-badge.open {
  border-color: #27ae60;
  color: #2ecc71;
  background: rgba(39, 174, 96, 0.1);
}
.status-badge.closed {
  border-color: var(--red-dim);
  color: var(--red);
  background: rgba(192, 57, 43, 0.1);
}

.detail-description {
  margin-bottom: 16px;
}
.detail-description p {
  color: var(--gray-light);
  font-size: 15px;
  line-height: 1.6;
  font-style: italic;
  white-space: pre-wrap;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tag {
  font-size: 11px;
  letter-spacing: 1px;
  padding: 3px 10px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  color: var(--gray-light);
}

.detail-section {
  padding: 24px 28px;
}

.section-label {
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 3px;
  color: var(--yellow);
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-count {
  font-family: var(--font-body);
  font-size: 14px;
  color: var(--gray);
  letter-spacing: 1px;
}
.member-count span {
  color: var(--yellow);
  font-weight: 700;
}

.slots-visual {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.member-slot {
  border: 2px solid var(--border);
  background: var(--panel-light);
  padding: 12px 16px;
  transition: border-color 0.15s;
}
.member-slot.filled {
  border-color: rgba(245, 197, 24, 0.2);
  background: rgba(245, 197, 24, 0.03);
}

.member-card {
  display: flex;
  align-items: center;
  gap: 12px;
  transition: opacity 0.15s;
}
.member-card:hover {
  opacity: 0.8;
}

.member-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
}
.member-slot.filled .member-avatar {
  border-color: var(--yellow-dim);
}

.member-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.member-name {
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 14px;
  color: var(--white);
  letter-spacing: 0.5px;
}

.creator-tag {
  font-family: var(--font-display);
  font-size: 10px;
  letter-spacing: 2px;
  padding: 2px 8px;
  background: rgba(245, 197, 24, 0.12);
  border: 1px solid var(--yellow-dim);
  color: var(--yellow);
}

.empty-slot-content {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--gray);
}

.empty-slot-icon {
  width: 36px;
  height: 36px;
  border: 2px dashed var(--border);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: var(--gray);
}

.empty-slot-text {
  font-size: 13px;
  font-style: italic;
  letter-spacing: 0.5px;
}

.action-error {
  color: var(--red);
  font-size: 13px;
  letter-spacing: 1px;
  text-align: center;
  padding: 8px;
  background: rgba(192, 57, 43, 0.08);
  border: 1px solid var(--red-dim);
}

.detail-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.btn-primary {
  font-family: var(--font-display);
  letter-spacing: 2px;
  font-size: 16px;
  padding: 12px 32px;
  border: 2px solid var(--yellow);
  background: var(--yellow);
  color: var(--black);
  text-transform: uppercase;
  transition: background 0.15s;
  display: inline-block;
}
.btn-primary:hover:not(:disabled) {
  background: var(--yellow-dim);
}
.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  font-family: var(--font-display);
  letter-spacing: 2px;
  font-size: 14px;
  padding: 10px 24px;
  border: 2px solid var(--yellow);
  background: transparent;
  color: var(--yellow);
  text-transform: uppercase;
  transition: background 0.15s, color 0.15s;
}
.btn-secondary:hover {
  background: var(--yellow);
  color: var(--black);
}

.btn-danger {
  font-family: var(--font-display);
  letter-spacing: 2px;
  font-size: 14px;
  padding: 10px 24px;
  border: 2px solid var(--red);
  background: var(--red);
  color: #fff;
  text-transform: uppercase;
  transition: background 0.15s;
}
.btn-danger:hover:not(:disabled) {
  background: var(--red-dim);
}
.btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-danger-outline {
  font-family: var(--font-display);
  letter-spacing: 2px;
  font-size: 14px;
  padding: 10px 24px;
  border: 2px solid var(--red-dim);
  background: transparent;
  color: var(--red);
  text-transform: uppercase;
  transition: background 0.15s, color 0.15s;
}
.btn-danger-outline:hover:not(:disabled) {
  background: var(--red);
  color: #fff;
}

.full-label {
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 2px;
  color: var(--gray);
  padding: 10px 24px;
  border: 2px dashed var(--border);
  text-transform: uppercase;
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

.empty-state p {
  color: var(--gray);
  font-size: 14px;
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
  .detail-header-top {
    flex-direction: column;
  }
  .detail-actions {
    flex-direction: column;
  }
  .detail-actions button,
  .detail-actions a {
    width: 100%;
    text-align: center;
  }
}
</style>




