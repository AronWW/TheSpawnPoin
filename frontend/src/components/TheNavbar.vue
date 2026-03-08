<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notifications'
import { useChatStore } from '../stores/chat'
import { useFriendStore } from '../stores/friends'
import { usePartyStore } from '../stores/parties'
import { useGameStore } from '../stores/games'
import { notificationIcon, timeAgo } from '../utils/helpers'
import { API_BASE_URL } from '../config'

const router = useRouter()
const auth = useAuthStore()
const notifStore = useNotificationStore()
const chatStore = useChatStore()
const friendStore = useFriendStore()
const partyStore = usePartyStore()
const gameStore = useGameStore()

const isAdmin = computed(() => auth.user?.role === 'ADMIN')

const notifOpen = ref(false)
const userMenuOpen = ref(false)

const avatarSrc = computed(() => {
  const url = auth.user?.avatarUrl
  if (!url) return API_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return API_BASE_URL + url
})

const profileLink = computed(() =>
    auth.user ? `/profile/${auth.user.id}` : '#'
)

function toggleNotif() {
  notifOpen.value = !notifOpen.value
  userMenuOpen.value = false
  if (notifOpen.value && auth.isLoggedIn) {
    notifStore.fetchNotifications()
  }
}

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
  notifOpen.value = false
}

function handleClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.notif-wrapper')) notifOpen.value = false
  if (!target.closest('.user-menu-wrapper')) userMenuOpen.value = false
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  if (auth.isLoggedIn && !isAdmin.value) {
    notifStore.fetchUnreadCount()
    chatStore.fetchChats()
    friendStore.fetchIncomingRequests()
    partyStore.fetchMyParties()
    gameStore.fetchMySuggestionsCount()
  }
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

async function handleLogout() {
  userMenuOpen.value = false
  await auth.logout()
  router.push('/')
}

async function goToMyParty() {
  userMenuOpen.value = false
  if (!partyStore.myParties.length) {
    await partyStore.fetchMyParties()
  }
  const party = partyStore.myParties[0]
  if (party) {
    router.push(`/party/${party.id}`)
  } else {
    router.push('/search-parties')
  }
}

function handleNotifClick(n: import('../types').Notification) {
  notifStore.markOneRead(n.id)
  notifOpen.value = false
  switch (n.type) {
    case 'FRIEND_REQUEST':
    case 'FRIEND_ACCEPTED':
      router.push('/friends'); break
    case 'PARTY_INVITE':
    case 'PARTY_JOIN':
    case 'PARTY_FULL':
      if (n.referenceId) router.push(`/party/${n.referenceId}`); break
    case 'MESSAGE':
      router.push('/chat'); break
    case 'GAME_SUGGESTION_APPROVED':
    case 'GAME_SUGGESTION_REJECTED':
      router.push('/my-suggestions'); break
    case 'REPORT_REVIEWED':
      break
    case 'SUPPORT_REPLY':
      router.push('/support'); break
  }
}

const respondedInviteIds = ref(new Set<number>())

function isInviteActionable(n: import('../types').Notification): boolean {
  if (!n.referenceId) return false
  if (respondedInviteIds.value.has(n.referenceId)) return false
  if (partyStore.respondedInvites.has(n.referenceId)) return false
  return n.message.includes('запрошує вас')
}

function getInviteResponseLabel(n: import('../types').Notification): string | null {
  if (!n.referenceId) return null
  const fromStore = partyStore.respondedInvites.get(n.referenceId)
  if (fromStore === 'accepted') return '✓ Ви прийняли запрошення'
  if (fromStore === 'declined') return '✗ Ви відхилили запрошення'
  if (fromStore === 'cancelled') return '⊘ Скасовано відправником'
  if (respondedInviteIds.value.has(n.referenceId)) return '✓ Відповідь надіслано'
  return null
}

async function handleAcceptInvite(n: import('../types').Notification) {
  if (!n.referenceId) return
  try {
    await partyStore.fetchIncomingInvites()
    const inv = partyStore.incomingInvites.find((i) => i.inviteId === n.referenceId)
    const partyId = inv?.partyId

    await partyStore.acceptInvite(n.referenceId)
    respondedInviteIds.value.add(n.referenceId)
    notifStore.markOneRead(n.id)
    await partyStore.fetchMyParties()
    notifOpen.value = false

    if (partyId) {
      router.push(`/party/${partyId}`)
    } else if (partyStore.myParties[0]) {
      router.push(`/party/${partyStore.myParties[0].id}`)
    }
  } catch {
  }
}

