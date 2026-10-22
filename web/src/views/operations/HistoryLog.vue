<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Filter Header -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow transition-colors">
      <div class="flex items-center gap-3 flex-wrap">
        <el-input
          v-model="searchQuery"
          placeholder="Filter logs..."
          clearable
          size="default"
          class="!w-60"
          :prefix-icon="Search"
          @keyup.enter="loadHistoryLogs"
        />

        <el-select v-model="logType" placeholder="Level" clearable size="default" class="!w-32">
          <el-option label="All Levels" value="" />
          <el-option label="INFO" value="INFO" />
          <el-option label="WARN" value="WARN" />
          <el-option label="ERROR" value="ERROR" />
        </el-select>

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

        <el-button type="primary" size="default" @click="loadHistoryLogs" class="!rounded-lg">
          <el-icon class="mr-1"><Search /></el-icon> Search
        </el-button>
      </div>

      <div class="flex items-center gap-2">
        <el-button size="default" @click="loadHistoryLogs" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Logs Table -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow overflow-hidden transition-colors">
      <el-table :data="logList" v-loading="loading" style="width: 100%" size="small">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="time" label="Timestamp" width="180">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-500 dark:text-slate-400">{{ row.time || row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="Level" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getTagType(row.type || row.level)">
              {{ row.type || row.level || 'INFO' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="Log Message / Event Detail" min-width="320">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-700 dark:text-slate-200">{{ row.description || row.message }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="User" width="120" align="center">
          <template #default="{ row }">
            <span class="text-xs font-semibold text-slate-600 dark:text-slate-300">{{ row.username || 'System' }}</span>
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
          @size-change="loadHistoryLogs"
          @current-change="loadHistoryLogs"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { systemService } from '@/api'
import { Search, Refresh } from '@element-plus/icons-vue'

const searchQuery = ref('')
const logType = ref('')
const dateRange = ref<[string, string] | null>(null)
const loading = ref(false)
const logList = ref<any[]>([])

const pagination = reactive({
  page: 1,
  count: 20,
  total: 0,
})

function getTagType(type: string) {
  if (type === 'ERROR') return 'danger'
  if (type === 'WARN') return 'warning'
  return 'info'
}

async function loadHistoryLogs() {
  loading.value = true
  try {
    const res: any = await systemService.getHistoryLogs({
      page: pagination.page,
      count: pagination.count,
      query: searchQuery.value || undefined,
      type: logType.value || undefined,
      startTime: dateRange.value ? `${dateRange.value[0]} 00:00:00` : undefined,
      endTime: dateRange.value ? `${dateRange.value[1]} 23:59:59` : undefined,
    })
    if (res && res.data) {
      logList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    console.warn('Failed to load history logs:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadHistoryLogs()
})
</script>
