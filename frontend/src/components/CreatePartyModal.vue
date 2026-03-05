<script setup lang="ts">
import { ref } from 'vue'
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

const form = ref<CreatePartyRequest>({
  gameId: null,
  description: '',
  eventTime: null,
  platform: ['PC'],
  language: 'UA',
  skillLevel: 'INTERMEDIATE',
  playStyle: null,
})

const selectedPlatform = ref('PC')
const submitting = ref(false)
const error = ref('')

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
    const newParty = await partyStore.createParty({
      ...form.value,
      platform: [selectedPlatform.value],
    })
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
    description: '',
    eventTime: null,
    platform: ['PC'],
    language: 'UA',
    skillLevel: 'INTERMEDIATE',
    playStyle: null,
  }
  selectedPlatform.value = 'PC'
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
          <div v-if="error" style="color:var(--red);font-size:13px;margin-bottom:12px;letter-spacing:1px">
            {{ error }}
          </div>

          <div class="form-group">
            <label class="form-label">Гра *</label>
            <select class="form-input form-select-modal" v-model="form.gameId">
              <option :value="null">— Оберіть гру —</option>
              <option v-for="g in gameStore.games" :key="g.id" :value="g.id">{{ g.name }}</option>
            </select>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Мова</label>
              <select class="form-input form-select-modal" v-model="form.language">
                <option>UA</option>
                <option>EN</option>
                <option>UA/EN</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Платформа</label>
              <select class="form-input form-select-modal" v-model="selectedPlatform">
                <option>PC</option>
                <option>PLAYSTATION</option>
                <option>XBOX</option>
                <option>NINTENDO</option>
                <option>MOBILE</option>
              </select>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Рівень гри</label>
              <select class="form-input form-select-modal" v-model="form.skillLevel">
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
            <label class="form-label">Опис (необов'язково)</label>
            <textarea class="form-textarea" v-model="form.description" placeholder="Що ти шукаєш у тімейтах? Стиль гри, вимоги, плани..."></textarea>
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

