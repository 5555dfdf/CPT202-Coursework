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
      <p class="muted mono">specialistId: {{ id }}</p>
    </header>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>
    <div v-else-if="loading" class="card muted">Loading…</div>

    <template v-else-if="specialist">
      <div class="card">
        <div class="title">{{ specialist.name ?? '—' }}</div>
        <p class="bio">{{ specialist.bio ?? 'No bio available.' }}</p>
        <p class="muted small">Expertise: {{ expertiseLabel }}</p>
        <p v-if="specialist.price != null" class="muted small">Reference Price: {{ specialist.price }}</p>
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
  font-size: 22px;
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
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
}
.title {
  font-weight: 700;
  margin-bottom: 8px;
}
.bio {
  margin: 0 0 8px;
  line-height: 1.5;
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
  opacity: 0.9;
}
</style>
