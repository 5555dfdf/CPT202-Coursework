<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/api/client'

const auth = useAuthStore()
const name = ref('')
const originalName = ref('')
const email = ref('')
const role = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const ok = ref('')
const okTimer = ref(null)
const showPasswordForm = ref(false)
const pwdSaving = ref(false)
const pwdError = ref('')
const pwdOk = ref('')
const pwdOkTimer = ref(null)
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const avatarPalette = [
  '#1f6feb',
  '#0e9f6e',
  '#dc6b19',
  '#b83ad8',
  '#d9486f',
  '#0f766e',
  '#7c3aed',
  '#2563eb'
]

const avatarLetter = computed(() => {
  const first = (name.value || email.value || '?').trim().charAt(0)
  return first ? first.toUpperCase() : '?'
})

const avatarStyle = computed(() => {
  const seed = (name.value || email.value || '?').trim()
  let hash = 0
  for (let i = 0; i < seed.length; i += 1) {
    hash = (hash << 5) - hash + seed.charCodeAt(i)
    hash |= 0
  }
  const index = Math.abs(hash) % avatarPalette.length
  return { backgroundColor: avatarPalette[index] }
})

function isAuthExpiredError(e) {
  const status = e?.status
  if (status !== 401 && status !== 403) return false
  const msg = String(e?.message || '').toLowerCase()
  return (
    msg.includes('token') ||
    msg.includes('expired') ||
    msg.includes('unauthorized') ||
    msg.includes('forbidden') ||
    msg.includes('µÇÂ¼') ||
    msg.includes('¹ýÆÚ')
  )
}

async function load() {
  error.value = ''
  ok.value = ''
  loading.value = true
  try {
    const me = await api.getMe()
    const u = me.user
    name.value = u?.name ?? ''
    originalName.value = (u?.name ?? '').trim()
    email.value = u?.email ?? ''
    role.value = u?.role ?? ''
    if (u) auth.setUser(u)
  } catch (e) {
    if (isAuthExpiredError(e)) {
      error.value = 'Login expired. Please sign in again.'
    } else {
      error.value = e?.message || 'Failed to load'
    }
    const u = auth.user
    name.value = u?.name ?? ''
    originalName.value = (u?.name ?? '').trim()
    email.value = u?.email ?? ''
    role.value = u?.role ?? ''
  } finally {
    loading.value = false
  }
}

async function onSave() {
  error.value = ''
  ok.value = ''
  const trimmedName = name.value.trim()
  if (!trimmedName) {
    error.value = 'Name cannot be empty.'
    return
  }
  if (trimmedName === originalName.value) {
    showOkMessage('No changes to save.', 1200)
    return
  }
  saving.value = true
  try {
    const me = await api.updateMe({ name: trimmedName })
    const u = me.user
    if (u) {
      auth.setUser(u)
      name.value = u?.name ?? ''
      originalName.value = (u?.name ?? '').trim()
      email.value = u?.email ?? ''
      role.value = u?.role ?? ''
    }
    showOkMessage('Updated successfully.', 1200)
  } catch (e) {
    if (isAuthExpiredError(e)) {
      error.value = 'Login expired. Please sign in again.'
    } else {
      error.value = e?.message || 'Failed to save'
    }
  } finally {
    saving.value = false
  }
}

function togglePasswordForm() {
  showPasswordForm.value = !showPasswordForm.value
  pwdError.value = ''
  pwdOk.value = ''
  if (!showPasswordForm.value) {
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  }
}

function stripPasswordWhitespace(field) {
  field.value = String(field.value || '').replace(/\s+/g, '')
}

function blockWhitespaceInput(event) {
  const text = event?.data ?? ''
  if (/\s/.test(text)) {
    event.preventDefault()
  }
}

function onPasswordPaste(event, field) {
  event.preventDefault()
  const pasted = String(event.clipboardData?.getData('text') || '').replace(/\s+/g, '')
  const current = String(field.value || '')
  const target = event.target
  const start = typeof target?.selectionStart === 'number' ? target.selectionStart : current.length
  const end = typeof target?.selectionEnd === 'number' ? target.selectionEnd : start
  field.value = `${current.slice(0, start)}${pasted}${current.slice(end)}`
}

function onCurrentPasswordInput() {
  stripPasswordWhitespace(currentPassword)
  if (pwdError.value) pwdError.value = ''
}

function onNewPasswordInput() {
  stripPasswordWhitespace(newPassword)
  if (pwdError.value === 'New password must be at least 8 characters.') {
    pwdError.value = ''
  }
}

function onNewPasswordBlur() {
  const newPasswordText = newPassword.value.trim()
  if (!newPasswordText) return
  if (newPasswordText.length < 8) {
    pwdError.value = 'New password must be at least 8 characters.'
  }
}

async function onCurrentPasswordBlur() {
  const currentPasswordText = currentPassword.value.trim()
  if (!currentPasswordText) return
  try {
    await api.verifyMyPassword({
      currentPassword: currentPasswordText,
      oldPassword: currentPasswordText
    })
  } catch (e) {
    if (isAuthExpiredError(e)) {
      pwdError.value = 'Login expired. Please sign in again.'
    } else {
      pwdError.value = e?.message || 'Old password is incorrect'
    }
  }
}

function onConfirmPasswordFocus() {
  if (!newPassword.value.trim()) {
    pwdError.value = 'Please enter new password first.'
  }
}

function showPwdOkMessage(message) {
  ok.value = message
  pwdOk.value = message
  if (okTimer.value) clearTimeout(okTimer.value)
  if (pwdOkTimer.value) clearTimeout(pwdOkTimer.value)
  okTimer.value = setTimeout(() => {
    ok.value = ''
    okTimer.value = null
  }, 2500)
  pwdOkTimer.value = setTimeout(() => {
    pwdOk.value = ''
    pwdOkTimer.value = null
  }, 2500)
}

