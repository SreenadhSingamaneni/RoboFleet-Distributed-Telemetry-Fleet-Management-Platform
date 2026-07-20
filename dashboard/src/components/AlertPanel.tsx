import { readableEnum, timeAgo } from '../format'
import type { FleetAlert } from '../types'

interface Props {
  alerts: FleetAlert[]
  onAcknowledge: (id: string) => void
}

export function AlertPanel({ alerts, onAcknowledge }: Props) {
  return (
    <section className="panel alert-panel">
      <div className="panel-heading">
        <div><span className="eyebrow">Requires attention</span><h2>Active alerts</h2></div>
        <span className="panel-count">{alerts.length}</span>
      </div>
      <div className="alert-list">
        {alerts.length === 0 && <div className="empty-state">No open alerts</div>}
        {alerts.map((alert) => (
          <article className={`alert-item ${alert.severity.toLowerCase()}`} key={alert.id}>
            <div className="alert-topline">
              <span className="severity">{alert.severity}</span>
              <time>{timeAgo(alert.lastTriggeredAt)}</time>
            </div>
            <strong>{readableEnum(alert.type)}</strong>
            <p>{alert.message}</p>
            <div className="alert-footer">
              <span>{alert.robotId} · {alert.occurrences}×</span>
              {alert.status === 'OPEN' && (
                <button type="button" onClick={() => onAcknowledge(alert.id)}>Acknowledge</button>
              )}
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}

