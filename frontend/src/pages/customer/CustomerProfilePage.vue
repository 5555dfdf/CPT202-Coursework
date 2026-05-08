<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useAuthStore } from "@/stores/auth";
import { api } from "@/api/client";

const auth = useAuthStore();
const name = ref("");
const originalName = ref("");
const editName = ref("");
const email = ref("");
const role = ref("");
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const ok = ref("");
const okTimer = ref(null);
const editOpen = ref(false);

const showPasswordForm = ref(false);
const pwdSaving = ref(false);
const pwdError = ref("");
const pwdOk = ref("");
const pwdOkTimer = ref(null);
const showCurrentPassword = ref(false);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);
const currentPassword = ref("");
const newPassword = ref("");
const confirmPassword = ref("");

const avatarPalette = [
  "#1f6feb",
  "#0e9f6e",
  "#dc6b19",
  "#b83ad8",
  "#d9486f",
  "#0f766e",
  "#7c3aed",
  "#2563eb",
];
const avatarLetter = computed(() => {
  const first = (name.value || email.value || "?").trim().charAt(0);
  return first ? first.toUpperCase() : "?";
});

const avatarStyle = computed(() => {
  const seed = (name.value || email.value || "?").trim();
  let hash = 0;
  for (let i = 0; i < seed.length; i += 1) {
    hash = (hash << 5) - hash + seed.charCodeAt(i);
    hash |= 0;
  }
  return {
    backgroundColor: avatarPalette[Math.abs(hash) % avatarPalette.length],
  };
});

function isAuthExpiredError(e) {
  const status = e?.status;
  if (status !== 401 && status !== 403) return false;
  const msg = String(e?.message || "").toLowerCase();
  return (
    msg.includes("token") ||
    msg.includes("expired") ||
    msg.includes("unauthorized") ||
    msg.includes("forbidden") ||
    msg.includes("登录") ||
    msg.includes("过期")
  );
}

function syncUserState(user) {
  name.value = user?.name ?? "";
  originalName.value = (user?.name ?? "").trim();
  editName.value = name.value;
  email.value = user?.email ?? "";
  role.value = user?.role ?? "";
  if (user) auth.setUser(user);
}

async function load() {
  error.value = "";
  ok.value = "";
  loading.value = true;
  try {
    const me = await api.getMe();
    syncUserState(me.user);
  } catch (e) {
    error.value = isAuthExpiredError(e)
      ? "Login expired. Please sign in again."
      : e?.message || "Failed to load";
    syncUserState(auth.user);
  } finally {
    loading.value = false;
  }
}

function showOkMessage(message, duration = 1600) {
  ok.value = message;
  if (okTimer.value) clearTimeout(okTimer.value);
  okTimer.value = setTimeout(() => {
    ok.value = "";
    okTimer.value = null;
  }, duration);
}

function showPwdOkMessage(message) {
  pwdOk.value = message;
  showOkMessage(message, 2500);
  if (pwdOkTimer.value) clearTimeout(pwdOkTimer.value);
  pwdOkTimer.value = setTimeout(() => {
    pwdOk.value = "";
    pwdOkTimer.value = null;
  }, 2500);
}

function openEdit() {
  editName.value = name.value;
  error.value = "";
  editOpen.value = true;
}

function closeEdit() {
  if (saving.value) return;
  editOpen.value = false;
}

async function onSave() {
  error.value = "";
  ok.value = "";
  const trimmedName = editName.value.trim();
  if (!trimmedName) {
    error.value = "Name cannot be empty.";
    return;
  }
  if (trimmedName === originalName.value) {
    showOkMessage("No changes to save.");
    editOpen.value = false;
    return;
  }

  saving.value = true;
  try {
    const me = await api.updateMe({ name: trimmedName });
    syncUserState(me.user ?? { ...auth.user, name: trimmedName });
    editOpen.value = false;
    showOkMessage("Updated successfully.");
  } catch (e) {
    error.value = isAuthExpiredError(e)
      ? "Login expired. Please sign in again."
      : e?.message || "Failed to save";
  } finally {
    saving.value = false;
  }
}

function togglePasswordForm() {
  showPasswordForm.value = !showPasswordForm.value;
  pwdError.value = "";
  pwdOk.value = "";
  if (!showPasswordForm.value) {
    showCurrentPassword.value = false;
    showNewPassword.value = false;
    showConfirmPassword.value = false;
    currentPassword.value = "";
    newPassword.value = "";
    confirmPassword.value = "";
  }
}

function stripPasswordWhitespace(field) {
  field.value = String(field.value || "").replace(/\s+/g, "");
}

function blockWhitespaceInput(event) {
  const text = event?.data ?? "";
  if (/\s/.test(text)) event.preventDefault();
}

