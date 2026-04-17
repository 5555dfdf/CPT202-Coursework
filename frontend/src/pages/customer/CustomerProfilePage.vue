<script setup>
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/api/client'

const auth = useAuthStore()
const name = ref('')
const editName = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const ok = ref('')
const editOpen = ref(false)

async function load() {
  error.value = ''
  ok.value = ''
  loading.value = true
  try {
    const me = await api.getMe()
    const u = me.user
    name.value = u?.name ?? ''
    editName.value = name.value
    if (u) auth.setUser(u)
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    const u = auth.user
    name.value = u?.name ?? ''
    editName.value = name.value
  } finally {
    loading.value = false
  }
}

function openEdit() {
  editName.value = name.value
  editOpen.value = true
}

function closeEdit() {
  if (saving.value) return
  editOpen.value = false
}

async function onSave() {
  error.value = ''
  ok.value = ''
  saving.value = true
  try {
    const me = await api.updateMe({ name: editName.value.trim() || undefined })
    const u = me.user
    name.value = u?.name ?? editName.value.trim()
    editName.value = name.value
    if (u) auth.setUser(u)
    ok.value = 'Saved'
    editOpen.value = false
  } catch (e) {
    error.value = e?.message || 'Failed to save'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Profile</h1>
      <p class="subtitle">Manage your basic account information.</p>
    </header>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>
    <div v-if="ok" class="banner banner--ok">{{ ok }}</div>

    <div class="card">
      <div class="title">Basic Info</div>
      <div v-if="loading" class="muted">Loading…</div>
      <template v-else>
        <div class="card-main">
          <dl class="kv">
            <dt>Name</dt>
            <dd>{{ name || '—' }}</dd>
            <dt>Email</dt>
            <dd>{{ auth.user?.email || '—' }}</dd>
            <dt>Role</dt>
            <dd>{{ auth.user?.role || '—' }}</dd>
          </dl>
          <div class="card-action">
            <button type="button" class="btn" @click="openEdit">
              Edit Profile
            </button>
          </div>
        </div>
      </template>
    </div>

    <div v-if="editOpen" class="modal-backdrop" @click.self="closeEdit">
      <section class="modal-card">
        <h3 class="modal-title">Edit Profile</h3>
        <label class="field">
          <span class="label">Name</span>
          <input v-model="editName" class="input" type="text" maxlength="50" autocomplete="name" />
        </label>
        <div class="modal-footer">
          <button type="button" class="btn btn--ghost" :disabled="saving" @click="closeEdit">Cancel</button>
          <button type="button" class="btn" :disabled="saving" @click="onSave">
            {{ saving ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </section>
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
.subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: #4b5563;
}
.muted {
  color: #6b7280;
}
.card {
  margin-top: 10px;
  padding: 16px;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.06);
}
.title {
  font-weight: 700;
  margin-bottom: 14px;
  font-size: 16px;
}
.card-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
}
.kv {
  margin: 0 0 14px;
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 10px 14px;
  font-size: 15px;
  flex: 1;
  max-width: 480px;
}
.kv dt {
  margin: 0;
  color: #6b7280;
}
.kv dd {
  margin: 0;
  color: #111827;
  font-weight: 600;
}
.field {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}
.label {
  font-size: 13px;
  color: #4b5563;
  font-weight: 600;
}
.input {
  height: 44px;
  padding: 0 12px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  background: #f8f5f2;
  color: #111827;
}
.btn {
  padding: 0 18px;
  height: 44px;
  border-radius: 0;
  border: 1px solid #D9533C;
  background: #D9533C;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
}
.btn:hover:not(:disabled) {
  opacity: 0.92;
}
.btn:disabled {
  opacity: 0.6;
}
.card-action {
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
  min-width: 220px;
}
.btn--ghost {
  border: 1px solid #202124;
  background: #ffffff;
  color: #202124;
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
.banner--ok {
  border: 1px solid rgba(52, 211, 153, 0.45);
  background: rgba(52, 211, 153, 0.12);
}
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(17, 24, 39, 0.42);
}
.modal-card {
  width: min(100%, 520px);
  background: #ffffff;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 0;
  padding: 18px;
  box-shadow: 0 16px 36px rgba(17, 24, 39, 0.16);
}
.modal-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}
.modal-footer {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 720px) {
  .card-main {
    flex-direction: column;
    align-items: stretch;
  }

  .card-action {
    min-width: 0;
    justify-content: flex-start;
  }
}
</style>

