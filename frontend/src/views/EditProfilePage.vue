<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { API_BASE_URL } from '../config'
import api from '../api/axios'
import type { Profile } from '../types'
import { ALL_COUNTRIES } from '../utils/countries'
import { useToast } from '../composables/useToast'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const toast = useToast()

const loading = ref(true)
const saving = ref(false)
const error = ref('')
const success = ref('')
const profile = ref<Profile | null>(null)

const form = ref({
  displayName: '',
  fullName: '',
  bio: '',
  birthDate: '',
  platforms: [] as string[],
  skillLevel: '',
  playStyle: '',
  languages: [] as string[],
  country: '',
  region: '',
  discord: '',
  steam: '',
  twitch: '',
  xbox: '',
  playstation: '',
  nintendo: '',
  bannerUrl: '' as string,
})

const BANNER_PRESETS = [
  { key: 'banner-1', label: 'Фіолетовий', gradient: 'linear-gradient(135deg, #1a0a2e 0%, #3d1a78 50%, #1a0a2e 100%)' },
  { key: 'banner-2', label: 'Морський', gradient: 'linear-gradient(135deg, #0a1a2e 0%, #0e4d6e 50%, #0a1a2e 100%)' },
  { key: 'banner-3', label: 'Червоний', gradient: 'linear-gradient(135deg, #2e0a0a 0%, #8b1a1a 50%, #2e0a0a 100%)' },
  { key: 'banner-4', label: 'Золотий', gradient: 'linear-gradient(135deg, #2e2a0a 0%, #8b7a1a 50%, #2e2a0a 100%)' },
  { key: 'banner-5', label: 'Смарагдовий', gradient: 'linear-gradient(135deg, #0a2e1a 0%, #1a6e3d 50%, #0a2e1a 100%)' },
]

const defaultAvatars = ref<string[]>([])
const currentAvatarUrl = ref<string | null>(null)
const uploadingAvatar = ref(false)
const avatarError = ref('')
const langSearch = ref('')
const countrySearch = ref('')
const countryDropdownOpen = ref(false)

const PLATFORMS = [
  { value: 'PC', label: 'PC' },
  { value: 'PLAYSTATION', label: 'PlayStation' },
  { value: 'XBOX', label: 'Xbox' },
  { value: 'NINTENDO', label: 'Nintendo' },
  { value: 'MOBILE', label: 'Mobile' },
  { value: 'OTHER', label: 'Інше' },
]

const SKILL_LEVELS = [
  { value: '', label: '— Не вказано —' },
  { value: 'BEGINNER', label: 'Початківець' },
  { value: 'INTERMEDIATE', label: 'Середній' },
  { value: 'ADVANCED', label: 'Просунутий' },
  { value: 'EXPERT', label: 'Експерт' },
]

const PLAY_STYLES = [
  { value: '', label: '— Не вказано —' },
  { value: 'CASUAL', label: 'Казуальний' },
  { value: 'SEMI_COMPETITIVE', label: 'Напів-змагальний' },
  { value: 'COMPETITIVE', label: 'Змагальний' },
]

const REGIONS = [
  { value: '', label: '— Не вказано —' },
  { value: 'EUROPE', label: '🌍 Європа' },
  { value: 'NORTH_AMERICA', label: '🌎 Північна Америка' },
  { value: 'SOUTH_AMERICA', label: '🌎 Південна Америка' },
  { value: 'ASIA', label: '🌏 Азія' },
  { value: 'MIDDLE_EAST', label: '🌍 Близький Схід' },
  { value: 'AFRICA', label: '🌍 Африка' },
  { value: 'OCEANIA', label: '🌏 Океанія' },
]

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
]

const resolvedAvatar = computed(() => {
  const url = currentAvatarUrl.value
  if (!url) return API_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return API_BASE_URL + url
})

const displayNameError = computed(() => {
  const v = form.value.displayName.trim()
  if (!v) return ''
  if (v.length < 2) return 'Мінімум 2 символи'
  if (v.length > 30) return 'Максимум 30 символів'
  if (!/^[a-zA-Z0-9_\- ]+$/.test(v)) return 'Лише латиниця, цифри, пробіл, _ та -'
  return ''
})

