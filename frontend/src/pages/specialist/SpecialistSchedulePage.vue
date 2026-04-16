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
      <label class="field">
        <span class="label">Specialist ID</span>
        <input v-model="specialistId" class="input" placeholder="sp-1" />
      </label>
      <label class="field">
        <span class="label">Date</span>
        <input v-model="slotDate" type="date" class="input" />
      </label>
      <button type="button" class="btn" :disabled="loading" @click="loadSlots">Refresh</button>
    </div>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>

    <div class="card">
      <div class="title">Available Slots</div>
      <p v-if="loading" class="muted">Loading…</p>

      <ul v-else-if="slots.length" class="slots">
        <li v-for="sl in slots" :key="sl.slotId ?? sl.id" class="slot">
          <div class="slot__time">
            <span>{{ sl.start ?? sl.startTime }}</span>
            <span>—</span>
            <span>{{ sl.end ?? sl.endTime }}</span>
          </div>

          <div class="slot__info">
            <span v-if="sl.bookingId && sl.customerName" class="customer-name">{{ sl.customerName }}</span>
            <span v-if="sl.status" class="badge" :class="getStatusClass(sl.status)">{{ sl.status }}</span>
            <span v-else-if="sl.available === false" class="muted small">Full</span>
            <span v-else class="muted small">Available</span>
          </div>

          <button
              v-if="sl.bookingId && sl.status === 'Confirmed'"
              type="button"
              class="btn-complete"
              :disabled="busySlotId === (sl.slotId ?? sl.id)"
              @click="handleComplete(sl.slotId ?? sl.id, sl.bookingId)"
          >
            {{ busySlotId === (sl.slotId ?? sl.id) ? '...' : 'Complete' }}
          </button>
        </li>
      </ul>

      <p v-else-if="!loading && specialistId.trim()" class="muted small">No slots found for this date.</p>
      <p v-else-if="!loading" class="muted small">No data. Enter a specialist ID and select a date.</p>
    </div>
  </section>
</template>

<style scoped>
.page__header h1 {
  margin: 0 0 6px;
  font-size: 22px;
}
.muted {
  opacity: 0.8;
}
.small {
  font-size: 12px;
}
.card {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
}
.title {
  font-weight: 700;
  margin-bottom: 10px;
}
.field {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
  max-width: 420px;
}
.label {
  font-size: 13px;
  opacity: 0.85;
}
.input {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: #ffffff;
  color: #111827;
}
.btn {
  padding: 10px 16px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.1);
  color: inherit;
  cursor: pointer;
}
.btn:disabled {
  opacity: 0.5;
}
.slots {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 6px;
}
.slot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  font-size: 14px;
}
.slot__time {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 180px;
  font-weight: 500;
}
.slot__info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.customer-name {
  font-weight: 600;
  color: #60a5fa;
  font-size: 13px;
}
.badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
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
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  background: #10b981;
  color: white;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-complete:hover:not(:disabled) {
  background: #059669;
  transform: translateY(-1px);
}

.btn-complete:active:not(:disabled) {
  transform: translateY(0);
}

.btn-complete:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
