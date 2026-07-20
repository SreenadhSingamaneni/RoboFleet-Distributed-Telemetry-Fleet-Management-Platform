import axios from 'axios'
import type { FleetAlert, FleetSummary, Page, Robot, Telemetry } from './types'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  timeout: 8_000,
  headers: import.meta.env.VITE_API_KEY
    ? { 'X-API-Key': import.meta.env.VITE_API_KEY }
    : undefined,
})

export const fleetApi = {
  summary: async () => (await api.get<FleetSummary>('/fleet/summary')).data,
  robots: async () =>
    (await api.get<Page<Robot>>('/robots', { params: { page: 0, size: 100 } })).data,
  alerts: async () =>
    (
      await api.get<Page<FleetAlert>>('/alerts', {
        params: { status: 'OPEN', page: 0, size: 50 },
      })
    ).data,
  telemetryHistory: async (robotId: string) =>
    (
      await api.get<Telemetry[]>(`/robots/${robotId}/telemetry`, {
        params: { limit: 180 },
      })
    ).data.reverse(),
  acknowledgeAlert: async (alertId: string, operator: string) =>
    (await api.post<FleetAlert>(`/alerts/${alertId}/acknowledge`, { operator })).data,
}

