<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import TheNavbar from './components/TheNavbar.vue'
import TheFooter from './components/TheFooter.vue'
import BannedModal from './components/BannedModal.vue'
import PartyInvitePopup from './components/PartyInvitePopup.vue'
import ToastContainer from './components/ToastContainer.vue'
import { useAuthStore } from './stores/auth'
import { useGlobalWebSocket } from './composables/useGlobalWebSocket'

const auth = useAuthStore()
const route = useRoute()

const showLayout = computed(() => !route.meta.hideNavbar)
const isBanned = computed(() => auth.user?.banned === true)

onMounted(() => {
  auth.init()
})

useGlobalWebSocket()
</script>

<template>
  <BannedModal v-if="isBanned" :ban-reason="auth.user?.banReason ?? null" />
  <TheNavbar v-if="showLayout" />
  <router-view />
  <TheFooter v-if="showLayout" />
  <PartyInvitePopup />
  <ToastContainer />
</template>
