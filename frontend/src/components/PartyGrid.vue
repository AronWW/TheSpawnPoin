<script setup lang="ts">
import { useRouter } from 'vue-router'
import { usePartyStore } from '../stores/parties'
import PartyCard from './PartyCard.vue'
import type { Party } from '../types'

const router = useRouter()
const partyStore = usePartyStore()

function selectParty(party: Party) {
  router.push(`/party/${party.id}`)
}
</script>

<template>
  <div class="party-grid">
    <TransitionGroup name="card">
      <template v-if="partyStore.filteredParties.length">
        <PartyCard
          v-for="party in partyStore.filteredParties"
          :key="party.id"
          :party="party"
          @select="selectParty"
        />
      </template>
      <div v-else key="empty" class="empty-state">
        <div class="empty-state-icon">🎮</div>
        <div class="empty-state-title">ЛОБІ НЕ ЗНАЙДЕНО</div>
        <div class="empty-state-sub">Спробуй змінити фільтри або створи власне лобі</div>
      </div>
    </TransitionGroup>
  </div>
</template>

