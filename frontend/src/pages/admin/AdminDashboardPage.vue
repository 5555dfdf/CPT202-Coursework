<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api } from '@/api/client'
import { BadgeDollarSign, CalendarCheck, ChevronRight, Clock3 } from '@lucide/vue'
import { showAlertModal } from '@/ui/alertModal'

const expertiseCount = ref(0)
const specialistTotal = ref(0)
const loading = ref(false)
const error = ref('')

async function load() {
  error.value = ''
  loading.value = true
  try {
    const [ex, sp] = await Promise.all([api.listExpertise(), api.listSpecialists({ pageSize: 1 })])
    expertiseCount.value = ex.length
    specialistTotal.value = sp.total ?? (sp.items || []).length
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Welcome</h1>
    </header>

    <section class="summary-card">
      <div class="summary-card__title">Resource Overview</div>
      <div class="summary-grid">
        <div class="metric-item">
          <div class="metric-main">
            <div class="num">{{ loading ? '…' : expertiseCount }}</div>
            <div class="label">Expertise Categories</div>
          </div>
          <div class="metric-action">
            <RouterLink class="link" :to="{ name: 'admin.expertise' }">Manage</RouterLink>
          </div>
        </div>
        <div class="metric-item">
          <div class="metric-main">
            <div class="num">{{ loading ? '…' : specialistTotal }}</div>
            <div class="label">Specialists</div>
          </div>
          <div class="metric-action">
            <RouterLink class="link" :to="{ name: 'admin.specialists' }">Manage</RouterLink>
          </div>
        </div>
      </div>
    </section>

    <div class="card links">
      <div class="title">Quick Links</div>
      <p class="links__subtitle">Shortcuts to common admin actions</p>
      <div class="quick-links-grid">
        <RouterLink class="quick-link-card" :to="{ name: 'admin.slots' }">
          <span class="quick-link-card__main">
            <Clock3 class="quick-link-card__icon" />
            <span>Slots</span>
          </span>
          <ChevronRight class="quick-link-card__chevron" />
        </RouterLink>
        <RouterLink class="quick-link-card" :to="{ name: 'admin.pricing' }">
          <span class="quick-link-card__main">
            <BadgeDollarSign class="quick-link-card__icon" />
            <span>Pricing</span>
          </span>
          <ChevronRight class="quick-link-card__chevron" />
        </RouterLink>
        <RouterLink class="quick-link-card" :to="{ name: 'admin.bookings' }">
          <span class="quick-link-card__main">
            <CalendarCheck class="quick-link-card__icon" />
            <span>Bookings</span>
          </span>
          <ChevronRight class="quick-link-card__chevron" />
        </RouterLink>
      </div>
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
.muted {
  opacity: 0.8;
}
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
.metric-item {
  padding: 14px 18px 12px;
  min-height: 132px;
  display: flex;
  flex-direction: column;
}
.metric-item:first-child {
  border-right: 1px solid rgba(17, 24, 39, 0.12);
}
.metric-main {
  display: flex;
  align-items: center;
  gap: 16px;
  /* Make KPI row the dominant content area of each metric column. */
  width: 68%;
  min-width: 220px;
  max-width: 100%;
}
.card {
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 0;
  background: rgba(255, 255, 255, 0.04);
}
.label {
  font-size: 14px;
  opacity: 0.85;
  font-weight: 600;
  line-height: 1.25;
  flex: 1;
}
.num {
  font-size: clamp(44px, 5vw, 56px);
  font-weight: 800;
  margin: 0;
  line-height: 1;
}
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
.metric-action {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  padding-right: 14px;
}
.links {
  margin-top: 14px;
  background: transparent;
  border: 0;
  padding: 0;
}
.title {
  width: 100%;
  font-weight: 700;
  margin-bottom: 2px;
}
.links__subtitle {
  margin: 0 0 10px;
  color: #4b5563;
  font-size: 13px;
}
.quick-links-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.quick-link-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  text-decoration: none;
  color: #111827;
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 0;
  padding: 12px 14px;
}
.quick-link-card:last-child {
  grid-column: 1 / span 2;
}
.quick-link-card__main {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.quick-link-card__icon {
  width: 16px;
  height: 16px;
}
.quick-link-card__chevron {
  width: 16px;
  height: 16px;
  opacity: 0.72;
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

@media (max-width: 760px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
  .metric-item:first-child {
    border-right: 0;
    border-bottom: 1px solid rgba(17, 24, 39, 0.12);
  }
  .quick-links-grid {
    grid-template-columns: 1fr;
  }
  .quick-link-card:last-child {
    grid-column: auto;
  }
}
</style>
