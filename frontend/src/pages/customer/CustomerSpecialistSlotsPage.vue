<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'

const props = defineProps({
  id: { type: String, required: true },
  bookingId: { type: String, default: '' }
})
const router = useRouter()

const isReschedule = computed(() => !!props.bookingId)

const slots = ref([])
const slotDate = ref(new Date().toISOString().slice(0, 10))
const selectedSlotId = ref('')
const loading = ref(false)
const error = ref('')
const note = ref('')
const submitting = ref(false)
const previewLoading = ref(false)
const previewError = ref('')
const weeklyPreview = ref([])
const previewOpen = ref(false)
const todayDate = new Date().toISOString().slice(0, 10)
const paymentModalOpen = ref(false)
const paymentBusy = ref(false)
const paymentError = ref('')
const paymentContext = ref({
  paymentIntentId: '',
  paymentId: '',
  specialistId: '',
  slotId: '',
  note: '',
  amount: 0,
  currency: 'CNY',
  slotLabel: '--',
  qrCodeUrl: '',
  mockMode: false
})

function formatSlotTime(value) {
  const raw = String(value ?? '').trim()
  if (!raw) return '--'

  const timeMatch = raw.match(/T?(\d{2}:\d{2})/)
  if (timeMatch) return timeMatch[1]

  return raw
}

function formatSlotRange(slot) {
  return `${formatSlotTime(slot?.start ?? slot?.startTime)} - ${formatSlotTime(slot?.end ?? slot?.endTime)}`
}

function formatMoney(slot) {
  const amount = Number(slot?.amount ?? 0)
  const safeAmount = Number.isNaN(amount) ? 0 : amount
  const currency = String(slot?.currency ?? 'CNY').trim() || 'CNY'
  return `${safeAmount.toFixed(2)} ${currency}`
}

function formatSession(slot) {
  const duration = Number(slot?.duration ?? 0)
  const safeDuration = Number.isNaN(duration) || duration <= 0 ? '--' : `${duration} min`
  const type = String(slot?.type ?? 'online').trim() || 'online'
  return `${safeDuration} / ${type}`
}

function formatDetail(slot) {
  return String(slot?.detail ?? '').trim() || '--'
}

function resolveSelectedSlot() {
  return slots.value.find((sl) => (sl.slotId ?? sl.id) === selectedSlotId.value) ?? null
}

async function openPaymentModal(paymentIntentId, selectedSlot, draftNote) {
  const safePaymentIntentId = String(paymentIntentId ?? '').trim()
  if (!safePaymentIntentId) {
    throw new Error('Missing payment intent id')
  }

  paymentBusy.value = true
  paymentError.value = ''
  const specialistId = String(props.id ?? '').trim()
  const slotId = String(selectedSlot?.slotId ?? selectedSlot?.id ?? '').trim()

  const slotAmount = Number(selectedSlot?.amount ?? 0)
  const safeSlotAmount = Number.isNaN(slotAmount) ? 0 : slotAmount
  const slotCurrency = String(selectedSlot?.currency ?? 'CNY').trim() || 'CNY'
  const slotLabel = formatSlotRange(selectedSlot)

  try {
    const payment = await api.createBookingPayment(safePaymentIntentId, {
      specialistId,
      slotId,
      amount: safeSlotAmount,
      currency: slotCurrency,
      scene: 'booking'
    })

    paymentContext.value = {
      paymentIntentId: safePaymentIntentId,
      paymentId: String(payment?.paymentId ?? '').trim(),
      specialistId,
      slotId,
      note: String(draftNote ?? '').trim(),
      amount: Number(payment?.amount ?? safeSlotAmount) || 0,
      currency: String(payment?.currency ?? slotCurrency).trim() || 'CNY',
      slotLabel,
      qrCodeUrl: String(payment?.qrCodeUrl ?? '').trim(),
      mockMode: false
    }
  } catch (e) {
    paymentContext.value = {
      paymentIntentId: safePaymentIntentId,
      paymentId: '',
      specialistId,
      slotId,
      note: String(draftNote ?? '').trim(),
      amount: safeSlotAmount,
      currency: slotCurrency,
      slotLabel,
      qrCodeUrl: '',
      mockMode: false
    }
    paymentError.value = e?.message || 'Failed to create Alipay order'
  } finally {
    paymentBusy.value = false
    paymentModalOpen.value = true
  }
}

function closePaymentModal() {
  paymentModalOpen.value = false
  paymentError.value = ''
}