function showOkMessage(message, duration = 1200) {
  ok.value = message
  if (okTimer.value) clearTimeout(okTimer.value)
  okTimer.value = setTimeout(() => {
    ok.value = ''
    okTimer.value = null
  }, duration)
}

async function onChangePassword() {
  ok.value = ''
  pwdError.value = ''
  pwdOk.value = ''
  const currentPasswordText = currentPassword.value.trim()
  const newPasswordText = newPassword.value.trim()
  const confirmPasswordText = confirmPassword.value.trim()

  if (!currentPasswordText) {
    pwdError.value = 'Please enter current password.'
    return
  }
  if (!newPasswordText) {
    pwdError.value = 'Please enter new password.'
    return
  }
  if (!confirmPasswordText) {
    pwdError.value = 'Please confirm new password.'
    return
  }

  pwdSaving.value = true
  try {
    await api.changeMyPassword({
      currentPassword: currentPassword.value,
      oldPassword: currentPassword.value,
      newPassword: newPassword.value,
      confirmPassword: confirmPassword.value
    })
    showPwdOkMessage('Password updated successfully.')
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    showPasswordForm.value = false
  } catch (e) {
    if (isAuthExpiredError(e)) {
      pwdError.value = 'Login expired. Please sign in again.'
    } else {
      pwdError.value = e?.message || 'Failed to change password'
    }
  } finally {
    pwdSaving.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => {
  if (okTimer.value) clearTimeout(okTimer.value)
  if (pwdOkTimer.value) clearTimeout(pwdOkTimer.value)
})
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Profile</h1>
    </header>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>
    <div v-if="ok" class="banner banner--ok">{{ ok }}</div>

    <div class="card">
      <div class="title">Basic Info</div>
      <div v-if="loading" class="muted">Loading...</div>
      <template v-else>
        <div class="profile-head">
          <div class="avatar" :style="avatarStyle">{{ avatarLetter }}</div>
          <div class="profile-head__meta">
            <p class="profile-head__name">{{ name || 'User' }}</p>
            <p class="muted small">{{ email || 'No email' }}</p>
          </div>
        </div>

        <label class="field">
          <span class="label">Name</span>
          <input v-model="name" class="input" type="text" autocomplete="name" />
        </label>

        <label class="field">
          <span class="label">Email (read-only)</span>
          <input :value="email" class="input input--readonly" type="email" readonly />
        </label>

        <label class="field">
          <span class="label">Role</span>
          <input :value="role" class="input input--readonly" type="text" readonly />
        </label>

        <div class="actions">
          <button type="button" class="btn" :disabled="saving" @click="onSave">
            {{ saving ? 'Saving...' : 'Save' }}
          </button>
          <button type="button" class="btn btn--secondary" @click="togglePasswordForm">
            {{ showPasswordForm ? 'Cancel Password Change' : 'Change Password' }}
          </button>
        </div>

        <div v-if="showPasswordForm" class="password-panel">
          <label class="field">
            <span class="label">Current Password</span>
            <input
              v-model="currentPassword"
              class="input"
              type="password"
              autocomplete="current-password"
              @blur="onCurrentPasswordBlur"
              @keydown.space.prevent
              @beforeinput="blockWhitespaceInput"
              @paste="onPasswordPaste($event, currentPassword)"
              @input="onCurrentPasswordInput"
            />
          </label>

          <label class="field">
            <span class="label">New Password</span>
            <input
              v-model="newPassword"
              class="input"
              type="password"
              autocomplete="new-password"
              @blur="onNewPasswordBlur"
              @keydown.space.prevent
              @beforeinput="blockWhitespaceInput"
              @paste="onPasswordPaste($event, newPassword)"
              @input="onNewPasswordInput"
            />
          </label>

          <label class="field">
            <span class="label">Confirm New Password</span>
            <input
              v-model="confirmPassword"
              class="input"
              type="password"
              autocomplete="new-password"
              @focus="onConfirmPasswordFocus"
              @keydown.space.prevent
              @beforeinput="blockWhitespaceInput"
              @paste="onPasswordPaste($event, confirmPassword)"
              @input="stripPasswordWhitespace(confirmPassword)"
            />
          </label>

          <div v-if="pwdError" class="banner banner--error" role="alert">{{ pwdError }}</div>
          <div v-if="pwdOk" class="banner banner--ok">{{ pwdOk }}</div>

          <button type="button" class="btn" :disabled="pwdSaving" @click="onChangePassword">
            {{ pwdSaving ? 'Submitting...' : 'Confirm Password Change' }}
          </button>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.page__header h1 {
  margin: 0 0 6px;
  font-size: 22px;
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
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
}
.title {
  font-weight: 700;
  margin-bottom: 12px;
}
.profile-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #ffffff;
  font-size: 20px;
  font-weight: 700;
}
.profile-head__meta {
  min-width: 0;
}
.profile-head__name {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}
.field {
  display: grid;
  gap: 6px;
  margin-bottom: 12px;
  max-width: 400px;
}
.label {
  font-size: 13px;
  opacity: 0.85;
}
.input {
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: #ffffff;
  color: #111827;
}
.input--readonly {
  opacity: 0.8;
  background: #f3f4f6;
  cursor: not-allowed;
}
.btn {
  padding: 10px 18px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.1);
  color: inherit;
  cursor: pointer;
}
.btn--secondary {
  background: rgba(255, 255, 255, 0.04);
}
.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.password-panel {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed rgba(255, 255, 255, 0.2);
  max-width: 400px;
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
