<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { api } from '@/api/client'

const expertiseList = ref([])
const page = ref({ items: [], total: 0, page: 1, pageSize: 10 })
const expertiseId = ref('')
const keyword = ref('')
const loading = ref(false)
const error = ref('')

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

function specialistExpertiseLabel(s) {
  const ids = Array.isArray(s?.expertiseIds) ? s.expertiseIds.map((id) => String(id)) : []
  if (!ids.length) return '—'
  return ids.map((id) => expertiseMap.value.get(id) || id).join(', ')
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
    const params = {}
    if (expertiseId.value) params.expertiseId = expertiseId.value
    page.value = await api.listSpecialists(params)
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    page.value = { items: [], total: 0, page: 1, pageSize: 10 }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadExpertise()
  await loadSpecialists()
})

watch(expertiseId, () => loadSpecialists())
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
        <input v-model="keyword" class="input" placeholder="Name or expertise ID..." />
      </label>
      <button type="button" class="btn" :disabled="loading" @click="loadSpecialists">Refresh</button>
    </div>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>

    <div v-if="loading && !(page.items || []).length" class="card muted">Loading…</div>

    <div v-else-if="!filteredItems.length && !error" class="empty">
      <div class="empty__title">No specialists found</div>
      <p class="muted">Try changing filters or wait for admin updates.</p>
    </div>

    <ul v-else class="list">
      <li v-for="s in filteredItems" :key="s.id" class="card card--row">
        <div class="card-main">
          <div class="name">{{ s.name ?? '—' }}</div>
          <div class="meta-line">
            <span class="meta-chip">Expertise: {{ specialistExpertiseLabel(s) }}</span>
            <span v-if="s.price != null" class="meta-chip">Reference Price: {{ s.price }}</span>
          </div>
        </div>
        <RouterLink class="link" :to="{ name: 'customer.specialistDetail', params: { id: s.id } }">
          View Details
        </RouterLink>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.page__header h1 {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 800;
}

.subtitle {
  margin: 0;
  font-size: 14px;
  color: #5b6472;
}

.panel {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 12px;
  align-items: end;
  padding: 16px;
  border: 1px solid #e6e8ef;
  border-radius: 14px;
  background: #f8fafc;
}
.field {
  display: grid;
  gap: 6px;
}
.label {
  font-size: 13px;
  color: #4b5563;
  font-weight: 600;
}
.input {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d3d8e1;
  background: #ffffff;
  color: #111827;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

select.input {
  cursor: pointer;
}
.btn {
  padding: 10px 18px;
  border-radius: 10px;
  border: 1px solid #07c160;
  background: #07c160;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
  height: 42px;
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.btn:hover:not(:disabled) {
  opacity: 1;
  background: #06ad56;
  border-color: #06ad56;
  transform: translateY(-1px);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.muted {
  opacity: 0.8;
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
  border: 1px dashed #cfd5df;
  border-radius: 14px;
  background: #fbfcfe;
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
  gap: 10px;
}
.card {
  padding: 16px;
  border: 1px solid #e6e8ef;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.04);
}
.card--row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.card-main {
  min-width: 0;
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

.meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid #d7dde8;
  background: #f8fafc;
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
  min-width: 110px;
  height: 38px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid #07c160;
  background: #07c160;
  color: #ffffff;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
  transition: background 0.15s ease, color 0.15s ease;
}

.link:hover {
  background: #06ad56;
  border-color: #06ad56;
  color: #ffffff;
}
@media (max-width: 720px) {
  .panel {
    grid-template-columns: 1fr;
  }

  .card--row {
    flex-direction: column;
    align-items: stretch;
  }

  .link {
    width: 100%;
  }
}
</style>
