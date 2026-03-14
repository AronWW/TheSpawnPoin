import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/axios'
import type {
  AdminDashboard, AdminUser, AdminActiveParty,
  GameSuggestion, Report, SupportTicket, TicketMessage,
  Game, Page, UnbanRequest,
} from '../types'

export const useAdminStore = defineStore('admin', () => {

  const dashboard = ref<AdminDashboard | null>(null)

  const users = ref<Page<AdminUser> | null>(null)
  const bannedUsers = ref<Page<AdminUser> | null>(null)

  const games = ref<Page<Game> | null>(null)

  const suggestions = ref<Page<GameSuggestion> | null>(null)

  const reports = ref<Page<Report> | null>(null)

  const tickets = ref<Page<SupportTicket> | null>(null)
  const ticketMessages = ref<TicketMessage[]>([])

  const activeParties = ref<Page<AdminActiveParty> | null>(null)

  const unbanRequests = ref<Page<UnbanRequest> | null>(null)

  async function fetchDashboard() {
    const { data } = await api.get<AdminDashboard>('/admin/dashboard')
    dashboard.value = data
  }

  async function fetchUsers(page = 0, size = 20, q?: string) {
    const params: Record<string, any> = { page, size }
    if (q) params.q = q
    const { data } = await api.get<Page<AdminUser>>('/admin/users', { params })
    users.value = data
  }

  async function fetchBannedUsers(page = 0, size = 20) {
    const { data } = await api.get<Page<AdminUser>>('/admin/users/banned', { params: { page, size } })
    bannedUsers.value = data
  }

  async function banUser(id: number, reason?: string) {
    await api.post(`/admin/users/${id}/ban`, { reason })
  }

  async function unbanUser(id: number) {
    await api.post(`/admin/users/${id}/unban`)
  }

  async function deleteUser(id: number) {
    await api.delete(`/admin/users/${id}`)
  }

  async function fetchGames(page = 0, size = 20, q?: string, genre?: string) {
    const params: Record<string, any> = { page, size }
    if (q) params.q = q
    if (genre) params.genre = genre
    const { data } = await api.get<Page<Game>>('/admin/games', { params })
    games.value = data
  }

  async function createGame(dto: { name: string; genre?: string; releaseYear?: number; imageUrl?: string; maxPartySize: number }) {
    const { data } = await api.post('/admin/games', dto)
    return data
  }

  async function updateGame(id: number, dto: Partial<{ name: string; genre: string; releaseYear: number; imageUrl: string; maxPartySize: number }>) {
    const { data } = await api.put(`/admin/games/${id}`, dto)
    return data
  }

  async function deleteGame(id: number) {
    await api.delete(`/admin/games/${id}`)
  }

  async function fetchSuggestions(page = 0, size = 20, status?: string) {
    const params: Record<string, any> = { page, size }
    if (status) params.status = status
    const { data } = await api.get<Page<GameSuggestion>>('/admin/game-suggestions', { params })
    suggestions.value = data
  }

  async function approveSuggestion(id: number, data?: { name: string; genre?: string; releaseYear?: number; imageUrl?: string; maxPartySize: number }) {
    await api.post(`/admin/game-suggestions/${id}/approve`, data || {})
  }

  async function rejectSuggestion(id: number, comment?: string) {
    await api.post(`/admin/game-suggestions/${id}/reject`, { comment })
  }

  async function fetchReports(page = 0, size = 20, status?: string) {
    const params: Record<string, any> = { page, size }
    if (status) params.status = status
    const { data } = await api.get<Page<Report>>('/admin/reports', { params })
    reports.value = data
  }

  async function reviewReport(id: number, status: string, adminComment?: string) {
    await api.post(`/admin/reports/${id}/review`, { status, adminComment })
  }

  async function fetchTickets(page = 0, size = 20, status?: string) {
    const params: Record<string, any> = { page, size }
    if (status) params.status = status
    const { data } = await api.get<Page<SupportTicket>>('/admin/tickets', { params })
    tickets.value = data
  }

  async function fetchTicketMessages(ticketId: number) {
    const { data } = await api.get<TicketMessage[]>(`/admin/tickets/${ticketId}/messages`)
    ticketMessages.value = data
  }

  async function replyToTicket(ticketId: number, content: string) {
    const { data } = await api.post(`/admin/tickets/${ticketId}/reply`, { content })
    ticketMessages.value.push(data)
    return data
  }

  async function changeTicketStatus(ticketId: number, status: string) {
    await api.post(`/admin/tickets/${ticketId}/status`, { status })
  }

  async function fetchActiveParties(page = 0, size = 20) {
    const { data } = await api.get<Page<AdminActiveParty>>('/admin/parties/active', { params: { page, size } })
    activeParties.value = data
  }

  async function fetchUnbanRequests(page = 0, size = 20, status?: string) {
    const params: Record<string, any> = { page, size }
    if (status) params.status = status
    const { data } = await api.get<Page<UnbanRequest>>('/admin/unban-requests', { params })
    unbanRequests.value = data
  }

  async function reviewUnbanRequest(id: number, status: string, adminComment?: string) {
    await api.post(`/admin/unban-requests/${id}/review`, { status, adminComment })
  }

  return {
    dashboard, users, bannedUsers, games, suggestions, reports, tickets, ticketMessages, activeParties, unbanRequests,
    fetchDashboard,
    fetchUsers, fetchBannedUsers, banUser, unbanUser, deleteUser,
    fetchGames, createGame, updateGame, deleteGame,
    fetchSuggestions, approveSuggestion, rejectSuggestion,
    fetchReports, reviewReport,
    fetchTickets, fetchTicketMessages, replyToTicket, changeTicketStatus,
    fetchActiveParties,
    fetchUnbanRequests, reviewUnbanRequest,
  }
})

