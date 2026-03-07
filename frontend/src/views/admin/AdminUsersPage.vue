<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAdminStore } from '../../stores/admin'

const admin = useAdminStore()
const route = useRoute()

const tab = ref<'all' | 'banned'>((route.query.tab as any) || 'all')
const search = ref('')
const page = ref(0)

const banModalUser = ref<any>(null)
const banReason = ref('')
const deleteModalUser = ref<any>(null)

function load() {
  if (tab.value === 'banned') {
    admin.fetchBannedUsers(page.value)
  } else {
    admin.fetchUsers(page.value, 20, search.value || undefined)
  }
}

onMounted(load)
watch([tab, page], load)

function doSearch() { page.value = 0; load() }

function openBanModal(user: any) { banModalUser.value = user; banReason.value = '' }
function closeBanModal() { banModalUser.value = null }
async function confirmBan() {
  if (!banModalUser.value) return
  await admin.banUser(banModalUser.value.id, banReason.value || undefined)
  closeBanModal()
  load()
}

async function doUnban(id: number) {
  await admin.unbanUser(id)
  load()
}

function openDeleteModal(user: any) { deleteModalUser.value = user }
function closeDeleteModal() { deleteModalUser.value = null }
async function confirmDelete() {
  if (!deleteModalUser.value) return
  await admin.deleteUser(deleteModalUser.value.id)
  closeDeleteModal()
  load()
}

function getPageData() {
  const data = tab.value === 'banned' ? admin.bannedUsers : admin.users
  if (!data) return { content: [], totalPages: 0 }
  return {
    content: data.content,
    totalPages: data.page?.totalPages ?? data.totalPages ?? 0,
  }
}
</script>

