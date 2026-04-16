<script setup>
import { cancelAction, confirmAction, useConfirmModalState } from '@/ui/confirmModal.js'

const state = useConfirmModalState()
</script>

<template>
  <teleport to="body">
    <div v-if="state.open" class="am-backdrop" @click.self="cancelAction">
      <section class="am-card" role="dialog" aria-modal="true" :aria-label="state.title">
        <header class="am-head am-head--warn">
          <h3 class="am-title">{{ state.title }}</h3>
        </header>

        <div class="am-body">
          <p class="am-message">{{ state.message }}</p>
        </div>

        <footer class="am-footer">
          <button type="button" class="am-btn am-btn--ghost" @click="cancelAction">Cancel</button>
          <button type="button" class="am-btn am-btn--primary" @click="confirmAction">Confirm</button>
        </footer>
      </section>
    </div>
  </teleport>
</template>

<style scoped>
.am-backdrop {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(17, 24, 39, 0.42);
}

.am-card {
  width: min(100%, 560px);
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  box-shadow: 0 16px 36px rgba(17, 24, 39, 0.16);
}

.am-head {
  padding: 14px 16px;
  border-bottom: 1px solid #eceff3;
}

.am-head--warn {
  background: rgba(234, 179, 8, 0.18);
}

.am-title {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: #111827;
}

.am-body {
  padding: 16px;
}

.am-message {
  margin: 0;
  color: #111827;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-line;
}

.am-footer {
  padding: 12px 16px 16px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.am-btn {
  min-width: 100px;
  height: 40px;
  padding: 0 14px;
  font-weight: 800;
  cursor: pointer;
}


.am-btn--primary {
  border: 1px solid #a94442;
  background: #a94442;
  color: #ffffff;
}


.am-btn--ghost {
  border: 1px solid rgba(17, 24, 39, 0.16);
  background: transparent;
  color: #111827;
}
</style>