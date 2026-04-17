<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import { formatReferencePrice } from '@/ui/referencePrice'

const expertiseList = ref([])
const page = ref({ items: [], total: 0, page: 1, pageSize: 10 })
const expertiseId = ref('')
const keyword = ref('')
const query = ref({ page: 1, pageSize: 10 })
const loading = ref(false)
const error = ref('')
const router = useRouter()

const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailSpecialist = ref(null)

const expertiseMap = computed(() => {
  const map = new Map()
  for (const item of expertiseList.value || []) {
    const id = String(item?.id ?? '').trim()
    if (!id) continue
    map.set(id, String(item?.name ?? id))
  }
  return map
})

const filteredItems = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  const items = page.value.items ?? []
  if (!q) return items
  return items.filter((s) => {
    const name = (s.name ?? '').toLowerCase()
    const ids = (s.expertiseIds ?? []).join(' ').toLowerCase()
    return name.includes(q) || ids.includes(q)
  })
})

function specialistExpertiseItems(s) {
  const ids = Array.isArray(s?.expertiseIds) ? s.expertiseIds.map((id) => String(id)) : []
  if (!ids.length) return ['--']
  return ids.map((id) => expertiseMap.value.get(id) || id)
}

const detailExpertiseLabel = computed(() => {
  const ex = detailSpecialist.value?.expertise
  if (Array.isArray(ex) && ex.length) return ex.map((e) => e.name ?? e.id).join(', ')
  return '--'
})

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
    const params = {
      page: query.value.page,
      pageSize: query.value.pageSize
    }
    if (expertiseId.value) params.expertiseId = expertiseId.value
    page.value = await api.listSpecialists(params)
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    page.value = { items: [], total: 0, page: 1, pageSize: 10 }
  } finally {
    loading.value = false
  }
}


function totalPages() {
  const total = Number(page.value.total || 0)
  const size = Number(page.value.pageSize || query.value.pageSize || 10)
  return Math.max(1, Math.ceil(total / size))
}

function prevPage() {
  if (loading.value || query.value.page <= 1) return
  query.value.page -= 1
  loadSpecialists()
}

function nextPage() {
  if (loading.value || query.value.page >= totalPages()) return
  query.value.page += 1
  loadSpecialists()
}

function onPageSizeChange() {
  query.value.page = 1
  loadSpecialists()
}

async function openDetail(id) {
  if (!id) return
  detailOpen.value = true
  detailLoading.value = true
  detailError.value = ''
  detailSpecialist.value = null
  try {
    detailSpecialist.value = await api.getSpecialist(id)
  } catch (e) {
    detailError.value = e?.message || 'Failed to load specialist details'
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  if (detailLoading.value) return
  detailOpen.value = false
  detailError.value = ''
  detailSpecialist.value = null
}

function goToBookingFromDetail() {
  const id = detailSpecialist.value?.id
  if (!id) return
  closeDetail()
  router.push({ name: 'customer.specialistSlots', params: { id } })
}
onMounted(async () => {
  await loadExpertise()
  await loadSpecialists()
})

watch(expertiseId, () => {
  query.value.page = 1
  loadSpecialists()
})
watch(keyword, () => {
  query.value.page = 1
})
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Specialists</h1>
      <p class="subtitle">Find the right specialist and view available booking details.</p>
    </header>

    <div class="panel">
      <label class="field">
        <div class="label">Filter by Expertise</div>
        <select v-model="expertiseId" class="input">
          <option value="">All</option>
          <option v-for="e in expertiseList" :key="e.id" :value="e.id">
            {{ e.name }}
          </option>
        </select>
      </label>
      <label class="field">
        <div class="label">Keyword</div>
        <input
          v-model="keyword"
          class="input"
          type="text"
          maxlength="100"
          placeholder="Name or expertise ID..."
        />
      </label>
      <button type="button" class="btn" :disabled="loading" @click="loadSpecialists">Refresh</button>
    </div>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>

    <div v-if="loading && !(page.items || []).length" class="card muted">Loading...</div>

    <div v-else-if="!filteredItems.length && !error" class="empty">
      <div class="empty__title">No specialists found</div>
      <p class="muted">Try changing filters or wait for admin updates.</p>
    </div>

    <ul v-else class="list">
      <li v-for="s in filteredItems" :key="s.id" class="card card--row">
        <div class="card-top">
          <div class="card-main">
            <div class="name">{{ s.name ?? '--' }}</div>
            <div class="meta-line">
              <span
                v-for="item in specialistExpertiseItems(s)"
                :key="`${s.id}-${item}`"
                class="meta-chip"
              >
                {{ item }}
              </span>
            </div>
            <div v-if="s.price != null" class="meta-line meta-line--below">
              <span class="price-text">Reference Price: {{ formatReferencePrice(s.price, s.currency) }}</span>
            </div>
          </div>
          <div class="card-actions">
            <button type="button" class="link" @click="openDetail(s.id)">
              View Details
            </button>
          </div>
        </div>
      </li>
    </ul>

    <div v-if="(page.items || []).length" class="pager">
      <button type="button" class="btn btn--ghost" :disabled="loading || query.page <= 1" @click="prevPage">
        Prev
      </button>
      <span class="pager__info">Page {{ page.page }} / {{ totalPages() }} · Total {{ page.total }}</span>
      <button type="button" class="btn btn--ghost" :disabled="loading || query.page >= totalPages()" @click="nextPage">
        Next
      </button>
      <label class="pager-size">
        <span>Page Size</span>
        <select v-model.number="query.pageSize" class="pager-size__select" @change="onPageSizeChange">
          <option :value="10">10</option>
          <option :value="20">20</option>
        </select>
      </label>
    </div>

    <div v-if="detailOpen" class="modal-backdrop" @click.self="closeDetail">
      <section class="modal-card">
        <h3 class="modal-title">Specialist Details</h3>

        <p v-if="detailError" class="banner banner--error">{{ detailError }}</p>
        <p v-else-if="detailLoading" class="muted">Loading...</p>

        <template v-else-if="detailSpecialist">
          <div class="detail-list">
            <div class="detail-row">
              <span class="detail-key">Name</span>
              <span class="detail-value">{{ detailSpecialist.name ?? '--' }}</span>
            </div>
            <div class="detail-row detail-row--block">
              <span class="detail-key">Bio</span>
              <p class="detail-bio">{{ detailSpecialist.bio ?? 'No bio available.' }}</p>
            </div>
            <div class="detail-row detail-row--block">
              <span class="detail-key">Expertise</span>
              <p class="detail-value">{{ detailExpertiseLabel }}</p>
            </div>
            <div v-if="detailSpecialist.price != null" class="detail-row">
              <span class="detail-key">Reference Price</span>
              <span class="detail-value">{{ formatReferencePrice(detailSpecialist.price, detailSpecialist.currency) }}</span>
            </div>
          </div>
        </template>

        <div class="modal-footer">
          <button type="button" class="btn btn--ghost modal-btn" :disabled="detailLoading" @click="closeDetail">
            Cancel
          </button>
          <button type="button" class="btn modal-btn" :disabled="detailLoading || !detailSpecialist?.id" @click="goToBookingFromDetail">
            Book Now
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
  font-size: 14px;
  color: #4b5563;
}

