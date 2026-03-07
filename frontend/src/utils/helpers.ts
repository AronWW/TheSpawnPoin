export function genreColor(genre: string | null): string {
  if (!genre) return ''
  const map: Record<string, string> = {
    Shooter: '',
    MOBA: 'blue',
    'Battle Royale': 'purple',
    RPG: 'red',
    MMO: 'green',
    Horror: 'red',
  }
  return map[genre] ?? ''
}

export function gameEmoji(genre: string | null): string {
  if (!genre) return '🎮'
  const map: Record<string, string> = {
    Shooter: '🔫',
    MOBA: '⚔️',
    'Battle Royale': '🪂',
    RPG: '🐉',
    MMO: '🏰',
    Horror: '👻',
    Sports: '⚽',
    Sandbox: '🏗️',
  }
  return map[genre] ?? '🎮'
}

export function skillLabel(skill: string | null): string {
  if (!skill) return ''
  const map: Record<string, string> = {
    BEGINNER: 'Початківець',
    INTERMEDIATE: 'Середній',
    ADVANCED: 'Просунутий',
    EXPERT: 'Експерт',
  }
  return map[skill] ?? skill
}

export function notificationIcon(type: string): string {
  const map: Record<string, string> = {
    PARTY_INVITE: '⚔️',
    FRIEND_REQUEST: '👥',
    FRIEND_ACCEPTED: '👥',
    MESSAGE: '💬',
    PARTY_FULL: '🎮',
    PARTY_JOIN: '🎮',
    GAME_SUGGESTION_APPROVED: '✅',
    GAME_SUGGESTION_REJECTED: '❌',
    REPORT_REVIEWED: '📋',
    SUPPORT_REPLY: '📩',
  }
  return map[type] ?? '🔔'
}

export function timeAgo(isoDate: string): string {
  const diff = Date.now() - new Date(isoDate).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return 'щойно'
  if (minutes < 60) return `${minutes} хв тому`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} год тому`
  const days = Math.floor(hours / 24)
  return `${days} д тому`
}

export interface SpeedLine {
  id: number
  top: number
  width: number
  dur: number
  delay: number
}

export function generateSpeedLines(count = 12): SpeedLine[] {
  return Array.from({ length: count }, (_, i) => ({
    id: i,
    top: 5 + Math.random() * 90,
    width: 200 + Math.random() * 400,
    dur: 2.5 + Math.random() * 3,
    delay: Math.random() * 4,
  }))
}

