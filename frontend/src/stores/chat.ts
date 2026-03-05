import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api/axios'
import { useStompClient } from '../composables/useStompClient'
import { useAuthStore } from './auth'
import type { ChatItem, ChatMessage } from '../types'

export const useChatStore = defineStore('chat', () => {
  const chats = ref<ChatItem[]>([])
  const activeChat = ref<ChatItem | null>(null)
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)
  const loadingMessages = ref(false)
  const page = ref(0)
  const hasMore = ref(true)

  const typingState = ref<Record<number, { name: string; timer: ReturnType<typeof setTimeout> | null }>>({})

  const totalUnread = computed(() =>
    chats.value.reduce((sum, c) => sum + c.unreadCount, 0)
  )

  async function fetchChats() {
    loading.value = true
    try {
      const { data } = await api.get<ChatItem[]>('/chats')
      chats.value = data
    } catch {
      chats.value = []
    } finally {
      loading.value = false
    }
  }

  async function openChat(chat: ChatItem) {
    activeChat.value = chat
    messages.value = []
    page.value = 0
    hasMore.value = true
    await fetchMessages()
    const found = chats.value.find((c) => c.id === chat.id)
    if (found) found.unreadCount = 0
  }

  async function openGroupChatById(chatId: number) {
    if (chats.value.length === 0) await fetchChats()
    const found = chats.value.find((c) => c.id === chatId)
    if (found) {
      await openChat(found)
    } else {
      await fetchChats()
      const retry = chats.value.find((c) => c.id === chatId)
      if (retry) await openChat(retry)
    }
  }

  async function fetchMessages() {
    if (!activeChat.value || loadingMessages.value) return
    loadingMessages.value = true
    try {
      let data: ChatMessage[]
      if (activeChat.value.isGroup) {
        const res = await api.get<ChatMessage[]>(
          `/chats/group/${activeChat.value.id}/messages`,
          { params: { page: page.value, size: 50 } }
        )
        data = res.data
      } else {
        const res = await api.get<ChatMessage[]>(
          `/chats/${encodeURIComponent(activeChat.value.partnerEmail!)}/messages`,
          { params: { page: page.value, size: 50 } }
        )
        data = res.data
      }
      if (data.length < 50) hasMore.value = false
      if (page.value === 0) {
        messages.value = data
      } else {
        messages.value = [...data, ...messages.value]
      }
    } catch {
    } finally {
      loadingMessages.value = false
    }
  }

  async function loadOlder() {
    if (!hasMore.value || loadingMessages.value) return
    page.value++
    await fetchMessages()
  }

  async function openDm(partnerEmail: string) {
    try {
      await api.post(`/chats/dm/${encodeURIComponent(partnerEmail)}`)
      await fetchChats()
      const found = chats.value.find((c) => !c.isGroup && c.partnerEmail === partnerEmail)
      if (found) {
        await openChat(found)
      }
    } catch {
    }
  }

  function onIncomingMessage(msg: ChatMessage) {
    const stomp = useStompClient()
    const auth = useAuthStore()
    const isOwnMessage = msg.senderEmail === auth.user?.email

    if (activeChat.value && msg.chatId === activeChat.value.id) {
      messages.value = [...messages.value, msg]

      if (!isOwnMessage) {
        if (activeChat.value.isGroup) {
          stomp.publish('/app/chat.readGroup', { chatId: activeChat.value.id })
        } else if (activeChat.value.partnerEmail) {
          stomp.publish('/app/chat.read', { senderEmail: activeChat.value.partnerEmail })
        }
      }
    }

    const chatIdx = chats.value.findIndex((c) => c.id === msg.chatId)
    if (chatIdx !== -1) {
      const existing = chats.value[chatIdx]!
      const updated: ChatItem = {
        ...existing,
        lastMessage: msg.content,
        lastMessageAt: msg.sentAt,
        unreadCount: (!activeChat.value || activeChat.value.id !== msg.chatId)
          ? existing.unreadCount + 1
          : existing.unreadCount,
      }
      const rest = chats.value.filter((_, i) => i !== chatIdx)
      chats.value = [updated, ...rest]
    } else {
      fetchChats()
    }
  }

  function onReadReceipt(payload: { readerEmail: string; chatId: number }) {
    messages.value = messages.value.map((m) => {
      if (m.chatId === payload.chatId && m.senderEmail !== payload.readerEmail && !m.read) {
        return { ...m, read: true }
      }
      return m
    })
  }

  function updatePartnerStatus(email: string, status: string, lastSeen: string | null) {
    for (const chat of chats.value) {
      if (!chat.isGroup && chat.partnerEmail === email) {
        chat.partnerStatus = status
        if (lastSeen) chat.partnerLastSeen = lastSeen
      }
    }
    if (activeChat.value && !activeChat.value.isGroup && activeChat.value.partnerEmail === email) {
      activeChat.value = { ...activeChat.value, partnerStatus: status, partnerLastSeen: lastSeen ?? activeChat.value.partnerLastSeen }
    }
  }

  function onPartnerTyping(chatId: number, senderEmail: string) {
    let name = senderEmail
    const chat = chats.value.find((c) => c.id === chatId)
    if (chat) {
      if (chat.isGroup) {
        const p = chat.participants?.find((p) => p.email === senderEmail)
        if (p) name = p.displayName
      } else {
        name = chat.partnerDisplayName || senderEmail
      }
    }

    const existing = typingState.value[chatId]
    if (existing?.timer) clearTimeout(existing.timer)

    const timer = setTimeout(() => {
      clearTyping(chatId)
    }, 3000)

    typingState.value = { ...typingState.value, [chatId]: { name, timer } }
  }

  function clearTyping(chatId: number) {
    const existing = typingState.value[chatId]
    if (existing?.timer) clearTimeout(existing.timer)
    const copy = { ...typingState.value }
    delete copy[chatId]
    typingState.value = copy
  }

  function isPartnerTyping(chatId: number): boolean {
    return !!typingState.value[chatId]
  }

  function typingDisplayName(chatId: number): string {
    return typingState.value[chatId]?.name ?? ''
  }

  function resetChat() {
    activeChat.value = null
    messages.value = []
    page.value = 0
    hasMore.value = true
  }

  return {
    chats,
    activeChat,
    messages,
    loading,
    loadingMessages,
    hasMore,
    totalUnread,
    fetchChats,
    openChat,
    openGroupChatById,
    openDm,
    fetchMessages,
    loadOlder,
    onIncomingMessage,
    onReadReceipt,
    onPartnerTyping,
    isPartnerTyping,
    typingDisplayName,
    clearTyping,
    updatePartnerStatus,
    resetChat,
  }
})

