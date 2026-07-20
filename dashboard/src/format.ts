import { formatDistanceToNowStrict } from 'date-fns'

export function compactNumber(value: number): string {
  return new Intl.NumberFormat('en-US', { notation: 'compact' }).format(value)
}

export function formatPercent(value: number | null): string {
  return value == null ? '—' : `${Math.round(value)}%`
}

export function timeAgo(value: string | null): string {
  if (!value) return 'No signal'
  return formatDistanceToNowStrict(new Date(value), { addSuffix: true })
}

export function readableEnum(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