function onPasswordPaste(event, field) {
  event.preventDefault();
  const pasted = String(event.clipboardData?.getData("text") || "").replace(
    /\s+/g,
    "",
  );
  const current = String(field.value || "");
  const target = event.target;
  const start =
    typeof target?.selectionStart === "number"
      ? target.selectionStart
      : current.length;
  const end =
    typeof target?.selectionEnd === "number" ? target.selectionEnd : start;
  field.value = `${current.slice(0, start)}${pasted}${current.slice(end)}`;
}

function onCurrentPasswordInput() {
  stripPasswordWhitespace(currentPassword);
  if (pwdError.value) pwdError.value = "";
}

function onNewPasswordInput() {
  stripPasswordWhitespace(newPassword);
  if (pwdError.value === "New password must be at least 8 characters.") {
    pwdError.value = "";
  }
}

function onNewPasswordBlur() {
  const next = newPassword.value.trim();
  if (!next) return;
  if (next.length < 8) {
    pwdError.value = "New password must be at least 8 characters.";
  }
}

async function onCurrentPasswordBlur() {
  const current = currentPassword.value.trim();
  if (!current) return;
  try {
    await api.verifyMyPassword({
      currentPassword: current,
      oldPassword: current,
    });
  } catch (e) {
    pwdError.value = isAuthExpiredError(e)
      ? "Login expired. Please sign in again."
      : e?.message || "Old password is incorrect";
  }
}

function onConfirmPasswordFocus() {
  if (!newPassword.value.trim()) {
    pwdError.value = "Please enter new password first.";
  }
}
async function onChangePassword() {
  ok.value = "";
  pwdError.value = "";
  pwdOk.value = "";
  const current = currentPassword.value.trim();
  const next = newPassword.value.trim();
  const confirm = confirmPassword.value.trim();

  if (!current) {
    pwdError.value = "Please enter current password.";
    return;
  }
  if (!next) {
    pwdError.value = "Please enter new password.";
    return;
  }
  if (next.length < 8) {
    pwdError.value = "New password must be at least 8 characters.";
    return;
  }
  if (!confirm) {
    pwdError.value = "Please confirm new password.";
    return;
  }
  if (next !== confirm) {
    pwdError.value = "New password and confirmation do not match.";
    return;
  }

  pwdSaving.value = true;
  try {
    await api.changeMyPassword({
      currentPassword: current,
      oldPassword: current,
      newPassword: next,
      confirmPassword: confirm,
    });
    currentPassword.value = "";
    newPassword.value = "";
    confirmPassword.value = "";
    showPasswordForm.value = false;
    showPwdOkMessage("Password updated successfully.");
  } catch (e) {
    pwdError.value = isAuthExpiredError(e)
      ? "Login expired. Please sign in again."
      : e?.message || "Failed to change password";
  } finally {
    pwdSaving.value = false;
  }
}

onMounted(load);