const filteredLanguages = computed(() => {
  const q = langSearch.value.toLowerCase()
  return ALL_LANGUAGES.filter(
      l => !form.value.languages.includes(l.code) &&
          (l.code.toLowerCase().includes(q) || l.name.toLowerCase().includes(q))
  )
})

const selectedLanguageNames = computed(() =>
    form.value.languages.map(code => {
      const found = ALL_LANGUAGES.find(l => l.code === code)
      return found ? { code, name: found.name } : { code, name: code }
    })
)

const filteredCountries = computed(() => {
  const q = countrySearch.value.toLowerCase().trim()
  if (!q) return ALL_COUNTRIES.slice(0, 20)
  return ALL_COUNTRIES.filter(c =>
    c.native.toLowerCase().includes(q) ||
    c.en.toLowerCase().includes(q) ||
    c.code.toLowerCase() === q
  ).slice(0, 15)
})

const countryDisplayValue = computed(() => {
  if (!form.value.country) return ''
  const found = ALL_COUNTRIES.find(c =>
    c.en === form.value.country || c.native === form.value.country
  )
  return found ? `${found.native} (${found.en})` : form.value.country
})

function selectCountry(c: typeof ALL_COUNTRIES[0]) {
  form.value.country = c.en
  countrySearch.value = ''
  countryDropdownOpen.value = false
}

function clearCountry() {
  form.value.country = ''
  countrySearch.value = ''
  countryDropdownOpen.value = false
}

function onCountryFocus() {
  countryDropdownOpen.value = true
  countrySearch.value = ''
}

function onCountryBlur() {
  setTimeout(() => { countryDropdownOpen.value = false }, 200)
}

onMounted(async () => {
  if (!auth.isLoggedIn) { router.push('/login'); return }
  try {
    const [profileRes, avatarsRes] = await Promise.all([
      api.get<Profile>('/profile/me'),
      api.get<string[]>('/profile/avatars/defaults'),
    ])
    profile.value = profileRes.data
    defaultAvatars.value = avatarsRes.data
    currentAvatarUrl.value = profileRes.data.avatarUrl
    const p = profileRes.data
    form.value = {
      displayName: p.displayName || '',
      fullName: p.fullName || '',
      bio: p.bio || '',
      birthDate: p.birthDate || '',
      platforms: p.platforms || [],
      skillLevel: p.skillLevel || '',
      playStyle: p.playStyle || '',
      languages: p.languages || [],
      country: p.country || '',
      region: p.region || '',
      discord: p.discord || '',
      steam: p.steam || '',
      twitch: p.twitch || '',
      xbox: p.xbox || '',
      playstation: p.playstation || '',
      nintendo: p.nintendo || '',
      bannerUrl: p.bannerUrl || '',
    }
  } catch {
    error.value = 'Не вдалося завантажити профіль'
  } finally {
    loading.value = false
    if (route.query.section === 'banner') {
      await nextTick()
      setTimeout(() => {
        document.getElementById('banner-section')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }, 100)
    }
  }
})

async function saveProfile() {
  if (displayNameError.value) return
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const body = {
      displayName:  form.value.displayName.trim() || null,
      fullName:     form.value.fullName || null,
      bio:          form.value.bio || null,
      birthDate:    form.value.birthDate || null,
      platforms:    form.value.platforms,
      skillLevel:   form.value.skillLevel || null,
      playStyle:    form.value.playStyle || null,
      languages:    form.value.languages,
      country:      form.value.country || null,
      region:       form.value.region || null,
      discord:      form.value.discord || null,
      steam:        form.value.steam || null,
      twitch:       form.value.twitch || null,
      xbox:         form.value.xbox || null,
      playstation:  form.value.playstation || null,
      nintendo:     form.value.nintendo || null,
      bannerUrl:    form.value.bannerUrl,
    }
    const { data } = await api.put<Profile>('/profile/me', body)
    profile.value = data
    auth.fetchMe()
    toast.show('Профіль успішно збережено ✓', 'success')
    router.push(`/profile/${auth.user!.id}`)
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Помилка збереження'
  } finally {
    saving.value = false
  }
}

