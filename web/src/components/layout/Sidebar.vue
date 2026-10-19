<template>
  <aside
    class="h-full bg-slate-900 border-r border-slate-800 flex flex-col transition-all duration-300 z-20 select-none"
    :class="appStore.sidebarCollapsed ? 'w-16' : 'w-64'"
  >
    <!-- Brand / Logo -->
    <div class="h-16 flex items-center px-4 border-b border-slate-800 bg-slate-900/90 gap-3">
      <div class="w-9 h-9 rounded-lg bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-blue-500/20 flex-shrink-0">
        <el-icon class="text-white text-xl"><VideoCamera /></el-icon>
      </div>
      <div v-show="!appStore.sidebarCollapsed" class="flex flex-col overflow-hidden whitespace-nowrap">
        <span class="font-bold text-base tracking-wide bg-gradient-to-r from-white via-slate-200 to-slate-400 bg-clip-text text-transparent">
          WVP-PRO
        </span>
        <span class="text-[10px] text-slate-400 font-medium tracking-wider uppercase">GB28181 Platform</span>
      </div>
    </div>

    <!-- Navigation Menu -->
    <div class="flex-1 overflow-y-auto overflow-x-hidden py-3">
      <el-menu
        :default-active="activeRoute"
        :collapse="appStore.sidebarCollapsed"
        background-color="transparent"
        text-color="#94a3b8"
        active-text-color="#38bdf8"
        class="border-none w-full custom-el-menu"
        router
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item :index="item.path" class="my-1 mx-2 rounded-lg hover:bg-slate-800/80 transition-colors">
            <el-icon class="text-lg">
              <component :is="item.icon" />
            </el-icon>
            <template #title>
              <span class="font-medium text-sm">{{ item.title }}</span>
            </template>
          </el-menu-item>
        </template>
      </el-menu>
    </div>

    <!-- Footer info -->
    <div v-show="!appStore.sidebarCollapsed" class="p-3 border-t border-slate-800 text-xs text-slate-400 flex items-center justify-between">
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
  Connection,
  Upload,
  Film,
  Location,
  Coin,
  Share,
  Bell,
  User,
  Cpu,
  Document
} from '@element-plus/icons-vue'

const route = useRoute()
const appStore = useAppStore()

const activeRoute = computed(() => {
  return route.path
})

const menuItems = [
  { path: '/dashboard', title: 'Dashboard', icon: Odometer },
  { path: '/live', title: 'Live Monitor', icon: VideoCamera },
  { path: '/channel', title: 'Channel List', icon: List },
  { path: '/device', title: 'GB Devices', icon: Monitor },
  { path: '/stream/proxy', title: 'Stream Proxy', icon: Connection },
  { path: '/stream/push', title: 'Stream Push', icon: Upload },
  { path: '/cloudRecord', title: 'Cloud Record', icon: Film },
  { path: '/map', title: 'GIS Map', icon: Location },
  { path: '/mediaServer', title: 'Media Nodes', icon: Coin },
  { path: '/platform', title: 'GB Cascade', icon: Share },
  { path: '/alarm', title: 'Alarm Log', icon: Bell },
  { path: '/user', title: 'Users & Roles', icon: User },
  { path: '/systemInfo', title: 'System Status', icon: Cpu },
  { path: '/logs', title: 'Real-time Logs', icon: Document },
]
</script>

<style scoped>
:deep(.el-menu-item.is-active) {
  background: rgba(56, 189, 248, 0.12) !important;
  color: #38bdf8 !important;
  border-left: 3px solid #38bdf8;
  font-weight: 600;
}
:deep(.el-menu--collapse .el-menu-item) {
  margin: 4px 8px;
  border-radius: 8px;
  justify-content: center;
}
</style>
