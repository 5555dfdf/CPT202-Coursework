import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

import PublicLayout from '@/layouts/PublicLayout.vue'
import AppLayout from '@/layouts/AppLayout.vue'

import LoginPage from '@/pages/public/LoginPage.vue'
import ExpertAdminLoginPage from '@/pages/public/ExpertAdminLoginPage.vue'
import RegisterPage from '@/pages/public/RegisterPage.vue'
import DevLoginPage from '@/pages/public/DevLoginPage.vue'

import CustomerSpecialistsPage from '@/pages/customer/CustomerSpecialistsPage.vue'
import CustomerSpecialistDetailPage from '@/pages/customer/CustomerSpecialistDetailPage.vue'
import CustomerSpecialistSlotsPage from '@/pages/customer/CustomerSpecialistSlotsPage.vue'
import CustomerBookingsPage from '@/pages/customer/CustomerBookingsPage.vue'
import CustomerBookingDetailPage from '@/pages/customer/CustomerBookingDetailPage.vue'
import CustomerProfilePage from '@/pages/customer/CustomerProfilePage.vue'

import SpecialistDashboardPage from '@/pages/specialist/SpecialistDashboardPage.vue'
import SpecialistSchedulePage from '@/pages/specialist/SpecialistSchedulePage.vue'
import SpecialistRequestsPage from '@/pages/specialist/SpecialistRequestsPage.vue'
import SpecialistBookingDetailPage from '@/pages/specialist/SpecialistBookingDetailPage.vue'
import SpecialistSlotsPage from '@/pages/specialist/SpecialistSlotsPage.vue'
import SpecialistSlotCreatePage from '@/pages/specialist/SpecialistSlotCreatePage.vue'

import AdminDashboardPage from '@/pages/admin/AdminDashboardPage.vue'
import AdminSpecialistsPage from '@/pages/admin/AdminSpecialistsPage.vue'
import AdminExpertisePage from '@/pages/admin/AdminExpertisePage.vue'
import AdminSlotsPage from '@/pages/admin/AdminSlotsPage.vue'
import AdminSlotCreatePage from '@/pages/admin/AdminSlotCreatePage.vue'
import AdminPricingPage from '@/pages/admin/AdminPricingPage.vue'
import AdminPricingRuleCreatePage from '@/pages/admin/AdminPricingRuleCreatePage.vue'
import AdminBookingsPage from '@/pages/admin/AdminBookingsPage.vue'

const ROLE = {
  Admin: 'Admin',
  Specialist: 'Specialist',
  Customer: 'Customer'
}

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/',
    component: PublicLayout,
    meta: { public: true },
    children: [
      { path: 'login', name: 'login', component: LoginPage },
      { path: 'expert-admin-login', name: 'expert-admin-login', component: ExpertAdminLoginPage },
      { path: 'register', name: 'register', component: RegisterPage },
      { path: 'dev-login', name: 'dev-login', component: DevLoginPage }
    ]
  },
  {
    path: '/customer',
    component: AppLayout,
    meta: { requiresAuth: true, roles: [ROLE.Customer] },
    children: [
      { path: '', redirect: '/customer/specialists' },
      { path: 'specialists', name: 'customer.specialists', component: CustomerSpecialistsPage },
      { path: 'specialists/:id', name: 'customer.specialistDetail', component: CustomerSpecialistDetailPage, props: true },
      {
        path: 'specialists/:id/slots',
        name: 'customer.specialistSlots',
        component: CustomerSpecialistSlotsPage,
        props: (route) => ({
          id: route.params.id,
          bookingId: route.query.bookingId || ''
        })
      },
      { path: 'bookings', name: 'customer.bookings', component: CustomerBookingsPage },
      { path: 'bookings/:id', name: 'customer.bookingDetail', component: CustomerBookingDetailPage, props: true },
      { path: 'profile', name: 'customer.profile', component: CustomerProfilePage }
    ]
  },
  {
    path: '/specialist',
    component: AppLayout,
    meta: { requiresAuth: true, roles: [ROLE.Specialist] },
    children: [
      { path: '', redirect: '/specialist/dashboard' },
      { path: 'dashboard', name: 'specialist.dashboard', component: SpecialistDashboardPage },
      { path: 'schedule', name: 'specialist.schedule', component: SpecialistSchedulePage },
      { path: 'requests', name: 'specialist.requests', component: SpecialistRequestsPage },
      { path: 'bookings/:id', name: 'specialist.bookingDetail', component: SpecialistBookingDetailPage, props: true },
      { path: 'slots', name: 'specialist.slots', component: SpecialistSlotsPage },
      { path: 'slots/create', name: 'specialist.slotCreate', component: SpecialistSlotCreatePage },
      { path: 'slots/:id/edit', name: 'specialist.slotEdit', component: SpecialistSlotCreatePage, props: true }
    ]
  },
  {
    path: '/admin',
    component: AppLayout,
    meta: { requiresAuth: true, roles: [ROLE.Admin] },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'admin.dashboard', component: AdminDashboardPage },
      { path: 'specialists', name: 'admin.specialists', component: AdminSpecialistsPage },
      { path: 'expertise', name: 'admin.expertise', component: AdminExpertisePage },
      { path: 'slots', name: 'admin.slots', component: AdminSlotsPage },
      { path: 'slots/create', name: 'admin.slotCreate', component: AdminSlotCreatePage },
      { path: 'pricing', name: 'admin.pricing', component: AdminPricingPage },
      { path: 'pricing/create', name: 'admin.pricingCreate', component: AdminPricingRuleCreatePage },
      { path: 'bookings', name: 'admin.bookings', component: AdminBookingsPage }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

export const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (!auth.initialized) {
    try {
      await auth.bootstrap()
    } catch {
      // ignore bootstrap errors; treat as logged out
    }
  }

  if (to.meta?.public) return true

  if (to.meta?.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { next: to.fullPath } }
  }

  const roles = to.meta?.roles
  if (Array.isArray(roles) && roles.length > 0) {
    if (!auth.user?.role || !roles.includes(auth.user.role)) {
      return auth.defaultHomeRoute()
    }
  }

  return true
})
