<script setup>
import { onMounted, ref, watch } from 'vue'
import { api } from '@/api/client'
import CustomerBookingDetailPage from '@/pages/customer/CustomerBookingDetailPage.vue'

const status = ref('')
const page = ref({ items: [], total: 0, page: 1, pageSize: 10 })
const query = ref({ from: '', to: '', page: 1, pageSize: 10 })
const loading = ref(false)
const error = ref('')
const detailOpen = ref(false)
const detailBookingId = ref('')

function formatTime(value) {
  if (!value) return '—'
  const t = String(value).trim()
  const m = t.match(/^(\d{4}-\d{2}-\d{2})[T\s](\d{2}:\d{2})/)
  if (m) return `${m[1]} ${m[2]}`
  return t
}

function statusClass(v) {
  const s = String(v ?? '').toLowerCase()
  if (s === 'pending') return 'status--pending'
  if (s === 'confirmed' || s === 'completed') return 'status--ok'
  if (s === 'cancelled' || s === 'rejected') return 'status--bad'
  return ''
}

async function load() {
  error.value = ''
  loading.value = true
  try {
    const params = {
      page: query.value.page,
      pageSize: query.value.pageSize
    }
    if (status.value) params.status = status.value
    if (query.value.from) params.from = query.value.from
    if (query.value.to) params.to = query.value.to
    page.value = await api.listMyBookings(params)
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    page.value = { items: [], total: 0, page: 1, pageSize: 10 }
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.value.page = 1
  load()
}

function prevPage() {
  if (query.value.page <= 1 || loading.value) return
  query.value.page -= 1
  load()
}

function nextPage() {
  if (loading.value || query.value.page >= totalPages()) return
  query.value.page += 1
  load()
}

function totalPages() {
  const total = Number(page.value.total || 0)
  const size = Number(page.value.pageSize || query.value.pageSize || 10)
  return Math.max(1, Math.ceil(total / size))
}

function openDetail(id) {
  if (!id) return
  detailBookingId.value = id
  detailOpen.value = true
}

function closeDetail() {
  detailOpen.value = false
  detailBookingId.value = ''
}

onMounted(load)
watch(status, () => onSearch())
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>My Bookings</h1>
      <p class="subtitle">Track your booking requests and check the latest status.</p>
    </header>

    <div class="toolbar card">
      <label class="field">
        <span class="label">Status</span>
        <select v-model="status" class="input">
          <option value="">All</option>
          <option value="Pending">Pending</option>
          <option value="Confirmed">Confirmed</option>
          <option value="Cancelled">Cancelled</option>
          <option value="Completed">Completed</option>
          <option value="Rejected">Rejected</option>
        </select>
      </label>
      <label class="field">
        <span class="label">From</span>
        <input v-model="query.from" type="date" class="input" />
      </label>
      <label class="field">
        <span class="label">To</span>
        <input v-model="query.to" type="date" class="input" />
      </label>
      <label class="field">
        <span class="label">Page Size</span>
        <select v-model.number="query.pageSize" class="input">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
      </label>
      <button type="button" class="btn btn--ghost" :disabled="loading" @click="onSearch">Search</button>
      <button type="button" class="btn" :disabled="loading" @click="load">Refresh</button>
    </div>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>

    <div v-if="loading && !(page.items || []).length" class="card muted">Loading…</div>

    <div v-else-if="!(page.items || []).length && !error" class="empty">
      <div class="empty__title">No bookings yet</div>
      <p class="muted">Create a booking from a specialist detail page.</p>
    </div>

    <ul v-else class="list">
      <li v-for="(b, idx) in page.items" :key="b.id" class="card row">
        <div class="main">
          <div class="booking-no">Booking #{{ idx + 1 }}</div>
          <div class="muted small">Schedule: {{ formatTime(b.time ?? b.startTime) }}</div>
          <div class="muted small">Specialist: {{ b.specialistName ?? b.specialistId ?? '—' }}</div>
          <div class="status" :class="statusClass(b.status)">{{ b.status ?? '—' }}</div>
        </div>
        <button type="button" class="link" @click="openDetail(b.id)">
          Details
        </button>
      </li>
    </ul>

    <div v-if="(page.items || []).length" class="pager">
      <button type="button" class="btn btn--ghost" :disabled="loading || query.page <= 1" @click="prevPage">
        Prev
      </button>
      <span class="pager__info">Page {{ page.page }} / {{ totalPages() }} · Total {{ page.total }}</span>
      <button type="button" class="btn btn--ghost" :disabled="loading || query.page >= totalPages()" @click="nextPage">
        Next
      </button>
    </div>

    <div v-if="detailOpen" class="modal-backdrop" @click.self="closeDetail">
      <section class="modal-card">
        <div class="modal-head">
          <h3 class="modal-title">Booking Details</h3>
          <button type="button" class="modal-close" @click="closeDetail">×</button>
        </div>
        <CustomerBookingDetailPage
          v-if="detailBookingId"
          :id="detailBookingId"
          embedded
          @close="closeDetail"
        />
      </section>
    </div>
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
  font-size: 14px;
  color: #4b5563;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 12px;
  margin-top: 10px;
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}
.field {
  display: grid;
  gap: 8px;
}
.label {
  font-size: 13px;
  color: #4b5563;
  font-weight: 600;
}
.input {
  min-width: 160px;
  height: 44px;
  padding: 0 12px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #111827;
}
.btn {
  padding: 0 16px;
  border-radius: 0;
  border: 1px solid #D9533C;
  background: #D9533C;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
  height: 44px;
}
.btn--ghost {
  border: 1px solid #202124;
  background: #ffffff;
  color: #202124;
}
.btn:disabled {
  opacity: 0.6;
}
.muted {
  color: #6b7280;
}
.small {
  font-size: 12px;
  margin-top: 4px;
}
.card {
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.list {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}
.main {
  min-width: 0;
}
.booking-no {
  font-weight: 700;
  font-size: 16px;
  color: #111827;
}
.status {
  margin-top: 8px;
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 0;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #374151;
}
.status--pending {
  background: #fff7ed;
  border-color: #fdba74;
  color: #9a3412;
}
.status--ok {
  background: #ecfdf3;
  border-color: #86efac;
  color: #166534;
}
.status--bad {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #991b1b;
}
.mono {
  font-family: ui-monospace, monospace;
  font-size: 12px;
}
.link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 100px;
  height: 40px;
  padding: 0 12px;
  border-radius: 0;
  border: 1px solid #D9533C;
  background: #D9533C;
  color: #ffffff;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
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
.empty {
  margin-top: 16px;
  padding: 18px;
  border: 1px dashed #d1d5db;
  border-radius: 0;
  background: #fafafa;
}
.empty__title {
  font-weight: 700;
  margin-bottom: 6px;
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
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(17, 24, 39, 0.42);
}
.modal-card {
  width: min(100%, 980px);
  max-height: calc(100vh - 40px);
  overflow: auto;
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  padding: 14px 16px 16px;
  box-shadow: 0 16px 36px rgba(17, 24, 39, 0.16);
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}
.modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}
.modal-close {
  width: 32px;
  height: 32px;
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #ffffff;
  color: #111827;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

@media (max-width: 720px) {
  .row {
    flex-direction: column;
    align-items: stretch;
  }

  .link {
    width: 100%;
  }

  .modal-card {
    padding: 10px;
  }
}
</style>

