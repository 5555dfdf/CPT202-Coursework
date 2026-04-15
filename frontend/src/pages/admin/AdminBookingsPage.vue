<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'

const PAGE_SIZE = 10
const page = ref({ items: [], total: 0, page: 1, pageSize: PAGE_SIZE })
const loading = ref(false)
const error = ref('')
const missingApi = ref(false)
const expandedBookingId = ref('')
const detailMap = ref({})
const detailErrorMap = ref({})
const detailLoadingId = ref('')
const bookingSearchQuery = ref('')

const totalBookings = computed(() => {
  return Math.max(Number(page.value?.total) || 0, (page.value?.items || []).length)
})

const bookingCountLabel = computed(() => {
  const count = totalBookings.value
  return `${count} booking${count === 1 ? '' : 's'}`
})
const filteredBookings = computed(() => {
  const query = bookingSearchQuery.value.trim().toLowerCase()
  const items = page.value?.items || []
  if (!query) return items

  return items.filter((row) => {
    const haystack = [
      bookingId(row),
      bookingStatus(row),
      bookingTime(row),
      customerLabel(row),
      specialistLabel(row)
    ]
      .join(' ')
      .toLowerCase()

    return haystack.includes(query)
  })
})

function bookingId(row) {
  return String(row?.id ?? '').trim()
}

function bookingStatus(row) {
  return String(row?.status ?? '').trim() || '--'
}

function bookingTime(row) {
  return String(row?.time ?? row?.startTime ?? '').trim() || '--'
}

function customerLabel(row) {
  return String(row?.customerName ?? row?.customerId ?? '').trim() || '--'
}

function specialistLabel(row) {
  return String(row?.specialistName ?? row?.specialistId ?? '').trim() || '--'
}

function bookingNote(row) {
  return String(row?.note ?? '').trim() || '--'
}

function formatPrice(value) {
  const amount = Number(value)
  if (!Number.isFinite(amount)) return '--'
  return amount.toFixed(2)
}

function detailRecord(id) {
  return detailMap.value[id] ?? null
}

function detailError(id) {
  return detailErrorMap.value[id] ?? ''
}

function statusClass(row) {
  const status = bookingStatus(row).toLowerCase()
  if (status === 'confirmed' || status === 'completed') return 'status-pill status-pill--positive'
  if (status === 'cancelled' || status === 'rejected') return 'status-pill status-pill--negative'
  return 'status-pill'
}

async function load() {
  error.value = ''
  missingApi.value = false
  loading.value = true
  expandedBookingId.value = ''
  detailMap.value = {}
  detailErrorMap.value = {}

  try {
    const current = Number(page.value?.page) || 1
    page.value = await api.adminListBookings({ page: current, pageSize: PAGE_SIZE })
  } catch (e) {
    if (e?.status === 404) {
      missingApi.value = true
      error.value = 'Admin bookings endpoint is not available (404)'
      showAlertModal({ type: 'warn', message: error.value })
    } else {
      error.value = e?.message || 'Failed to load bookings'
      showAlertModal({ type: 'error', message: error.value })
    }
    page.value = { items: [], total: 0, page: 1, pageSize: PAGE_SIZE }
  } finally {
    loading.value = false
  }
}

function totalPages() {
  const total = Number(page.value?.total || 0)
  return Math.max(1, Math.ceil(total / PAGE_SIZE))
}

async function prevPage() {
  if (loading.value) return
  const current = Number(page.value?.page) || 1
  if (current <= 1) return
  page.value = { ...page.value, page: current - 1 }
  await load()
}

async function nextPage() {
  if (loading.value) return
  const current = Number(page.value?.page) || 1
  if (current >= totalPages()) return
  page.value = { ...page.value, page: current + 1 }
  await load()
}

async function toggleBooking(row) {
  const id = bookingId(row)
  if (!id) return

  if (expandedBookingId.value === id) {
    expandedBookingId.value = ''
    return
  }

  expandedBookingId.value = id
  if (detailRecord(id) || detailLoadingId.value === id) return

  detailErrorMap.value = { ...detailErrorMap.value, [id]: '' }
  detailLoadingId.value = id

  try {
    const detail = await api.adminGetBooking(id)
    detailMap.value = { ...detailMap.value, [id]: detail }
  } catch (e) {
    const msg =
      e?.status === 404 ? 'Booking detail endpoint is not available (404)' : e?.message || 'Failed to load booking detail'
    detailErrorMap.value = {
      ...detailErrorMap.value,
      [id]: msg
    }
    showAlertModal({ type: e?.status === 404 ? 'warn' : 'error', message: msg })
  } finally {
    if (detailLoadingId.value === id) {
      detailLoadingId.value = ''
    }
  }
}

