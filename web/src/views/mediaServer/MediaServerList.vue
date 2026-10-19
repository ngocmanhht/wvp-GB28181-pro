<template>
  <div class="flex flex-col gap-4 select-none">
    <div class="p-4 bg-slate-900 border border-slate-800 rounded-xl flex items-center justify-between shadow">
      <div class="flex items-center gap-2">
        <span class="w-2.5 h-2.5 rounded-full bg-purple-500"></span>
        <span class="font-semibold text-sm text-white">ZLMediaKit Server Nodes</span>
      </div>
      <el-button size="default" @click="loadServers" class="!rounded-lg !bg-slate-800 !text-slate-200 !border-slate-700">
        <el-icon class="mr-1"><Refresh /></el-icon> Refresh
      </el-button>
    </div>

    <!-- Media Server Cards Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4" v-loading="loading">
      <div
        v-for="node in serverList"
        :key="node.id"
        class="bg-slate-900 border border-slate-800 rounded-xl p-5 flex flex-col gap-4 shadow hover:border-slate-700 transition"
      >
        <div class="flex items-center justify-between border-b border-slate-800 pb-3">
          <div class="flex items-center gap-2.5">
            <div class="w-9 h-9 rounded-lg bg-purple-500/10 text-purple-400 flex items-center justify-center font-bold">
              <el-icon><Coin /></el-icon>
            </div>
            <div class="flex flex-col">
              <span class="font-bold text-sm text-slate-100">{{ node.id || 'Default Node' }}</span>
              <span class="text-xs text-slate-400 font-mono">{{ node.ip }}:{{ node.httpPort }}</span>
            </div>
          </div>

          <span
            class="px-2.5 py-0.5 rounded-full text-xs font-semibold"
            :class="node.status ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-rose-500/10 text-rose-400'"
          >
            {{ node.status ? 'Online' : 'Offline' }}
          </span>
        </div>

        <div class="grid grid-cols-2 gap-2 text-xs">
          <div class="p-2 rounded bg-slate-950/60 flex flex-col">
            <span class="text-slate-500">RTSP Port</span>
            <span class="font-mono text-slate-200 font-semibold">{{ node.rtspPort || 554 }}</span>
          </div>
          <div class="p-2 rounded bg-slate-950/60 flex flex-col">
            <span class="text-slate-500">RTMP Port</span>
            <span class="font-mono text-slate-200 font-semibold">{{ node.rtmpPort || 1935 }}</span>
          </div>
          <div class="p-2 rounded bg-slate-950/60 flex flex-col">
            <span class="text-slate-500">RTP Port Range</span>
            <span class="font-mono text-slate-200 font-semibold">{{ node.rtpPortRange || '30000-30500' }}</span>
          </div>
          <div class="p-2 rounded bg-slate-950/60 flex flex-col">
            <span class="text-slate-500">Auto Close Inactive</span>
            <span class="font-mono text-slate-200 font-semibold">{{ node.autoConfig ? 'Enabled' : 'Disabled' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { systemService } from '@/api'
import { Coin, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const serverList = ref<any[]>([])

async function loadServers() {
  loading.value = true
  try {
    const res: any = await systemService.getMediaServerList()
    if (res && res.data) {
      serverList.value = res.data || []
    }
  } catch (e) {
    console.error('Failed to load media servers:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadServers()
})
</script>
