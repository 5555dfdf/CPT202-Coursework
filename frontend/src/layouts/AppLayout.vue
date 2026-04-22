<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { showConfirmModal } from '@/ui/confirmModal.js'
import { showAlertModal } from '@/ui/alertModal'
import { api } from '@/api/client'
import {
  BadgeDollarSign,
  CalendarCheck,
  Clock3,
  FolderKanban,
  House,
  UserRound,
  Users
} from '@lucide/vue'

const auth = useAuthStore()
const router = useRouter()
const mobileNavOpen = ref(false)
const unpaidOpen = ref(false)
const unpaidLoading = ref(false)
const unpaidError = ref('')
const unpaidItems = ref([])
const resumeBusyId = ref('')
const paymentPanelOpen = ref(false)
const paymentBusy = ref(false)
const paymentError = ref('')
const paymentPanel = ref({
  paymentIntentId: '',
  paymentId: '',
  specialistId: '',
  slotId: '',
  amount: 0,
  currency: 'CNY',
  qrCodeUrl: ''
})
let unpaidTimer = null

const role = computed(() => auth.user?.role || '')
const logoutButtonClass = computed(() =>
    role.value === 'Customer' ? 'logout-btn--customer' : 'logout-btn--team'
)

const links = computed(() => {
  if (role.value === 'Admin') {
    return [
      { to: '/admin/dashboard', label: 'Overview', icon: House },
      { to: '/admin/specialists', label: 'Specialists', icon: Users },
      { to: '/admin/expertise', label: 'Expertise', icon: FolderKanban },
      { to: '/admin/slots', label: 'Slots', icon: Clock3 },
      { to: '/admin/pricing', label: 'Pricing', icon: BadgeDollarSign },
      { to: '/admin/bookings', label: 'Bookings', icon: CalendarCheck }
    ]
  }
  if (role.value === 'Specialist') {
    return [
      { to: '/specialist/dashboard', label: 'Dashboard', icon: House },
      { to: '/specialist/requests', label: 'Requests', icon: CalendarCheck },
      { to: '/specialist/slots', label: 'Slots', icon: Clock3 },
      { to: '/specialist/schedule', label: 'Schedule', icon: CalendarCheck }
    ]
  }
  if (role.value === 'Customer') {
    return [
      { to: '/customer/specialists', label: 'Specialists', icon: Users },
      { to: '/customer/bookings', label: 'My Bookings', icon: CalendarCheck },
      { to: '/customer/profile', label: 'Profile', icon: UserRound }
    ]
  }
  return []
})

function onLogout() {
  showConfirmModal({
    title: 'Log out',
    message: 'Are you sure you want to log out of the current account?',
    confirmVariant: role.value === 'Customer' ? 'customer' : '',
    onConfirm: async () => {
      await auth.logout()
      await router.replace({ name: 'login' })
    }
  })
}

function formatRemain(seconds) {
  const s = Number(seconds ?? 0)
  if (!Number.isFinite(s) || s <= 0) return 'Expired'
  const mm = Math.floor(s / 60)
  const ss = s % 60
  return `${mm}:${String(ss).padStart(2, '0')}`
}

const countdownMap = ref({})
let countdownTimer = null

function startCountdown() {
  if (countdownTimer) return

  countdownTimer = setInterval(() => {
    const map = countdownMap.value

    Object.keys(map).forEach(id => {
      if (map[id] > 0) {
        map[id]--
      }
    })
  }, 1000)
}

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

async function loadUnpaidPayments() {
  if (role.value !== 'Customer') return
  unpaidLoading.value = true
  unpaidError.value = ''
  try {
    const list = await api.listUnpaidPayments()
    unpaidItems.value = list

    // 初始化 / 更新倒计时
    const map = {}
    list.forEach(item => {
      const id = String(item.paymentIntentId)
      map[id] = Number(item.remainingSeconds ?? 0)
    })
    countdownMap.value = map

  } catch (e) {
    unpaidError.value = e?.message || 'Failed to load unpaid orders'
    unpaidItems.value = []
  } finally {
    unpaidLoading.value = false
  }
}

async function toggleUnpaidPanel() {
  unpaidOpen.value = !unpaidOpen.value
  if (unpaidOpen.value) {
    await loadUnpaidPayments()
  }
}