async function selectDefaultAvatar(index: number) {
  uploadingAvatar.value = true
  avatarError.value = ''
  try {
    const { data } = await api.post<Profile>('/profile/me/avatar/default', { index })
    currentAvatarUrl.value = data.avatarUrl
    profile.value = data
    auth.fetchMe()
  } catch {
    avatarError.value = 'Не вдалося обрати аватар'
  } finally {
    uploadingAvatar.value = false
  }
}

async function onFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) { avatarError.value = 'Файл не повинен перевищувати 2 МБ'; return }
  uploadingAvatar.value = true
  avatarError.value = ''
  try {
    const fd = new FormData()
    fd.append('file', file)
    const { data } = await api.post<Profile>('/profile/me/avatar', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    currentAvatarUrl.value = data.avatarUrl
    profile.value = data
    auth.fetchMe()
  } catch {
    avatarError.value = 'Не вдалося завантажити аватар'
  } finally {
    uploadingAvatar.value = false
    input.value = ''
  }
}

function togglePlatform(p: string) {
  const idx = form.value.platforms.indexOf(p)
  if (idx === -1) form.value.platforms.push(p)
  else form.value.platforms.splice(idx, 1)
}

function toggleLanguage(code: string) {
  const idx = form.value.languages.indexOf(code)
  if (idx === -1) form.value.languages.push(code)
  else form.value.languages.splice(idx, 1)
}

function resolveDefaultAvatar(url: string) {
  return url.startsWith('http') ? url : API_BASE_URL + url
}
</script>

