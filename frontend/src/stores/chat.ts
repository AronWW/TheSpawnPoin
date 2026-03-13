import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api/axios'
import { useStompClient } from '../composables/useStompClient'
import { useAuthStore } from './auth'
import { useToast } from '../composables/useToast'
import type { ChatItem, ChatMessage, ChatEvent, PinnedMessageInfo, ReactionInfo } from '../types'

export const useChatStore = defineStore('chat', () => {
  const chats = ref<ChatItem[]>([])
  const activeChat = ref<ChatItem | null>(null)
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)
  const loadingMessages = ref(false)
  const page = ref(0)
  const hasMore = ref(true)

  const typingState = ref<Record<number, { name: string; timer: ReturnType<typeof setTimeout> | null }>>({})

  const replyingTo = ref<ChatMessage | null>(null)
  const editingMessage = ref<ChatMessage | null>(null)
  const searchQuery = ref('')
  const searchResults = ref<ChatMessage[]>([])
  const searchLoading = ref(false)
  const showArchived = ref(false)
  const pinnedMessages = ref<PinnedMessageInfo[]>([])

  const totalUnread = computed(() =>
    chats.value.reduce((sum, c) => sum + c.unreadCount, 0)
  )

  const sortedChats = computed(() => {
    const list = [...chats.value]
    list.sort((a, b) => {
      if (a.pinned && !b.pinned) return -1
      if (!a.pinned && b.pinned) return 1
      if (a.pinned && b.pinned) {
        const pa = a.pinnedAt ? new Date(a.pinnedAt).getTime() : 0
        const pb = b.pinnedAt ? new Date(b.pinnedAt).getTime() : 0
        return pa - pb
      }
      const ta = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0
      const tb = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0
      return tb - ta
    })
    return list
  })

  const activeChats = computed(() => sortedChats.value.filter(c => !c.archived))
  const archivedChats = computed(() => sortedChats.value.filter(c => c.archived))

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
    replyingTo.value = null
    editingMessage.value = null
    searchQuery.value = ''
    searchResults.value = []
    await fetchMessages()
    const found = chats.value.find((c) => c.id === chat.id)
    if (found) found.unreadCount = 0
    await fetchPinnedMessages(chat.id)
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

  function deleteMessage(messageId: number) {
    const stomp = useStompClient()
    stomp.publish('/app/chat.deleteMessage', { messageId })
  }

  function editMessageAction(messageId: number, newContent: string) {
    const stomp = useStompClient()
    stomp.publish('/app/chat.editMessage', { messageId, newContent })
    editingMessage.value = null
  }

  function toggleReaction(messageId: number, emoji: string) {
    const stomp = useStompClient()
    stomp.publish('/app/chat.toggleReaction', { messageId, emoji })
  }

  function setReplyingTo(msg: ChatMessage | null) {
    replyingTo.value = msg
    editingMessage.value = null
  }

  function setEditingMessage(msg: ChatMessage | null) {
    editingMessage.value = msg
    replyingTo.value = null
  }

  async function archiveChat(chatId: number) {
    try {
      await api.post(`/chats/${chatId}/archive`)
      const chat = chats.value.find(c => c.id === chatId)
      if (chat) chat.archived = true
      if (activeChat.value?.id === chatId) resetChat()
    } catch {}
  }

  async function unarchiveChat(chatId: number) {
    try {
      await api.delete(`/chats/${chatId}/archive`)
      const chat = chats.value.find(c => c.id === chatId)
      if (chat) chat.archived = false
    } catch {}
  }

  async function pinChatAction(chatId: number) {
    try {
      await api.post(`/chats/${chatId}/pin`)
      const chat = chats.value.find(c => c.id === chatId)
      if (chat) {
        chat.pinned = true
        chat.pinnedAt = new Date().toISOString()
      }
    } catch {}
  }

  async function unpinChatAction(chatId: number) {
    try {
      await api.delete(`/chats/${chatId}/pin`)
      const chat = chats.value.find(c => c.id === chatId)
      if (chat) {
        chat.pinned = false
        chat.pinnedAt = null
      }
    } catch {}
  }

  async function deleteChatAction(chatId: number) {
    try {
      await api.delete(`/chats/${chatId}`)
      chats.value = chats.value.filter(c => c.id !== chatId)
      if (activeChat.value?.id === chatId) resetChat()
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.response?.data?.error || 'Не вдалося видалити чат'
      const toast = useToast()
      toast.show(msg, 'error', 5000)
    }
  }

  async function searchMessagesInChat(chatId: number, query: string) {
    if (!query.trim()) {
      searchResults.value = []
      return
    }
    searchLoading.value = true
    try {
      const { data } = await api.get<ChatMessage[]>(`/chats/${chatId}/messages/search`, {
        params: { q: query, page: 0, size: 50 }
      })
      searchResults.value = data
    } catch {
      searchResults.value = []
    } finally {
      searchLoading.value = false
    }
  }

  function pinMessageAction(chatId: number, messageId: number) {
    const stomp = useStompClient()
    stomp.publish('/app/chat.pinMessage', { chatId, messageId })
  }

  function unpinMessageAction(chatId: number, messageId: number) {
    const stomp = useStompClient()
    stomp.publish('/app/chat.unpinMessage', { chatId, messageId })
  }

  async function fetchPinnedMessages(chatId: number) {
    try {
      const { data } = await api.get<PinnedMessageInfo[]>(`/chats/${chatId}/pinned-messages`)
      pinnedMessages.value = data
    } catch {
      pinnedMessages.value = []
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

  function onChatEvent(event: ChatEvent) {
    switch (event.type) {
      case 'MESSAGE_DELETED': {
        const { messageId } = event.payload
        messages.value = messages.value.map(m =>
          m.id === messageId
            ? { ...m, deleted: true, content: 'Повідомлення видалено', reactions: [] }
            : m
        )
        break
      }
      case 'MESSAGE_EDITED': {
        const updated = event.payload as ChatMessage
        messages.value = messages.value.map(m =>
          m.id === updated.id ? { ...m, content: updated.content, edited: true, editedAt: updated.editedAt } : m
        )
        break
      }
      case 'REACTION_UPDATED': {
        const { messageId, reactions } = event.payload as { messageId: number; reactions: ReactionInfo[] }
        messages.value = messages.value.map(m =>
          m.id === messageId ? { ...m, reactions } : m
        )
        break
      }
      case 'MESSAGE_PINNED': {
        const pinned = event.payload as PinnedMessageInfo
        pinnedMessages.value = [...pinnedMessages.value, pinned]
        break
      }
      case 'MESSAGE_UNPINNED': {
        const { messageId } = event.payload
        pinnedMessages.value = pinnedMessages.value.filter(p => p.messageId !== messageId)
        break
      }
      case 'CHAT_DELETED': {
        const { chatId } = event.payload
        chats.value = chats.value.filter(c => c.id !== chatId)
        if (activeChat.value?.id === chatId) resetChat()
        break
      }
    }
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
    replyingTo.value = null
    editingMessage.value = null
    searchQuery.value = ''
    searchResults.value = []
    pinnedMessages.value = []
  }

  return {
    chats,
    activeChat,
    messages,
    loading,
    loadingMessages,
    hasMore,
    totalUnread,
    sortedChats,
    activeChats,
    archivedChats,
    replyingTo,
    editingMessage,
    searchQuery,
    searchResults,
    searchLoading,
    showArchived,
    pinnedMessages,
    fetchChats,
    openChat,
    openGroupChatById,
    openDm,
    fetchMessages,
    loadOlder,
    deleteMessage,
    editMessageAction,
    toggleReaction,
    setReplyingTo,
    setEditingMessage,
    archiveChat,
    unarchiveChat,
    pinChatAction,
    unpinChatAction,
    deleteChatAction,
    searchMessagesInChat,
    pinMessageAction,
    unpinMessageAction,
    fetchPinnedMessages,
    onIncomingMessage,
    onReadReceipt,
    onChatEvent,
    onPartnerTyping,
    isPartnerTyping,
    typingDisplayName,
    clearTyping,
    updatePartnerStatus,
    resetChat,
  }
})