onMounted(load)
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Booking Management</h1>
      <p class="subtitle">
        Review all bookings and inspect booking details inline.
      </p>
    </header>

    <section class="calc-card list-card">
      <div class="list-toolbar">
        <div class="toolbar-title">
          <h2 class="card-title">All Bookings</h2>
          <p class="list-note">Click any booking row to expand its detail.</p>
        </div>
        <div class="toolbar-actions">
          <input
            v-model.trim="bookingSearchQuery"
            class="input search-input"
            type="text"
            placeholder="Search bookings"
            aria-label="Search bookings"
          />
          <button type="button" class="btn-neutral btn-refresh" :disabled="loading" @click="load">
            {{ loading ? 'Loading...' : 'Refresh' }}
          </button>
        </div>
      </div>

      <div class="toolbar-meta">
        <span class="meta-pill">{{ bookingCountLabel }}</span>
      </div>

      <div v-if="loading && !(page.items || []).length" class="state">
        Loading bookings...
      </div>

      <div v-else-if="!(page.items || []).length && !missingApi" class="state state--empty">
        No booking data.
      </div>

      <div v-else-if="!filteredBookings.length" class="state state--empty">
        No bookings matched your search.
      </div>

      <div v-else-if="(page.items || []).length" class="booking-list">
        <article
          v-for="booking in filteredBookings"
          :key="bookingId(booking)"
          class="booking-item"
        >
          <button type="button" class="booking-summary" @click="toggleBooking(booking)">
            <div class="summary-main">
              <div class="summary-id mono">{{ bookingId(booking) || '--' }}</div>
              <div class="summary-meta">
                <span class="summary-label">Time</span>
                <span class="summary-value">{{ bookingTime(booking) }}</span>
              </div>
            </div>

            <div class="summary-grid">
              <div class="summary-meta">
                <span class="summary-label">Status</span>
                <span :class="statusClass(booking)">{{ bookingStatus(booking) }}</span>
              </div>
              <div class="summary-meta">
                <span class="summary-label">Customer</span>
                <span class="summary-value">{{ customerLabel(booking) }}</span>
              </div>
              <div class="summary-meta">
                <span class="summary-label">Specialist</span>
                <span class="summary-value">{{ specialistLabel(booking) }}</span>
              </div>
            </div>
          </button>

          <div v-if="expandedBookingId === bookingId(booking)" class="booking-detail">
            <div v-if="detailLoadingId === bookingId(booking)" class="detail-state">
              Loading booking detail...
            </div>

            <div v-else-if="detailError(bookingId(booking))" class="state muted">
              {{ detailError(bookingId(booking)) }}
            </div>

            <div v-else-if="detailRecord(bookingId(booking))" class="detail-grid">
              <div class="detail-row">
                <span class="detail-key">Booking ID</span>
                <span class="detail-value mono">{{ detailRecord(bookingId(booking))?.id ?? '--' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-key">Status</span>
                <span class="detail-value">{{ detailRecord(bookingId(booking))?.status ?? '--' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-key">Customer</span>
                <span class="detail-value">
                  {{ detailRecord(bookingId(booking))?.customerName ?? detailRecord(bookingId(booking))?.customerId ?? '--' }}
                </span>
              </div>
              <div class="detail-row">
                <span class="detail-key">Customer ID</span>
                <span class="detail-value mono">{{ detailRecord(bookingId(booking))?.customerId ?? '--' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-key">Specialist</span>
                <span class="detail-value">
                  {{ detailRecord(bookingId(booking))?.specialistName ?? detailRecord(bookingId(booking))?.specialistId ?? '--' }}
                </span>
              </div>
              <div class="detail-row">
                <span class="detail-key">Specialist ID</span>
                <span class="detail-value mono">{{ detailRecord(bookingId(booking))?.specialistId ?? '--' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-key">Time</span>
                <span class="detail-value">
                  {{ detailRecord(bookingId(booking))?.time ?? detailRecord(bookingId(booking))?.startTime ?? '--' }}
                </span>
              </div>
              <div class="detail-row">
                <span class="detail-key">Price</span>
                <span class="detail-value">{{ formatPrice(detailRecord(bookingId(booking))?.price) }}</span>
              </div>
              <div class="detail-row detail-row--full">
                <span class="detail-key">Note</span>
                <span class="detail-value detail-value--multiline">{{ bookingNote(detailRecord(bookingId(booking))) }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-key">Created</span>
                <span class="detail-value">{{ detailRecord(bookingId(booking))?.createdAt ?? '--' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-key">Updated</span>
                <span class="detail-value">{{ detailRecord(bookingId(booking))?.updatedAt ?? '--' }}</span>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-if="(page.items || []).length && !missingApi" class="pager">
        <button type="button" class="btn-neutral" :disabled="loading || (page.page ?? 1) <= 1" @click="prevPage">
          Prev
        </button>
        <span class="pager__info">Page {{ page.page }} / {{ totalPages() }} · Total {{ page.total }}</span>
        <button
          type="button"
          class="btn-neutral"
          :disabled="loading || (page.page ?? 1) >= totalPages()"
          @click="nextPage"
        >
          Next
        </button>
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

.calc-card {
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  padding: 16px;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}

.list-card {
  margin-top: 14px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input {
  width: 100%;
  height: 44px;
  padding: 0 12px;
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #f8f5f2;
  color: #111827;
}

.search-input {
  width: min(320px, 50vw);
}

.btn-neutral {
  height: 40px;
  padding: 0 14px;
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #f8f5f2;
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-neutral:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-refresh {
  border-color: #a94442;
  background: #a94442;
  color: #ffffff;
}

.banner {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 0;
  font-size: 13px;
}

.banner--inline {
  margin-top: 0;
}

.banner--error {
  border: 1px solid rgba(248, 113, 113, 0.45);
  background: rgba(248, 113, 113, 0.12);
  color: #991b1b;
}

.banner--warn {
  border: 1px solid rgba(251, 191, 36, 0.45);
  background: rgba(251, 191, 36, 0.12);
  color: #92400e;
}

.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.list-note {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.toolbar-meta {
  margin: 12px 0;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #374151;
  font-size: 12px;
  font-weight: 700;
}

.pager {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.pager__info {
  font-size: 13px;
  color: #475569;
}

.state {
  margin-top: 4px;
  padding: 16px 14px;
  border: 1px solid #eceff3;
  background: #fafafa;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.state--empty {
  border-style: dashed;
}

.booking-list {
  display: grid;
  gap: 10px;
}

.booking-item {
  border: 1px solid #d8d1cb;
  background: #ffffff;
}

.booking-summary {
  width: 100%;
  padding: 14px;
  border: 0;
  background: transparent;
  display: grid;
  gap: 12px;
  text-align: left;
  cursor: pointer;
}

.summary-main {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}

.summary-id {
  font-size: 13px;
  color: #111827;
  font-weight: 700;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-meta {
  display: grid;
  gap: 4px;
}

.summary-label {
  font-size: 12px;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.summary-value {
  font-size: 14px;
  color: #111827;
  font-weight: 600;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  width: fit-content;
  padding: 0 10px;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #374151;
  font-size: 12px;
  font-weight: 700;
}

.status-pill--positive {
  border-color: rgba(34, 197, 94, 0.32);
  background: rgba(34, 197, 94, 0.08);
  color: #166534;
}

.status-pill--negative {
  border-color: rgba(248, 113, 113, 0.38);
  background: rgba(248, 113, 113, 0.12);
  color: #991b1b;
}

.booking-detail {
  border-top: 1px solid #eceff3;
  padding: 14px;
  background: #fafafa;
}

.detail-state {
  color: #6b7280;
  font-size: 13px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 14px;
}

.detail-row {
  display: grid;
  gap: 4px;
}

.detail-row--full {
  grid-column: 1 / -1;
}

.detail-key {
  font-size: 12px;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-value {
  font-size: 13px;
  color: #111827;
  font-weight: 600;
}

.detail-value--multiline {
  white-space: pre-wrap;
  line-height: 1.55;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

@media (max-width: 900px) {
  .summary-main,
  .list-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .summary-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