<template>
  <div class="edit-profile-page">
    <div class="edit-container">
      <h1 class="page-title">РЕДАГУВАТИ ПРОФІЛЬ</h1>

      <div v-if="loading" class="empty-state"><p>Завантаження...</p></div>

      <div v-else-if="error && !profile" class="empty-state">
        <div class="empty-icon">😕</div>
        <h3>{{ error }}</h3>
      </div>

      <div v-else class="edit-form">

        <div class="edit-section ink-panel">
          <h2 class="section-label">АВАТАР</h2>
          <div class="avatar-current">
            <img :src="resolvedAvatar" alt="Аватар" class="avatar-preview" />
            <div class="avatar-actions">
              <label class="upload-btn">
                ЗАВАНТАЖИТИ ФОТО
                <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" @change="onFileSelect" hidden />
              </label>
              <div class="avatar-hint">JPEG, PNG, WebP або GIF · до 2 МБ</div>
            </div>
          </div>
          <div v-if="avatarError" class="field-error">{{ avatarError }}</div>
          <div v-if="uploadingAvatar" class="uploading-text">Завантаження...</div>
          <div class="default-avatars-label">Або оберіть стандартний аватар:</div>
          <div class="default-avatars-grid">
            <button
                v-for="(url, idx) in defaultAvatars" :key="idx"
                class="default-avatar-btn"
                :class="{ active: currentAvatarUrl === url }"
                @click="selectDefaultAvatar(idx + 1)"
                :disabled="uploadingAvatar"
            >
              <img :src="resolveDefaultAvatar(url)" :alt="'Avatar ' + (idx + 1)" />
            </button>
          </div>
        </div>

        <div id="banner-section" class="edit-section ink-panel">
          <h2 class="section-label">БАНЕР ПРОФІЛЮ</h2>
          <p class="banner-hint">Оберіть банер, який буде відображатися на вашому профілі</p>
          <div class="banner-grid">
            <button
              class="banner-option"
              :class="{ active: !form.bannerUrl }"
              @click="form.bannerUrl = ''"
              type="button"
            >
              <div class="banner-preview banner-default">
                <div class="banner-preview-dots"></div>
              </div>
              <span class="banner-option-label">За замовч.</span>
            </button>
            <button
              v-for="b in BANNER_PRESETS"
              :key="b.key"
              class="banner-option"
              :class="{ active: form.bannerUrl === b.key }"
              @click="form.bannerUrl = b.key"
              type="button"
            >
              <div class="banner-preview" :style="{ background: b.gradient }"></div>
              <span class="banner-option-label">{{ b.label }}</span>
            </button>
          </div>
        </div>

        <div class="edit-section ink-panel">
          <h2 class="section-label">ОСНОВНА ІНФОРМАЦІЯ</h2>

          <div class="form-group">
            <label class="form-label">Нікнейм</label>
            <input
                v-model="form.displayName"
                type="text"
                class="form-input"
                :class="{ 'input-error': displayNameError }"
                maxlength="30"
                placeholder="GhostSniper"
            />
            <div v-if="displayNameError" class="field-error">{{ displayNameError }}</div>
            <div v-else class="field-hint">Латиниця, цифри, пробіл, _ та -</div>
          </div>

          <div class="form-group">
            <label class="form-label">Повне ім'я</label>
            <input v-model="form.fullName" type="text" class="form-input" maxlength="100" placeholder="Ваше ім'я" />
          </div>

          <div class="form-group">
            <label class="form-label">Про себе</label>
            <textarea v-model="form.bio" class="form-input form-textarea" maxlength="500" rows="4" placeholder="Розкажіть про себе як гравця..." />
            <div class="field-hint char-count">{{ form.bio.length }}/500</div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Дата народження</label>
              <input v-model="form.birthDate" type="date" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">Країна</label>
              <div class="country-autocomplete">
                <div v-if="form.country && !countryDropdownOpen" class="country-selected" @click="onCountryFocus">
                  <span class="country-selected-text">{{ countryDisplayValue }}</span>
                  <button class="country-clear" @click.stop="clearCountry" type="button">✕</button>
                </div>
                <input
                  v-else
                  v-model="countrySearch"
                  type="text"
                  class="form-input"
                  placeholder="Пошук країни..."
                  autocomplete="off"
                  @focus="onCountryFocus"
                  @blur="onCountryBlur"
                />
                <div v-if="countryDropdownOpen" class="country-dropdown">
                  <button
                    v-for="c in filteredCountries"
                    :key="c.code"
                    class="country-option"
                    type="button"
                    @mousedown.prevent="selectCountry(c)"
                  >
                    <span class="country-native">{{ c.native }}</span>
                    <span class="country-en">({{ c.en }})</span>
                  </button>
                  <div v-if="filteredCountries.length === 0" class="country-empty">
                    Країну не знайдено
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Регіон</label>
            <select v-model="form.region" class="form-input">
              <option v-for="r in REGIONS" :key="r.value" :value="r.value">{{ r.label }}</option>
            </select>
          </div>
        </div>

        <div class="edit-section ink-panel">
          <h2 class="section-label">ІГРОВИЙ ПРОФІЛЬ</h2>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Рівень гри</label>
              <select v-model="form.skillLevel" class="form-input">
                <option v-for="s in SKILL_LEVELS" :key="s.value" :value="s.value">{{ s.label }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Стиль гри</label>
              <select v-model="form.playStyle" class="form-input">
                <option v-for="ps in PLAY_STYLES" :key="ps.value" :value="ps.value">{{ ps.label }}</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Платформи</label>
            <div class="chip-group">
              <button
                  v-for="p in PLATFORMS" :key="p.value"
                  class="chip"
                  :class="{ selected: form.platforms.includes(p.value) }"
                  @click="togglePlatform(p.value)"
                  type="button"
              >{{ p.label }}</button>
            </div>
          </div>

          <div class="form-group">
            <div class="form-label-row">
              <label class="form-label">Мови спілкування</label>
              <button
                v-if="form.languages.length > 1"
                class="clear-all-btn"
                @click="form.languages = []"
                type="button"
                title="Очистити всі"
              >🗑 Очистити</button>
            </div>

            <div v-if="selectedLanguageNames.length" class="selected-langs">
              <span
                  v-for="lang in selectedLanguageNames" :key="lang.code"
                  class="chip selected lang-chip"
              >
                <span class="lang-code">{{ lang.code }}</span>
                <span class="lang-name">{{ lang.name }}</span>
                <button class="chip-remove" @click="toggleLanguage(lang.code)" type="button">×</button>
              </span>
            </div>

            <div class="lang-search-wrap">
              <input
                  v-model="langSearch"
                  class="form-input lang-search"
                  placeholder="🔍 Пошук мови..."
              />
            </div>

            <div class="lang-grid">
              <button
                  v-for="lang in filteredLanguages" :key="lang.code"
                  class="lang-btn"
                  @click="toggleLanguage(lang.code)"
                  type="button"
              >
                <span class="lang-btn-code">{{ lang.code }}</span>
                <span class="lang-btn-name">{{ lang.name }}</span>
              </button>
            </div>
            <div v-if="filteredLanguages.length === 0 && langSearch" class="field-hint">
              Мову "{{ langSearch }}" не знайдено
            </div>
          </div>
        </div>

        <div class="edit-section ink-panel">
          <h2 class="section-label">СОЦІАЛЬНІ МЕРЕЖІ</h2>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label"><span class="social-icon">🎮</span> Discord</label>
              <input v-model="form.discord" type="text" class="form-input" maxlength="100" placeholder="username або user#1234" />
              <div class="field-hint">Нік — інші зможуть скопіювати</div>
            </div>
            <div class="form-group">
              <label class="form-label"><span class="social-icon">📺</span> Twitch</label>
              <input v-model="form.twitch" type="text" class="form-input" maxlength="200" placeholder="https://twitch.tv/yourname" />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label"><span class="social-icon">🕹️</span> Steam</label>
            <input v-model="form.steam" type="text" class="form-input" maxlength="200" placeholder="https://steamcommunity.com/id/yourname" />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label"><span class="social-icon">🟢</span> Xbox Gamertag</label>
              <input v-model="form.xbox" type="text" class="form-input" maxlength="200" placeholder="YourGamertag" />
            </div>
            <div class="form-group">
              <label class="form-label"><span class="social-icon">🔵</span> PlayStation ID</label>
              <input v-model="form.playstation" type="text" class="form-input" maxlength="200" placeholder="YourPSN_ID" />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label"><span class="social-icon">🔴</span> Nintendo Friend Code</label>
            <input v-model="form.nintendo" type="text" class="form-input" maxlength="200" placeholder="SW-XXXX-XXXX-XXXX" />
            <div class="field-hint">Код друга — інші зможуть скопіювати</div>
          </div>
        </div>

        <div class="edit-actions">
          <div v-if="error" class="field-error">{{ error }}</div>
          <div v-if="success" class="field-success">{{ success }}</div>
          <div class="edit-actions-row">
            <router-link v-if="auth.user" :to="`/profile/${auth.user.id}`" class="cancel-btn">СКАСУВАТИ</router-link>
            <button class="save-btn" :disabled="saving || !!displayNameError" @click="saveProfile">
              {{ saving ? 'ЗБЕРЕЖЕННЯ...' : 'ЗБЕРЕГТИ' }}
            </button>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.edit-profile-page {
  padding-top: 64px;
  min-height: 100vh;
  background: var(--black);
}

/* ── HEADER AREA ── */
.edit-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 0 24px 60px;
}
.page-title {
  font-family: var(--font-display);
  font-size: 42px;
  letter-spacing: 4px;
  color: var(--yellow);
  margin: 0;
  padding: 36px 0 32px;
  position: relative;
  line-height: 1;
}
.page-title::after {
  content: '';
  position: absolute;
  bottom: 16px;
  left: 0;
  width: 80px;
  height: 2px;
  background: linear-gradient(90deg, var(--yellow), transparent);
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── PANELS — cinematic style matching .va-panel ── */
.edit-section {
  background: var(--panel);
  border: 2px solid var(--border);
  padding: 24px 28px;
  position: relative;
  overflow: hidden;
  transition: border-color 0.25s, box-shadow 0.25s;
}
.edit-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--yellow-dim), transparent 70%);
  opacity: 0;
  transition: opacity 0.25s;
}
.edit-section::after {
  content: '';
  position: absolute;
  inset: 3px;
  border: 1px solid rgba(245, 197, 24, 0.04);
  pointer-events: none;
}
.edit-section:hover {
  border-color: rgba(245, 197, 24, 0.25);
  box-shadow: 0 4px 30px rgba(245, 197, 24, 0.03), 0 0 0 1px rgba(245, 197, 24, 0.05);
}
.edit-section:hover::before {
  opacity: 1;
}
.section-label {
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 3px;
  color: var(--yellow);
  margin-bottom: 18px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
  text-transform: uppercase;
  position: relative;
}

