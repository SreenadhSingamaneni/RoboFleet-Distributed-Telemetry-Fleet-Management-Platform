import { formatPercent, timeAgo } from '../format'
import type { Robot, Telemetry } from '../types'

interface Props {
  robots: Robot[]
  live: Record<string, Telemetry>
  selectedId: string | null
  onSelect: (id: string) => void
}

export function RobotTable({ robots, live, selectedId, onSelect }: Props) {
  return (
    <section className="panel fleet-panel">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Live registry</span>
          <h2>Hospital fleet</h2>
        </div>
        <span className="panel-count">{robots.length} shown</span>
      </div>
      <div className="table-scroll">
        <table>
          <thead>
            <tr><th>Robot</th><th>Status</th><th>Battery</th><th>Mission</th><th>Last signal</th></tr>
          </thead>
          <tbody>
            {robots.map((robot) => {
              const point = live[robot.id]
              const status = point
                ? point.connectivity === 'CONNECTED' && point.errorCodes.length === 0 ? 'ONLINE' : 'DEGRADED'
                : robot.operationalStatus
              return (
                <tr
                  key={robot.id}
                  className={selectedId === robot.id ? 'selected' : ''}
                  onClick={() => onSelect(robot.id)}
                  tabIndex={0}
                  onKeyDown={(event) => event.key === 'Enter' && onSelect(robot.id)}
                >
                  <td><strong>{robot.id}</strong><small>{robot.ward} · F{point?.floor ?? robot.floor}</small></td>
                  <td><span className={`status ${status.toLowerCase()}`}>{status}</span></td>
                  <td>{formatPercent(point?.batteryPercent ?? robot.batteryPercent)}</td>
                  <td>{point?.missionState ?? '—'}</td>
                  <td>{timeAgo(point?.recordedAt ?? robot.lastSeenAt)}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </section>
  )
}

