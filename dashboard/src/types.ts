export type RobotStatus = 'ONLINE' | 'DEGRADED' | 'OFFLINE' | 'MAINTENANCE'
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED'
export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL'

export interface Robot {
  id: string
  displayName: string
  model: string
  facility: string
  ward: string
  floor: number
  operationalStatus: RobotStatus
  lastSeenAt: string | null
  batteryPercent: number | null
  currentX: number | null
  currentY: number | null
}

export interface Telemetry {
  eventId: string
  robotId: string
  sequenceNumber: number
  recordedAt: string
  x: number
  y: number
  floor: number
  batteryPercent: number
  speedMps: number
  headingDegrees: number
  temperatureCelsius: number
  missionState: string
  connectivity: string
  errorCodes: string[]
}

export interface FleetAlert {
  id: string
  robotId: string
  type: string
  severity: AlertSeverity
  status: AlertStatus
  message: string
  firstTriggeredAt: string
  lastTriggeredAt: string
  acknowledgedAt: string | null
  acknowledgedBy: string | null
  resolvedAt: string | null
  occurrences: number
}

export interface FleetSummary {
  totalRobots: number
  onlineRobots: number
  degradedRobots: number
  offlineRobots: number
  maintenanceRobots: number
  openAlerts: number
  criticalAlerts: number
  averageBatteryPercent: number
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

