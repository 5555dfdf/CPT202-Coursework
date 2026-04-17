<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'
import { showConfirmModal } from '@/ui/confirmModal.js'
const auth = useAuthStore()
const specialistId = ref('')
const slotDate = ref(new Date().toISOString().slice(0, 10))
const slots = ref([])
const loading = ref(false)
const error = ref('')
const busySlotId = ref('')

const hintId = computed(() => auth.user?.specialistId ?? auth.user?.id ?? '')

watch(
    hintId,
    (v) => {
      if (v && !specialistId.value) {
        specialistId.value = String(v)
      }
    },
    { immediate: true }
)

async function loadSlots() {
  if (!specialistId.value.trim()) {
    slots.value = []
    return
  }
  error.value = ''
  loading.value = true
  try {
    slots.value = await api.listSpecialistSlots(specialistId.value.trim(), { date: slotDate.value })
  } catch (e) {
    error.value = e?.message || 'Failed to load slots'
    slots.value = []
  } finally {
    loading.value = false
  }
}

async function handleComplete(slotId, bookingId) {
  if (!bookingId) {
    error.value = 'No booking found for this slot'
    return
  }

  showConfirmModal({
    title: 'Complete Reservation',
    message: 'Are you sure that this reservation service has been completed?  \n' +
        'Once confirmed, the status will change to \'Completed\'.',
    onConfirm: async () => {
      busySlotId.value = slotId
      try {
        await api.completeBooking(bookingId)

        showAlertModal({
          title: 'Success',
          message: 'Booking completed successfully.',
          type: 'success'
        })

        await loadSlots()
      } catch (e) {
        error.value = e?.message || 'Failed to complete booking'
        showAlertModal({
          title: 'Error',
          message: error.value,
          type: 'error'
        })
      } finally {
        busySlotId.value = ''
      }
    }
  })
}

function getStatusClass(status) {
  if (!status) return ''
  const lowerStatus = status.toLowerCase()
  return `badge--${lowerStatus}`
}

watch([specialistId, slotDate], () => {
  if (specialistId.value.trim()) {
    loadSlots()
  }
})

onMounted(() => {
  if (specialistId.value.trim()) {
    loadSlots()
  }
})
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>My Schedule</h1>
    </header>

    <div class="card">
      <div class="title">Search Filters</div>
      <div class="filters-grid">
        <label class="field field--specialist">
          <span class="label">Specialist ID</span>
          <input v-model="specialistId" class="input" placeholder="sp-1" />
        </label>
        <label class="field field--date">
          <span class="label">Date</span>
          <input v-model="slotDate" type="date" class="input" />
        </label>
        <div class="field field--refresh">
          <span class="label label--ghost">Action</span>
          <button type="button" class="btn" :disabled="loading" @click="loadSlots">Refresh</button>
        </div>
      </div>
    </div>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>

    <div class="card">
      <div class="title">Available Slots</div>
      <p v-if="loading" class="muted">Loading…</p>

      <div v-else-if="slots.length" class="slots-table-wrap">
        <table class="slots-table">
          <thead>
            <tr>
              <th>Time</th>
              <th>Customer</th>
              <th>Status</th>
              <th class="th-action">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="sl in slots" :key="sl.slotId ?? sl.id">
              <td>
                <span>{{ sl.start ?? sl.startTime }}</span>
                <span> — </span>
                <span>{{ sl.end ?? sl.endTime }}</span>
              </td>
              <td>{{ sl.bookingId && sl.customerName ? sl.customerName : '—' }}</td>
              <td>
                <span v-if="sl.status" class="badge" :class="getStatusClass(sl.status)">{{ sl.status }}</span>
                <span v-else-if="sl.available === false" class="muted small">Full</span>
                <span v-else class="muted small">Available</span>
              </td>
              <td class="cell-action">
                <button
                  v-if="sl.bookingId && sl.status === 'Confirmed'"
                  type="button"
                  class="btn-complete"
                  :disabled="busySlotId === (sl.slotId ?? sl.id)"
                  @click="handleComplete(sl.slotId ?? sl.id, sl.bookingId)"
                >
                  {{ busySlotId === (sl.slotId ?? sl.id) ? '...' : 'Complete' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-else-if="!loading && specialistId.trim()" class="muted small">No slots found for this date.</p>
      <p v-else-if="!loading" class="muted small">No data. Enter a specialist ID and select a date.</p>
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
.muted {
  color: #6b7280;
}
.small {
  font-size: 12px;
}
.card {
  margin-top: 14px;
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}
.title {
  font-weight: 700;
  margin-bottom: 10px;
  font-size: 16px;
}
.field {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
  max-width: 420px;
}
.filters-grid {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(260px, 1fr) 140px;
  grid-template-areas: "specialist date refresh";
  gap: 10px 16px;
  align-items: end;
}
.field--specialist {
  grid-area: specialist;
  margin-bottom: 0;
  max-width: none;
}
.field--date {
  grid-area: date;
  margin-bottom: 0;
  max-width: none;
}
.field--refresh {
  grid-area: refresh;
  margin-bottom: 0;
  max-width: none;
  justify-self: end;
  width: 100%;
}
.label--ghost {
  visibility: hidden;
}
.label {
  font-size: 13px;
  color: #4b5563;
  font-weight: 600;
}
.input {
  height: 44px;
  padding: 0 12px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #111827;
}
.btn {
  height: 44px;
  padding: 0 16px;
  border-radius: 0;
  border: 1px solid #a94442;
  background: #a94442;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
}
.btn:disabled {
  opacity: 0.6;
}
.slots-table-wrap {
  overflow-x: auto;
  border: 1px solid #eceff3;
  background: #ffffff;
}
.slots-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 760px;
}
.slots-table th,
.slots-table td {
  border-bottom: 1px solid #eceff3;
  padding: 12px 14px;
  text-align: center;
  font-size: 14px;
}
.slots-table th {
  font-size: 12px;
  color: #111827;
  font-weight: 700;
  text-transform: uppercase;
  background: #fafafa;
}
.slots-table tbody tr:last-child td {
  border-bottom: 0;
}
.th-action,
.cell-action {
  width: 130px;
  text-align: center;
}
.badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
}
.badge--confirmed {
  background: rgba(52, 211, 153, 0.15);
  color: #34d399;
}
.badge--pending {
  background: rgba(251, 191, 36, 0.15);
  color: #fbbf24;
}
.badge--completed {
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
}
.badge--cancelled {
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
}
.badge--rejected {
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
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

.btn-complete {
  margin-left: auto;
  height: 34px;
  padding: 0 14px;
  border-radius: 0;
  font-size: 13px;
  font-weight: 700;
  background: #a94442;
  color: #ffffff;
  border: 1px solid #a94442;
  cursor: pointer;
  transition: opacity 0.18s ease;
}

.btn-complete:hover:not(:disabled) {
  opacity: 0.92;
}

.btn-complete:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 980px) {
  .filters-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      "specialist"
      "date"
      "refresh";
  }

  .field--refresh {
    justify-self: start;
  }
}
</style>
