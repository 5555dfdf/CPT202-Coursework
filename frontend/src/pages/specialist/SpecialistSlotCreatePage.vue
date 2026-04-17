<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'

const router = useRouter()
const route = useRoute()
const slotId = route.params.id
const isEditMode = !!slotId

const createForm = ref({
  date: new Date().toISOString().split('T')[0],
  start: '09:00',
  end: '09:30',
  available: true,
  amount: '0.00',
  currency: 'CNY',
  type: 'online',
  detail: ''
})

const loading = ref(false)
const createLoading = ref(false)
const DETAIL_MAX = 300

const createDurationMinutes = computed(() => {
  return calcDurationMinutes(createForm.value.start, createForm.value.end)
})

function calcDurationMinutes(startStr, endStr) {
  if (!startStr || !endStr) return 0
  const [startHour, startMin] = startStr.split(':').map(Number)
  const [endHour, endMin] = endStr.split(':').map(Number)
  const startTotal = startHour * 60 + startMin
  const endTotal = endHour * 60 + endMin
  return Math.max(0, endTotal - startTotal)
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

function buildSlotPayload(form) {
  return {
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

async function loadSlotDetails() {
  if (!slotId) return
  loading.value = true
  try {
    const slot = await api.specialistGetSlot(slotId)
    if (slot) {
      createForm.value = {
        date: slot.date || createForm.value.date,
        start: slot.start || createForm.value.start,
        end: slot.end || createForm.value.end,
        available: slot.available ?? true,
        amount: slot.amount?.toString() || '0.00',
        currency: slot.currency || 'CNY',
        type: slot.type || 'online',
        detail: slot.detail || ''
      }
    }
  } catch (e) {
    showAlertModal({ type: 'error', message: e?.message || 'Failed to load slot details' })
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  if (!createForm.value.date || !createForm.value.start || !createForm.value.end) {
    showAlertModal({ type: 'error', message: 'Please complete date, start time, and end time.' })
    return
  }
  if (!isValidTimeRange(createForm.value.start, createForm.value.end)) {
    showAlertModal({ type: 'error', message: 'Start time must be earlier than end time.' })
    return
  }
  if (createDurationMinutes.value <= 0) {
    showAlertModal({ type: 'error', message: 'Please enter a valid duration in minutes.' })
    return
  }

  createLoading.value = true
  try {
    if (isEditMode) {
      await api.specialistUpdateSlot(slotId, buildSlotPayload(createForm.value))
      showAlertModal({ type: 'success', message: 'Slot updated successfully.' })
    } else {
      await api.specialistCreateSlot(buildSlotPayload(createForm.value))
      showAlertModal({ type: 'success', message: 'Slot created successfully.' })
    }
    await router.push({ name: 'specialist.slots' })
  } catch (e) {
    showAlertModal({ type: 'error', message: e?.message || 'Failed to save slot' })
  } finally {
    createLoading.value = false
  }
}

function resetForm() {
  createForm.value = {
    date: new Date().toISOString().split('T')[0],
    start: '09:00',
    end: '09:30',
    available: true,
    amount: '0.00',
    currency: 'CNY',
    type: 'online',
    detail: ''
  }
}

function goBack() {
  router.push({ name: 'specialist.slots' })
}

onMounted(async () => {
  if (isEditMode) {
    await loadSlotDetails()
  }
})
</script>

<template>
  <section class="page">
    <header class="page__header">
      <div>
        <h1>{{ isEditMode ? 'Edit Slot' : 'Create Slot' }}</h1>
        <p class="subtitle">{{ isEditMode ? 'Edit an existing consultation slot.' : 'Create consultation slots for your availability.' }}</p>
      </div>
      <button type="button" class="btn-neutral" @click="goBack">
        Back to Slot Management
      </button>
    </header>

    <section class="calc-card">
      <div v-if="loading" class="loading-state">
        <p>Loading slot details...</p>
      </div>

      <div v-else class="form-container">
        <div class="field-grid field-grid--two">
          <label class="field">
            <span class="label">Date</span>
            <input 
              v-model="createForm.date" 
              type="date" 
              class="input" 
              :disabled="createLoading"
            />
          </label>

          <div class="field">
            <span class="label">Availability</span>
            <div class="option-row">
              <button
                type="button"
                class="option-btn option-btn--type"
                :class="{ 'option-btn--active': createForm.available === true }"
                @click="createForm.available = true"
                :disabled="createLoading"
              >
                Available
              </button>
              <button
                type="button"
                class="option-btn option-btn--type"
                :class="{ 'option-btn--active': createForm.available === false }"
                @click="createForm.available = false"
                :disabled="createLoading"
              >
                Unavailable
              </button>
            </div>
          </div>
        </div>

        <div class="field-grid field-grid--two">
          <label class="field">
            <span class="label">Start Time</span>
            <input 
              v-model="createForm.start" 
              type="time" 
              class="input" 
              :disabled="createLoading"
            />
          </label>

          <label class="field">
            <span class="label">End Time</span>
            <input 
              v-model="createForm.end" 
              type="time" 
              class="input" 
              :disabled="createLoading"
            />
          </label>
        </div>

        <div class="field-grid field-grid--two">
          <label class="field">
            <span class="label">Amount</span>
            <input 
              v-model="createForm.amount" 
              type="number" 
              step="0.01" 
              min="0" 
              class="input" 
              :disabled="createLoading"
            />
          </label>

          <label class="field">
            <span class="label">Currency</span>
            <select 
              v-model="createForm.currency" 
              class="input input--select" 
              :disabled="createLoading"
            >
              <option value="CNY">CNY</option>
              <option value="USD">USD</option>
              <option value="EUR">EUR</option>
            </select>
          </label>
        </div>

        <div class="field-grid field-grid--two">
          <label class="field">
            <span class="label">Duration (minutes, auto)</span>
            <input 
              :value="createDurationMinutes" 
              type="number" 
              class="input" 
              readonly 
            />
          </label>

          <label class="field">
            <span class="label">Type</span>
            <select 
              v-model="createForm.type" 
              class="input input--select" 
              :disabled="createLoading"
            >
              <option value="online">Online</option>
              <option value="offline">Offline</option>
            </select>
          </label>
        </div>

        <label class="field">
          <span class="label">Detail</span>
          <textarea 
            v-model="createForm.detail" 
            class="input input--textarea" 
            rows="3" 
            maxlength="300"
            :disabled="createLoading"
          ></textarea>
          <p class="detail-count">{{ (createForm.detail || '').length }}/{{ DETAIL_MAX }}</p>
        </label>

        <div class="form-actions">
          <button 
            type="button" 
            class="btn-secondary" 
            @click="resetForm"
            :disabled="createLoading"
          >
            Reset
          </button>
          <button 
            type="button" 
            class="btn-primary" 
            @click="onCreate"
            :disabled="createLoading"
          >
            {{ createLoading ? 'Saving...' : (isEditMode ? 'Update Slot' : 'Create Slot') }}
          </button>
        </div>
      </div>
    </section>
  </section>
</template>

<style scoped>
.page__header {
  margin: 8px 0 20px;
  padding: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
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
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  padding: 16px;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}

.loading-state {
  margin: 40px 0;
  text-align: center;
  color: #6b7280;
  font-size: 14px;
}

.form-container {
  display: grid;
  gap: 16px;
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

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
  height: 44px;
  padding: 0 12px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #111827;
}

.input--select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e");
  background-position: right 0.5rem center;
  background-repeat: no-repeat;
  background-size: 1.5em 1.5em;
  padding-right: 2.5rem;
}

.input--textarea {
  height: auto;
  min-height: 80px;
  resize: vertical;
  padding: 12px;
}

.detail-count {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
  text-align: right;
}

.option-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-btn {
  min-width: 110px;
  height: 44px;
  padding: 0 12px;
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #f8f5f2;
  color: #374151;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.option-btn--type {
  min-width: 120px;
}

.option-btn--active {
  border-color: #202124;
  background: #202124;
  color: #ffffff;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
}

.btn-primary {
  height: 44px;
  padding: 0 20px;
  border: 1px solid #a94442;
  border-radius: 0;
  background: #a94442;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.18s ease;
}

.btn-primary:hover {
  opacity: 0.92;
}

.btn-secondary {
  height: 44px;
  padding: 0 14px;
  border: 1px solid #202124;
  border-radius: 0;
  background: #ffffff;
  color: #202124;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.18s ease;
}

.btn-secondary:hover {
  opacity: 0.92;
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
  transition: opacity 0.18s ease;
}

.btn-neutral:hover {
  opacity: 0.92;
}

.btn-primary:disabled,
.btn-secondary:disabled,
.btn-neutral:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
  }

  .btn-primary,
  .btn-secondary,
  .btn-neutral {
    width: 100%;
  }
}
</style>
