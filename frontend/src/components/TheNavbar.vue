<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notifications'
import { useChatStore } from '../stores/chat'
import { notificationIcon, timeAgo } from '../utils/helpers'

const router = useRouter()
const auth = useAuthStore()
const notifStore = useNotificationStore()
const chatStore = useChatStore()

const notifOpen = ref(false)

const avatarBase = 'http://localhost:8080'

const avatarSrc = computed(() => {
  const url = auth.user?.avatarUrl
  if (!url) return avatarBase + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return avatarBase + url
})

const profileLink = computed(() =>
  auth.user ? `/profile/${auth.user.id}` : '#'
)

function toggleNotif() {
  notifOpen.value = !notifOpen.value
  if (notifOpen.value && auth.isLoggedIn) {
    notifStore.fetchNotifications()
  }
}

function handleClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.notif-wrapper')) {
    notifOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  if (auth.isLoggedIn) {
    notifStore.fetchUnreadCount()
    chatStore.fetchChats()
  }
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

async function handleLogout() {
  await auth.logout()
  router.push('/')
}
</script>

<template>
  <nav class="navbar">
    <router-link to="/" class="nav-logo">THE<span>SPAWN</span>POINT</router-link>
    <div class="nav-divider"></div>

    <ul class="nav-links">
      <li><router-link to="/">Головна</router-link></li>
      <li v-if="auth.isLoggedIn"><router-link to="/my-parties">Мої лобі</router-link></li>
      <li v-if="auth.isLoggedIn"><router-link to="/friends">Друзі</router-link></li>
      <li>
        <router-link to="/chat" class="nav-chat-link">
          Чат
          <span v-if="auth.isLoggedIn && chatStore.totalUnread > 0" class="nav-chat-badge">{{ chatStore.totalUnread }}</span>
        </router-link>
      </li>
    </ul>

    <div class="nav-right">
      <div v-if="auth.isLoggedIn" class="notif-wrapper" style="position: relative;">
        <button class="notif-btn" @click.stop="toggleNotif" title="Сповіщення">
          🔔
          <span v-if="notifStore.hasUnread" class="notif-badge">{{ notifStore.unreadCount }}</span>
        </button>

        <div v-if="notifOpen" class="notif-panel">
          <div class="notif-panel-header">
            <span>СПОВІЩЕННЯ</span>
            <button @click="notifStore.markAllRead">Позначити прочитаними</button>
          </div>

          <div
            v-for="n in notifStore.notifications"
            :key="n.id"
            class="notif-item"
            :class="{ unread: !n.read }"
            @click="notifStore.markOneRead(n.id)"
          >
            <div class="notif-icon">{{ notificationIcon(n.type) }}</div>
            <div>
              <div class="notif-text">{{ n.message }}</div>
              <div class="notif-time">{{ timeAgo(n.createdAt) }}</div>
            </div>
          </div>

          <div
            v-if="!notifStore.notifications.length"
            style="padding: 20px; text-align: center; color: var(--gray); font-size: 13px;"
          >
            Нових сповіщень немає
          </div>
        </div>
      </div>

      <template v-if="!auth.isLoggedIn">
        <router-link to="/login" class="nav-auth-btn">Увійти</router-link>
        <router-link to="/register" class="nav-auth-btn filled">Реєстрація</router-link>
      </template>
      <template v-else>
        <router-link :to="profileLink" class="nav-user-link">
          <img :src="avatarSrc" :alt="auth.displayName" class="nav-avatar" />
          <span class="nav-user-name">{{ auth.displayName }}</span>
        </router-link>
        <button class="nav-auth-btn" @click="handleLogout">Вийти</button>
      </template>
    </div>
  </nav>
</template>

<style scoped>
.nav-user-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border: 1px solid transparent;
  border-radius: var(--radius);
  transition: border-color 0.15s, background 0.15s;
}
.nav-user-link:hover {
  border-color: var(--border);
  background: rgba(245, 197, 24, 0.04);
}

.nav-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--yellow-dim);
  flex-shrink: 0;
  transition: border-color 0.15s;
}
.nav-user-link:hover .nav-avatar {
  border-color: var(--yellow);
}

.nav-user-name {
  font-family: var(--font-body);
  font-weight: 600;
  font-size: 14px;
  color: var(--yellow);
  letter-spacing: 1px;
}

.nav-chat-link {
  position: relative;
}

.nav-chat-badge {
  position: absolute;
  top: -6px;
  right: -10px;
  background: var(--red);
  color: #fff;
  font-size: 9px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 2px;
  font-family: var(--font-body);
  letter-spacing: 0;
  line-height: 1.3;
  border: 1px solid var(--black);
}
</style>

