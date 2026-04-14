<script setup>
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { api } from "@/api/client";

const auth = useAuthStore();
const router = useRouter();

const name = ref("");
const email = ref("");
const password = ref("");
const verificationCode = ref("");
const loading = ref(false);
const error = ref("");
const sendingCode = ref(false);
const codeCountdown = ref(0);
const passwordFocused = ref(false);
const passwordInteracted = ref(false);
const passwordError = ref("");

const PASSWORD_VALIDATION_MESSAGE =
  "Password must be 8-20 characters and include uppercase, lowercase, and a number.";

const passwordRules = computed(() => [
  {
    key: "length",
    label: "8-20 characters",
    passed: password.value.length >= 8 && password.value.length <= 20,
  },
  {
    key: "uppercase",
    label: "At least one uppercase letter",
    passed: /[A-Z]/.test(password.value),
  },
  {
    key: "lowercase",
    label: "At least one lowercase letter",
    passed: /[a-z]/.test(password.value),
  },
  {
    key: "number",
    label: "At least one number",
    passed: /\d/.test(password.value),
  },
]);

const isPasswordValid = computed(() =>
  passwordRules.value.every((rule) => rule.passed)
);

const showPasswordHelper = computed(
  () => passwordFocused.value && passwordInteracted.value
);

function onPasswordFocus() {
  passwordFocused.value = true;
  passwordInteracted.value = true;
}

function onPasswordBlur() {
  passwordFocused.value = false;
  if (!isPasswordValid.value) {
    passwordError.value = PASSWORD_VALIDATION_MESSAGE;
    return;
  }
  passwordError.value = "";
}

function ensurePasswordValid() {
  passwordInteracted.value = true;
  if (!isPasswordValid.value) {
    passwordError.value = PASSWORD_VALIDATION_MESSAGE;
    return false;
  }
  passwordError.value = "";
  return true;
}

watch(password, () => {
  if (!passwordInteracted.value) return;
  if (isPasswordValid.value) {
    passwordError.value = "";
  }
});

async function onSendCode() {
  if (!email.value || sendingCode.value || codeCountdown.value > 0) return;
  error.value = "";
  sendingCode.value = true;
  try {
    await api.sendRegisterEmailCode({ email: email.value, scene: "register" });
    codeCountdown.value = 60;
    const timer = setInterval(() => {
      if (codeCountdown.value <= 1) {
        codeCountdown.value = 0;
        clearInterval(timer);
      } else {
        codeCountdown.value -= 1;
      }
    }, 1000);
  } catch (e) {
    error.value =
      e?.message ||
      e?.data?.message ||
      "The verification code failed to be sent.";
  } finally {
    sendingCode.value = false;
  }
}

async function onSubmit() {
  error.value = "";
  if (!ensurePasswordValid()) return;
  loading.value = true;
  try {
    await auth.register({
      name: name.value,
      email: email.value,
      password: password.value,
      verificationCode: verificationCode.value,
    });
    await router.replace(auth.defaultHomeRoute());
  } catch (e) {
    error.value = e?.message || "Registration failed";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="register-shell">
    <div class="background-layer" aria-hidden="true"></div>
    <div class="overlay-layer" aria-hidden="true"></div>

    <div class="foreground-layer">
      <article class="register-card">
        <h1>Create account</h1>
        <p class="subtitle">Register with Schedly</p>

        <form class="form" @submit.prevent="onSubmit">
          <label>
            <span class="sr-only">Name</span>
            <input
              v-model="name"
              type="text"
              autocomplete="name"
              placeholder="name"
              required
            />
          </label>

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

          <label>
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

          <label class="password-group">
            <span class="sr-only">Password</span>
            <input
              v-model="password"
              type="password"
              autocomplete="new-password"
              placeholder="password"
              required
              @focus="onPasswordFocus"
              @blur="onPasswordBlur"
            />

            <div
              v-if="showPasswordHelper"
              class="password-helper"
              role="status"
              aria-live="polite"
            >
              <p class="password-helper__title">Password must include:</p>
              <ul class="password-helper__list">
                <li
                  v-for="rule in passwordRules"
                  :key="rule.key"
                  :class="[
                    'password-helper__item',
                    { 'password-helper__item--valid': rule.passed },
                  ]"
                >
                  {{ rule.label }}
                </li>
              </ul>
            </div>
            <div v-if="passwordError" class="password-error">{{ passwordError }}</div>
          </label>

          <div v-if="error" class="error">{{ error }}</div>

          <button class="btn" type="submit" :disabled="loading">
            {{ loading ? "Registering..." : "Register" }}
          </button>
        </form>

        <p class="hint">
          Already have an account?
          <router-link to="/login">Log in</router-link>
        </p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.register-shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #f0eae5;
}

.background-layer,
.overlay-layer {
  position: absolute;
  inset: 0;
}

.background-layer {
  background-image: url("/images/register-bg.jpg");
  background-size: cover;
  background-position: 16% 56%;
  background-repeat: no-repeat;
  filter: blur(6px) brightness(0.96);
  transform: scale(1.03);
}

.overlay-layer {
  background: rgba(240, 234, 229, 0.14);
}

.foreground-layer {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(72px, 12vh, 128px) clamp(80px, 10vw, 164px)
    clamp(36px, 7vh, 72px) clamp(28px, 6vw, 80px);
}

.register-card {
  width: clamp(280px, 23vw, 330px);
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

.password-group {
  display: grid;
  gap: 8px;
}

.password-helper {
  border: 1px solid #ddd3cb;
  background: #f8f5f2;
  padding: 10px 12px;
}

.password-helper__title {
  margin: 0 0 6px;
  font-size: 0.85rem;
  color: #4b5563;
}

.password-helper__list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 4px;
}

.password-helper__item {
  font-size: 0.84rem;
  color: #6b7280;
}

.password-helper__item--valid {
  color: #166534;
}

.password-error {
  font-size: 0.84rem;
  color: #b42318;
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
  border: 1px solid #d9533c;
  border-radius: 0;
  background: #d9533c;
  color: #ffffff;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
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
    background-position: 24% 56%;
  }

  .foreground-layer {
    align-items: flex-start;
    padding: clamp(34px, 8vh, 56px) 20px 24px;
  }

  .register-card {
    width: min(360px, 100%);
    padding: 30px 24px;
  }
}
</style>
