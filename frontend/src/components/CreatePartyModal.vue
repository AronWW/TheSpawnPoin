<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { usePartyStore } from '../stores/parties'
import { useGameStore } from '../stores/games'
import { useAuthStore } from '../stores/auth'
import type { CreatePartyRequest } from '../types'

defineProps<{ visible: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const router = useRouter()
const partyStore = usePartyStore()
const gameStore = useGameStore()
const auth = useAuthStore()

const PLATFORMS = [
  { value: 'PC', label: 'PC' },
  { value: 'PLAYSTATION', label: 'PlayStation' },
  { value: 'XBOX', label: 'Xbox' },
  { value: 'NINTENDO', label: 'Nintendo' },
  { value: 'MOBILE', label: 'Mobile' },
  { value: 'OTHER', label: 'Інше' },
] as const

const ALL_LANGUAGES = [
  { code: 'UA', name: 'Українська' },
  { code: 'EN', name: 'English' },
  { code: 'PL', name: 'Polski' },
  { code: 'DE', name: 'Deutsch' },
  { code: 'FR', name: 'Français' },
  { code: 'ES', name: 'Español' },
  { code: 'PT', name: 'Português' },
  { code: 'TR', name: 'Türkçe' },
  { code: 'KO', name: '한국어' },
  { code: 'ZH', name: '中文' },
  { code: 'JA', name: '日本語' },
  { code: 'IT', name: 'Italiano' },
  { code: 'NL', name: 'Nederlands' },
  { code: 'SV', name: 'Svenska' },
  { code: 'NO', name: 'Norsk' },
  { code: 'DA', name: 'Dansk' },
  { code: 'FI', name: 'Suomi' },
  { code: 'CS', name: 'Čeština' },
  { code: 'SK', name: 'Slovenčina' },
  { code: 'HU', name: 'Magyar' },
  { code: 'RO', name: 'Română' },
  { code: 'BG', name: 'Български' },
  { code: 'HR', name: 'Hrvatski' },
  { code: 'SR', name: 'Srpski' },
  { code: 'AR', name: 'العربية' },
  { code: 'HI', name: 'हिन्दी' },
  { code: 'VI', name: 'Tiếng Việt' },
  { code: 'TH', name: 'ภาษาไทย' },
  { code: 'ID', name: 'Bahasa Indonesia' },
] as const

const REGIONS = [
  { value: 'EUROPE', label: '🌍 Європа' },
  { value: 'NORTH_AMERICA', label: '🌎 Пн. Америка' },
  { value: 'SOUTH_AMERICA', label: '🌎 Пд. Америка' },
  { value: 'ASIA', label: '🌏 Азія' },
  { value: 'MIDDLE_EAST', label: '🌍 Близький Схід' },
  { value: 'AFRICA', label: '🌍 Африка' },
  { value: 'OCEANIA', label: '🌏 Океанія' },
] as const

const TAG_PRESETS = ['mic required', 'ranked', 'chill', 'tryhard', 'newbie-friendly', 'no toxicity', '18+', 'fun'] as const

const form = ref<CreatePartyRequest>({
  gameId: null,
  title: '',
  description: '',
  eventTime: null,
  platform: ['PC'],
  languages: ['UA'],
  skillLevel: null,
  playStyle: null,
  tags: [],
  region: null,
  maxMembers: 4,
})

const gameSearch = ref('')
const submitting = ref(false)
const error = ref('')
const customTag = ref('')
const langSearch = ref('')

const filteredGames = computed(() => {
  const q = gameSearch.value.toLowerCase().trim()
  if (!q) return gameStore.games.slice(0, 20)
  return gameStore.games.filter(g => g.name.toLowerCase().includes(q))
})

const filteredLanguages = computed(() => {
  const q = langSearch.value.toLowerCase().trim()
  if (!q) return ALL_LANGUAGES
  return ALL_LANGUAGES.filter(l =>
    l.code.toLowerCase().includes(q) || l.name.toLowerCase().includes(q)
  )
})

function langName(code: string): string {
  return ALL_LANGUAGES.find(l => l.code === code)?.name ?? code
}

const selectedGame = computed(() =>
  form.value.gameId ? gameStore.games.find(g => g.id === form.value.gameId) : null
)

const gameMaxPartySize = computed(() => selectedGame.value?.maxPartySize ?? 5)

const memberOptions = computed(() => {
  const max = gameMaxPartySize.value
  const opts: number[] = []
  for (let i = 2; i <= max; i++) opts.push(i)
  return opts
})

watch(() => form.value.gameId, () => {
  form.value.maxMembers = gameMaxPartySize.value
})

function togglePlatform(p: string) {
  const idx = form.value.platform.indexOf(p)
  if (idx !== -1) {
    if (form.value.platform.length > 1) form.value.platform.splice(idx, 1)
  } else {
    form.value.platform.push(p)
  }
}

function toggleLanguage(lang: string) {
  const idx = form.value.languages.indexOf(lang)
  if (idx !== -1) {
    if (form.value.languages.length > 1) form.value.languages.splice(idx, 1)
  } else {
    form.value.languages.push(lang)
  }
}

function toggleTag(tag: string) {
  const idx = form.value.tags.indexOf(tag)
  if (idx !== -1) {
    form.value.tags.splice(idx, 1)
  } else {
    if (form.value.tags.length < 5) form.value.tags.push(tag)
  }
}

function addCustomTag() {
  const tag = customTag.value.trim().toLowerCase()
  if (!tag || tag.length > 30) return
  if (form.value.tags.includes(tag)) return
  if (form.value.tags.length >= 5) return
  form.value.tags.push(tag)
  customTag.value = ''
}

function selectGame(gameId: number) {
  form.value.gameId = gameId
  gameSearch.value = gameStore.games.find(g => g.id === gameId)?.name ?? ''
}

async function submit() {
  if (!form.value.gameId) {
    error.value = 'Оберіть гру'
    return
  }
  if (!auth.isLoggedIn) {
    error.value = 'Потрібно увійти для створення лобі'
    return
  }

  error.value = ''
  submitting.value = true
  try {
    const newParty = await partyStore.createParty(form.value)
    resetForm()
    emit('close')
    router.push(`/party/${newParty.id}`)
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Помилка при створенні лобі'
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.value = {
    gameId: null,
    title: '',
    description: '',
    eventTime: null,
    platform: ['PC'],
    languages: ['UA'],
    skillLevel: null,
    playStyle: null,
    tags: [],
    region: null,
    maxMembers: 4,
  }
  gameSearch.value = ''
  customTag.value = ''
  langSearch.value = ''
  error.value = ''
}

function close() {
  resetForm()
  emit('close')
}
</script>

<template>
  <Transition name="fade">
    <div v-if="visible" class="modal-overlay" @click.self="close">
      <div class="modal">
        <div class="modal-header">
          <div class="modal-title">НОВЕ ЛОБІ</div>
          <button class="modal-close" @click="close">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="error" class="modal-error">{{ error }}</div>

          <div class="form-group">
            <label class="form-label">Гра *</label>
            <div class="game-search-wrapper">
              <input
                class="form-input"
                v-model="gameSearch"
                placeholder="🔍 Пошук гри..."
                @focus="form.gameId = null"
              />
              <div v-if="!form.gameId && gameSearch.length > 0" class="game-dropdown">
                <div
                  v-for="g in filteredGames"
                  :key="g.id"
                  class="game-option"
                  @click="selectGame(g.id)"
                >
                  <img v-if="g.imageUrl" :src="g.imageUrl" class="game-option-img" />
                  <span>{{ g.name }}</span>
                </div>
                <div v-if="filteredGames.length === 0" class="game-option disabled">Нічого не знайдено</div>
              </div>
            </div>
            <div v-if="selectedGame" class="selected-game-badge">
              ✅ {{ selectedGame.name }} (макс. {{ selectedGame.maxPartySize }} гравців)
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Назва лобі</label>
            <input
              class="form-input"
              v-model="form.title"
              placeholder="Наприклад: Ranked push, Chill evening..."
              maxlength="150"
            />
          </div>

          <div class="form-group">
            <label class="form-label">Платформа</label>
            <div class="toggle-group">
              <button
                v-for="p in PLATFORMS"
                :key="p.value"
                type="button"
                class="toggle-btn"
                :class="{ active: form.platform.includes(p.value) }"
                @click="togglePlatform(p.value)"
              >{{ p.label }}</button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Мова</label>
            <div v-if="form.languages.length" class="selected-tags" style="margin-bottom: 8px">
              <span v-for="code in form.languages" :key="code" class="tag-chip">
                {{ langName(code) }}
                <button v-if="form.languages.length > 1" class="tag-remove" @click="toggleLanguage(code)">✕</button>
              </span>
            </div>
            <input
              class="form-input"
              v-model="langSearch"
              placeholder="🔍 Пошук мови..."
              style="margin-bottom: 6px"
            />
            <div class="toggle-group">
              <button
                v-for="lang in filteredLanguages"
                :key="lang.code"
                type="button"
                class="toggle-btn lang-btn"
                :class="{ active: form.languages.includes(lang.code) }"
                @click="toggleLanguage(lang.code)"
              >
                <span class="lang-code">{{ lang.code }}</span>
                <span class="lang-name">{{ lang.name }}</span>
              </button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Регіон</label>
            <div class="toggle-group">
              <button
                v-for="r in REGIONS"
                :key="r.value"
                type="button"
                class="toggle-btn"
                :class="{ active: form.region === r.value }"
                @click="form.region = form.region === r.value ? null : r.value"
              >{{ r.label }}</button>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Рівень гри</label>
              <select class="form-input form-select-modal" v-model="form.skillLevel">
                <option :value="null">Будь-який</option>
                <option value="BEGINNER">Початківець</option>
                <option value="INTERMEDIATE">Середній</option>
                <option value="ADVANCED">Просунутий</option>
                <option value="EXPERT">Експерт</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Стиль гри</label>
              <select class="form-input form-select-modal" v-model="form.playStyle">
                <option :value="null">— Не вказано —</option>
                <option value="CASUAL">Casual</option>
                <option value="SEMI_COMPETITIVE">Semi-competitive</option>
                <option value="COMPETITIVE">Competitive</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">
              Максимум гравців
              <span v-if="selectedGame" class="form-hint">(за замовчуванням: {{ gameMaxPartySize }})</span>
            </label>
            <div v-if="!selectedGame" class="members-hint">Спочатку оберіть гру</div>
            <div v-else class="members-select">
              <button
                  v-for="n in memberOptions"
                  :key="n"
                  type="button"
                  class="members-btn"
                  :class="{ active: form.maxMembers === n }"
                  @click="form.maxMembers = n"
              >{{ n }}</button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Теги <span class="form-hint">(макс. 5)</span></label>
            <div class="toggle-group">
              <button
                v-for="tag in TAG_PRESETS"
                :key="tag"
                type="button"
                class="toggle-btn tag-btn"
                :class="{ active: form.tags.includes(tag) }"
                :disabled="!form.tags.includes(tag) && form.tags.length >= 5"
                @click="toggleTag(tag)"
              >{{ tag }}</button>
            </div>
            <div class="custom-tag-row" v-if="form.tags.length < 5">
              <input
                class="form-input custom-tag-input"
                v-model="customTag"
                placeholder="Свій тег..."
                maxlength="30"
                @keydown.enter.prevent="addCustomTag"
              />
              <button type="button" class="add-tag-btn" @click="addCustomTag">+</button>
            </div>
            <div v-if="form.tags.length" class="selected-tags">
              <span v-for="tag in form.tags" :key="tag" class="tag-chip">
                {{ tag }}
                <button class="tag-remove" @click="toggleTag(tag)">✕</button>
              </span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Опис (необов'язково)</label>
            <textarea
                class="form-textarea"
                v-model="form.description"
                placeholder="Що ти шукаєш у тімейтах? Стиль гри, вимоги, плани..."
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="close">СКАСУВАТИ</button>
          <button class="btn-submit" @click="submit" :disabled="submitting">
            {{ submitting ? 'СТВОРЕННЯ...' : '⚡ СТВОРИТИ ЛОБІ' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.modal-error {
  color: var(--red);
  font-size: 13px;
  margin-bottom: 12px;
  letter-spacing: 1px;
}

.game-search-wrapper {
  position: relative;
}

.game-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--panel);
  border: 2px solid var(--border);
  max-height: 200px;
  overflow-y: auto;
  z-index: 100;
}

.game-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 13px;
  color: var(--gray-light);
  transition: background 0.1s;
}
.game-option:hover {
  background: var(--yellow-glow);
  color: var(--yellow);
}
.game-option.disabled {
  color: var(--gray);
  cursor: default;
}
.game-option-img {
  width: 32px;
  height: 42px;
  object-fit: cover;
  border: 1px solid var(--border);
}

.selected-game-badge {
  font-size: 12px;
  color: var(--yellow);
  margin-top: 6px;
  letter-spacing: 0.5px;
}

.toggle-group {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.toggle-btn {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 1.5px;
  padding: 6px 14px;
  background: var(--dark);
  border: 2px solid var(--border);
  color: var(--gray-light);
  cursor: pointer;
  transition: all 0.15s;
}
.toggle-btn:hover:not(:disabled) {
  border-color: var(--yellow-dim);
  color: var(--white);
}
.toggle-btn.active {
  background: var(--yellow-glow);
  border-color: var(--yellow);
  color: var(--yellow);
  font-weight: 700;
}
.toggle-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.toggle-btn.tag-btn {
  font-family: var(--font-body);
  font-size: 12px;
  letter-spacing: 0.5px;
  text-transform: none;
}

.toggle-btn.lang-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  font-size: 11px;
}
.lang-code {
  font-weight: 700;
  font-size: 11px;
}
.lang-name {
  font-family: var(--font-body);
  font-size: 10px;
  opacity: 0.7;
  letter-spacing: 0;
}

.custom-tag-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.custom-tag-input {
  flex: 1;
  font-size: 13px;
}
.add-tag-btn {
  font-family: var(--font-display);
  font-size: 18px;
  width: 38px;
  background: var(--dark);
  border: 2px solid var(--border);
  color: var(--yellow);
  cursor: pointer;
  transition: all 0.15s;
}
.add-tag-btn:hover {
  background: var(--yellow-glow);
  border-color: var(--yellow);
}

.selected-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 8px;
}
.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 3px 10px;
  background: var(--yellow-glow);
  border: 1px solid var(--yellow-dim);
  color: var(--yellow);
  letter-spacing: 0.5px;
}
.tag-remove {
  background: none;
  border: none;
  color: var(--yellow);
  cursor: pointer;
  font-size: 11px;
  padding: 0 2px;
  opacity: 0.7;
  transition: opacity 0.15s;
}
.tag-remove:hover {
  opacity: 1;
}

.members-select {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.members-btn {
  width: 44px;
  height: 44px;
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 1px;
  background: var(--dark);
  border: 2px solid var(--border);
  color: var(--gray-light);
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.members-btn:hover {
  border-color: var(--yellow-dim);
  color: var(--white);
}

.members-btn.active {
  background: var(--yellow-glow);
  border-color: var(--yellow);
  color: var(--yellow);
  font-weight: 700;
}

.form-hint {
  font-size: 0.8rem;
  color: var(--gray);
  font-family: var(--font-body);
  font-weight: 400;
  letter-spacing: 0;
}

.members-hint {
  font-size: 0.85rem;
  color: var(--gray);
  padding: 10px 0;
}
</style>