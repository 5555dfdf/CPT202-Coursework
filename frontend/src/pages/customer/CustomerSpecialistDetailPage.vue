<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'

const props = defineProps({
  id: { type: String, required: true }
})

const router = useRouter()
const specialist = ref(null)
const loading = ref(false)
const error = ref('')

const expertiseLabel = computed(() => {
  const ex = specialist.value?.expertise
  if (Array.isArray(ex) && ex.length) return ex.map((e) => e.name ?? e.id).join(', ')
  return '—'
})

async function loadSpecialist() {
  error.value = ''
  loading.value = true
  specialist.value = null
  try {
    specialist.value = await api.getSpecialist(props.id)
  } catch (e) {
    error.value = e?.message || 'Failed to load specialist'
  } finally {
    loading.value = false
  }
}

function goToBooking() {
  router.push({ name: 'customer.specialistSlots', params: { id: props.id } })
}

watch(
    () => props.id,
    () => loadSpecialist(),
    { immediate: true }
)
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Specialist Details</h1>
      <p class="subtitle">Review profile, expertise, and reference pricing before booking.</p>
    </header>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>
    <div v-else-if="loading" class="card muted">Loading…</div>

    <template v-else-if="specialist">
      <div class="card">
        <div class="title">{{ specialist.name ?? '—' }}</div>
        <p class="bio">{{ specialist.bio ?? 'No bio available.' }}</p>
        <div class="meta-list">
          <div class="meta-item">
            <span class="meta-key">Expertise</span>
            <span class="meta-value">{{ expertiseLabel }}</span>
          </div>
          <div v-if="specialist.price != null" class="meta-item">
            <span class="meta-key">Reference Price</span>
            <span class="meta-value">￥{{ specialist.price }}</span>
          </div>
        </div>
      </div>

      <button type="button" class="btn-book" @click="goToBooking">
        Book Now
      </button>
    </template>
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
  color: #5b6472;
  font-size: 14px;
}
.muted {
  opacity: 0.8;
}
.small {
  font-size: 12px;
}
.mono {
  font-family: ui-monospace, monospace;
  font-size: 13px;
}
.card {
  margin-top: 14px;
  padding: 16px;
  border: 1px solid #e6e8ef;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.04);
}
.title {
  font-weight: 700;
  font-size: 20px;
  margin-bottom: 8px;
}
.bio {
  margin: 0 0 8px;
  line-height: 1.5;
  color: #334155;
}
.meta-list {
  margin-top: 12px;
  display: grid;
  gap: 8px;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.meta-key {
  font-size: 12px;
  font-weight: 700;
  color: #4b5563;
  background: #f1f5f9;
  border: 1px solid #dbe2ea;
  border-radius: 999px;
  padding: 3px 10px;
}
.meta-value {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
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

.btn-book {
  width: 100%;
  margin-top: 20px;
  padding: 12px 18px;
  border-radius: 10px;
  border: none;
  background: #07c160;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}
.btn-book:hover {
  background: #06ad56;
}
</style>
