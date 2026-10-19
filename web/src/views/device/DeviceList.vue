<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Search and Action Header -->
    <div class="p-4 bg-slate-900 border border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow">
      <div class="flex items-center gap-3 flex-wrap">
        <el-input
          v-model="searchQuery"
          placeholder="Search Device ID or Name..."
          clearable
          size="default"
          class="!w-64"
          :prefix-icon="Search"
          @keyup.enter="loadDevices"
        />

        <el-select v-model="statusFilter" placeholder="Status" clearable size="default" class="!w-36">
          <el-option label="All Status" value="" />
          <el-option label="Online Only" :value="true" />
          <el-option label="Offline Only" :value="false" />
        </el-select>

        <el-button type="primary" size="default" @click="loadDevices" class="!rounded-lg">
          <el-icon class="mr-1"><Search /></el-icon> Search
        </el-button>
      </div>

      <div class="flex items-center gap-2">
        <el-button size="default" @click="loadDevices" class="!rounded-lg !bg-slate-800 !text-slate-200 !border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Device Data Table -->
    <div class="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow overflow-hidden">
      <el-table
        :data="deviceList"
        v-loading="loading"
        style="width: 100%"
        class="custom-table"
      >
        <el-table-column prop="deviceId" label="Device ID (SIP)" min-width="170">
          <template #default="{ row }">
            <span class="font-mono font-medium text-blue-400">{{ row.deviceId }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="Device Name" min-width="150">
          <template #default="{ row }">
            <span class="font-medium text-slate-200">{{ row.name || 'Unnamed Device' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="channelCount" label="Channels" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info" class="font-mono">{{ row.channelCount || 0 }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="streamMode" label="Transport" width="130" align="center">
          <template #default="{ row }">
            <el-select
              v-model="row.streamMode"
              size="small"
              class="w-full"
              @change="handleTransportChange(row)"
            >
              <el-option label="UDP" value="UDP" />
              <el-option label="TCP-ACTIVE" value="TCP-ACTIVE" />
              <el-option label="TCP-PASSIVE" value="TCP-PASSIVE" />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column prop="onLine" label="Status" width="110" align="center">
          <template #default="{ row }">
            <span
              v-if="row.onLine"
              class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Online
            </span>
            <span
              v-else
              class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-rose-500"></span>
              Offline
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="ip" label="IP Address" min-width="140">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-300">{{ row.ip }}:{{ row.port }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="keepaliveTime" label="Last Keepalive" min-width="160">
          <template #default="{ row }">
            <span class="text-xs text-slate-400 font-mono">{{ row.keepaliveTime || 'N/A' }}</span>
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="Actions" width="220" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1.5">
              <el-button
                size="small"
                type="primary"
                plain
                @click="handleSync(row)"
                :loading="syncingDeviceId === row.deviceId"
                title="Sync Channels"
              >
                Sync
              </el-button>

              <el-button
                size="small"
                type="success"
                plain
                @click="viewChannels(row)"
                title="View Channels"
              >
                Channels
              </el-button>

              <el-button
                size="small"
                type="danger"
                plain
                @click="handleDelete(row)"
                title="Delete"
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
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadDevices"
          @current-change="loadDevices"
        />
      </div>
    </div>

    <!-- Channel List Drawer / Dialog -->
    <el-drawer
      v-model="drawerVisible"
      :title="`Channels of ${currentDevice?.name || currentDevice?.deviceId}`"
      size="55%"
      class="custom-drawer"
    >
      <div class="flex flex-col gap-3 h-full">
        <el-table :data="channelList" v-loading="channelsLoading" height="100%">
          <el-table-column prop="channelId" label="Channel ID" min-width="160">
            <template #default="{ row }">
              <span class="font-mono text-xs text-blue-400">{{ row.channelId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="Name" min-width="140" />
          <el-table-column prop="status" label="Status" width="100" align="center">
            <template #default="{ row }">
              <span
                class="px-2 py-0.5 rounded text-xs font-semibold"
                :class="row.status === 1 ? 'bg-emerald-500/10 text-emerald-400' : 'bg-slate-800 text-slate-500'"
              >
                {{ row.status === 1 ? 'Online' : 'Offline' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="Action" width="100" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="previewChannel(row)">Play</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- Preview Modal -->
    <el-dialog
      v-model="previewModalVisible"
      :title="`Preview: ${previewTitle}`"
      width="700px"
      destroy-on-close
    >
      <div class="w-full h-[400px]">
        <PlayerTabs v-if="previewStreamInfo" :stream-info="previewStreamInfo" :show-tab="true" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { deviceService, playService } from '@/api'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'

const searchQuery = ref('')
const statusFilter = ref<any>('')
const loading = ref(false)
const deviceList = ref<any[]>([])
const syncingDeviceId = ref<string | null>(null)

const pagination = reactive({
  page: 1,
  count: 15,
  total: 0,
})

// Channel Drawer State
const drawerVisible = ref(false)
const currentDevice = ref<any>(null)
const channelList = ref<any[]>([])
const channelsLoading = ref(false)

// Preview State
const previewModalVisible = ref(false)
const previewStreamInfo = ref<any>(null)
const previewTitle = ref('')

async function loadDevices() {
  loading.value = true
  try {
    const res: any = await deviceService.getDevices({
      page: pagination.page,
      count: pagination.count,
      query: searchQuery.value || undefined,
      status: statusFilter.value !== '' ? statusFilter.value : undefined,
    })
    if (res && res.data) {
      deviceList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (e: any) {
    console.error('Failed to load devices:', e)
  } finally {
    loading.value = false
  }
}

async function handleSync(row: any) {
  syncingDeviceId.value = row.deviceId
  try {
    await deviceService.syncDevice(row.deviceId)
    ElMessage.success(`Sync request sent for ${row.name || row.deviceId}`)
  } catch (e) {
    console.error('Sync failed:', e)
  } finally {
    syncingDeviceId.value = null
  }
}

async function handleTransportChange(row: any) {
  try {
    await deviceService.updateDeviceTransport(row.deviceId, row.streamMode)
    ElMessage.success(`Transport mode set to ${row.streamMode}`)
  } catch (e) {
    console.error('Transport change failed:', e)
  }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`Are you sure you want to delete device ${row.deviceId}?`, 'Delete Device', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning'
  }).then(async () => {
    await deviceService.deleteDevice(row.deviceId)
    ElMessage.success('Device removed')
    loadDevices()
  })
}

async function viewChannels(row: any) {
  currentDevice.value = row
  drawerVisible.value = true
  channelsLoading.value = true
  try {
    const res: any = await deviceService.getDeviceChannels({
      deviceId: row.deviceId,
      page: 1,
      count: 100
    })
    if (res && res.data) {
      channelList.value = res.data.list || []
    }
  } catch (e) {
    console.error('Failed to fetch channels:', e)
  } finally {
    channelsLoading.value = false
  }
}

async function previewChannel(channel: any) {
  try {
    const res: any = await playService.startPlay(currentDevice.value.deviceId, channel.channelId)
    if (res && res.data) {
      previewStreamInfo.value = res.data
      previewTitle.value = channel.name || channel.channelId
      previewModalVisible.value = true
    }
  } catch (e) {
    console.error('Preview stream failed:', e)
  }
}

onMounted(() => {
  loadDevices()
})
</script>
