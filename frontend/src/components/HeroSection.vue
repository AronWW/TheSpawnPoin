<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { generateSpeedLines } from '../utils/helpers'
import { usePartyStore } from '../stores/parties'
import { useAuthStore } from '../stores/auth'
import { useGameStore } from '../stores/games'

defineEmits<{
  (e: 'open-create-modal'): void
}>()

const router = useRouter()
const partyStore = usePartyStore()
const auth = useAuthStore()
const gameStore = useGameStore()
const speedLines = generateSpeedLines(12)

const featuredParty = computed(() => partyStore.parties[0] ?? null)
const myParty = computed(() => partyStore.myParties[0] ?? null)
const heroParty = computed(() => {
  if (auth.isLoggedIn && myParty.value) return myParty.value
  return featuredParty.value
})
const isMyParty = computed(() => auth.isLoggedIn && myParty.value !== null)

const joinError = ref('')
const joining = ref(false)

async function handleJoin() {
  if (!heroParty.value) return
  joinError.value = ''
  joining.value = true
  try {
    await partyStore.joinParty(heroParty.value.id)
  } catch (e: any) {
    joinError.value = e.message || 'Не вдалося приєднатись'
    setTimeout(() => { joinError.value = '' }, 4000)
  } finally {
    joining.value = false
  }
}

function goToParty() {
  if (heroParty.value) {
    router.push(`/party/${heroParty.value.id}`)
  }
}
</script>

<template>
  <section class="hero halftone">
    <div class="hero-bg"></div>

    <div class="speed-lines">
      <div
        v-for="line in speedLines"
        :key="line.id"
        class="speed-line"
        :style="{
          top: line.top + '%',
          width: line.width + 'px',
          animationDuration: line.dur + 's',
          animationDelay: line.delay + 's',
        }"
      ></div>
    </div>

    <div class="hero-inner">
      <div>
        <div class="hero-kicker">🎮 Gaming Community Platform</div>
        <h1 class="hero-title">
          <span class="shadow-word">ЗНАЙДИ</span>
          <span class="accent">СВІЙ СКВАД</span>
        </h1>
        <p class="hero-sub">
          Платформа для гравців, які хочуть більше ніж просто грати. Знаходь тімейтів,
          організовуй лобі, грай на своєму рівні.
        </p>
        <div class="hero-cta-row">
          <button class="btn-primary" @click="$emit('open-create-modal')">⚡ СТВОРИТИ ЛОБІ</button>
          <router-link to="/search-parties" class="btn-secondary">ПЕРЕГЛЯНУТИ ЛОБІ</router-link>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-num">{{ partyStore.parties.length || '—' }}</div>
            <div class="stat-label">Відкриті лобі</div>
          </div>
          <div class="stat-item">
            <div class="stat-num">{{ gameStore.games.length || '25+' }}</div>
            <div class="stat-label">Популярних ігор</div>
          </div>
        </div>
      </div>

      <div v-if="heroParty" class="hero-card" @click="isMyParty ? goToParty() : undefined" :style="isMyParty ? 'cursor:pointer' : ''">
        <div class="party-card-accent"></div>
        <div v-if="isMyParty" class="hero-card-label my-party-label">⚔ МОЄ ЛОБІ</div>
        <div class="hero-card-header">
          <img
            v-if="heroParty.gameImageUrl"
            :src="heroParty.gameImageUrl"
            :alt="heroParty.gameName"
            class="game-thumb"
          />
          <div v-else class="game-thumb-placeholder">🎮</div>
          <div>
            <div class="card-game-name">{{ heroParty.gameName }}</div>
            <div class="card-meta">
              {{ heroParty.skillLevel ?? 'OPEN' }} ·
              {{ heroParty.playStyle ?? 'CASUAL' }}
            </div>
          </div>
        </div>
        <div class="hero-card-body">
          <p class="card-desc">
            {{ heroParty.description || 'Шукаємо гравців!' }}
          </p>

          <div class="members-row">
            <div
              v-for="member in (heroParty.members || [])"
              :key="member.userId"
              class="member-avatar online"
              :title="member.displayName"
            >
              {{ member.displayName.substring(0, 2).toUpperCase() }}
            </div>
            <div
              v-for="i in (heroParty.maxMembers - heroParty.currentMembers)"
              :key="'empty-' + i"
              class="members-empty"
            >
              +
            </div>
          </div>

          <div class="card-tags">
            <span
              v-for="p in heroParty.platform"
              :key="p"
              class="tag yellow"
            >
              {{ p }}
            </span>
            <span v-if="heroParty.status === 'OPEN'" class="tag green">🟢 Відкрите</span>
            <span v-else-if="heroParty.status === 'IN_GAME'" class="tag blue">🎮 В грі</span>
            <span v-for="lang in (heroParty.languages || [])" :key="lang" class="tag">
              {{ lang }}
            </span>
          </div>

          <template v-if="isMyParty">
            <button class="join-btn" @click.stop="goToParty">
              ПЕРЕЙТИ ДО ЛОБІ →
            </button>
          </template>
          <template v-else>
            <div v-if="joinError" class="hero-join-error">{{ joinError }}</div>
            <button class="join-btn" @click="handleJoin" :disabled="joining">
              {{ joining ? 'ПРИЄДНАННЯ...' : 'ПРИЄДНАТИСЬ ДО ЛОБІ →' }}
            </button>
          </template>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero-card-label {
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 3px;
  padding: 8px 24px;
  text-align: center;
  border-bottom: 2px solid var(--border);
}
.my-party-label {
  background: rgba(245, 197, 24, 0.08);
  color: var(--yellow);
}
</style>

