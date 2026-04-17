<script setup>
import { ref, watch } from "vue";
import { useRouter } from "vue-router";
import { api } from "@/api/client";
import { showConfirmModal } from "@/ui/confirmModal.js";
const props = defineProps({
  id: { type: String, required: true },
});

const router = useRouter();
const booking = ref(null);
const loading = ref(false);
const error = ref("");
const rejectReason = ref("");
const busy = ref("");

function formatPrice(row) {
  const direct = row?.price;
  if (typeof direct === "string" && direct.trim()) return direct.trim();
  const n = Number(row?.amount ?? 0);
  const safe = Number.isNaN(n) ? 0 : n;
  const c = String(row?.currency ?? "CNY").trim() || "CNY";
  return `${safe.toFixed(2)} ${c}`;
}

async function load() {
  error.value = "";
  loading.value = true;
  booking.value = null;
  try {
    booking.value = await api.specialistGetBooking(props.id);
  } catch (e) {
    error.value = e?.message || "Failed to load";
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.id,
  () => load(),
  { immediate: true },
);

function run(action) {
  // 根据不同的动作，设置不同的提示文案
  let title = "";
  let message = "";

  if (action === "confirm") {
    title = "Confirm acceptance";
    message = "Are you sure you want to accept this reservation?";
  } else if (action === "reject") {
    title = "Refuse Reservation";
    message = "Are you sure you want to decline this reservation?";
  } else if (action === "complete") {
    title = "Complete Reservation";
    message =
      "Are you sure that this reservation service has been completed?  \n" +
      "Once confirmed, the status will change to 'Completed'.";
  }

  // 弹出提示框
  showConfirmModal({
    title: title,
    message: message,
    onConfirm: async () => {
      busy.value = action;
      try {
        if (action === "confirm") {
          await api.confirmBooking(props.id);
        } else if (action === "reject") {
          await api.rejectBooking(props.id, {
            reason: rejectReason.value.trim() || undefined,
          });
          rejectReason.value = "";
        } else if (action === "complete") {
          await api.completeBooking(props.id);
        }
        await load();
      } catch (e) {
        error.value = e?.message || "Operation failed";
      } finally {
        busy.value = "";
      }
    },
  });
}
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Booking Details (Specialist)</h1>
    </header>

    <div v-if="error" class="banner banner--error" role="alert">
      {{ error }}
    </div>
    <div v-else-if="loading" class="card muted">Loading…</div>

    <template v-else-if="booking">
      <div class="card">
        <div class="title">Booking Info</div>
        <dl class="kv">
          <dt>Status</dt>
          <dd>{{ booking.status ?? "—" }}</dd>
          <dt>Time</dt>
          <dd>{{ booking.time ?? booking.startTime ?? "—" }}</dd>
          <dt>Customer</dt>
          <dd>{{ booking.customerName ?? booking.customerId ?? "—" }}</dd>
          <dt>Duration</dt>
          <dd>
            {{ booking.duration ?? booking.slot ?? booking.slotId ?? "—" }}
          </dd>
          <dt>Price</dt>
          <dd>{{ formatPrice(booking) }}</dd>
          <dt>Note</dt>
          <dd>{{ booking.note ?? "—" }}</dd>
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
            {{ busy === "confirm" ? "…" : "Confirm" }}
          </button>
          <button
            type="button"
            class="btn btn--danger"
            :disabled="!!busy"
            @click="run('reject')"
          >
            {{ busy === "reject" ? "…" : "Reject" }}
          </button>
          <button
            type="button"
            class="btn"
            :disabled="!!busy"
            @click="run('complete')"
          >
            {{ busy === "complete" ? "…" : "Complete" }}
          </button>
        </div>
      </div>

      <p class="muted small">
        <button
          type="button"
          class="linkish btn-neutral"
          @click="router.push({ name: 'specialist.requests' })"
        >
          Back to Request List
        </button>
      </p>
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
.muted {
  color: #6b7280;
}
.small {
  font-size: 12px;
  margin-top: 12px;
}
.mono {
  font-family: ui-monospace, monospace;
  font-size: 12px;
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
  gap: 8px;
  margin-bottom: 12px;
  max-width: 400px;
}
.label {
  font-size: 13px;
  color: #4b5563;
  font-weight: 600;
}
.input,
.btn {
  height: 44px;
  box-sizing: border-box;
}
.input {
  min-width: 160px;
  padding: 0 12px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #111827;
}
select.input {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  line-height: normal;
}
.btns {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-radius: 0;
  border: 1px solid #a94442;
  background: #a94442;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}
.btn--ok {
  border-color: #202124;
  background: #ffffff;
  color: #202124;
}
.btn--danger {
  border-color: #a94442;
  background: #ffffff;
  color: #a94442;
}
.btn:disabled {
  opacity: 0.6;
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
  padding: 0 14px;
  height: 40px;
  text-decoration: none;
  cursor: pointer;
  font: inherit;
}

.btn-neutral {
  border: 1px solid #202124;
  border-radius: 0;
  background: #ffffff;
  color: #202124;
  font-weight: 700;
}
</style>
