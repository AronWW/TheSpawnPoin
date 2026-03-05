import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api/axios'
import type { Notification, Page } from '../types'

export const useNotificationStore = defineStore('notifications', () => {
  const notifications = ref<Notification[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)

  const hasUnread = computed(() => unreadCount.value > 0)

  async function fetchNotifications() {
    loading.value = true
    try {
      const { data } = await api.get<Page<Notification>>('/notifications', {
        params: { page: 0, size: 20 },
      })
      notifications.value = data.content
    } catch {
    } finally {
      loading.value = false
    }
  }

  async function fetchUnreadCount() {
    try {
      const { data } = await api.get<{ count: number }>('/notifications/unread-count')
      unreadCount.value = data.count
    } catch {
      unreadCount.value = 0
    }
  }

  async function markAllRead() {
    try {
      await api.post('/notifications/read-all')
      notifications.value.forEach((n) => (n.read = true))
      unreadCount.value = 0
    } catch {

    }
  }

  async function markOneRead(id: number) {
    try {
      await api.post(`/notifications/${id}/read`)
      const n = notifications.value.find((n) => n.id === id)
      if (n && !n.read) {
        n.read = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch {

    }
  }

  function addNotification(n: Notification) {
    notifications.value = [n, ...notifications.value]
    unreadCount.value++
  }

  function setUnreadCount(count: number) {
    unreadCount.value = count
  }

  return {
    notifications,
    unreadCount,
    loading,
    hasUnread,
    fetchNotifications,
    fetchUnreadCount,
    markAllRead,
    markOneRead,
    addNotification,
    setUnreadCount,
  }
})

