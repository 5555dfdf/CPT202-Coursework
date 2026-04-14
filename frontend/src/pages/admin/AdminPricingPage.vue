<script setup>
import { computed, ref } from 'vue'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'

const specialistId = ref('')
const resolvedSpecialistId = ref('')
const resolvedSpecialistName = ref('')
const duration = ref(null)
const type = ref('')
const loading = ref(false)
const error = ref('')
const quote = ref(null)
const quoteResults = ref([])
const resultMode = ref('idle')
const history = ref([])
const specialistDirectory = ref([])
const specialistDirectoryLoaded = ref(false)

const durationOptions = [30, 45, 60, 90]
const sessionTypeOptions = [
  { label: 'Online', value: 'online' },
  { label: 'Offline', value: 'offline' }
]

const hasDuration = computed(() => {
  const mins = Number(duration.value)
  return Number.isFinite(mins) && mins > 0
})

const hasType = computed(() => !!String(type.value || '').trim())
const hasQuote = computed(() => resultMode.value === 'single' && !!quote.value)
const hasResultList = computed(() => resultMode.value === 'list' && quoteResults.value.length > 0)
const isListMode = computed(() => resultMode.value === 'list')
const resultCountLabel = computed(() => {
  const count = quoteResults.value.length
  return `${count} result${count === 1 ? '' : 's'}`
})

const quoteAmount = computed(
  () => quote.value?.amount ?? quote.value?.totalAmount ?? quote.value?.total ?? quote.value?.price ?? null
)
const quoteCurrency = computed(() => quote.value?.currency ?? 'USD')
const quoteSpecialist = computed(() => {
  return buildSpecialistLabel(
    quote.value?.specialistId ?? resolvedSpecialistId.value ?? specialistId.value.trim(),
    resolvedSpecialistName.value
  )
})
const quoteDuration = computed(() => {
  const fallback = Number(duration.value)
  const value = quote.value?.duration ?? (Number.isFinite(fallback) ? fallback : null)
  return Number.isFinite(Number(value)) && Number(value) > 0 ? Number(value) : null
})
const quoteType = computed(() => quote.value?.type ?? type.value ?? '')
const quoteTypeLabel = computed(() => formatTypeLabel(quoteType.value))
const formattedAmount = computed(() => formatCurrency(quoteAmount.value, quoteCurrency.value))
const quoteSummary = computed(() => buildQuoteSummary(quoteDuration.value, quoteType.value))
const previewHint = computed(() => {
  if (hasQuote.value) return 'Exact quote result'
  if (hasResultList.value) return 'Available quote combinations'
  if (loading.value) return 'Calculating quotes...'
  return 'Enter a specialist only to browse combinations, or add all filters for one exact quote.'
})
const emptyPreviewMessage = computed(() => {
  if (loading.value) return 'Calculating quotes...'
  if (resultMode.value === 'list') {
    return 'No available quote combinations were found for the current specialist and filters.'
  }
  return 'Enter a specialist only to browse all available quote combinations, or add duration and session type for a single exact quote.'
})

function getTypeOrder(value) {
  return sessionTypeOptions.findIndex((option) => option.value === value)
}

function formatTypeLabel(value) {
  const normalized = String(value || '').trim().toLowerCase()
  const match = sessionTypeOptions.find((option) => option.value === normalized)
  return match?.label ?? (normalized ? normalized[0].toUpperCase() + normalized.slice(1) : '--')
}

function formatDurationLabel(value) {
  const mins = Number(value)
  return Number.isFinite(mins) && mins > 0 ? `${mins} minutes` : '--'
}

function buildSpecialistLabel(idValue, nameValue = '') {
  const id = String(idValue || '').trim()
  const name = String(nameValue || '').trim()
  if (name && id) return `${name} (${id})`
  return name || id || '--'
}

function formatCurrency(amountValue, currencyValue = 'USD') {
  const amount = Number(amountValue)
  if (!Number.isFinite(amount)) return '--'
  try {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currencyValue || 'USD',
      maximumFractionDigits: 2
    }).format(amount)
  } catch {
    return `${amount.toFixed(2)} ${currencyValue || ''}`.trim()
  }
}

