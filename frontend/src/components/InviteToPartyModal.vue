<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useFriendStore } from '../stores/friends'
import { usePartyStore } from '../stores/parties'
import api from '../api/axios'
import { API_BASE_URL } from '../config'

const props = defineProps<{
  visible: boolean
  partyId: number
  partyMembers: number[]
}>()

const emit = defineEmits<{ (e: 'close'): void }>()

const friendStore = useFriendStore()
const partyStore = usePartyStore()

const tab = ref<'friends' | 'search'>('friends')
const searchQuery = ref('')
const searchResults = ref<{ id: number; displayName: string; avatarUrl: string | null }[]>([])
const searchLoading = ref(false)

const invitingId = ref<number | null>(null)
const invitedIds = ref<Set<number>>(new Set())
const errorMsg = ref('')
const successMsg = ref('')

function resolveAvatar(url: string | null): string {
  if (!url) return API_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return API_BASE_URL + url
}

const pendingInviteUserIds = computed(() =>
  new Set(partyStore.partyInvites.map((i) => i.receiverId))
)

const availableFriends = computed(() => {
  return friendStore.friends.filter((f) => {
    if (props.partyMembers.includes(f.userId)) return false
    if (pendingInviteUserIds.value.has(f.userId)) return false
    if (invitedIds.value.has(f.userId)) return false
    return true
  })
})

const filteredFriends = computed(() => {
  const q = searchQuery.value.toLowerCase().trim()
  if (!q) return availableFriends.value
  return availableFriends.value.filter((f) =>
    f.displayName.toLowerCase().includes(q)
  )
})

const filteredSearchResults = computed(() => {
  return searchResults.value.filter((u) => {
    if (props.partyMembers.includes(u.id)) return false
    if (pendingInviteUserIds.value.has(u.id)) return false
    if (invitedIds.value.has(u.id)) return false
    if (friendStore.friends.some((f) => f.userId === u.id)) return false
    return true
  })
})

async function searchUsers() {
  const q = searchQuery.value.trim()
  if (q.length < 2) {
    searchResults.value = []
    return
  }
  searchLoading.value = true
  try {
    const { data } = await api.get<{ id: number; displayName: string; avatarUrl: string | null }[]>(
      '/users/search',
      { params: { q } }
    )
    searchResults.value = data
  } catch {
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

let searchTimeout: ReturnType<typeof setTimeout> | null = null
watch(searchQuery, () => {
  if (tab.value === 'search') {
    if (searchTimeout) clearTimeout(searchTimeout)
    searchTimeout = setTimeout(searchUsers, 400)
  }
})

async function invite(userId: number) {
  invitingId.value = userId
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await partyStore.sendInvite(props.partyId, userId)
    invitedIds.value.add(userId)
    successMsg.value = 'Запрошення надіслано!'
    setTimeout(() => (successMsg.value = ''), 2000)
    await partyStore.fetchPartyInvites(props.partyId)
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || 'Не вдалося надіслати запрошення'
    setTimeout(() => (errorMsg.value = ''), 3000)
  } finally {
    invitingId.value = null
  }
}

async function cancelPendingInvite(inviteId: number) {
  try {
    await partyStore.cancelInvite(inviteId)
    await partyStore.fetchPartyInvites(props.partyId)
  } catch {
  }
}

function close() {
  searchQuery.value = ''
  searchResults.value = []
  invitedIds.value = new Set()
  errorMsg.value = ''
  successMsg.value = ''
  tab.value = 'friends'
  emit('close')
}

onMounted(() => {
  if (friendStore.friends.length === 0) {
    friendStore.fetchFriends()
  }
  partyStore.fetchPartyInvites(props.partyId)
})

watch(() => props.visible, (v) => {
  if (v) {
    friendStore.fetchFriends()
    partyStore.fetchPartyInvites(props.partyId)
    invitedIds.value = new Set()
  }
})
</script>

<template>
  <Transition name="fade">
    <div v-if="visible" class="modal-overlay" @click.self="close">
      <div class="modal">
        <div class="modal-header">
          <h2 class="modal-title">ЗАПРОСИТИ ГРАВЦЯ</h2>
          <button class="modal-close" @click="close">✕</button>
        </div>

        <div class="tab-bar">
          <button
            class="tab-btn"
            :class="{ active: tab === 'friends' }"
            @click="tab = 'friends'"
          >
            👥 ДРУЗІ
          </button>
          <button
            class="tab-btn"
            :class="{ active: tab === 'search' }"
            @click="tab = 'search'"
          >
            🔍 ПОШУК
          </button>
        </div>

        <div v-if="tab === 'friends'" class="search-box">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Фільтр по друзям..."
            class="search-input"
          />
        </div>

        <div v-if="tab === 'search'" class="search-box">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Пошук гравців за ім'ям..."
            class="search-input"
          />
        </div>

        <div v-if="successMsg" class="msg msg-success">{{ successMsg }}</div>
        <div v-if="errorMsg" class="msg msg-error">{{ errorMsg }}</div>

        <div class="user-list">
          <template v-if="tab === 'friends'">
            <div
              v-for="friend in filteredFriends"
              :key="friend.userId"
              class="user-row"
            >
              <img :src="resolveAvatar(friend.avatarUrl)" class="user-avatar" :alt="friend.displayName" />
              <div class="user-info">
                <span class="user-name">{{ friend.displayName }}</span>
                <span class="user-status" :class="friend.status.toLowerCase()">
                  {{ friend.status === 'ONLINE' ? 'Онлайн' : 'Офлайн' }}
                </span>
              </div>
              <button
                class="invite-btn"
                :disabled="invitingId === friend.userId"
                @click="invite(friend.userId)"
              >
                {{ invitingId === friend.userId ? '...' : 'ЗАПРОСИТИ' }}
              </button>
            </div>
            <div v-if="filteredFriends.length === 0" class="empty-msg">
              Немає доступних друзів для запрошення
            </div>
          </template>

          <template v-if="tab === 'search'">
            <div v-if="searchLoading" class="empty-msg">Пошук...</div>
            <div
              v-for="user in filteredSearchResults"
              :key="user.id"
              class="user-row"
            >
              <img :src="resolveAvatar(user.avatarUrl)" class="user-avatar" :alt="user.displayName" />
              <div class="user-info">
                <span class="user-name">{{ user.displayName }}</span>
              </div>
              <button
                class="invite-btn"
                :disabled="invitingId === user.id"
                @click="invite(user.id)"
              >
                {{ invitingId === user.id ? '...' : 'ЗАПРОСИТИ' }}
              </button>
            </div>
            <div v-if="!searchLoading && searchQuery.length >= 2 && filteredSearchResults.length === 0" class="empty-msg">
              Нікого не знайдено
            </div>
            <div v-if="!searchLoading && searchQuery.length < 2" class="empty-msg">
              Введіть мінімум 2 символи для пошуку
            </div>
          </template>
        </div>

        <div v-if="partyStore.partyInvites.length > 0" class="pending-section">
          <h3 class="pending-title">ОЧІКУЮТЬ ВІДПОВІДІ</h3>
          <div
            v-for="inv in partyStore.partyInvites"
            :key="inv.inviteId"
            class="user-row pending-row"
          >
            <img :src="resolveAvatar(inv.receiverAvatarUrl)" class="user-avatar" :alt="inv.receiverDisplayName" />
            <div class="user-info">
              <span class="user-name">{{ inv.receiverDisplayName }}</span>
              <span class="pending-label">Очікує...</span>
            </div>
            <button class="cancel-btn" @click="cancelPendingInvite(inv.inviteId)">
              СКАСУВАТИ
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 5000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(2px);
}

