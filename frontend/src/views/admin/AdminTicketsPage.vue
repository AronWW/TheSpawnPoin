<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { useAdminStore } from '../../stores/admin'
import { API_BASE_URL } from '../../config'

const admin = useAdminStore()
const page = ref(0)
const statusFilter = ref('OPEN')

const selectedTicket = ref<any>(null)
const replyText = ref('')
const messagesEl = ref<HTMLElement | null>(null)

function load() {
  admin.fetchTickets(page.value, 20, statusFilter.value || undefined)
}

onMounted(load)
watch([page, statusFilter], load)

async function selectTicket(t: any) {
  selectedTicket.value = t
  await admin.fetchTicketMessages(t.id)
  nextTick(() => scrollToBottom())
}

function scrollToBottom() {
  if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
}

async function sendReply() {
  if (!replyText.value.trim() || !selectedTicket.value) return
  await admin.replyToTicket(selectedTicket.value.id, replyText.value)
  replyText.value = ''
  nextTick(() => scrollToBottom())
}

async function closeTicket() {
  if (!selectedTicket.value) return
  await admin.changeTicketStatus(selectedTicket.value.id, 'CLOSED')
  selectedTicket.value.status = 'CLOSED'
  load()
}

function resolveAvatar(url: string | null): string {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return API_BASE_URL + url
}

function getPageData() {
  if (!admin.tickets) return { content: [], totalPages: 0 }
  return {
    content: admin.tickets.content,
    totalPages: admin.tickets.page?.totalPages ?? admin.tickets.totalPages ?? 0,
  }
}
</script>

<template>
  <div class="admin-tickets">
    <h1 class="admin-page-title">ПІДТРИМКА</h1>

    <div class="tickets-layout">
      <div class="tickets-list-panel">
        <div class="filter-row">
          <select v-model="statusFilter" class="admin-select" @change="page = 0">
            <option value="OPEN">Відкриті</option>
            <option value="IN_PROGRESS">В роботі</option>
            <option value="CLOSED">Закриті</option>
            <option value="">Усі</option>
          </select>
        </div>

        <div
          v-for="t in getPageData().content" :key="t.id"
          class="ticket-item"
          :class="{ selected: selectedTicket?.id === t.id }"
          @click="selectTicket(t)"
        >
          <div class="ticket-item-header">
            <span class="ticket-subject">{{ t.subject }}</span>
            <span class="ticket-status-dot" :class="t.status.toLowerCase().replace('_', '-')"></span>
          </div>
          <div class="ticket-item-meta">
            {{ t.userDisplayName }} · {{ new Date(t.createdAt).toLocaleDateString('uk-UA') }}
          </div>
        </div>

        <div v-if="!getPageData().content.length" class="empty-tickets">Немає тікетів</div>

        <div v-if="getPageData().totalPages > 1" class="pagination">
          <button :disabled="page === 0" @click="page--">←</button>
          <span>{{ page + 1 }} / {{ getPageData().totalPages }}</span>
          <button :disabled="page >= getPageData().totalPages - 1" @click="page++">→</button>
        </div>
      </div>

      <div class="ticket-chat-panel">
        <template v-if="selectedTicket">
          <div class="chat-header">
            <div>
              <strong>{{ selectedTicket.subject }}</strong>
              <span class="chat-header-user">від {{ selectedTicket.userDisplayName }}</span>
            </div>
            <button v-if="selectedTicket.status !== 'CLOSED'" class="action-sm danger" @click="closeTicket">Закрити тікет</button>
            <span v-else class="closed-label">ЗАКРИТО</span>
          </div>

          <div class="messages-list" ref="messagesEl">
            <div v-for="m in admin.ticketMessages" :key="m.id" class="msg" :class="{ 'msg-admin': m.admin }">
              <img v-if="m.senderAvatarUrl" :src="resolveAvatar(m.senderAvatarUrl)" class="msg-avatar" />
              <div class="msg-avatar-placeholder" v-else>{{ m.senderDisplayName?.charAt(0) }}</div>
              <div class="msg-body">
                <div class="msg-sender">
                  {{ m.senderDisplayName }}
                  <span v-if="m.admin" class="admin-label">АДМІН</span>
                </div>
                <div class="msg-content">{{ m.content }}</div>
                <div class="msg-time">{{ new Date(m.sentAt).toLocaleString('uk-UA') }}</div>
              </div>
            </div>
          </div>

          <div v-if="selectedTicket.status !== 'CLOSED'" class="reply-bar">
            <input v-model="replyText" class="admin-input reply-input" placeholder="Написати відповідь..." @keyup.enter="sendReply" />
            <button class="admin-btn primary" @click="sendReply" :disabled="!replyText.trim()">Надіслати</button>
          </div>
        </template>
        <div v-else class="empty-chat">Оберіть тікет зі списку</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-tickets { padding: 0; }
