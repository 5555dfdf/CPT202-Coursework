<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { api } from "@/api/client";

const pending = ref({ items: [], total: 0 });
const loading = ref(false);
const error = ref("");


// fetches the list of pending booking requests from the API.
async function load() {
  error.value = "";
  loading.value = true;
  try {
    // requesting only "Pending" status bookings with a specific page size
    pending.value = await api.listBookingRequests({
      status: "Pending",
      pageSize: 20,
    });
  } catch (e) {
    // fallback error handling
    error.value = e?.message || "Failed to load";
    pending.value = { items: [], total: 0 };
  } finally {
    loading.value = false;
  }
}
// initial data load when the component is mounted to the DOM
onMounted(load);
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Welcome</h1>
    </header>

    <div v-if="error" class="banner banner--error" role="alert">
      {{ error }}
    </div>

    <section class="summary-card">
      <div class="summary-card__title">Request Overview</div>
      <div class="summary-grid summary-grid--single">
        <div class="metric-item">
          <div class="metric-main">
            <div class="num">
              {{
                loading ? "…" : (pending.total ?? (pending.items || []).length)
              }}
            </div>
            <div class="label">Pending Requests</div>
          </div>
          <div class="metric-action">
            <RouterLink class="link" :to="{ name: 'specialist.requests' }"
              >Go to Requests</RouterLink
            >
          </div>
        </div>
      </div>
    </section>

    <ul v-if="(pending.items || []).length" class="preview">
      <li
        v-for="b in pending.items.slice(0, 5)"
        :key="b.id"
        class="preview__row"
      >
        <span class="mono">{{ b.id }}</span>
        <span class="muted small">{{
          b.customerName ?? b.customerId ?? "—"
        }}</span>
        <span class="small">{{ b.time ?? b.startTime }}</span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.page__header {
  /* Page Layout */
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
  margin-bottom: 8px;
  font-size: 16px;
}
/* Flex/Grid layout for metrics */
.num {
  font-size: clamp(44px, 5vw, 56px);
  font-weight: 800;
  margin: 0;
  line-height: 1;
}
/* Action Button/Link */
.link {
  display: inline-block;
  padding: 8px 14px;
  border-radius: 0;
  border: 1px solid #000000;
  background: #000000;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
}
/* Statistic Card Styles */
.summary-card {
  margin-top: 8px;
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 0;
  padding: 16px;
  box-shadow: 0 6px 16px rgba(17, 24, 39, 0.06);
}

.summary-card__title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
}

.summary-grid--single {
  grid-template-columns: 1fr;
}

.metric-item {
  padding: 14px 18px 12px;
  min-height: 132px;
  display: flex;
  flex-direction: column;
}

.metric-main {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 68%;
  min-width: 220px;
  max-width: 100%;
}

.label {
  font-size: 14px;
  opacity: 0.85;
  font-weight: 600;
  line-height: 1.25;
  flex: 1;
}

.metric-action {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  padding-right: 14px;
}
/* Preview List Rows */
.preview {
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}
.preview__row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 0;
  border: 1px solid #eceff3;
  background: #fafafa;
  font-size: 13px;
}
.mono {
  font-family: ui-monospace, monospace;
}
/* Feedback Banners */
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
/* Responsive adjustments for smaller screens */
@media (max-width: 640px) {
  .preview__row {
    grid-template-columns: 1fr;
  }
}
</style>
