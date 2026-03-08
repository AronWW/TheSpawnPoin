<script setup lang="ts">
import { watch } from 'vue'
import { usePartyStore } from '../stores/parties'
import { useGameStore } from '../stores/games'

const partyStore = usePartyStore()
const gameStore = useGameStore()

watch(
  [
    () => partyStore.filterGameId,
    () => partyStore.filterPlatform,
    () => partyStore.filterSkillLevel,
  ],
  () => {
    partyStore.fetchParties()
  }
)
</script>

<template>
  <div class="filters-bar">
    <span class="filter-label">Фільтри:</span>

    <input
      class="filter-search"
      v-model="partyStore.search"
      placeholder="🔍 Пошук по грі або опису..."
    />

    <select class="filter-select" v-model="partyStore.filterGameId">
      <option :value="null">Всі ігри</option>
      <option v-for="game in gameStore.games" :key="game.id" :value="game.id">
        {{ game.name }}
      </option>
    </select>

    <select class="filter-select" v-model="partyStore.filterSkillLevel">
      <option value="">Будь-який рівень</option>
      <option value="BEGINNER">Початківець</option>
      <option value="INTERMEDIATE">Середній</option>
      <option value="ADVANCED">Просунутий</option>
      <option value="EXPERT">Експерт</option>
    </select>

    <select class="filter-select" v-model="partyStore.filterPlatform">
      <option value="">Всі платформи</option>
      <option value="PC">PC</option>
      <option value="PLAYSTATION">PlayStation</option>
      <option value="XBOX">Xbox</option>
      <option value="NINTENDO">Nintendo</option>
      <option value="MOBILE">Mobile</option>
    </select>

    <div class="sort-btns">
      <button
        class="sort-btn"
        :class="{ active: partyStore.sortBy === 'newest' }"
        @click="partyStore.sortBy = 'newest'"
      >
        Нові
      </button>
      <button
        class="sort-btn"
        :class="{ active: partyStore.sortBy === 'slots' }"
        @click="partyStore.sortBy = 'slots'"
      >
        Є місця
      </button>
      <button
        class="sort-btn"
        :class="{ active: partyStore.sortBy === 'game' }"
        @click="partyStore.sortBy = 'game'"
      >
        Гра А-Я
      </button>
    </div>
  </div>
</template>