function startUnpaidPolling() {
  if (unpaidTimer || role.value !== 'Customer') return
  loadUnpaidPayments()
  unpaidTimer = setInterval(() => {
    loadUnpaidPayments()
  }, 30000)
}

function stopUnpaidPolling() {
  if (unpaidTimer) {
    clearInterval(unpaidTimer)
    unpaidTimer = null
  }
}

async function onContinuePay(item) {
  const intentId = String(item?.paymentIntentId ?? '').trim()
  if (!intentId) return
  resumeBusyId.value = intentId
  try {
    const resumed = await api.resumeUnpaidPayment(intentId)
    paymentPanel.value = {
      paymentIntentId: intentId,
      paymentId: String(resumed?.paymentId ?? '').trim(),
      specialistId: String(item?.specialistId ?? '').trim(),
      slotId: String(item?.slotId ?? '').trim(),
      amount: Number(resumed?.amount ?? item?.amount ?? 0) || 0,
      currency: String(resumed?.currency ?? item?.currency ?? 'CNY').trim() || 'CNY',
      qrCodeUrl: String(resumed?.qrCodeUrl ?? '').trim()
    }
    paymentError.value = ''
    paymentPanelOpen.value = true
    await loadUnpaidPayments()
  } catch (e) {
    showAlertModal({ type: 'error', message: e?.message || 'Failed to resume payment' })
  } finally {
    resumeBusyId.value = ''
  }
}

function closePaymentPanel() {
  paymentPanelOpen.value = false
  paymentError.value = ''
}

async function confirmPaymentFromPanel() {
  if (!paymentPanel.value.paymentIntentId) return
  paymentBusy.value = true
  paymentError.value = ''
  try {
    await api.confirmBookingPayment(paymentPanel.value.paymentIntentId, {
      paymentId: paymentPanel.value.paymentId || undefined,
      status: 'SUCCESS'
    })
    await api.createBooking({
      specialistId: paymentPanel.value.specialistId,
      slotId: paymentPanel.value.slotId,
      paymentId: paymentPanel.value.paymentId
    })
    closePaymentPanel()
    await loadUnpaidPayments()
    showAlertModal({
      type: 'success',
      message: 'Payment successful.',
      onClose: () =>
        router.push({
          name: 'customer.bookings',
          query: { refresh: String(Date.now()) }
        })
    })
  } catch (e) {
    paymentError.value = e?.message || 'Failed to confirm payment'
  } finally {
    paymentBusy.value = false
  }
}

async function mockPaymentFromPanel() {
  if (!paymentPanel.value.paymentIntentId) return
  paymentBusy.value = true
  paymentError.value = ''
  try {
    await api.mockBookingPayment(paymentPanel.value.paymentIntentId)
    await api.createBooking({
      specialistId: paymentPanel.value.specialistId,
      slotId: paymentPanel.value.slotId,
      paymentId: paymentPanel.value.paymentId
    })
    closePaymentPanel()
    await loadUnpaidPayments()
    showAlertModal({
      type: 'success',
      message: 'Mock payment successful.',
      onClose: () =>
        router.push({
          name: 'customer.bookings',
          query: { refresh: String(Date.now()) }
        })
    })
  } catch (e) {
    paymentError.value = e?.message || 'Failed to mock payment'
  } finally {
    paymentBusy.value = false
  }
}

async function cancelPaymentFromPanel() {
  if (!paymentPanel.value.paymentIntentId) return
  paymentBusy.value = true
  paymentError.value = ''
  try {
    await api.cancelUnpaidPayment(paymentPanel.value.paymentIntentId)
    closePaymentPanel()
    await loadUnpaidPayments()
    showAlertModal({ type: 'success', message: 'Payment cancelled.' })
  } catch (e) {
    paymentError.value = e?.message || 'Failed to cancel payment'
  } finally {
    paymentBusy.value = false
  }
}

watch(role, (nextRole) => {
  if (nextRole === 'Customer') {
    startUnpaidPolling()
    startCountdown()
  } else {
    stopUnpaidPolling()
    stopCountdown()
    unpaidItems.value = []
    countdownMap.value = {}
  }
}, { immediate: true })

