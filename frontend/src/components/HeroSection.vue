<script setup lang="ts">
import { computed, ref } from 'vue'
import { generateSpeedLines } from '../utils/helpers'
import { usePartyStore } from '../stores/parties'

defineEmits<{
  (e: 'open-create-modal'): void
}>()

const partyStore = usePartyStore()
const speedLines = generateSpeedLines(12)
const featuredParty = computed(() => partyStore.parties[0] ?? null)

const joinError = ref('')
const joining = ref(false)

async function handleJoin() {
  if (!featuredParty.value) return
  joinError.value = ''
  joining.value = true
  try {
    await partyStore.joinParty(featuredParty.value.id)
  } catch (e: any) {
    joinError.value = e.message || 'Не вдалося приєднатись'
    setTimeout(() => { joinError.value = '' }, 4000)
  } finally {
    joining.value = false
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
          <a href="#parties" class="btn-secondary">ПЕРЕГЛЯНУТИ ЛОБІ</a>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-num">{{ partyStore.parties.length || '—' }}</div>
            <div class="stat-label">Відкриті лобі</div>
          </div>
          <div class="stat-item">
            <div class="stat-num">25+</div>
            <div class="stat-label">Популярних ігор</div>
          </div>
        </div>
      </div>

      <div v-if="featuredParty" class="hero-card">
        <div class="party-card-accent"></div>
        <div class="hero-card-header">
          <img
            v-if="featuredParty.gameImageUrl"
            :src="featuredParty.gameImageUrl"
            :alt="featuredParty.gameName"
            class="game-thumb"
          />
          <div v-else class="game-thumb-placeholder">🎮</div>
          <div>
            <div class="card-game-name">{{ featuredParty.gameName }}</div>
            <div class="card-meta">
              {{ featuredParty.skillLevel ?? 'OPEN' }} ·
              {{ featuredParty.playStyle ?? 'CASUAL' }}
            </div>
          </div>
        </div>
        <div class="hero-card-body">
          <p class="card-desc">
            {{ featuredParty.description || 'Шукаємо гравців!' }}
          </p>

          <div class="members-row">
            <div
              v-for="member in (featuredParty.members || [])"
              :key="member.userId"
              class="member-avatar online"
              :title="member.displayName"
            >
              {{ member.displayName.substring(0, 2).toUpperCase() }}
            </div>
            <div
              v-for="i in (featuredParty.maxMembers - featuredParty.currentMembers)"
              :key="'empty-' + i"
              class="members-empty"
            >
              +
            </div>
          </div>

          <div class="card-tags">
            <span
              v-for="p in featuredParty.platform"
              :key="p"
              class="tag yellow"
            >
              {{ p }}
            </span>
            <span v-if="featuredParty.isOpen" class="tag green">🟢 Відкрите</span>
            <span v-if="featuredParty.language" class="tag">
              {{ featuredParty.language }}
            </span>
          </div>

          <div v-if="joinError" class="hero-join-error">{{ joinError }}</div>
          <button class="join-btn" @click="handleJoin" :disabled="joining">
            {{ joining ? 'ПРИЄДНАННЯ...' : 'ПРИЄДНАТИСЬ ДО ЛОБІ →' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

