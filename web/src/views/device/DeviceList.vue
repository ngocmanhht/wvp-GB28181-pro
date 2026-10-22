<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Search and Action Header -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow transition-colors">
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
        <el-button type="success" size="default" @click="openAddDeviceModal" class="!rounded-lg">
          <el-icon class="mr-1"><Plus /></el-icon> Add Device
        </el-button>
        <el-button size="default" @click="loadDevices" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Device Data Table -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow overflow-hidden transition-colors">
      <el-table
        :data="deviceList"
        v-loading="loading"
        style="width: 100%"
        class="custom-table"
      >
        <el-table-column prop="deviceId" label="Device ID (SIP)" min-width="170">
          <template #default="{ row }">
            <span class="font-mono font-medium text-blue-600 dark:text-blue-400">{{ row.deviceId }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="Device Name" min-width="150">
          <template #default="{ row }">
            <span class="font-medium text-slate-800 dark:text-slate-200">{{ row.name || 'Unnamed Device' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="channelCount" label="Channels" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info" class="font-mono">{{ row.channelCount || 0 }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="streamMode" label="Transport" width="135" align="center">
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
              class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Online
            </span>
            <span
              v-else
              class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/20"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-rose-500"></span>
              Offline
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="ip" label="IP & Port" min-width="150">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-600 dark:text-slate-300">{{ row.ip }}:{{ row.port }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="manufacturer" label="Vendor" width="110" align="center">
          <template #default="{ row }">
            <span class="text-xs text-slate-500 dark:text-slate-400">{{ row.manufacturer || 'Standard' }}</span>
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="Actions" width="310" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1">
              <el-button size="small" type="primary" plain @click="viewChannels(row)">Channels</el-button>
              <el-button size="small" type="success" plain @click="handleSync(row)" :loading="syncingDeviceId === row.deviceId">Sync</el-button>
              
              <!-- More Options Dropdown -->
              <el-dropdown trigger="click" @command="(cmd: string) => handleMoreAction(cmd, row)">
                <el-button size="small" type="info" plain>
                  More <el-icon class="ml-1"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="guard">Set Guard (Bố phòng)</el-dropdown-item>
                    <el-dropdown-item command="resetGuard">Reset Guard (Giải phòng)</el-dropdown-item>
                    <el-dropdown-item command="subscribe">Subscribe Catalog</el-dropdown-item>
                    <el-dropdown-item command="teleboot" divided class="text-amber-500">Reboot (Teleboot)</el-dropdown-item>
                    <el-dropdown-item command="delete" class="text-rose-500">Delete Device</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
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
          :page-sizes="[15, 30, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadDevices"
          @current-change="loadDevices"
        />
      </div>
    </div>

    <!-- Channels Drawer -->
    <el-drawer
      v-model="drawerVisible"
      :title="`Channels: ${currentDevice?.name || currentDevice?.deviceId}`"
      size="540px"
    >
      <div class="flex flex-col gap-3 h-full">
        <el-table :data="channelList" v-loading="channelsLoading" height="100%" size="small">
          <el-table-column prop="channelId" label="Channel ID" min-width="160">
            <template #default="{ row }">
              <span class="font-mono text-xs text-blue-600 dark:text-blue-400">{{ row.channelId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="Name" min-width="140" />
          <el-table-column prop="status" label="Status" width="90" align="center">
            <template #default="{ row }">
              <span
                class="px-2 py-0.5 rounded text-xs font-semibold"
                :class="row.status === 1 ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' : 'bg-slate-100 dark:bg-slate-800 text-slate-500'"
              >
                {{ row.status === 1 ? 'Online' : 'Offline' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="Action" width="140" align="center">
            <template #default="{ row }">
              <div class="flex items-center gap-1">
                <el-button size="small" type="primary" @click="previewChannel(row)">Play</el-button>
                <el-button size="small" type="warning" plain @click="$router.push(`/channel/record/${row.channelId}?deviceId=${currentDevice.deviceId}`)">Record</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- Add Manual Device Modal -->
    <el-dialog v-model="addModalVisible" title="Add GB28181 Device Manually" width="540px" destroy-on-close>
      <el-form label-width="140px" class="py-2 text-xs">
        <el-form-item label="Device SIP ID" required>
          <el-input v-model="addForm.deviceId" placeholder="20-digit National Standard Code" />
        </el-form-item>
        <el-form-item label="Device Name" required>
          <el-input v-model="addForm.name" placeholder="Camera / NVR Name" />
        </el-form-item>
        <el-form-item label="Device IP">
          <el-input v-model="addForm.ip" placeholder="192.168.1.50" />
        </el-form-item>
        <el-form-item label="Device Port">
          <el-input-number v-model="addForm.port" :min="1" :max="65535" class="!w-full" />
        </el-form-item>
        <el-form-item label="SIP Password">
          <el-input v-model="addForm.password" placeholder="Device registration password" />
        </el-form-item>
        <el-form-item label="Stream Mode">
          <el-select v-model="addForm.streamMode" class="w-full">
            <el-option label="UDP" value="UDP" />
            <el-option label="TCP-ACTIVE" value="TCP-ACTIVE" />
            <el-option label="TCP-PASSIVE" value="TCP-PASSIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="addModalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="submitAddDevice">Save Device</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Preview Modal -->
    <el-dialog
      v-model="previewModalVisible"
      :title="`Preview: ${previewTitle}`"
      width="780px"
      destroy-on-close
    >
      <div class="w-full h-[440px]">
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
import { Search, Refresh, Plus, ArrowDown } from '@element-plus/icons-vue'

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

// Add Modal State
const addModalVisible = ref(false)
const addForm = reactive({
  deviceId: '',
  name: '',
  ip: '',
  port: 5060,
  password: '',
  streamMode: 'UDP',
})

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
    console.warn('Failed to load devices:', e)
  } finally {
    loading.value = false
  }
}

async function handleSync(row: any) {
  syncingDeviceId.value = row.deviceId
  try {
    await deviceService.syncDevice(row.deviceId)
    ElMessage.success(`Catalog synchronization initiated for ${row.name || row.deviceId}`)
  } catch (e) {
    console.error('Sync failed:', e)
  } finally {
    syncingDeviceId.value = null
  }
}

async function handleTransportChange(row: any) {
  try {
    await deviceService.updateDeviceTransport(row.deviceId, row.streamMode)
    ElMessage.success(`Transport mode updated to ${row.streamMode}`)
  } catch (e) {
    console.error('Transport change failed:', e)
  }
}

function handleMoreAction(cmd: string, row: any) {
  if (cmd === 'guard') {
    deviceService.setGuard(row.deviceId, 'SetGuard').then(() => {
      ElMessage.success('Arming command (SetGuard) sent successfully')
    })
  } else if (cmd === 'resetGuard') {
    deviceService.resetGuard(row.deviceId, 'ResetGuard').then(() => {
      ElMessage.success('Disarming command (ResetGuard) sent successfully')
    })
  } else if (cmd === 'subscribe') {
    deviceService.subscribeCatalog(row.deviceId, 3600).then(() => {
      ElMessage.success('Subscribed to device catalog updates (3600s)')
    })
  } else if (cmd === 'teleboot') {
    ElMessageBox.confirm(`Reboot device ${row.deviceId} remotely?`, 'Confirm Teleboot', {
      type: 'warning',
    }).then(async () => {
      await deviceService.teleboot(row.deviceId)
      ElMessage.success('Remote reboot command transmitted')
    })
  } else if (cmd === 'delete') {
    ElMessageBox.confirm(`Delete device ${row.deviceId}?`, 'Delete Device', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning',
    }).then(async () => {
      await deviceService.deleteDevice(row.deviceId)
      ElMessage.success('Device removed')
      loadDevices()
    })
  }
}

function openAddDeviceModal() {
  addForm.deviceId = ''
  addForm.name = ''
  addForm.ip = ''
  addForm.port = 5060
  addForm.password = ''
  addForm.streamMode = 'UDP'
  addModalVisible.value = true
}

async function submitAddDevice() {
  if (!addForm.deviceId || !addForm.name) {
    ElMessage.warning('Device ID and Name are required')
    return
  }
  try {
    await deviceService.addDevice(addForm)
    ElMessage.success('Device registered successfully')
    addModalVisible.value = false
    loadDevices()
  } catch (e: any) {
    ElMessage.error(e.message || 'Failed to add device')
  }
}

async function viewChannels(row: any) {
  currentDevice.value = row
  drawerVisible.value = true
  channelsLoading.value = true
  try {
    const res: any = await deviceService.getDeviceChannels({
      deviceId: row.deviceId,
      page: 1,
      count: 100,
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