function buildQuoteSummary(durationValue, typeValue) {
  const mins = Number(durationValue)
  const normalizedType = String(typeValue || '').toLowerCase()
  const typeText = normalizedType ? ` ${normalizedType}` : ''
  if (!Number.isFinite(mins) || mins <= 0) return `Consultation session${typeText}`.trim()
  if (mins % 60 === 0) {
    const hours = mins / 60
    return `${hours} hour${hours > 1 ? 's' : ''}${typeText} session`
  }
  return `${mins} minute${typeText} session`
}

function toggleDuration(mins) {
  duration.value = Number(duration.value) === mins ? null : mins
}

function toggleType(nextType) {
  type.value = type.value === nextType ? '' : nextType
}

function resetResults() {
  quote.value = null
  quoteResults.value = []
  resultMode.value = 'idle'
}

async function ensureSpecialistDirectory() {
  if (specialistDirectoryLoaded.value) return specialistDirectory.value
  const page = await api.listSpecialists({ pageSize: 100 })
  specialistDirectory.value = Array.isArray(page?.items) ? page.items : []
  specialistDirectoryLoaded.value = true
  return specialistDirectory.value
}

async function resolveSpecialistReference(inputValue) {
  const query = String(inputValue || '').trim()
  if (!query) return { specialistId: '', specialistName: '' }

  try {
    const items = await ensureSpecialistDirectory()
    const normalizedQuery = query.toLowerCase()
    const exactIdMatch = items.find((row) => String(row?.id ?? '').trim().toLowerCase() === normalizedQuery)
    if (exactIdMatch) {
      return {
        specialistId: String(exactIdMatch.id),
        specialistName: String(exactIdMatch.name ?? '').trim()
      }
    }

    const exactNameMatches = items.filter(
      (row) => String(row?.name ?? '').trim().toLowerCase() === normalizedQuery
    )
    if (exactNameMatches.length === 1) {
      return {
        specialistId: String(exactNameMatches[0].id),
        specialistName: String(exactNameMatches[0].name ?? '').trim()
      }
    }

    const partialNameMatches = items.filter((row) =>
      String(row?.name ?? '').trim().toLowerCase().includes(normalizedQuery)
    )
    if (partialNameMatches.length === 1) {
      return {
        specialistId: String(partialNameMatches[0].id),
        specialistName: String(partialNameMatches[0].name ?? '').trim()
      }
    }

    if (exactNameMatches.length > 1 || partialNameMatches.length > 1) {
      throw new Error('Multiple specialists matched that name. Please use the specialist ID.')
    }
  } catch (e) {
    if (e?.message === 'Multiple specialists matched that name. Please use the specialist ID.') {
      throw e
    }
  }

  return { specialistId: query, specialistName: '' }
}

function createQuoteRecord(source, fallback) {
  const amount = source?.amount ?? source?.totalAmount ?? source?.total ?? source?.price ?? null
  const currency = source?.currency ?? 'USD'
  const rawDuration = source?.duration ?? fallback.duration ?? null
  const rawType = source?.type ?? fallback.type ?? ''
  const normalizedDuration = Number.isFinite(Number(rawDuration)) && Number(rawDuration) > 0 ? Number(rawDuration) : null
  const normalizedType = String(rawType || '').trim().toLowerCase()
  const detail = String(source?.detail ?? source?.description ?? source?.summary ?? '').trim()
  const summary = buildQuoteSummary(normalizedDuration, normalizedType)

  return {
    id: [
      fallback.specialistId,
      normalizedDuration ?? 'na',
      normalizedType || 'na',
      amount ?? 'na',
      currency,
      detail || summary
    ].join(':'),
    specialistId: String(source?.specialistId ?? fallback.specialistId ?? '').trim(),
    specialistName: fallback.specialistName ?? '',
    specialistDisplay: buildSpecialistLabel(
      source?.specialistId ?? fallback.specialistId ?? '',
      fallback.specialistName ?? ''
    ),
    duration: normalizedDuration,
    type: normalizedType,
    amount,
    currency,
    summary,
    detail: detail && detail !== summary ? detail : ''
  }
}