async function handleDeclineInvite(n: import('../types').Notification) {
  if (!n.referenceId) return
  try {
    await partyStore.declineInvite(n.referenceId)
    respondedInviteIds.value.add(n.referenceId)
    notifStore.markOneRead(n.id)
  } catch {
  }
}
</script>

<template>
  <nav class="navbar">
    <router-link to="/" class="nav-logo">THE<span>SPAWN</span>POINT</router-link>
    <div class="nav-divider"></div>

    <template v-if="auth.isLoggedIn && isAdmin">
      <ul class="nav-links">
        <li><router-link to="/">Головна</router-link></li>
      </ul>

      <div class="nav-right">
        <router-link to="/admin" class="nav-admin-btn">
          АДМІН ПАНЕЛЬ
        </router-link>
        <button class="nav-logout-btn" @click="handleLogout">
          Вийти
        </button>
      </div>
    </template>

    <template v-else>
      <ul class="nav-links">
        <li><router-link to="/">Головна</router-link></li>
        <li><router-link to="/games">Ігри</router-link></li>
        <li><router-link to="/search-parties">Пошук лобі</router-link></li>
        <li v-if="auth.isLoggedIn">
          <router-link to="/friends" class="nav-friends-link">
            Друзі
            <span v-if="friendStore.pendingCount > 0" class="nav-friends-badge">{{ friendStore.pendingCount }}</span>
          </router-link>
        </li>
        <li>
          <router-link to="/chat" class="nav-chat-link">
            Чат
            <span v-if="auth.isLoggedIn && chatStore.totalUnread > 0" class="nav-chat-badge">{{ chatStore.totalUnread }}</span>
          </router-link>
        </li>
      </ul>

      <div class="nav-right">
        <div v-if="auth.isLoggedIn" class="notif-wrapper">
          <button class="notif-btn" @click.stop="toggleNotif" title="Сповіщення">
            🔔
            <span v-if="notifStore.hasUnread" class="notif-badge">{{ notifStore.unreadCount }}</span>
          </button>

          <div v-if="notifOpen" class="notif-panel">
            <div class="notif-panel-header">
              <span>СПОВІЩЕННЯ</span>
              <div class="notif-header-actions">
                <button @click="notifStore.markAllRead" title="Позначити прочитаними">✓ Все</button>
                <button v-if="notifStore.notifications.length" @click="notifStore.deleteAll" title="Видалити всі" class="notif-delete-all-btn">🗑 Все</button>
              </div>
            </div>

            <div class="notif-list">
              <div
                  v-for="n in notifStore.notifications"
                  :key="n.id"
                  class="notif-item"
                  :class="{ unread: !n.read }"
                  @click="handleNotifClick(n)"
              >
                <div class="notif-icon">{{ notificationIcon(n.type) }}</div>
                <div class="notif-content">
                  <div class="notif-text">{{ n.message }}</div>
                  <div class="notif-time">{{ timeAgo(n.createdAt) }}</div>
                  <div
                      v-if="n.type === 'PARTY_INVITE' && n.referenceId && isInviteActionable(n)"
                      class="notif-invite-actions"
                      @click.stop
                  >
                    <button class="notif-accept-btn" @click="handleAcceptInvite(n)">Прийняти</button>
                    <button class="notif-decline-btn" @click="handleDeclineInvite(n)">Відхилити</button>
                  </div>
                  <div
                      v-else-if="n.type === 'PARTY_INVITE' && n.referenceId && getInviteResponseLabel(n)"
                      class="notif-invite-status"
                      :class="{
                        accepted: partyStore.respondedInvites.get(n.referenceId!) === 'accepted',
                        cancelled: partyStore.respondedInvites.get(n.referenceId!) === 'cancelled'
                      }"
                      @click.stop
                  >
                    {{ getInviteResponseLabel(n) }}
                  </div>
                </div>
                <button class="notif-delete-btn" title="Видалити" @click.stop="notifStore.deleteOne(n.id)">✕</button>
              </div>

              <button
                  v-if="notifStore.hasMore && notifStore.notifications.length > 0"
                  class="notif-load-more"
                  :disabled="notifStore.loading"
                  @click.stop="notifStore.loadMore()"
              >
                {{ notifStore.loading ? 'Завантаження...' : 'Завантажити ще' }}
              </button>
            </div>

            <div v-if="!notifStore.notifications.length && !notifStore.loading" class="notif-empty">
              Нових сповіщень немає
            </div>
          </div>
        </div>

        <template v-if="!auth.isLoggedIn">
          <router-link to="/login" class="nav-auth-btn">Увійти</router-link>
          <router-link to="/register" class="nav-auth-btn filled">Реєстрація</router-link>
        </template>

        <template v-else>
          <div class="user-menu-wrapper">
            <button class="nav-user-btn" @click.stop="toggleUserMenu" :class="{ active: userMenuOpen }">
              <div class="nav-avatar-wrap">
                <img :src="avatarSrc" :alt="auth.displayName" class="nav-avatar" />
                <span class="nav-avatar-online"></span>
              </div>
              <div class="nav-user-info">
                <span class="nav-user-name">{{ auth.displayName }}</span>
                <span class="nav-user-hint">Мій акаунт</span>
              </div>
              <span class="nav-user-chevron" :class="{ open: userMenuOpen }">
                <svg width="10" height="6" viewBox="0 0 10 6" fill="none">
                  <path d="M1 1L5 5L9 1" stroke="currentColor" stroke-width="1.5" stroke-linecap="square"/>
                </svg>
              </span>
            </button>

            <Transition name="dropdown">
              <div v-if="userMenuOpen" class="user-dropdown">
                <div class="dropdown-header">
                  <img :src="avatarSrc" :alt="auth.displayName" class="dropdown-header-avatar" />
                  <div class="dropdown-header-info">
                    <span class="dropdown-header-name">{{ auth.displayName }}</span>
                    <span class="dropdown-header-role">Гравець</span>
                  </div>
                </div>

                <div class="dropdown-section">
                  <router-link :to="profileLink" class="dropdown-item" @click="userMenuOpen = false">
                  <span class="di-icon">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>
                  </span>
                    <span class="di-text">Мій профіль</span>
                  </router-link>

                  <button v-if="partyStore.myParties.length > 0" class="dropdown-item" @click="goToMyParty">
                  <span class="di-icon di-icon--yellow">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                  </span>
                    <span class="di-text">Моє лобі</span>
                    <span class="di-badge">Live</span>
                  </button>

                  <router-link to="/favorite-games" class="dropdown-item" @click="userMenuOpen = false">
                  <span class="di-icon">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                  </span>
                    <span class="di-text">Улюблені ігри</span>
                  </router-link>

                  <router-link v-if="gameStore.mySuggestionsCount > 0" to="/my-suggestions" class="dropdown-item" @click="userMenuOpen = false">
                  <span class="di-icon">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 1 1 7.072 0l-.548.547A3.374 3.374 0 0 0 12 18.469c-.89 0-1.74.353-2.366.992l-.172.176z"/></svg>
                  </span>
                    <span class="di-text">Мої заявки на ігри</span>
                  </router-link>

                  <router-link to="/settings" class="dropdown-item" @click="userMenuOpen = false">
                  <span class="di-icon">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
                  </span>
                    <span class="di-text">Налаштування</span>
                  </router-link>

                  <router-link to="/support" class="dropdown-item" @click="userMenuOpen = false">
                  <span class="di-icon">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                  </span>
                    <span class="di-text">Підтримка</span>
                  </router-link>
                </div>

                <div class="dropdown-divider"></div>

                <div class="dropdown-section">
                  <button class="dropdown-item dropdown-item--danger" @click="handleLogout">
                  <span class="di-icon">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                  </span>
                    <span class="di-text">Вийти</span>
                  </button>
                </div>
              </div>
            </Transition>
          </div>
        </template>
      </div>
    </template>
  </nav>
