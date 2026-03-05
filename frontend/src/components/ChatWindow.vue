<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useChatStore } from '../stores/chat'
import { useAuthStore } from '../stores/auth'
import { useStompClient } from '../composables/useStompClient'

const chatStore = useChatStore()
const auth = useAuthStore()
const stomp = useStompClient()

const messageInput = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const typingTimeout = ref<ReturnType<typeof setTimeout> | null>(null)

const isGroup = computed(() => chatStore.activeChat?.isGroup ?? false)
const activeChatId = computed(() => chatStore.activeChat?.id ?? 0)

const chatTitle = computed(() => {
  const chat = chatStore.activeChat
  if (!chat) return ''
  if (chat.isGroup) return chat.title || 'Груповий чат'
  return chat.partnerDisplayName || 'Чат'
})

const isTyping = computed(() => activeChatId.value ? chatStore.isPartnerTyping(activeChatId.value) : false)
const typingName = computed(() => activeChatId.value ? chatStore.typingDisplayName(activeChatId.value) : '')

const chatSubtitle = computed(() => {
  const chat = chatStore.activeChat
  if (!chat) return ''
  if (chat.isGroup) {
    if (isTyping.value) return `${typingName.value} друкує...`
    const count = chat.participants?.length ?? 0
    return `${count} учасників`
  }
  if (isTyping.value) return 'друкує...'
  return statusText(chat.partnerStatus || 'OFFLINE')
})

function scrollToBottom(force = false) {
  nextTick(() => {
    const el = messagesContainer.value
    if (!el) return
    const isNearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 120
    if (force || isNearBottom) el.scrollTop = el.scrollHeight
  })
}

watch(() => chatStore.messages.length, () => scrollToBottom())
watch(() => chatStore.activeChat, () => {
  nextTick(() => scrollToBottom(true))
})

function sendMessage() {
  const text = messageInput.value.trim()
  if (!text || !chatStore.activeChat) return

  if (isGroup.value) {
    stomp.publish('/app/chat.sendGroup', {
      chatId: chatStore.activeChat.id,
      content: text,
    })
  } else {
    stomp.publish('/app/chat.send', {
      recipientEmail: chatStore.activeChat.partnerEmail,
      content: text,
    })
  }
  messageInput.value = ''
  scrollToBottom(true)
}

function onTyping() {
  if (!chatStore.activeChat) return
  if (!typingTimeout.value) {
    if (isGroup.value) {
      stomp.publish('/app/chat.typingGroup', { chatId: chatStore.activeChat.id })
    } else {
      stomp.publish('/app/chat.typing', { recipientEmail: chatStore.activeChat.partnerEmail })
    }
  }
  if (typingTimeout.value) clearTimeout(typingTimeout.value)
  typingTimeout.value = setTimeout(() => { typingTimeout.value = null }, 2000)
}


watch(() => chatStore.activeChat?.id, (newId) => {
  if (newId && chatStore.activeChat) {
    if (chatStore.activeChat.isGroup) {
      stomp.publish('/app/chat.readGroup', { chatId: chatStore.activeChat.id })
    } else if (chatStore.activeChat.partnerEmail) {
      stomp.publish('/app/chat.read', { senderEmail: chatStore.activeChat.partnerEmail })
    }
  }
})

function onScroll() {
  const el = messagesContainer.value
  if (!el) return
  if (el.scrollTop < 60 && chatStore.hasMore && !chatStore.loadingMessages) {
    const oldHeight = el.scrollHeight
    chatStore.loadOlder().then(() => {
      nextTick(() => { el.scrollTop = el.scrollHeight - oldHeight })
    })
  }
}

function isOwnMessage(email: string | null) { return auth.user?.email === email }

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString('uk-UA', { hour: '2-digit', minute: '2-digit' })
}

function formatDate(iso: string) {
  const d = new Date(iso)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) return 'Сьогодні'
  const y = new Date(now); y.setDate(now.getDate() - 1)
  if (d.toDateString() === y.toDateString()) return 'Вчора'
  return d.toLocaleDateString('uk-UA', { day: 'numeric', month: 'long' })
}

