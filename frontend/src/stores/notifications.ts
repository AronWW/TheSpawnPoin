import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api/axios'
import type { Notification, Page } from '../types'

export const useNotificationStore = defineStore('notifications', () => {
  const notifications = ref<Notification[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const page = ref(0)
  const hasMore = ref(true)
  const pageSize = 15

  const hasUnread = computed(() => unreadCount.value > 0)

  async function fetchNotifications() {
    loading.value = true
    page.value = 0
    hasMore.value = true
    try {
      const { data } = await api.get<Page<Notification>>('/notifications', {
        params: { page: 0, size: pageSize },
      })
      notifications.value = data.content
      hasMore.value = data.content.length >= pageSize
    } catch {
    } finally {
      loading.value = false
    }
  }

  async function loadMore() {
    if (!hasMore.value || loading.value) return
    loading.value = true
    page.value++
    try {
      const { data } = await api.get<Page<Notification>>('/notifications', {
        params: { page: page.value, size: pageSize },
      })
      notifications.value = [...notifications.value, ...data.content]
      hasMore.value = data.content.length >= pageSize
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

  async function deleteOne(id: number) {
    try {
      await api.delete(`/notifications/${id}`)
      const n = notifications.value.find((n) => n.id === id)
      if (n && !n.read) {
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
      notifications.value = notifications.value.filter((n) => n.id !== id)
    } catch {
    }
  }

  async function deleteAll() {
    try {
      await api.delete('/notifications')
      notifications.value = []
      unreadCount.value = 0
    } catch {
    }
  }

  function removeLocal(id: number) {
    const n = notifications.value.find((n) => n.id === id)
    if (n && !n.read) {
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
    notifications.value = notifications.value.filter((n) => n.id !== id)
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
    hasMore,
    fetchNotifications,
    loadMore,
    fetchUnreadCount,
    markAllRead,
    markOneRead,
    deleteOne,
    deleteAll,
    removeLocal,
    addNotification,
    setUnreadCount,
  }
})

