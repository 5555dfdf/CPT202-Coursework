<script setup>
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
const router = useRouter();
const auth = useAuthStore();

async function loginAs(role) {
  auth.setToken("dev-token");
  auth.setUser({
    id: `dev-${role.toLowerCase()}-1`,
    role,
    name: `Dev ${role}`,
    email: `${role.toLowerCase()}@example.com`,
  });
  await router.replace(auth.defaultHomeRoute());
}
</script>

<template>
  <section class="card">
    <h1>Developer Quick Login</h1>
    <p class="muted">
      Use this page to enter each role directly when the backend is not
      connected.
    </p>

    <div class="grid">
      <button class="btn" @click="loginAs('Customer')">
        Enter as Customer
      </button>
      <button class="btn" @click="loginAs('Specialist')">
        Enter as Specialist
      </button>
      <button class="btn" @click="loginAs('Admin')">Enter as Admin</button>
    </div>

    <p class="hint">
      Entry path:
      <code>/dev-login</code>
    </p>
  </section>
</template>

<style scoped>
.dev-login-shell {
  display: flex;
  justify-content: center;
  padding: 40px 20px;
}

.dev-login-card {
  width: min(720px, 100%);
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 0;
  padding: 32px;
  box-shadow: 0 14px 36px rgba(18, 22, 30, 0.14);
}

h1 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
  line-height: 1.2;
  color: #202124;
}

.muted {
  margin: 8px 0 24px;
  font-size: 0.96rem;
  color: #4b5563;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.btn {
  height: 46px;
  padding: 0 12px;
  border-radius: 0;
  border: 1px solid #d9533c;
  background: #d9533c;
  color: #ffffff;
  font-size: 0.96rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.btn:hover {
  opacity: 0.92;
}

.hint {
  margin: 20px 0 0;
  font-size: 0.92rem;
  color: #4b5563;
}

code {
  background: #f7f4f2;
  color: #202124;
  padding: 2px 8px;
  border-radius: 0;
  border: 1px solid #d8d1cb;
  font-family: inherit;
}

@media (max-width: 880px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
