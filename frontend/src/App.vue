<script setup lang="ts">
import { onMounted } from 'vue'
import TheNavbar from './components/TheNavbar.vue'
import TheFooter from './components/TheFooter.vue'
import { useAuthStore } from './stores/auth'
import { useGlobalWebSocket } from './composables/useGlobalWebSocket'

const auth = useAuthStore()

onMounted(() => {
  auth.fetchMe()
})

useGlobalWebSocket()
</script>

<template>
  <TheNavbar />
  <router-view />
  <TheFooter />
</template>
