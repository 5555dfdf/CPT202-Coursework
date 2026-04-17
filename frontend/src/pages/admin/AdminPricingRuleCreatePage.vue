<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'

const router = useRouter()

const specialists = ref([])
const specialistsLoading = ref(false)
const createLoading = ref(false)

const createForm = ref({
  specialistId: '',
  duration: 60,
  type: 'online',
  amount: '0.00',
  currency: 'CNY',
  detail: ''
})

const specialistMap = computed(() => {
  return new Map(
    specialists.value.map((row) => [String(row?.id ?? '').trim(), String(row?.name ?? '').trim()])
  )
})

function formatSpecialistLabel(idValue) {
  const id = String(idValue || '').trim()
  const name = specialistMap.value.get(id) || ''
  if (name && id) return `${name} (${id})`
  return name || id || '--'
}

function normalizeAmountInput(value) {
  if (value === null || value === undefined || value === '') return 0
  const num = Number(value)
  return Number.isNaN(num) ? 0 : num
}

function buildRulePayload(form) {
  return {
    specialistId: String(form.specialistId ?? '').trim(),
    duration: Number(form.duration),
    type: String(form.type ?? '').trim().toLowerCase(),
    amount: normalizeAmountInput(form.amount),
    currency: String(form.currency ?? '').trim().toUpperCase() || 'CNY',
    detail: String(form.detail ?? '').trim()
  }
}

async function loadSpecialists() {
  specialistsLoading.value = true
  try {
    const page = await api.listSpecialists({ pageSize: 100 })
    specialists.value = Array.isArray(page?.items) ? page.items : []
  } catch (e) {
    specialists.value = []
    showAlertModal({ type: 'error', message: e?.message || 'Failed to load specialists' })
  } finally {
    specialistsLoading.value = false
  }
}

function resetCreateForm() {
  createForm.value = {
    specialistId: createForm.value.specialistId,
    duration: 60,
    type: 'online',
    amount: '0.00',
    currency: 'CNY',
    detail: ''
  }
}

async function onCreate() {
  if (!createForm.value.specialistId) {
    showAlertModal({ type: 'error', message: 'Please select a specialist for the new pricing rule.' })
    return
  }
  if (!Number.isFinite(Number(createForm.value.duration)) || Number(createForm.value.duration) <= 0) {
    showAlertModal({ type: 'error', message: 'Please enter a valid duration in minutes.' })
    return
  }
  if (!String(createForm.value.type || '').trim()) {
    showAlertModal({ type: 'error', message: 'Please enter a session type.' })
    return
  }

  createLoading.value = true
  try {
    await api.adminCreatePricingRule(buildRulePayload(createForm.value))
    showAlertModal({ type: 'success', message: 'Pricing rule created successfully.' })
    await router.push({ name: 'admin.pricing' })
  } catch (e) {
    showAlertModal({ type: 'error', message: e?.message || 'Failed to create pricing rule' })
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
        <h1>Add New Price Rule</h1>
        <p class="subtitle">Create a dedicated pricing rule for a specialist, duration, and session type.</p>
      </div>
      <button type="button" class="btn-neutral" @click="router.push({ name: 'admin.pricing' })">
        Back to Pricing
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
          <span class="label">Duration (minutes)</span>
          <input v-model="createForm.duration" type="number" min="1" step="1" class="input" />
        </label>
        <label class="field">
          <span class="label">Session Type</span>
          <select v-model="createForm.type" class="input input--select">
            <option value="online">online</option>
            <option value="offline">offline</option>
          </select>
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

      <label class="field">
        <span class="label">Detail</span>
        <textarea v-model="createForm.detail" class="input input--textarea" rows="3" />
      </label>

      <div class="button-row">
        <button type="button" class="btn-primary btn-primary--fit" :disabled="createLoading" @click="onCreate">
          {{ createLoading ? 'Creating...' : 'Create Pricing Rule' }}
        </button>
        <button type="button" class="btn-neutral" :disabled="createLoading" @click="resetCreateForm">
          Reset
        </button>
      </div>
    </section>
  </section>
</template>

<style scoped>
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

.btn-primary:disabled,
.btn-neutral:disabled {
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