<template>
  <div class="admin-users">
    <h1 class="admin-page-title">КОРИСТУВАЧІ</h1>

    <div class="tab-row">
      <button class="tab-btn" :class="{ active: tab === 'all' }" @click="tab = 'all'; page = 0">Усі</button>
      <button class="tab-btn" :class="{ active: tab === 'banned' }" @click="tab = 'banned'; page = 0">Забанені</button>
    </div>

    <div v-if="tab === 'all'" class="search-row">
      <input v-model="search" class="admin-input" placeholder="Пошук за ім'ям або email..." @keyup.enter="doSearch" />
      <button class="admin-btn" @click="doSearch">Пошук</button>
    </div>

    <table class="admin-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Ім'я</th>
          <th>Email</th>
          <th>Роль</th>
          <th>Статус</th>
          <th>Бан</th>
          <th>Дії</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="u in getPageData().content" :key="u.id">
          <td>{{ u.id }}</td>
          <td>{{ u.displayName }}</td>
          <td>{{ u.email }}</td>
          <td><span class="role-badge" :class="{ admin: u.role === 'ADMIN' }">{{ u.role }}</span></td>
          <td>{{ u.status }}</td>
          <td>
            <span v-if="u.banned" class="ban-badge" :title="u.banReason || ''">🚫 Забанений</span>
            <span v-else class="ok-badge">✓</span>
          </td>
          <td class="actions-cell">
            <template v-if="u.role !== 'ADMIN'">
              <button v-if="!u.banned" class="action-sm danger" @click="openBanModal(u)">Бан</button>
              <button v-else class="action-sm success" @click="doUnban(u.id)">Розбан</button>
              <button class="action-sm danger-outline" @click="openDeleteModal(u)">Видалити</button>
            </template>
            <span v-else class="admin-self-label">—</span>
          </td>
        </tr>
        <tr v-if="!getPageData().content.length">
          <td colspan="7" class="empty-cell">Немає даних</td>
        </tr>
      </tbody>
    </table>

    <div v-if="getPageData().totalPages > 1" class="pagination">
      <button :disabled="page === 0" @click="page--">←</button>
      <span>{{ page + 1 }} / {{ getPageData().totalPages }}</span>
      <button :disabled="page >= getPageData().totalPages - 1" @click="page++">→</button>
    </div>

    <div v-if="banModalUser" class="modal-overlay" @click.self="closeBanModal">
      <div class="modal-box">
        <h3>Забанити {{ banModalUser.displayName }}?</h3>
        <label class="modal-label">Причина (необов'язково)</label>
        <textarea v-model="banReason" class="admin-input admin-textarea" rows="3" placeholder="Причина бану..."></textarea>
        <div class="modal-actions">
          <button class="admin-btn danger" @click="confirmBan">Забанити</button>
          <button class="admin-btn outline" @click="closeBanModal">Скасувати</button>
        </div>
      </div>
    </div>

    <div v-if="deleteModalUser" class="modal-overlay" @click.self="closeDeleteModal">
      <div class="modal-box">
        <h3>Видалити {{ deleteModalUser.displayName }}?</h3>
        <p class="modal-warn">Цю дію неможливо скасувати. Всі дані користувача будуть видалені.</p>
        <div class="modal-actions">
          <button class="admin-btn danger" @click="confirmDelete">Видалити</button>
          <button class="admin-btn outline" @click="closeDeleteModal">Скасувати</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-users { padding: 0; }
.admin-page-title {
  font-family: var(--font-display);
  font-size: 2rem;
  color: var(--yellow);
  margin-bottom: 20px;
}
.tab-row { display: flex; gap: 8px; margin-bottom: 16px; }
.tab-btn {
  padding: 8px 20px;
  background: var(--panel);
  border: 1px solid var(--border);
  color: var(--gray);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.95rem;
  transition: all 0.15s;
}
.tab-btn.active { border-color: var(--yellow); color: var(--yellow); }

.search-row { display: flex; gap: 8px; margin-bottom: 16px; }

.role-badge { font-size: 0.8rem; padding: 2px 8px; border: 1px solid var(--border); }
.role-badge.admin { color: var(--yellow); border-color: var(--yellow-dim); }

.ban-badge { color: var(--red); font-size: 0.85rem; cursor: help; }
.ok-badge { color: #4caf50; }

.actions-cell { display: flex; gap: 6px; }
.action-sm {
  padding: 4px 10px;
  font-size: 0.8rem;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--gray-light);
  cursor: pointer;
  font-family: var(--font-body);
  transition: all 0.15s;
}
.action-sm.danger { color: var(--red); border-color: var(--red-dim); }
.action-sm.danger:hover { background: var(--red-dim); color: var(--white); }
.action-sm.success { color: #4caf50; border-color: #388e3c; }
.action-sm.success:hover { background: #388e3c; color: var(--white); }
.action-sm.danger-outline { color: var(--red); border-color: var(--red-dim); }
.action-sm.danger-outline:hover { background: rgba(192, 57, 43, 0.15); }
.admin-self-label { color: var(--gray); font-size: 0.85rem; }

.pagination { display: flex; align-items: center; gap: 12px; justify-content: center; margin-top: 16px; }
.pagination button {
  padding: 6px 14px;
  background: var(--panel);
  border: 1px solid var(--border);
  color: var(--gray-light);
  cursor: pointer;
  font-family: var(--font-body);
}
.pagination button:disabled { opacity: 0.4; cursor: default; }
.pagination span { color: var(--gray); font-size: 0.9rem; }

.empty-cell { text-align: center; color: var(--gray); padding: 30px !important; }

.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000;
}
.modal-box {
  background: var(--panel);
  border: 1px solid var(--border);
  padding: 24px;
  min-width: 380px;
  max-width: 460px;
}
.modal-box h3 { font-family: var(--font-display); font-size: 1.4rem; color: var(--white); margin-bottom: 12px; }
.modal-label { display: block; color: var(--gray); font-size: 0.85rem; margin-bottom: 6px; }
.modal-warn { color: var(--red); font-size: 0.9rem; margin-bottom: 16px; }
.modal-actions { display: flex; gap: 10px; margin-top: 16px; }
</style>