.modal {
  width: 90%;
  max-width: 480px;
  max-height: 85vh;
  background: var(--panel);
  border: 2px solid var(--border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid var(--border);
}

.modal-title {
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 3px;
  color: var(--yellow);
}

.modal-close {
  background: none;
  border: none;
  color: var(--gray);
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  transition: color 0.15s;
}
.modal-close:hover {
  color: var(--white);
}

.tab-bar {
  display: flex;
  border-bottom: 1px solid var(--border);
}

.tab-btn {
  flex: 1;
  padding: 10px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 2px;
  color: var(--gray);
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
}
.tab-btn:hover {
  color: var(--gray-light);
}
.tab-btn.active {
  color: var(--yellow);
  border-bottom-color: var(--yellow);
}

.search-box {
  padding: 12px 22px;
}

.search-input {
  width: 100%;
  padding: 9px 14px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  color: var(--white);
  font-family: var(--font-body);
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s;
}
.search-input:focus {
  border-color: var(--yellow-dim);
}

.msg {
  padding: 8px 22px;
  font-size: 13px;
  letter-spacing: 0.5px;
}
.msg-success {
  color: #2ecc71;
  background: rgba(46, 204, 113, 0.08);
}
.msg-error {
  color: var(--red);
  background: rgba(192, 57, 43, 0.08);
}

.user-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 22px;
  max-height: 320px;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.user-row:last-child {
  border-bottom: none;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.user-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--white);
  letter-spacing: 0.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-status {
  font-size: 11px;
  letter-spacing: 0.5px;
}
.user-status.online {
  color: #2ecc71;
}
.user-status.offline {
  color: var(--gray);
}

.invite-btn {
  flex-shrink: 0;
  padding: 6px 14px;
  background: transparent;
  border: 1px solid var(--yellow-dim);
  color: var(--yellow);
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.invite-btn:hover:not(:disabled) {
  background: var(--yellow);
  color: var(--black);
}
.invite-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.cancel-btn {
  flex-shrink: 0;
  padding: 6px 14px;
  background: transparent;
  border: 1px solid var(--red-dim);
  color: var(--red);
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.cancel-btn:hover {
  background: var(--red);
  color: #fff;
}

.empty-msg {
  text-align: center;
  color: var(--gray);
  font-size: 13px;
  padding: 24px 0;
}

.pending-section {
  border-top: 1px solid var(--border);
  padding: 12px 22px 16px;
}

.pending-title {
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 2px;
  color: var(--gray);
  margin-bottom: 8px;
}

.pending-label {
  font-size: 11px;
  color: var(--yellow-dim);
  letter-spacing: 0.5px;
}

.pending-row .user-avatar {
  border-color: var(--yellow-dim);
  opacity: 0.7;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>


