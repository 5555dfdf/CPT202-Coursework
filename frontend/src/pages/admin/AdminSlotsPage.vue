<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'

const router = useRouter()
const today = new Date().toISOString().slice(0, 10)

const specialists = ref([])
const specialistsLoading = ref(false)
const specialistLoadError = ref('')

const slots = ref([])
const searchedOnce = ref(false)
const slotSearchQuery = ref('')

const searchLoading = ref(false)
const updateLoading = ref(false)
const deletingId = ref('')

const success = ref('')
const error = ref('')

const searchForm = ref({
  specialistId: '',
  date: today,
  from: '',
  to: '',
  available: ''
})

const editOpen = ref(false)
const editForm = ref({
  id: '',
  specialistId: '',
  date: '',
  start: '',
  end: '',
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

const slotCountLabel = computed(() => {
  const count = slots.value.length
  return `${count} slot${count === 1 ? '' : 's'}`
})

const filteredSlots = computed(() => {
  const query = slotSearchQuery.value.trim().toLowerCase()
  if (!query) return slots.value

  return slots.value.filter((row) => {
    const haystack = [
      slotId(row),
      formatSpecialistLabel(slotSpecialistId(row)),
      slotDate(row),
      slotStart(row),
      slotEnd(row),
      formatAvailabilityLabel(slotAvailable(row))
    ]
      .join(' ')
      .toLowerCase()

    return haystack.includes(query)
  })
})

function clearMessages() {
  success.value = ''
  error.value = ''
}

function formatSpecialistLabel(idValue) {
  const id = String(idValue || '').trim()
  const name = specialistMap.value.get(id) || ''
  if (name && id) return `${name} (${id})`
  return name || id || '--'
}

function slotId(row) {
  return String(row?.id ?? row?.slotId ?? '').trim()
}

function slotSpecialistId(row) {
  return String(row?.specialistId ?? '').trim()
}

function slotDate(row) {
  return String(row?.date ?? '').trim() || '--'
}

function slotStart(row) {
  return String(row?.start ?? row?.startTime ?? '').trim() || '--'
}

function slotEnd(row) {
  return String(row?.end ?? row?.endTime ?? '').trim() || '--'
}

function slotAvailable(row) {
  return row?.available !== false
}

function formatAvailabilityLabel(value) {
  return value ? 'Available' : 'Unavailable'
}

function slotAmount(row) {
  const amount = row?.amount
  if (amount === null || amount === undefined || amount === '') return '0.00'
  const num = Number(amount)
  if (Number.isNaN(num)) return String(amount)
  return num.toFixed(2)
}

function slotCurrency(row) {
  return String(row?.currency ?? 'CNY').trim() || 'CNY'
}

function slotDuration(row) {
  const duration = Number(row?.duration)
  if (Number.isNaN(duration) || duration <= 0) return '--'
  return `${duration} min`
}

function slotType(row) {
  return String(row?.type ?? 'online').trim() || 'online'
}

function slotDetail(row) {
  return String(row?.detail ?? '').trim() || '--'
}

function slotSchedule(row) {
  const date = slotDate(row)
  const start = slotStart(row)
  const end = slotEnd(row)
  if (date === '--') return '--'
  return `${date} ${start}-${end}`
}

function slotSession(row) {
  const duration = slotDuration(row)
  const type = slotType(row)
  if (duration === '--') return type
  return `${duration} · ${type}`
}

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

const editDurationMinutes = computed(() =>
  calcDurationMinutes(editForm.value.start, editForm.value.end)
)

function normalizeAmountInput(value) {
  if (value === null || value === undefined || value === '') return 0
  const num = Number(value)
  return Number.isNaN(num) ? 0 : num
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

function sortSlots(rows) {
  return [...rows].sort((a, b) => {
    const specialistDiff = slotSpecialistId(a).localeCompare(slotSpecialistId(b))
    if (specialistDiff !== 0) return specialistDiff

    const dateDiff = slotDate(a).localeCompare(slotDate(b))
    if (dateDiff !== 0) return dateDiff

    const startDiff = slotStart(a).localeCompare(slotStart(b))
    if (startDiff !== 0) return startDiff

    return slotEnd(a).localeCompare(slotEnd(b))
  })
}

function isValidTimeRange(startValue, endValue) {
  if (!startValue || !endValue) return true
  return String(startValue) < String(endValue)
}

async function loadSpecialists() {
  specialistsLoading.value = true
  specialistLoadError.value = ''
  try {
    const page = await api.listSpecialists({ pageSize: 100 })
    specialists.value = Array.isArray(page?.items) ? page.items : []
  } catch (e) {
    specialistLoadError.value = e?.message || 'Failed to load specialists'
    specialists.value = []
    showAlertModal({ type: 'error', message: specialistLoadError.value })
  } finally {
    specialistsLoading.value = false
  }
}

function buildSearchParams() {
  const params = {}
  if (searchForm.value.specialistId) params.specialistId = searchForm.value.specialistId
  if (searchForm.value.date) params.date = searchForm.value.date
  if (searchForm.value.from) params.from = searchForm.value.from
  if (searchForm.value.to) params.to = searchForm.value.to
  if (searchForm.value.available !== '') params.available = searchForm.value.available === 'true'
  return params
}

async function loadSlots(options = {}) {
  const { announceSuccess = false, preserveMessages = false } = options

  if (!preserveMessages) clearMessages()

  if (!isValidTimeRange(searchForm.value.from, searchForm.value.to)) {
    error.value = 'Search start time must be earlier than end time.'
    slots.value = []
    searchedOnce.value = true
    showAlertModal({ type: 'error', message: error.value })
    return
  }

  searchLoading.value = true
  try {
    const rows = await api.adminListSlots(buildSearchParams())
    slots.value = sortSlots(Array.isArray(rows) ? rows : [])
    searchedOnce.value = true
    if (announceSuccess) {
      success.value = `Loaded ${slotCountLabel.value}.`
      showAlertModal({ type: 'success', message: success.value })
    }
  } catch (e) {
    error.value = e?.message || 'Failed to load slots'
    slots.value = []
    searchedOnce.value = true
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    searchLoading.value = false
  }
}

function resetSearchForm() {
  searchForm.value = {
    specialistId: '',
    date: today,
    from: '',
    to: '',
    available: ''
  }
}

function openEdit(row) {
  editForm.value = {
    id: slotId(row),
    specialistId: slotSpecialistId(row),
    date: String(row?.date ?? '').trim(),
    start: String(row?.start ?? row?.startTime ?? '').trim(),
    end: String(row?.end ?? row?.endTime ?? '').trim(),
    available: slotAvailable(row),
    amount: slotAmount(row),
    currency: slotCurrency(row),
    type: slotType(row),
    detail: String(row?.detail ?? '').trim()
  }
  editOpen.value = true
}

function closeEdit() {
  if (updateLoading.value) return
  editOpen.value = false
  editForm.value = {
    id: '',
    specialistId: '',
    date: '',
    start: '',
    end: '',
    available: true,
    amount: '0.00',
    currency: 'CNY',
    type: 'online',
    detail: ''
  }
}

async function onSearch() {
  await loadSlots({ announceSuccess: true })
}

async function onUpdate() {
  clearMessages()

  if (!editForm.value.id) {
    error.value = 'Missing slot ID.'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  if (!editForm.value.date || !editForm.value.start || !editForm.value.end) {
    error.value = 'Please complete date, start time, and end time.'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  if (!isValidTimeRange(editForm.value.start, editForm.value.end)) {
    error.value = 'Edit slot start time must be earlier than end time.'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  if (editDurationMinutes.value <= 0) {
    error.value = 'Please enter a valid duration in minutes.'
    showAlertModal({ type: 'error', message: error.value })
    return
  }

  updateLoading.value = true
  try {
    await api.adminUpdateSlot(editForm.value.id, {
      ...buildSlotPayload(editForm.value)
    })
    await loadSlots({ preserveMessages: true })
    if (!error.value) {
      success.value = `Slot ${editForm.value.id} updated successfully.`
      showAlertModal({ type: 'success', message: success.value })
      closeEdit()
    }
  } catch (e) {
    error.value = e?.message || 'Failed to update slot'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    updateLoading.value = false
  }
}

async function onDelete(row) {
  const id = slotId(row)
  if (!id) {
    error.value = 'This slot is missing an ID and cannot be deleted.'
    showAlertModal({ type: 'error', message: error.value })
    return
  }

  const confirmed = window.confirm(`Delete slot "${id}"? This action cannot be undone.`)
  if (!confirmed) return

  clearMessages()
  deletingId.value = id
  try {
    await api.adminDeleteSlot(id)
    await loadSlots({ preserveMessages: true })
    if (!error.value) {
      success.value = `Slot ${id} deleted successfully.`
      showAlertModal({ type: 'success', message: success.value })
    }
  } catch (e) {
    error.value = e?.message || 'Failed to delete slot'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    deletingId.value = ''
  }
}

onMounted(async () => {
  await Promise.all([loadSpecialists(), loadSlots()])
})
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Slot Management</h1>
      <p class="subtitle">
        Manage specialist availability by searching, reviewing, and updating consultation slots.
      </p>
    </header>

    <div class="workspace-grid">
      <section class="calc-card">
        <div class="panel-head">
          <h2 class="card-title">Search Slots</h2>
        </div>

        <div class="field">
          <span class="label">Specialist</span>
          <select v-model="searchForm.specialistId" class="input input--select" :disabled="specialistsLoading">
            <option value="">All specialists</option>
            <option v-for="row in specialists" :key="row.id" :value="row.id">
              {{ row.name || row.id }} ({{ row.id }})
            </option>
          </select>
        </div>

        <div class="field-grid field-grid--two">
          <label class="field">
            <span class="label">Date</span>
            <input v-model="searchForm.date" type="date" class="input" />
          </label>
          <div class="field">
            <span class="label">Availability</span>
            <div class="option-row option-row--compact">
              <button
                type="button"
                class="option-btn option-btn--all"
                :class="{ 'option-btn--active': searchForm.available === '' }"
                @click="searchForm.available = ''"
              >
                All
              </button>
              <button
                type="button"
                class="option-btn option-btn--available"
                :class="{ 'option-btn--active': searchForm.available === 'true' }"
                @click="searchForm.available = 'true'"
              >
                Available
              </button>
              <button
                type="button"
                class="option-btn option-btn--unavailable"
                :class="{ 'option-btn--active': searchForm.available === 'false' }"
                @click="searchForm.available = 'false'"
              >
                Unavailable
              </button>
            </div>
          </div>
        </div>

        <div class="field-grid field-grid--two">
          <label class="field">
            <span class="label">From</span>
            <input v-model="searchForm.from" type="time" class="input" />
          </label>
          <label class="field">
            <span class="label">To</span>
            <input v-model="searchForm.to" type="time" class="input" />
          </label>
        </div>

        <!-- banner 弃用：错误改为弹窗提示（见 loadSpecialists） -->

        <div class="button-row">
          <button type="button" class="btn-primary btn-primary--fit" :disabled="searchLoading" @click="onSearch">
            {{ searchLoading ? 'Searching...' : 'Search Slots' }}
          </button>
          <button type="button" class="btn-neutral" :disabled="searchLoading" @click="resetSearchForm">
            Reset Filters
          </button>
        </div>
      </section>
    </div>

    <section class="calc-card list-card">
      <div class="list-toolbar">
        <div class="toolbar-title">
          <h2 class="card-title">Existing Slots</h2>
          <p class="list-note">
            Search results populate this management table for review, edit, and delete actions.
          </p>
        </div>
        <div class="toolbar-actions">
          <button type="button" class="btn-neutral btn-create-nav" @click="router.push({ name: 'admin.slotCreate' })">
            Go to Create Slot
          </button>
          <input
            v-model.trim="slotSearchQuery"
            class="input search-input"
            type="text"
            placeholder="Search slots"
            aria-label="Search slots"
          />
          <button type="button" class="btn-neutral btn-refresh" :disabled="searchLoading" @click="onSearch">
            {{ searchLoading ? 'Refreshing...' : 'Refresh' }}
          </button>
        </div>
      </div>

      <div class="toolbar-meta">
        <span class="meta-pill">{{ slotCountLabel }}</span>
      </div>

      <div v-if="searchLoading && !slots.length" class="state">Loading slots...</div>

      <div v-else-if="!slots.length" class="state state--empty">
        {{ searchedOnce ? 'No slots found for the current filters.' : 'Run a search to review slots.' }}
      </div>

      <div v-else-if="!filteredSlots.length" class="state state--empty">
        No existing slots matched your search.
      </div>

      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th scope="col">Specialist</th>
              <th scope="col">Schedule</th>
              <th scope="col">Price</th>
              <th scope="col">Session</th>
              <th scope="col">Detail</th>
              <th scope="col">Availability</th>
              <th scope="col" class="th-actions">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredSlots" :key="slotId(row)">
              <td class="cell--wrap">{{ formatSpecialistLabel(slotSpecialistId(row)) }}</td>
              <td>{{ slotSchedule(row) }}</td>
              <td>{{ slotAmount(row) }} {{ slotCurrency(row) }}</td>
              <td>{{ slotSession(row) }}</td>
              <td class="cell--detail" :title="slotDetail(row)">{{ slotDetail(row) }}</td>
              <td>
                <span class="status-pill" :class="{ 'status-pill--off': !slotAvailable(row) }">
                  {{ formatAvailabilityLabel(slotAvailable(row)) }}
                </span>
              </td>
              <td>
                <div class="row-actions">
                  <button
                    type="button"
                    class="action-btn"
                    :disabled="updateLoading || deletingId === slotId(row)"
                    @click="openEdit(row)"
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    class="action-btn action-btn--danger"
                    :disabled="updateLoading || deletingId === slotId(row)"
                    @click="onDelete(row)"
                  >
                    {{ deletingId === slotId(row) ? 'Deleting...' : 'Delete' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="editOpen" class="modal-backdrop" @click.self="closeEdit">
      <section class="modal-card">
        <div class="panel-head">
          <h3 class="modal-title">Edit Slot</h3>
        </div>

        <div class="detail-list">
          <div class="detail-row">
            <span class="detail-key">Slot ID</span>
            <span class="detail-value mono">{{ editForm.id || '--' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-key">Specialist</span>
            <span class="detail-value">{{ formatSpecialistLabel(editForm.specialistId) }}</span>
          </div>
        </div>

        <div class="field-grid field-grid--two">
          <label class="field">
            <span class="label">Date</span>
            <input v-model="editForm.date" type="date" class="input" />
          </label>
          <div class="field">
            <span class="label">Availability</span>
            <div class="option-row">
              <button
                type="button"
                class="option-btn option-btn--type"
                :class="{ 'option-btn--active': editForm.available === true }"
                @click="editForm.available = true"
              >
                Available
              </button>
              <button
                type="button"
                class="option-btn option-btn--type"
                :class="{ 'option-btn--active': editForm.available === false }"
                @click="editForm.available = false"
              >
                Unavailable
              </button>
            </div>
          </div>
        </div>

        <div class="field-grid field-grid--two">
          <label class="field">
            <span class="label">Start Time</span>
            <input v-model="editForm.start" type="time" class="input" />
          </label>
          <label class="field">
            <span class="label">End Time</span>
            <input v-model="editForm.end" type="time" class="input" />
          </label>
        </div>

        <div class="field-grid field-grid--two">
          <label class="field">
            <span class="label">Amount</span>
            <input v-model="editForm.amount" type="number" min="0" step="0.01" class="input" />
          </label>
          <label class="field">
            <span class="label">Currency</span>
            <input v-model.trim="editForm.currency" type="text" maxlength="10" class="input" />
          </label>
        </div>

        <div class="field-grid field-grid--two">
          <label class="field">
            <span class="label">Duration (minutes, auto)</span>
            <input :value="editDurationMinutes" type="number" class="input" readonly />
          </label>
          <label class="field">
            <span class="label">Type</span>
            <input v-model.trim="editForm.type" type="text" maxlength="20" class="input" />
          </label>
        </div>

        <label class="field">
          <span class="label">Detail</span>
          <textarea v-model="editForm.detail" class="input input--textarea" rows="3" />
        </label>

        <div class="modal-footer">
          <div class="tip-wrap">
            <span class="icon">!</span>
            <div class="tooltip">
              Please check and follow Specialist's Pricing Rules!<br>
            </div>
          </div>
          <button type="button" class="btn-neutral" :disabled="updateLoading" @click="closeEdit">
            Cancel
          </button>
          <button type="button" class="btn-primary btn-primary--fit modal-save" :disabled="updateLoading" @click="onUpdate">
            {{ updateLoading ? 'Saving...' : 'Save Changes' }}
          </button>
        </div>
      </section>
    </div>
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

.workspace-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
  align-items: start;
  max-width: 980px;
}

.btn-create-nav {
  min-width: 200px;
}

.calc-card {
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  padding: 16px;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}

.list-card {
  margin-top: 14px;
  max-width: 1160px;
}

.panel-head {
  margin-bottom: 12px;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.panel-note {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.45;
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

.option-row--compact {
  flex-wrap: nowrap;
}

.option-row--compact .option-btn {
  min-width: 0;
  flex: 1 1 0;
  padding: 0 10px;
}

.option-row--compact .option-btn--all {
  flex: 0.8 1 0;
}

.option-row--compact .option-btn--available {
  flex: 1 1 0;
}

.option-row--compact .option-btn--unavailable {
  flex: 1.25 1 0;
}

.option-btn {
  min-width: 84px;
  height: 38px;
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
  min-width: 110px;
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

.banner {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 0;
  font-size: 13px;
}

.banner--inline {
  margin-top: 0;
  margin-bottom: 14px;
}

.banner--success {
  border: 1px solid rgba(34, 197, 94, 0.32);
  background: rgba(34, 197, 94, 0.08);
  color: #166534;
}

.banner--error {
  border: 1px solid rgba(248, 113, 113, 0.45);
  background: rgba(248, 113, 113, 0.12);
  color: #991b1b;
}

.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.toolbar-title .list-note {
  max-width: 520px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-meta {
  margin: 12px 0;
}

.search-input {
  width: min(280px, 44vw);
}

.btn-refresh {
  min-width: 0;
  height: 40px;
  padding: 0 14px;
  border-color: #a94442;
  background: #a94442;
  color: #ffffff;
  font-weight: 600;
}

.list-note {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.btn-create-nav {
  height: 40px;
  min-width: 150px;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #374151;
  font-size: 12px;
  font-weight: 700;
}

.state {
  margin-top: 4px;
  padding: 16px 14px;
  border: 1px solid #eceff3;
  background: #fafafa;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.state--empty {
  border-style: dashed;
}

.table-wrap {
  overflow-x: auto;
  border: 1px solid #eceff3;
  background: #ffffff;
}

.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 980px;
}

.table th,
.table td {
  padding: 12px 14px;
  border-bottom: 1px solid #eceff3;
  text-align: center;
  vertical-align: middle;
  font-size: 13px;
  color: #111827;
}

.table th {
  background: #fafafa;
  color: #4b5563;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.table tbody tr:last-child td {
  border-bottom: 0;
}

.th-actions {
  min-width: 150px;
}

.cell--wrap {
  min-width: 180px;
}

.cell--detail {
  max-width: 260px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0 auto;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.weak {
  color: #6b7280;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(34, 197, 94, 0.32);
  background: rgba(34, 197, 94, 0.08);
  color: #166534;
  font-size: 12px;
  font-weight: 700;
}

.status-pill--off {
  border-color: rgba(248, 113, 113, 0.38);
  background: rgba(248, 113, 113, 0.12);
  color: #991b1b;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  width: 100%;
  transform: translateX(10px);
}

.action-btn {
  min-width: 64px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid #202124;
  border-radius: 0;
  background: #ffffff;
  color: #202124;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.action-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.action-btn--danger {
  border-color: #a94442;
  color: #a94442;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(17, 24, 39, 0.42);
}

.modal-card {
  width: min(100%, 620px);
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  padding: 18px;
  box-shadow: 0 16px 36px rgba(17, 24, 39, 0.16);
}

.modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}

.detail-list {
  margin-bottom: 14px;
  border-bottom: 1px solid #eceff3;
  padding: 10px 0;
  display: grid;
  gap: 8px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.detail-key {
  color: #6b7280;
  font-size: 13px;
}

.detail-value {
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  text-align: right;
}

.modal-footer {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.modal-save {
  max-width: 180px;
}

@media (max-width: 980px) {
  .workspace-grid,
  .field-grid--two {
    grid-template-columns: 1fr;
  }

  .list-toolbar,
  .button-row,
  .modal-footer,
  .toolbar-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-actions {
    width: 100%;
  }

  .search-input {
    width: 100%;
  }

  .btn-primary--fit,
  .modal-save {
    max-width: none;
  }
}
</style>
