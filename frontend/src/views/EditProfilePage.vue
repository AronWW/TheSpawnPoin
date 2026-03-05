<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../api/axios'
import type { Profile } from '../types'

const router = useRouter()
const auth = useAuthStore()

const avatarBase = 'http://localhost:8080'

const loading = ref(true)
const saving = ref(false)
const error = ref('')
const success = ref('')
const profile = ref<Profile | null>(null)

const form = ref({
  fullName: '',
  bio: '',
  birthDate: '',
  platforms: [] as string[],
  skillLevel: '',
  playStyle: '',
  languages: '',
  country: '',
  region: '',
})

const defaultAvatars = ref<string[]>([])
const currentAvatarUrl = ref<string | null>(null)
const uploadingAvatar = ref(false)
const avatarError = ref('')

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

const resolvedAvatar = computed(() => {
  const url = currentAvatarUrl.value
  if (!url) return avatarBase + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return avatarBase + url
})

onMounted(async () => {
  if (!auth.isLoggedIn) {
    router.push('/login')
    return
  }

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
      fullName: p.fullName || '',
      bio: p.bio || '',
      birthDate: p.birthDate || '',
      platforms: p.platforms || [],
      skillLevel: p.skillLevel || '',
      playStyle: p.playStyle || '',
      languages: (p.languages || []).join(', '),
      country: p.country || '',
      region: p.region || '',
    }
  } catch {
    error.value = 'Не вдалося завантажити профіль'
  } finally {
    loading.value = false
  }
})