</template>

<style scoped>
.nav-logout-btn {
  font-family: var(--font-display), sans-serif;
  letter-spacing: 2px;
  font-size: 12px;
  padding: 6px 16px;
  border: 2px solid var(--red-dim);
  background: transparent;
  color: var(--red);
  cursor: pointer;
  text-transform: uppercase;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.nav-logout-btn:hover {
  background: var(--red);
  color: var(--white);
  border-color: var(--red);
}

.user-menu-wrapper {
  position: relative;
}

.nav-user-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 12px 5px 5px;
  background: var(--panel);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;
  position: relative;
}
.nav-user-btn:hover,
.nav-user-btn.active {
  border-color: var(--yellow-dim);
  background: var(--panel-light);
  box-shadow: 0 0 0 1px var(--yellow-dim);
}

.nav-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}
.nav-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--yellow-dim);
  display: block;
  transition: border-color 0.15s;
}
.nav-user-btn:hover .nav-avatar,
.nav-user-btn.active .nav-avatar {
  border-color: var(--yellow);
}
.nav-avatar-online {
  position: absolute;
  bottom: -1px;
  right: -1px;
  width: 8px;
  height: 8px;
  background: #27ae60;
  border: 2px solid var(--black);
  border-radius: 50%;
}

.nav-user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;
}
.nav-user-name {
  font-family: var(--font-body);
  font-weight: 700;
  font-size: 13px;
  color: var(--white);
  letter-spacing: 0.5px;
  line-height: 1;
}
.nav-user-hint {
  font-size: 10px;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--gray);
  line-height: 1;
}

