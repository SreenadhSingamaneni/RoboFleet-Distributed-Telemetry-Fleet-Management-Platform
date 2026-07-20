import { Client } from '@stomp/stompjs'
import { useEffect } from 'react'
import type { FleetAlert, Telemetry } from './types'

type ConnectionState = 'CONNECTING' | 'CONNECTED' | 'DISCONNECTED'

function websocketUrl(): string {
  if (import.meta.env.VITE_WEBSOCKET_URL) return import.meta.env.VITE_WEBSOCKET_URL
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws`
}

export function useLiveFleet(
  onTelemetry: (batch: Telemetry[]) => void,
  onAlerts: (batch: FleetAlert[]) => void,
  onConnectionState: (state: ConnectionState) => void,
): void {
  useEffect(() => {
    const client = new Client({
      brokerURL: websocketUrl(),
      reconnectDelay: 2_000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      debug: () => undefined,
      onConnect: () => {
        onConnectionState('CONNECTED')
        client.subscribe('/topic/telemetry', (message) => {
          onTelemetry(JSON.parse(message.body) as Telemetry[])
        })
        client.subscribe('/topic/alerts', (message) => {
          onAlerts(JSON.parse(message.body) as FleetAlert[])
        })
      },
      onWebSocketClose: () => onConnectionState('DISCONNECTED'),
      onStompError: () => onConnectionState('DISCONNECTED'),
    })
    onConnectionState('CONNECTING')
    client.activate()
    return () => {
      void client.deactivate()
    }
  }, [onAlerts, onConnectionState, onTelemetry])
}

