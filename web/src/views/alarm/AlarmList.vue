<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Header Controls -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow transition-colors">
      <div class="flex items-center gap-3 flex-wrap">
        <div class="w-8 h-8 rounded-lg bg-rose-500/10 text-rose-600 dark:text-rose-400 flex items-center justify-center font-bold">
          <el-icon><Bell /></el-icon>
        </div>
        <div>
          <h2 class="text-sm font-bold text-slate-800 dark:text-white">Security Alarms & Event Logs</h2>
          <p class="text-xs text-slate-500 dark:text-slate-400">Real-time alerts, sensor alarms and national standard video detections</p>
        </div>
      </div>

      <div class="flex items-center gap-2 flex-wrap">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="to"
          start-placeholder="Start date"
          end-placeholder="End date"
          value-format="YYYY-MM-DD"
          size="default"
          class="!w-64"
        />

        <el-button type="primary" size="default" @click="loadAlarms" class="!rounded-lg">
          <el-icon class="mr-1"><Search /></el-icon> Search
        </el-button>

        <el-button
          size="default"
          type="danger"
          plain
          :disabled="selectedIds.length === 0"
          @click="deleteSelected"
          class="!rounded-lg"
        >
          Delete Selected ({{ selectedIds.length }})
        </el-button>

        <el-button size="default" type="danger" @click="clearAllAlarms" class="!rounded-lg">
          Clear All
        </el-button>

        <el-button size="default" @click="loadAlarms" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Data Table -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow overflow-hidden transition-colors">
      <el-table
        :data="alarmList"
        v-loading="loading"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="deviceId" label="Device ID" min-width="170">
          <template #default="{ row }">
            <span class="font-mono text-xs text-blue-600 dark:text-blue-400 font-semibold">{{ row.deviceId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="channelId" label="Channel ID" min-width="160">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-700 dark:text-slate-300">{{ row.channelId || 'Global' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="alarmPriority" label="Priority" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.alarmPriority >= 3 ? 'danger' : 'warning'">
              Level {{ row.alarmPriority || 1 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmMethod" label="Method" width="130" align="center">
          <template #default="{ row }">
            <span class="text-xs font-medium">{{ row.alarmMethod || 'GB Sensor' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="alarmTime" label="Alarm Time" min-width="170">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-500 dark:text-slate-400">{{ row.alarmTime }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="alarmDescription" label="Description" min-width="220">
          <template #default="{ row }">
            <span class="text-xs text-slate-700 dark:text-slate-200">{{ row.alarmDescription || 'Motion / Tamper Alert' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.count"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadAlarms"
          @current-change="loadAlarms"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { alarmService } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Search, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const alarmList = ref<any[]>([])
const selectedIds = ref<(string | number)[]>([])
const dateRange = ref<[string, string] | null>(null)

const pagination = reactive({
  page: 1,
  count: 20,
  total: 0,
})

async function loadAlarms() {
  loading.value = true
  try {
    const res: any = await alarmService.getAlarmList({
      page: pagination.page,
      count: pagination.count,
      beginTime: dateRange.value ? `${dateRange.value[0]} 00:00:00` : undefined,
      endTime: dateRange.value ? `${dateRange.value[1]} 23:59:59` : undefined,
    })
    if (res && res.data) {
      alarmList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    console.warn('Alarm API warning:', e)
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(selection: any[]) {
  selectedIds.value = selection.map((s) => s.id)
}

function deleteSelected() {
  if (selectedIds.value.length === 0) return
  ElMessageBox.confirm(`Delete ${selectedIds.value.length} alarm records?`, 'Confirm Delete', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(async () => {
    await alarmService.deleteAlarms(selectedIds.value)
    ElMessage.success('Selected alarms deleted')
    loadAlarms()
  })
}

function clearAllAlarms() {
  ElMessageBox.confirm('Clear all security alarm logs in the system?', 'Clear All Alarms', {
    confirmButtonText: 'Clear All',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(async () => {
    await alarmService.clearAlarms()
    ElMessage.success('All alarm history cleared')
    loadAlarms()
  })
}

onMounted(() => {
  loadAlarms()
})
</script>