.nav-user-chevron {
  color: var(--gray);
  display: flex;
  align-items: center;
  transition: transform 0.2s, color 0.15s;
  flex-shrink: 0;
}
.nav-user-chevron.open {
  transform: rotate(180deg);
  color: var(--yellow);
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 230px;
  background: var(--panel);
  border: 1px solid var(--border);
  border-top: 2px solid var(--yellow-dim);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(245, 197, 24, 0.05);
  z-index: 200;
  overflow: hidden;
}

.dropdown-enter-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.dropdown-leave-active {
  transition: opacity 0.1s ease, transform 0.1s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--panel-light);
  border-bottom: 1px solid var(--border);
}
.dropdown-header-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--yellow-dim);
  flex-shrink: 0;
}
.dropdown-header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.dropdown-header-name {
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 1px;
  color: var(--yellow);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dropdown-header-role {
  font-size: 10px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--gray);
}

.dropdown-section {
  padding: 6px 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 16px;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--gray-light);
  text-decoration: none;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  width: 100%;
  transition: background 0.1s, color 0.1s, padding-left 0.1s;
  position: relative;
}
.dropdown-item:hover {
  background: rgba(245, 197, 24, 0.06);
  color: var(--white);
  padding-left: 20px;
}

.di-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 1px solid var(--border);
  background: var(--dark);
  color: var(--gray);
  flex-shrink: 0;
  transition: border-color 0.1s, color 0.1s;
}
.dropdown-item:hover .di-icon {
  border-color: var(--yellow-dim);
  color: var(--yellow);
}
.di-icon--yellow {
  border-color: var(--yellow-dim);
  color: var(--yellow);
  background: rgba(245, 197, 24, 0.06);
}

.di-text {
  flex: 1;
}

.di-badge {
  font-family: var(--font-display);
  font-size: 9px;
  letter-spacing: 1.5px;
  color: var(--black);
  background: var(--yellow);
  padding: 2px 6px;
  animation: pulse-yellow 2s ease-in-out infinite;
}
@keyframes pulse-yellow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245, 197, 24, 0.5); }
  50%       { box-shadow: 0 0 0 4px rgba(245, 197, 24, 0); }
}

.dropdown-item--danger {
  color: var(--red);
}
.dropdown-item--danger:hover {
  background: rgba(192, 57, 43, 0.08);
  color: var(--red);
}
.dropdown-item--danger .di-icon {
  color: var(--red-dim);
  border-color: var(--red-dim);
}
.dropdown-item--danger:hover .di-icon {
  color: var(--red);
  border-color: var(--red);
}

.dropdown-divider {
  height: 1px;
  background: var(--border);
  margin: 2px 0;
}

.nav-chat-link { position: relative; }
.nav-chat-badge {
  position: absolute;
  top: -6px; right: -10px;
  background: var(--red);
  color: #fff;
  font-size: 9px; font-weight: 700;
  padding: 1px 5px;
  border-radius: 2px;
  font-family: var(--font-body);
  letter-spacing: 0; line-height: 1.3;
  border: 1px solid var(--black);
}

.nav-friends-link { position: relative; }
.nav-friends-badge {
  position: absolute;
  top: -6px; right: -10px;
  background: var(--red);
  color: #fff;
  font-size: 9px; font-weight: 700;
  padding: 1px 5px;
  border-radius: 2px;
  font-family: var(--font-body);
  letter-spacing: 0; line-height: 1.3;
  border: 1px solid var(--black);
}

.notif-wrapper { position: relative; }
.notif-empty {
  padding: 20px;
  text-align: center;
  color: var(--gray);
  font-size: 13px;
}
</style>