<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { api } from '@/api/client'

const status = ref('')
const page = ref({ items: [], total: 0, page: 1, pageSize: 10 })
const loading = ref(false)
const error = ref('')

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
    const params = {}
    if (status.value) params.status = status.value
    page.value = await api.listMyBookings(params)
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    page.value = { items: [], total: 0, page: 1, pageSize: 10 }
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(status, () => load())
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
          <div class="muted small">Specialist: {{ b.specialistName ?? 'Assigned specialist' }}</div>
          <div class="status" :class="statusClass(b.status)">{{ b.status ?? '—' }}</div>
        </div>
        <RouterLink class="link" :to="{ name: 'customer.bookingDetail', params: { id: b.id } }">
          Details
        </RouterLink>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.page__header h1 {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 800;
}
.subtitle {
  margin: 0;
  font-size: 14px;
  color: #5b6472;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 12px;
  margin-top: 14px;
  padding: 14px;
  border: 1px solid #e6e8ef;
  border-radius: 14px;
  background: #f8fafc;
}
.field {
  display: grid;
  gap: 6px;
}
.label {
  font-size: 13px;
  opacity: 0.85;
}
.input {
  min-width: 160px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d3d8e1;
  background: #ffffff;
  color: #111827;
}
.btn {
  padding: 10px 16px;
  border-radius: 10px;
  border: 1px solid #07c160;
  background: #07c160;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
  height: 42px;
}
.btn:disabled {
  opacity: 0.5;
}
.muted {
  opacity: 0.8;
}
.small {
  font-size: 12px;
  margin-top: 4px;
}
.card {
  padding: 16px;
  border: 1px solid #e6e8ef;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.04);
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
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid #d0d7e2;
  background: #f8fafc;
  color: #334155;
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
  height: 38px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid #07c160;
  background: #07c160;
  color: #ffffff;
  font-weight: 700;
  text-decoration: none;
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
  border: 1px dashed #cfd5df;
  border-radius: 14px;
  background: #fbfcfe;
}
.empty__title {
  font-weight: 700;
  margin-bottom: 6px;
}

@media (max-width: 720px) {
  .row {
    flex-direction: column;
    align-items: stretch;
  }

  .link {
    width: 100%;
  }
}
</style>
