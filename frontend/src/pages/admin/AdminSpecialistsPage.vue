<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'

const expertiseList = ref([])
const page = ref({ items: [], total: 0 })
const loading = ref(false)
const error = ref('')
const success = ref('')
const searchQuery = ref('')

const form = ref({
  name: '',
  userEmail: '',
  password: '',
  price: '',
  bio: '',
  expertiseIds: []
})

const creating = ref(false)
const updateLoading = ref(false)
const deletingId = ref('')
const nameFocused = ref(false)
const nameLimitError = ref('')
const bioFocused = ref(false)
const bioLimitError = ref('')
const editNameFocused = ref(false)
const editNameLimitError = ref('')
const editBioFocused = ref(false)
const editBioLimitError = ref('')

const expertiseOpen = ref(false)
const expertiseSearch = ref('')
const expertiseFieldRef = ref(null)
const editOpen = ref(false)
const editExpertiseOpen = ref(false)
const editExpertiseSearch = ref('')
const editExpertiseFieldRef = ref(null)
const editOriginalStatus = ref('Active')
const editForm = ref({
  id: '',
  name: '',
  price: '',
  bio: '',
  expertiseIds: [],
  status: 'Active'
})
const NAME_MAX = 50
const BIO_MAX_WORDS = 1000

const expertiseMap = computed(() => {
  const map = new Map()
  for (const item of expertiseList.value || []) {
    const id = item?.id != null ? String(item.id) : ''
    if (!id) continue
    map.set(id, item)
  }
  return map
})

const filteredExpertiseOptions = computed(() => {
  const q = expertiseSearch.value.trim().toLowerCase()
  if (!q) return expertiseList.value
  return (expertiseList.value || []).filter((item) => String(item?.name ?? '').toLowerCase().includes(q))
})

const selectedExpertise = computed(() =>
    (form.value.expertiseIds || []).map((id) => expertiseMap.value.get(String(id)) || { id, name: id })
)
const selectedEditExpertise = computed(() =>
    (editForm.value.expertiseIds || []).map((id) => expertiseMap.value.get(String(id)) || { id, name: id })
)

const filteredSpecialists = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  const rows = page.value?.items || []
  if (!q) return rows
  return rows.filter((row) => String(row?.name ?? '').toLowerCase().includes(q))
})
const filteredEditExpertiseOptions = computed(() => {
  const q = editExpertiseSearch.value.trim().toLowerCase()
  if (!q) return expertiseList.value
  return (expertiseList.value || []).filter((item) => String(item?.name ?? '').toLowerCase().includes(q))
})

const totalSpecialists = computed(() => Math.max(Number(page.value?.total) || 0, (page.value?.items || []).length))
const specialistCountLabel = computed(() => {
  const count = totalSpecialists.value
  return `${count} specialist${count === 1 ? '' : 's'}`
})
const showNameHelper = computed(() => {
  return nameFocused.value || !!form.value.name.length || !!nameLimitError.value
})
const showBioHelper = computed(() => {
  return bioFocused.value || !!form.value.bio.length || !!bioLimitError.value
})
const bioWordCount = computed(() => countWords(form.value.bio))
const showEditNameHelper = computed(() => {
  return editNameFocused.value || !!editNameLimitError.value
})
const showEditBioHelper = computed(() => {
  return editBioFocused.value || !!editBioLimitError.value
})
const editBioWordCount = computed(() => countWords(editForm.value.bio))

const expertisePlaceholder = computed(() => {
  if (!form.value.expertiseIds.length) return 'Select expertise'
  if (form.value.expertiseIds.length === 1) return selectedExpertise.value[0]?.name || '1 selected'
  return `${form.value.expertiseIds.length} expertise selected`
})
const editExpertisePlaceholder = computed(() => {
  if (!editForm.value.expertiseIds.length) return 'Select expertise'
  if (editForm.value.expertiseIds.length === 1) return selectedEditExpertise.value[0]?.name || '1 selected'
  return `${editForm.value.expertiseIds.length} expertise selected`
})

function normalizeExpertiseIds(value) {
  if (!Array.isArray(value)) return []
  return value.map((id) => String(id)).filter(Boolean)
}