function shouldShowDate(i: number): boolean {
  if (i === 0) return true
  const prev = chatStore.messages[i - 1]
  const curr = chatStore.messages[i]
  if (!prev || !curr) return false
  return new Date(prev.sentAt).toDateString() !== new Date(curr.sentAt).toDateString()
}

function statusText(s: string) {
  if (s === 'ONLINE') return 'онлайн'
  if (s === 'AWAY') return 'відійшов'
  return 'офлайн'
}


</script>

<template>
  <div class="chat-window" v-if="chatStore.activeChat">
    <div class="chat-window-header">
      <div class="cw-avatar" :class="{ group: isGroup }">
        <span class="cw-letter">{{ chatTitle.charAt(0).toUpperCase() }}</span>
      </div>
      <div>
        <div class="cw-name">{{ chatTitle }}</div>
        <div class="cw-status" :class="{ typing: isTyping }">
          {{ chatSubtitle }}
        </div>
      </div>
    </div>

    <div class="chat-messages" ref="messagesContainer" @scroll="onScroll">
      <div v-if="chatStore.loadingMessages && chatStore.messages.length > 0" class="chat-loading-older">
        Завантаження...
      </div>
      <template v-for="(msg, idx) in chatStore.messages" :key="msg.id">
        <div v-if="shouldShowDate(idx)" class="chat-date-divider">
          <span>{{ formatDate(msg.sentAt) }}</span>
        </div>

        <div v-if="msg.system" class="chat-system-msg">
          {{ msg.content }}
        </div>

        <div v-else class="chat-msg" :class="{ own: isOwnMessage(msg.senderEmail) }">
          <div class="chat-msg-bubble">
            <div v-if="isGroup && !isOwnMessage(msg.senderEmail)" class="chat-msg-sender">
              {{ msg.senderName }}
            </div>
            <div class="chat-msg-text">{{ msg.content }}</div>
            <div class="chat-msg-meta">
              <span class="chat-msg-time">{{ formatTime(msg.sentAt) }}</span>
              <span v-if="isOwnMessage(msg.senderEmail)" class="chat-msg-read">
                {{ msg.read ? '✓✓' : '✓' }}
              </span>
            </div>
          </div>
        </div>
      </template>
      <div v-if="chatStore.messages.length === 0 && !chatStore.loadingMessages" class="chat-empty">
        <div class="chat-empty-icon">💬</div>
        <div>Напишіть перше повідомлення!</div>
      </div>
    </div>

    <div class="chat-input-bar">
      <input
        v-model="messageInput"
        @keydown.enter.prevent="sendMessage"
        @input="onTyping"
        type="text"
        class="chat-input"
        placeholder="Написати повідомлення..."
        maxlength="5000"
      />
      <button class="chat-send-btn" @click="sendMessage" :disabled="!messageInput.trim()">
        НАДІСЛАТИ
      </button>
    </div>
  </div>

  <div class="chat-window chat-placeholder" v-else>
    <div class="chat-placeholder-inner">
      <div class="chat-placeholder-icon">💬</div>
      <div class="chat-placeholder-text">Оберіть чат щоб почати спілкування</div>
    </div>
  </div>
</template>

<style scoped>
.cw-avatar.group .cw-letter {
  background: rgba(41, 128, 185, 0.15);
  border-color: rgba(41, 128, 185, 0.4);
  color: #5dade2;
}

.chat-system-msg {
  text-align: center;
  padding: 6px 16px;
  margin: 4px 0;
  font-size: 12px;
  color: var(--gray);
  font-style: italic;
  letter-spacing: 0.5px;
}

.chat-msg-sender {
  font-size: 11px;
  font-weight: 700;
  color: #5dade2;
  letter-spacing: 0.5px;
  margin-bottom: 2px;
}

.cw-status.typing {
  color: var(--yellow) !important;
}
</style>