function normalizeQuoteResponse(response, fallback) {
  const rows = Array.isArray(response) ? response : response ? [response] : []
  return rows
    .filter((row) => row && typeof row === 'object')
    .map((row) =>
      createQuoteRecord(row, {
        specialistId: fallback.specialistId,
        specialistName: fallback.specialistName,
        duration: row?.duration ?? fallback.duration ?? null,
        type: row?.type ?? fallback.type ?? ''
      })
    )
}

function dedupeQuoteResults(rows) {
  const map = new Map()
  for (const row of rows) {
    const key = [row.specialistId, row.duration ?? '', row.type || '', row.amount ?? '', row.currency || ''].join('|')
    if (!map.has(key)) map.set(key, row)
  }
  return Array.from(map.values())
}

function sortQuoteResults(rows) {
  return [...rows].sort((a, b) => {
    const durationA = a.duration ?? Number.MAX_SAFE_INTEGER
    const durationB = b.duration ?? Number.MAX_SAFE_INTEGER
    if (durationA !== durationB) return durationA - durationB

    const typeOrderA = getTypeOrder(a.type)
    const typeOrderB = getTypeOrder(b.type)
    if (typeOrderA !== typeOrderB) return typeOrderA - typeOrderB

    const amountA = Number(a.amount)
    const amountB = Number(b.amount)
    if (Number.isFinite(amountA) && Number.isFinite(amountB) && amountA !== amountB) {
      return amountA - amountB
    }

    return String(a.specialistId || '').localeCompare(String(b.specialistId || ''))
  })
}

function appendHistoryItems(rows) {
  if (!Array.isArray(rows) || !rows.length) return
  const createdAt = new Date().toISOString()
  const items = rows.map((row, index) => ({
    id: `${Date.now()}-${index}-${Math.random().toString(36).slice(2, 8)}`,
    specialistId: row.specialistId,
    specialistDisplay: row.specialistDisplay,
    duration: row.duration,
    type: row.type,
    amount: row.amount,
    currency: row.currency,
    summary: row.detail || row.summary,
    createdAt
  }))
  history.value = items.concat(history.value)
}

function clearHistory() {
  history.value = []
}

function buildQuoteCombinations() {
  const selectedDuration = hasDuration.value ? Number(duration.value) : null
  const selectedType = hasType.value ? String(type.value).trim().toLowerCase() : ''
  const durations = selectedDuration ? [selectedDuration] : durationOptions
  const types = selectedType ? [selectedType] : sessionTypeOptions.map((option) => option.value)

  return durations.flatMap((mins) =>
    types.map((sessionType) => ({
      duration: mins,
      type: sessionType
    }))
  )
}