function toggleExpertisePicker() {
  expertiseOpen.value = !expertiseOpen.value
}

function closeExpertisePicker() {
  expertiseOpen.value = false
}

function toggleExpertise(id) {
  const targetId = String(id)
  if (!targetId) return
  const set = new Set(normalizeExpertiseIds(form.value.expertiseIds))
  if (set.has(targetId)) set.delete(targetId)
  else set.add(targetId)
  form.value.expertiseIds = [...set]
}

function removeExpertise(id) {
  const targetId = String(id)
  form.value.expertiseIds = normalizeExpertiseIds(form.value.expertiseIds).filter((itemId) => itemId !== targetId)
}

function toggleEditExpertisePicker() {
  editExpertiseOpen.value = !editExpertiseOpen.value
}

function closeEditExpertisePicker() {
  editExpertiseOpen.value = false
}

function toggleEditExpertise(id) {
  const targetId = String(id)
  if (!targetId) return
  const set = new Set(normalizeExpertiseIds(editForm.value.expertiseIds))
  if (set.has(targetId)) set.delete(targetId)
  else set.add(targetId)
  editForm.value.expertiseIds = [...set]
}

function removeEditExpertise(id) {
  const targetId = String(id)
  editForm.value.expertiseIds = normalizeExpertiseIds(editForm.value.expertiseIds).filter((itemId) => itemId !== targetId)
}

function handleGlobalClick(event) {
  const createHost = expertiseFieldRef.value
  if (createHost && !createHost.contains(event.target)) {
    closeExpertisePicker()
  }

  const editHost = editExpertiseFieldRef.value
  if (editHost && !editHost.contains(event.target)) {
    closeEditExpertisePicker()
  }
}

function formatPrice(value) {
  const amount = Number(value)
  if (!Number.isFinite(amount)) return '--'
  return `$${amount.toFixed(2)}`
}

function onNameFocus() {
  nameFocused.value = true
}

function onNameBlur() {
  nameFocused.value = false
  if (form.value.name.length <= NAME_MAX) {
    nameLimitError.value = ''
  }
}

function onBioFocus() {
  bioFocused.value = true
}

function onBioBlur() {
  bioFocused.value = false
  if (bioWordCount.value <= BIO_MAX_WORDS) {
    bioLimitError.value = ''
  }
}

function onEditNameFocus() {
  editNameFocused.value = true
}

function onEditNameBlur() {
  editNameFocused.value = false
  if (editForm.value.name.length <= NAME_MAX) {
    editNameLimitError.value = ''
  }
}

function onEditBioFocus() {
  editBioFocused.value = true
}

function onEditBioBlur() {
  editBioFocused.value = false
  if (editBioWordCount.value <= BIO_MAX_WORDS) {
    editBioLimitError.value = ''
  }
}

function countWords(value) {
  const text = String(value || '').trim()
  if (!text) return 0
  return text.split(/\s+/).filter(Boolean).length
}

function trimToWordLimit(value, maxWords) {
  const text = String(value || '')
  const trimmed = text.trim()
  if (!trimmed) return ''
  const words = trimmed.split(/\s+/)
  if (words.length <= maxWords) return text
  return words.slice(0, maxWords).join(' ')
}

function specialistExpertiseNames(row) {
  if (Array.isArray(row?.expertiseNames)) {
    return row.expertiseNames.map((name) => String(name)).filter(Boolean)
  }

  if (Array.isArray(row?.expertise)) {
    const names = row.expertise
        .map((item) => String(item?.name ?? '').trim())
        .filter(Boolean)
    if (names.length) return names
  }

  const ids = normalizeExpertiseIds(row?.expertiseIds)
  if (!ids.length) return []
  return ids.map((id) => expertiseMap.value.get(id)?.name || id)
}

function specialistStatus(row) {
  const status = String(row?.status ?? '').trim()
  return status || 'Unknown'
}

function specialistStatusClass(row) {
  const status = specialistStatus(row).toLowerCase()
  if (status === 'active') return 'status-pill'
  if (status === 'inactive') return 'status-pill status-pill--off'
  return 'status-pill'
}