/* ── AVATAR ── */
.avatar-current {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 20px;
  padding: 16px;
  background: var(--dark);
  border: 1px solid var(--border);
}
.avatar-preview {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--yellow-dim);
  box-shadow: 0 0 20px rgba(245, 197, 24, 0.15), 0 4px 16px rgba(0, 0, 0, 0.4);
  flex-shrink: 0;
  outline: 2px solid var(--panel);
}
.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 20px;
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 2px;
  background: transparent;
  border: 2px solid var(--yellow-dim);
  color: var(--yellow);
  cursor: pointer;
  transition: all 0.15s;
  text-transform: uppercase;
}
.upload-btn:hover {
  background: var(--yellow);
  color: var(--black);
  border-color: var(--yellow);
}
.avatar-hint {
  font-size: 10px;
  color: var(--gray);
  letter-spacing: 1px;
  text-transform: uppercase;
}
.uploading-text {
  font-size: 13px;
  color: var(--yellow);
  margin-bottom: 12px;
  letter-spacing: 1px;
}
.default-avatars-label {
  font-size: 10px;
  color: var(--gray);
  margin-bottom: 12px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
}
.default-avatars-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.default-avatar-btn {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: 3px solid var(--border);
  background: transparent;
  cursor: pointer;
  padding: 0;
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
}
.default-avatar-btn img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}
.default-avatar-btn:hover {
  border-color: var(--yellow-dim);
  transform: scale(1.08);
}
.default-avatar-btn.active {
  border-color: var(--yellow);
  box-shadow: 0 0 14px rgba(245, 197, 24, 0.35);
  transform: scale(1.08);
}
.default-avatar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ── BANNER PICKER ── */
.banner-hint {
  font-size: 12px;
  color: var(--gray);
  margin-bottom: 16px;
  letter-spacing: 0.5px;
}
.banner-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.banner-option {
  background: none;
  border: 2px solid var(--border);
  padding: 0;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}
