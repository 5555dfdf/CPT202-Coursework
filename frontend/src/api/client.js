import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const TOKEN_KEY = 'booking.auth.token'

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(TOKEN_KEY)
  const url = config?.url || ''
  const isPublicAuthEndpoint =
      url.startsWith('/auth/login') ||
      url.startsWith('/auth/login-by-email-code') ||
      url.startsWith('/auth/register') ||
      url.startsWith('/auth/send-email-code')

  if (token && !isPublicAuthEndpoint) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function unwrap(res) {
  return res?.data ?? res
}

function throwIfHtmlString(body) {
  if (typeof body !== 'string') return
  const t = body.trimStart()
  if (t.startsWith('<') || t.startsWith('<!')) {
    throw new Error(
        '接口返回了 HTML 而非 JSON（请设置 VITE_API_BASE_URL，或在 vite 中把对应 API 路径代理到后端）'
    )
  }
}


export function extractListPayload(body) {
  if (body == null) return []
  throwIfHtmlString(body)
  if (Array.isArray(body)) return body

  const topKeys = ['data', 'result', 'rows', 'list', 'records', 'items', 'content']
  for (const k of topKeys) {
    const v = body[k]
    if (Array.isArray(v)) return v
  }

  const inner = body.data
  if (inner && typeof inner === 'object' && !Array.isArray(inner)) {
    for (const k of ['list', 'records', 'rows', 'items', 'content']) {
      if (Array.isArray(inner[k])) return inner[k]
    }
  }

  return []
}


export function extractDataPayload(body) {
  if (body == null) return null
  throwIfHtmlString(body)
  if (Array.isArray(body)) return null
  if (typeof body !== 'object') return null
  if (Object.prototype.hasOwnProperty.call(body, 'data')) {
    const d = body.data
    if (d != null && typeof d === 'object' && !Array.isArray(d)) return d
  }
  return body
}


export function extractDataOrListPayload(body) {
  if (body == null) return null
  throwIfHtmlString(body)
  if (Array.isArray(body)) return body
  if (typeof body !== 'object') return body
  if (Object.prototype.hasOwnProperty.call(body, 'data')) {
    const data = body.data
    if (Array.isArray(data)) return data
    if (data != null && typeof data === 'object') return data
  }
  return body
}

function extractPaymentPayload(body) {
  const d = extractDataPayload(body) ?? body
  if (!d || typeof d !== 'object' || Array.isArray(d)) return {}
  return {
    ...d,
    paymentId: d.paymentId ?? d.id ?? '',
    paymentToken: d.paymentToken ?? d.token ?? d.paymentId ?? d.id ?? '',
    qrCodeUrl: d.qrCodeUrl ?? d.qrUrl ?? d.paymentQrCodeUrl ?? d.codeUrl ?? ''
  }
}


export function extractPagePayload(body) {
  const empty = { items: [], total: 0, page: 1, pageSize: 10 }
  if (body == null) return empty
  throwIfHtmlString(body)

  const coerce = (b) => {
    if (!b || typeof b !== 'object') return null
    if (Array.isArray(b)) return { items: b, total: b.length, page: 1, pageSize: b.length || 10 }
    const d = b.data
    if (d != null && typeof d === 'object') {
      if (Array.isArray(d)) return { items: d, total: d.length, page: 1, pageSize: d.length || 10 }
      const nestedItems = d.items ?? d.records ?? d.rows ?? d.list
      if (Array.isArray(nestedItems)) return { ...d, items: nestedItems }
    }
    const items = b.items ?? b.records ?? b.rows ?? b.list
    if (Array.isArray(items)) return { ...b, items }
    return null
  }

  const page = coerce(body)
  if (!page) return empty
  const arr = Array.isArray(page.items) ? page.items : []
  return {
    items: arr,
    total: page.total ?? page.totalCount ?? arr.length,
    page: page.page ?? page.current ?? 1,
    pageSize: page.pageSize ?? page.size ?? 10
  }
}

function normalizeMeResponse(body) {
  const d = extractDataPayload(body) ?? body
  if (!d || typeof d !== 'object') return { user: null }
  if (d.user) return { user: d.user }
  if (d.id != null || d.email != null || d.role != null) return { user: d }
  return { user: null }
}

function extractAuthPayload(body) {
  const d = extractDataPayload(body) ?? body
  if (!d || typeof d !== 'object') return body
  const token = d.token ?? d.accessToken
  const user = d.user
  return { ...d, token, user }
}

