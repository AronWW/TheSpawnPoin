<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../api/axios'
import type { Party } from '../types'
import PartyCard from '../components/PartyCard.vue'

const router = useRouter()
const auth = useAuthStore()

const parties = ref<Party[]>([])
const loading = ref(false)

function goToParty(party: Party) {
  router.push(`/party/${party.id}`)
}

onMounted(async () => {
  if (!auth.isLoggedIn) {
    router.push('/login')
    return
  }
  loading.value = true
  try {
    const { data } = await api.get<Party[]>('/parties/my')
    parties.value = data
  } catch {
    parties.value = []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="my-parties-page">
    <div class="my-parties-container">
      <div class="section-head">
        <div class="section-title">
          МОЇ ЛОБІ
          <span class="section-count">{{ parties.length }} активних</span>
        </div>
      </div>

      <div v-if="loading" class="empty-state">
        <div class="empty-icon">⏳</div>
        <p>Завантаження...</p>
      </div>

      <div v-else-if="parties.length === 0" class="empty-state">
        <div class="empty-icon">🎮</div>
        <h3>У тебе ще немає лобі</h3>
        <p>Приєднуйся до існуючих патей або створи свою!</p>
        <router-link to="/" class="action-btn">ЗНАЙТИ ПАТИ</router-link>
      </div>

      <div v-else class="parties-grid">
        <PartyCard
          v-for="party in parties"
          :key="party.id"
          :party="party"
          @select="goToParty"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.my-parties-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}

.my-parties-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 32px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state h3 {
  font-family: var(--font-display);
  font-size: 28px;
  letter-spacing: 2px;
  color: var(--yellow);
  margin-bottom: 8px;
}

.empty-state p {
  color: var(--gray);
  font-size: 15px;
  margin-bottom: 24px;
}

.action-btn {
  font-family: var(--font-display);
  letter-spacing: 2px;
  font-size: 15px;
  padding: 10px 28px;
  border: 2px solid var(--yellow);
  background: var(--yellow);
  color: var(--black);
  text-transform: uppercase;
  transition: background 0.15s;
  display: inline-block;
}
.action-btn:hover {
  background: var(--yellow-dim);
}

.parties-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}
</style>

