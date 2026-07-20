import { useQuery } from '@tanstack/react-query'
import { format } from 'date-fns'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { fleetApi } from '../api'
import { formatPercent, readableEnum } from '../format'
import type { Robot, Telemetry } from '../types'

interface Props {
  robot?: Robot
  live?: Telemetry
}

export function RobotDetail({ robot, live }: Props) {
  const history = useQuery({
    queryKey: ['telemetry-history', robot?.id],
    queryFn: () => fleetApi.telemetryHistory(robot!.id),
    enabled: Boolean(robot),
    refetchInterval: 15_000,
  })
  if (!robot) return <section className="panel detail-panel empty-state">Select a robot to inspect telemetry</section>

  const chartData = [...(history.data ?? []), ...(live ? [live] : [])]
    .slice(-120)
    .map((point) => ({
      time: format(new Date(point.recordedAt), 'HH:mm:ss'),
      battery: point.batteryPercent,
      temperature: point.temperatureCelsius,
    }))

  return (
    <section className="panel detail-panel">
      <div className="panel-heading">
        <div><span className="eyebrow">Robot detail</span><h2>{robot.id}</h2></div>
        <span className="model-chip">{robot.model}</span>
      </div>
      <div className="detail-metrics">
        <div><span>Battery</span><strong>{formatPercent(live?.batteryPercent ?? robot.batteryPercent)}</strong></div>
        <div><span>Speed</span><strong>{live ? `${live.speedMps.toFixed(2)} m/s` : '—'}</strong></div>
        <div><span>Temperature</span><strong>{live ? `${live.temperatureCelsius.toFixed(1)}°C` : '—'}</strong></div>
        <div><span>Mission</span><strong>{live ? readableEnum(live.missionState) : '—'}</strong></div>
      </div>
      <div className="chart-wrap" aria-label="Battery and temperature telemetry chart">
        <ResponsiveContainer width="100%" height={230}>
          <LineChart data={chartData} margin={{ top: 10, right: 8, left: -20, bottom: 0 }}>
            <CartesianGrid stroke="#203149" strokeDasharray="3 3" />
            <XAxis dataKey="time" stroke="#7890ad" tick={{ fontSize: 11 }} minTickGap={28} />
            <YAxis yAxisId="battery" domain={[0, 100]} stroke="#7890ad" tick={{ fontSize: 11 }} />
            <YAxis yAxisId="temp" orientation="right" domain={[20, 90]} hide />
            <Tooltip contentStyle={{ background: '#0c1a2b', border: '1px solid #29415f' }} />
            <Line yAxisId="battery" dataKey="battery" stroke="#31d6a4" dot={false} strokeWidth={2} />
            <Line yAxisId="temp" dataKey="temperature" stroke="#ffb84d" dot={false} strokeWidth={2} />
          </LineChart>
        </ResponsiveContainer>
      </div>
      <div className="chart-legend"><span className="battery-line">Battery %</span><span className="temp-line">Temperature °C</span></div>
    </section>
  )
}

