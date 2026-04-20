<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { api } from '@/api/client'
import { showConfirmModal } from '@/ui/confirmModal.js'
const status = ref('Pending')
const page = ref({ items: [], total: 0 })
const loading = ref(false)
const error = ref('')
const busyId = ref('')
const rejectReason = ref('')

/**
 * Fetch data from the API based on current filter status
 */
async function load() {
  error.value = ''
  loading.value = true
  try {
    const params = { pageSize: 50 }
    if (status.value) params.status = status.value
    page.value = await api.listBookingRequests(params)
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    page.value = { items: [], total: 0 }
  } finally {
    loading.value = false
  }
}
// initial load on component mount
onMounted(load)

// automatically re-fetch data whenever the status filter changes
watch(status, () => load())

function onConfirm(id) {
  showConfirmModal({
    title: 'Confirm acceptance',
    message: 'Are you sure you want to accept this reservation?',
    onConfirm: async () => {
      busyId.value = id
      try {
        await api.confirmBooking(id)
        await load()
      } catch (e) {
        error.value = e?.message || 'Failed to confirm'
      } finally {
        busyId.value = ''
      }
    }
  })
}
// handles the rejection of a booking request
function onReject(id) {
  showConfirmModal({
    title: 'Refuse Reservation',
    message: 'Are you sure you want to decline this reservation',
    onConfirm: async () => {
      busyId.value = id
      try {
        await api.rejectBooking(id, { reason: rejectReason.value.trim() || undefined })
        rejectReason.value = ''
        await load()
      } catch (e) {
        error.value = e?.message || 'Failed to reject'
      } finally {
        busyId.value = ''
      }
    }
  })
}
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Booking Requests</h1>
    </header>

    <div class="toolbar card">
      <label class="field">
        <span class="label">Status</span>
        <select v-model="status" class="input">
          <option value="">All</option>
          <option value="Pending">Pending</option>
          <option value="Confirmed">Confirmed</option>
        </select>
      </label>
      <button type="button" class="btn" :disabled="loading" @click="load">Refresh</button>
    </div>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>

    <div v-if="loading && !(page.items || []).length" class="card muted">Loading…</div>

    <div v-else-if="!(page.items || []).length" class="empty">
      <div class="empty__title">No records</div>
    </div>

    <ul v-else class="list">
      <li v-for="b in page.items" :key="b.id" class="card item">
        <div class="item__main">
          <RouterLink class="mono link" :to="{ name: 'specialist.bookingDetail', params: { id: b.id } }">
            {{ b.id }}
          </RouterLink>
          <div class="muted small">{{ b.customerName ?? b.customerId ?? '—' }}</div>
          <div class="small">{{ b.time ?? b.startTime }}</div>
          <div class="badge">{{ b.status }}</div>
        </div>
        <div class="actions">
          <input
              v-model="rejectReason"
              class="input input--sm"
              placeholder="Rejection reason (optional)"
          />
          <button
              type="button"
              class="btn btn--ok"
              :disabled="busyId === b.id"
              @click="onConfirm(b.id)"
          >
            Confirm
          </button>
          <button
              type="button"
              class="btn btn--danger"
              :disabled="busyId === b.id"
              @click="onReject(b.id)"
          >
            Reject
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
/* Page & Header Typography */
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
/* Filter Toolbar Layout */
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
/* Form Fields & Inputs */
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
.input--sm {
  min-width: 140px;
  flex: 1;
}
/* Button Components */
.btn {
  height: 40px;
  padding: 0 14px;
  border-radius: 0;
  border: 1px solid #a94442;
  background: #a94442;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}
.btn--ok {
  border-color: #202124;
  background: #ffffff;
  color: #202124;
}
.btn--danger {
  border-color: #a94442;
  background: #ffffff;
  color: #a94442;
}
.btn:disabled {
  opacity: 0.55;
}
/* Utility Classes */
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
.list {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 12px;
}
.item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.item__main {
  display: grid;
  gap: 4px;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.mono {
  font-family: ui-monospace, monospace;
  font-size: 13px;
}
.link {
  color: inherit;
  font-weight: 600;
}
.badge {
  display: inline-block;
  margin-top: 6px;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
}
.banner {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 0;
  font-size: 13px;
}
/* Error Banner & Empty State Styling */
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
}
</style>
