import { watch, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notifications'
import { useFriendStore } from '../stores/friends'
import { useChatStore } from '../stores/chat'
import { useStompClient } from './useStompClient'
import type { Notification, ChatMessage } from '../types'


export function useGlobalWebSocket() {
  const auth = useAuthStore()
  const notifStore = useNotificationStore()
  const friendStore = useFriendStore()
  const chatStore = useChatStore()
  const stomp = useStompClient()

  let teardowns: (() => void)[] = []

  function connectAndSubscribe() {
    const email = auth.user?.email
    if (!email) return

    stomp.activate()

    teardowns.push(
      stomp.subscribe('/user/queue/notifications', (frame) => {
        try {
          const notification: Notification = JSON.parse(frame.body)
          notifStore.addNotification(notification)
        } catch {  }
      })
    )

    teardowns.push(
      stomp.subscribe('/user/queue/notifications/unread-count', (frame) => {
        try {
          const payload = JSON.parse(frame.body)
          notifStore.setUnreadCount(payload.count ?? 0)
        } catch {  }
      })
    )

    teardowns.push(
      stomp.subscribe('/topic/status', (frame) => {
        try {
          const payload = JSON.parse(frame.body)
          const { email: userEmail, status, lastSeen } = payload
          if (userEmail) {
            friendStore.updateFriendStatus(userEmail, status, lastSeen || null)
            chatStore.updatePartnerStatus(userEmail, status, lastSeen || null)
          }
        } catch {  }
      })
    )

    teardowns.push(
      stomp.subscribe('/user/queue/messages', (frame) => {
        try {
          const msg: ChatMessage = JSON.parse(frame.body)
          chatStore.onIncomingMessage(msg)
        } catch {  }
      })
    )

    teardowns.push(
      stomp.subscribe('/user/queue/typing', (frame) => {
        try {
          const payload = JSON.parse(frame.body)
          if (payload.chatId && payload.senderEmail) {
            chatStore.onPartnerTyping(payload.chatId, payload.senderEmail)
          }
        } catch {  }
      })
    )

    teardowns.push(
      stomp.subscribe('/user/queue/read', (frame) => {
        try {
          const payload = JSON.parse(frame.body)
          chatStore.onReadReceipt(payload)
        } catch {  }
      })
    )

    notifStore.fetchUnreadCount()
    chatStore.fetchChats()
  }


  function disconnectAndCleanup() {
    for (const unsub of teardowns) {
      try { unsub() } catch {  }
    }
    teardowns = []
    stomp.deactivate()
  }

  const stopWatcher = watch(
    () => auth.isLoggedIn,
    (loggedIn) => {
      if (loggedIn) {
        connectAndSubscribe()
      } else {
        disconnectAndCleanup()
      }
    },
    { immediate: true }
  )

  onUnmounted(() => {
    stopWatcher()
    disconnectAndCleanup()
  })
}

