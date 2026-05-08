import { reactive, readonly } from "vue";

// global confirm modal state with confirm/cancel callbacks
const state = reactive({
  open: false,
  title: "",
  message: "",
  confirmVariant: "",
  onConfirm: null,
  onCancel: null,
});

export function showConfirmModal(payload) {
  // apply defaults; callers only pass required fields
  const p = payload ?? {};
  state.title = p.title || "Confirm operation";
  state.message = p.message || "Are you sure you want to perform this action?";
  state.confirmVariant = p.confirmVariant || "";
  state.onConfirm = typeof p.onConfirm === "function" ? p.onConfirm : null;
  state.onCancel = typeof p.onCancel === "function" ? p.onCancel : null;
  state.open = true;
}

export function confirmAction() {
  // close modal and run confirm callback if provided
  const cb = state.onConfirm;
  state.open = false;
  if (cb) cb();
}

export function cancelAction() {
  // close modal and run cancel callback if provided
  const cb = state.onCancel;
  state.open = false;
  if (cb) cb();
}

export function useConfirmModalState() {
  // expose readonly state for modal host component
  return readonly(state);
}
