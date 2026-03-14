<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useSupportStore } from '../stores/support'
import { useAuthStore } from '../stores/auth'
import { API_BASE_URL } from '../config'

const support = useSupportStore()
const auth = useAuthStore()

const selectedTicket = ref<any>(null)
const replyText = ref('')
const messagesEl = ref<HTMLElement | null>(null)

const showNewTicket = ref(false)
const newSubject = ref('')
const newMessage = ref('')

onMounted(() => {
  support.fetchMyTickets()
})

async function selectTicket(t: any) {
  selectedTicket.value = t
  await support.fetchTicketMessages(t.id)
  nextTick(() => scrollToBottom())
}

function scrollToBottom() {
  if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
}

async function sendReply() {
  if (!replyText.value.trim() || !selectedTicket.value) return
  await support.replyToTicket(selectedTicket.value.id, replyText.value)
  replyText.value = ''
  nextTick(() => scrollToBottom())
}

async function createTicket() {
  if (!newSubject.value.trim() || !newMessage.value.trim()) return
  const ticket = await support.createTicket(newSubject.value, newMessage.value)
  showNewTicket.value = false
  newSubject.value = ''
  newMessage.value = ''
  selectTicket(ticket)
}

function resolveAvatar(url: string | null): string {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return API_BASE_URL + url
}

const STATUS_LABELS: Record<string, string> = {
  OPEN: 'Відкрито',
  IN_PROGRESS: 'В роботі',
  CLOSED: 'Закрито',
}
</script>

<template>
  <div class="support-page">
    <div class="support-container">
      <div class="support-header">
        <h1 class="page-title">ПІДТРИМКА</h1>
        <button class="btn-new-ticket" @click="showNewTicket = true">+ Новий тікет</button>
      </div>

      <div class="support-layout">
        <div class="tickets-panel">
          <div
            v-for="t in support.myTickets" :key="t.id"
            class="ticket-card"
            :class="{ selected: selectedTicket?.id === t.id }"
            @click="selectTicket(t)"
          >
            <div class="ticket-card-top">
              <span class="ticket-subj">{{ t.subject }}</span>
              <span class="ticket-status" :class="t.status.toLowerCase().replace('_', '-')">
                {{ STATUS_LABELS[t.status] || t.status }}
              </span>
            </div>
            <div class="ticket-card-date">{{ new Date(t.createdAt).toLocaleDateString('uk-UA') }}</div>
          </div>
          <div v-if="!support.myTickets.length" class="empty-tickets">
            <p>У вас ще немає тікетів</p>
          </div>
        </div>

        <div class="chat-panel">
          <template v-if="selectedTicket">
            <div class="chat-head">
              <strong>{{ selectedTicket.subject }}</strong>
              <span class="chat-head-status" :class="selectedTicket.status.toLowerCase().replace('_', '-')">
                {{ STATUS_LABELS[selectedTicket.status] || selectedTicket.status }}
              </span>
            </div>

            <div class="messages-area" ref="messagesEl">
              <div v-for="m in support.currentMessages" :key="m.id" class="msg" :class="{ 'msg--mine': m.senderId === auth.user?.id }">
                <div class="msg-avatar-wrap">
                  <img v-if="m.senderAvatarUrl" :src="resolveAvatar(m.senderAvatarUrl)" class="msg-avatar" />
                  <div v-else class="msg-avatar-ph">{{ m.senderDisplayName?.charAt(0) }}</div>
                </div>
                <div class="msg-body">
                  <div class="msg-name">
                    {{ m.senderDisplayName }}
                    <span v-if="m.admin" class="admin-tag">ПІДТРИМКА</span>
                  </div>
                  <div class="msg-text">{{ m.content }}</div>
                  <div class="msg-time">{{ new Date(m.sentAt).toLocaleString('uk-UA') }}</div>
                </div>
              </div>
            </div>

            <div v-if="selectedTicket.status !== 'CLOSED'" class="reply-row">
              <input v-model="replyText" class="reply-input" placeholder="Написати повідомлення..." @keyup.enter="sendReply" />
              <button class="reply-btn" @click="sendReply" :disabled="!replyText.trim()">Надіслати</button>
            </div>
            <div v-else class="closed-notice">Цей тікет закрито</div>
          </template>
          <div v-else class="empty-chat">
            <p>Оберіть тікет або створіть новий</p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showNewTicket" class="modal-overlay" @click.self="showNewTicket = false">
      <div class="modal-box">
        <h3>Новий тікет</h3>
        <label class="modal-label">Тема</label>
        <input v-model="newSubject" class="modal-input" placeholder="Опишіть проблему коротко..." />
        <label class="modal-label">Повідомлення</label>
        <textarea v-model="newMessage" class="modal-input modal-textarea" rows="4" placeholder="Детальний опис..."></textarea>
        <div class="modal-actions">
          <button class="modal-btn primary" @click="createTicket" :disabled="!newSubject.trim() || !newMessage.trim()">Створити</button>
          <button class="modal-btn" @click="showNewTicket = false">Скасувати</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.support-page { padding-top: 100px; padding-left: 20px; padding-right: 20px; padding-bottom: 40px; max-width: 1100px; margin: 0 auto; }
