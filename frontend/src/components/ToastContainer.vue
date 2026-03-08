<script setup lang="ts">
import { useToast } from '../composables/useToast'

const { toasts } = useToast()
</script>

<template>
  <Teleport to="body">
    <TransitionGroup name="toast" tag="div" class="toast-container">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast-item"
        :class="t.type"
      >
        <span class="toast-icon">
          <template v-if="t.type === 'success'">✓</template>
          <template v-else-if="t.type === 'error'">✕</template>
          <template v-else>ℹ</template>
        </span>
        <span class="toast-msg">{{ t.message }}</span>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 80px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.toast-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  min-width: 260px;
  max-width: 400px;
  background: var(--panel);
  border: 2px solid var(--border);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(245, 197, 24, 0.05);
  pointer-events: auto;
  position: relative;
  overflow: hidden;
}
.toast-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
}
.toast-item.success {
  border-color: rgba(46, 204, 113, 0.3);
}
.toast-item.success::before {
  background: #2ecc71;
}
.toast-item.error {
  border-color: rgba(192, 57, 43, 0.3);
}
.toast-item.error::before {
  background: var(--red);
}
.toast-item.info {
  border-color: var(--yellow-dim);
}
.toast-item.info::before {
  background: var(--yellow);
}

.toast-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  border: 2px solid;
}
.success .toast-icon {
  color: #2ecc71;
  border-color: rgba(46, 204, 113, 0.4);
  background: rgba(46, 204, 113, 0.08);
}
.error .toast-icon {
  color: var(--red);
  border-color: rgba(192, 57, 43, 0.4);
  background: rgba(192, 57, 43, 0.08);
}
.info .toast-icon {
  color: var(--yellow);
  border-color: var(--yellow-dim);
  background: rgba(245, 197, 24, 0.08);
}

.toast-msg {
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--white);
}

.toast-enter-active {
  transition: all 0.3s ease;
}
.toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(40px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(40px);
}

@media (max-width: 480px) {
  .toast-container {
    right: 12px;
    left: 12px;
    top: 72px;
  }
  .toast-item {
    min-width: 0;
    max-width: none;
  }
}
</style>

