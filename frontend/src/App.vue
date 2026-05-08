<template>
  <router-view />
  <AlertModalHost />
  <ConfirmModalHost />
</template>

<script setup>
import AlertModalHost from '@/components/AlertModalHost.vue'
import ConfirmModalHost from '@/components/ConfirmModalHost.vue'
// app shell keeps router content and global modal hosts mounted once
</script>