async function confirmPaymentAndGoBookings() {
  if (!paymentContext.value.paymentIntentId) {
    paymentError.value = 'Missing payment intent id for payment confirmation'
    return
  }

  paymentBusy.value = true
  paymentError.value = ''
  try {
    await api.confirmBookingPayment(paymentContext.value.paymentIntentId, {
      paymentId: paymentContext.value.paymentId || undefined,
      status: 'SUCCESS'
    })
    await api.createBooking({
      specialistId: paymentContext.value.specialistId,
      slotId: paymentContext.value.slotId,
      paymentId: paymentContext.value.paymentId,
      note: paymentContext.value.note || undefined
    })
  } catch (e) {
    paymentError.value = e?.message || 'Failed to confirm payment'
    paymentBusy.value = false
    return
  }

  paymentModalOpen.value = false
  showAlertModal({
    type: 'success',
    message: 'Payment successful.',
    onClose: () =>
      router.push({
        name: 'customer.bookings',
        query: { refresh: String(Date.now()) }
      })
  })
  paymentBusy.value = false
}

async function mockPaymentAndGoBookings() {
  if (!paymentContext.value.paymentIntentId) {
    paymentError.value = 'Missing payment intent id for mock payment'
    return
  }

  paymentBusy.value = true
  paymentError.value = ''
  try {
    await api.mockBookingPayment(paymentContext.value.paymentIntentId)
    await api.createBooking({
      specialistId: paymentContext.value.specialistId,
      slotId: paymentContext.value.slotId,
      paymentId: paymentContext.value.paymentId,
      note: paymentContext.value.note || undefined
    })
  } catch (e) {
    paymentError.value = e?.message || 'Failed to mock payment'
    paymentBusy.value = false
    return
  }

  paymentModalOpen.value = false
  showAlertModal({
    type: 'success',
    message: 'Mock payment successful.',
    onClose: () =>
      router.push({
        name: 'customer.bookings',
        query: { refresh: String(Date.now()) }
      })
  })
  paymentBusy.value = false
}

function nextSevenDates() {
  const out = []
  const base = new Date()
  for (let i = 0; i < 7; i += 1) {
    const d = new Date(base)
    d.setDate(base.getDate() + i)
    out.push(d.toISOString().slice(0, 10))
  }
  return out
}

async function loadWeeklyPreview() {
  if (!props.id) return

  previewLoading.value = true
  previewError.value = ''
  try {
    const days = nextSevenDates()
    const rows = await Promise.all(
      days.map(async (date) => {
        const list = await api.listSpecialistSlots(props.id, { date })
        const available = (Array.isArray(list) ? list : []).filter((sl) => sl?.available !== false)
        return {
          date,
          slots: available
        }
      })
    )
    weeklyPreview.value = rows
  } catch (e) {
    previewError.value = e?.message || 'Failed to load weekly preview'
    weeklyPreview.value = []
  } finally {
    previewLoading.value = false
  }
}

function togglePreview() {
  previewOpen.value = !previewOpen.value
}

const todayPreviewSlots = computed(() => {
  const today = weeklyPreview.value.find((d) => d.date === todayDate)
  return today?.slots ?? []
})

const todayPreviewText = computed(() => {
  if (previewLoading.value) return 'Loading today availability...'
  if (previewError.value) return 'Failed to load today availability'
  if (!todayPreviewSlots.value.length) return 'Today: No available slots'
  return `Today: ${todayPreviewSlots.value.map((sl) => formatSlotRange(sl)).join(' / ')}`
})

async function loadSlots() {
  if (!props.id) return

  loading.value = true
  error.value = ''

  try {
    slots.value = await api.listSpecialistSlots(props.id, { date: slotDate.value })
    selectedSlotId.value = ''
  } catch (e) {
    slots.value = []
    error.value = e?.message || 'Failed to load slots'
  } finally {
    loading.value = false
  }
}

