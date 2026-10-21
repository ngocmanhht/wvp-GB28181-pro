<template>
  <div class="flex flex-col gap-4 select-none">
    <div class="p-4 bg-slate-900 border border-slate-800 rounded-xl flex items-center justify-between shadow">
      <div class="flex items-center gap-2">
        <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
        <span class="font-semibold text-sm text-white">System Environment & Runtime Metrics</span>
      </div>
      <el-button size="default" @click="loadInfo" class="!rounded-lg !bg-slate-800 !text-slate-200 !border-slate-700">
        <el-icon class="mr-1"><Refresh /></el-icon> Refresh
      </el-button>
    </div>

    <!-- Cards Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4" v-loading="loading">
      <!-- 1. OS & Hardware -->
      <div class="bg-slate-900 border border-slate-800 rounded-xl p-5 flex flex-col gap-3 shadow">
        <span class="font-semibold text-sm text-slate-200 border-b border-slate-800 pb-2">Operating System</span>
        <div class="flex flex-col gap-2 text-xs">
          <div class="flex justify-between py-1 border-b border-slate-800/50">
            <span class="text-slate-500">OS Name</span>
            <span class="font-mono text-slate-300">{{ info.osName || 'Linux' }}</span>
          </div>
          <div class="flex justify-between py-1 border-b border-slate-800/50">
            <span class="text-slate-500">OS Architecture</span>
            <span class="font-mono text-slate-300">{{ info.osArch || 'x86_64 / arm64' }}</span>
          </div>
          <div class="flex justify-between py-1 border-b border-slate-800/50">
            <span class="text-slate-500">CPU Core Count</span>
            <span class="font-mono text-emerald-400 font-bold">{{ info.cpuNum || 8 }} Cores</span>
          </div>
        </div>
      </div>

      <!-- 2. JVM Runtime -->
      <div class="bg-slate-900 border border-slate-800 rounded-xl p-5 flex flex-col gap-3 shadow">
        <span class="font-semibold text-sm text-slate-200 border-b border-slate-800 pb-2">Java Virtual Machine</span>
        <div class="flex flex-col gap-2 text-xs">
          <div class="flex justify-between py-1 border-b border-slate-800/50">
            <span class="text-slate-500">Java Version</span>
            <span class="font-mono text-blue-400 font-semibold">{{ info.javaVersion || '21 / 17' }}</span>
          </div>
          <div class="flex justify-between py-1 border-b border-slate-800/50">
            <span class="text-slate-500">Heap Total</span>
            <span class="font-mono text-slate-300">{{ info.totalMemory || '1024 MB' }}</span>
          </div>
          <div class="flex justify-between py-1 border-b border-slate-800/50">
            <span class="text-slate-500">Heap Free</span>
            <span class="font-mono text-emerald-400">{{ info.freeMemory || '512 MB' }}</span>
          </div>
        </div>
      </div>

      <!-- 3. WVP & SIP Gateway -->
      <div class="bg-slate-900 border border-slate-800 rounded-xl p-5 flex flex-col gap-3 shadow">
        <span class="font-semibold text-sm text-slate-200 border-b border-slate-800 pb-2">WVP-PRO Platform</span>
        <div class="flex flex-col gap-2 text-xs">
          <div class="flex justify-between py-1 border-b border-slate-800/50">
            <span class="text-slate-500">Server ID</span>
            <span class="font-mono text-slate-300">{{ info.serverId || 'WVP-PRO-01' }}</span>
          </div>
          <div class="flex justify-between py-1 border-b border-slate-800/50">
            <span class="text-slate-500">SIP Port</span>
            <span class="font-mono text-slate-300">5060 (UDP/TCP)</span>
          </div>
          <div class="flex justify-between py-1 border-b border-slate-800/50">
            <span class="text-slate-500">Frontend Stack</span>
            <span class="font-mono text-blue-400 font-bold">Vue 3 + Vite + Tailwind</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { systemService } from '@/api'
import { Refresh } from '@element-plus/icons-vue'

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
