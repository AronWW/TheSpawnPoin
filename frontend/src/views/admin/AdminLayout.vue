<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const links = [
  { path: '/admin', label: 'Дашборд', icon: '📊' },
  { path: '/admin/users', label: 'Користувачі', icon: '👥' },
  { path: '/admin/games', label: 'Ігри', icon: '🎮' },
  { path: '/admin/game-suggestions', label: 'Заявки на ігри', icon: '💡' },
  { path: '/admin/reports', label: 'Скарги', icon: '⚠️' },
  { path: '/admin/tickets', label: 'Підтримка', icon: '📩' },
  { path: '/admin/unban-requests', label: 'Запити на розбан', icon: '🔓' },
]

const currentPath = computed(() => route.path)
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <router-link to="/" class="sidebar-logo">THE<span>SPAWN</span>POINT</router-link>
        <div class="sidebar-badge">ADMIN</div>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="link in links" :key="link.path"
          :to="link.path"
          class="sidebar-link"
          :class="{ active: currentPath === link.path }"
        >
          <span class="sidebar-icon">{{ link.icon }}</span>
          <span class="sidebar-text">{{ link.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/" class="sidebar-link sidebar-link--back">
          <span class="sidebar-icon">←</span>
          <span class="sidebar-text">На сайт</span>
        </router-link>
      </div>
    </aside>

    <main class="admin-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100vh;
  background: var(--black);
}

.admin-sidebar {
  background: var(--panel);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  padding: 0;
  position: sticky;
  top: 0;
  height: 100vh;
}

.sidebar-header {
  padding: 20px 16px;
  border-bottom: 1px solid var(--border);
}
.sidebar-logo {
  font-family: var(--font-display);
  font-size: 1.4rem;
  color: var(--white);
  text-decoration: none;
  letter-spacing: 2px;
}
.sidebar-logo span { color: var(--yellow); }
.sidebar-badge {
  display: inline-block;
  margin-top: 6px;
  font-family: var(--font-display);
  font-size: 0.75rem;
  color: var(--yellow);
  border: 1px solid var(--yellow-dim);
  padding: 1px 10px;
  letter-spacing: 3px;
}

.sidebar-nav {
  flex: 1;
  padding: 12px 0;
  overflow-y: auto;
}

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  color: var(--gray-light);
  text-decoration: none;
  font-size: 0.95rem;
  transition: all 0.15s;
  border-left: 3px solid transparent;
}
.sidebar-link:hover { background: var(--panel-light); color: var(--white); }
.sidebar-link.active {
  color: var(--yellow);
  border-left-color: var(--yellow);
  background: var(--panel-light);
}
.sidebar-icon { font-size: 1.1rem; width: 22px; text-align: center; }

.sidebar-footer {
  border-top: 1px solid var(--border);
  padding: 8px 0;
}
.sidebar-link--back { color: var(--gray); }
.sidebar-link--back:hover { color: var(--white); }

.admin-main {
  padding: 32px 40px;
  overflow-y: auto;
}

:deep(.admin-table) {
  width: 100%;
  border-collapse: collapse;
}
:deep(.admin-table th),
:deep(.admin-table td) {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid var(--border);
  font-size: 0.9rem;
}
:deep(.admin-table th) {
  color: var(--gray);
  text-transform: uppercase;
  font-size: 0.75rem;
  letter-spacing: 1px;
  font-weight: 500;
}
:deep(.admin-table td) { color: var(--gray-light); }
:deep(.admin-table tbody tr:hover) { background: var(--panel-light); }

:deep(.admin-input) {
  padding: 8px 12px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  color: var(--white);
  font-family: var(--font-body);
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.15s;
}
:deep(.admin-input:focus) { border-color: var(--yellow-dim); }
:deep(.admin-textarea) { resize: vertical; width: 100%; }

:deep(.admin-select) {
  padding: 8px 12px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  color: var(--white);
  font-family: var(--font-body);
  font-size: 0.95rem;
  outline: none;
  cursor: pointer;
}
:deep(.admin-select:focus) { border-color: var(--yellow-dim); }

:deep(.admin-btn) {
  padding: 8px 18px;
  border: 1px solid var(--border);
  background: var(--panel);
  color: var(--gray-light);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.9rem;
  transition: all 0.15s;
}
:deep(.admin-btn:hover) { border-color: var(--yellow-dim); color: var(--white); }
:deep(.admin-btn.primary) { background: var(--yellow-dim); color: var(--black); border-color: var(--yellow); }
:deep(.admin-btn.primary:hover) { background: var(--yellow); }
:deep(.admin-btn.danger) { background: var(--red-dim); color: var(--white); border-color: var(--red); }
:deep(.admin-btn.danger:hover) { background: var(--red); }
:deep(.admin-btn.outline) { background: transparent; }
:deep(.admin-btn:disabled) { opacity: 0.5; cursor: default; }
</style>


