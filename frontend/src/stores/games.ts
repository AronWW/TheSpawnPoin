import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/axios'
import type { Game } from '../types'

export const useGameStore = defineStore('games', () => {
  const games = ref<Game[]>([])
  const loading = ref(false)

  const favoriteGameIds = ref<Set<number>>(new Set())
  const favoriteGames = ref<Game[]>([])
  const favoritesLoading = ref(false)

  const mySuggestionsCount = ref(0)

  async function fetchGames() {
    loading.value = true
    try {
      const { data } = await api.get<Game[]>('/games')
      games.value = data
    } catch {
      games.value = []
    } finally {
      loading.value = false
    }
  }

  async function fetchFavorites() {
    favoritesLoading.value = true
    try {
      const { data } = await api.get<Game[]>('/users/me/games')
      favoriteGames.value = data
      favoriteGameIds.value = new Set(data.map((g) => g.id))
    } catch {
      favoriteGames.value = []
      favoriteGameIds.value = new Set()
    } finally {
      favoritesLoading.value = false
    }
  }

  function isFavorite(gameId: number): boolean {
    return favoriteGameIds.value.has(gameId)
  }

  async function addFavorite(gameId: number) {
    favoriteGameIds.value = new Set([...favoriteGameIds.value, gameId])
    try {
      await api.post(`/users/me/games/${gameId}`)
      await fetchFavorites()
    } catch {
      const s = new Set(favoriteGameIds.value)
      s.delete(gameId)
      favoriteGameIds.value = s
    }
  }

  async function removeFavorite(gameId: number) {
    const s = new Set(favoriteGameIds.value)
    s.delete(gameId)
    favoriteGameIds.value = s
    try {
      await api.delete(`/users/me/games/${gameId}`)
      await fetchFavorites()
    } catch {
      favoriteGameIds.value = new Set([...favoriteGameIds.value, gameId])
    }
  }

  async function toggleFavorite(gameId: number) {
    if (isFavorite(gameId)) {
      await removeFavorite(gameId)
    } else {
      await addFavorite(gameId)
    }
  }

  async function fetchMySuggestionsCount() {
    try {
      const { data } = await api.get<any[]>('/game-suggestions/my')
      mySuggestionsCount.value = data.length
    } catch {
      mySuggestionsCount.value = 0
    }
  }

  return {
    games, loading, fetchGames,
    favoriteGameIds, favoriteGames, favoritesLoading,
    fetchFavorites, isFavorite, addFavorite, removeFavorite, toggleFavorite,
    mySuggestionsCount, fetchMySuggestionsCount,
  }
})

