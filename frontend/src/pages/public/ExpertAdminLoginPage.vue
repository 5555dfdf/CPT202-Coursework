<script setup>
import { onBeforeUnmount, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { api } from "@/api/client";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const email = ref("");
const password = ref("");
const verificationCode = ref("");
const loginMode = ref("password");
const loading = ref(false);
const error = ref("");
const sendingCode = ref(false);
const codeCountdown = ref(0);
let codeTimer = null;

function clearCodeTimer() {
  if (codeTimer) {
    clearInterval(codeTimer);
    codeTimer = null;
  }
}

onBeforeUnmount(() => {
  clearCodeTimer();
});

function switchLoginMode(mode) {
  if (mode !== "password" && mode !== "emailCode") return;
  if (loginMode.value === mode) return;
  loginMode.value = mode;
  error.value = "";
}

async function onSendCode() {
  if (!email.value || sendingCode.value || codeCountdown.value > 0) return;
  error.value = "";
  sendingCode.value = true;
  try {
    await api.sendRegisterEmailCode({ email: email.value, scene: "login" });
    codeCountdown.value = 60;
    clearCodeTimer();
    codeTimer = setInterval(() => {
      if (codeCountdown.value <= 1) {
        codeCountdown.value = 0;
        clearCodeTimer();
      } else {
        codeCountdown.value -= 1;
      }
    }, 1000);
  } catch (e) {
    error.value = e?.message || e?.data?.message || "Failed to send verification code";
  } finally {
    sendingCode.value = false;
  }
}

async function onSubmit() {
  error.value = "";
  if (!email.value.trim()) {
    error.value = "Email is required";
    return;
  }
  if (loginMode.value === "password" && !password.value) {
    error.value = "Password is required";
    return;
  }
  if (loginMode.value === "emailCode" && !verificationCode.value.trim()) {
    error.value = "Verification code is required";
    return;
  }
  loading.value = true;
  try {
    if (loginMode.value === "password") {
      await auth.login({ email: email.value, password: password.value });
    } else {
      await auth.loginByEmailCode({
        email: email.value,
        verificationCode: verificationCode.value,
      });
    }
    const next = typeof route.query.next === "string" ? route.query.next : "";
    if (next) await router.replace(next);
    else await router.replace(auth.defaultHomeRoute());
  } catch (e) {
    error.value = e?.message || "Login failed";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="login-shell expert-shell">
    <div class="background-layer" aria-hidden="true"></div>

    <div class="foreground-layer">
      <article class="login-card">
        <h1>Welcome back, Team</h1>
        <p class="subtitle">Log in as Expert/Admin</p>
        <div class="login-mode" role="tablist" aria-label="Login methods">
          <button
            type="button"
            class="mode-btn"
            :class="{ 'mode-btn--active': loginMode === 'password' }"
            @click="switchLoginMode('password')"
          >
            Password
          </button>
          <button
            type="button"
            class="mode-btn"
            :class="{ 'mode-btn--active': loginMode === 'emailCode' }"
            @click="switchLoginMode('emailCode')"
          >
            Email Code
          </button>
        </div>

        <form class="form" @submit.prevent="onSubmit">
          <label>
            <span class="sr-only">Email</span>
            <input
              v-model="email"
              type="email"
              autocomplete="email"
              placeholder="email"
              required
            />
          </label>

          <label v-if="loginMode === 'password'">
            <span class="sr-only">Password</span>
            <input
              v-model="password"
              type="password"
              autocomplete="current-password"
              placeholder="password"
              required
            />
          </label>
          <label v-else>
            <span class="sr-only">Verification code</span>
            <div class="code-row">
              <input
                v-model="verificationCode"
                type="text"
                inputmode="numeric"
                maxlength="6"
                placeholder="verification code"
                required
              />
              <button
                type="button"
                class="btn-secondary"
                :disabled="sendingCode || !email || codeCountdown > 0"
                @click="onSendCode"
              >
                {{
                  codeCountdown > 0
                    ? `Resend (${codeCountdown}s)`
                    : sendingCode
                      ? "Sending..."
                      : "Send Code"
                }}
              </button>
            </div>
          </label>

          <div v-if="error" class="error">{{ error }}</div>

          <button class="btn btn--team" type="submit" :disabled="loading">
            {{ loading ? "Logging in..." : "Log in" }}
          </button>
        </form>

        <p class="hint alt-hint">
          Not Expert/Admin?
          <router-link :to="{ name: 'login' }">Log as Customer</router-link>
        </p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #f0eae5;
}

.background-layer {
  position: absolute;
  inset: 0;
}

.background-layer {
  background-image: url("/images/login-02-bg.jpg");
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  filter: blur(6px) brightness(0.9);
  transform: scale(1.03);
}

.foreground-layer {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: clamp(72px, 12vh, 128px) clamp(80px, 10vw, 164px)
    clamp(36px, 7vh, 72px) clamp(28px, 6vw, 80px);
}

.login-card {
  width: clamp(280px, 23vw, 330px);
  height: auto;
  min-height: 0;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 0;
  box-shadow: 0 14px 36px rgba(18, 22, 30, 0.14);
  padding: clamp(32px, 3vw, 40px);
}

h1 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  line-height: 1.2;
  color: #202124;
}

.subtitle {
  margin: 8px 0 26px;
  font-size: 1.05rem;
  font-weight: 700;
  color: #202124;
}

.login-mode {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  margin-bottom: 14px;
}

.mode-btn {
  height: 36px;
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #f0eeee;
  color: #374151;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
}

.mode-btn--active {
  border-color: #a94442;
  color: #202124;
  background: #f3d9d8;
}

.form {
  display: grid;
  gap: 13px;
}

.code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: center;
}

input {
  width: 100%;
  height: 46px;
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #f0eeee;
  color: #111827;
  padding: 0 13px;
  outline: none;
}

input::placeholder {
  color: #6b7280;
}

.btn {
  margin-top: 6px;
  height: 46px;
  border: 1px solid #a94442;
  border-radius: 0;
  background: #a94442;
  color: #ffffff;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}

.btn--team {
  border-color: #a94442;
  background: #a94442;
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-secondary {
  height: 46px;
  padding: 0 12px;
  border: 1px solid #d8d1cb;
  border-radius: 0;
  background: #f0eeee;
  color: #111827;
  white-space: nowrap;
  cursor: pointer;
  font-size: 0.88rem;
  font-weight: 500;
}

.btn-secondary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.error {
  border: 1px solid rgba(217, 83, 60, 0.4);
  background: rgba(217, 83, 60, 0.12);
  border-radius: 0;
  padding: 10px 12px;
  color: #991b1b;
  font-size: 13px;
}

.hint {
  margin: 20px 0 0;
  font-size: 0.92rem;
  color: #4b5563;
}

.hint a {
  color: #202124;
  font-weight: 600;
  text-decoration: none;
}

.alt-hint {
  margin-top: 10px;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 900px) {
  .background-layer {
    background-position: center;
  }

  .foreground-layer {
    justify-content: center;
    align-items: flex-start;
    padding: clamp(34px, 8vh, 56px) 20px 24px;
  }

  .login-card {
    width: min(360px, 100%);
    padding: 30px 24px;
  }
}
</style>
