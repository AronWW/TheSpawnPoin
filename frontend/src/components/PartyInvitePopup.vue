<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { usePartyStore } from '../stores/parties'
import { useNotificationStore } from '../stores/notifications'
import { API_BASE_URL } from '../config'

const router = useRouter()
const partyStore = usePartyStore()
const notifStore = useNotificationStore()

const invite = computed(() => partyStore.pendingInvite)

function resolveAvatar(url: string | null): string {
  if (!url) return API_BASE_URL + '/avatars/default/avatar-1.png'
  if (url.startsWith('http')) return url
  return API_BASE_URL + url
}

async function accept() {
  if (!invite.value) return
  const partyId = invite.value.partyId
  const inviteId = invite.value.inviteId
  try {
    await partyStore.acceptInvite(inviteId)
    markRelatedNotificationRead(inviteId)
    await partyStore.fetchMyParties()
    if (partyId) {
      router.push(`/party/${partyId}`)
    }
  } catch {
  } finally {
    partyStore.dismissInvitePopup()
  }
}

async function decline() {
  if (!invite.value) return
  const inviteId = invite.value.inviteId
  try {
    await partyStore.declineInvite(inviteId)
    markRelatedNotificationRead(inviteId)
  } catch {
  } finally {
    partyStore.dismissInvitePopup()
  }
}

function dismiss() {
  partyStore.dismissInvitePopup()
}

function markRelatedNotificationRead(inviteId: number) {
  const notif = notifStore.notifications.find(
    (n) => n.type === 'PARTY_INVITE' && n.referenceId === inviteId && !n.read
  )
  if (notif) {
    notifStore.markOneRead(notif.id)
  }
}
</script>

<template>
  <Transition name="popup-slide">
    <div v-if="invite" class="popup-overlay">
      <div class="popup">
        <button class="popup-close" @click="dismiss" title="Закрити">✕</button>

        <div class="popup-icon">⚔️</div>
        <h3 class="popup-title">ЗАПРОШЕННЯ В ЛОБІ</h3>

        <div class="popup-sender">
          <img :src="resolveAvatar(invite.senderAvatarUrl)" class="sender-avatar" :alt="invite.senderDisplayName" />
          <span class="sender-name">{{ invite.senderDisplayName }}</span>
        </div>

        <div class="popup-game">
          <img v-if="invite.gameImageUrl" :src="invite.gameImageUrl" class="game-cover" :alt="invite.gameName ?? ''" />
          <span class="game-name">{{ invite.gameName }}</span>
        </div>

        <div class="popup-actions">
          <button class="btn-accept" @click="accept">✓ ПРИЙНЯТИ</button>
          <button class="btn-decline" @click="decline">✗ ВІДХИЛИТИ</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.popup-overlay {
  position: fixed;
  top: 80px;
  right: 24px;
  z-index: 9999;
}

.popup {
  pointer-events: all;
  width: 320px;
  background: var(--panel);
  border: 2px solid var(--yellow-dim);
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  position: relative;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(245, 197, 24, 0.08);
  animation: glow-pulse 2s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% { box-shadow: 0 12px 40px rgba(0, 0, 0, 0.6), 0 0 8px rgba(245, 197, 24, 0.15); }
  50%       { box-shadow: 0 12px 40px rgba(0, 0, 0, 0.6), 0 0 20px rgba(245, 197, 24, 0.3); }
}

.popup-close {
  position: absolute;
  top: 8px;
  right: 10px;
  background: none;
  border: none;
  color: var(--gray);
  font-size: 16px;
  cursor: pointer;
  padding: 2px 6px;
  transition: color 0.15s;
}
.popup-close:hover {
  color: var(--white);
}

.popup-icon {
  font-size: 28px;
}

.popup-title {
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 3px;
  color: var(--yellow);
}

.popup-sender {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sender-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--yellow-dim);
}

.sender-name {
  font-weight: 700;
  font-size: 14px;
  color: var(--white);
  letter-spacing: 0.5px;
}

.popup-game {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--panel-light);
  border: 1px solid var(--border);
  width: 100%;
}

.game-cover {
  width: 36px;
  height: 48px;
  object-fit: cover;
  border: 1px solid var(--border);
  flex-shrink: 0;
}

.game-name {
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 1px;
  color: var(--white);
}

.popup-actions {
  display: flex;
  gap: 10px;
  width: 100%;
}

.btn-accept {
  flex: 1;
  padding: 8px 0;
  background: var(--yellow);
  border: 2px solid var(--yellow);
  color: var(--black);
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-accept:hover {
  background: var(--yellow-dim);
}

.btn-decline {
  flex: 1;
  padding: 8px 0;
  background: transparent;
  border: 2px solid var(--red-dim);
  color: var(--red);
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.btn-decline:hover {
  background: var(--red);
  color: #fff;
}

.popup-slide-enter-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.popup-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.popup-slide-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}
.popup-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

@media (max-width: 960px) {
  .popup-overlay {
    top: 80px;
    right: 50%;
    transform: translateX(50%);
  }
}

@media (max-width: 480px) {
  .popup-overlay {
    top: 70px;
    right: 0;
    left: 0;
    transform: none;
    display: flex;
    justify-content: center;
    padding: 0 8px;
  }
  .popup {
    width: 100%;
    max-width: 360px;
  }
}
</style>

