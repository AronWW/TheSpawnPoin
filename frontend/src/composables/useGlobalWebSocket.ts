import { watch, onUnmounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notifications'
import { useFriendStore } from '../stores/friends'
import { useChatStore } from '../stores/chat'
import { usePartyStore } from '../stores/parties'
import { useStompClient } from './useStompClient'
import type { Notification, ChatMessage, ChatEvent } from '../types'


export function useGlobalWebSocket() {
  const auth = useAuthStore()
  const notifStore = useNotificationStore()
  const friendStore = useFriendStore()
  const chatStore = useChatStore()
  const partyStore = usePartyStore()
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

          const isCancelledInvite =
            notification.type === 'PARTY_INVITE' &&
            notification.referenceId &&
            notification.message.includes('скасував запрошення')

          if (isCancelledInvite && notification.referenceId) {
            if (
              partyStore.pendingInvite &&
              partyStore.pendingInvite.inviteId === notification.referenceId
            ) {
              partyStore.dismissInvitePopup()
            }
            partyStore.respondedInvites.set(notification.referenceId, 'cancelled')

            const existingIdx = notifStore.notifications.findIndex((n) => n.id === notification.id)
            if (existingIdx !== -1) {
              notifStore.notifications[existingIdx] = notification
            } else {
              const origIdx = notifStore.notifications.findIndex(
                (n) => n.type === 'PARTY_INVITE' && n.referenceId === notification.referenceId
              )
              if (origIdx !== -1) {
                notifStore.notifications[origIdx] = notification
              } else {
                notifStore.addNotification(notification)
              }
            }
          } else {
            notifStore.addNotification(notification)
          }

          if (
            notification.type === 'PARTY_INVITE' &&
            notification.referenceId &&
            notification.message.includes('запрошує вас')
          ) {
            partyStore.fetchIncomingInvites().then(() => {
              const inv = partyStore.incomingInvites.find(
                (i) => i.inviteId === notification.referenceId
              )
              if (inv) {
                partyStore.showInvitePopup(inv)
              }
            })
          }
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

    teardowns.push(
      stomp.subscribe('/user/queue/chat-events', (frame) => {
        try {
          const event: ChatEvent = JSON.parse(frame.body)
          chatStore.onChatEvent(event)
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
