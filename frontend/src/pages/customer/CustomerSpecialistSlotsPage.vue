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
  return `${safeDuration} 路 ${type}`
}

function formatDetail(slot) {
  return String(slot?.detail ?? '').trim() || '--'
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
  return `Today: ${todayPreviewSlots.value.map((sl) => formatSlotRange(sl)).join(' 路 ')}`
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
      await api.createBooking({
        specialistId: props.id,
        slotId: selectedSlotId.value,
        note: note.value.trim() || undefined
      })
      note.value = ''
      selectedSlotId.value = ''
      showAlertModal({
        type: 'success',
        message: 'Booking request submitted successfully.',
        onClose: () => router.push({ name: 'customer.bookings' })
      })
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
                <span class="slot-meta">{{ formatMoney(sl) }} 路 {{ formatSession(sl) }}</span>
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
  margin-top: 6px;
  width: 100%;
  max-width: 260px;
  height: 44px;
  border: 1px solid #D9533C;
  border-radius: 0;
  background: #D9533C;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.btn-submit:hover:not(:disabled) {
  opacity: 0.92;
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .preview-row {
    grid-template-columns: 1fr;
  }
}
</style>




