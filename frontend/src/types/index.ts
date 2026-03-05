
export interface UserMe {
  id: number
  email: string
  displayName: string
  emailVerified: boolean
  role: string
  status: string
  lastSeen: string
  avatarUrl: string | null
}

export interface Game {
  id: number
  name: string
  genre: string | null
  releaseYear: number | null
  imageUrl: string | null
  maxPartySize: number
}

export interface PartyMember {
  userId: number
  displayName: string
  avatarUrl: string | null
  isCreator: boolean
  joinedAt: string
}

export interface Party {
  id: number
  creatorId: number
  creatorDisplayName: string
  creatorAvatarUrl: string | null
  gameId: number
  gameName: string
  gameImageUrl: string | null
  maxMembers: number
  currentMembers: number
  isOpen: boolean
  description: string | null
  eventTime: string | null
  platform: string[]
  language: string | null
  skillLevel: string | null
  playStyle: string | null
  members: PartyMember[] | null
  chatId: number | null
  createdAt: string
}

export interface CreatePartyRequest {
  gameId: number | null
  description: string
  eventTime: string | null
  platform: string[]
  language: string
  skillLevel: string
  playStyle: string | null
}

export interface Notification {
  id: number
  type: string
  message: string
  referenceId: number | null
  read: boolean
  createdAt: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface PartyFilters {
  gameId: number | null
  platform: string
  skillLevel: string
  playStyle: string
  language: string
  search: string
}

export type SortOption = 'newest' | 'slots' | 'game'

export interface Friend {
  userId: number
  email: string
  displayName: string
  avatarUrl: string | null
  status: string
  lastSeen: string | null
  friendsSince: string
}

export interface FriendRequest {
  inviteId: number
  senderId: number
  senderEmail: string
  senderDisplayName: string
  senderAvatarUrl: string | null
  receiverId: number
  receiverDisplayName: string
  receiverAvatarUrl: string | null
  createdAt: string
}

export interface Profile {
  userId: number
  email: string
  displayName: string
  fullName: string | null
  avatarUrl: string | null
  bio: string | null
  birthDate: string | null
  platforms: string[]
  skillLevel: string | null
  playStyle: string | null
  languages: string[]
  country: string | null
  region: string | null
}

export interface ChatParticipant {
  userId: number
  displayName: string
  email: string
  avatarUrl: string | null
}

export interface ChatItem {
  id: number


  isGroup: boolean
  lastMessage: string | null
  lastMessageAt: string | null
  unreadCount: number

  partnerEmail: string | null
  partnerDisplayName: string | null
  partnerStatus: string | null
  partnerLastSeen: string | null

  isPartyLinked: boolean
  title: string | null
  partyId: number | null
  participants: ChatParticipant[] | null
}

export interface ChatMessage {
  id: number
  chatId: number
  senderEmail: string | null
  senderName: string | null
  senderAvatarUrl: string | null
  content: string
  sentAt: string
  read: boolean
  system: boolean
}

