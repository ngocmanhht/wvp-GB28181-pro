<template>
  <div class="flex flex-col gap-4 select-none">
    <div class="p-4 bg-slate-900 border border-slate-800 rounded-xl flex items-center justify-between shadow">
      <div class="flex items-center gap-2">
        <span class="w-2.5 h-2.5 rounded-full bg-rose-500"></span>
        <span class="font-semibold text-sm text-white">Security Alarms & Notifications</span>
      </div>
      <el-button size="default" @click="loadAlarms" class="!rounded-lg !bg-slate-800 !text-slate-200 !border-slate-700">
        <el-icon class="mr-1"><Refresh /></el-icon> Refresh
      </el-button>
    </div>

    <div class="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow overflow-hidden">
      <el-table :data="alarmList" v-loading="loading" style="width: 100%">
        <el-table-column prop="deviceId" label="Device ID" min-width="160">
          <template #default="{ row }">
            <span class="font-mono text-xs text-blue-400">{{ row.deviceId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="alarmPriority" label="Priority" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="danger">{{ row.alarmPriority || 'Normal' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmMethod" label="Method" width="120" />
        <el-table-column prop="alarmTime" label="Alarm Time" min-width="160">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-400">{{ row.alarmTime }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="alarmDescription" label="Description" min-width="200" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const alarmList = ref<any[]>([])

async function loadAlarms() {
  loading.value = true
  try {
    const res: any = await request({ url: '/api/alarm/all', method: 'get', params: { page: 1, count: 50 } })
    if (res && res.data) {
      alarmList.value = res.data.list || []
    }
  } catch (e) {
    console.warn('Alarm API warning:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAlarms()
})
</script>
