import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../api/axios'
import type { Party, CreatePartyRequest, SortOption, Page } from '../types'

export const usePartyStore = defineStore('parties', () => {
  const parties = ref<Party[]>([])
  const loading = ref(false)

  const search = ref('')
  const filterGameId = ref<number | null>(null)
  const filterPlatform = ref('')
  const filterSkillLevel = ref('')
  const filterLanguage = ref('')
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
      const params: Record<string, string | number> = {}
      if (filterGameId.value) params.gameId = filterGameId.value
      if (filterPlatform.value) params.platform = filterPlatform.value
      if (filterSkillLevel.value) params.skillLevel = filterSkillLevel.value
      if (filterLanguage.value) params.language = filterLanguage.value

      const { data } = await api.get<Party[]>('/parties', { params })
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
              (p.description ?? '').toLowerCase().includes(q)
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
    return data
  }

  async function joinParty(partyId: number): Promise<Party> {
    try {
      const { data } = await api.post<Party>(`/parties/${partyId}/join`)
      const idx = parties.value.findIndex((p) => p.id === partyId)
      if (idx !== -1) parties.value[idx] = data
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
        party.isOpen = false
      }
    } catch (e: any) {
      const msg = e.response?.data?.message || 'Не вдалося закрити лобі'
      throw new Error(msg)
    }
  }

  function resetFilters() {
    search.value = ''
    filterGameId.value = null
    filterPlatform.value = ''
    filterSkillLevel.value = ''
    filterLanguage.value = ''
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
      if (filterLanguage.value) params.language = filterLanguage.value

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

  return {
    parties,
    loading,
    search,
    filterGameId,
    filterPlatform,
    filterSkillLevel,
    filterLanguage,
    sortBy,
    filteredParties,
    fetchParties,
    createParty,
    joinParty,
    fetchParty,
    leaveParty,
    closeParty,
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
  }
})