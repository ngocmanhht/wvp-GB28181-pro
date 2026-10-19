<template>
  <el-config-provider :size="'default'" :z-index="3000">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'

onMounted(() => {
  // Ensure dark class is active on root by default
  document.documentElement.classList.add('dark')
})
</script>

<style>
/* Global reset handled by main.scss */
</style>
