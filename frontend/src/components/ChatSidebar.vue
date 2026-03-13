<script setup lang="ts">
import { ref, computed } from 'vue'
import { useChatStore } from '../stores/chat'
import { timeAgo } from '../utils/helpers'
import type { ChatItem } from '../types'

const chatStore = useChatStore()

const emit = defineEmits<{
  (e: 'select', chat: ChatItem): void
}>()

const props = defineProps<{ search: string }>()

const ctxMenu = ref<{ show: boolean; x: number; y: number; chat: ChatItem | null }>({
  show: false, x: 0, y: 0, chat: null
})

const filteredActiveChats = computed(() => {
  const q = props.search.toLowerCase().trim()
  const list = chatStore.activeChats
  if (!q) return list
  return list.filter((c) => chatDisplayName(c).toLowerCase().includes(q))
})

const filteredArchivedChats = computed(() => {
  const q = props.search.toLowerCase().trim()
  const list = chatStore.archivedChats
  if (!q) return list
  return list.filter((c) => chatDisplayName(c).toLowerCase().includes(q))
})

function chatDisplayName(chat: ChatItem): string {
  if (chat.isGroup) return chat.title || 'Груповий чат'
  return chat.partnerDisplayName || 'Чат'
}

function chatAvatarLetter(chat: ChatItem): string {
  return chatDisplayName(chat).charAt(0).toUpperCase()
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

function openCtxMenu(event: MouseEvent, chat: ChatItem) {
  event.preventDefault()
  ctxMenu.value = { show: true, x: event.clientX, y: event.clientY, chat }
}

function closeCtxMenu() {
  ctxMenu.value = { show: false, x: 0, y: 0, chat: null }
}

function onPinChat() {
  if (!ctxMenu.value.chat) return
  if (ctxMenu.value.chat.pinned) {
    chatStore.unpinChatAction(ctxMenu.value.chat.id)
  } else {
    chatStore.pinChatAction(ctxMenu.value.chat.id)
  }
  closeCtxMenu()
}

function onArchiveChat() {
  if (!ctxMenu.value.chat) return
  if (ctxMenu.value.chat.archived) {
    chatStore.unarchiveChat(ctxMenu.value.chat.id)
  } else {
    chatStore.archiveChat(ctxMenu.value.chat.id)
  }
  closeCtxMenu()
}

function onDeleteChat() {
  if (!ctxMenu.value.chat) return
  chatStore.deleteChatAction(ctxMenu.value.chat.id)
  closeCtxMenu()
}

function onWindowClick() {
  closeCtxMenu()
}
</script>

<template>
  <div class="chat-sidebar" @click="onWindowClick">
    <div class="chat-sidebar-header">
      <span class="chat-sidebar-title">ПОВІДОМЛЕННЯ</span>
      <span v-if="chatStore.totalUnread" class="chat-sidebar-badge">{{ chatStore.totalUnread }}</span>
    </div>

    <div v-if="chatStore.loading" class="chat-sidebar-loading">
      Завантаження...
    </div>

    <template v-else>
      <div v-if="filteredActiveChats.length === 0 && filteredArchivedChats.length === 0" class="chat-sidebar-empty">
        Чатів поки немає
      </div>

      <div v-if="filteredActiveChats.some(c => c.pinned)" class="chat-section-label">ЗАКРІПЛЕНІ</div>

      <div
        v-for="chat in filteredActiveChats.filter(c => c.pinned)"
        :key="'pin-' + chat.id"
        class="chat-sidebar-item pinned-item"
        :class="{ active: isActive(chat), unread: chat.unreadCount > 0 }"
        @click="selectChat(chat)"
        @contextmenu="openCtxMenu($event, chat)"
      >
        <div class="chat-sidebar-avatar">
          <span class="chat-avatar-letter" :class="{ group: chat.isGroup }">{{ chatAvatarLetter(chat) }}</span>
          <span class="chat-status-dot">{{ statusDot(chat) }}</span>
        </div>
        <div class="chat-sidebar-info">
          <div class="chat-sidebar-name">
            <span class="pin-icon">●</span>
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

      <div v-if="filteredActiveChats.some(c => c.pinned) && filteredActiveChats.some(c => !c.pinned)" class="chat-section-label">ВСІ ЧАТИ</div>

      <div
        v-for="chat in filteredActiveChats.filter(c => !c.pinned)"
        :key="chat.id"
        class="chat-sidebar-item"
        :class="{ active: isActive(chat), unread: chat.unreadCount > 0 }"
        @click="selectChat(chat)"
        @contextmenu="openCtxMenu($event, chat)"
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

      <div v-if="filteredArchivedChats.length > 0" class="chat-archive-toggle" @click="chatStore.showArchived = !chatStore.showArchived">
        <span>АРХІВ ({{ filteredArchivedChats.length }})</span>
        <span>{{ chatStore.showArchived ? '▲' : '▼' }}</span>
      </div>

      <template v-if="chatStore.showArchived">
        <div
          v-for="chat in filteredArchivedChats"
          :key="'arch-' + chat.id"
          class="chat-sidebar-item archived-item"
          :class="{ active: isActive(chat) }"
          @click="selectChat(chat)"
          @contextmenu="openCtxMenu($event, chat)"
        >
          <div class="chat-sidebar-avatar">
            <span class="chat-avatar-letter" :class="{ group: chat.isGroup }">{{ chatAvatarLetter(chat) }}</span>
          </div>
          <div class="chat-sidebar-info">
            <div class="chat-sidebar-name">
              <span v-if="chat.isGroup" class="group-tag">ГРУПА</span>
              {{ chatDisplayName(chat) }}
            </div>
            <div class="chat-sidebar-last">{{ truncate(chat.lastMessage) }}</div>
          </div>
        </div>
      </template>
    </template>

    <Teleport to="body">
      <div
        v-if="ctxMenu.show && ctxMenu.chat"
        class="chat-ctx-menu"
        :style="{ top: ctxMenu.y + 'px', left: ctxMenu.x + 'px' }"
        @click.stop
      >
        <button class="ctx-item" @click="onPinChat">
          {{ ctxMenu.chat!.pinned ? 'ВІДКРІПИТИ' : 'ЗАКРІПИТИ' }}
        </button>
        <button class="ctx-item" @click="onArchiveChat">
          {{ ctxMenu.chat!.archived ? 'РОЗАРХІВУВАТИ' : 'АРХІВУВАТИ' }}
        </button>
        <div class="ctx-divider"></div>
        <button class="ctx-item ctx-danger" @click="onDeleteChat">
          {{ ctxMenu.chat!.isGroup ? 'ВИЙТИ З ЧАТУ' : 'ВИДАЛИТИ ЧАТ' }}
        </button>
      </div>
    </Teleport>
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
  position: relative;
}
.chat-sidebar-header::after {
  content: '';
  position: absolute;
  bottom: -3px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, var(--yellow-dim), transparent);
  opacity: 0.4;
}

