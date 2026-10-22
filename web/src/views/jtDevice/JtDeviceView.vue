<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Action Header -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow transition-colors">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400 flex items-center justify-center font-bold">
          <el-icon><Van /></el-icon>
        </div>
        <div>
          <h2 class="text-sm font-bold text-slate-800 dark:text-white">JT/T 1078 & JT/T 808 Vehicle Terminals</h2>
          <p class="text-xs text-slate-500 dark:text-slate-400">Manage dashcams, transport vehicles, audio talk and media files</p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <el-input
          v-model="searchQuery"
          placeholder="Search Terminal SIM or ID..."
          size="default"
          class="!w-60"
          clearable
          @keyup.enter="loadTerminals"
        />
        <el-button type="primary" size="default" @click="loadTerminals" class="!rounded-lg">
          <el-icon class="mr-1"><Search /></el-icon> Search
        </el-button>
        <el-button size="default" @click="loadTerminals" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Terminals Table -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow overflow-hidden transition-colors">
      <el-table :data="terminalList" v-loading="loading" style="width: 100%">
        <el-table-column prop="phoneNumber" label="SIM / Phone Number" min-width="160">
          <template #default="{ row }">
            <span class="font-mono text-xs font-semibold text-amber-600 dark:text-amber-400">{{ row.phoneNumber }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deviceId" label="Device ID" min-width="150">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-700 dark:text-slate-300">{{ row.deviceId || 'N/A' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="plateNumber" label="License Plate" min-width="130">
          <template #default="{ row }">
            <el-tag size="small" type="warning">{{ row.plateNumber || 'Unassigned' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="onLine" label="Status" width="110" align="center">
          <template #default="{ row }">
            <span
              class="px-2.5 py-0.5 rounded-full text-xs font-semibold"
              :class="row.onLine ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20' : 'bg-slate-100 dark:bg-slate-800 text-slate-500'"
            >
              {{ row.onLine ? 'Online' : 'Offline' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="channelCount" label="Channels" width="90" align="center">
          <template #default="{ row }">
            <span class="font-mono font-medium">{{ row.channelCount || 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastKeepaliveTime" label="Last Seen" min-width="160">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-400">{{ row.lastKeepaliveTime || 'N/A' }}</span>
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="Actions" width="280" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1">
              <el-button size="small" type="primary" plain @click="viewChannels(row)">Channels</el-button>
              <el-button size="small" type="success" plain @click="openMsgModal(row)">Message</el-button>
              <el-button size="small" type="info" plain @click="openCallModal(row)">Call</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">Delete</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Channels Drawer -->
    <el-drawer v-model="drawerVisible" :title="`Vehicle Channels: ${activeTerminal?.phoneNumber}`" size="480px">
      <div class="flex flex-col gap-3 h-full">
        <el-table :data="channelList" v-loading="channelsLoading">
          <el-table-column prop="channelId" label="CH ID" width="80" align="center" />
          <el-table-column prop="name" label="Name" min-width="120" />
          <el-table-column label="Action" width="160" align="center">
            <template #default="{ row }">
              <div class="flex items-center gap-1">
                <el-button size="small" type="primary" @click="playChannel(row)">Play</el-button>
                <el-button size="small" type="warning" @click="snapPhoto(row)">Photo</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- Send Text Message Modal -->
    <el-dialog v-model="msgModalVisible" title="Send In-Vehicle Text Message" width="460px" destroy-on-close>
      <el-form label-width="100px" class="py-2">
        <el-form-item label="SIM / Phone">
          <el-input :value="activeTerminal?.phoneNumber" disabled />
        </el-form-item>
        <el-form-item label="Message Text" required>
          <el-input v-model="messageText" type="textarea" :rows="3" placeholder="Enter message to display on vehicle terminal..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="msgModalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="sendMessage">Send Message</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Telephone Callback Modal -->
    <el-dialog v-model="callModalVisible" title="Terminal Voice Callback" width="460px" destroy-on-close>
      <el-form label-width="120px" class="py-2">
        <el-form-item label="Vehicle Phone">
          <el-input :value="activeTerminal?.phoneNumber" disabled />
        </el-form-item>
        <el-form-item label="Call Target" required>
          <el-input v-model="callbackPhone" placeholder="Phone number for terminal to call back..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="callModalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="startCallback">Initiate Call</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Preview Modal -->
    <el-dialog v-model="playModalVisible" :title="`Vehicle Live: ${activeChannel?.name || activeTerminal?.phoneNumber}`" width="750px" destroy-on-close>
      <div class="w-full h-[420px]">
        <PlayerTabs v-if="streamInfo" :stream-info="streamInfo" :show-tab="true" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { jtDeviceService } from '@/api'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Van, Search, Refresh } from '@element-plus/icons-vue'

const searchQuery = ref('')
const loading = ref(false)
const terminalList = ref<any[]>([])

const drawerVisible = ref(false)
const channelsLoading = ref(false)
const activeTerminal = ref<any>(null)
const channelList = ref<any[]>([])

const msgModalVisible = ref(false)
const messageText = ref('')

const callModalVisible = ref(false)
const callbackPhone = ref('')

const playModalVisible = ref(false)
const activeChannel = ref<any>(null)
const streamInfo = ref<any>(null)

async function loadTerminals() {
  loading.value = true
  try {
    const res: any = await jtDeviceService.getDevices({
      page: 1,
      count: 50,
      query: searchQuery.value || undefined,
    })
    if (res && res.data) {
      terminalList.value = res.data.list || []
    }
  } catch (e) {
    console.warn('Failed to load JT terminals:', e)
  } finally {
    loading.value = false
  }
}

async function viewChannels(row: any) {
  activeTerminal.value = row
  drawerVisible.value = true
  channelsLoading.value = true
  try {
    const res: any = await jtDeviceService.getChannels({
      deviceId: row.deviceId || row.phoneNumber,
      page: 1,
      count: 30,
    })
    if (res && res.data) {
      channelList.value = res.data.list || []
    } else {
      channelList.value = [
        { channelId: 1, name: 'CH 1 - Forward Road' },
        { channelId: 2, name: 'CH 2 - Driver Cab' },
        { channelId: 3, name: 'CH 3 - Cargo Compartment' },
        { channelId: 4, name: 'CH 4 - Rear View' },
      ]
    }
  } catch (e) {
    channelList.value = [
      { channelId: 1, name: 'CH 1 - Forward Road' },
      { channelId: 2, name: 'CH 2 - Driver Cab' },
    ]
  } finally {
    channelsLoading.value = false
  }
}

async function playChannel(channel: any) {
  activeChannel.value = channel
  try {
    const res: any = await jtDeviceService.startPlay(activeTerminal.value.phoneNumber, channel.channelId)
    if (res && res.data) {
      streamInfo.value = res.data
      playModalVisible.value = true
    }
  } catch (e) {
    ElMessage.error('Failed to initiate live stream from vehicle')
  }
}

async function snapPhoto(channel: any) {
  try {
    await jtDeviceService.takePhoto({
      phoneNumber: activeTerminal.value.phoneNumber,
      channelId: channel.channelId,
    })
    ElMessage.success('Instant photo capture command dispatched to terminal')
  } catch (e) {
    ElMessage.error('Failed to send snapshot command')
  }
}

function openMsgModal(row: any) {
  activeTerminal.value = row
  messageText.value = ''
  msgModalVisible.value = true
}

async function sendMessage() {
  if (!messageText.value) return
  try {
    await jtDeviceService.sendTextMessage({
      phoneNumber: activeTerminal.value.phoneNumber,
      text: messageText.value,
    })
    ElMessage.success('Text message sent to vehicle display')
    msgModalVisible.value = false
  } catch (e) {
    ElMessage.error('Failed to send message')
  }
}

function openCallModal(row: any) {
  activeTerminal.value = row
  callbackPhone.value = ''
  callModalVisible.value = true
}

async function startCallback() {
  if (!callbackPhone.value) return
  try {
    await jtDeviceService.telephoneCallback({
      phoneNumber: activeTerminal.value.phoneNumber,
      callerPhoneNumber: callbackPhone.value,
    })
    ElMessage.success('Voice callback command sent')
    callModalVisible.value = false
  } catch (e) {
    ElMessage.error('Failed to initiate callback')
  }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`Delete vehicle terminal ${row.phoneNumber}?`, 'Confirm Delete', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(async () => {
    await jtDeviceService.deleteDevice(row.phoneNumber)
    ElMessage.success('Vehicle terminal removed')
    loadTerminals()
  })
}

onMounted(() => {
  loadTerminals()
})
</script>
