<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const authRouteNames = [
  "login",
  "register",
  "dev-login",
  "expert-admin-login",
  // Keep compatibility with component-style names if route naming changes later.
  "LoginPage",
  "RegisterPage",
  "DevLoginPage",
  "ExpertAdminLoginPage",
];

const isAuthHeroPage = computed(() =>
  authRouteNames.includes(String(route.name))
);
</script>

<template>
  <div class="public">
    <header class="public__header">
      <div class="brand">Schedly</div>
      <nav class="nav">
        <router-link to="/login">Login</router-link>
        <router-link to="/register">Register</router-link>
      </nav>
    </header>

    <main :class="['public__main', { 'public__main--login': isAuthHeroPage }]">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.public {
  min-height: 100vh;
  background: #f0eae5;
  color: #111827;
}

.public__header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(2px);
}

.brand {
  font-weight: 700;
  letter-spacing: 0.4px;
}

.nav {
  display: flex;
  gap: 12px;
}

.public__main {
  max-width: 980px;
  margin: 0 auto;
  padding: 92px 20px 60px;
}

.public__main--login {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0;
  min-height: 100vh;
}

a {
  color: #d9533c;
  text-decoration: none;
}

a.router-link-active {
  color: #d9533c;
  font-weight: 600;
}
</style>