.chat-sidebar-badge {
  background: var(--red);
  color: #fff;
  font-family: var(--font-body);
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 2px;
  letter-spacing: 0;
  animation: badgePulse 2s ease-in-out infinite;
  box-shadow: 0 0 8px rgba(192,57,43,0.3);
}
@keyframes badgePulse {
  0%, 100% { box-shadow: 0 0 6px rgba(192,57,43,0.3); }
  50% { box-shadow: 0 0 12px rgba(192,57,43,0.6); }
}

.chat-sidebar-loading,
.chat-sidebar-empty {
  padding: 40px 20px;
  text-align: center;
  color: var(--gray);
  font-size: 13px;
  letter-spacing: 1px;
}

.chat-section-label {
  padding: 10px 20px 6px;
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 3px;
  color: var(--gray);
  border-bottom: 1px solid var(--border);
  font-family: var(--font-display);
  border-left: 3px solid var(--yellow-dim);
  background: linear-gradient(90deg, rgba(245,197,24,0.03), transparent);
}

.chat-sidebar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
  border-left: 3px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.chat-sidebar-item:hover {
  background: linear-gradient(90deg, var(--panel-light), var(--panel));
  border-left-color: rgba(245,197,24,0.3);
}
.chat-sidebar-item.active {
  background: linear-gradient(90deg, rgba(245,197,24,0.06), var(--panel-light));
  border-left-color: var(--yellow);
  box-shadow: inset 0 0 20px rgba(245,197,24,0.03);
}
.chat-sidebar-item.unread {
  background: rgba(245, 197, 24, 0.03);
}
.chat-sidebar-item.unread::after {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--yellow);
  animation: unreadGlow 2s ease-in-out infinite;
}
@keyframes unreadGlow {
  0%, 100% { box-shadow: 0 0 4px rgba(245,197,24,0.3); }
  50% { box-shadow: 0 0 10px rgba(245,197,24,0.6); }
}
.chat-sidebar-item.pinned-item {
  background: linear-gradient(90deg, rgba(245,197,24,0.02), transparent);
  background-image:
    linear-gradient(90deg, rgba(245,197,24,0.02), transparent),
    repeating-linear-gradient(
      -45deg,
      transparent,
      transparent 8px,
      rgba(245,197,24,0.008) 8px,
      rgba(245,197,24,0.008) 10px
    );
}
.chat-sidebar-item.archived-item {
  opacity: 0.5;
  filter: grayscale(0.3);
}
.chat-sidebar-item.archived-item:hover {
  opacity: 0.8;
  filter: grayscale(0);
}

