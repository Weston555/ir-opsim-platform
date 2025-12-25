import SockJS from 'sockjs-client'
import { Client, IMessage } from 'stompjs'
import { useAuthStore } from '@/stores/auth'

class WebSocketService {
  private stompClient: Client | null = null
  private connected = false
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000

  // 订阅回调函数
  private subscriptions: Map<string, (message: any) => void> = new Map()

  constructor() {
    this.connect()
  }

  // 连接WebSocket
  private connect(): void {
    const wsUrl = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080'
    const authStore = useAuthStore()

    try {
      const socket = new SockJS(`${wsUrl}/ws`)
      this.stompClient = new Client({
        webSocketFactory: () => socket,
        connectHeaders: {
          Authorization: authStore.token ? `Bearer ${authStore.token}` : '',
        },
        debug: (str) => {
          console.log('WebSocket Debug:', str)
        },
        reconnectDelay: this.reconnectDelay,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      })

      this.stompClient.onConnect = () => {
        console.log('WebSocket connected')
        this.connected = true
        this.reconnectAttempts = 0

        // 重新订阅所有主题
        this.resubscribeAll()
      }

      this.stompClient.onDisconnect = () => {
        console.log('WebSocket disconnected')
        this.connected = false
      }

      this.stompClient.onStompError = (frame) => {
        console.error('WebSocket error:', frame.headers['message'])
        console.error('Details:', frame.body)
        this.attemptReconnect()
      }

      this.stompClient.activate()
    } catch (error) {
      console.error('WebSocket connection failed:', error)
      this.attemptReconnect()
    }
  }

  // 尝试重连
  private attemptReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached')
      return
    }

    this.reconnectAttempts++
    console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)

    setTimeout(() => {
      this.connect()
    }, this.reconnectDelay)
  }

  // 订阅主题
  subscribe(destination: string, callback: (message: any) => void): void {
    if (!this.connected || !this.stompClient) {
      console.warn('WebSocket not connected, queuing subscription:', destination)
      this.subscriptions.set(destination, callback)
      return
    }

    const subscription = this.stompClient.subscribe(destination, (message: IMessage) => {
      try {
        const body = JSON.parse(message.body)
        callback(body)
      } catch (error) {
        console.error('Error parsing WebSocket message:', error)
      }
    })

    this.subscriptions.set(destination, callback)
    console.log('Subscribed to:', destination)
  }

  // 取消订阅
  unsubscribe(destination: string): void {
    this.subscriptions.delete(destination)
    console.log('Unsubscribed from:', destination)
  }

  // 重新订阅所有主题
  private resubscribeAll(): void {
    for (const [destination, callback] of this.subscriptions) {
      this.subscribe(destination, callback)
    }
  }

  // 发送消息
  send(destination: string, body: any): void {
    if (!this.connected || !this.stompClient) {
      console.warn('WebSocket not connected, cannot send message')
      return
    }

    this.stompClient.publish({
      destination,
      body: JSON.stringify(body),
    })
  }

  // 断开连接
  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate()
    }
    this.connected = false
    this.subscriptions.clear()
  }

  // 获取连接状态
  isConnected(): boolean {
    return this.connected
  }
}

// 创建单例实例
const webSocketService = new WebSocketService()

export default webSocketService
