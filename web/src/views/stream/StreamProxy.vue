<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Header Controls -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow transition-colors">
      <div class="flex items-center gap-3 flex-wrap">
        <el-input
          v-model="searchQuery"
          placeholder="Search Stream or App..."
          clearable
          size="default"
          class="!w-64"
          :prefix-icon="Search"
          @keyup.enter="loadProxies"
        />

        <el-button type="primary" size="default" @click="loadProxies" class="!rounded-lg">
          <el-icon class="mr-1"><Search /></el-icon> Search
        </el-button>
      </div>

      <div class="flex items-center gap-2">
        <el-button type="success" size="default" @click="openAddDialog" class="!rounded-lg">
          <el-icon class="mr-1"><Plus /></el-icon> Add Stream Proxy
        </el-button>
        <el-button size="default" @click="loadProxies" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Data Table -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow overflow-hidden transition-colors">
      <el-table
        :data="proxyList"
        v-loading="loading"
        style="width: 100%"
        class="custom-table"
      >
        <el-table-column prop="app" label="App Name" width="120">
          <template #default="{ row }">
            <span class="font-mono text-xs text-blue-600 dark:text-blue-400 font-semibold">{{ row.app }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="stream" label="Stream ID" min-width="160">
          <template #default="{ row }">
            <span class="font-mono font-medium text-slate-800 dark:text-slate-200">{{ row.stream }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="url" label="RTSP/RTMP Source URL" min-width="260">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-500 dark:text-slate-400 truncate block max-w-md">{{ row.url }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="Status" width="120" align="center">
          <template #default="{ row }">
            <span
              v-if="row.status"
              class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Pulling
            </span>
            <span
              v-else
              class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 dark:bg-slate-800 text-slate-500"
            >
              Stopped
            </span>
          </template>
        </el-table-column>

        <el-table-column label="Actions" width="240" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1.5">
              <el-button
                v-if="row.status"
                size="small"
                type="primary"
                plain
                @click="previewProxy(row)"
              >
                Play
              </el-button>

              <el-button
                v-if="!row.status"
                size="small"
                type="success"
                plain
                @click="startProxy(row)"
              >
                Start
              </el-button>
              <el-button
                v-else
                size="small"
                type="warning"
                plain
                @click="stopProxy(row)"
              >
                Stop
              </el-button>

              <el-button
                size="small"
                type="danger"
                plain
                @click="deleteProxy(row)"
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
          @size-change="loadProxies"
          @current-change="loadProxies"
        />
      </div>
    </div>

    <!-- Add/Edit Stream Proxy Dialog -->
    <el-dialog
      v-model="addModalVisible"
      :title="isEdit ? 'Edit Stream Proxy' : 'Add Stream Proxy (Pull RTSP/RTMP)'"
      width="600px"
      destroy-on-close
    >
      <el-form label-width="120px" class="py-2 text-xs">
        <el-form-item label="App" required>
          <el-input v-model="form.app" placeholder="live" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="Stream ID" required>
          <el-input v-model="form.stream" placeholder="e.g. proxy_cam_01" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="Source URL" required>
          <el-input v-model="form.url" placeholder="rtsp://admin:pass@192.168.1.50:554/h264/ch1/main/av_stream" />
        </el-form-item>
        <el-form-item label="RTP Transport">
          <el-select v-model="form.rtpType" class="w-full">
            <el-option label="TCP (Recommended)" :value="0" />
            <el-option label="UDP" :value="1" />
          </el-select>
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="Auto Pull" label-width="120px">
            <el-switch v-model="form.enable" />
          </el-form-item>
          <el-form-item label="Enable Audio" label-width="90px">
            <el-switch v-model="form.enableAudio" />
          </el-form-item>
        </div>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="Record MP4" label-width="120px">
            <el-switch v-model="form.enableMp4" />
          </el-form-item>
          <el-form-item label="Enable RTSP" label-width="90px">
            <el-switch v-model="form.enableRtsp" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="addModalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="submitProxy">Save Stream Proxy</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Preview Modal -->
    <el-dialog
      v-model="previewModalVisible"
      :title="`Stream: ${activeProxy?.app}/${activeProxy?.stream}`"
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
import { Search, Plus, Refresh } from '@element-plus/icons-vue'

const searchQuery = ref('')
const loading = ref(false)
const proxyList = ref<any[]>([])

const pagination = reactive({
  page: 1,
  count: 15,
  total: 0,
})

const addModalVisible = ref(false)
const isEdit = ref(false)
const form = reactive({
  app: 'live',
  stream: '',
  url: '',
  rtpType: 0,
  enable: true,
  enableAudio: true,
  enableMp4: false,
  enableRtsp: true,
})

const previewModalVisible = ref(false)
const activeProxy = ref<any>(null)
const streamInfo = ref<any>(null)

async function loadProxies() {
  loading.value = true
  try {
    const res: any = await streamService.getStreamProxyList({
      page: pagination.page,
      count: pagination.count,
      query: searchQuery.value || undefined,
    })
    if (res && res.data) {
      proxyList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    console.warn('Failed to load stream proxies:', e)
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  isEdit.value = false
  form.app = 'live'
  form.stream = `proxy_${Date.now().toString().slice(-4)}`
  form.url = ''
  form.rtpType = 0
  form.enable = true
  form.enableAudio = true
  form.enableMp4 = false
  form.enableRtsp = true
  addModalVisible.value = true
}

async function submitProxy() {
  if (!form.url || !form.stream) {
    ElMessage.warning('Source URL and Stream ID are required')
    return
  }
  try {
    await streamService.saveStreamProxy(form)
    ElMessage.success('Stream proxy configured successfully')
    addModalVisible.value = false
    loadProxies()
  } catch (e: any) {
    ElMessage.error(e.message || 'Failed to save proxy')
  }
}

async function startProxy(row: any) {
  try {
    await streamService.startStreamProxy(row.app, row.stream)
    ElMessage.success('Pulling stream initiated')
    loadProxies()
  } catch (e) {
    ElMessage.error('Failed to start stream pull')
  }
}

async function stopProxy(row: any) {
  try {
    await streamService.stopStreamProxy(row.app, row.stream)
    ElMessage.info('Stream pull halted')
    loadProxies()
  } catch (e) {
    ElMessage.error('Failed to stop stream pull')
  }
}

function deleteProxy(row: any) {
  ElMessageBox.confirm(`Remove proxy for ${row.app}/${row.stream}?`, 'Confirm Delete', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(async () => {
    await streamService.deleteStreamProxy(row.app, row.stream)
    ElMessage.success('Stream proxy removed')
    loadProxies()
  })
}

function previewProxy(row: any) {
  activeProxy.value = row
  streamInfo.value = {
    app: row.app,
    stream: row.stream,
    ws_flv: `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/${row.app}/${row.stream}.live.flv`,
    flv: `${location.origin}/${row.app}/${row.stream}.live.flv`,
    rtc: `${location.origin}/index/api/webrtc?app=${row.app}&stream=${row.stream}&type=play`,
  }
  previewModalVisible.value = true
}

onMounted(() => {
  loadProxies()
})
</script>
