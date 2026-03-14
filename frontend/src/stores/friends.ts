import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api/axios'
import type { Friend, FriendRequest } from '../types'

export const useFriendStore = defineStore('friends', () => {
  const friends = ref<Friend[]>([])
  const incomingRequests = ref<FriendRequest[]>([])
  const outgoingRequests = ref<FriendRequest[]>([])
  const loading = ref(false)

  const friendCount = computed(() => friends.value.length)
  const pendingCount = computed(() => incomingRequests.value.length)

  async function fetchFriends() {
    loading.value = true
    try {
      const { data } = await api.get<Friend[]>('/friends')
      friends.value = data
    } catch {
      friends.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchIncomingRequests() {
    try {
      const { data } = await api.get<FriendRequest[]>('/friends/requests/incoming')
      incomingRequests.value = data
    } catch {
      incomingRequests.value = []
    }
  }

  async function fetchOutgoingRequests() {
    try {
      const { data } = await api.get<FriendRequest[]>('/friends/requests/outgoing')
      outgoingRequests.value = data
    } catch {
      outgoingRequests.value = []
    }
  }

  async function sendRequest(userId: number) {
    await api.post(`/friends/request/${userId}`)
    await fetchOutgoingRequests()
  }

  async function acceptRequest(inviteId: number) {
    await api.post(`/friends/accept/${inviteId}`)
    await Promise.all([fetchFriends(), fetchIncomingRequests()])
  }

  async function declineRequest(inviteId: number) {
    await api.post(`/friends/decline/${inviteId}`)
    await fetchIncomingRequests()
  }

  async function cancelRequest(inviteId: number) {
    await api.delete(`/friends/cancel/${inviteId}`)
    await fetchOutgoingRequests()
  }

  async function unfriend(userId: number) {
    await api.delete(`/friends/${userId}`)
    await fetchFriends()
  }

  function updateFriendStatus(email: string, status: string, lastSeen: string | null) {
    const friend = friends.value.find((f) => f.email === email)
    if (friend) {
      friend.status = status
      if (lastSeen) friend.lastSeen = lastSeen
    }
  }

  return {
    friends,
    incomingRequests,
    outgoingRequests,
    loading,
    friendCount,
    pendingCount,
    fetchFriends,
    fetchIncomingRequests,
    fetchOutgoingRequests,
    sendRequest,
    acceptRequest,
    declineRequest,
    cancelRequest,
    unfriend,
    updateFriendStatus,
  }
})