.panel {
  margin-top: 10px;
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 12px;
  align-items: end;
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
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
  width: 100%;
  height: 44px;
  padding: 0 12px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #111827;
  outline: none;
  transition: border-color 0.18s ease;
}

.input:focus {
  border-color: #202124;
}

select.input {
  cursor: pointer;
}
.btn {
  padding: 0 16px;
  border-radius: 0;
  border: 1px solid #D9533C;
  background: #D9533C;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
  height: 44px;
  transition: opacity 0.18s ease;
}

.btn:hover:not(:disabled) {
  opacity: 0.92;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn--ghost {
  border: 1px solid #202124;
  background: #ffffff;
  color: #202124;
}
.muted {
  color: #6b7280;
}
.small {
  font-size: 12px;
  margin-top: 4px;
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
.empty {
  margin-top: 16px;
  padding: 18px;
  border: 1px dashed #d1d5db;
  border-radius: 0;
  background: #fafafa;
}
.empty__title {
  font-weight: 700;
  margin-bottom: 6px;
}
.list {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.card {
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}
.card--row {
  display: block;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  gap: 16px;
}

.card-main {
  min-width: 0;
  flex: 1;
}

.card-actions {
  display: flex;
  align-items: flex-end;
}

.name {
  font-weight: 700;
  font-size: 17px;
  color: #111827;
}

.meta-line {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-line--below {
  margin-top: 6px;
}

.price-text {
  font-size: 13px;
  color: #374151;
  font-weight: 600;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  font-size: 12px;
  color: #374151;
}
.mono {
  font-family: ui-monospace, monospace;
}
.link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  width: 170px;
  height: 40px;
  padding: 0 12px;
  border-radius: 0;
  border: 1px solid #D9533C;
  background: #D9533C;
  color: #ffffff;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
  transition: opacity 0.18s ease;
  cursor: pointer;
}

.link:hover {
  opacity: 0.92;
}

.pager {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.pager-size {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #4b5563;
  font-weight: 600;
}

.pager-size__select {
  height: 40px;
  min-width: 88px;
  padding: 0 10px;
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #f8f5f2;
  color: #111827;
}

.pager__info {
  font-size: 13px;
  color: #475569;
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
  width: min(100%, 760px);
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

.detail-list {
  display: grid;
  gap: 12px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  border-bottom: 1px solid #eceff3;
  padding-bottom: 10px;
}

.detail-row--block {
  display: grid;
  justify-content: initial;
  gap: 6px;
}

.detail-key {
  color: #6b7280;
  font-size: 13px;
  font-weight: 600;
}

.detail-value {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  text-align: right;
}

.detail-bio {
  margin: 0;
  color: #334155;
  line-height: 1.5;
}

.modal-footer {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.modal-btn {
  min-width: 140px;
}
@media (max-width: 720px) {
  .list {
    grid-template-columns: 1fr;
  }

  .panel {
    grid-template-columns: 1fr;
  }

  .card--row {
    display: block;
  }

  .card-top {
    flex-direction: column;
    align-items: stretch;
  }

  .card-actions {
    align-items: stretch;
  }

  .modal-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .modal-btn {
    width: 100%;
  }

  .link {
    width: 100%;
  }

  .pager {
    flex-wrap: wrap;
  }
}
</style>


