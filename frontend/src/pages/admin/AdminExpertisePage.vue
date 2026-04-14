<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'

const items = ref([])
const loading = ref(false)
const error = ref('')
const success = ref('')
const searchQuery = ref('')

const createName = ref('')
const createDesc = ref('')
const creating = ref(false)
const nameFocused = ref(false)
const descFocused = ref(false)
const nameLimitError = ref('')
const descLimitError = ref('')

const editOpen = ref(false)
const editId = ref('')
const editName = ref('')
const editDesc = ref('')
const updating = ref(false)
const deletingId = ref('')
const editNameFocused = ref(false)
const editDescFocused = ref(false)
const editNameLimitError = ref('')
const editDescLimitError = ref('')

const NAME_MAX = 50
const DESC_MAX = 300

const showNameHelper = computed(() => nameFocused.value || !!nameLimitError.value)
const showDescHelper = computed(() => descFocused.value || !!descLimitError.value)
const showEditNameHelper = computed(() => editNameFocused.value || !!editNameLimitError.value)
const showEditDescHelper = computed(() => editDescFocused.value || !!editDescLimitError.value)

const filteredItems = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((row) => String(row?.name ?? '').toLowerCase().includes(q))
})
const expertiseCountLabel = computed(() => {
  const count = items.value.length
  return `${count} expertise item${count === 1 ? '' : 's'}`
})

function rowId(row) {
  return row?.id != null ? String(row.id) : ''
}

function rowDesc(row) {
  const v = row?.description ?? row?.desc ?? ''
  return typeof v === 'string' ? v : ''
}

function onNameFocus() {
  nameFocused.value = true
}

function onNameBlur() {
  nameFocused.value = false
  if (createName.value.length <= NAME_MAX) {
    nameLimitError.value = ''
  }
}

function onDescFocus() {
  descFocused.value = true
}

function onDescBlur() {
  descFocused.value = false
  if (createDesc.value.length <= DESC_MAX) {
    descLimitError.value = ''
  }
}

function onEditNameFocus() {
  editNameFocused.value = true
}

function onEditNameBlur() {
  editNameFocused.value = false
  if (editName.value.length <= NAME_MAX) {
    editNameLimitError.value = ''
  }
}

function onEditDescFocus() {
  editDescFocused.value = true
}

function onEditDescBlur() {
  editDescFocused.value = false
  if (editDesc.value.length <= DESC_MAX) {
    editDescLimitError.value = ''
  }
}

watch(createName, (val) => {
  if (val.length > NAME_MAX) {
    createName.value = val.slice(0, NAME_MAX)
    nameLimitError.value = `Maximum ${NAME_MAX} characters allowed.`
    return
  }
  if (val.length < NAME_MAX) {
    nameLimitError.value = ''
  }
})

watch(createDesc, (val) => {
  if (val.length > DESC_MAX) {
    createDesc.value = val.slice(0, DESC_MAX)
    descLimitError.value = `Maximum ${DESC_MAX} characters allowed.`
    return
  }
  if (val.length < DESC_MAX) {
    descLimitError.value = ''
  }
})

watch(editName, (val) => {
  if (val.length > NAME_MAX) {
    editName.value = val.slice(0, NAME_MAX)
    editNameLimitError.value = `Maximum ${NAME_MAX} characters allowed.`
    return
  }
  if (val.length < NAME_MAX) {
    editNameLimitError.value = ''
  }
})

watch(editDesc, (val) => {
  if (val.length > DESC_MAX) {
    editDesc.value = val.slice(0, DESC_MAX)
    editDescLimitError.value = `Maximum ${DESC_MAX} characters allowed.`
    return
  }
  if (val.length < DESC_MAX) {
    editDescLimitError.value = ''
  }
})