.banner-option::after {
  content: '';
  position: absolute;
  top: 4px;
  right: 4px;
  width: 16px;
  height: 16px;
  border: 2px solid var(--border);
  background: var(--dark);
  opacity: 0;
  transition: opacity 0.2s;
}
.banner-option.active::after {
  opacity: 1;
  border-color: var(--yellow);
  background: var(--yellow);
  box-shadow: 0 0 6px rgba(245, 197, 24, 0.4);
}
.banner-option:hover {
  border-color: var(--yellow-dim);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}
.banner-option.active {
  border-color: var(--yellow);
  box-shadow: 0 0 16px rgba(245, 197, 24, 0.2);
}
.banner-preview {
  width: 100%;
  height: 60px;
}
.banner-default {
  background: var(--black);
  position: relative;
  overflow: hidden;
}
.banner-preview-dots {
  width: 100%;
  height: 100%;
  background-image: radial-gradient(circle, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 4px 4px;
}
.banner-option-label {
  padding: 6px 0;
  font-family: var(--font-body);
  font-size: 10px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: var(--gray);
  text-align: center;
  background: var(--panel);
  border-top: 1px solid var(--border);
  transition: color 0.15s;
}
.banner-option.active .banner-option-label {
  color: var(--yellow);
}

/* ── FORM CONTROLS ── */
.form-group {
  margin-bottom: 18px;
}
.form-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  color: var(--gray);
  letter-spacing: 2px;
  text-transform: uppercase;
  margin-bottom: 6px;
  font-family: var(--font-body);
}
.form-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.form-label-row .form-label {
  margin-bottom: 0;
}
.clear-all-btn {
  background: none;
  border: 1px solid var(--border);
  color: var(--gray);
  font-size: 10px;
  letter-spacing: 1px;
  padding: 3px 10px;
  cursor: pointer;
  font-family: var(--font-body);
  transition: all 0.15s;
}
.clear-all-btn:hover {
  color: var(--red);
  border-color: rgba(192,57,43,0.4);
}
.social-icon {
  font-size: 14px;
}
.form-input {
  width: 100%;
  background: var(--dark);
  border: 2px solid var(--border);
  padding: 10px 14px;
  font-size: 14px;
  color: var(--white);
  font-family: var(--font-body);
  letter-spacing: 0.5px;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  box-sizing: border-box;
}
.form-input:focus {
  border-color: var(--yellow-dim);
  box-shadow: 0 0 0 1px rgba(245, 197, 24, 0.1), 0 2px 12px rgba(245, 197, 24, 0.04);
}
.form-input::placeholder {
  color: var(--gray);
}
.form-input.input-error {
  border-color: var(--red);
}
.form-textarea {
  resize: vertical;
  min-height: 80px;
}
select.form-input {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%238a8a9a' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
}
select.form-input option {
  background: var(--dark);
  color: var(--white);
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.field-hint {
  font-size: 11px;
  color: var(--gray);
  margin-top: 4px;
}
.char-count {
  text-align: right;
}

/* ── CHIPS ── */
.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  padding: 6px 16px;
  font-size: 13px;
  font-family: var(--font-body);
  letter-spacing: 0.5px;
  background: var(--dark);
  border: 2px solid var(--border);
  color: var(--gray-light);
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  gap: 6px;
}
.chip:hover {
  border-color: var(--yellow-dim);
  color: var(--white);
}
.chip.selected {
  background: var(--yellow-glow);
  border-color: var(--yellow);
  color: var(--yellow);
  font-weight: 600;
}
.chip-remove {
  background: none;
  border: none;
  color: var(--yellow);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  padding: 0 0 0 2px;
  opacity: 0.7;
  transition: opacity 0.1s;
}
.chip-remove:hover {
  opacity: 1;
}