.admin-page-title { font-family: var(--font-display); font-size: 2rem; color: var(--yellow); margin-bottom: 20px; }

.tickets-layout { display: grid; grid-template-columns: 340px 1fr; gap: 16px; height: calc(100vh - 220px); }

.tickets-list-panel {
  border: 1px solid var(--border);
  background: var(--panel);
  overflow-y: auto;
  padding: 12px;
}
.filter-row { margin-bottom: 12px; }

.ticket-item {
  padding: 10px 12px;
  border: 1px solid var(--border);
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s;
}
.ticket-item:hover { border-color: var(--yellow-dim); }
.ticket-item.selected { border-color: var(--yellow); background: var(--panel-light); }
.ticket-item-header { display: flex; justify-content: space-between; align-items: center; }
.ticket-subject { font-size: 0.95rem; color: var(--white); }
.ticket-status-dot {
  width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
}
.ticket-status-dot.open { background: var(--yellow); }
.ticket-status-dot.in-progress { background: #2196f3; }
.ticket-status-dot.closed { background: var(--gray); }
.ticket-item-meta { font-size: 0.8rem; color: var(--gray); margin-top: 4px; }

.empty-tickets { text-align: center; color: var(--gray); padding: 30px 0; }

.ticket-chat-panel {
  border: 1px solid var(--border);
  background: var(--panel);
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}
.chat-header strong { color: var(--white); font-family: var(--font-display); font-size: 1.1rem; }
.chat-header-user { color: var(--gray); font-size: 0.85rem; margin-left: 8px; }
.closed-label { color: var(--gray); font-size: 0.8rem; text-transform: uppercase; border: 1px solid var(--border); padding: 2px 8px; }

.messages-list { flex: 1; overflow-y: auto; padding: 16px; }
.msg { display: flex; gap: 10px; margin-bottom: 16px; }
.msg-admin { }
.msg-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.msg-avatar-placeholder {
  width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0;
  background: var(--panel-light); border: 1px solid var(--border);
  display: flex; align-items: center; justify-content: center;
  color: var(--gray); font-size: 0.9rem;
}
.msg-body { flex: 1; }
.msg-sender { font-size: 0.85rem; color: var(--gray-light); margin-bottom: 2px; }
.admin-label { color: var(--yellow); font-size: 0.7rem; border: 1px solid var(--yellow-dim); padding: 0 5px; margin-left: 6px; }
.msg-content { color: var(--white); font-size: 0.95rem; line-height: 1.4; }
.msg-time { font-size: 0.75rem; color: var(--gray); margin-top: 4px; }

.reply-bar { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid var(--border); }
.reply-input { flex: 1; }

.empty-chat { display: flex; align-items: center; justify-content: center; height: 100%; color: var(--gray); }

.action-sm { padding: 4px 10px; font-size: 0.8rem; border: 1px solid var(--border); background: transparent; cursor: pointer; font-family: var(--font-body); transition: all 0.15s; }
.action-sm.danger { color: var(--red); border-color: var(--red-dim); }
.action-sm.danger:hover { background: var(--red-dim); color: var(--white); }

.pagination { display: flex; align-items: center; gap: 12px; justify-content: center; margin-top: 12px; }
.pagination button { padding: 4px 10px; background: var(--panel); border: 1px solid var(--border); color: var(--gray-light); cursor: pointer; font-family: var(--font-body); font-size: 0.85rem; }
.pagination button:disabled { opacity: 0.4; cursor: default; }
.pagination span { color: var(--gray); font-size: 0.85rem; }
</style>

