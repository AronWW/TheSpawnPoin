import { ref } from 'vue'
import { Client, type IMessage } from '@stomp/stompjs'

const WS_URL = 'ws://localhost:8080/ws'

let client: Client | null = null
const connected = ref(false)

const activeSubs = new Map<string, { id: string; unsub: () => void }>()

let nextCbId = 0
interface CbEntry { cbId: number; callback: (msg: IMessage) => void }
const pendingCallbacks = new Map<string, CbEntry[]>()

function makeDispatcher(destination: string): (msg: IMessage) => void {
  return (msg: IMessage) => {
    const entries = pendingCallbacks.get(destination)
    if (entries) {
      for (const entry of entries) {
        try { entry.callback(msg) } catch { /* ignore */ }
      }
    }
  }
}

function subscribeOnServer(destination: string) {
  if (!client || !client.connected) return
  if (activeSubs.has(destination)) return
  const sub = client.subscribe(destination, makeDispatcher(destination))
  activeSubs.set(destination, { id: sub.id, unsub: () => sub.unsubscribe() })
}

function unsubscribeOnServer(destination: string) {
  const entry = activeSubs.get(destination)
  if (entry) {
    entry.unsub()
    activeSubs.delete(destination)
  }
}

function getOrCreateClient(): Client {
  if (!client) {
    client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.debug('[STOMP]', str)
        }
      },
      onConnect: () => {
        connected.value = true
        for (const dest of pendingCallbacks.keys()) {
          subscribeOnServer(dest)
        }
      },
      onDisconnect: () => {
        connected.value = false
        activeSubs.clear()
      },
      onWebSocketClose: () => {
        connected.value = false
        activeSubs.clear()
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame)
      },
    })
  }
  return client
}

export function useStompClient() {
  function activate() {
    const c = getOrCreateClient()
    if (!c.active) {
      c.activate()
    }
  }

  function deactivate() {
    if (client) {
      client.deactivate()
      client = null
      connected.value = false
      activeSubs.clear()
      pendingCallbacks.clear()
    }
  }

  function subscribe(destination: string, callback: (msg: IMessage) => void): () => void {
    getOrCreateClient()

    const cbId = nextCbId++
    const entries = pendingCallbacks.get(destination) || []
    entries.push({ cbId, callback })
    pendingCallbacks.set(destination, entries)

    subscribeOnServer(destination)

    return () => {
      const arr = pendingCallbacks.get(destination)
      if (arr) {
        const filtered = arr.filter((e) => e.cbId !== cbId)
        if (filtered.length === 0) {
          pendingCallbacks.delete(destination)
          unsubscribeOnServer(destination)
        } else {
          pendingCallbacks.set(destination, filtered)
        }
      }
    }
  }

  function publish(destination: string, body: object) {
    const c = getOrCreateClient()
    if (c.connected) {
      c.publish({ destination, body: JSON.stringify(body) })
    } else {
      console.warn('[STOMP] Not connected, cannot publish to', destination)
    }
  }

  return {
    connected,
    activate,
    deactivate,
    subscribe,
    publish,
  }
}
