<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { usePartyStore } from '../stores/parties'
import { useGameStore } from '../stores/games'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notifications'
import HeroSection from '../components/HeroSection.vue'
import PartyFilters from '../components/PartyFilters.vue'
import PartyGrid from '../components/PartyGrid.vue'
import HowItWorks from '../components/HowItWorks.vue'
import CreatePartyModal from '../components/CreatePartyModal.vue'

const partyStore = usePartyStore()
const gameStore = useGameStore()
const auth = useAuthStore()
const notifStore = useNotificationStore()

const modalOpen = ref(false)

function openModal() {
  modalOpen.value = true
}

onMounted(async () => {
  await Promise.all([
    partyStore.fetchParties(),
    gameStore.fetchGames(),
    auth.fetchMe(),
  ])
  if (auth.isLoggedIn) {
    notifStore.fetchUnreadCount()
  }
})
</script>

<template>
  <HeroSection @open-create-modal="openModal" />

  <main id="parties" class="main">
    <div class="section-head">
      <div class="section-title">
        ВІДКРИТІ ЛОБІ
        <span class="section-count">{{ partyStore.filteredParties.length }} активних</span>
      </div>
      <button class="create-party-btn" @click="openModal">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        СТВОРИТИ ЛОБІ
      </button>
    </div>

    <PartyFilters />
    <PartyGrid />
    <HowItWorks />
  </main>

  <CreatePartyModal :visible="modalOpen" @close="modalOpen = false" />
</template>