onBeforeUnmount(() => {
  if (okTimer.value) clearTimeout(okTimer.value);
  if (pwdOkTimer.value) clearTimeout(pwdOkTimer.value);
});
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Profile</h1>
      <p class="subtitle">
        Manage your basic account information and account security.
      </p>
    </header>

    <div v-if="error" class="banner banner--error" role="alert">
      {{ error }}
    </div>
    <div v-if="ok" class="banner banner--ok">{{ ok }}</div>

    <div class="card">
      <div class="title">Basic Info</div>
      <div v-if="loading" class="muted">Loading...</div>
      <template v-else>
        <div class="profile-head">
          <div class="avatar" :style="avatarStyle">{{ avatarLetter }}</div>
          <div class="profile-head__meta">
            <p class="profile-head__name">{{ name || "User" }}</p>
            <p class="muted small">{{ email || "No email" }}</p>
          </div>
        </div>

        <div class="card-main">
          <dl class="kv">
            <dt>Name</dt>
            <dd>{{ name || "—" }}</dd>
            <dt>Email</dt>
            <dd>{{ email || "—" }}</dd>
            <dt>Role</dt>
            <dd>{{ role || "—" }}</dd>
          </dl>
          <div class="card-action">
            <button type="button" class="btn" @click="openEdit">
              Edit Profile
            </button>
            <button
              type="button"
              class="btn btn--ghost"
              @click="togglePasswordForm"
            >
              {{
                showPasswordForm ? "Cancel Password Change" : "Change Password"
              }}
            </button>
          </div>
        </div>

        <div v-if="showPasswordForm" class="password-panel">
          <label class="field">
            <span class="label">Current Password</span>
            <div class="password-input-wrap">
              <input
                v-model="currentPassword"
                class="input input--password"
                :type="showCurrentPassword ? 'text' : 'password'"
                autocomplete="current-password"
                @blur="onCurrentPasswordBlur"
                @keydown.space.prevent
                @beforeinput="blockWhitespaceInput"
                @paste="onPasswordPaste($event, currentPassword)"
                @input="onCurrentPasswordInput"
              />
              <button
                type="button"
                class="password-toggle"
                :aria-label="
                  showCurrentPassword
                    ? 'Hide current password'
                    : 'Show current password'
                "
                @click="showCurrentPassword = !showCurrentPassword"
              >
                {{ showCurrentPassword ? "Hide" : "Show" }}
              </button>
            </div>
          </label>

          <label class="field">
            <span class="label">New Password</span>
            <div class="password-input-wrap">
              <input
                v-model="newPassword"
                class="input input--password"
                :type="showNewPassword ? 'text' : 'password'"
                autocomplete="new-password"
                @blur="onNewPasswordBlur"
                @keydown.space.prevent
                @beforeinput="blockWhitespaceInput"
                @paste="onPasswordPaste($event, newPassword)"
                @input="onNewPasswordInput"
              />
              <button
                type="button"
                class="password-toggle"
                :aria-label="
                  showNewPassword ? 'Hide new password' : 'Show new password'
                "
                @click="showNewPassword = !showNewPassword"
              >
                {{ showNewPassword ? "Hide" : "Show" }}
              </button>
            </div>
          </label>

          <label class="field">
            <span class="label">Confirm New Password</span>
            <div class="password-input-wrap">
              <input
                v-model="confirmPassword"
                class="input input--password"
                :type="showConfirmPassword ? 'text' : 'password'"
                autocomplete="new-password"
                @focus="onConfirmPasswordFocus"
                @keydown.space.prevent
                @beforeinput="blockWhitespaceInput"
                @paste="onPasswordPaste($event, confirmPassword)"
                @input="stripPasswordWhitespace(confirmPassword)"
              />
              <button
                type="button"
                class="password-toggle"
                :aria-label="
                  showConfirmPassword
                    ? 'Hide confirm password'
                    : 'Show confirm password'
                "
                @click="showConfirmPassword = !showConfirmPassword"
              >
                {{ showConfirmPassword ? "Hide" : "Show" }}
              </button>
            </div>
          </label>

          <div v-if="pwdError" class="banner banner--error" role="alert">
            {{ pwdError }}
          </div>
          <div v-if="pwdOk" class="banner banner--ok">{{ pwdOk }}</div>

          <button
            type="button"
            class="btn"
            :disabled="pwdSaving"
            @click="onChangePassword"
          >
            {{ pwdSaving ? "Submitting..." : "Confirm Password Change" }}
          </button>
        </div>
      </template>
    </div>

    <div v-if="editOpen" class="modal-backdrop" @click.self="closeEdit">
      <section class="modal-card">
        <h3 class="modal-title">Edit Profile</h3>
        <label class="field">
          <span class="label">Name</span>
          <input
            v-model="editName"
            class="input"
            type="text"
            maxlength="50"
            autocomplete="name"
          />
        </label>
        <div class="modal-footer">
          <button
            type="button"
            class="btn btn--ghost"
            :disabled="saving"
            @click="closeEdit"
          >
            Cancel
          </button>
          <button type="button" class="btn" :disabled="saving" @click="onSave">
            {{ saving ? "Saving..." : "Save" }}
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

.small {
  font-size: 12px;
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

.profile-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #ffffff;
  font-size: 20px;
  font-weight: 800;
}

.profile-head__meta {
  min-width: 0;
}

.profile-head__name {
  margin: 0 0 4px;
  color: #111827;
  font-size: 18px;
  font-weight: 700;
}

.card-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
}

.kv {
  margin: 0;
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

.password-input-wrap {
  position: relative;
}

.input--password {
  width: 100%;
  padding-right: 76px;
}

.input--password::-ms-reveal,
.input--password::-ms-clear {
  display: none;
}

.password-toggle {
  position: absolute;
  top: 50%;
  right: 10px;
  transform: translateY(-50%);
  border: 0;
  background: transparent;
  color: #4b5563;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  padding: 4px 6px;
}

.password-toggle:hover {
  color: #111827;
}

.btn {
  height: 40px;
  padding: 0 14px;
  border-radius: 0;
  border: 1px solid #a94442;
  background: #a94442;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.btn:hover:not(:disabled) {
  opacity: 0.92;
}

.btn:disabled {
  opacity: 0.6;
}

.btn--ghost {
  border: 1px solid #202124;
  background: #ffffff;
  color: #202124;
}

.card-action {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: flex-end;
  gap: 10px;
  min-width: 220px;
}

.password-panel {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed #d8d1cb;
  max-width: 420px;
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
  color: #065f46;
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
  .card-main,
  .card-action,
  .modal-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .card-action {
    min-width: 0;
  }

  .kv {
    grid-template-columns: 80px 1fr;
  }
}
</style>
