<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'

const props = defineProps({
  id: { type: String, required: true }
})

const router = useRouter()
const booking = ref(null)
const loading = ref(false)
const error = ref('')
const cancelReason = ref('')
const newSlotId = ref('')
const busy = ref('')
const actionError = ref('')

async function load() {
  error.value = ''
  loading.value = true
  booking.value = null
  try {
    booking.value = await api.getBooking(props.id)
  } catch (e) {
    error.value = e?.message || 'Failed to load'
  } finally {
    loading.value = false
  }
}

watch(
  () => props.id,
  () => load(),
  { immediate: true }
)

async function onCancel() {
  actionError.value = ''
  busy.value = 'cancel'
  try {
    booking.value = await api.cancelBooking(props.id, {
      reason: cancelReason.value.trim() || undefined
    })
    window.alert('Booking cancelled successfully.')
    await router.push({ name: 'customer.bookings' })
  } catch (e) {
    actionError.value = e?.message || 'Failed to cancel'
  } finally {
    busy.value = ''
  }
}

async function onReschedule() {
  actionError.value = ''
  if (!newSlotId.value.trim()) {
    actionError.value = 'Please enter a new slotId'
    return
  }
  busy.value = 'reschedule'
  try {
    booking.value = await api.rescheduleBooking(props.id, { slotId: newSlotId.value.trim() })
  } catch (e) {
    actionError.value = e?.message || 'Failed to reschedule'
  } finally {
    busy.value = ''
  }
}
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Booking Details</h1>
    </header>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>
    <div v-else-if="loading" class="card muted">Loading…</div>

    <template v-else-if="booking">
      <div class="card">
        <div class="title">Booking Info</div>
        <dl class="kv">
          <dt>Status</dt>
          <dd>{{ booking.status ?? '—' }}</dd>
          <dt>Time</dt>
          <dd>{{ booking.time ?? booking.startTime ?? '—' }}</dd>
          <dt>Specialist</dt>
          <dd>{{ booking.specialistName ?? booking.specialistId ?? '—' }}</dd>
          <dt>Duration</dt>
          <dd>{{ booking.duration ?? booking.slot ?? booking.slotId ?? '—' }}</dd>
          <dt>Note</dt>
          <dd>{{ booking.note ?? '—' }}</dd>
        </dl>
      </div>

      <div class="card">
        <div class="title">Cancel Booking</div>
        <label class="field">
          <span class="label">Reason (optional)</span>
          <input v-model="cancelReason" class="input" placeholder="Reason" />
        </label>
        <button
          type="button"
          class="btn btn--danger"
          :disabled="busy === 'cancel'"
          @click="onCancel"
        >
          {{ busy === 'cancel' ? 'Processing…' : 'Cancel Booking' }}
        </button>
      </div>

      <div class="card">
        <div class="title">Reschedule</div>
        <label class="field">
          <span class="label">New slotId</span>
          <input v-model="newSlotId" class="input" placeholder="Get from specialist slots" />
        </label>
        <button
          type="button"
          class="btn"
          :disabled="busy === 'reschedule'"
          @click="onReschedule"
        >
          {{ busy === 'reschedule' ? 'Processing…' : 'Submit Reschedule' }}
        </button>
      </div>

      <p v-if="actionError" class="banner banner--error">{{ actionError }}</p>

      <p class="muted small">
        <button type="button" class="linkish" @click="router.push({ name: 'customer.bookings' })">
          Back to My Bookings
        </button>
      </p>
    </template>
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
  margin-top: 12px;
}
.mono {
  font-family: ui-monospace, monospace;
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
.kv {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 8px 12px;
  margin: 0;
  font-size: 14px;
}
.kv dt {
  opacity: 0.75;
  margin: 0;
}
.kv dd {
  margin: 0;
}
.field {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
}
.label {
  font-size: 13px;
  opacity: 0.85;
}
.input {
  width: 100%;
  max-width: 400px;
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
.btn--danger {
  border-color: rgba(248, 113, 113, 0.5);
  background: rgba(248, 113, 113, 0.15);
}
.btn:disabled {
  opacity: 0.5;
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
.linkish {
  background: none;
  border: none;
  padding: 0;
  color: inherit;
  text-decoration: underline;
  cursor: pointer;
  font: inherit;
}
</style>
