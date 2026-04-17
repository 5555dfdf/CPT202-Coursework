<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'

const router = useRouter()

const slots = ref([])
const loading = ref(false)
const error = ref('')
const success = ref('')
const deletingId = ref('')
const editOpen = ref(false)
const editLoading = ref(false)
const editSaving = ref(false)
const editingSlotId = ref('')
const DETAIL_MAX = 300

const editForm = ref({
  date: '',
  start: '',
  end: '',
  available: true,
  amount: '0.00',
  currency: 'CNY',
  type: 'online',
  detail: ''
})

const formattedSlots = computed(() => {
  return slots.value.map(slot => ({
    ...slot,
    formattedDate: formatDate(slot.date),
    formattedDuration: formatDuration(slot.duration),
    formattedAmount: formatCurrency(slot.amount, slot.currency),
    formattedType: formatType(slot.type)
  }))
})

function formatDate(dateStr) {
  if (!dateStr) return '--'
  try {
    return new Date(dateStr).toLocaleDateString('zh-CN')
  } catch {
    return dateStr
  }
}

function formatDuration(minutes) {
  const mins = Number(minutes)
  return mins > 0 ? `${mins} min` : '--'
}

function formatCurrency(amount, currency = 'CNY') {
  const num = Number(amount)
  if (!Number.isFinite(num)) return '--'
  return `${num.toFixed(2)} ${currency}`
}

function formatType(type) {
  return String(type || '--')
}

