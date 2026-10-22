<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Header -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow transition-colors">
      <div class="flex items-center gap-3 flex-wrap">
        <el-input
          v-model="searchQuery"
          placeholder="Filter App or Stream..."
          clearable
          size="default"
          class="!w-64"
          :prefix-icon="Search"
          @keyup.enter="loadRecords"
        />

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

        <el-button type="primary" size="default" @click="loadRecords" class="!rounded-lg">
          <el-icon class="mr-1"><Search /></el-icon> Search
        </el-button>
      </div>

      <div class="flex items-center gap-2">
        <el-button size="default" @click="openCutTaskModal" class="!rounded-lg">
          <el-icon class="mr-1"><Scissor /></el-icon> Slice & Export Clip
        </el-button>
        <el-button size="default" @click="loadRecords" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Data Table -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow overflow-hidden transition-colors">
      <el-table
        :data="recordList"
        v-loading="loading"
        style="width: 100%"
        class="custom-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="app" label="App" width="120">
          <template #default="{ row }">
            <span class="font-mono text-xs text-blue-600 dark:text-blue-400 font-semibold">{{ row.app }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="stream" label="Stream ID" min-width="160">
          <template #default="{ row }">
            <span class="font-mono font-medium text-slate-800 dark:text-slate-200">{{ row.stream }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="fileName" label="File Name" min-width="220">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-600 dark:text-slate-300">{{ row.fileName || row.filePath }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="fileSize" label="Size" width="110" align="center">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-500 dark:text-slate-400">{{ formatSize(row.fileSize) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="timeLen" label="Duration" width="110" align="center">
          <template #default="{ row }">
            <span class="font-mono text-xs text-emerald-600 dark:text-emerald-400">{{ formatDuration(row.timeLen) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="startTime" label="Start Time" min-width="160">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-500 dark:text-slate-400">{{ row.startTime }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Actions" width="200" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1.5">
              <el-button
                size="small"
                type="primary"
                plain
                @click="playRecord(row)"
              >
                Play
              </el-button>

              <el-button
                size="small"
                type="success"
                plain
                @click="downloadRecord(row)"
              >
                Download
              </el-button>

              <el-button
                size="small"
                type="danger"
                plain
                @click="deleteRecordItem(row)"
              >
                Delete
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.count"
          :total="pagination.total"
          :page-sizes="[15, 30, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadRecords"
          @current-change="loadRecords"
        />
      </div>
    </div>

    <!-- Playback Modal -->
    <el-dialog
      v-model="playModalVisible"
      :title="`Recording Playback: ${currentFile?.fileName || currentFile?.stream}`"
      width="780px"
      destroy-on-close
    >
      <div class="w-full h-[440px] bg-black rounded-lg overflow-hidden flex items-center justify-center">
        <PlayerTabs v-if="playerStreamInfo" :stream-info="playerStreamInfo" :show-tab="true" />
      </div>
    </el-dialog>

    <!-- Slice & Export Task Modal -->
    <el-dialog v-model="cutModalVisible" title="Create Video Clip Export Task" width="520px" destroy-on-close>
      <el-form label-width="120px" class="py-2 text-xs">
        <el-form-item label="App" required>
          <el-input v-model="cutForm.app" placeholder="live" />
        </el-form-item>
        <el-form-item label="Stream ID" required>
          <el-input v-model="cutForm.stream" placeholder="Stream Identifier" />
        </el-form-item>
        <el-form-item label="Start Time" required>
          <el-date-picker v-model="cutForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="Select clip start time" class="!w-full" />
        </el-form-item>
        <el-form-item label="End Time" required>
          <el-date-picker v-model="cutForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="Select clip end time" class="!w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="cutModalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="submitCutTask">Create Slice Task</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { recordService } from '@/api'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Scissor } from '@element-plus/icons-vue'

const searchQuery = ref('')
const dateRange = ref<[string, string] | null>(null)
const loading = ref(false)
const recordList = ref<any[]>([])
const selectedRecords = ref<any[]>([])

const pagination = reactive({
  page: 1,
  count: 15,
  total: 0,
})

const playModalVisible = ref(false)
const currentFile = ref<any>(null)
const playerStreamInfo = ref<any>(null)

// Slice Task Modal
const cutModalVisible = ref(false)
const cutForm = reactive({
  app: 'live',
  stream: '',
  startTime: '',
  endTime: '',
})

function formatSize(bytes: number) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function formatDuration(sec: number) {
  if (!sec) return '00:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

async function loadRecords() {
  loading.value = true
  try {
    const res: any = await recordService.getCloudRecordList({
      page: pagination.page,
      count: pagination.count,
      query: searchQuery.value || undefined,
      startTime: dateRange.value ? `${dateRange.value[0]} 00:00:00` : undefined,
      endTime: dateRange.value ? `${dateRange.value[1]} 23:59:59` : undefined,
    })
    if (res && res.data) {
      recordList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    console.warn('Failed to load cloud records:', e)
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(selection: any[]) {
  selectedRecords.value = selection
}

function playRecord(row: any) {
  currentFile.value = row
  const playUrl = row.mediaServerPath || row.filePath
  playerStreamInfo.value = {
    flv: playUrl,
    ws_flv: playUrl,
    rtc: playUrl,
  }
  playModalVisible.value = true
}

function downloadRecord(row: any) {
  const url = row.mediaServerPath || row.filePath
  if (url) {
    window.open(url, '_blank')
  }
}

function deleteRecordItem(row: any) {
  ElMessageBox.confirm(`Delete record file ${row.fileName || row.stream}?`, 'Confirm Delete', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(async () => {
    await recordService.deleteCloudRecords([row.id])
    ElMessage.success('Record file deleted')
    loadRecords()
  })
}

function openCutTaskModal() {
  cutForm.app = 'live'
  cutForm.stream = recordList.value[0]?.stream || ''
  cutForm.startTime = ''
  cutForm.endTime = ''
  cutModalVisible.value = true
}

async function submitCutTask() {
  if (!cutForm.stream || !cutForm.startTime || !cutForm.endTime) {
    ElMessage.warning('Please fill in all slice parameters')
    return
  }
  try {
    await recordService.addCloudRecordTask(cutForm)
    ElMessage.success('Video clip slicing task dispatched successfully')
    cutModalVisible.value = false
  } catch (e: any) {
    ElMessage.error(e.message || 'Failed to create slicing task')
  }
}

onMounted(() => {
  loadRecords()
})
</script>