async function loadExpertise() {
  try {
    expertiseList.value = await api.listExpertise()
  } catch {
    expertiseList.value = []
  }
}

async function loadSpecialists() {
  error.value = ''
  loading.value = true
  try {
    page.value = await api.listSpecialists({ pageSize: 100 })
  } catch (e) {
    error.value = e?.message || 'Failed to load specialists'
    showAlertModal({ type: 'error', message: error.value })
    page.value = { items: [], total: 0 }
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  error.value = ''
  success.value = ''

  if (!form.value.name.trim()) {
    error.value = 'Please enter a name'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  if (!form.value.userEmail.trim()) {
    error.value = 'Please enter user email'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  if (!form.value.expertiseIds.length) {
    error.value = 'Please select at least one expertise item'
    showAlertModal({ type: 'error', message: error.value })
    return
  }

  creating.value = true
  try {
    const price = form.value.price === '' ? undefined : Number(form.value.price)
    await api.adminCreateSpecialist({
      name: form.value.name.trim(),
      userEmail: form.value.userEmail.trim(),
      password: form.value.password.trim() || undefined,
      expertiseIds: normalizeExpertiseIds(form.value.expertiseIds),
      price: Number.isFinite(price) ? price : undefined,
      bio: form.value.bio.trim() || undefined
    })
    form.value = { name: '', userEmail: '', password: '', price: '', bio: '', expertiseIds: [] }
    nameFocused.value = false
    nameLimitError.value = ''
    bioFocused.value = false
    bioLimitError.value = ''
    expertiseSearch.value = ''
    closeExpertisePicker()
    await loadSpecialists()
    success.value = 'Specialist created successfully.'
    showAlertModal({ type: 'success', message: success.value })
  } catch (e) {
    error.value = e?.message || 'Failed to create specialist'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    creating.value = false
  }
}

function openEdit(row) {
  editForm.value = {
    id: row?.id != null ? String(row.id) : '',
    name: String(row?.name ?? '').trim(),
    price: row?.price == null ? '' : String(row.price),
    bio: String(row?.bio ?? '').trim(),
    expertiseIds: normalizeExpertiseIds(row?.expertiseIds),
    status: specialistStatus(row)
  }
  editOriginalStatus.value = specialistStatus(row)
  editExpertiseSearch.value = ''
  editExpertiseOpen.value = false
  editNameFocused.value = false
  editNameLimitError.value = ''
  editBioFocused.value = false
  editBioLimitError.value = ''
  editOpen.value = true
}

function closeEdit() {
  if (updateLoading.value) return
  editOpen.value = false
  editExpertiseOpen.value = false
  editExpertiseSearch.value = ''
  editNameFocused.value = false
  editNameLimitError.value = ''
  editBioFocused.value = false
  editBioLimitError.value = ''
}

async function onSaveEdit() {
  error.value = ''
  success.value = ''

  if (!editForm.value.id) {
    error.value = 'Missing specialist ID.'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  if (!editForm.value.name.trim()) {
    error.value = 'Please enter a name.'
    showAlertModal({ type: 'error', message: error.value })
    return
  }
  if (!editForm.value.expertiseIds.length) {
    error.value = 'Please select at least one expertise item.'
    showAlertModal({ type: 'error', message: error.value })
    return
  }

  updateLoading.value = true
  try {
    const price = editForm.value.price === '' ? undefined : Number(editForm.value.price)
    const updated = await api.adminUpdateSpecialist(editForm.value.id, {
      name: editForm.value.name.trim(),
      expertiseIds: normalizeExpertiseIds(editForm.value.expertiseIds),
      price: Number.isFinite(price) ? price : undefined,
      bio: editForm.value.bio.trim() || undefined
    })
    void updated
    if (editForm.value.status !== editOriginalStatus.value) {
      const statusUpdated = await api.adminSetSpecialistStatus(editForm.value.id, { status: editForm.value.status })
      void statusUpdated
    }

    await loadSpecialists()
    success.value = `Specialist ${editForm.value.id} updated successfully.`
    showAlertModal({ type: 'success', message: success.value, onClose: () => closeEdit() })
  } catch (e) {
    error.value = e?.message || 'Failed to update specialist'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    updateLoading.value = false
  }
}

async function onDelete(row) {
  const specialistId = row?.id != null ? String(row.id) : ''
  if (!specialistId) {
    error.value = 'This specialist is missing an ID and cannot be deleted.'
    showAlertModal({ type: 'error', message: error.value })
    return
  }

  const confirmed = window.confirm(`Delete specialist "${specialistId}"?`)
  if (!confirmed) return

  error.value = ''
  success.value = ''
  deletingId.value = specialistId

  try {
    await api.adminDeleteSpecialist(specialistId)
    await loadSpecialists()
    success.value = `Specialist ${specialistId} deleted successfully.`
    showAlertModal({ type: 'success', message: success.value })
    if (editOpen.value && editForm.value.id === specialistId) {
      closeEdit()
    }
    if (!page.value?.items?.length) {
      searchQuery.value = ''
    }
  } catch (e) {
    error.value = e?.message || `Failed to delete specialist ${specialistId}.`
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    deletingId.value = ''
  }
}

onMounted(async () => {
  document.addEventListener('click', handleGlobalClick)
  await loadExpertise()
  await loadSpecialists()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleGlobalClick)
})

watch(
    () => form.value.name,
    (val) => {
      if (val.length > NAME_MAX) {
        form.value.name = val.slice(0, NAME_MAX)
        nameLimitError.value = `Maximum ${NAME_MAX} characters allowed.`
        return
      }

      if (val.length < NAME_MAX) {
        nameLimitError.value = ''
      }
    }
)

watch(
    () => form.value.bio,
    (val) => {
      const wordCount = countWords(val)
      if (wordCount > BIO_MAX_WORDS) {
        form.value.bio = trimToWordLimit(val, BIO_MAX_WORDS)
        bioLimitError.value = `Maximum ${BIO_MAX_WORDS} words allowed.`
        return
      }

      if (wordCount < BIO_MAX_WORDS) {
        bioLimitError.value = ''
      }
    }
)

watch(
    () => editForm.value.name,
    (val) => {
      if (val.length > NAME_MAX) {
        editForm.value.name = val.slice(0, NAME_MAX)
        editNameLimitError.value = `Maximum ${NAME_MAX} characters allowed.`
        return
      }

      if (val.length < NAME_MAX) {
        editNameLimitError.value = ''
      }
    }
)

watch(
    () => editForm.value.bio,
    (val) => {
      const wordCount = countWords(val)
      if (wordCount > BIO_MAX_WORDS) {
        editForm.value.bio = trimToWordLimit(val, BIO_MAX_WORDS)
        editBioLimitError.value = `Maximum ${BIO_MAX_WORDS} words allowed.`
        return
      }

      if (wordCount < BIO_MAX_WORDS) {
        editBioLimitError.value = ''
      }
    }
)
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Specialist Management</h1>
      <p class="subtitle">
        Create and manage specialist profiles, pricing, and expertise assignments.
      </p>
    </header>

    <section class="calc-card create-card">
      <h2 class="card-title">Create Specialist</h2>

      <div class="create-grid">
        <label class="field">
          <span class="label">Name</span>
          <input
              v-model="form.name"
              class="input"
              type="text"
              placeholder="Enter specialist name"
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
            <p class="limit-helper__count">{{ form.name.length }}/{{ NAME_MAX }}</p>
          </div>
        </label>

        <label class="field">
          <span class="label">User Email</span>
          <input
              v-model.trim="form.userEmail"
              class="input"
              type="email"
              placeholder="Enter user email"
          />
        </label>

        <label class="field">
          <span class="label">Initial Password (optional)</span>
          <input
              v-model="form.password"
              class="input"
              type="password"
              placeholder="Required only if this email is not registered"
          />
        </label>

        <label class="field">
          <span class="label">Default Price (optional)</span>
          <input
              v-model="form.price"
              class="input"
              type="number"
              min="0"
              step="1"
              placeholder="Enter price"
          />
        </label>

        <label class="field field--full">
          <span class="label">Bio (optional)</span>
          <textarea
              v-model="form.bio"
              class="input input--area"
              rows="3"
              placeholder="Add specialist bio"
              @focus="onBioFocus"
              @blur="onBioBlur"
          />

          <div
              v-if="showBioHelper"
              class="limit-helper"
              :class="{ 'limit-helper--error': bioLimitError }"
              role="status"
              aria-live="polite"
          >
            <p class="limit-helper__text">{{ bioLimitError || `Maximum ${BIO_MAX_WORDS} words` }}</p>
            <p class="limit-helper__count">{{ bioWordCount }}/{{ BIO_MAX_WORDS }} words</p>
          </div>
        </label>
      </div>

      <div ref="expertiseFieldRef" class="field expertise-field field--full">
        <span class="label">Expertise</span>
        <button
            type="button"
            class="multi-trigger"
            :class="{ 'multi-trigger--open': expertiseOpen }"
            @click="toggleExpertisePicker"
        >
          <span>{{ expertisePlaceholder }}</span>
          <span class="multi-trigger__arrow">{{ expertiseOpen ? '^' : 'v' }}</span>
        </button>

        <div v-if="expertiseOpen" class="multi-panel">
          <input
              v-model.trim="expertiseSearch"
              class="input input--search"
              type="text"
              placeholder="Search expertise"
          />

          <div class="multi-options">
            <button
                v-for="item in filteredExpertiseOptions"
                :key="item.id"
                type="button"
                class="multi-option"
                :class="{ 'multi-option--active': form.expertiseIds.includes(String(item.id)) }"
                @click="toggleExpertise(item.id)"
            >
              <span>{{ item.name }}</span>
              <span class="multi-option__state">
                {{ form.expertiseIds.includes(String(item.id)) ? 'Selected' : 'Select' }}
              </span>
            </button>

            <div v-if="!filteredExpertiseOptions.length" class="multi-empty">
              No expertise matched your search.
            </div>
          </div>
        </div>

        <div v-if="selectedExpertise.length" class="selected-chips">
          <button
              v-for="item in selectedExpertise"
              :key="item.id"
              type="button"
              class="selected-chip"
              @click="removeExpertise(item.id)"
          >
            <span>{{ item.name }}</span>
            <span class="selected-chip__remove">x</span>
          </button>
        </div>
      </div>

      <button type="button" class="btn-primary btn-primary--fit" :disabled="creating" @click="onCreate">
        {{ creating ? 'Creating...' : 'Create' }}
      </button>
    </section>

    <section class="calc-card list-card">
      <div class="list-toolbar">
        <div class="toolbar-title">
          <h2 class="card-title">Specialists</h2>
          <p v-if="searchQuery" class="list-note">
            Matched: {{ filteredSpecialists.length }}
          </p>
        </div>
        <div class="toolbar-actions">
          <input
              v-model.trim="searchQuery"
              class="input search-input"
              type="text"
              placeholder="Search specialist by name"
              aria-label="Search specialist by name"
          />
          <button type="button" class="btn-neutral btn-refresh" :disabled="loading" @click="loadSpecialists">
            {{ loading ? 'Loading...' : 'Refresh' }}
          </button>
        </div>
      </div>

      <div class="toolbar-meta">
        <span class="meta-pill">{{ specialistCountLabel }}</span>
      </div>

      <div v-if="loading && !(page.items || []).length" class="state muted">Loading...</div>

      <div v-else-if="!filteredSpecialists.length" class="state muted">
        {{ searchQuery ? 'No specialist matched your search.' : 'No specialist data.' }}
      </div>

      <div v-else class="table-wrap">
        <table class="table">
          <thead>
          <tr>
            <th scope="col" class="th-id">ID</th>
            <th scope="col">Name</th>
            <th scope="col">Price</th>
            <th scope="col">Expertise</th>
            <th scope="col">Status</th>
            <th scope="col" class="th-actions">Actions</th>
          </tr>
          </thead>

          <tbody>
          <tr v-for="s in filteredSpecialists" :key="s.id">
            <td class="mono weak">{{ s.id ?? '--' }}</td>
            <td class="name-cell">{{ s.name ?? '--' }}</td>
            <td>{{ formatPrice(s.price) }}</td>
            <td>
              <div v-if="specialistExpertiseNames(s).length" class="expertise-summary">
                  <span
                      v-for="name in specialistExpertiseNames(s).slice(0, 2)"
                      :key="name"
                      class="summary-chip"
                  >
                    {{ name }}
                  </span>
                <span
                    v-if="specialistExpertiseNames(s).length > 2"
                    class="summary-chip summary-chip--more"
                >
                    +{{ specialistExpertiseNames(s).length - 2 }}
                  </span>
              </div>
              <span v-else class="muted">--</span>
            </td>
            <td>
              <span :class="specialistStatusClass(s)">{{ specialistStatus(s) }}</span>
            </td>
            <td>
              <div class="row-actions">
                <button type="button" class="action-btn" :disabled="updateLoading || deletingId === `${s.id}`" @click="openEdit(s)">
                  Edit
                </button>
                <button
                    type="button"
                    class="action-btn action-btn--danger"
                    :disabled="updateLoading || deletingId === `${s.id}`"
                    @click="onDelete(s)"
                >
                  {{ deletingId === `${s.id}` ? 'Deleting...' : 'Delete' }}
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
          <h3 class="modal-title">Edit Specialist</h3>
        </div>

        <div class="detail-list">
          <div class="detail-row">
            <span class="detail-key">Specialist ID</span>
            <span class="detail-value mono">{{ editForm.id || '--' }}</span>
          </div>
        </div>

        <div class="create-grid">
          <label class="field">
            <span class="label">Name</span>
            <input
                v-model="editForm.name"
                class="input"
                type="text"
                placeholder="Enter specialist name"
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
              <p class="limit-helper__count">{{ editForm.name.length }}/{{ NAME_MAX }}</p>
            </div>
          </label>

          <label class="field">
            <span class="label">Default Price</span>
            <input
                v-model="editForm.price"
                class="input"
                type="number"
                min="0"
                step="1"
                placeholder="Enter price"
            />
          </label>

          <label class="field field--full">
            <span class="label">Bio</span>
            <textarea
                v-model="editForm.bio"
                class="input input--area"
                rows="3"
                placeholder="Update specialist bio"
                @focus="onEditBioFocus"
                @blur="onEditBioBlur"
            />

            <div
                v-if="showEditBioHelper"
                class="limit-helper"
                :class="{ 'limit-helper--error': editBioLimitError }"
                role="status"
                aria-live="polite"
            >
              <p class="limit-helper__text">{{ editBioLimitError || `Maximum ${BIO_MAX_WORDS} words` }}</p>
              <p class="limit-helper__count">{{ editBioWordCount }}/{{ BIO_MAX_WORDS }} words</p>
            </div>
          </label>
        </div>

        <div ref="editExpertiseFieldRef" class="field expertise-field">
          <span class="label">Expertise</span>
          <button
              type="button"
              class="multi-trigger"
              :class="{ 'multi-trigger--open': editExpertiseOpen }"
              @click="toggleEditExpertisePicker"
          >
            <span>{{ editExpertisePlaceholder }}</span>
            <span class="multi-trigger__arrow">{{ editExpertiseOpen ? '^' : 'v' }}</span>
          </button>

          <div v-if="editExpertiseOpen" class="multi-panel">
            <input
                v-model.trim="editExpertiseSearch"
                class="input input--search"
                type="text"
                placeholder="Search expertise"
            />

            <div class="multi-options">
              <button
                  v-for="item in filteredEditExpertiseOptions"
                  :key="item.id"
                  type="button"
                  class="multi-option"
                  :class="{ 'multi-option--active': editForm.expertiseIds.includes(String(item.id)) }"
                  @click="toggleEditExpertise(item.id)"
              >
                <span>{{ item.name }}</span>
                <span class="multi-option__state">
                  {{ editForm.expertiseIds.includes(String(item.id)) ? 'Selected' : 'Select' }}
                </span>
              </button>

              <div v-if="!filteredEditExpertiseOptions.length" class="multi-empty">
                No expertise matched your search.
              </div>
            </div>
          </div>

          <div v-if="selectedEditExpertise.length" class="selected-chips">
            <button
                v-for="item in selectedEditExpertise"
                :key="item.id"
                type="button"
                class="selected-chip"
                @click="removeEditExpertise(item.id)"
            >
              <span>{{ item.name }}</span>
              <span class="selected-chip__remove">x</span>
            </button>
          </div>
        </div>

        <div class="field">
          <span class="label">Status</span>
          <div class="option-row">
            <button
                type="button"
                class="option-btn option-btn--positive"
                :class="{ 'option-btn--active-positive': editForm.status === 'Active' }"
                @click="editForm.status = 'Active'"
            >
              Active
            </button>
            <button
                type="button"
                class="option-btn option-btn--negative"
                :class="{ 'option-btn--active-negative': editForm.status === 'Inactive' }"
                @click="editForm.status = 'Inactive'"
            >
              Inactive
            </button>
          </div>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn-neutral modal-cancel" :disabled="updateLoading" @click="closeEdit">
            Cancel
          </button>
          <button
              type="button"
              class="btn-primary btn-primary--fit modal-save"
              :disabled="updateLoading"
              @click="onSaveEdit"
          >
            {{ updateLoading ? 'Saving...' : 'Save Changes' }}
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

.create-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}

.field {
  display: grid;
  gap: 8px;
  margin-bottom: 14px;
}

.field--full {
  grid-column: 1 / -1;
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
  width: 100%;
}

.input::placeholder {
  color: #6b7280;
}

.input--area {
  min-height: 102px;
  height: auto;
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
  max-width: 220px;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
  margin-bottom: 14px;
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

.expertise-field {
  position: relative;
}

.multi-trigger {
  height: 44px;
  width: 100%;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #111827;
  border-radius: 0;
  padding: 0 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  cursor: pointer;
}

.multi-trigger--open {
  border-color: #111827;
}

.multi-trigger__arrow {
  font-size: 11px;
  color: #6b7280;
}

.multi-panel {
  margin-top: 8px;
  border: 1px solid #d8d1cb;
  background: #ffffff;
  border-radius: 0;
  padding: 10px;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}

.input--search {
  margin-bottom: 8px;
}

.multi-options {
  max-height: 220px;
  overflow: auto;
  display: grid;
  gap: 6px;
}

.multi-option {
  height: 36px;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #111827;
  border-radius: 0;
  padding: 0 10px;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
}

.multi-option--active {
  border-color: #111827;
  background: #111827;
  color: #ffffff;
}

.multi-option__state {
  font-size: 11px;
  opacity: 0.8;
}

.multi-empty {
  border: 1px dashed #d1d5db;
  background: #fafafa;
  color: #6b7280;
  padding: 10px;
  font-size: 12px;
}

.selected-chips {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-chip {
  height: 30px;
  border: 1px solid #111827;
  background: #111827;
  color: #ffffff;
  border-radius: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  font-size: 12px;
  cursor: pointer;
}

.selected-chip__remove {
  font-size: 14px;
  line-height: 1;
}

.option-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-btn {
  min-width: 108px;
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

.option-btn--positive {
  border-color: rgba(34, 197, 94, 0.32);
  background: rgba(34, 197, 94, 0.08);
  color: #166534;
}

.option-btn--negative {
  border-color: rgba(248, 113, 113, 0.38);
  background: rgba(248, 113, 113, 0.12);
  color: #991b1b;
}

.option-btn--active-positive {
  border-color: #166534;
  background: #166534;
  color: #ffffff;
}

.option-btn--active-negative {
  border-color: #991b1b;
  background: #991b1b;
  color: #ffffff;
}

.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}

.toolbar-title .list-note {
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

.list-note {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
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
  width: 180px;
}

.th-actions {
  width: 260px;
}

.mono {
  font-family: ui-monospace, monospace;
}

.weak {
  font-size: 12px;
  color: #6b7280;
}

.name-cell {
  font-weight: 600;
}

.expertise-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.summary-chip {
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #374151;
  font-size: 12px;
  padding: 2px 8px;
}

.summary-chip--more {
  border-color: #111827;
  color: #111827;
  font-weight: 600;
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
  width: min(100%, 680px);
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  overscroll-behavior: contain;
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

@media (max-width: 980px) {
  .create-grid {
    grid-template-columns: 1fr;
  }

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

  .modal-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
