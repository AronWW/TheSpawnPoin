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

onMounted(async () => {
  if (!auth.isLoggedIn) {
    router.push('/login')
    return
  }

  await chatStore.fetchChats()

  const groupId = route.query.groupId
  if (groupId) {
    await chatStore.openGroupChatById(Number(groupId))
  }
})

function onSelectChat(chat: ChatItem) {
  chatStore.openChat(chat)
}
</script>

<template>
  <div class="chat-page">
    <div class="chat-layout">
      <div class="chat-sidebar-panel ink-panel">
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

      <div class="chat-main-panel ink-panel">
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

@media (max-width: 768px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }
  .chat-sidebar-panel {
    display: none;
  }
}
</style>