.support-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-title { font-family: var(--font-display); font-size: 2rem; color: var(--yellow); }

.btn-new-ticket {
  padding: 8px 20px;
  background: var(--yellow-dim);
  color: var(--black);
  border: 1px solid var(--yellow);
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 0.95rem;
  transition: background 0.15s;
}
.btn-new-ticket:hover { background: var(--yellow); }

.support-layout { display: grid; grid-template-columns: 300px 1fr; gap: 16px; height: 65vh; }

.tickets-panel {
  border: 1px solid var(--border);
  background: var(--panel);
  overflow-y: auto;
  padding: 12px;
}

.ticket-card {
  padding: 10px 12px;
  border: 1px solid var(--border);
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s;
}
.ticket-card:hover { border-color: var(--yellow-dim); }
.ticket-card.selected { border-color: var(--yellow); background: var(--panel-light); }
.ticket-card-top { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.ticket-subj { color: var(--white); font-size: 0.95rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.ticket-status { font-size: 0.7rem; padding: 1px 8px; border: 1px solid var(--border); text-transform: uppercase; white-space: nowrap; }
.ticket-status.open { color: var(--yellow); border-color: var(--yellow-dim); }
.ticket-status.in-progress { color: #2196f3; border-color: #1565c0; }
.ticket-status.closed { color: var(--gray); }
.ticket-card-date { font-size: 0.8rem; color: var(--gray); margin-top: 4px; }
.empty-tickets { text-align: center; color: var(--gray); padding: 30px 0; }

.chat-panel {
  border: 1px solid var(--border);
  background: var(--panel);
  display: flex;
  flex-direction: column;
}
.chat-head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}
.chat-head strong { color: var(--white); font-family: var(--font-display); font-size: 1.1rem; }
.chat-head-status { font-size: 0.7rem; padding: 1px 8px; border: 1px solid var(--border); text-transform: uppercase; }
.chat-head-status.open { color: var(--yellow); border-color: var(--yellow-dim); }
.chat-head-status.in-progress { color: #2196f3; border-color: #1565c0; }
.chat-head-status.closed { color: var(--gray); }

.messages-area { flex: 1; overflow-y: auto; padding: 16px; }
.msg { display: flex; gap: 10px; margin-bottom: 14px; }
.msg-avatar-wrap { flex-shrink: 0; }
.msg-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
.msg-avatar-ph {
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--panel-light); border: 1px solid var(--border);
  display: flex; align-items: center; justify-content: center;
  color: var(--gray); font-size: 0.85rem;
}
.msg-name { font-size: 0.85rem; color: var(--gray-light); margin-bottom: 2px; }
.admin-tag { color: var(--yellow); font-size: 0.7rem; border: 1px solid var(--yellow-dim); padding: 0 5px; margin-left: 6px; }
.msg-text { color: var(--white); font-size: 0.95rem; line-height: 1.4; }
.msg-time { font-size: 0.75rem; color: var(--gray); margin-top: 3px; }

.reply-row { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid var(--border); }
.reply-input {
  flex: 1; padding: 8px 12px;
  background: var(--panel-light); border: 1px solid var(--border);
  color: var(--white); font-family: var(--font-body); font-size: 0.95rem;
  outline: none;
}
.reply-input:focus { border-color: var(--yellow-dim); }
.reply-btn {
  padding: 8px 18px; background: var(--yellow-dim); color: var(--black); border: 1px solid var(--yellow);
  cursor: pointer; font-family: var(--font-body); font-size: 0.9rem;
}
.reply-btn:disabled { opacity: 0.5; cursor: default; }
.reply-btn:hover:not(:disabled) { background: var(--yellow); }
.closed-notice { text-align: center; padding: 12px; color: var(--gray); border-top: 1px solid var(--border); }
.empty-chat { display: flex; align-items: center; justify-content: center; height: 100%; color: var(--gray); }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-box { background: var(--panel); border: 1px solid var(--border); padding: 24px; min-width: 400px; max-width: 500px; }
.modal-box h3 { font-family: var(--font-display); font-size: 1.4rem; color: var(--white); margin-bottom: 16px; }
.modal-label { display: block; color: var(--gray); font-size: 0.85rem; margin: 12px 0 4px; }
.modal-input {
  width: 100%; padding: 8px 12px;
  background: var(--panel-light); border: 1px solid var(--border);
  color: var(--white); font-family: var(--font-body); font-size: 0.95rem; outline: none;
}
.modal-input:focus { border-color: var(--yellow-dim); }
.modal-textarea { resize: vertical; }
.modal-actions { display: flex; gap: 10px; margin-top: 20px; }
.modal-btn {
  padding: 8px 18px; border: 1px solid var(--border); background: var(--panel); color: var(--gray-light);
  cursor: pointer; font-family: var(--font-body); font-size: 0.9rem; transition: all 0.15s;
}
.modal-btn:hover { border-color: var(--yellow-dim); color: var(--white); }
.modal-btn.primary { background: var(--yellow-dim); color: var(--black); border-color: var(--yellow); }
.modal-btn.primary:hover { background: var(--yellow); }
.modal-btn:disabled { opacity: 0.5; cursor: default; }
</style>

