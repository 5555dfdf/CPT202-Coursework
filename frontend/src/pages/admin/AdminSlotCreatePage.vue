<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'

const router = useRouter()
const today = new Date().toISOString().slice(0, 10)

const specialists = ref([])
const specialistsLoading = ref(false)
const createLoading = ref(false)

const createForm = ref({
  specialistId: '',
  date: today,
  start: '09:00',
  end: '09:30',
  available: true,
  amount: '0.00',
  currency: 'CNY',
  type: 'online',
  detail: ''
})

const specialistMap = computed(() => {
  return new Map(
    specialists.value.map((row) => [String(row?.id ?? '').trim(), String(row?.name ?? '').trim()])
  )
})

const createDurationMinutes = computed(() =>
  calcDurationMinutes(createForm.value.start, createForm.value.end)
)

function calcDurationMinutes(startValue, endValue) {
  const start = String(startValue ?? '').trim()
  const end = String(endValue ?? '').trim()
  if (!start || !end) return 0
  const [startHour, startMin] = start.split(':').map(Number)
  const [endHour, endMin] = end.split(':').map(Number)
  if ([startHour, startMin, endHour, endMin].some(Number.isNaN)) return 0
  const startTotal = startHour * 60 + startMin
  const endTotal = endHour * 60 + endMin
  const diff = endTotal - startTotal
  return diff > 0 ? diff : 0
}

function normalizeAmountInput(value) {
  if (value === null || value === undefined || value === '') return 0
  const num = Number(value)
  return Number.isNaN(num) ? 0 : num
}

function isValidTimeRange(startValue, endValue) {
  if (!startValue || !endValue) return true
  return String(startValue) < String(endValue)
}

function formatSpecialistLabel(idValue) {
  const id = String(idValue || '').trim()
  const name = specialistMap.value.get(id) || ''
  if (name && id) return `${name} (${id})`
  return name || id || '--'
}

function buildSlotPayload(form) {
  return {
    specialistId: form.specialistId,
    date: form.date,
    start: form.start,
    end: form.end,
    available: form.available,
    amount: normalizeAmountInput(form.amount),
    currency: String(form.currency ?? '').trim() || 'CNY',
    duration: calcDurationMinutes(form.start, form.end),
    type: String(form.type ?? '').trim() || 'online',
    detail: String(form.detail ?? '').trim()
  }
}

async function loadSpecialists() {
  specialistsLoading.value = true
  try {
    const page = await api.listSpecialists({ pageSize: 100 })
    specialists.value = Array.isArray(page?.items) ? page.items : []
  } catch (e) {
    showAlertModal({ type: 'error', message: e?.message || 'Failed to load specialists' })
    specialists.value = []
  } finally {
    specialistsLoading.value = false
  }
}

function resetCreateForm() {
  createForm.value = {
    specialistId: createForm.value.specialistId,
    date: createForm.value.date || today,
    start: '09:00',
    end: '09:30',
    available: true,
    amount: '0.00',
    currency: 'CNY',
    type: 'online',
    detail: ''
  }
}

async function onCreate() {
  if (!createForm.value.specialistId) {
    showAlertModal({ type: 'error', message: 'Please select a specialist for the new slot.' })
    return
  }
  if (!createForm.value.date || !createForm.value.start || !createForm.value.end) {
    showAlertModal({ type: 'error', message: 'Please complete date, start time, and end time.' })
    return
  }
  if (!isValidTimeRange(createForm.value.start, createForm.value.end)) {
    showAlertModal({ type: 'error', message: 'Create slot start time must be earlier than end time.' })
    return
  }
  if (createDurationMinutes.value <= 0) {
    showAlertModal({ type: 'error', message: 'Please enter a valid duration in minutes.' })
    return
  }

  createLoading.value = true
  try {
    await api.adminCreateSlot(buildSlotPayload(createForm.value))
    showAlertModal({ type: 'success', message: 'Slot created successfully.' })
    await router.push({ name: 'admin.slots' })
  } catch (e) {
    showAlertModal({ type: 'error', message: e?.message || 'Failed to create slot' })
  } finally {
    createLoading.value = false
  }
}

onMounted(async () => {
  await loadSpecialists()
})
</script>