onMounted(() => {
  if (role.value === 'Customer') {
    startUnpaidPolling()
    startCountdown()
  }
})

onBeforeUnmount(() => {
  stopUnpaidPolling()
  stopCountdown()
})
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <div class="sidebar__brand">
        <div class="brand__name">Schedly</div>
        <div v-if="auth.user" class="brand__meta">
          <span class="pill">{{ auth.user.role }}</span>
          <span class="muted">{{ auth.user.name || auth.user.email }}</span>
        </div>
      </div>

      <nav class="sidebar__nav">
        <router-link v-for="l in links" :key="l.to" :to="l.to">
          <component :is="l.icon" class="nav-item__icon" />
          <span>{{ l.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar__footer">
        <button class="btn logout-btn" :class="logoutButtonClass" @click="onLogout">
          Logout
        </button>
      </div>
    </aside>

    <div class="content">
      <header class="topbar">
        <button class="icon-btn" @click="mobileNavOpen = !mobileNavOpen" aria-label="Toggle navigation">
          ☰
        </button>
        <div class="topbar__title">{{ auth.user?.role || '' }}</div>
        <button class="btn btn--ghost" @click="onLogout">Logout</button>
      </header>

      <nav v-if="mobileNavOpen" class="mobile-nav">
        <router-link
            v-for="l in links"
            :key="l.to"
            :to="l.to"
            @click="mobileNavOpen = false"
        >
          <component :is="l.icon" class="mobile-nav__icon" />
          <span>{{ l.label }}</span>
        </router-link>
      </nav>

      <main class="content__main">
        <router-view />
      </main>

      <div
        v-if="role === 'Customer'"
        class="unpaid-float"
      >
        <button type="button" class="unpaid-float__btn" :title="'Unpaid orders'" @click="toggleUnpaidPanel">
          Pay
          <span v-if="unpaidItems.length" class="unpaid-float__badge">{{ unpaidItems.length }}</span>
        </button>
        <div v-if="unpaidOpen" class="unpaid-float__panel" @click.stop>
          <div class="unpaid-float__title">Unpaid Orders</div>
          <p v-if="unpaidError" class="unpaid-float__error">{{ unpaidError }}</p>
          <p v-else-if="unpaidLoading && !unpaidItems.length" class="unpaid-float__muted">Loading...</p>
          <p v-else-if="!unpaidItems.length" class="unpaid-float__muted">No unpaid orders</p>
          <ul v-else class="unpaid-float__list">
            <li v-for="item in unpaidItems" :key="item.paymentIntentId" class="unpaid-float__row">
              <div class="unpaid-float__line">{{ item.slotLabel || item.slotId }}</div>
              <div class="unpaid-float__meta">
                {{ Number(item.amount ?? 0).toFixed(2) }} {{ item.currency || 'CNY' }} · {{ formatRemain(countdownMap[item.paymentIntentId]) }}
              </div>
              <button
                type="button"
                class="unpaid-float__action"
                :disabled="resumeBusyId === item.paymentIntentId"
                @click="onContinuePay(item)"
              >
                {{ resumeBusyId === item.paymentIntentId ? 'Loading...' : 'Continue' }}
              </button>
            </li>
          </ul>
        </div>
      </div>

      <div v-if="paymentPanelOpen" class="payment-modal-backdrop" @click.self="closePaymentPanel">
        <section class="payment-modal-card" role="dialog" aria-modal="true" aria-label="Continue Payment">
          <header class="payment-modal-head">
            <h3 class="payment-modal-title">Continue Payment</h3>
            <button type="button" class="payment-btn-secondary" :disabled="paymentBusy" @click="closePaymentPanel">Pay Later</button>
          </header>
          <div class="payment-modal-body">
            <p class="payment-tip">Intent: {{ paymentPanel.paymentIntentId }}</p>
            <p class="payment-amount">Amount: {{ Number(paymentPanel.amount).toFixed(2) }} {{ paymentPanel.currency }}</p>
            <div class="payment-qr-wrap">
              <img v-if="paymentPanel.qrCodeUrl" class="payment-qr" :src="paymentPanel.qrCodeUrl" alt="Payment QR Code" />
              <div v-else class="payment-qr-empty">QR code unavailable</div>
            </div>
            <p v-if="paymentError" class="payment-banner">{{ paymentError }}</p>
          </div>
          <footer class="payment-modal-foot">
            <div class="payment-qr-tip-wrap">
              <span class="payment-qr-icon">!</span>
              <div class="payment-qr-tooltip">
                Please use Mock Payment!<br>
                Only support QR code payment on the Android sandbox version of Alipay!<br>
                Test Account: ruaalx2721@sandbox.com<br>
                Password: 111111
              </div>
            </div>
            <button type="button" class="payment-btn-secondary" :disabled="paymentBusy" @click="cancelPaymentFromPanel">Cancel</button>
            <button type="button" class="payment-btn-secondary" :disabled="paymentBusy" @click="mockPaymentFromPanel">Mock Payment</button>
            <button type="button" class="payment-btn-submit" :disabled="paymentBusy" @click="confirmPaymentFromPanel">
              {{ paymentBusy ? 'Confirming...' : 'Paid' }}
            </button>
          </footer>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.payment-qr-tip-wrap {
  position: relative;
  display: flex;
  justify-content: center;
  margin-bottom: 8px;
  margin-top: 12px;
}

/* 感叹号 */
.payment-qr-icon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #faad14;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

}

/* tooltip 本体 */
.payment-qr-tooltip {
  position: absolute;
  bottom: 130%; /* 在上方 */
  left: 50%;
  transform: translateX(-50%);
  width: 240px;

  background: #111827;
  color: #fff;
  font-size: 12px;
  line-height: 1.4;

  padding: 8px 10px;
  border-radius: 6px;

  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease;
}

/* hover 显示 */
.payment-qr-tip-wrap:hover .payment-qr-tooltip {
  opacity: 1;
}
.app {
  min-height: 100vh;
  background: #F0EAE5;
  color: #111827;
  display: grid;
  grid-template-columns: 260px 1fr;
}

.sidebar {
  border-right: 1px solid #c7b29d;
  padding: 16px 14px;
  position: sticky;
  top: 0;
  height: 100vh;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 14px;
}
.sidebar__brand {
  padding: 16px 12px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
}
.brand__name {
  font-size: 28px;
  font-weight: 900;
  line-height: 1.1;
  letter-spacing: 0.8px;
  margin-bottom: 10px;
}
.brand__meta {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 2px;
  align-items: center;
}
.pill {
  font-size: 12px;
  padding: 2px 8px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
}
.muted {
  opacity: 0.8;
  font-size: 13px;
}
.sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 6px 2px;
}
.sidebar__nav a {
  color: #111827;
  text-decoration: none;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 0;
  background: transparent;
  transition: background-color 0.15s ease, color 0.15s ease;
}
.nav-item__icon {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
}
.sidebar__nav a:hover {
  background: rgba(17, 24, 39, 0.08);
}
.sidebar__nav a.router-link-active {
  color: #ffffff;
  background: #000000;
}
.sidebar__footer {
  padding: 12px 2px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.content {
  min-width: 0;
  display: grid;
  grid-template-rows: auto auto 1fr;
}
.topbar {
  display: none;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.topbar__title {
  font-weight: 700;
  opacity: 0.9;
}
.icon-btn {
  appearance: none;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.06);
  color: #111827;
  border-radius: 10px;
  padding: 8px 10px;
  cursor: pointer;
}
.mobile-nav {
  display: none;
  padding: 10px 14px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.mobile-nav a {
  color: #111827;
  text-decoration: none;
  padding: 10px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 0;
  margin-top: 6px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.mobile-nav__icon {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
}
.content__main {
  padding: 18px 20px 60px 28px;
  max-width: 1280px;
  width: 100%;
}
.btn {
  appearance: none;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.06);
  color: #111827;
  padding: 8px 12px;
  border-radius: 10px;
  cursor: pointer;
}
.btn--ghost {
  background: transparent;
}

.logout-btn {
  min-width: 132px;
  padding: 10px 18px;
  border-radius: 0;
  border: 1px solid transparent;
  color: #ffffff;
  font-weight: 600;
  text-align: center;
  transition: filter 0.15s ease, transform 0.05s ease;
}

.logout-btn:hover {
  filter: brightness(0.95);
}

.logout-btn:active {
  transform: translateY(1px);
}

.logout-btn--customer {
  background: #a94442;
  border-color: #a94442;
}

.logout-btn--team {
  background: #a94442;
  border-color: #a94442;
}

.unpaid-float {
  position: fixed;
  right: 62px;
  bottom: 80px;
  z-index: 50;
}

.unpaid-float__btn {
  width: 64px;
  height: 64px;
  border-radius: 999px;
  border: 2px solid #a94442;
  background: #fff;
  color: #a94442;
  font-weight: 800;
  font-size: 14px;
  cursor: pointer;
  position: relative;
}

.unpaid-float__badge {
  position: absolute;
  top: -9px;
  right: -9px;
  min-width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #a94442;
  color: #fff;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  padding: 0 4px;
}

.unpaid-float__panel {
  width: 320px;
  max-height: 360px;
  overflow: auto;
  margin-top: 10px;
  border: 1px solid #d8d1cb;
  background: #fff;
  box-shadow: 0 10px 24px rgba(17, 24, 39, 0.18);
  padding: 10px;
}

.unpaid-float__title {
  font-size: 14px;
  font-weight: 800;
  margin-bottom: 8px;
}

.unpaid-float__muted {
  margin: 0;
  color: #6b7280;
  font-size: 12px;
}

.unpaid-float__error {
  margin: 0;
  color: #991b1b;
  font-size: 12px;
}

.unpaid-float__list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 8px;
}

.unpaid-float__row {
  border: 1px solid #eceff3;
  padding: 8px;
}

.unpaid-float__line {
  font-size: 12px;
  color: #111827;
  font-weight: 600;
}

.unpaid-float__meta {
  margin-top: 4px;
  font-size: 12px;
  color: #4b5563;
}

.unpaid-float__action {
  margin-top: 6px;
  height: 30px;
  border: 1px solid #a94442;
  background: #a94442;
  color: #fff;
  padding: 0 10px;
  cursor: pointer;
}

.payment-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  background: rgba(17, 24, 39, 0.42);
}

