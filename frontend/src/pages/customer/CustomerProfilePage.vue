<script setup>
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/api/client'

const auth = useAuthStore()
const name = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const ok = ref('')

async function load() {
  error.value = ''
  ok.value = ''
  loading.value = true
  try {
    const me = await api.getMe()
    const u = me.user
    name.value = u?.name ?? ''
    if (u) auth.setUser(u)
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    const u = auth.user
    name.value = u?.name ?? ''
  } finally {
    loading.value = false
  }
}

async function onSave() {
  error.value = ''
  ok.value = ''
  saving.value = true
  try {
    const me = await api.updateMe({ name: name.value.trim() || undefined })
    const u = me.user
    if (u) auth.setUser(u)
    ok.value = 'Saved'
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
        <label class="field">
          <span class="label">Name</span>
          <input v-model="name" class="input" type="text" autocomplete="name" />
        </label>
        <div class="meta-list">
          <p v-if="auth.user?.email" class="meta-item">
            <span class="meta-key">Email</span>
            <span class="meta-value">{{ auth.user.email }}</span>
          </p>
          <p v-if="auth.user?.role" class="meta-item">
            <span class="meta-key">Role</span>
            <span class="meta-value">{{ auth.user.role }}</span>
          </p>
        </div>
        <button type="button" class="btn" :disabled="saving" @click="onSave">
          {{ saving ? 'Saving…' : 'Save' }}
        </button>
      </template>
    </div>
  </section>
</template>

<style scoped>
.page__header h1 {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 800;
}
.subtitle {
  margin: 0;
  font-size: 14px;
  color: #5b6472;
}
.muted {
  opacity: 0.8;
}
.small {
  font-size: 12px;
  margin-top: 8px;
}
.card {
  margin-top: 14px;
  padding: 16px;
  border: 1px solid #e6e8ef;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.04);
}
.title {
  font-weight: 700;
  margin-bottom: 14px;
  font-size: 18px;
}
.field {
  display: grid;
  gap: 6px;
  margin-bottom: 12px;
  max-width: 400px;
}
.label {
  font-size: 13px;
  color: #4b5563;
  font-weight: 600;
}
.input {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d3d8e1;
  background: #ffffff;
  color: #111827;
}
.meta-list {
  display: grid;
  gap: 8px;
  margin-bottom: 14px;
}
.meta-item {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.meta-key {
  font-size: 12px;
  font-weight: 700;
  color: #4b5563;
  background: #f1f5f9;
  border: 1px solid #dbe2ea;
  border-radius: 999px;
  padding: 3px 10px;
}
.meta-value {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
}
.btn {
  padding: 10px 18px;
  border-radius: 10px;
  border: 1px solid #07c160;
  background: #07c160;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
}
.btn:hover:not(:disabled) {
  background: #06ad56;
  border-color: #06ad56;
}
.btn:disabled {
  opacity: 0.5;
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
</style>