async function onQuote() {
  error.value = ''
  resetResults()

  if (!specialistId.value.trim()) {
    error.value = 'Please enter a specialist name or ID'
    showAlertModal({ type: 'error', message: error.value })
    return
  }

  loading.value = true

  try {
    const resolved = await resolveSpecialistReference(specialistId.value)
    resolvedSpecialistId.value = resolved.specialistId
    resolvedSpecialistName.value = resolved.specialistName

    if (hasDuration.value && hasType.value) {
      const response = await api.quotePricing({
        specialistId: resolved.specialistId,
        duration: Number(duration.value),
        type: String(type.value).trim().toLowerCase()
      })
      const records = normalizeQuoteResponse(response, {
        specialistId: resolved.specialistId,
        specialistName: resolved.specialistName,
        duration: Number(duration.value),
        type: String(type.value).trim().toLowerCase()
      })
      const [firstRecord] = records

      if (!firstRecord) {
        error.value = 'No quote available for the selected specialist and filters'
        resultMode.value = 'single'
        showAlertModal({ type: 'warn', message: error.value })
        return
      }

      quote.value = {
        specialistId: firstRecord.specialistId,
        duration: firstRecord.duration,
        type: firstRecord.type,
        amount: firstRecord.amount,
        currency: firstRecord.currency,
        detail: firstRecord.detail
      }
      resultMode.value = 'single'
      appendHistoryItems([firstRecord])
      return
    }

    const combinations = buildQuoteCombinations()
    const settled = await Promise.allSettled(
      combinations.map((combo) =>
        api.quotePricing({
          specialistId: resolved.specialistId,
          duration: combo.duration,
          type: combo.type
        }).then((response) =>
          normalizeQuoteResponse(response, {
            specialistId: resolved.specialistId,
            specialistName: resolved.specialistName,
            duration: combo.duration,
            type: combo.type
          })
        )
      )
    )

    const successful = settled.filter((row) => row.status === 'fulfilled').flatMap((row) => row.value)
    const normalizedResults = sortQuoteResults(dedupeQuoteResults(successful))

    if (!normalizedResults.length) {
      const firstFailure = settled.find((row) => row.status === 'rejected')
      error.value =
        firstFailure?.reason?.message || 'No quote combinations available for this specialist'
      resultMode.value = 'list'
      showAlertModal({ type: 'warn', message: error.value })
      return
    }

    quoteResults.value = normalizedResults
    resultMode.value = 'list'
    appendHistoryItems(normalizedResults)
  } catch (e) {
    error.value = e?.message || 'Failed to get quote'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Pricing Calculator</h1>
      <p class="subtitle">
        Calculate consultation pricing based on specialist, duration, and session type.
      </p>
    </header>

    <div class="calculator-layout">
      <section class="calc-card setup-card">
        <div class="panel-head">
          <div>
            <h2 class="card-title">Quote Setup</h2>
            <p class="panel-note">Leave duration or session type unselected to browse available combinations.</p>
          </div>
        </div>

        <div class="setup-card__body">
          <label class="field">
            <span class="label">Specialist</span>
            <input
              v-model="specialistId"
              class="input"
              placeholder="Enter specialist ID or name (e.g. sp-1)"
            />
          </label>

          <div class="field">
            <div class="field-head">
              <span class="label">Duration</span>
              <span class="field-state" :class="{ 'field-state--selected': hasDuration }">
                {{ hasDuration ? `${duration} minutes` : 'Not selected' }}
              </span>
            </div>

            <div class="option-row">
              <button
                v-for="mins in durationOptions"
                :key="mins"
                type="button"
                class="option-btn"
                :class="{ 'option-btn--active': Number(duration) === mins }"
                @click="toggleDuration(mins)"
              >
                {{ mins }}m
              </button>
            </div>

            <p class="field-hint">
              {{ hasDuration ? 'Click the active option again to clear this filter.' : 'No duration selected yet.' }}
            </p>
          </div>

          <div class="field">
            <div class="field-head">
              <span class="label">Session Type</span>
              <span class="field-state" :class="{ 'field-state--selected': hasType }">
                {{ hasType ? formatTypeLabel(type) : 'Not selected' }}
              </span>
            </div>

            <div class="option-row type-row">
              <button
                v-for="option in sessionTypeOptions"
                :key="option.value"
                type="button"
                class="option-btn option-btn--type"
                :class="{ 'option-btn--active': type === option.value }"
                @click="toggleType(option.value)"
              >
                {{ option.label }}
              </button>
            </div>

            <p class="field-hint">
              {{ hasType ? 'Click the active option again to clear this filter.' : 'No session type selected yet.' }}
            </p>
          </div>

          <button type="button" class="btn-primary" :disabled="loading" @click="onQuote">
            {{ loading ? 'Calculating...' : 'Calculate Quote' }}
          </button>

        </div>
      </section>

      <section class="calc-card preview-card">
        <div class="panel-head panel-head--split">
          <div>
            <h2 class="card-title">Quote Preview</h2>
            <p class="panel-note">{{ previewHint }}</p>
          </div>
          <span v-if="isListMode && quoteResults.length" class="result-count">{{ resultCountLabel }}</span>
        </div>

        <div class="preview-card__body">
          <template v-if="hasQuote">
            <div class="amount-block">
              <div class="amount-label">Total Price</div>
              <div class="amount">{{ formattedAmount }}</div>
            </div>

            <p class="summary">{{ quoteSummary }}</p>

            <div v-if="quote.value?.detail" class="detail-copy">{{ quote.value.detail }}</div>

            <div class="detail-list">
              <div class="detail-row">
                <span class="detail-key">Specialist</span>
                <span class="detail-value">{{ quoteSpecialist }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-key">Duration</span>
                <span class="detail-value">{{ formatDurationLabel(quoteDuration) }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-key">Session Type</span>
                <span class="detail-value">{{ quoteTypeLabel }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-key">Currency</span>
                <span class="detail-value">{{ quoteCurrency || '--' }}</span>
              </div>
            </div>
          </template>

          <div v-else-if="hasResultList" class="results-list">
            <article v-for="row in quoteResults" :key="row.id" class="result-item">
              <div class="result-item__top">
                <div class="result-item__copy">
                  <div class="result-item__summary">{{ row.summary }}</div>
                  <div v-if="row.detail" class="result-item__detail">{{ row.detail }}</div>
                </div>
                <div class="result-item__amount">{{ formatCurrency(row.amount, row.currency) }}</div>
              </div>

              <div class="result-item__meta">
                <div class="detail-row">
                  <span class="detail-key">Specialist</span>
                  <span class="detail-value">{{ row.specialistDisplay }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-key">Duration</span>
                  <span class="detail-value">{{ formatDurationLabel(row.duration) }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-key">Session Type</span>
                  <span class="detail-value">{{ formatTypeLabel(row.type) }}</span>
                </div>
                <div class="detail-row">
                  <span class="detail-key">Currency</span>
                  <span class="detail-value">{{ row.currency || '--' }}</span>
                </div>
              </div>
            </article>
          </div>

          <div v-else class="empty-preview">
            {{ emptyPreviewMessage }}
          </div>
        </div>
      </section>
    </div>

    <section class="history-card">
      <div class="history-head">
        <div>
          <h2 class="history-title">Recent Calculations</h2>
          <p class="history-note">Temporary history. It resets when the page is refreshed.</p>
        </div>
        <button
          type="button"
          class="clear-btn"
          :disabled="!history.length"
          @click="clearHistory"
        >
          Clear History
        </button>
      </div>

      <div v-if="!history.length" class="history-empty">
        No calculations yet. Successful quotes will appear here.
      </div>

      <div v-else class="history-list">
        <article v-for="row in history" :key="row.id" class="history-item">
          <div class="history-main">
            <div class="history-amount">{{ formatCurrency(row.amount, row.currency) }}</div>
            <div class="history-summary">{{ row.summary }}</div>
          </div>
          <div class="history-meta">
            <span><b>Specialist:</b> {{ row.specialistDisplay || row.specialistId || '--' }}</span>
            <span><b>Duration:</b> {{ formatDurationLabel(row.duration) }}</span>
            <span><b>Type:</b> {{ formatTypeLabel(row.type) }}</span>
            <span><b>Currency:</b> {{ row.currency || '--' }}</span>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.page__header {
  margin: 8px 0 20px;
  padding: 0;
}

.page__header h1 {
  margin: 0;
  font-size: clamp(32px, 3.1vw, 38px);
  font-weight: 800;
  line-height: 1.12;
}

.subtitle {
  margin: 6px 0 0;
  color: #4b5563;
  font-size: 14px;
}

.calculator-layout {
  margin-top: 0;
  display: grid;
  grid-template-columns: minmax(300px, 0.95fr) minmax(320px, 1.05fr);
  gap: 14px;
  align-items: start;
}

.calc-card {
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  padding: 16px;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}

.setup-card,
.preview-card {
  display: flex;
  flex-direction: column;
  min-height: 360px;
  max-height: 460px;
}

.setup-card__body,
.preview-card__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.panel-head {
  margin-bottom: 12px;
}

.panel-head--split {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.panel-note {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.45;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.result-count {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #374151;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.field {
  display: grid;
  gap: 8px;
  margin-bottom: 14px;
}

.field-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.label {
  font-size: 13px;
  color: #4b5563;
  font-weight: 600;
}

.field-state {
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.field-state--selected {
  color: #111827;
}

.field-hint {
  margin: 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.45;
}

.input {
  height: 44px;
  padding: 0 12px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #111827;
}

.option-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-btn {
  min-width: 62px;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #f8f5f2;
  color: #374151;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.option-btn--type {
  min-width: 110px;
}

.option-btn--active {
  border-color: #202124;
  background: #202124;
  color: #ffffff;
}

.btn-primary {
  width: 100%;
  height: 44px;
  border: 1px solid #a94442;
  border-radius: 0;
  background: #a94442;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.banner {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 0;
  font-size: 13px;
}

.banner--error {
  border: 1px solid rgba(248, 113, 113, 0.45);
  background: rgba(248, 113, 113, 0.12);
  color: #991b1b;
}

.amount-block {
  border: 1px solid #e5e7eb;
  background: #fafafa;
  border-radius: 0;
  padding: 14px;
}

.amount-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.amount {
  font-size: clamp(30px, 4vw, 42px);
  line-height: 1.05;
  font-weight: 800;
  color: #111827;
}

.summary {
  margin: 12px 0 0;
  color: #374151;
  font-size: 14px;
}

.detail-copy {
  margin-top: 12px;
  padding: 12px 14px;
  border: 1px solid #eceff3;
  background: #fafafa;
  color: #374151;
  font-size: 13px;
  line-height: 1.5;
}

.detail-list {
  margin-top: 14px;
  border-top: 1px solid #eceff3;
  padding-top: 10px;
  display: grid;
  gap: 8px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.detail-key {
  color: #6b7280;
  font-size: 13px;
}

.detail-value {
  font-size: 13px;
  color: #111827;
  font-weight: 600;
  text-align: right;
}

.results-list {
  display: grid;
  gap: 10px;
}

.result-item {
  border: 1px solid #d8d1cb;
  background: #ffffff;
  padding: 14px;
  display: grid;
  gap: 12px;
}

.result-item__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.result-item__copy {
  display: grid;
  gap: 4px;
}

.result-item__summary {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.result-item__detail {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.45;
}

.result-item__amount {
  font-size: 22px;
  font-weight: 800;
  color: #111827;
  white-space: nowrap;
}

.result-item__meta {
  border-top: 1px solid #eceff3;
  padding-top: 10px;
  display: grid;
  gap: 8px;
}

.empty-preview {
  margin-top: 2px;
  border: 1px dashed #d1d5db;
  border-radius: 0;
  background: #fafafa;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
  padding: 16px 14px;
}

.history-card {
  margin-top: 14px;
  background: transparent;
  border: 0;
  border-radius: 0;
  padding: 0;
  box-shadow: none;
}

.history-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.history-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.history-note {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.clear-btn {
  height: 36px;
  padding: 0 12px;
  border: 1px solid #000000;
  border-radius: 0;
  background: #000000;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.clear-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.clear-btn:hover:not(:disabled) {
  filter: brightness(0.92);
}

.history-empty {
  margin-top: 12px;
  padding: 14px;
  border: 1px dashed #d1d5db;
  background: #fafafa;
  color: #6b7280;
  font-size: 13px;
}

.history-list {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.history-item {
  border: 1px solid #d8d1cb;
  background: #ffffff;
  padding: 12px 14px;
  display: grid;
  gap: 8px;
}

.history-main {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}

.history-amount {
  font-size: 20px;
  font-weight: 800;
  color: #111827;
}

.history-summary {
  font-size: 13px;
  color: #4b5563;
}

.history-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px 10px;
  font-size: 12px;
  color: #374151;
}

@media (max-width: 980px) {
  .calculator-layout {
    grid-template-columns: 1fr;
  }

  .setup-card,
  .preview-card {
    min-height: auto;
    max-height: none;
  }

  .setup-card__body,
  .preview-card__body {
    overflow: visible;
    padding-right: 0;
  }

  .history-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .history-main,
  .result-item__top,
  .panel-head--split {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .history-head {
    flex-direction: column;
  }
}
</style>
