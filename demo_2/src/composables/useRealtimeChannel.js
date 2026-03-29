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
  const lastError = ref('')
  let client = null

  const disconnect = () => {
    if (client) {
      client.deactivate()
      client = null
    }
    isConnected.value = false
  }

  const connect = async ({
    topics = [],
    reconnectDelay = 1000,
    onConnect,
    onDisconnect,
    onError
  } = {}) => {
    disconnect()

    const token = String(getStoredToken() || '')
    client = new Client({
      reconnectDelay,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      webSocketFactory: () => new SockJS(buildWebSocketUrl())
    })

    client.onConnect = () => {
      isConnected.value = true
      lastError.value = ''
      topics.forEach(({ destination, handler }) => {
        client.subscribe(destination, (frame) => {
          try {
            handler?.(JSON.parse(frame.body || '{}'), frame)
          } catch {
            handler?.(frame.body, frame)
          }
        })
      })
      onConnect?.(client)
    }

    client.onStompError = (frame) => {
      isConnected.value = false
      lastError.value = frame?.body || 'STOMP error'
      onError?.(frame)
    }

    client.onWebSocketClose = (event) => {
      isConnected.value = false
      onDisconnect?.(event)
    }

    client.activate()
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    isConnected,
    lastError,
    connect,
    disconnect
  }
}
