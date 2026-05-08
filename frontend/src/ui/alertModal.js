import { reactive, readonly } from "vue";

// tiny global alert modal state shared across pages
const state = reactive({
  open: false,
  type: "info",
  title: "",
  message: "",
  onClose: null,
});

export function showAlertModal(payload) {
  // normalize payload to support partial fields
  const p = payload ?? {};
  state.type = p.type || "info";
  state.title = p.title || "";
  state.message = p.message || "";
  state.onClose = typeof p.onClose === "function" ? p.onClose : null;
  state.open = true;
}

export function closeAlertModal() {
  // close first then run optional callback
  const cb = state.onClose;
  state.open = false;
  state.onClose = null;
  if (cb) cb();
}

export function useAlertModalState() {
  // expose readonly state for modal host component
  return readonly(state);
}
