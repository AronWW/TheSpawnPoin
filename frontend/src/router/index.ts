import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const GUEST_ONLY_ROUTES = new Set([
  'login',
  'register',
  'forgot-password',
])

const ADMIN_BLOCKED_ROUTES = new Set([
  'party-detail',
  'friends',
  'chat',
  'favorite-games',
  'my-suggestions',
  'support',
  'settings',
  'profile',
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
    { path: '/support', name: 'support', component: () => import('../views/SupportPage.vue'), meta: { requiresAuth: true } },
    { path: '/my-suggestions', name: 'my-suggestions', component: () => import('../views/MySuggestionsPage.vue'), meta: { requiresAuth: true } },

    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true, hideNavbar: true },
      children: [
        { path: '', name: 'admin-dashboard', component: () => import('../views/admin/AdminDashboardPage.vue') },
        { path: 'users', name: 'admin-users', component: () => import('../views/admin/AdminUsersPage.vue') },
        { path: 'games', name: 'admin-games', component: () => import('../views/admin/AdminGamesPage.vue') },
        { path: 'game-suggestions', name: 'admin-suggestions', component: () => import('../views/admin/AdminGameSuggestionsPage.vue') },
        { path: 'reports', name: 'admin-reports', component: () => import('../views/admin/AdminReportsPage.vue') },
        { path: 'tickets', name: 'admin-tickets', component: () => import('../views/admin/AdminTicketsPage.vue') },
        { path: 'unban-requests', name: 'admin-unban-requests', component: () => import('../views/admin/AdminUnbanRequestsPage.vue') },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (!auth.initialized) {
    await auth.init()
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin && auth.user?.role !== 'ADMIN') {
    return { name: 'home' }
  }

  if (auth.user?.role === 'ADMIN' && ADMIN_BLOCKED_ROUTES.has(to.name as string)) {
    return { name: 'admin-dashboard' }
  }

  if (GUEST_ONLY_ROUTES.has(to.name as string) && auth.isLoggedIn) {
    return { name: 'home' }
  }
})

export default router