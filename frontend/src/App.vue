<template>
  <router-view />
  <AlertModalHost />
  <ConfirmModalHost />
</template>

<script setup>
import AlertModalHost from '@/components/AlertModalHost.vue'
import ConfirmModalHost from '@/components/ConfirmModalHost.vue'
</script>
