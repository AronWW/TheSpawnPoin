<script setup lang="ts">
import { computed } from 'vue'
import { useChatStore } from '../stores/chat'
import { timeAgo } from '../utils/helpers'
import type { ChatItem } from '../types'

const chatStore = useChatStore()

const emit = defineEmits<{
  (e: 'select', chat: ChatItem): void
}>()

const props = defineProps<{ search: string }>()

const filteredChats = computed(() => {
  const q = props.search.toLowerCase().trim()
  if (!q) return chatStore.chats
  return chatStore.chats.filter((c) => {
    const name = chatDisplayName(c).toLowerCase()
    return name.includes(q)
  })
})

function chatDisplayName(chat: ChatItem): string {
  if (chat.isGroup) {
    return chat.title || 'Груповий чат'
  }
  return chat.partnerDisplayName || 'Чат'
}

function chatAvatarLetter(chat: ChatItem): string {
  const name = chatDisplayName(chat)
  return name.charAt(0).toUpperCase()
}

function statusDot(chat: ChatItem): string {
  if (chat.isGroup) return '👥'
  const status = chat.partnerStatus
  if (status === 'ONLINE') return '🟢'
  if (status === 'AWAY') return '🟡'
  return '⚫'
}

function isActive(chat: ChatItem) {
  return chatStore.activeChat?.id === chat.id
}

function selectChat(chat: ChatItem) {
  emit('select', chat)
}

function truncate(text: string | null, max = 40) {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '…' : text
}
</script>

<template>
  <div class="chat-sidebar">
    <div class="chat-sidebar-header">
      <span class="chat-sidebar-title">ПОВІДОМЛЕННЯ</span>
      <span v-if="chatStore.totalUnread" class="chat-sidebar-badge">{{ chatStore.totalUnread }}</span>
    </div>

    <div v-if="chatStore.loading" class="chat-sidebar-loading">
      Завантаження...
    </div>

    <div v-else-if="filteredChats.length === 0" class="chat-sidebar-empty">
      Чатів поки немає
    </div>

    <div
      v-for="chat in filteredChats"
      :key="chat.id"
      class="chat-sidebar-item"
      :class="{ active: isActive(chat), unread: chat.unreadCount > 0 }"
      @click="selectChat(chat)"
    >
      <div class="chat-sidebar-avatar">
        <span class="chat-avatar-letter" :class="{ group: chat.isGroup }">{{ chatAvatarLetter(chat) }}</span>
        <span class="chat-status-dot">{{ statusDot(chat) }}</span>
      </div>
      <div class="chat-sidebar-info">
        <div class="chat-sidebar-name">
          <span v-if="chat.isGroup" class="group-tag">ГРУПА</span>
          {{ chatDisplayName(chat) }}
        </div>
        <div class="chat-sidebar-last" :class="{ 'sidebar-typing': chatStore.isPartnerTyping(chat.id) }">
          <template v-if="chatStore.isPartnerTyping(chat.id)">
            <span v-if="chat.isGroup">{{ chatStore.typingDisplayName(chat.id) }} друкує...</span>
            <span v-else>друкує...</span>
          </template>
          <template v-else>{{ truncate(chat.lastMessage) }}</template>
        </div>
      </div>
      <div class="chat-sidebar-meta">
        <div class="chat-sidebar-time" v-if="chat.lastMessageAt">{{ timeAgo(chat.lastMessageAt) }}</div>
        <div v-if="chat.unreadCount > 0" class="chat-unread-badge">{{ chat.unreadCount }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
}

.chat-sidebar-header {
  padding: 18px 20px 14px;
  border-bottom: 2px solid var(--border);
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 3px;
  color: var(--yellow);
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-sidebar-badge {
  background: var(--red);
  color: #fff;
  font-family: var(--font-body);
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 2px;
  letter-spacing: 0;
}

.chat-sidebar-loading,
.chat-sidebar-empty {
  padding: 30px 20px;
  text-align: center;
  color: var(--gray);
  font-size: 13px;
  letter-spacing: 1px;
}

.chat-sidebar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.chat-sidebar-item:hover {
  background: var(--panel-light);
}
.chat-sidebar-item.active {
  background: var(--panel-light);
  border-left: 3px solid var(--yellow);
}
.chat-sidebar-item.unread {
  background: rgba(245, 197, 24, 0.03);
}

.chat-sidebar-avatar {
  position: relative;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

.chat-avatar-letter {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: var(--yellow-glow);
  border: 2px solid var(--border);
  font-family: var(--font-display);
  font-size: 18px;
  color: var(--yellow);
  letter-spacing: 1px;
}
.chat-avatar-letter.group {
  background: rgba(41, 128, 185, 0.15);
  border-color: rgba(41, 128, 185, 0.4);
  color: #5dade2;
}

.chat-status-dot {
  position: absolute;
  bottom: -2px;
  right: -2px;
  font-size: 10px;
  line-height: 1;
}

.chat-sidebar-info {
  flex: 1;
  min-width: 0;
}

.chat-sidebar-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--white);
  letter-spacing: 0.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  align-items: center;
  gap: 6px;
}

.group-tag {
  font-size: 9px;
  letter-spacing: 1px;
  padding: 1px 5px;
  background: rgba(41, 128, 185, 0.15);
  border: 1px solid rgba(41, 128, 185, 0.4);
  color: #5dade2;
  flex-shrink: 0;
}

.chat-sidebar-last {
  font-size: 12px;
  color: var(--gray);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.chat-sidebar-last.sidebar-typing {
  color: var(--yellow);
  font-style: italic;
}

.chat-sidebar-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.chat-sidebar-time {
  font-size: 10px;
  color: var(--gray);
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.chat-unread-badge {
  background: var(--yellow);
  color: var(--black);
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 2px;
  font-family: var(--font-body);
}
</style>

