<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'

const props = defineProps({
  id: { type: String, required: true },
  embedded: { type: Boolean, default: false }
})
const emit = defineEmits(['close'])

const router = useRouter()
const booking = ref(null)
const loading = ref(false)
const error = ref('')
const cancelReason = ref('')
const busy = ref('')
const actionError = ref('')
const reasonFocused = ref(false)
const reasonLimitError = ref('')

const REASON_MAX = 300
const showReasonHelper = computed(() => reasonFocused.value || !!reasonLimitError.value)

function formatMoney(amount, currency) {
  const n = Number(amount ?? 0)
  const safe = Number.isNaN(n) ? 0 : n
  const c = String(currency ?? 'CNY').trim() || 'CNY'
  return `${safe.toFixed(2)} ${c}`
}

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

watch(cancelReason, (val) => {
  if (val.length > REASON_MAX) {
    cancelReason.value = val.slice(0, REASON_MAX)
    reasonLimitError.value = `Maximum ${REASON_MAX} characters allowed.`
    return
  }
  if (val.length < REASON_MAX) {
    reasonLimitError.value = ''
  }
})

async function onCancel() {
  actionError.value = ''
  busy.value = 'cancel'
  try {
    booking.value = await api.cancelBooking(props.id, {
      reason: cancelReason.value.trim() || undefined
    })
    if (props.embedded) {
      showAlertModal({
        type: 'success',
        message: 'Booking cancelled successfully.',
        onClose: () => emit('close', { refresh: true })
      })
    } else {
      showAlertModal({
        type: 'success',
        message: 'Booking cancelled successfully.',
        onClose: () =>
          router.push({
            name: 'customer.bookings',
            query: { refresh: String(Date.now()) }
          })
      })
    }
  } catch (e) {
    actionError.value = e?.message || 'Failed to cancel'
  } finally {
    busy.value = ''
  }
}

function goReschedule() {
  const specialistId = booking.value?.specialistId
  if (!specialistId) {
    actionError.value = 'Missing specialistId for reschedule'
    return
  }
  router.push({
    name: 'customer.specialistSlots',
    params: { id: specialistId },
    query: { bookingId: props.id }
  })
}

function onReasonFocus() {
  reasonFocused.value = true
}

function onReasonBlur() {
  reasonFocused.value = false
  if (cancelReason.value.length <= REASON_MAX) {
    reasonLimitError.value = ''
  }
}
</script>

<template>
  <section class="page" :class="{ 'page--embedded': embedded }">
    <header v-if="!embedded" class="page__header">
      <h1>Booking Details</h1>
    </header>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>
    <div v-else-if="loading" class="card muted">Loading…</div>

    <template v-else-if="booking">
      <div :class="embedded ? 'embedded-grid' : ''">
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
            <dt>Price</dt>
            <dd>{{ formatMoney(booking.amount, booking.currency) }}</dd>
            <dt>Type</dt>
            <dd>{{ booking.type ?? '—' }}</dd>
            <dt>Detail</dt>
            <dd class="detail-text" :title="booking.detail ?? '—'">{{ booking.detail ?? '—' }}</dd>
            <dt>Note</dt>
            <dd>{{ booking.note ?? '—' }}</dd>
          </dl>
        </div>

        <div :class="embedded ? 'embedded-side' : ''">
          <div class="card card--cancel">
            <div class="title">Cancel Booking</div>
            <label class="field">
              <span class="label">Reason (optional)</span>
              <input
                v-model="cancelReason"
                class="input"
                placeholder="Reason"
                maxlength="300"
                @focus="onReasonFocus"
                @blur="onReasonBlur"
              />
            </label>
            <div
              v-if="showReasonHelper"
              class="limit-helper"
              :class="{ 'limit-helper--error': reasonLimitError }"
              role="status"
              aria-live="polite"
            >
              <p class="limit-helper__text">{{ reasonLimitError || `Maximum ${REASON_MAX} characters` }}</p>
              <p class="limit-helper__count">{{ cancelReason.length }}/{{ REASON_MAX }}</p>
            </div>
            <div class="cancel-action">
              <button
                type="button"
                class="btn btn--danger"
                :disabled="busy === 'cancel'"
                @click="onCancel"
              >
                {{ busy === 'cancel' ? 'Processing…' : 'Cancel Booking' }}
              </button>
            </div>
          </div>

          <div class="card card--reschedule">
            <div class="title">Reschedule</div>
            <div class="reschedule-action">
              <button
                type="button"
                class="btn"
                @click="goReschedule"
              >
                Choose a new time slot
              </button>
            </div>
          </div>

          <p v-if="actionError" class="banner banner--error">{{ actionError }}</p>
        </div>
      </div>

      <p v-if="!embedded" class="muted small">
        <button type="button" class="linkish btn-neutral" @click="router.push({ name: 'customer.bookings' })">
          Back to My Bookings
        </button>
      </p>
    </template>
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
.page--embedded .card:first-of-type {
  margin-top: 0;
}
.embedded-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 460px;
  gap: 14px;
  align-items: stretch;
}
.embedded-side {
  display: flex;
  flex-direction: column;
  align-self: stretch;
}
.embedded-side .card {
  margin-top: 0;
}
.embedded-side .card + .card {
  margin-top: 14px;
}
.muted {
  color: #6b7280;
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

.detail-text {
  max-width: 520px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.field {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
}
.limit-helper {
  border: 1px solid #ddd3cb;
  background: #f8f5f2;
  padding: 8px 10px;
  margin-bottom: 10px;
}
.limit-helper--error {
  border-color: rgba(180, 35, 24, 0.35);
  background: rgba(180, 35, 24, 0.08);
}
.limit-helper__text {
  margin: 0;
  font-size: 12px;
  color: #4b5563;
}
.limit-helper__count {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
}
.label {
  font-size: 13px;
  opacity: 0.85;
}
.input {
  width: 100%;
  max-width: 400px;
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
  border: 1px solid #D9533C;
  background: #D9533C;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
}
.btn--danger {
  border-color: #D9533C;
  background: #ffffff;
  color: #D9533C;
}
.btn:disabled {
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
.linkish {
  padding: 0 14px;
  height: 40px;
  text-decoration: none;
  cursor: pointer;
  font: inherit;
}

.btn-neutral {
  border: 1px solid #202124;
  border-radius: 0;
  background: #ffffff;
  color: #202124;
  font-weight: 700;
}

.card--cancel .input {
  max-width: none;
}

.card--cancel .btn--danger {
  min-width: 220px;
}

.cancel-action {
  display: flex;
  justify-content: flex-end;
}

.card--reschedule .btn {
  min-width: 220px;
}

.card--reschedule {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.reschedule-action {
  display: flex;
  justify-content: flex-end;
  margin-top: auto;
}

@media (max-width: 1080px) {
  .embedded-grid {
    grid-template-columns: 1fr;
  }

  .reschedule-action {
    justify-content: flex-start;
  }

  .cancel-action {
    justify-content: flex-start;
  }
}
</style>

