<template>
  <div class="flex flex-col gap-4 select-none">
    <div class="p-4 bg-slate-900 border border-slate-800 rounded-xl flex items-center justify-between shadow">
      <div class="flex items-center gap-2">
        <span class="w-2.5 h-2.5 rounded-full bg-blue-500"></span>
        <span class="font-semibold text-sm text-white">GB28181 Cascading Upstream Platforms</span>
      </div>
      <el-button size="default" @click="loadPlatforms" class="!rounded-lg !bg-slate-800 !text-slate-200 !border-slate-700">
        <el-icon class="mr-1"><Refresh /></el-icon> Refresh
      </el-button>
    </div>

    <!-- Data Table -->
    <div class="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow overflow-hidden">
      <el-table :data="platformList" v-loading="loading" style="width: 100%">
        <el-table-column prop="serverGBId" label="Platform SIP ID" min-width="180">
          <template #default="{ row }">
            <span class="font-mono text-xs font-semibold text-blue-400">{{ row.serverGBId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="Platform Name" min-width="150" />
        <el-table-column prop="serverIP" label="Upstream IP" min-width="140">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-300">{{ row.serverIP }}:{{ row.serverPort }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="120" align="center">
          <template #default="{ row }">
            <span
              class="px-2.5 py-0.5 rounded-full text-xs font-semibold"
              :class="row.status ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-slate-800 text-slate-500'"
            >
              {{ row.status ? 'Registered' : 'Offline' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="transport" label="Transport" width="110" align="center" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { systemService } from '@/api'
import { Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const platformList = ref<any[]>([])

async function loadPlatforms() {
  loading.value = true
  try {
    const res: any = await systemService.getPlatformList({ page: 1, count: 50 })
    if (res && res.data) {
      platformList.value = res.data.list || []
    }
  } catch (e) {
    console.error('Failed to load platforms:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPlatforms()
})
</script>
