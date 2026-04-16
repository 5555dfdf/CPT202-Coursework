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

onMounted(load)
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
.page__header h1 {
  margin: 0 0 6px;
  font-size: 22px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 12px;
  margin-top: 14px;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
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
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: #ffffff;
  color: #111827;
}
.input--sm {
  min-width: 140px;
  flex: 1;
}
.btn {
  padding: 8px 12px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
  color: inherit;
  cursor: pointer;
  white-space: nowrap;
}
.btn--ok {
  border-color: rgba(52, 211, 153, 0.45);
  background: rgba(52, 211, 153, 0.12);
}
.btn--danger {
  border-color: rgba(248, 113, 113, 0.45);
  background: rgba(248, 113, 113, 0.12);
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
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
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
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
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
  border: 1px dashed rgba(255, 255, 255, 0.16);
  border-radius: 14px;
}
.empty__title {
  font-weight: 700;
}
</style>
