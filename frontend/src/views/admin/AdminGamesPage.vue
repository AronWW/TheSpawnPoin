<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useAdminStore } from '../../stores/admin'

const admin = useAdminStore()
const page = ref(0)
const search = ref('')

const editModal = ref<any>(null)
const form = ref({ name: '', genre: '', releaseYear: null as number | null, imageUrl: '', maxPartySize: 5 })
const isCreating = ref(false)
const deleteTarget = ref<any>(null)

function load() {
  admin.fetchGames(page.value, 20, search.value || undefined)
}

onMounted(load)
watch(page, load)

function doSearch() { page.value = 0; load() }

function openCreate() {
  isCreating.value = true
  form.value = { name: '', genre: '', releaseYear: null, imageUrl: '', maxPartySize: 5 }
  editModal.value = true
}

function openEdit(g: any) {
  isCreating.value = false
  form.value = { name: g.name, genre: g.genre || '', releaseYear: g.releaseYear, imageUrl: g.imageUrl || '', maxPartySize: g.maxPartySize }
  editModal.value = g
}

function closeEditModal() { editModal.value = null }

async function saveGame() {
  if (isCreating.value) {
    await admin.createGame({
      name: form.value.name,
      genre: form.value.genre || undefined,
      releaseYear: form.value.releaseYear || undefined,
      imageUrl: form.value.imageUrl || undefined,
      maxPartySize: form.value.maxPartySize,
    })
  } else {
    await admin.updateGame(editModal.value.id, {
      name: form.value.name || undefined,
      genre: form.value.genre || undefined,
      releaseYear: form.value.releaseYear || undefined,
      imageUrl: form.value.imageUrl || undefined,
      maxPartySize: form.value.maxPartySize || undefined,
    })
  }
  closeEditModal()
  load()
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  await admin.deleteGame(deleteTarget.value.id)
  deleteTarget.value = null
  load()
}

function getPageData() {
  if (!admin.games) return { content: [], totalPages: 0 }
  return {
    content: admin.games.content,
    totalPages: admin.games.page?.totalPages ?? admin.games.totalPages ?? 0,
  }
}
</script>

<template>
  <div class="admin-games">
    <div class="header-row">
      <h1 class="admin-page-title">ІГРИ</h1>
      <button class="admin-btn primary" @click="openCreate">+ Додати гру</button>
    </div>

    <div class="search-row">
      <input v-model="search" class="admin-input" placeholder="Пошук за назвою..." @keyup.enter="doSearch" />
      <button class="admin-btn" @click="doSearch">Пошук</button>
    </div>

    <table class="admin-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Назва</th>
          <th>Жанр</th>
          <th>Рік</th>
          <th>Max party</th>
          <th>Дії</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="g in getPageData().content" :key="g.id">
          <td>{{ g.id }}</td>
          <td>{{ g.name }}</td>
          <td>{{ g.genre || '—' }}</td>
          <td>{{ g.releaseYear || '—' }}</td>
          <td>{{ g.maxPartySize }}</td>
          <td class="actions-cell">
            <button class="action-sm" @click="openEdit(g)">Редагувати</button>
            <button class="action-sm danger" @click="deleteTarget = g">Видалити</button>
          </td>
        </tr>
        <tr v-if="!getPageData().content.length">
          <td colspan="6" class="empty-cell">Немає ігор</td>
        </tr>
      </tbody>
    </table>

    <div v-if="getPageData().totalPages > 1" class="pagination">
      <button :disabled="page === 0" @click="page--">←</button>
      <span>{{ page + 1 }} / {{ getPageData().totalPages }}</span>
      <button :disabled="page >= getPageData().totalPages - 1" @click="page++">→</button>
    </div>

    <div v-if="editModal" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal-box">
        <h3>{{ isCreating ? 'Нова гра' : 'Редагування' }}</h3>
        <label class="modal-label">Назва *</label>
        <input v-model="form.name" class="admin-input" placeholder="Назва гри" />
        <label class="modal-label">Жанр</label>
        <input v-model="form.genre" class="admin-input" placeholder="Shooter, RPG..." />
        <label class="modal-label">Рік випуску</label>
        <input v-model.number="form.releaseYear" class="admin-input" type="number" placeholder="2024" />
        <label class="modal-label">URL зображення</label>
        <input v-model="form.imageUrl" class="admin-input" placeholder="https://..." />
        <label class="modal-label">Макс. розмір пати *</label>
        <input v-model.number="form.maxPartySize" class="admin-input" type="number" min="2" max="100" />
        <div class="modal-actions">
          <button class="admin-btn primary" @click="saveGame" :disabled="!form.name">Зберегти</button>
          <button class="admin-btn outline" @click="closeEditModal">Скасувати</button>
        </div>
      </div>
    </div>

    <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
      <div class="modal-box">
        <h3>Видалити "{{ deleteTarget.name }}"?</h3>
        <p class="modal-warn">Гра та всі пов'язані партії будуть видалені.</p>
        <div class="modal-actions">
          <button class="admin-btn danger" @click="confirmDelete">Видалити</button>
          <button class="admin-btn outline" @click="deleteTarget = null">Скасувати</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-games { padding: 0; }
.admin-page-title { font-family: var(--font-display); font-size: 2rem; color: var(--yellow); }
.header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.search-row { display: flex; gap: 8px; margin-bottom: 16px; }
.actions-cell { display: flex; gap: 6px; }
.action-sm { padding: 4px 10px; font-size: 0.8rem; border: 1px solid var(--border); background: transparent; color: var(--gray-light); cursor: pointer; font-family: var(--font-body); transition: all 0.15s; }
.action-sm:hover { border-color: var(--yellow-dim); color: var(--yellow); }
.action-sm.danger { color: var(--red); border-color: var(--red-dim); }
.action-sm.danger:hover { background: var(--red-dim); color: var(--white); }
.pagination { display: flex; align-items: center; gap: 12px; justify-content: center; margin-top: 16px; }
.pagination button { padding: 6px 14px; background: var(--panel); border: 1px solid var(--border); color: var(--gray-light); cursor: pointer; font-family: var(--font-body); }
.pagination button:disabled { opacity: 0.4; cursor: default; }
.pagination span { color: var(--gray); font-size: 0.9rem; }
.empty-cell { text-align: center; color: var(--gray); padding: 30px !important; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-box { background: var(--panel); border: 1px solid var(--border); padding: 24px; min-width: 400px; max-width: 500px; }
.modal-box h3 { font-family: var(--font-display); font-size: 1.4rem; color: var(--white); margin-bottom: 16px; }
.modal-label { display: block; color: var(--gray); font-size: 0.85rem; margin: 12px 0 4px; }
.modal-warn { color: var(--red); font-size: 0.9rem; margin-bottom: 16px; }
.modal-actions { display: flex; gap: 10px; margin-top: 20px; }
</style>

