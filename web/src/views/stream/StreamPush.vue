<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Header -->
    <div class="p-4 bg-slate-900 border border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow">
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

      <div class="flex items-center gap-2">
        <el-button type="success" size="default" @click="openPushUrlGenerator" class="!rounded-lg">
          <el-icon class="mr-1"><Upload /></el-icon> Generate Push URL
        </el-button>
        <el-button size="default" @click="loadPushes" class="!rounded-lg !bg-slate-800 !text-slate-200 !border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Push Data Table -->
    <div class="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow overflow-hidden">
      <el-table
        :data="pushList"
        v-loading="loading"
        style="width: 100%"
        class="custom-table"
      >
        <el-table-column prop="app" label="App" width="120">
          <template #default="{ row }">
            <span class="font-mono text-xs text-blue-400 font-semibold">{{ row.app }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="stream" label="Stream ID" min-width="160">
          <template #default="{ row }">
            <span class="font-mono font-medium text-slate-200">{{ row.stream }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="pushing" label="Ingest Status" width="130" align="center">
          <template #default="{ row }">
            <span
              v-if="row.pushing"
              class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Ingesting
            </span>
            <span
              v-else
              class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-800 text-slate-500"
            >
              Idle
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="Created Time" min-width="160">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-400">{{ row.createTime || 'N/A' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Actions" width="180" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1.5">
              <el-button
                v-if="row.pushing"
                size="small"
                type="primary"
                plain
                @click="previewPush(row)"
              >
                Play
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
          @size-change="loadPushes"
          @current-change="loadPushes"
        />
      </div>
    </div>

    <!-- Push URL Modal -->
    <el-dialog
      v-model="urlModalVisible"
      title="RTMP / RTSP Push Ingest URL Generator"
      width="600px"
      destroy-on-close
    >
      <div class="flex flex-col gap-4 py-2">
        <p class="text-xs text-slate-400">
          Use OBS, ffmpeg, or your hardware encoder to push a live video stream to the following URL:
        </p>

        <div class="flex flex-col gap-2">
          <span class="text-xs font-medium text-slate-300">RTMP Push URL:</span>
          <el-input :value="rtmpPushUrl" readonly size="default">
            <template #append>
              <el-button @click="copyText(rtmpPushUrl)">Copy</el-button>
            </template>
          </el-input>
        </div>

        <div class="flex flex-col gap-2">
          <span class="text-xs font-medium text-slate-300">RTSP Push URL:</span>
          <el-input :value="rtspPushUrl" readonly size="default">
            <template #append>
              <el-button @click="copyText(rtspPushUrl)">Copy</el-button>
            </template>
          </el-input>
        </div>
      </div>
    </el-dialog>

    <!-- Play Stream Modal -->
    <el-dialog
      v-model="playModalVisible"
      :title="`Push Stream Preview: ${currentPush?.app}/${currentPush?.stream}`"
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
import { ref, reactive, computed, onMounted } from 'vue'
import { streamService } from '@/api'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, Refresh } from '@element-plus/icons-vue'

const searchQuery = ref('')
const loading = ref(false)
const pushList = ref<any[]>([])

const pagination = reactive({
  page: 1,
  count: 15,
  total: 0,
})

const urlModalVisible = ref(false)
const playModalVisible = ref(false)
const currentPush = ref<any>(null)
const streamInfo = ref<any>(null)

const generatedStreamId = ref('')

const rtmpPushUrl = computed(() => {
  const host = window.location.hostname
  return `rtmp://${host}:1935/live/${generatedStreamId.value || 'test'}`
})

const rtspPushUrl = computed(() => {
  const host = window.location.hostname
  return `rtsp://${host}:554/live/${generatedStreamId.value || 'test'}`
})

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
    console.error('Failed to load pushes:', e)
  } finally {
    loading.value = false
  }
}

function openPushUrlGenerator() {
  generatedStreamId.value = `stream_${Math.floor(Math.random() * 900000 + 100000)}`
  urlModalVisible.value = true
}

function copyText(text: string) {
  navigator.clipboard.writeText(text)
  ElMessage.success('Copied URL to clipboard!')
}

function previewPush(row: any) {
  currentPush.value = row
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

function handleDelete(row: any) {
  ElMessageBox.confirm(`Delete push stream ${row.app}/${row.stream}?`, 'Delete Push Stream', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning'
  }).then(async () => {
    await streamService.deleteStreamPush(row.app, row.stream)
    ElMessage.success('Push stream removed')
    loadPushes()
  })
}

onMounted(() => {
  loadPushes()
})
</script>
