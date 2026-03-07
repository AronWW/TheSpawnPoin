import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const GUEST_ONLY_ROUTES = new Set([
  'login',
  'register',
  'forgot-password',
])

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomePage.vue') },
    { path: '/login', name: 'login', component: () => import('../views/LoginPage.vue') },
    { path: '/register', name: 'register', component: () => import('../views/RegisterPage.vue') },
    { path: '/verify-email', name: 'verify-email', component: () => import('../views/VerifyEmailPage.vue') },
    { path: '/forgot-password', name: 'forgot-password', component: () => import('../views/ForgotPasswordPage.vue') },
    { path: '/reset-password', name: 'reset-password', component: () => import('../views/ResetPasswordPage.vue') },
    { path: '/chat', name: 'chat', component: () => import('../views/ChatPage.vue'), meta: { requiresAuth: true } },
    { path: '/search-parties', name: 'search-parties', component: () => import('../views/SearchPartiesPage.vue') },
    { path: '/games', name: 'games', component: () => import('../views/GamesPage.vue') },
    { path: '/favorite-games', name: 'favorite-games', component: () => import('../views/FavoriteGamesPage.vue'), meta: { requiresAuth: true } },
    { path: '/friends', name: 'friends', component: () => import('../views/FriendsPage.vue'), meta: { requiresAuth: true } },
    { path: '/profile/:userId', name: 'profile', component: () => import('../views/ProfilePage.vue'), props: true },
    { path: '/settings', name: 'settings', component: () => import('../views/EditProfilePage.vue'), meta: { requiresAuth: true } },
    { path: '/party/:id', name: 'party-detail', component: () => import('../views/PartyDetailPage.vue') },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (GUEST_ONLY_ROUTES.has(to.name as string) && auth.isLoggedIn) {
    return { name: 'home' }
  }
})

export default router