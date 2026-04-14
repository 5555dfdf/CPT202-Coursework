<script setup>
import {onMounted, onUnmounted, ref, watch} from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'

const props = defineProps({
  id: { type: String, required: true }
})

const router = useRouter()
const booking = ref(null)
const loading = ref(false)
const error = ref('')
const rejectReason = ref('')
const busy = ref('')

async function load() {
  error.value = ''
  loading.value = true
  booking.value = null
  try {
    booking.value = await api.getBooking(props.id)
  } catch (e) {
    error.value = e?.message || 'Failed to load'
  } finally {
    loading.value = false
  }
}

watch(
  () => props.id,
  () => load(),
  { immediate: true }
)

async function run(action) {
  busy.value = action
  try {
    if (action === 'confirm') booking.value = await api.confirmBooking(props.id)
    else if (action === 'reject')
      booking.value = await api.rejectBooking(props.id, {
        reason: rejectReason.value.trim() || undefined
      })
    else if (action === 'complete') booking.value = await api.completeBooking(props.id)
  } catch (e) {
    error.value = e?.message || 'Operation failed'
  } finally {
    busy.value = ''
  }
}

// 1. 在顶层声明变量，不要在这里赋值
let autoUpdateTimer = null

async function handleAutoStatusUpdate() {
  try {
    const message = await api.triggerAutoStatusUpdate()
    if (message) {
      console.log(message)
      await load()
    }
  } catch (e) {
    console.error('Auto status update failed:', e?.message)
  }
}

onMounted(() => {
  // 立即执行一次
  handleAutoStatusUpdate()

  // 2. 赋值给顶层变量，去掉 const
  autoUpdateTimer = setInterval(handleAutoStatusUpdate, 3600000)
})

onUnmounted(() => {
  // 3. 现在这里可以正常访问到变量了
  if (autoUpdateTimer) {
    clearInterval(autoUpdateTimer)
    autoUpdateTimer = null // 良好的习惯：清理引用
  }
})
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Booking Details (Specialist)</h1>
    </header>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>
    <div v-else-if="loading" class="card muted">Loading…</div>

    <template v-else-if="booking">
      <div class="card">
        <div class="title">Booking Info</div>
        <dl class="kv">
          <dt>Status</dt>
          <dd>{{ booking.status ?? '—' }}</dd>
          <dt>Time</dt>
          <dd>{{ booking.time ?? booking.startTime ?? '—' }}</dd>
          <dt>Customer</dt>
          <dd>{{ booking.customerName ?? booking.customerId ?? '—' }}</dd>
          <dt>Duration</dt>
          <dd>{{ booking.duration ?? booking.slot ?? booking.slotId ?? '—' }}</dd>
        </dl>
      </div>

      <div class="card">
        <div class="title">Actions</div>
        <label class="field">
          <span class="label">Reject reason (optional)</span>
          <input v-model="rejectReason" class="input" />
        </label>
        <div class="btns">
          <button
            type="button"
            class="btn btn--ok"
            :disabled="!!busy"
            @click="run('confirm')"
          >
            {{ busy === 'confirm' ? '…' : 'Confirm' }}
          </button>
          <button
            type="button"
            class="btn btn--danger"
            :disabled="!!busy"
            @click="run('reject')"
          >
            {{ busy === 'reject' ? '…' : 'Reject' }}
          </button>
          <button type="button" class="btn" :disabled="!!busy" @click="run('complete')">
            {{ busy === 'complete' ? '…' : 'Complete' }}
          </button>
        </div>
      </div>

      <p class="muted small">
        <button type="button" class="linkish" @click="router.push({ name: 'specialist.requests' })">
          Back to Request List
        </button>
      </p>
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
  margin-top: 12px;
}
.mono {
  font-family: ui-monospace, monospace;
  font-size: 12px;
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
  margin-bottom: 10px;
}
.kv {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 8px 12px;
  margin: 0;
  font-size: 14px;
}
.kv dt {
  opacity: 0.75;
  margin: 0;
}
.kv dd {
  margin: 0;
}
.field {
  display: grid;
  gap: 6px;
  margin-bottom: 12px;
  max-width: 400px;
}
.label {
  font-size: 13px;
  opacity: 0.85;
}
.input {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: #ffffff;
  color: #111827;
}
.btns {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.btn {
  padding: 10px 16px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.1);
  color: inherit;
  cursor: pointer;
}
.btn--ok {
  border-color: rgba(52, 211, 153, 0.45);
  background: rgba(52, 211, 153, 0.12);
}
.btn--danger {
  border-color: rgba(248, 113, 113, 0.45);
  background: rgba(248, 113, 113, 0.12);
}
.btn:disabled {
  opacity: 0.5;
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
.linkish {
  background: none;
  border: none;
  padding: 0;
  color: inherit;
  text-decoration: underline;
  cursor: pointer;
  font: inherit;
}
</style>
