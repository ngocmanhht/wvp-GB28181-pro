<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Header Controls -->
    <div class="p-4 bg-slate-900 border border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow">
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
        <el-button size="default" @click="loadProxies" class="!rounded-lg !bg-slate-800 !text-slate-200 !border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Data Table -->
    <div class="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow overflow-hidden">
      <el-table
        :data="proxyList"
        v-loading="loading"
        style="width: 100%"
        class="custom-table"
      >
        <el-table-column prop="app" label="App Name" width="120">
          <template #default="{ row }">
            <span class="font-mono text-xs text-blue-400 font-semibold">{{ row.app }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="stream" label="Stream ID" min-width="160">
          <template #default="{ row }">
            <span class="font-mono font-medium text-slate-200">{{ row.stream }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="url" label="RTSP/RTMP Source URL" min-width="260">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-400 truncate block max-w-md">{{ row.url }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="Status" width="110" align="center">
          <template #default="{ row }">
            <span
              v-if="row.status"
              class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Pulling
            </span>
            <span
              v-else
              class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-800 text-slate-500"
            >
              Stopped
            </span>
          </template>
        </el-table-column>

        <el-table-column label="Actions" width="220" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1.5">
              <el-button
                v-if="row.status"
                size="small"
                type="primary"
                plain
                @click="previewStream(row)"
              >
                Play
              </el-button>

              <el-button
                v-if="!row.status"
                size="small"
                type="success"
                plain
                @click="toggleProxy(row, true)"
              >
                Start
              </el-button>

              <el-button
                v-else
                size="small"
                type="warning"
                plain
                @click="toggleProxy(row, false)"
              >
                Stop
              </el-button>

              <el-button
                size="small"
                type="danger"
                plain
                @click="handleDelete(row)"
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

    <!-- Add Proxy Dialog -->
    <el-dialog
      v-model="addDialogVisible"
      title="Add Stream Proxy (Pull RTSP/RTMP)"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="py-2">
        <el-form-item label="App" prop="app">
          <el-input v-model="form.app" placeholder="live" />
        </el-form-item>
        <el-form-item label="Stream ID" prop="stream">
          <el-input v-model="form.stream" placeholder="camera_01" />
        </el-form-item>
        <el-form-item label="Source URL" prop="url">
          <el-input v-model="form.url" placeholder="rtsp://admin:12345@192.168.1.100:554/h264" />
        </el-form-item>
        <el-form-item label="RTP Mode">
          <el-radio-group v-model="form.rtpType">
            <el-radio :label="0">TCP</el-radio>
            <el-radio :label="1">UDP</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Auto Start">
          <el-switch v-model="form.enable" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="addDialogVisible = false">Cancel</el-button>
          <el-button type="primary" :loading="saving" @click="submitAddProxy">Save</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Play Stream Modal -->
    <el-dialog
      v-model="playModalVisible"
      :title="`Stream Preview: ${currentProxy?.app}/${currentProxy?.stream}`"
      width="750px"
      destroy-on-close
    >
      <div class="w-full h-[420px]">
        <PlayerTabs v-if="streamInfo" :stream-info="streamInfo" :show-tab="true" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { streamService } from '@/api'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import { Search, Plus, Refresh } from '@element-plus/icons-vue'

const searchQuery = ref('')
const loading = ref(false)
const proxyList = ref<any[]>([])

const pagination = reactive({
  page: 1,
  count: 15,
  total: 0,
})

const addDialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  app: 'live',
  stream: '',
  url: '',
  rtpType: 0,
  enable: true,
})

const rules = {
  app: [{ required: true, message: 'Please input App', trigger: 'blur' }],
  stream: [{ required: true, message: 'Please input Stream ID', trigger: 'blur' }],
  url: [{ required: true, message: 'Please input Source URL', trigger: 'blur' }],
}

const playModalVisible = ref(false)
const currentProxy = ref<any>(null)
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
    console.error('Failed to load stream proxies:', e)
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  form.stream = `stream_${Date.now()}`
  form.url = ''
  addDialogVisible.value = true
}

async function submitAddProxy() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        await streamService.saveStreamProxy(form)
        ElMessage.success('Stream proxy configured successfully')
        addDialogVisible.value = false
        loadProxies()
      } catch (e) {
        console.error('Save proxy error:', e)
      } finally {
        saving.value = false
      }
    }
  })
}

async function toggleProxy(row: any, start: boolean) {
  try {
    if (start) {
      await streamService.startStreamProxy(row.app, row.stream)
      ElMessage.success('Stream proxy started')
    } else {
      await streamService.stopStreamProxy(row.app, row.stream)
      ElMessage.info('Stream proxy stopped')
    }
    loadProxies()
  } catch (e) {
    console.error('Toggle proxy error:', e)
  }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`Delete proxy ${row.app}/${row.stream}?`, 'Confirm Delete', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning'
  }).then(async () => {
    await streamService.deleteStreamProxy(row.app, row.stream)
    ElMessage.success('Proxy removed')
    loadProxies()
  })
}

function previewStream(row: any) {
  currentProxy.value = row
  const host = window.location.hostname
  const port = window.location.port ? `:${window.location.port}` : ''
  const isHttps = window.location.protocol === 'https:'
  const wsProto = isHttps ? 'wss:' : 'ws:'

  streamInfo.value = {
    ws_flv: `${wsProto}//${host}${port}/${row.app}/${row.stream}.live.flv`,
    wss_flv: `wss://${host}${port}/${row.app}/${row.stream}.live.flv`,
    flv: `${window.location.protocol}//${host}${port}/${row.app}/${row.stream}.live.flv`,
    rtc: `${window.location.protocol}//${host}${port}/index/api/webrtc?app=${row.app}&stream=${row.stream}&type=play`,
    rtcs: `https://${host}${port}/index/api/webrtc?app=${row.app}&stream=${row.stream}&type=play`,
  }
  playModalVisible.value = true
}

onMounted(() => {
  loadProxies()
})
</script>
