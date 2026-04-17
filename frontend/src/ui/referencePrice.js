export const DEFAULT_REFERENCE_CURRENCY = 'CNY'

function normalizeAmount(value) {
  const n = Number(value)
  if (!Number.isFinite(n)) return '--'
  if (Number.isInteger(n)) return String(n)
  return n.toFixed(2).replace(/\.?0+$/, '')
}

export function formatReferencePrice(amount, currency = DEFAULT_REFERENCE_CURRENCY) {
  const value = normalizeAmount(amount)
  const unit = String(currency || DEFAULT_REFERENCE_CURRENCY).trim() || DEFAULT_REFERENCE_CURRENCY
  if (value === '--') return '--'
  return `${value} ${unit}`
}