async function submitBooking() {
  if (!props.id) {
    error.value = 'Missing specialistId'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  if (!selectedSlotId.value) {
    error.value = 'Please select a slot first'
    showAlertModal({ type: 'error', message: error.value })
    return
  }

  submitting.value = true
  error.value = ''
  try {
    if (isReschedule.value) {
      await api.rescheduleBooking(props.bookingId, { slotId: selectedSlotId.value })
      selectedSlotId.value = ''
      showAlertModal({
        type: 'success',
        message: 'Rescheduled successfully.',
        onClose: () => router.push({ name: 'customer.bookingDetail', params: { id: props.bookingId } })
      })
    } else {
      const selectedSlot = resolveSelectedSlot()
      const draftNote = note.value.trim()
      const paymentIntentId = (globalThis.crypto?.randomUUID?.() ?? `pi_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`)
      selectedSlotId.value = ''
      await loadSlots()
      await openPaymentModal(paymentIntentId, selectedSlot, draftNote)
      return
    }
    await loadSlots()
  } catch (e) {
    error.value = e?.message || (isReschedule.value ? 'Failed to reschedule' : 'Failed to submit booking')
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    submitting.value = false
  }
}

watch(
    () => props.id,
    async () => {
      await Promise.all([loadSlots(), loadWeeklyPreview()])
    },
    { immediate: true }
)

watch(slotDate, () => loadSlots())

defineExpose({
  selectedSlotId,
  getSelectedSlot: () => slots.value.find((sl) => (sl.slotId ?? sl.id) === selectedSlotId.value)
})
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>{{ isReschedule ? 'Reschedule - Choose a New Slot' : 'Specialist Available Slots' }}</h1>
      <p class="subtitle">
        {{ isReschedule ? 'Choose a new available time slot to reschedule this booking.' : 'Choose a date and an available time slot to submit your booking request.' }}
      </p>
    </header>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>
    <div v-else-if="loading" class="card muted">Loading slots...</div>

    <template v-else>
      <div class="card">
        <div class="title">Available Slots</div>

        <section class="field">
          <span class="label">Availability Preview</span>
          <div class="preview-panel" :class="{ 'is-open': previewOpen }">
            <button type="button" class="preview-head" @click="togglePreview">
              <span class="preview-title">{{ todayPreviewText }}</span>
              <span class="preview-toggle">{{ previewOpen ? '^' : 'v' }}</span>
            </button>
            <div v-if="previewOpen" class="preview-body">
              <p v-if="previewError" class="banner banner--error preview-banner">{{ previewError }}</p>
              <p v-else-if="previewLoading" class="muted small preview-loading">Loading next 7 days...</p>
              <ul v-else class="preview-list">
                <li v-for="day in weeklyPreview" :key="day.date" class="preview-row">
                  <span class="preview-date">{{ day.date }}</span>
                  <div v-if="day.slots.length" class="preview-slots">
                    <span v-for="sl in day.slots" :key="sl.slotId ?? sl.id" class="preview-chip">
                      {{ formatSlotRange(sl) }}
                    </span>
                  </div>
                  <span v-else class="muted small">No slots</span>
                </li>
              </ul>
            </div>
          </div>
        </section>

        <label class="field">
          <span class="label">Date</span>
          <input v-model="slotDate" type="date" lang="en" class="input" />
        </label>

        <ul v-if="slots.length" class="slots">
          <li v-for="sl in slots" :key="sl.slotId ?? sl.id" class="slot-row">
            <label class="pick">
              <input
                  v-model="selectedSlotId"
                  type="radio"
                  name="slot"
                  :value="sl.slotId ?? sl.id"
                  :disabled="sl.available === false"
              />
              <div class="slot-main">
                <span class="slot-time">{{ formatSlotRange(sl) }}</span>
                <span class="slot-meta">{{ formatMoney(sl) }} / {{ formatSession(sl) }}</span>
                <span class="slot-meta slot-meta--detail" :title="formatDetail(sl)">Detail: {{ formatDetail(sl) }}</span>
              </div>
              <span v-if="sl.available === false" class="muted small">(Full)</span>
            </label>
          </li>
        </ul>

        <p v-else class="muted small">No available slots for this date.</p>

        <label v-if="!isReschedule" class="field field-note">
          <span class="label">Note (optional)</span>
          <textarea
            v-model="note"
            class="input input--area"
            rows="3"
            maxlength="300"
            placeholder="Tell the specialist any context for this booking."
          ></textarea>
        </label>

        <button
          type="button"
          class="btn-submit"
          :disabled="submitting || !selectedSlotId"
          @click="submitBooking"
        >
          {{ submitting ? 'Submitting...' : isReschedule ? 'Submit Reschedule' : 'Submit Booking Request' }}
        </button>
      </div>
    </template>

    <div v-if="paymentModalOpen" class="payment-modal-backdrop" @click.self="closePaymentModal">
      <section class="payment-modal-card" role="dialog" aria-modal="true" aria-label="Booking Payment">
        <header class="payment-modal-head">
          <h3 class="payment-modal-title">Complete Payment</h3>
          <button type="button" class="payment-modal-close" :disabled="paymentBusy" @click="closePaymentModal">
            ×
          </button>
        </header>

        <div class="payment-modal-body">
          <p class="payment-tip">Payment Intent ID: {{ paymentContext.paymentIntentId || '--' }}</p>
          <p class="payment-tip">Slot: {{ paymentContext.slotLabel }}</p>
          <p class="payment-amount">Amount: {{ paymentContext.amount.toFixed(2) }} {{ paymentContext.currency }}</p>

          <div class="payment-qr-wrap">
            <img v-if="paymentContext.qrCodeUrl" class="payment-qr" :src="paymentContext.qrCodeUrl" alt="Payment QR Code" />
            <div v-else class="payment-qr-empty">QR code unavailable</div>
          </div>

          <p v-if="paymentError" class="banner banner--error payment-banner">{{ paymentError }}</p>
        </div>

        <footer class="payment-modal-foot">
          <button type="button" class="btn-secondary" :disabled="paymentBusy" @click="mockPaymentAndGoBookings">
            Mock Payment
          </button>
          <button type="button" class="btn-secondary" :disabled="paymentBusy" @click="closePaymentModal">
            Pay Later
          </button>
          <button type="button" class="btn-submit" :disabled="paymentBusy" @click="confirmPaymentAndGoBookings">
            {{ paymentBusy ? 'Confirming...' : 'Paid' }}
          </button>
        </footer>
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
  color: #4b5563;
  font-size: 14px;
}

