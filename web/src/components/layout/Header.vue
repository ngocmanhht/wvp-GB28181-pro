<template>
  <header class="h-16 bg-slate-900/90 border-b border-slate-800 backdrop-blur-md px-4 flex items-center justify-between z-10">
    <!-- Left: Collapse toggle + Breadcrumb -->
    <div class="flex items-center gap-4">
      <button
        @click="appStore.toggleSidebar"
        class="w-9 h-9 rounded-lg flex items-center justify-center text-slate-400 hover:text-white hover:bg-slate-800 transition"
      >
        <el-icon class="text-xl">
          <Fold v-if="!appStore.sidebarCollapsed" />
          <Expand v-else />
        </el-icon>
      </button>

      <!-- Breadcrumb -->
      <el-breadcrumb separator="/" class="text-sm hidden sm:flex">
        <el-breadcrumb-item :to="{ path: '/' }">Home</el-breadcrumb-item>
        <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- Right: Status indicator + System actions + Profile -->
    <div class="flex items-center gap-3">
      <!-- Media Server Status Pill -->
      <div class="hidden md:flex items-center gap-2 px-3 py-1 rounded-full bg-slate-800/80 border border-slate-700/60 text-xs">
        <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
        <span class="text-slate-300">ZLM Server: <strong class="text-emerald-400">Online</strong></span>
      </div>

      <!-- Fullscreen Toggle -->
      <button
        @click="toggleFullscreen"
        class="w-9 h-9 rounded-lg flex items-center justify-center text-slate-400 hover:text-white hover:bg-slate-800 transition"
        title="Toggle Fullscreen"
      >
        <el-icon class="text-lg"><FullScreen /></el-icon>
      </button>

      <!-- Dark / Light Theme Toggle -->
      <button
        @click="appStore.toggleTheme"
        class="w-9 h-9 rounded-lg flex items-center justify-center text-slate-400 hover:text-white hover:bg-slate-800 transition"
        title="Toggle Theme"
      >
        <el-icon class="text-lg">
          <Moon v-if="appStore.isDark" />
          <Sunny v-else />
        </el-icon>
      </button>

      <div class="h-5 w-[1px] bg-slate-800 mx-1"></div>

      <!-- User Profile Dropdown -->
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="flex items-center gap-2.5 cursor-pointer px-2 py-1.5 rounded-lg hover:bg-slate-800 transition">
          <div class="w-8 h-8 rounded-full bg-gradient-to-tr from-blue-500 to-cyan-400 flex items-center justify-center text-white font-bold text-sm shadow-md">
            {{ userInitial }}
          </div>
          <span class="text-sm font-medium text-slate-200 hidden sm:inline">{{ userStore.username || 'Admin' }}</span>
          <el-icon class="text-xs text-slate-400"><ArrowDown /></el-icon>
        </div>

        <template #dropdown>
          <el-dropdown-menu class="bg-slate-800 border-slate-700 text-slate-200">
            <el-dropdown-item command="password">
              <el-icon><Key /></el-icon>
              <span>Change Password</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout" class="text-rose-400">
              <el-icon><SwitchButton /></el-icon>
              <span>Sign Out</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import screenfull from 'screenfull'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Fold,
  Expand,
  FullScreen,
  Moon,
  Sunny,
  ArrowDown,
  Key,
  SwitchButton
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const currentTitle = computed(() => route.meta?.title as string || '')
const userInitial = computed(() => (userStore.username ? userStore.username[0].toUpperCase() : 'A'))

function toggleFullscreen() {
  if (screenfull.isEnabled) {
    screenfull.toggle()
  } else {
    ElMessage.warning('Fullscreen not supported in this browser')
  }
}

async function handleCommand(command: string) {
  if (command === 'logout') {
    ElMessageBox.confirm('Are you sure you want to sign out?', 'Confirm Sign Out', {
      confirmButtonText: 'Sign Out',
      cancelButtonText: 'Cancel',
      type: 'warning'
    }).then(async () => {
      await userStore.logout()
      router.push('/login')
      ElMessage.success('Signed out successfully')
    })
  } else if (command === 'password') {
    ElMessage.info('Please visit User Management to change your password')
    router.push('/user')
  }
}
</script>
