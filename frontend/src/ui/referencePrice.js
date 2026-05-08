export const DEFAULT_REFERENCE_CURRENCY = "CNY";

// keep amount compact; preserve decimals when needed
function normalizeAmount(value) {
  const n = Number(value);
  if (!Number.isFinite(n)) return "--";
  if (Number.isInteger(n)) return String(n);
  return n.toFixed(2).replace(/\.?0+$/, "");
}

export function formatReferencePrice(
  amount,
  currency = DEFAULT_REFERENCE_CURRENCY,
) {
  // build consistent reference label for specialist cards and details
  const value = normalizeAmount(amount);
  const unit =
    String(currency || DEFAULT_REFERENCE_CURRENCY).trim() ||
    DEFAULT_REFERENCE_CURRENCY;
  if (value === "--") return "--";
  return `${value} ${unit}/hour`;
}
