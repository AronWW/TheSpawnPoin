<script setup lang="ts">
import { onMounted } from 'vue'
import { useAdminStore } from '../../stores/admin'
import { useRouter } from 'vue-router'

const admin = useAdminStore()
const router = useRouter()

onMounted(() => {
  admin.fetchDashboard()
})
</script>

<template>
  <div class="admin-dashboard">
    <h1 class="admin-page-title">ДАШБОРД</h1>

    <div v-if="admin.dashboard" class="stats-grid">
      <div class="stat-card" @click="router.push('/admin/users')">
        <div class="stat-value">{{ admin.dashboard.totalUsers }}</div>
        <div class="stat-label">Користувачів</div>
      </div>
      <div class="stat-card stat-card--danger" @click="router.push('/admin/users?tab=banned')">
        <div class="stat-value">{{ admin.dashboard.bannedUsers }}</div>
        <div class="stat-label">Забанено</div>
      </div>
      <div class="stat-card" @click="router.push('/admin/games')">
        <div class="stat-value">{{ admin.dashboard.totalGames }}</div>
        <div class="stat-label">Ігор</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ admin.dashboard.openParties }}</div>
        <div class="stat-label">Активних лобі</div>
      </div>
      <div class="stat-card stat-card--warn" @click="router.push('/admin/game-suggestions')">
        <div class="stat-value">{{ admin.dashboard.pendingSuggestions }}</div>
        <div class="stat-label">Заявок на ігри</div>
      </div>
      <div class="stat-card stat-card--danger" @click="router.push('/admin/reports')">
        <div class="stat-value">{{ admin.dashboard.openReports }}</div>
        <div class="stat-label">Відкритих скарг</div>
      </div>
      <div class="stat-card stat-card--warn" @click="router.push('/admin/tickets')">
        <div class="stat-value">{{ admin.dashboard.openTickets }}</div>
        <div class="stat-label">Відкритих тікетів</div>
      </div>
      <div class="stat-card stat-card--warn" @click="router.push('/admin/unban-requests')">
        <div class="stat-value">{{ admin.dashboard.pendingUnbanRequests }}</div>
        <div class="stat-label">Запитів на розбан</div>
      </div>
    </div>

    <div v-else class="loading-text">Завантаження...</div>
  </div>
</template>

<style scoped>
.admin-dashboard { padding: 0; }
.admin-page-title {
  font-family: var(--font-display);
  font-size: 2rem;
  color: var(--yellow);
  margin-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.stat-card {
  background: var(--panel);
  border: 1px solid var(--border);
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, transform 0.15s;
}
.stat-card:hover {
  border-color: var(--yellow-dim);
  transform: translateY(-2px);
}
.stat-card--danger { border-left: 3px solid var(--red); }
.stat-card--warn { border-left: 3px solid var(--yellow); }

.stat-value {
  font-family: var(--font-display);
  font-size: 2.2rem;
  color: var(--white);
}
.stat-label {
  font-size: 0.85rem;
  color: var(--gray);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-top: 4px;
}

.loading-text { color: var(--gray); padding: 40px 0; text-align: center; }
</style>