.payment-modal-card {
  width: min(100%, 460px);
  background: #fff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  box-shadow: 0 16px 36px rgba(17, 24, 39, 0.16);
}

.payment-modal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-bottom: 1px solid #eceff3;
}

.payment-modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.payment-modal-close {
  width: 32px;
  height: 32px;
  border: 1px solid #d8d1cb;
  background: #fff;
  cursor: pointer;
}

.payment-modal-body {
  padding: 12px 14px;
}

.payment-tip {
  margin: 0 0 4px;
  font-size: 12px;
  color: #4b5563;
}

.payment-amount {
  margin: 8px 0 10px;
  font-size: 14px;
  font-weight: 700;
}

.payment-qr-wrap {
  width: 280px;
  height: 280px;
  margin: 0 auto;
  border: 1px solid #d8d1cb;
  display: grid;
  place-items: center;
}

.payment-qr {
  width: 260px;
  height: 260px;
  object-fit: contain;
}

.payment-qr-empty {
  color: #6b7280;
  font-size: 12px;
}

.payment-banner {
  margin-top: 10px;
  color: #991b1b;
  font-size: 12px;
}

.payment-modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 0 14px 12px;
}

.payment-btn-secondary {
  height: 40px;
  padding: 0 14px;
  border: 1px solid #202124;
  background: #ffffff;
  color: #202124;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.payment-btn-submit {
  height: 40px;
  padding: 0 14px;
  border: 1px solid #a94442;
  background: #a94442;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.payment-btn-submit:hover:not(:disabled) {
  opacity: 0.92;
}

.payment-btn-secondary:disabled,
.payment-btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 980px) {
  .app {
    grid-template-columns: 1fr;
  }
  .sidebar {
    display: none;
  }
  .topbar {
    display: flex;
  }
  .mobile-nav {
    display: block;
  }
  .content__main {
    padding: 16px 14px 50px;
  }

  .unpaid-float {
    right: 54px;
    bottom: 70px;
  }

  .unpaid-float__panel {
    width: min(88vw, 320px);
  }
}
</style>
