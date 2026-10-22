<template>
  <div class="flex flex-col gap-4 select-none">
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex items-center justify-between shadow transition-colors">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold">
          <el-icon><Cpu /></el-icon>
        </div>
        <div>
          <h2 class="text-sm font-bold text-slate-800 dark:text-white">System Environment & Runtime Metrics</h2>
          <p class="text-xs text-slate-500 dark:text-slate-400">Server hardware, operating system, and JVM memory statistics</p>
        </div>
      </div>
      <el-button size="default" @click="loadInfo" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
        <el-icon class="mr-1"><Refresh /></el-icon> Refresh
      </el-button>
    </div>

    <!-- Cards Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4" v-loading="loading">
      <!-- 1. OS & Hardware -->
      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 flex flex-col gap-3 shadow transition-colors">
        <span class="font-semibold text-sm text-slate-800 dark:text-slate-200 border-b border-slate-200 dark:border-slate-800 pb-2">Operating System</span>
        <div class="flex flex-col gap-2 text-xs">
          <div class="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800/50">
            <span class="text-slate-400">OS Name</span>
            <span class="font-mono text-slate-800 dark:text-slate-300">{{ info.osName || 'Linux' }}</span>
          </div>
          <div class="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800/50">
            <span class="text-slate-400">OS Architecture</span>
            <span class="font-mono text-slate-800 dark:text-slate-300">{{ info.osArch || 'x86_64 / arm64' }}</span>
          </div>
          <div class="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800/50">
            <span class="text-slate-400">CPU Core Count</span>
            <span class="font-mono text-emerald-600 dark:text-emerald-400 font-bold">{{ info.cpuNum || 8 }} Cores</span>
          </div>
        </div>
      </div>

      <!-- 2. JVM Runtime -->
      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 flex flex-col gap-3 shadow transition-colors">
        <span class="font-semibold text-sm text-slate-800 dark:text-slate-200 border-b border-slate-200 dark:border-slate-800 pb-2">Java Virtual Machine</span>
        <div class="flex flex-col gap-2 text-xs">
          <div class="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800/50">
            <span class="text-slate-400">Java Version</span>
            <span class="font-mono text-blue-600 dark:text-blue-400 font-semibold">{{ info.javaVersion || '21 / 17' }}</span>
          </div>
          <div class="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800/50">
            <span class="text-slate-400">Heap Total</span>
            <span class="font-mono text-slate-800 dark:text-slate-300">{{ info.totalMemory || '1024 MB' }}</span>
          </div>
          <div class="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800/50">
            <span class="text-slate-400">Heap Free</span>
            <span class="font-mono text-emerald-600 dark:text-emerald-400">{{ info.freeMemory || '512 MB' }}</span>
          </div>
        </div>
      </div>

      <!-- 3. WVP & SIP Gateway -->
      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 flex flex-col gap-3 shadow transition-colors">
        <span class="font-semibold text-sm text-slate-800 dark:text-slate-200 border-b border-slate-200 dark:border-slate-800 pb-2">WVP-PRO Platform</span>
        <div class="flex flex-col gap-2 text-xs">
          <div class="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800/50">
            <span class="text-slate-400">Server ID</span>
            <span class="font-mono text-slate-800 dark:text-slate-300">{{ info.serverId || 'WVP-PRO-01' }}</span>
          </div>
          <div class="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800/50">
            <span class="text-slate-400">SIP Port</span>
            <span class="font-mono text-slate-800 dark:text-slate-300">5060 (UDP/TCP)</span>
          </div>
          <div class="flex justify-between py-1 border-b border-slate-100 dark:border-slate-800/50">
            <span class="text-slate-400">Frontend Stack</span>
            <span class="font-mono text-blue-600 dark:text-blue-400 font-bold">Vue 3 + Vite + Tailwind</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { systemService } from '@/api'
import { Cpu, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const info = ref<any>({})

async function loadInfo() {
  loading.value = true
  try {
    const res: any = await systemService.getSystemInfo()
    if (res && res.data) {
      info.value = res.data
    }
  } catch (e) {
    console.warn('System info load error:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadInfo()
})
</script>
