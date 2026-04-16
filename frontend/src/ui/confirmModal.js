import { reactive, readonly } from 'vue'

const state = reactive({
    open: false,
    title: '',
    message: '',
    onConfirm: null,
    onCancel: null
})

export function showConfirmModal(payload) {
    const p = payload ?? {}
    state.title = p.title || 'Confirm operation'
    state.message = p.message || 'Are you sure you want to perform this action?'
    state.onConfirm = typeof p.onConfirm === 'function' ? p.onConfirm : null
    state.onCancel = typeof p.onCancel === 'function' ? p.onCancel : null
    state.open = true
}

export function confirmAction() {
    const cb = state.onConfirm
    state.open = false
    if (cb) cb()
}

export function cancelAction() {
    const cb = state.onCancel
    state.open = false
    if (cb) cb()
}

export function useConfirmModalState() {
    return readonly(state)
}