function calcDurationMinutes(startStr, endStr) {
  if (!startStr || !endStr) return 0
  const [startHour, startMin] = String(startStr).split(':').map(Number)
  const [endHour, endMin] = String(endStr).split(':').map(Number)
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

function buildEditPayload(form) {
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
  return [...rows].sort((a, b) =>
    (a.date + a.start).localeCompare(b.date + b.start)
  )
}

async function loadSlots() {
  loading.value = true
  try {
    const res = await api.specialistListSlots()
    slots.value = sortSlots(Array.isArray(res) ? res : [])
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  if (!confirm('Delete this slot?')) return
  deletingId.value = id
  try {
    await api.specialistDeleteSlot(id)
    await loadSlots()
    success.value = `Slot ${id} deleted`
    showAlertModal({ type: 'success', message: success.value })
  } catch (e) {
    error.value = e?.message || 'Delete failed'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    deletingId.value = ''
  }
}

async function handleEdit(id) {
  if (!id) return
  editOpen.value = true
  editLoading.value = true
  editingSlotId.value = String(id)
  try {
    const slot = await api.specialistGetSlot(id)
    editForm.value = {
      date: slot?.date || new Date().toISOString().split('T')[0],
      start: slot?.start || '09:00',
      end: slot?.end || '09:30',
      available: slot?.available ?? true,
      amount: slot?.amount?.toString() || '0.00',
      currency: slot?.currency || 'CNY',
      type: slot?.type || 'online',
      detail: slot?.detail || ''
    }
  } catch (e) {
    editOpen.value = false
    error.value = e?.message || 'Failed to load slot details'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    editLoading.value = false
  }
}

function closeEditModal() {
  if (editSaving.value) return
  editOpen.value = false
  editLoading.value = false
  editingSlotId.value = ''
}

async function saveEditSlot() {
  if (!editingSlotId.value) return
  if (!editForm.value.date || !editForm.value.start || !editForm.value.end) {
    showAlertModal({ type: 'error', message: 'Please complete date, start time, and end time.' })
    return
  }
  if (!isValidTimeRange(editForm.value.start, editForm.value.end)) {
    showAlertModal({ type: 'error', message: 'Start time must be earlier than end time.' })
    return
  }
  if (calcDurationMinutes(editForm.value.start, editForm.value.end) <= 0) {
    showAlertModal({ type: 'error', message: 'Please enter a valid duration in minutes.' })
    return
  }

  editSaving.value = true
  try {
    await api.specialistUpdateSlot(editingSlotId.value, buildEditPayload(editForm.value))
    await loadSlots()
    success.value = `Slot ${editingSlotId.value} updated`
    showAlertModal({ type: 'success', message: success.value })
    closeEditModal()
  } catch (e) {
    error.value = e?.message || 'Failed to update slot'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    editSaving.value = false
  }
}

function goToCreate() {
  router.push({ name: 'specialist.slotCreate' })
}

onMounted(loadSlots)
</script>

<template>
  <section class="page">
    <header class="page__header">
      <div>
        <h1>Slot Management</h1>
        <p class="subtitle">Manage your consultation slots</p>
      </div>

      <!-- ✅ btn-neutral（不会再被覆盖） -->
      <button class="btn-neutral" @click="goToCreate">
        Create Slot
      </button>
    </header>

    <section class="calc-card">
      <div v-if="error" class="banner banner--error">{{ error }}</div>
      <div v-if="success" class="banner banner--success">{{ success }}</div>

      <!-- ✅ 第一份代码风格 -->
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Schedule</th>
              <th>Price</th>
              <th>Session</th>
              <th>Detail</th>
              <th>Availability</th>
              <th class="th-actions">Actions</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="slot in formattedSlots" :key="slot.id">
              
              <td>
                {{ slot.formattedDate }} {{ slot.start }}-{{ slot.end }}
              </td>

              <td>{{ slot.formattedAmount }}</td>

              <td>
                {{ slot.formattedDuration }} · {{ slot.formattedType }}
              </td>

              <td class="cell--detail" :title="slot.detail">
                {{ slot.detail || '--' }}
              </td>

              <td>
                <span
                  class="status-pill"
                  :class="{ 'status-pill--off': !slot.available }"
                >
                  {{ slot.available ? 'Available' : 'Unavailable' }}
                </span>
              </td>

              <td>
                <div class="row-actions">
                  <button class="action-btn" @click="handleEdit(slot.id)">
                    Edit
                  </button>

                  <button
                    class="action-btn action-btn--danger"
                    @click="handleDelete(slot.id)"
                    :disabled="deletingId === slot.id"
                  >
                    {{ deletingId === slot.id ? 'Deleting...' : 'Delete' }}
                  </button>
                </div>
              </td>

            </tr>
          </tbody>
        </table>

        <div v-if="!loading && !formattedSlots.length" class="state state--empty">
          No slots found.
        </div>

        <div v-if="loading" class="state">
          Loading slots...
        </div>
      </div>
    </section>
  </section>

  <div v-if="editOpen" class="modal-backdrop" @click.self="closeEditModal">
    <section class="modal-card">
      <h3 class="modal-title">Edit Slot</h3>

      <div v-if="editLoading" class="state">Loading slot details...</div>
      <template v-else>
        <div class="form-container">
          <div class="field-grid field-grid--two">
            <label class="field">
              <span class="label">Date</span>
              <input v-model="editForm.date" type="date" class="input" :disabled="editSaving" />
            </label>

            <div class="field">
              <span class="label">Availability</span>
              <div class="option-row">
                <button
                  type="button"
                  class="option-btn option-btn--type"
                  :class="{ 'option-btn--active': editForm.available === true }"
                  :disabled="editSaving"
                  @click="editForm.available = true"
                >
                  Available
                </button>
                <button
                  type="button"
                  class="option-btn option-btn--type"
                  :class="{ 'option-btn--active': editForm.available === false }"
                  :disabled="editSaving"
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
              <input v-model="editForm.start" type="time" class="input" :disabled="editSaving" />
            </label>
            <label class="field">
              <span class="label">End Time</span>
              <input v-model="editForm.end" type="time" class="input" :disabled="editSaving" />
            </label>
          </div>

          <div class="field-grid field-grid--two">
            <label class="field">
              <span class="label">Amount</span>
              <input v-model="editForm.amount" type="number" step="0.01" min="0" class="input" :disabled="editSaving" />
            </label>
            <label class="field">
              <span class="label">Currency</span>
              <select v-model="editForm.currency" class="input input--select" :disabled="editSaving">
                <option value="CNY">CNY</option>
                <option value="USD">USD</option>
                <option value="EUR">EUR</option>
              </select>
            </label>
          </div>

          <div class="field-grid field-grid--two">
            <label class="field">
              <span class="label">Duration (minutes, auto)</span>
              <input :value="calcDurationMinutes(editForm.start, editForm.end)" type="number" class="input" readonly />
            </label>
            <label class="field">
              <span class="label">Type</span>
              <select v-model="editForm.type" class="input input--select" :disabled="editSaving">
                <option value="online">Online</option>
                <option value="offline">Offline</option>
              </select>
            </label>
          </div>

          <label class="field">
            <span class="label">Detail</span>
            <textarea
              v-model="editForm.detail"
              class="input input--textarea"
              rows="3"
              maxlength="300"
              :disabled="editSaving"
            ></textarea>
            <p class="detail-count">{{ (editForm.detail || '').length }}/{{ DETAIL_MAX }}</p>
          </label>

          <div class="form-actions">
            <button type="button" class="action-btn modal-cancel" :disabled="editSaving" @click="closeEditModal">Cancel</button>
            <button type="button" class="btn-primary modal-save" :disabled="editSaving" @click="saveEditSlot">
              {{ editSaving ? 'Saving...' : 'Update Slot' }}
            </button>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<style scoped>
.page__header {
  margin: 8px 0 20px;
  padding: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  background: #fff;
  border: 1px solid rgba(17,24,39,0.1);
  border-radius: 0;
  padding: 16px;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}

/* ✅ 你的按钮风格（最终版） */
.btn-neutral {
  height: 44px;
  padding: 0 14px;
  border: 1px solid #202124;
  background: #ffffff;
  color: #202124;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.btn-neutral:hover {
  opacity: 0.92;
}

/* ✅ 表格风格（第一份代码） */
.table-wrap {
  overflow-x: auto;
  border: 1px solid #eceff3;
}

.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 900px;
}

.table th,
.table td {
  padding: 12px 14px;
  border-bottom: 1px solid #eceff3;
  text-align: center;
}

.table th {
  background: #fafafa;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.cell--detail {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ✅ 状态 */
.status-pill {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(34,197,94,0.3);
  background: rgba(34,197,94,0.08);
  color: #166534;
  font-size: 12px;
  font-weight: 700;
}

.status-pill--off {
  border-color: rgba(248,113,113,0.3);
  background: rgba(248,113,113,0.12);
  color: #991b1b;
}

/* ✅ 操作按钮 */
.row-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-btn {
  height: 34px;
  padding: 0 12px;
  border: 1px solid #202124;
  border-radius: 0;
  background: #fff;
  cursor: pointer;
  font-weight: 700;
}

.action-btn--danger {
  border-color: #a94442;
  color: #a94442;
}

/* 状态 */
.state {
  text-align: center;
  padding: 16px;
  color: #6b7280;
}

.state--empty {
  border-style: dashed;
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
  width: min(100%, 680px);
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  padding: 18px;
  box-shadow: 0 16px 36px rgba(17, 24, 39, 0.16);
}

.modal-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
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

.modal-cancel {
  min-width: 64px;
  height: 44px;
}

.modal-save {
  height: 44px;
  padding: 0 20px;
  border: 1px solid #a94442;
  border-radius: 0;
  background: #a94442;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.modal-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .field-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
  }

  .page__header {
    flex-direction: column;
    align-items: stretch;
  }

  .modal-cancel,
  .modal-save {
    width: 100%;
  }
}
</style>