<template>
  <section class="page">
    <header class="page__header">
      <div>
        <h1>Create Slot</h1>
        <p class="subtitle">Create consultation slots on a dedicated page for a cleaner workflow.</p>
      </div>
      <button type="button" class="btn-neutral" @click="router.push({ name: 'admin.slots' })">
        Back to Slot Management
      </button>
    </header>

    <section class="calc-card">
      <label class="field">
        <span class="label">Specialist</span>
        <select v-model="createForm.specialistId" class="input input--select" :disabled="specialistsLoading || createLoading">
          <option value="">Select a specialist</option>
          <option v-for="row in specialists" :key="row.id" :value="row.id">
            {{ formatSpecialistLabel(row.id) }}
          </option>
        </select>
      </label>

      <div class="field-grid field-grid--two">
        <label class="field">
          <span class="label">Date</span>
          <input v-model="createForm.date" type="date" class="input" />
        </label>
        <div class="field">
          <span class="label">Availability</span>
          <div class="option-row">
            <button
              type="button"
              class="option-btn option-btn--type"
              :class="{ 'option-btn--active': createForm.available === true }"
              @click="createForm.available = true"
            >
              Available
            </button>
            <button
              type="button"
              class="option-btn option-btn--type"
              :class="{ 'option-btn--active': createForm.available === false }"
              @click="createForm.available = false"
            >
              Unavailable
            </button>
          </div>
        </div>
      </div>

      <div class="field-grid field-grid--two">
        <label class="field">
          <span class="label">Start Time</span>
          <input v-model="createForm.start" type="time" class="input" />
        </label>
        <label class="field">
          <span class="label">End Time</span>
          <input v-model="createForm.end" type="time" class="input" />
        </label>
      </div>

      <div class="field-grid field-grid--two">
        <label class="field">
          <span class="label">Amount</span>
          <input v-model="createForm.amount" type="number" min="0" step="0.01" class="input" />
        </label>
        <label class="field">
          <span class="label">Currency</span>
          <input v-model.trim="createForm.currency" type="text" maxlength="10" class="input" />
        </label>
      </div>

      <div class="field-grid field-grid--two">
        <label class="field">
          <span class="label">Duration (minutes, auto)</span>
          <input :value="createDurationMinutes" type="number" class="input" readonly />
        </label>
        <label class="field">
          <span class="label">Type</span>
          <input v-model.trim="createForm.type" type="text" maxlength="20" class="input" />
        </label>
      </div>

      <label class="field">
        <span class="label">Detail</span>
        <textarea v-model="createForm.detail" class="input input--textarea" rows="3" />
      </label>

      <div class="button-row">
        <button type="button" class="btn-primary btn-primary--fit" :disabled="createLoading" @click="onCreate">
          {{ createLoading ? 'Creating...' : 'Create Slot' }}
        </button>
        <button type="button" class="btn-neutral" :disabled="createLoading" @click="resetCreateForm">
          Reset
        </button>
        <div class="tip-wrap">
          <span class="icon">!</span>
          <div class="tooltip">
            Please check and follow Specialist's Pricing Rules!<br>
          </div>
        </div>
      </div>
    </section>
  </section>
</template>

<style scoped>
.tip-wrap {
  position: relative;
  display: flex;
  justify-content: center;
  margin-bottom: 8px;
  margin-top: 12px;
}

/* 感叹号 */
.icon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #faad14;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

}

/* tooltip 本体 */
.tooltip {
  position: absolute;
  bottom: 130%; /* 在上方 */
  left: 50%;
  transform: translateX(-50%);
  width: 240px;

  background: #111827;
  color: #fff;
  font-size: 12px;
  line-height: 1.4;

  padding: 8px 10px;
  border-radius: 6px;

  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
}

/* hover 显示 */
.tip-wrap:hover .tooltip {
  opacity: 1;
}

.page__header {
  margin: 8px 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
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

.calc-card {
  max-width: 980px;
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  padding: 16px;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}

.field {
  display: grid;
  gap: 8px;
  margin-bottom: 14px;
}

.field-grid {
  display: grid;
  gap: 12px;
}

.field-grid--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #f8f5f2;
  color: #111827;
}

.input--textarea {
  min-height: 88px;
  padding: 10px 12px;
  resize: vertical;
  font: inherit;
}

.input--select {
  appearance: none;
  padding-right: 36px;
  background-image:
    linear-gradient(45deg, transparent 50%, #6b7280 50%),
    linear-gradient(135deg, #6b7280 50%, transparent 50%);
  background-position:
    calc(100% - 18px) calc(50% - 3px),
    calc(100% - 12px) calc(50% - 3px);
  background-size: 6px 6px, 6px 6px;
  background-repeat: no-repeat;
  cursor: pointer;
}

.option-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-btn {
  min-width: 110px;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #f8f5f2;
  color: #374151;
  font-weight: 600;
  cursor: pointer;
}

.option-btn--active {
  border-color: #202124;
  background: #202124;
  color: #ffffff;
}

.button-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.btn-primary {
  width: 100%;
  height: 44px;
  border: 1px solid #a94442;
  border-radius: 0;
  background: #a94442;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.btn-primary--fit {
  width: 100%;
  max-width: 220px;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-neutral {
  height: 44px;
  padding: 0 14px;
  border: 1px solid #202124;
  border-radius: 0;
  background: #ffffff;
  color: #202124;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.btn-neutral:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 980px) {
  .field-grid--two {
    grid-template-columns: 1fr;
  }

  .button-row,
  .page__header {
    flex-direction: column;
    align-items: stretch;
  }

  .btn-primary--fit {
    max-width: none;
  }
}
</style>
