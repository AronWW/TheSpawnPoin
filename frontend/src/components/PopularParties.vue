<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { usePartyStore } from '../stores/parties'
import { skillLabel, timeAgo } from '../utils/helpers'
import type { Party } from '../types'

const router = useRouter()
const partyStore = usePartyStore()

const popularParties = computed(() => partyStore.parties.slice(0, 8))

function selectParty(party: Party) {
  router.push(`/party/${party.id}`)
}
</script>

<template>
  <section class="popular-parties">
    <div class="section-head">
      <div class="section-title">
        ПОПУЛЯРНІ ЛОБІ
        <span class="section-count">{{ partyStore.parties.length }} активних</span>
      </div>
      <router-link to="/search-parties" class="view-all-btn">
        ВСІ ЛОБІ →
      </router-link>
    </div>

    <div class="pp-grid">
      <div
        v-for="party in popularParties"
        :key="party.id"
        class="pp-card"
        @click="selectParty(party)"
      >
        <div class="pp-card-cover">
          <img
            v-if="party.gameImageUrl"
            :src="party.gameImageUrl"
            :alt="party.gameName"
            class="pp-cover-img"
          />
          <div v-else class="pp-cover-ph">🎮</div>
          <div class="pp-cover-fade"></div>
          <div class="pp-cover-info">
            <span class="pp-game-name">{{ party.gameName }}</span>
          </div>
        </div>

        <div class="pp-body">
          <div class="pp-host">
            <span class="pp-host-label">Хост:</span>
            <span class="pp-host-name">{{ party.creatorDisplayName }}</span>
          </div>

          <p class="pp-desc">{{ party.description || 'Шукаємо гравців!' }}</p>

          <div class="pp-slots-row">
            <div class="pp-slots-bar">
              <div
                class="pp-slots-fill"
                :style="{ width: (party.currentMembers / party.maxMembers * 100) + '%' }"
              ></div>
            </div>
            <span class="pp-slots-text">
              <strong>{{ party.currentMembers }}</strong>/{{ party.maxMembers }}
            </span>
          </div>

          <div class="pp-tags">
            <span
              v-if="party.skillLevel"
              class="pp-tag skill"
              :class="party.skillLevel.toLowerCase()"
            >{{ skillLabel(party.skillLevel) }}</span>
            <span
              v-for="p in party.platform.slice(0, 2)"
              :key="p"
              class="pp-tag platform"
            >{{ p }}</span>
            <span class="pp-time">{{ timeAgo(party.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!popularParties.length" class="pp-empty">
      <span>Зараз немає активних лобі — створи перше!</span>
    </div>
  </section>
</template>

<style scoped>
.popular-parties {
  margin-bottom: 48px;
}

.view-all-btn {
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 3px;
  color: var(--yellow);
  background: transparent;
  border: 2px solid var(--yellow-dim);
  padding: 8px 24px;
  transition: all 0.15s;
  display: inline-block;
}
.view-all-btn:hover {
  background: var(--yellow);
  color: var(--black);
  border-color: var(--yellow);
}

.pp-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.pp-card {
  background: var(--panel);
  border: 2px solid var(--border);
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s, transform 0.15s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
}
.pp-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--yellow), transparent);
  opacity: 0;
  transition: opacity 0.2s;
}
.pp-card:hover {
  border-color: var(--yellow-dim);
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}
.pp-card:hover::before {
  opacity: 1;
}

.pp-card-cover {
  position: relative;
  width: 100%;
  height: 100px;
  overflow: hidden;
  background: var(--dark);
  flex-shrink: 0;
}

.pp-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
  filter: brightness(0.7);
}
.pp-card:hover .pp-cover-img {
  transform: scale(1.08);
  filter: brightness(0.85);
}

.pp-cover-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  background: var(--panel-light);
}

.pp-cover-fade {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 60%;
  background: linear-gradient(to top, var(--panel) 0%, transparent 100%);
  pointer-events: none;
}

.pp-cover-info {
  position: absolute;
  bottom: 8px;
  left: 12px;
  right: 12px;
  z-index: 2;
}

.pp-game-name {
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 1.5px;
  color: var(--yellow);
  text-shadow: 0 1px 4px rgba(0,0,0,0.8);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.pp-body {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.pp-host {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
.pp-host-label {
  color: var(--gray);
  letter-spacing: 1px;
}
.pp-host-name {
  color: var(--gray-light);
  font-weight: 600;
}

.pp-desc {
  font-size: 12px;
  color: var(--gray);
  line-height: 1.4;
  font-style: italic;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}

.pp-slots-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pp-slots-bar {
  flex: 1;
  height: 4px;
  background: var(--border);
  overflow: hidden;
}

.pp-slots-fill {
  height: 100%;
  background: var(--yellow);
  transition: width 0.3s;
  min-width: 2px;
}

.pp-slots-text {
  font-size: 11px;
  color: var(--gray);
  letter-spacing: 1px;
  flex-shrink: 0;
}
.pp-slots-text strong {
  color: var(--yellow);
}

.pp-tags {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: auto;
}

.pp-tag {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  padding: 2px 7px;
  border: 1px solid var(--border);
  color: var(--gray);
}
.pp-tag.skill.beginner     { border-color: #27ae6066; color: #2ecc71; }
.pp-tag.skill.intermediate { border-color: rgba(245,197,24,0.4); color: var(--yellow); }
.pp-tag.skill.advanced     { border-color: #e67e2266; color: #f39c12; }
.pp-tag.skill.expert       { border-color: rgba(192,57,43,0.4); color: var(--red); }

.pp-tag.platform {
  border-color: var(--yellow-dim);
  color: var(--yellow-dim);
  background: rgba(245,197,24,0.04);
}

.pp-time {
  margin-left: auto;
  font-size: 10px;
  color: var(--gray);
  letter-spacing: 0.5px;
  font-style: italic;
}

.pp-empty {
  text-align: center;
  padding: 40px 20px;
  color: var(--gray);
  font-size: 14px;
  border: 2px dashed var(--border);
}

@media (max-width: 1200px) {
  .pp-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 900px) {
  .pp-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 550px) {
  .pp-grid { grid-template-columns: 1fr; }
}
</style>