/* ── LANGUAGES ── */
.selected-langs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.lang-chip {
  padding: 5px 10px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.lang-code {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 1px;
  opacity: 0.8;
}
.lang-name {
  font-size: 12px;
}
.lang-search-wrap {
  margin-bottom: 10px;
}
.lang-search {
  padding: 8px 12px;
  font-size: 13px;
}
.lang-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 6px;
  max-height: 220px;
  overflow-y: auto;
  padding-right: 4px;
}
.lang-grid::-webkit-scrollbar {
  width: 4px;
}
.lang-grid::-webkit-scrollbar-track {
  background: var(--dark);
}
.lang-grid::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 2px;
}
.lang-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px;
  background: var(--dark);
  border: 1px solid var(--border);
  color: var(--gray-light);
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
}
.lang-btn:hover {
  border-color: var(--yellow-dim);
  color: var(--white);
  background: var(--panel-light);
}
.lang-btn-code {
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--yellow);
  opacity: 0.8;
  min-width: 24px;
}
.lang-btn-name {
  font-size: 12px;
}

/* ── ACTIONS BAR — styled like ProfilePage hero bar ── */
.edit-actions {
  background: var(--panel);
  border: 2px solid var(--border);
  padding: 20px 28px;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.edit-actions::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--yellow), rgba(245, 197, 24, 0.3) 60%, transparent);
}
.edit-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.save-btn {
  padding: 12px 40px;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 3px;
  background: var(--yellow);
  border: 2px solid var(--yellow);
  color: var(--black);
  cursor: pointer;
  transition: all 0.2s;
  text-transform: uppercase;
  position: relative;
  overflow: hidden;
}
.save-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.4s;
}
.save-btn:hover::before {
  left: 100%;
}
.save-btn:hover {
  box-shadow: 0 0 24px rgba(245, 197, 24, 0.3);
}
.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.save-btn:disabled:hover::before {
  left: -100%;
}
.cancel-btn {
  padding: 12px 28px;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 3px;
  background: transparent;
  border: 2px solid var(--border);
  color: var(--gray-light);
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  text-transform: uppercase;
}
.cancel-btn:hover {
  border-color: var(--white);
  color: var(--white);
}

