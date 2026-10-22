<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Header -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow transition-colors">
      <div class="flex items-center gap-3 flex-wrap">
        <el-input
          v-model="searchQuery"
          placeholder="Filter Stream Push..."
          clearable
          size="default"
          class="!w-64"
          :prefix-icon="Search"
          @keyup.enter="loadPushes"
        />

        <el-button type="primary" size="default" @click="loadPushes" class="!rounded-lg">
          <el-icon class="mr-1"><Search /></el-icon> Search
        </el-button>
      </div>

      <div class="flex items-center gap-2 flex-wrap">
        <el-button type="success" size="default" @click="openPushUrlGenerator" class="!rounded-lg">
          <el-icon class="mr-1"><Upload /></el-icon> Generate Push URL
        </el-button>
        <el-button size="default" @click="triggerExcelImport" class="!rounded-lg">
          <el-icon class="mr-1"><DocumentAdd /></el-icon> Import Excel
        </el-button>
        <input type="file" ref="fileInputRef" class="hidden" accept=".xlsx,.xls" @change="handleFileSelected" />
        <el-button size="default" @click="exportExcel" class="!rounded-lg">
          <el-icon class="mr-1"><Download /></el-icon> Export Excel
        </el-button>
        <el-button size="default" @click="loadPushes" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Push Data Table -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow overflow-hidden transition-colors">
      <el-table
        :data="pushList"
        v-loading="loading"
        style="width: 100%"
        class="custom-table"
      >
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

        <el-table-column prop="pushing" label="Ingest Status" width="130" align="center">
          <template #default="{ row }">
            <span
              v-if="row.pushing"
              class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Ingesting
            </span>
            <span
              v-else
              class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 dark:bg-slate-800 text-slate-500"
            >
              Idle
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="Created Time" min-width="160">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-500 dark:text-slate-400">{{ row.createTime || 'N/A' }}</span>
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="Actions" width="260" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1.5">
              <el-button
                v-if="row.pushing"
                size="small"
                type="primary"
                plain
                @click="previewStream(row)"
              >
                Play
              </el-button>

              <el-button
                size="small"
                :type="row.gbId ? 'success' : 'info'"
                plain
                @click="toggleGbJoin(row)"
              >
                {{ row.gbId ? 'Joined GB' : 'Join GB' }}
              </el-button>

              <el-button
                size="small"
                type="danger"
                plain
                @click="deletePush(row)"
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
          @size-change="loadPushes"
          @current-change="loadPushes"
        />
      </div>
    </div>

    <!-- Push URL Generator Modal -->
    <el-dialog v-model="generatorModalVisible" title="Generate RTMP / RTSP Push URL" width="600px" destroy-on-close>
      <el-form label-width="110px" class="py-2 text-xs">
        <el-form-item label="App Name">
          <el-input v-model="genForm.app" placeholder="live" />
        </el-form-item>
        <el-form-item label="Stream ID">
          <el-input v-model="genForm.stream" placeholder="e.g. cam01" />
        </el-form-item>
        <div class="p-3 rounded-lg bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 flex flex-col gap-2 font-mono text-xs">
          <span class="text-slate-400 font-sans font-semibold">Generated Ingest Addresses:</span>
          <div class="text-blue-600 dark:text-blue-400 select-all break-all">
            rtmp://{{ hostUrl }}:1935/{{ genForm.app }}/{{ genForm.stream }}
          </div>
          <div class="text-emerald-600 dark:text-emerald-400 select-all break-all">
            rtsp://{{ hostUrl }}:554/{{ genForm.app }}/{{ genForm.stream }}
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="generatorModalVisible = false">Close</el-button>
      </template>
    </el-dialog>

    <!-- Preview Modal -->
    <el-dialog
      v-model="previewModalVisible"
      :title="`Live Push: ${activeStream?.app}/${activeStream?.stream}`"
      width="780px"
      destroy-on-close
    >
      <div class="w-full h-[450px]">
        <PlayerTabs v-if="streamInfo" :stream-info="streamInfo" :show-tab="true" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { streamService } from '@/api'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, Refresh, DocumentAdd, Download } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'

