<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'
import ChatSidebar from '../components/ChatSidebar.vue'
import ChatWindow from '../components/ChatWindow.vue'
import type { ChatItem } from '../types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const chatStore = useChatStore()

const searchQuery = ref('')
const mobileView = ref<'sidebar' | 'chat'>('sidebar')

onMounted(async () => {
  if (!auth.isLoggedIn) {
    router.push('/login')
    return
  }

  await chatStore.fetchChats()

  const groupId = route.query.groupId
  if (groupId) {
    await chatStore.openGroupChatById(Number(groupId))
    mobileView.value = 'chat'
  }
})

function onSelectChat(chat: ChatItem) {
  chatStore.openChat(chat)
  mobileView.value = 'chat'
}

function goBackToSidebar() {
  mobileView.value = 'sidebar'
}
</script>

<template>
  <div class="chat-page">
    <div class="chat-layout">
      <div
          class="chat-sidebar-panel ink-panel"
          :class="{ 'mobile-hidden': mobileView === 'chat' }"
      >
        <div class="chat-search-wrapper">
          <input
              v-model="searchQuery"
              type="text"
              class="chat-search"
              placeholder="Пошук чатів..."
          />
        </div>
        <ChatSidebar :search="searchQuery" @select="onSelectChat" />
      </div>

      <div
          class="chat-main-panel ink-panel"
          :class="{ 'mobile-hidden': mobileView === 'sidebar' }"
      >
        <button class="mobile-back-btn" @click="goBackToSidebar">
          ← Всі чати
        </button>
        <ChatWindow />
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}

.chat-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  height: calc(100vh - 64px);
  max-width: 1440px;
  margin: 0 auto;
  border: 2px solid var(--border);
  border-top: none;
  box-shadow: 0 0 60px rgba(0,0,0,0.4);
}

.chat-sidebar-panel {
  display: flex;
  flex-direction: column;
  border-right: 2px solid var(--border);
  overflow: hidden;
  background: var(--panel);
  position: relative;
}
.chat-sidebar-panel.ink-panel:hover,
.chat-main-panel.ink-panel:hover {
  border-color: var(--border);
  box-shadow: none;
}
.chat-sidebar-panel.ink-panel::after,
.chat-main-panel.ink-panel::after {
  display: none;
}

.chat-sidebar-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle, rgba(245,197,24,0.012) 1px, transparent 1px);
  background-size: 6px 6px;
  pointer-events: none;
  z-index: 0;
}

.chat-search-wrapper {
  padding: 16px 16px 12px;
  position: relative;
  z-index: 1;
}

.chat-search {
  width: 100%;
  background: var(--dark);
  border: 2px solid var(--border);
  border-left: 3px solid transparent;
  padding: 11px 14px;
  font-size: 13px;
  color: var(--white);
  font-family: var(--font-body);
  letter-spacing: 0.5px;
  outline: none;
  transition: all 0.2s;
}
.chat-search:focus {
  border-color: var(--yellow-dim);
  border-left-color: var(--yellow);
  box-shadow: 0 0 12px rgba(245,197,24,0.08);
}
.chat-search::placeholder {
  color: var(--gray);
  font-style: italic;
}

.chat-main-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--panel);
}

.mobile-back-btn {
  display: none;
}

@media (max-width: 768px) {
  .chat-layout {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr;
    border: none;
  }

  .chat-sidebar-panel,
  .chat-main-panel {
    grid-column: 1;
    grid-row: 1;
  }

  .mobile-hidden {
    display: none;
  }

  .mobile-back-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: var(--panel);
    border: none;
    border-bottom: 2px solid var(--border);
    color: var(--yellow);
    font-family: var(--font-display);
    font-size: 13px;
    font-weight: 400;
    letter-spacing: 2px;
    cursor: pointer;
    flex-shrink: 0;
    transition: all 0.15s;
    position: relative;
  }
  .mobile-back-btn::after {
    content: '';
    position: absolute;
    bottom: -3px;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, var(--yellow-dim), transparent);
    opacity: 0.3;
  }
  .mobile-back-btn:hover {
    background: rgba(245,197,24,0.04);
  }
}
</style>