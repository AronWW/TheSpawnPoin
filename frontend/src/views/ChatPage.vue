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
  grid-template-columns: 340px 1fr;
  height: calc(100vh - 64px);
  max-width: 1400px;
  margin: 0 auto;
}

.chat-sidebar-panel {
  display: flex;
  flex-direction: column;
  border-right: 2px solid var(--border);
  overflow: hidden;
}

.chat-search-wrapper {
  padding: 16px 16px 0;
}

.chat-search {
  width: 100%;
  background: var(--dark);
  border: 2px solid var(--border);
  padding: 10px 14px;
  font-size: 13px;
  color: var(--white);
  font-family: var(--font-body);
  letter-spacing: 0.5px;
  outline: none;
  transition: border-color 0.15s;
}
.chat-search:focus {
  border-color: var(--yellow-dim);
}
.chat-search::placeholder {
  color: var(--gray);
}

.chat-main-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.mobile-back-btn {
  display: none;
}

@media (max-width: 768px) {
  .chat-layout {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr;
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
    gap: 6px;
    padding: 10px 16px;
    background: none;
    border: none;
    border-bottom: 1px solid var(--border);
    color: var(--yellow);
    font-family: var(--font-body);
    font-size: 13px;
    font-weight: 600;
    letter-spacing: 1px;
    cursor: pointer;
    flex-shrink: 0;
    transition: background 0.15s;
  }

  .mobile-back-btn:hover {
    background: var(--yellow-glow);
  }
}
</style>