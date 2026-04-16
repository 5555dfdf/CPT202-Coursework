<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { showConfirmModal } from '@/ui/confirmModal.js'
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
    onConfirm: async () => {
      await auth.logout()
      await router.replace({ name: 'login' })
    }
  })
}
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
    </div>
  </div>
</template>

<style scoped>
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
  background: #d9533c;
  border-color: #d9533c;
}

.logout-btn--team {
  background: #a94442;
  border-color: #a94442;
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
}
</style>
