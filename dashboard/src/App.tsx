import { lazy, Suspense, useCallback, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fleetApi } from './api'
import { AlertPanel } from './components/AlertPanel'
import { RobotTable } from './components/RobotTable'
import { SummaryCards } from './components/SummaryCards'
import type { FleetAlert, Telemetry } from './types'
import { useLiveFleet } from './useLiveFleet'

const RobotDetail = lazy(() => import('./components/RobotDetail').then((module) => ({
  default: module.RobotDetail,
})))

export default function App() {
  const queryClient = useQueryClient()
  const [live, setLive] = useState<Record<string, Telemetry>>({})
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [connection, setConnection] = useState<'CONNECTING' | 'CONNECTED' | 'DISCONNECTED'>('CONNECTING')

  const summary = useQuery({ queryKey: ['summary'], queryFn: fleetApi.summary, refetchInterval: 5_000 })
  const robots = useQuery({ queryKey: ['robots'], queryFn: fleetApi.robots, refetchInterval: 10_000 })
  const alerts = useQuery({ queryKey: ['alerts'], queryFn: fleetApi.alerts, refetchInterval: 8_000 })
  const acknowledge = useMutation({
    mutationFn: (id: string) => fleetApi.acknowledgeAlert(id, 'fleet-operator'),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['alerts'] })
      void queryClient.invalidateQueries({ queryKey: ['summary'] })
    },
  })

  const onTelemetry = useCallback((batch: Telemetry[]) => {
    setLive((current) => {
      const next = { ...current }
      batch.forEach((point) => { next[point.robotId] = point })
      return next
    })
  }, [])
  const onAlerts = useCallback((_batch: FleetAlert[]) => {
    void queryClient.invalidateQueries({ queryKey: ['alerts'] })
    void queryClient.invalidateQueries({ queryKey: ['summary'] })
  }, [queryClient])
  const onConnection = useCallback((state: typeof connection) => setConnection(state), [])
  useLiveFleet(onTelemetry, onAlerts, onConnection)

  const robotList = robots.data?.content ?? []
  const effectiveSelectedId = selectedId ?? robotList[0]?.id ?? null
  const selectedRobot = useMemo(
    () => robotList.find((robot) => robot.id === effectiveSelectedId),
    [effectiveSelectedId, robotList],
  )

  return (
    <main>
      <header className="app-header">
        <div className="brand-mark">RF</div>
        <div className="brand-copy"><span>RoboFleet</span><strong>Hospital Operations</strong></div>
        <div className={`connection ${connection.toLowerCase()}`}><i />{connection}</div>
      </header>
      <div className="page-shell">
        <div className="title-row">
          <div><span className="eyebrow">Rovex Medical Center</span><h1>Fleet command center</h1></div>
          <time>{new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })}</time>
        </div>
        <SummaryCards summary={summary.data} />
        {(summary.isError || robots.isError || alerts.isError) && (
          <div className="error-banner">The dashboard could not reach one or more backend endpoints.</div>
        )}
        <div className="dashboard-grid">
          <RobotTable robots={robotList} live={live} selectedId={effectiveSelectedId} onSelect={setSelectedId} />
          <AlertPanel alerts={alerts.data?.content ?? []} onAcknowledge={(id) => acknowledge.mutate(id)} />
          <Suspense fallback={<section className="panel detail-panel empty-state">Loading telemetry chart…</section>}>
            <RobotDetail robot={selectedRobot} live={effectiveSelectedId ? live[effectiveSelectedId] : undefined} />
          </Suspense>
        </div>
      </div>
    </main>
  )
}