.pin-icon {
  font-size: 6px;
  flex-shrink: 0;
  color: var(--yellow);
  line-height: 1;
  text-shadow: 0 0 6px rgba(245,197,24,0.4);
}

.chat-sidebar-avatar {
  position: relative;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
}

.chat-avatar-letter {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  background: linear-gradient(135deg, var(--yellow-glow), rgba(245,197,24,0.06));
  border: 2px solid var(--border);
  font-family: var(--font-display);
  font-size: 18px;
  color: var(--yellow);
  letter-spacing: 1px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
  transition: all 0.2s;
}
.chat-sidebar-item:hover .chat-avatar-letter {
  border-color: var(--yellow-dim);
  box-shadow: 0 2px 10px rgba(245,197,24,0.1);
}

.chat-avatar-letter.group {
  background: linear-gradient(135deg, rgba(41, 128, 185, 0.2), rgba(41, 128, 185, 0.08));
  border-color: rgba(41, 128, 185, 0.4);
  color: #5dade2;
}
.chat-sidebar-item:hover .chat-avatar-letter.group {
  border-color: rgba(93,173,226,0.6);
  box-shadow: 0 2px 10px rgba(41,128,185,0.15);
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
  transition: color 0.2s;
}
.chat-sidebar-item:hover .chat-sidebar-name {
  color: var(--white);
}
.chat-sidebar-item.active .chat-sidebar-name {
  color: var(--yellow);
}

.group-tag {
  font-size: 9px;
  letter-spacing: 1.5px;
  padding: 2px 6px;
  background: linear-gradient(135deg, rgba(41, 128, 185, 0.2), rgba(41, 128, 185, 0.1));
  border: 1px solid rgba(41, 128, 185, 0.4);
  color: #5dade2;
  flex-shrink: 0;
  font-family: var(--font-display);
}

.chat-sidebar-last {
  font-size: 12px;
  color: var(--gray);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 3px;
  transition: color 0.2s;
}

.chat-sidebar-last.sidebar-typing {
  color: var(--yellow);
  font-style: italic;
  animation: typingFade 1.2s ease-in-out infinite;
}
@keyframes typingFade {
  0%, 100% { opacity: 0.7; }
  50% { opacity: 1; }
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
  padding: 2px 7px;
  border-radius: 2px;
  font-family: var(--font-body);
  box-shadow: 0 0 8px rgba(245,197,24,0.3);
  animation: badgePulseYellow 2s ease-in-out infinite;
}
@keyframes badgePulseYellow {
  0%, 100% { box-shadow: 0 0 6px rgba(245,197,24,0.2); }
  50% { box-shadow: 0 0 12px rgba(245,197,24,0.5); }
}

.chat-archive-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  font-size: 11px;
  color: var(--gray);
  cursor: pointer;
  border-bottom: 1px solid var(--border);
  border-top: 2px solid var(--border);
  letter-spacing: 2px;
  font-family: var(--font-display);
  transition: all 0.2s;
  position: relative;
}
.chat-archive-toggle:hover {
  background: var(--panel-light);
  color: var(--yellow);
}
.chat-archive-toggle::before {
  content: '';
  position: absolute;
  top: -2px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--border), transparent);
}

.chat-ctx-menu {
  position: fixed;
  z-index: 9999;
  background: var(--panel);
  border: 2px solid var(--border);
  border-top: 3px solid var(--yellow);
  min-width: 200px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.7), 0 0 0 1px rgba(245,197,24,0.05);
  padding: 6px 0;
  animation: ctxMenuIn 0.12s ease-out;
}
@keyframes ctxMenuIn {
  from { opacity: 0; transform: scale(0.95) translateY(-4px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.chat-ctx-menu::after {
  content: '';
  position: absolute;
  inset: 3px;
  border: 1px solid rgba(245,197,24,0.06);
  pointer-events: none;
}
.ctx-item {
  display: block;
  width: 100%;
  text-align: left;
  background: none;
  border: none;
  border-left: 3px solid transparent;
  padding: 10px 18px 10px 15px;
  color: var(--gray-light);
  font-size: 11px;
  font-family: var(--font-display);
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.12s;
}
.ctx-item:hover {
  background: var(--panel-light);
  color: var(--yellow);
  border-left-color: var(--yellow-dim);
}
.ctx-item.ctx-danger {
  color: var(--gray);
}
.ctx-item.ctx-danger:hover {
  color: var(--red);
  background: rgba(192, 57, 43, 0.08);
  border-left-color: var(--red);
}
.ctx-divider {
  height: 1px;
  background: var(--border);
  margin: 4px 12px;
}
</style>
