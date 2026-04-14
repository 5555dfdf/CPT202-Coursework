import { reactive, readonly } from 'vue'

const state = reactive({
  open: false,
  type: 'info', // 'success' | 'error' | 'warn' | 'info'
  title: '',
  message: '',
  onClose: null
})

export function showAlertModal(payload) {
  const p = payload ?? {}
  state.type = p.type || 'info'
  state.title = p.title || ''
  state.message = p.message || ''
  state.onClose = typeof p.onClose === 'function' ? p.onClose : null
  state.open = true
}

export function closeAlertModal() {
  const cb = state.onClose
  state.open = false
  state.onClose = null
  if (cb) cb()
}

export function useAlertModalState() {
  return readonly(state)
}