const searchQuery = ref('')
const hostUrl = ref(window.location.hostname)
const loading = ref(false)
const pushList = ref<any[]>([])

const pagination = reactive({
  page: 1,
  count: 15,
  total: 0,
})

const generatorModalVisible = ref(false)
const genForm = reactive({
  app: 'live',
  stream: `stream_${Date.now().toString().slice(-4)}`,
})

const previewModalVisible = ref(false)
const activeStream = ref<any>(null)
const streamInfo = ref<any>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

async function loadPushes() {
  loading.value = true
  try {
    const res: any = await streamService.getStreamPushList({
      page: pagination.page,
      count: pagination.count,
      query: searchQuery.value || undefined,
    })
    if (res && res.data) {
      pushList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    console.warn('Failed to load pushes:', e)
  } finally {
    loading.value = false
  }
}

function openPushUrlGenerator() {
  genForm.app = 'live'
  genForm.stream = `stream_${Date.now().toString().slice(-4)}`
  generatorModalVisible.value = true
}

function previewStream(row: any) {
  activeStream.value = row
  streamInfo.value = {
    app: row.app,
    stream: row.stream,
    ws_flv: `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/${row.app}/${row.stream}.live.flv`,
    flv: `${location.origin}/${row.app}/${row.stream}.live.flv`,
    rtc: `${location.origin}/index/api/webrtc?app=${row.app}&stream=${row.stream}&type=play`,
  }
  previewModalVisible.value = true
}

async function toggleGbJoin(row: any) {
  try {
    if (row.gbId) {
      await streamService.removeFromGb({ app: row.app, stream: row.stream })
      ElMessage.success('Stream removed from GB cascade catalog')
    } else {
      await streamService.saveToGb({ app: row.app, stream: row.stream, name: row.stream })
      ElMessage.success('Stream assigned GB ID and published to catalog')
    }
    loadPushes()
  } catch (e: any) {
    ElMessage.error(e.message || 'Action failed')
  }
}

function deletePush(row: any) {
  ElMessageBox.confirm(`Remove stream push configuration for ${row.app}/${row.stream}?`, 'Confirm Delete', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(async () => {
    await streamService.deleteStreamPush(row.app, row.stream)
    ElMessage.success('Stream push entry removed')
    loadPushes()
  })
}

function triggerExcelImport() {
  fileInputRef.value?.click()
}

function handleFileSelected(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files || files.length === 0) return
  const file = files[0]
  const reader = new FileReader()

  reader.onload = async (evt) => {
    try {
      const data = new Uint8Array(evt.target?.result as ArrayBuffer)
      const workbook = XLSX.read(data, { type: 'array' })
      const firstSheet = workbook.Sheets[workbook.SheetNames[0]]
      const rows: any[] = XLSX.utils.sheet_to_json(firstSheet)

      let importedCount = 0
      for (const r of rows) {
        if (r.app && r.stream) {
          await streamService.addStreamPush({
            app: r.app,
            stream: r.stream,
            name: r.name || r.stream,
          })
          importedCount++
        }
      }
      ElMessage.success(`Successfully imported ${importedCount} push streams from Excel!`)
      loadPushes()
    } catch (err) {
      ElMessage.error('Failed to parse Excel file')
    }
  }
  reader.readAsArrayBuffer(file)
}

function exportExcel() {
  if (pushList.value.length === 0) {
    ElMessage.warning('No stream push data to export')
    return
  }
  const exportData = pushList.value.map((item) => ({
    App: item.app,
    Stream: item.stream,
    Status: item.pushing ? 'Ingesting' : 'Idle',
    CreatedTime: item.createTime || '',
  }))

  const ws = XLSX.utils.json_to_sheet(exportData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'StreamPush')
  XLSX.writeFile(wb, `wvp_stream_push_${Date.now()}.xlsx`)
  ElMessage.success('Excel exported successfully')
}

onMounted(() => {
  loadPushes()
})
</script>
