import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const ws = ref(null)
const connected = ref(false)
const listeners = new Map()
let reconnectTimer = null
let reconnectAttempts = 0
const MAX_RECONNECT_ATTEMPTS = 10

function getWsUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host.replace(':5173', ':8080')
  return `${protocol}//${host}/ws/ride`
}

export function useWebSocket() {
  const authStore = useAuthStore()

  function connect() {
    if (ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) {
      return
    }

    const userId = authStore.userId
    if (!userId) return

    const url = getWsUrl() + `?userId=${userId}`
    ws.value = new WebSocket(url)

    ws.value.onopen = () => {
      connected.value = true
      reconnectAttempts = 0
      console.log('[WS] Connected')
    }

    ws.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        handleMessage(data)
      } catch (e) {
        console.error('[WS] Parse error:', e)
      }
    }

    ws.value.onclose = () => {
      connected.value = false
      console.log('[WS] Disconnected')
      scheduleReconnect()
    }

    ws.value.onerror = (err) => {
      console.error('[WS] Error:', err)
      connected.value = false
    }
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    reconnectAttempts = MAX_RECONNECT_ATTEMPTS
    if (ws.value) {
      ws.value.close()
      ws.value = null
    }
    connected.value = false
  }

  function scheduleReconnect() {
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return
    if (reconnectTimer) return

    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000)
    reconnectAttempts++
    console.log(`[WS] Reconnecting in ${delay}ms (attempt ${reconnectAttempts})`)

    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, delay)
  }

  function on(event, callback) {
    if (!listeners.has(event)) {
      listeners.set(event, new Set())
    }
    listeners.get(event).add(callback)
  }

  function off(event, callback) {
    if (listeners.has(event)) {
      listeners.get(event).delete(callback)
    }
  }

  function handleMessage(data) {
    const type = data.type
    const event = data.event

    const key = `${type}:${event}`
    if (listeners.has(key)) {
      listeners.get(key).forEach(cb => cb(data))
    }

    if (listeners.has(type)) {
      listeners.get(type).forEach(cb => cb(data))
    }

    if (listeners.has('*')) {
      listeners.get('*').forEach(cb => cb(data))
    }
  }

  return {
    connected: computed(() => connected.value),
    connect,
    disconnect,
    on,
    off
  }
}
