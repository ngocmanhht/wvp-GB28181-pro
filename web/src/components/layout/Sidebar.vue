<template>
  <aside
    class="h-full bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 flex flex-col transition-all duration-300 z-20 select-none"
    :class="appStore.sidebarCollapsed ? 'w-16' : 'w-64'"
  >
    <!-- Brand / Logo -->
    <div class="h-16 flex items-center px-4 border-b border-slate-200 dark:border-slate-800 bg-white/90 dark:bg-slate-900/90 gap-3">
      <div class="w-9 h-9 rounded-lg bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-blue-500/20 flex-shrink-0">
        <el-icon class="text-white text-xl"><VideoCamera /></el-icon>
      </div>
      <div v-show="!appStore.sidebarCollapsed" class="flex flex-col overflow-hidden whitespace-nowrap">
        <span class="font-bold text-base tracking-wide text-slate-800 dark:text-white">
          WVP-PRO
        </span>
        <span class="text-[10px] text-slate-400 font-medium tracking-wider uppercase">GB28181 & JT1078</span>
      </div>
    </div>

    <!-- Navigation Menu -->
    <div class="flex-1 overflow-y-auto overflow-x-hidden py-3">
      <el-menu
        :default-active="activeRoute"
        :collapse="appStore.sidebarCollapsed"
        background-color="transparent"
        :text-color="appStore.isDark ? '#94a3b8' : '#475569'"
        active-text-color="#2563eb"
        class="border-none w-full custom-el-menu"
        router
      >
        <!-- Dashboard -->
        <el-menu-item index="/dashboard" class="my-1 mx-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-colors">
          <el-icon class="text-lg"><Odometer /></el-icon>
          <template #title>
            <span class="font-medium text-sm">Dashboard</span>
          </template>
        </el-menu-item>

        <!-- Live Monitor -->
        <el-menu-item index="/live" class="my-1 mx-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-colors">
          <el-icon class="text-lg"><VideoCamera /></el-icon>
          <template #title>
            <span class="font-medium text-sm">Split Live Monitor</span>
          </template>
        </el-menu-item>

        <!-- Channels -->
        <el-menu-item index="/channel" class="my-1 mx-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-colors">
          <el-icon class="text-lg"><List /></el-icon>
          <template #title>
            <span class="font-medium text-sm">Channel List</span>
          </template>
        </el-menu-item>

        <!-- Device Access Submenu -->
        <el-sub-menu index="device-group" class="my-1 mx-2 rounded-lg">
          <template #title>
            <el-icon class="text-lg"><Monitor /></el-icon>
            <span class="font-medium text-sm">Device Access</span>
          </template>
          <el-menu-item index="/device">GB28181 Devices</el-menu-item>
          <el-menu-item index="/jtDevice">JT/T 1078 Vehicles</el-menu-item>
          <el-menu-item index="/stream/push">Stream Push</el-menu-item>
          <el-menu-item index="/stream/proxy">Stream Proxy</el-menu-item>
        </el-sub-menu>

        <!-- Organizational Structure Submenu -->
        <el-sub-menu index="org-group" class="my-1 mx-2 rounded-lg">
          <template #title>
            <el-icon class="text-lg"><OfficeBuilding /></el-icon>
            <span class="font-medium text-sm">Organization</span>
          </template>
          <el-menu-item index="/org/region">Administrative Division</el-menu-item>
          <el-menu-item index="/org/group">Business Grouping</el-menu-item>
        </el-sub-menu>

        <!-- Recording & Storage Submenu -->
        <el-sub-menu index="record-group" class="my-1 mx-2 rounded-lg">
          <template #title>
            <el-icon class="text-lg"><Film /></el-icon>
            <span class="font-medium text-sm">Video Recording</span>
          </template>
          <el-menu-item index="/cloudRecord">Cloud Recording</el-menu-item>
          <el-menu-item index="/recordPlan">Recording Plan</el-menu-item>
        </el-sub-menu>

        <!-- GIS Map -->
        <el-menu-item index="/map" class="my-1 mx-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-colors">
          <el-icon class="text-lg"><Location /></el-icon>
          <template #title>
            <span class="font-medium text-sm">GIS Electronic Map</span>
          </template>
        </el-menu-item>

        <!-- Media Nodes -->
        <el-menu-item index="/mediaServer" class="my-1 mx-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-colors">
          <el-icon class="text-lg"><Coin /></el-icon>
          <template #title>
            <span class="font-medium text-sm">Media Nodes (ZLM)</span>
          </template>
        </el-menu-item>

        <!-- Cascade Platform -->
        <el-menu-item index="/platform" class="my-1 mx-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-colors">
          <el-icon class="text-lg"><Share /></el-icon>
          <template #title>
            <span class="font-medium text-sm">GB Cascade</span>
          </template>
        </el-menu-item>

        <!-- Alarm Management -->
        <el-menu-item index="/alarm" class="my-1 mx-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-colors">
          <el-icon class="text-lg"><Bell /></el-icon>
          <template #title>
            <span class="font-medium text-sm">Security Alarms</span>
          </template>
        </el-menu-item>

        <!-- User & Auth -->
        <el-menu-item index="/user" class="my-1 mx-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-colors">
          <el-icon class="text-lg"><User /></el-icon>
          <template #title>
            <span class="font-medium text-sm">User & API Keys</span>
          </template>
        </el-menu-item>

        <!-- Operations Center Submenu -->
        <el-sub-menu index="ops-group" class="my-1 mx-2 rounded-lg">
          <template #title>
            <el-icon class="text-lg"><Cpu /></el-icon>
            <span class="font-medium text-sm">Operations</span>
          </template>
          <el-menu-item index="/systemInfo">Platform Status</el-menu-item>
          <el-menu-item index="/logs">Live Real-time Log</el-menu-item>
          <el-menu-item index="/operations/history">Audit History Logs</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </div>

    <!-- Footer info -->
    <div v-show="!appStore.sidebarCollapsed" class="p-3 border-t border-slate-200 dark:border-slate-800 text-xs text-slate-500 dark:text-slate-400 flex items-center justify-between">
      <span class="flex items-center gap-1.5">
        <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
        Core Active
      </span>
      <span>v2.0 Pro</span>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import {
  Odometer,
  VideoCamera,
  List,
  Monitor,
  Location,
  Coin,
  Share,
  Bell,
  User,
  Cpu,
  Film,
  OfficeBuilding
} from '@element-plus/icons-vue'

const route = useRoute()
const appStore = useAppStore()

const activeRoute = computed(() => {
  return route.path
})
</script>

<style scoped>
:deep(.el-menu-item.is-active) {
  background: rgba(37, 99, 235, 0.1) !important;
  color: #2563eb !important;
  border-left: 3px solid #2563eb;
  font-weight: 600;
}
.dark :deep(.el-menu-item.is-active) {
  background: rgba(56, 189, 248, 0.12) !important;
  color: #38bdf8 !important;
  border-left: 3px solid #38bdf8;
}
:deep(.el-menu--collapse .el-menu-item) {
  margin: 4px 8px;
  border-radius: 8px;
  justify-content: center;
}
</style>
