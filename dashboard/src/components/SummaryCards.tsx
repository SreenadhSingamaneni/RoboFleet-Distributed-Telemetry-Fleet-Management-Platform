import type { FleetSummary } from '../types'
import { compactNumber, formatPercent } from '../format'

export function SummaryCards({ summary }: { summary?: FleetSummary }) {
  const cards = [
    { label: 'Fleet online', value: summary ? `${summary.onlineRobots}/${summary.totalRobots}` : '—', tone: 'good' },
    { label: 'Degraded', value: summary ? compactNumber(summary.degradedRobots) : '—', tone: 'warning' },
    { label: 'Offline', value: summary ? compactNumber(summary.offlineRobots) : '—', tone: 'neutral' },
    { label: 'Critical alerts', value: summary ? compactNumber(summary.criticalAlerts) : '—', tone: 'critical' },
    { label: 'Average battery', value: summary ? formatPercent(summary.averageBatteryPercent) : '—', tone: 'energy' },
  ]
  return (
    <section className="summary-grid" aria-label="Fleet summary">
      {cards.map((card) => (
        <article className={`summary-card ${card.tone}`} key={card.label}>
          <span>{card.label}</span>
          <strong>{card.value}</strong>
        </article>
      ))}
    </section>
  )
}