async function saveProfile() {
  saving.value = true
  error.value = ''
  success.value = ''

  try {
    const langs = form.value.languages
      .split(',')
      .map((l) => l.trim())
      .filter(Boolean)

    const body: Record<string, unknown> = {
      fullName: form.value.fullName || null,
      bio: form.value.bio || null,
      birthDate: form.value.birthDate || null,
      platforms: form.value.platforms,
      skillLevel: form.value.skillLevel || null,
      playStyle: form.value.playStyle || null,
      languages: langs,
      country: form.value.country || null,
      region: form.value.region || null,
    }

    const { data } = await api.put<Profile>('/profile/me', body)
    profile.value = data
    success.value = 'Профіль збережено!'

    auth.fetchMe()

    setTimeout(() => { success.value = '' }, 3000)
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

  if (file.size > 2 * 1024 * 1024) {
    avatarError.value = 'Файл не повинен перевищувати 2 МБ'
    return
  }

  uploadingAvatar.value = true
  avatarError.value = ''

  try {
    const formData = new FormData()
    formData.append('file', file)

    const { data } = await api.post<Profile>('/profile/me/avatar', formData, {
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

function togglePlatform(platform: string) {
  const idx = form.value.platforms.indexOf(platform)
  if (idx === -1) {
    form.value.platforms.push(platform)
  } else {
    form.value.platforms.splice(idx, 1)
  }
}

function resolveDefaultAvatar(url: string): string {
  if (url.startsWith('http')) return url
  return avatarBase + url
}
</script>

<template>
  <div class="edit-profile-page">
    <div class="edit-container">
      <h1 class="page-title">РЕДАГУВАТИ ПРОФІЛЬ</h1>

      <div v-if="loading" class="empty-state">
        <p>Завантаження...</p>
      </div>

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
                📁 ЗАВАНТАЖИТИ ФОТО
                <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" @change="onFileSelect" hidden />
              </label>
              <div class="avatar-hint">JPEG, PNG, WebP або GIF, до 2 МБ</div>
            </div>
          </div>

          <div v-if="avatarError" class="field-error">{{ avatarError }}</div>
          <div v-if="uploadingAvatar" class="avatar-uploading">Завантаження...</div>

          <div class="default-avatars-label">Або оберіть стандартний аватар:</div>
          <div class="default-avatars-grid">
            <button
              v-for="(url, idx) in defaultAvatars"
              :key="idx"
              class="default-avatar-btn"
              :class="{ active: currentAvatarUrl === url }"
              @click="selectDefaultAvatar(idx + 1)"
              :disabled="uploadingAvatar"
            >
              <img :src="resolveDefaultAvatar(url)" :alt="'Avatar ' + (idx + 1)" />
            </button>
          </div>
        </div>

        <div class="edit-section ink-panel">
          <h2 class="section-label">ОСНОВНА ІНФОРМАЦІЯ</h2>

          <div class="form-group">
            <label class="form-label">Повне ім'я</label>
            <input v-model="form.fullName" type="text" class="form-input" maxlength="100" placeholder="Ваше ім'я" />
          </div>

          <div class="form-group">
            <label class="form-label">Про себе</label>
            <textarea v-model="form.bio" class="form-input form-textarea" maxlength="500" rows="4" placeholder="Розкажіть про себе як гравця..." />
          </div>

          <div class="form-group">
            <label class="form-label">Дата народження</label>
            <input v-model="form.birthDate" type="date" class="form-input" />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Країна</label>
              <input v-model="form.country" type="text" class="form-input" maxlength="100" placeholder="Україна" />
            </div>
            <div class="form-group">
              <label class="form-label">Регіон / Місто</label>
              <input v-model="form.region" type="text" class="form-input" maxlength="100" placeholder="Київ" />
            </div>
          </div>
        </div>

        <div class="edit-section ink-panel">
          <h2 class="section-label">ІГРОВИЙ ПРОФІЛЬ</h2>

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

          <div class="form-group">
            <label class="form-label">Платформи</label>
            <div class="platform-chips">
              <button
                v-for="p in PLATFORMS"
                :key="p.value"
                class="platform-chip"
                :class="{ selected: form.platforms.includes(p.value) }"
                @click="togglePlatform(p.value)"
                type="button"
              >
                {{ p.label }}
              </button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Мови (через кому)</label>
            <input v-model="form.languages" type="text" class="form-input" placeholder="Українська, English" />
          </div>
        </div>

        <div class="edit-actions">
          <div v-if="error" class="field-error">{{ error }}</div>
          <div v-if="success" class="field-success">{{ success }}</div>
          <div class="edit-actions-row">
            <router-link v-if="auth.user" :to="`/profile/${auth.user.id}`" class="cancel-btn">СКАСУВАТИ</router-link>
            <button class="save-btn" :disabled="saving" @click="saveProfile">
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

.edit-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 32px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 36px;
  letter-spacing: 4px;
  color: var(--yellow);
  margin-bottom: 28px;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.edit-section {
  padding: 24px 28px;
}

.section-label {
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 3px;
  color: var(--yellow);
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 8px;
}

.avatar-current {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 20px;
}

.avatar-preview {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--yellow-dim);
  box-shadow: 0 0 20px rgba(245, 197, 24, 0.15);
  flex-shrink: 0;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.upload-btn {
  display: inline-block;
  padding: 8px 18px;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 2px;
  background: var(--panel-light);
  border: 2px solid var(--border);
  color: var(--white);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.upload-btn:hover {
  border-color: var(--yellow-dim);
  background: var(--panel);
}

.avatar-hint {
  font-size: 11px;
  color: var(--gray);
  letter-spacing: 0.5px;
}

.avatar-uploading {
  font-size: 13px;
  color: var(--yellow);
  margin-bottom: 12px;
}

.default-avatars-label {
  font-size: 13px;
  color: var(--gray);
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}

.default-avatars-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.default-avatar-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 3px solid var(--border);
  background: transparent;
  cursor: pointer;
  padding: 0;
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.default-avatar-btn img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}
.default-avatar-btn:hover {
  border-color: var(--yellow-dim);
}
.default-avatar-btn.active {
  border-color: var(--yellow);
  box-shadow: 0 0 12px rgba(245, 197, 24, 0.3);
}
.default-avatar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-group {
  margin-bottom: 18px;
}

.form-label {
  display: block;
  font-size: 11px;
  color: var(--gray);
  letter-spacing: 2px;
  text-transform: uppercase;
  margin-bottom: 6px;
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
  transition: border-color 0.15s;
}
.form-input:focus {
  border-color: var(--yellow-dim);
}
.form-input::placeholder {
  color: var(--gray);
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

.platform-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.platform-chip {
  padding: 6px 16px;
  font-size: 13px;
  font-family: var(--font-body);
  letter-spacing: 0.5px;
  background: var(--dark);
  border: 2px solid var(--border);
  color: var(--gray-light);
  cursor: pointer;
  transition: all 0.15s;
}
.platform-chip:hover {
  border-color: var(--yellow-dim);
  color: var(--white);
}
.platform-chip.selected {
  background: var(--yellow-glow);
  border-color: var(--yellow);
  color: var(--yellow);
  font-weight: 600;
}

.edit-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edit-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.save-btn {
  padding: 12px 36px;
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 3px;
  background: var(--yellow);
  border: none;
  color: var(--black);
  cursor: pointer;
  transition: background 0.15s;
}
.save-btn:hover {
  background: var(--yellow-dim);
}
.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cancel-btn {
  padding: 12px 28px;
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 3px;
  background: transparent;
  border: 2px solid var(--border);
  color: var(--gray-light);
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
}
.cancel-btn:hover {
  border-color: var(--gray);
  color: var(--white);
}

.field-error {
  color: var(--red);
  font-size: 13px;
  letter-spacing: 0.5px;
}

.field-success {
  color: #2ecc71;
  font-size: 13px;
  letter-spacing: 0.5px;
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
  font-size: 24px;
  letter-spacing: 2px;
  color: var(--yellow);
}

@media (max-width: 600px) {
  .edit-container {
    padding: 24px 16px;
  }
  .avatar-current {
    flex-direction: column;
    text-align: center;
  }
  .form-row {
    grid-template-columns: 1fr;
  }
  .edit-actions-row {
    flex-direction: column;
  }
}
</style>