.field-error {
  color: var(--red);
  font-size: 13px;
}
.field-success {
  color: #2ecc71;
  font-size: 13px;
}

/* ── EMPTY STATE ── */
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
  font-size: 24px;
  letter-spacing: 2px;
  color: var(--yellow);
}

/* ── COUNTRY AUTOCOMPLETE ── */
.country-autocomplete {
  position: relative;
}
.country-selected {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  background: var(--dark);
  border: 2px solid var(--border);
  padding: 10px 14px;
  cursor: pointer;
  transition: border-color 0.15s;
  box-sizing: border-box;
}
.country-selected:hover {
  border-color: var(--yellow-dim);
}
.country-selected-text {
  font-size: 14px;
  color: var(--white);
  font-family: var(--font-body);
  letter-spacing: 0.5px;
}
.country-clear {
  background: none;
  border: none;
  color: var(--gray);
  font-size: 14px;
  cursor: pointer;
  padding: 0 0 0 8px;
  transition: color 0.15s;
  line-height: 1;
}
.country-clear:hover {
  color: var(--red);
}
.country-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  max-height: 220px;
  overflow-y: auto;
  background: var(--dark);
  border: 2px solid var(--border);
  border-top: none;
  z-index: 50;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}
.country-dropdown::-webkit-scrollbar {
  width: 4px;
}
.country-dropdown::-webkit-scrollbar-track {
  background: var(--dark);
}
.country-dropdown::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 2px;
}
.country-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 9px 14px;
  background: none;
  border: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  cursor: pointer;
  text-align: left;
  transition: background 0.1s;
}
.country-option:hover {
  background: rgba(245, 197, 24, 0.06);
}
.country-native {
  font-size: 13px;
  color: var(--white);
  font-family: var(--font-body);
  font-weight: 600;
}
.country-en {
  font-size: 12px;
  color: var(--gray);
  font-family: var(--font-body);
}
.country-empty {
  padding: 14px;
  text-align: center;
  color: var(--gray);
  font-size: 12px;
}

/* ── RESPONSIVE ── */
@media (max-width: 768px) {
  .edit-container {
    padding: 0 16px 40px;
  }
  .page-title {
    font-size: 28px;
    letter-spacing: 3px;
    padding: 24px 0 24px;
  }
  .edit-section {
    padding: 20px 18px;
  }
  .edit-actions {
    padding: 16px 18px;
  }
  .section-label {
    font-size: 13px;
    letter-spacing: 2px;
  }
  .banner-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }
  .banner-preview {
    height: 48px;
  }
  .form-row {
    grid-template-columns: 1fr;
  }
  .avatar-current {
    flex-direction: column;
    text-align: center;
    padding: 16px 12px;
  }
}
@media (max-width: 480px) {
  .edit-container {
    padding: 0 10px 32px;
  }
  .page-title {
    font-size: 22px;
    letter-spacing: 2px;
    padding: 20px 0 20px;
  }
  .edit-section {
    padding: 16px 14px;
  }
  .edit-actions {
    padding: 14px 14px;
  }
  .section-label {
    font-size: 12px;
  }
  .avatar-current {
    flex-direction: column;
    text-align: center;
  }
  .avatar-preview {
    width: 80px;
    height: 80px;
  }
  .banner-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 6px;
  }
  .banner-preview {
    height: 40px;
  }
  .banner-option-label {
    font-size: 9px;
  }
  .edit-actions-row {
    flex-direction: column;
  }
  .save-btn,
  .cancel-btn {
    width: 100%;
    justify-content: center;
    text-align: center;
  }
  .lang-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  }
  .chip {
    padding: 5px 12px;
    font-size: 12px;
  }
}
</style>