.muted {
  color: #6b7280;
}

.small {
  font-size: 12px;
}

.mono {
  font-family: ui-monospace, monospace;
  font-size: 13px;
}

.card {
  margin-top: 6px;
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}

.title {
  font-weight: 700;
  margin-bottom: 10px;
  font-size: 18px;
}

.preview-title {
  margin: 0;
  font-size: 16px;
  font-weight: 400;
  color: #757575;
  font-family: monospace;
  line-height: normal;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  text-align: left;
  flex: 1;
}

.preview-panel {
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
}

.preview-panel.is-open .preview-head {
  border-bottom: 1px solid #d8d1cb;
}

.preview-head {
  width: 100%;
  min-height: 44px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.preview-toggle {
  font-size: 14px;
  color: #4b5563;
  font-weight: 600;
  line-height: 1;
}

.preview-body {
  margin-top: 0;
  background: #f8f5f2;
}

.preview-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.preview-row {
  display: grid;
  grid-template-columns: 110px 1fr;
  gap: 10px;
  align-items: start;
  padding: 10px 12px;
  border-top: 1px solid #d8d1cb;
}

.preview-loading {
  margin: 0;
  padding: 10px 12px;
}

.preview-date {
  font-size: 12px;
  font-weight: 700;
  color: #6b7280;
}

.preview-slots {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.preview-chip {
  height: 26px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  font-size: 12px;
  color: #202124;
  font-weight: 600;
}

.preview-banner {
  margin: 0;
  border-left: 0;
  border-right: 0;
  border-top: 0;
  border-bottom: 1px solid rgba(248, 113, 113, 0.45);
}

.field {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.label {
  font-size: 13px;
  color: #4b5563;
  font-weight: 600;
}

.input {
  width: 100%;
  height: 44px;
  padding: 0 12px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #111827;
  outline: none;
}

.input--area {
  min-height: 92px;
  height: auto;
  padding: 10px 12px;
  resize: vertical;
}

.slots {
  list-style: none;
  padding: 0;
  margin: 8px 0 0;
  display: grid;
  gap: 6px;
}

.pick {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
}

.slot-main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.slot-time {
  font-weight: 700;
  color: #111827;
}

.slot-meta {
  font-size: 12px;
  color: #4b5563;
}

.slot-meta--detail {
  max-width: 560px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

.field-note {
  margin-top: 12px;
}

.btn-submit {
  margin-top: 16px;
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

.btn-submit:hover:not(:disabled) {
  opacity: 0.92;
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  height: 40px;
  padding: 0 14px;
  border-radius: 0;
  border: 1px solid #202124;
  background: #ffffff;
  color: #202124;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.payment-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 55;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(17, 24, 39, 0.42);
}

.payment-modal-card {
  width: min(100%, 460px);
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  box-shadow: 0 16px 36px rgba(17, 24, 39, 0.16);
}

.payment-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid #eceff3;
}

.payment-modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}

.payment-modal-close {
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

.payment-modal-body {
  padding: 14px 16px;
}

.payment-tip {
  margin: 0 0 4px;
  font-size: 13px;
  color: #4b5563;
}

.payment-amount {
  margin: 8px 0 12px;
  font-size: 15px;
  color: #111827;
  font-weight: 700;
}

.payment-qr-wrap {
  width: 280px;
  height: 280px;
  margin: 0 auto;
  border: 1px solid #d8d1cb;
  background: #ffffff;
  display: grid;
  place-items: center;
}

.payment-qr {
  width: 260px;
  height: 260px;
  object-fit: contain;
}

.payment-qr-empty {
  color: #6b7280;
  font-size: 13px;
}

.payment-banner {
  margin-top: 12px;
}

.payment-modal-foot {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  padding: 12px 16px 16px;
}

.payment-modal-foot .btn-submit {
  margin-top: 0;
}

@media (max-width: 720px) {
  .preview-row {
    grid-template-columns: 1fr;
  }

  .payment-qr-wrap {
    width: 240px;
    height: 240px;
  }

  .payment-qr {
    width: 220px;
    height: 220px;
  }
}
</style>