function normalizeError(err) {
  const status = err?.response?.status
  const data = err?.response?.data
  const code = data?.code
  const message = data?.message || err?.message || '请求失败'
  return { status, code, message, raw: err, data }
}

function throwIfBusinessError(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) return
  const code = typeof body.code === 'string' ? body.code.trim().toUpperCase() : ''
  if (code === 'ERROR') {
    const err = new Error(body.message || '请求失败')
    throw normalizeError({
      message: err.message,
      response: {
        status: 200,
        data: body
      }
    })
  }
}

async function request(promise) {
  try {
    const body = unwrap(await promise)
    throwIfBusinessError(body)
    return body
  } catch (err) {
    throw normalizeError(err)
  }
}

export const api = {
  sendRegisterEmailCode: (payload) => request(http.post('/auth/send-email-code', payload)),
  register: (payload) => request(http.post('/auth/register', payload)).then(extractAuthPayload),
  login: (payload) => request(http.post('/auth/login', payload)).then(extractAuthPayload),
  loginByEmailCode: (payload) =>
      request(http.post('/auth/login-by-email-code', payload)).then(extractAuthPayload),
  logout: () => request(http.post('/auth/logout')),
  getMe: () => request(http.get('/me')).then(normalizeMeResponse),
  updateMe: (payload) => request(http.patch('/me', payload)).then(normalizeMeResponse),
  verifyMyPassword: (payload) => request(http.post('/me/verify-password', payload)),
  changeMyPassword: async (payload) => {
    const candidates = ['/me/change-password', '/me/password', '/auth/change-password']
    let lastErr = null
    for (const url of candidates) {
      try {
        return await request(http.post(url, payload))
      } catch (e) {
        lastErr = e
        if (e?.status === 403 || e?.status === 404 || e?.status === 405) continue
        throw e
      }
    }
    throw lastErr ?? new Error('Failed to change password')
  },

  listExpertise: () => request(http.get('/expertise')).then(extractListPayload),
  listSpecialists: (params) => request(http.get('/specialists', { params })).then(extractPagePayload),
  getSpecialist: (id) => request(http.get(`/specialists/${id}`)).then(extractDataPayload),
  listSpecialistSlots: (id, params) =>
      request(http.get(`/specialists/${id}/slots`, { params })).then(extractListPayload),

  createBooking: (payload) => request(http.post('/bookings', payload)).then(extractDataPayload),
  listMyBookings: (params) => request(http.get('/bookings', { params })).then(extractPagePayload),
  getBooking: (id) => request(http.get(`/bookings/${id}`)).then(extractDataPayload),
  cancelBooking: (id, payload) => request(http.post(`/bookings/${id}/cancel`, payload)).then(extractDataPayload),
  rescheduleBooking: (id, payload) =>
      request(http.post(`/bookings/${id}/reschedule`, payload)).then(extractDataPayload),

  listBookingRequests: (params) =>
      request(http.get('/specialist/booking-requests', { params })).then(extractPagePayload),
  specialistGetBooking: (id) => request(http.get(`/specialist/bookings/${id}`)).then(extractDataPayload),
  confirmBooking: (id) => request(http.post(`/specialist/bookings/${id}/confirm`)).then(extractDataPayload),
  rejectBooking: (id, payload) =>
      request(http.post(`/specialist/bookings/${id}/reject`, payload)).then(extractDataPayload),
  completeBooking: (id) =>
      request(http.post(`/specialist/bookings/${id}/complete`)).then(extractDataPayload),
  adminListBookings: (params) => request(http.get('/admin/bookings', { params })).then(extractPagePayload),
  adminGetBooking: (id) => request(http.get(`/admin/bookings/${id}`)).then(extractDataPayload),
  adminListSlots: (params) => request(http.get('/admin/slots', { params })).then(extractListPayload),
  adminCreateSlot: (payload) => request(http.post('/admin/slots', payload)).then(extractDataPayload),
  adminUpdateSlot: (id, payload) =>
      request(http.patch(`/admin/slots/${id}`, payload)).then(extractDataPayload),
  adminDeleteSlot: (id) => request(http.delete(`/admin/slots/${id}`)).then(extractDataPayload),
  adminCreateSpecialist: (payload) => request(http.post('/admin/specialists', payload)).then(extractDataPayload),
  adminUpdateSpecialist: (id, payload) =>
      request(http.patch(`/admin/specialists/${id}`, payload)).then(extractDataPayload),
  adminSetSpecialistStatus: (id, payload) =>
      request(http.post(`/admin/specialists/${id}/status`, payload)).then(extractDataPayload),
  adminDeleteSpecialist: (id) =>
      request(http.delete(`/admin/specialists/${id}`)).then(extractDataPayload),
  adminCreateExpertise: (payload) => request(http.post('/admin/expertise', payload)).then(extractDataPayload),
  adminUpdateExpertise: (id, payload) =>
      request(http.patch(`/admin/expertise/${id}`, payload)).then(extractDataPayload),
  adminDeleteExpertise: (id) =>
      request(http.delete(`/admin/expertise/${id}`)).then(extractDataPayload),

  quotePricing: (payload) => request(http.post('/pricing/quote', payload)).then(extractDataOrListPayload),
  adminListPricingRules: (params) =>
      request(http.get('/admin/pricing-rules', { params })).then(extractListPayload),
  adminGetPricingRule: (id) => request(http.get(`/admin/pricing-rules/${id}`)).then(extractDataPayload),
  adminCreatePricingRule: (payload) =>
      request(http.post('/admin/pricing-rules', payload)).then(extractDataPayload),
  adminUpdatePricingRule: (id, payload) =>
      request(http.patch(`/admin/pricing-rules/${id}`, payload)).then(extractDataPayload),
  adminDeletePricingRule: (id) =>
      request(http.delete(`/admin/pricing-rules/${id}`)).then(extractDataPayload),

  createBookingPayment: async (bookingId, payload) => {
    const candidates = [
      `/bookings/${bookingId}/payment`,
      `/bookings/${bookingId}/pay`,
      `/payments/bookings/${bookingId}`
    ]
    let lastErr = null
    for (const url of candidates) {
      try {
        return await request(http.post(url, payload)).then(extractPaymentPayload)
      } catch (e) {
        lastErr = e
        if (e?.status === 403 || e?.status === 404 || e?.status === 405) continue
        throw e
      }
    }
    throw lastErr ?? new Error('Failed to create payment')
  },
  confirmBookingPayment: async (bookingId, payload) => {
    const candidates = [
      `/bookings/${bookingId}/payment/confirm`,
      `/bookings/${bookingId}/pay/confirm`,
      `/bookings/${bookingId}/pay/success`,
      `/payments/bookings/${bookingId}/confirm`
    ]
    let lastErr = null
    for (const url of candidates) {
      try {
        return await request(http.post(url, payload)).then(extractDataPayload)
      } catch (e) {
        lastErr = e
        if (e?.status === 403 || e?.status === 404 || e?.status === 405) continue
        throw e
      }
    }
    throw lastErr ?? new Error('Failed to confirm payment')
  },
  mockBookingPayment: async (bookingId) => {
    const candidates = [
      `/bookings/${bookingId}/payment/mock-success`,
      `/bookings/${bookingId}/pay/mock-success`
    ]
    let lastErr = null
    for (const url of candidates) {
      try {
        return await request(http.post(url)).then(extractDataPayload)
      } catch (e) {
        lastErr = e
        if (e?.status === 403 || e?.status === 404 || e?.status === 405) continue
        throw e
      }
    }
    throw lastErr ?? new Error('Failed to mock payment')
  },
  listUnpaidPayments: () =>
      request(http.get('/bookings/unpaid-payments')).then((body) => {
        const data = extractDataPayload(body) ?? {}
        return Array.isArray(data.items) ? data.items : []
      }),
  getUnpaidPayment: (paymentIntentId) =>
      request(http.get(`/bookings/unpaid-payments/${paymentIntentId}`)).then(extractDataPayload),
  resumeUnpaidPayment: (paymentIntentId) =>
      request(http.post(`/bookings/unpaid-payments/${paymentIntentId}/resume`)).then(extractPaymentPayload),

  // Specialist slot management
  specialistListSlots: (params) => request(http.get('/specialist/slots', { params })).then(extractListPayload),
  specialistGetSlot: (id) => request(http.get(`/specialist/slots/${id}`)).then(extractDataPayload),
  specialistCreateSlot: (payload) => request(http.post('/specialist/slots', payload)).then(extractDataPayload),
  specialistUpdateSlot: (id, payload) => request(http.patch(`/specialist/slots/${id}`, payload)).then(extractDataPayload),
  specialistDeleteSlot: (id) => request(http.delete(`/specialist/slots/${id}`)).then(extractDataPayload)
}