async function load() {
  error.value = ''
  loading.value = true
  try {
    items.value = await api.listExpertise()
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    items.value = []
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  if (!createName.value.trim()) {
    error.value = 'Please enter an expertise name'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  creating.value = true
  error.value = ''
  success.value = ''
  try {
    await api.adminCreateExpertise({
      name: createName.value.trim(),
      description: createDesc.value.trim() || undefined
    })
    createName.value = ''
    createDesc.value = ''
    await load()
    success.value = 'Expertise created successfully.'
    showAlertModal({ type: 'success', message: success.value })
  } catch (e) {
    error.value = e?.message || 'Failed to create'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    creating.value = false
  }
}

function openEdit(row) {
  const id = rowId(row)
  if (!id) {
    error.value = 'This expertise record has no ID and cannot be edited.'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  error.value = ''
  success.value = ''
  editId.value = id
  editName.value = String(row?.name ?? '')
  editDesc.value = rowDesc(row)
  editNameFocused.value = false
  editDescFocused.value = false
  editNameLimitError.value = ''
  editDescLimitError.value = ''
  editOpen.value = true
}

function closeEdit(force = false) {
  if (updating.value && !force) return
  editOpen.value = false
  editId.value = ''
  editName.value = ''
  editDesc.value = ''
  editNameFocused.value = false
  editDescFocused.value = false
  editNameLimitError.value = ''
  editDescLimitError.value = ''
}

async function onUpdate() {
  if (!editId.value) {
    error.value = 'Missing expertise ID'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  if (!editName.value.trim()) {
    error.value = 'Please enter an expertise name'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  updating.value = true
  error.value = ''
  success.value = ''
  try {
    await api.adminUpdateExpertise(editId.value, {
      name: editName.value.trim(),
      description: editDesc.value.trim() || undefined
    })
    await load()
    success.value = 'Expertise updated successfully.'
    showAlertModal({ type: 'success', message: success.value, onClose: () => closeEdit(true) })
  } catch (e) {
    error.value = e?.message || 'Failed to update'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    updating.value = false
  }
}

async function onDelete(row) {
  const id = rowId(row)
  if (!id) {
    error.value = 'This expertise record has no ID and cannot be deleted.'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  const name = String(row?.name ?? id)
  const ok = window.confirm(`Delete expertise "${name}"? This action cannot be undone.`)
  if (!ok) return

  deletingId.value = id
  error.value = ''
  success.value = ''
  try {
    await api.adminDeleteExpertise(id)
    await load()
    success.value = 'Expertise deleted successfully.'
    showAlertModal({ type: 'success', message: success.value })
  } catch (e) {
    error.value = e?.message || 'Failed to delete'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    deletingId.value = ''
  }
}

onMounted(load)
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Expertise Management</h1>
      <p class="subtitle">
        Manage expertise categories used for specialist classification and booking.
      </p>
    </header>

    <section class="calc-card create-card">
      <h2 class="card-title">Create Expertise</h2>
      <div class="create-form">
        <input
          v-model="createName"
          class="input"
          type="text"
          placeholder="Name"
          aria-label="Name"
          maxlength="50"
          @focus="onNameFocus"
          @blur="onNameBlur"
        />

        <div
          v-if="showNameHelper"
          class="limit-helper"
          :class="{ 'limit-helper--error': nameLimitError }"
          role="status"
          aria-live="polite"
        >
          <p class="limit-helper__text">{{ nameLimitError || `Maximum ${NAME_MAX} characters` }}</p>
          <p class="limit-helper__count">{{ createName.length }}/{{ NAME_MAX }}</p>
        </div>

        <textarea
          v-model="createDesc"
          class="input input--area"
          placeholder="Description (optional)"
          aria-label="Description (optional)"
          maxlength="300"
          @focus="onDescFocus"
          @blur="onDescBlur"
        ></textarea>

        <div
          v-if="showDescHelper"
          class="limit-helper"
          :class="{ 'limit-helper--error': descLimitError }"
          role="status"
          aria-live="polite"
        >
          <p class="limit-helper__text">{{ descLimitError || `Maximum ${DESC_MAX} characters` }}</p>
          <p class="limit-helper__count">{{ createDesc.length }}/{{ DESC_MAX }}</p>
        </div>

        <button type="button" class="btn-primary btn-primary--fit" :disabled="creating" @click="onCreate">
          {{ creating ? 'Creating...' : 'Create' }}
        </button>
      </div>
    </section>

    <section class="calc-card list-card">
      <div class="list-toolbar">
        <div class="toolbar-title">
          <h2 class="card-title">Expertise List</h2>
          <p v-if="searchQuery" class="muted small">
            Matched: {{ filteredItems.length }}
          </p>
        </div>

        <div class="toolbar-actions">
          <input
            v-model.trim="searchQuery"
            class="input search-input"
            type="text"
            placeholder="Search expertise by name"
            aria-label="Search expertise by name"
          />
          <button type="button" class="btn-neutral btn-refresh" :disabled="loading" @click="load">
            {{ loading ? 'Loading...' : 'Refresh' }}
          </button>
        </div>
      </div>

      <div class="toolbar-meta">
        <span class="meta-pill">{{ expertiseCountLabel }}</span>
      </div>

      <div v-if="loading && !items.length" class="state muted">Loading...</div>

      <div v-else-if="!filteredItems.length" class="state muted">
        {{ searchQuery ? 'No expertise matched your search.' : 'No expertise data.' }}
      </div>

      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th scope="col" class="th-id">ID</th>
              <th scope="col">Name</th>
              <th scope="col">Description</th>
              <th scope="col" class="th-actions">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredItems" :key="row.id ?? row.name">
              <td class="mono weak">{{ rowId(row) || '—' }}</td>
              <td class="name-cell">{{ row.name ?? '—' }}</td>
              <td class="desc-cell">{{ rowDesc(row) || '—' }}</td>
              <td>
                <div class="row-actions">
                  <button
                    type="button"
                    class="action-btn"
                    :disabled="!rowId(row) || updating || deletingId === rowId(row)"
                    @click="openEdit(row)"
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    class="action-btn action-btn--danger"
                    :disabled="!rowId(row) || updating || deletingId === rowId(row)"
                    @click="onDelete(row)"
                  >
                    {{ deletingId === rowId(row) ? 'Deleting...' : 'Delete' }}
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
          <h3 class="modal-title">Edit Expertise</h3>
        </div>

        <div class="detail-list">
          <div class="detail-row">
            <span class="detail-key">Expertise ID</span>
            <span class="detail-value mono">{{ editId || '--' }}</span>
          </div>
        </div>

        <div class="modal-form">
          <input
            v-model="editName"
            class="input"
            type="text"
            placeholder="Name"
            aria-label="Edit expertise name"
            maxlength="50"
            @focus="onEditNameFocus"
            @blur="onEditNameBlur"
          />

          <div
            v-if="showEditNameHelper"
            class="limit-helper"
            :class="{ 'limit-helper--error': editNameLimitError }"
            role="status"
            aria-live="polite"
          >
            <p class="limit-helper__text">{{ editNameLimitError || `Maximum ${NAME_MAX} characters` }}</p>
            <p class="limit-helper__count">{{ editName.length }}/{{ NAME_MAX }}</p>
          </div>

          <textarea
            v-model="editDesc"
            class="input input--area"
            placeholder="Description (optional)"
            aria-label="Edit expertise description"
            maxlength="300"
            @focus="onEditDescFocus"
            @blur="onEditDescBlur"
          ></textarea>

          <div
            v-if="showEditDescHelper"
            class="limit-helper"
            :class="{ 'limit-helper--error': editDescLimitError }"
            role="status"
            aria-live="polite"
          >
            <p class="limit-helper__text">{{ editDescLimitError || `Maximum ${DESC_MAX} characters` }}</p>
            <p class="limit-helper__count">{{ editDesc.length }}/{{ DESC_MAX }}</p>
          </div>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn-neutral modal-cancel" :disabled="updating" @click="closeEdit">
            Cancel
          </button>
          <button
            type="button"
            class="btn-primary btn-primary--fit modal-save"
            :disabled="updating"
            @click="onUpdate"
          >
            {{ updating ? 'Saving...' : 'Save Changes' }}
          </button>
        </div>
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

.calc-card {
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  padding: 16px;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}

.list-card {
  margin-top: 14px;
}

.card-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 700;
}

.panel-head {
  margin-bottom: 12px;
}

.create-form {
  margin-top: 12px;
  display: grid;
  gap: 10px;
  max-width: 760px;
}

.input {
  height: 44px;
  padding: 0 12px;
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #f8f5f2;
  color: #111827;
  width: 100%;
}

.input::placeholder {
  color: #6b7280;
}

.input--area {
  height: auto;
  min-height: 118px;
  padding: 10px 12px;
  resize: vertical;
}

.limit-helper {
  border: 1px solid #ddd3cb;
  background: #f8f5f2;
  padding: 8px 10px;
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

.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}

.toolbar-title .small {
  margin: 4px 0 0;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-meta {
  margin: 12px 0;
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

.search-input {
  width: min(320px, 50vw);
}

.btn-neutral {
  height: 40px;
  padding: 0 14px;
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #f8f5f2;
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-neutral:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-refresh {
  border-color: #a94442;
  background: #a94442;
  color: #ffffff;
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

.banner--success {
  border: 1px solid rgba(34, 197, 94, 0.35);
  background: rgba(34, 197, 94, 0.12);
  color: #14532d;
}

.state {
  margin-top: 12px;
  padding: 10px 0;
}

.muted {
  color: #6b7280;
}

.table-wrap {
  margin-top: 12px;
  overflow-x: auto;
  border: 1px solid #eceff3;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  background: #ffffff;
}

.table th,
.table td {
  padding: 11px 14px;
  text-align: left;
  border-bottom: 1px solid #eceff3;
  vertical-align: top;
}

.table th {
  font-weight: 700;
  color: #4b5563;
  background: #fafafa;
  white-space: nowrap;
}

.table tbody tr:last-child td {
  border-bottom: none;
}

.th-id {
  width: 160px;
}

.th-actions {
  width: 170px;
}

.name-cell {
  font-weight: 600;
}

.desc-cell {
  color: #4b5563;
  line-height: 1.45;
}

.mono {
  font-family: ui-monospace, monospace;
}

.weak {
  font-size: 12px;
  color: #6b7280;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn--danger {
  border-color: #a94442;
  background: #ffffff;
  color: #a94442;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: rgba(17, 24, 39, 0.45);
  display: grid;
  place-items: center;
  padding: 20px;
}

.modal-card {
  width: min(560px, 100%);
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.14);
  border-radius: 0;
  padding: 16px;
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

.modal-form {
  display: grid;
  gap: 10px;
}

.modal-footer {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.modal-cancel {
  height: 44px;
  border-color: #202124;
  background: #ffffff;
  color: #202124;
  font-weight: 700;
}

.modal-save {
  max-width: 180px;
}

@media (max-width: 920px) {
  .toolbar-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .list-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .input--area {
    min-height: 104px;
  }

  .modal-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
