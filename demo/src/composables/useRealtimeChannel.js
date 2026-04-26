import { onUnmounted, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { API_BASE_URL } from '../services/apiClient'
import { getStoredToken } from '../services/authService'

const buildWebSocketUrl = () => {
  const token = String(getStoredToken() || '')
  const baseWs = `${API_BASE_URL.replace(/\/$/, '')}/ws`
  return token ? `${baseWs}?access_token=${encodeURIComponent(token)}` : baseWs
}

export function useRealtimeChannel() {
  const isConnected = ref(false)
  const isConnecting = ref(false)
  const lastError = ref('')
  let client = null
  let reconnectTimer = null
  let reconnectAttempts = 0
  let heartbeatTimer = null
  let currentTopics = []
  const activeSubscriptions = new Map()
  const MAX_RECONNECT_DELAY = 30000
  const BASE_RECONNECT_DELAY = 1000
  const HEARTBEAT_INTERVAL_MS = 20000

  const getReconnectDelay = () => {
    const delay = Math.min(BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts), MAX_RECONNECT_DELAY)
    return delay + Math.random() * 1000
  }

  const clearHeartbeat = () => {
    if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null }
  }

  const startHeartbeat = () => {
    clearHeartbeat()
    heartbeatTimer = setInterval(() => {
      if (client?.connected) {
        try {
          client.publish({ destination: '/app/heartbeat', body: JSON.stringify({ ts: Date.now() }) })
        } catch { /* ignore */ }
      }
    }, HEARTBEAT_INTERVAL_MS)
  }

  const disconnect = () => {
    clearHeartbeat()
    if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
    reconnectAttempts = 0
    if (client) {
      client.deactivate()
      client = null
    }
    isConnected.value = false
    isConnecting.value = false
  }

  const connect = async ({
    topics = [],
    reconnectDelay = 5000,
    onConnect,
    onDisconnect,
    onError
  } = {}) => {
    disconnect()
    isConnecting.value = true
    currentTopics = topics

    const token = String(getStoredToken() || '')
    client = new Client({
      reconnectDelay,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      webSocketFactory: () => new SockJS(buildWebSocketUrl()),
      heartbeatIncoming: 15000,
      heartbeatOutgoing: 15000,
      onStompError: (frame) => {
        isConnected.value = false
        isConnecting.value = false
        lastError.value = frame?.body || 'STOMP error'
        onError?.(frame)
      },
      onWebSocketClose: (event) => {
        isConnected.value = false
        isConnecting.value = false
        clearHeartbeat()
        onDisconnect?.(event)
        scheduleReconnect({ topics: currentTopics, reconnectDelay, onConnect, onDisconnect, onError })
      }
    })

    client.onConnect = () => {
      isConnected.value = true
      isConnecting.value = false
      lastError.value = ''
      reconnectAttempts = 0

      currentTopics.forEach(({ destination, handler }) => {
        client.subscribe(destination, (frame) => {
          try {
            handler?.(JSON.parse(frame.body || '{}'), frame)
          } catch {
            handler?.(frame.body, frame)
          }
        })
      })

      startHeartbeat()
      onConnect?.(client)
    }

    client.activate()
  }

  const scheduleReconnect = ({ topics = [], reconnectDelay = 5000, onConnect, onDisconnect, onError } = {}) => {
    if (reconnectTimer) return
    const delay = getReconnectDelay()
    reconnectAttempts += 1
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      // Merge original topics with dynamically added subscriptions
      const allTopics = [
        ...topics,
        ...[...activeSubscriptions.keys()].map(d => ({ destination: d, handler: null }))
      ]
      connect({ topics: allTopics, reconnectDelay, onConnect, onDisconnect, onError })
    }, delay)
  }

  const subscribe = (destination, handler) => {
    if (!client?.connected) return
    if (activeSubscriptions.has(destination)) return
    const sub = client.subscribe(destination, (frame) => {
      try {
        handler?.(JSON.parse(frame.body || '{}'), frame)
      } catch {
        handler?.(frame.body, frame)
      }
    })
    activeSubscriptions.set(destination, sub)
  }

  const unsubscribe = (destination) => {
    const sub = activeSubscriptions.get(destination)
    if (sub) { sub.unsubscribe(); activeSubscriptions.delete(destination) }
  }

  const retryConnection = () => {
    if (client) {
      client.deactivate()
      client = null
    }
    if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
    reconnectAttempts = 0
    isConnecting.value = false
    lastError.value = ''
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected,
    isConnecting,
    lastError,
    connect,
    disconnect,
    retryConnection,
    subscribe,
    unsubscribe
  }
}
