import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/axios'
import type { Game } from '../types'

export const useGameStore = defineStore('games', () => {
  const games = ref<Game[]>([])
  const loading = ref(false)

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

  return { games, loading, fetchGames }
})

