import { defineStore } from 'pinia'
import api from '../api/axios'
import type { SupportTicket, TicketMessage } from '../types'
import { ref } from 'vue'

export const useSupportStore = defineStore('support', () => {
  const myTickets = ref<SupportTicket[]>([])
  const currentMessages = ref<TicketMessage[]>([])

  async function fetchMyTickets() {
    const { data } = await api.get<SupportTicket[]>('/support/tickets')
    myTickets.value = data
  }

  async function createTicket(subject: string, message: string) {
    const { data } = await api.post<SupportTicket>('/support/tickets', { subject, message })
    myTickets.value.unshift(data)
    return data
  }

  async function fetchTicketMessages(ticketId: number) {
    const { data } = await api.get<TicketMessage[]>(`/support/tickets/${ticketId}/messages`)
    currentMessages.value = data
  }

  async function replyToTicket(ticketId: number, content: string) {
    const { data } = await api.post<TicketMessage>(`/support/tickets/${ticketId}/reply`, { content })
    currentMessages.value.push(data)
    return data
  }

  return {
    myTickets, currentMessages,
    fetchMyTickets, createTicket, fetchTicketMessages, replyToTicket,
  }
})

