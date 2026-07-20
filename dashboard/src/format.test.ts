import { describe, expect, it } from 'vitest'
import { formatPercent, readableEnum } from './format'

describe('format helpers', () => {
  it('formats nullable battery percentages', () => {
    expect(formatPercent(71.6)).toBe('72%')
    expect(formatPercent(null)).toBe('—')
  })

  it('turns wire enum values into readable labels', () => {
    expect(readableEnum('NAVIGATION_STALLED')).toBe('Navigation Stalled')
  })
})

