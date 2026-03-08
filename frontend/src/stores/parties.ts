import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api/axios'
import type { Party, CreatePartyRequest, SortOption, Page, PartyInvite } from '../types'

export const usePartyStore = defineStore('parties', () => {
  const parties = ref<Party[]>([])
  const loading = ref(false)

  const search = ref('')
  const filterGameId = ref<number | null>(null)
  const filterPlatform = ref('')
  const filterSkillLevel = ref('')
  const sortBy = ref<SortOption>('newest')

  const searchParties = ref<Party[]>([])
  const searchLoading = ref(false)
  const searchPage = ref(0)
  const searchTotalPages = ref(0)
  const searchTotalElements = ref(0)
  const searchSize = ref(8)

  async function fetchParties() {
    loading.value = true
    try {
      const { data } = await api.get<Party[]>('/parties')
      parties.value = data
    } catch {
      parties.value = []
    } finally {
      loading.value = false
    }
  }

  const filteredParties = computed(() => {
    let result = parties.value

    const q = search.value.toLowerCase().trim()
    if (q) {
      result = result.filter(
          (p) =>
              p.gameName.toLowerCase().includes(q) ||
              (p.description ?? '').toLowerCase().includes(q) ||
              (p.title ?? '').toLowerCase().includes(q)
      )
    }

    if (sortBy.value === 'slots') {
      result = [...result].sort(
          (a, b) => (b.maxMembers - b.currentMembers) - (a.maxMembers - a.currentMembers)
      )
    } else if (sortBy.value === 'game') {
      result = [...result].sort((a, b) => a.gameName.localeCompare(b.gameName))
    }

    return result
  })

  async function createParty(dto: CreatePartyRequest): Promise<Party> {
    const { data } = await api.post<Party>('/parties', dto)
    parties.value.unshift(data)
    await fetchMyParties()
    return data
  }

  async function joinParty(partyId: number): Promise<Party> {
    try {
      const { data } = await api.post<Party>(`/parties/${partyId}/join`)
      const idx = parties.value.findIndex((p) => p.id === partyId)
      if (idx !== -1) parties.value[idx] = data
      await fetchMyParties()
      return data
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Не вдалося приєднатись'
      throw new Error(msg)
    }
  }

  async function fetchParty(partyId: number): Promise<Party> {
    const { data } = await api.get<Party>(`/parties/${partyId}`)
    return data
  }

  async function leaveParty(partyId: number): Promise<void> {
    try {
      await api.post(`/parties/${partyId}/leave`)
      parties.value = parties.value.filter((p) => p.id !== partyId)
      await fetchMyParties()
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Не вдалося покинути лобі'
      throw new Error(msg)
    }
  }

  async function closeParty(partyId: number): Promise<void> {
    try {
      await api.post(`/parties/${partyId}/close`)
      const idx = parties.value.findIndex((p) => p.id === partyId)
      const party = parties.value[idx]
      if (party) {
        party.status = 'CANCELLED'
        party.isOpen = false
      }
      await fetchMyParties()
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Не вдалося закрити лобі'
      throw new Error(msg)
    }
  }

  async function startGame(partyId: number): Promise<Party> {
    try {
      const { data } = await api.post<Party>(`/parties/${partyId}/start`)
      return data
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Не вдалося почати гру'
      throw new Error(msg)
    }
  }

  async function completeParty(partyId: number): Promise<Party> {
    try {
      const { data } = await api.post<Party>(`/parties/${partyId}/complete`)
      await fetchMyParties()
      return data
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Не вдалося завершити гру'
      throw new Error(msg)
    }
  }

  async function kickMember(partyId: number, userId: number): Promise<Party> {
    try {
      const { data } = await api.post<Party>(`/parties/${partyId}/kick/${userId}`)
      return data
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Не вдалося кікнути гравця'
      throw new Error(msg)
    }
  }

  function resetFilters() {
    search.value = ''
    filterGameId.value = null
    filterPlatform.value = ''
    filterSkillLevel.value = ''
    sortBy.value = 'newest'
  }

  async function fetchSearchParties(page = 0) {
    searchLoading.value = true
    try {
      const params: Record<string, string | number> = { page, size: searchSize.value }
      if (search.value.trim()) params.q = search.value.trim()
      if (filterGameId.value) params.gameId = filterGameId.value
      if (filterPlatform.value) params.platform = filterPlatform.value
      if (filterSkillLevel.value) params.skillLevel = filterSkillLevel.value

      const { data } = await api.get<Page<Party>>('/parties/search', { params })
      searchParties.value = data.content
      searchPage.value = data.page?.number ?? page
      searchTotalPages.value = data.page?.totalPages ?? 0
      searchTotalElements.value = data.page?.totalElements ?? 0
    } catch {
      searchParties.value = []
    } finally {
      searchLoading.value = false
    }
  }

  const myParties = ref<Party[]>([])
  const myPartiesLoading = ref(false)

  async function fetchMyParties() {
    myPartiesLoading.value = true
    try {
      const { data } = await api.get<Party[]>('/parties/my')
      myParties.value = data
    } catch {
      myParties.value = []
    } finally {
      myPartiesLoading.value = false
    }
  }

  const historyParties = ref<Party[]>([])
  const historyLoading = ref(false)
  const historyPage = ref(0)
  const historyTotalPages = ref(0)

  async function fetchHistory(page = 0) {
    historyLoading.value = true
    try {
      const { data } = await api.get<Page<Party>>('/parties/history', {
        params: { page, size: 10 },
      })
      historyParties.value = data.content
      historyPage.value = data.page?.number ?? page
      historyTotalPages.value = data.page?.totalPages ?? 0
    } catch {
      historyParties.value = []
    } finally {
      historyLoading.value = false
    }
  }

  const incomingInvites = ref<PartyInvite[]>([])
  const outgoingInvites = ref<PartyInvite[]>([])
  const partyInvites = ref<PartyInvite[]>([])
  const pendingInvite = ref<PartyInvite | null>(null)
  const respondedInvites = ref<Map<number, 'accepted' | 'declined' | 'cancelled'>>(new Map())

  async function sendInvite(partyId: number, userId: number): Promise<PartyInvite> {
    const { data } = await api.post<PartyInvite>(`/parties/${partyId}/invite/${userId}`)
    return data
  }

  async function acceptInvite(inviteId: number): Promise<void> {
    await api.post(`/parties/invites/${inviteId}/accept`)
    respondedInvites.value.set(inviteId, 'accepted')
    pendingInvite.value = null
    incomingInvites.value = incomingInvites.value.filter((i) => i.inviteId !== inviteId)
  }

  async function declineInvite(inviteId: number): Promise<void> {
    await api.post(`/parties/invites/${inviteId}/decline`)
    respondedInvites.value.set(inviteId, 'declined')
    pendingInvite.value = null
    incomingInvites.value = incomingInvites.value.filter((i) => i.inviteId !== inviteId)
  }

  async function cancelInvite(inviteId: number): Promise<void> {
    await api.delete(`/parties/invites/${inviteId}`)
    outgoingInvites.value = outgoingInvites.value.filter((i) => i.inviteId !== inviteId)
    partyInvites.value = partyInvites.value.filter((i) => i.inviteId !== inviteId)
  }

  async function fetchIncomingInvites(): Promise<void> {
    try {
      const { data } = await api.get<PartyInvite[]>('/parties/invites/incoming')
      incomingInvites.value = data
    } catch {
      incomingInvites.value = []
    }
  }

  async function fetchOutgoingInvites(): Promise<void> {
    try {
      const { data } = await api.get<PartyInvite[]>('/parties/invites/outgoing')
      outgoingInvites.value = data
    } catch {
      outgoingInvites.value = []
    }
  }

  async function fetchPartyInvites(partyId: number): Promise<void> {
    try {
      const { data } = await api.get<PartyInvite[]>(`/parties/${partyId}/invites`)
      partyInvites.value = data
    } catch {
      partyInvites.value = []
    }
  }

  function showInvitePopup(invite: PartyInvite) {
    pendingInvite.value = invite
  }

  function dismissInvitePopup() {
    pendingInvite.value = null
  }

  return {
    parties,
    loading,
    search,
    filterGameId,
    filterPlatform,
    filterSkillLevel,
    sortBy,
    filteredParties,
    fetchParties,
    createParty,
    joinParty,
    fetchParty,
    leaveParty,
    closeParty,
    startGame,
    completeParty,
    kickMember,
    resetFilters,
    searchParties,
    searchLoading,
    searchPage,
    searchTotalPages,
    searchTotalElements,
    searchSize,
    fetchSearchParties,
    myParties,
    myPartiesLoading,
    fetchMyParties,
    historyParties,
    historyLoading,
    historyPage,
    historyTotalPages,
    fetchHistory,
    incomingInvites,
    outgoingInvites,
    partyInvites,
    pendingInvite,
    respondedInvites,
    sendInvite,
    acceptInvite,
    declineInvite,
    cancelInvite,
    fetchIncomingInvites,
    fetchOutgoingInvites,
    fetchPartyInvites,
    showInvitePopup,
    dismissInvitePopup,
  }
})