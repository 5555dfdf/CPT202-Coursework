<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import { formatReferencePrice } from '@/ui/referencePrice'

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
            <span class="meta-value">{{ formatReferencePrice(specialist.price, specialist.currency) }}</span>
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
.muted {
  color: #6b7280;
}
.small {
  font-size: 12px;
}
.mono {
  font-family: ui-monospace, monospace;
  font-size: 13px;
}
.card {
  margin-top: 10px;
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}
.title {
  font-weight: 700;
  font-size: 18px;
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
  color: #374151;
  background: #f8f5f2;
  border: 1px solid #d8d1cb;
  border-radius: 0;
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
  height: 44px;
  padding: 0 18px;
  border-radius: 0;
  border: 1px solid #D9533C;
  background: #D9533C;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}
.btn-book:hover {
  opacity: 0.92;
}
</style>

