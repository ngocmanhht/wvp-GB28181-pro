<template>
  <el-config-provider :size="'default'" :z-index="3000">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

onMounted(() => {
  appStore.applyTheme(appStore.isDark)
})
</script>

<style>
/* Global reset handled by main.scss */
</style>
