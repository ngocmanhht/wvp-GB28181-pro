<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Search Bar -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow transition-colors">
      <div class="flex items-center gap-3 flex-wrap">
        <el-input
          v-model="searchQuery"
          placeholder="Filter Channel Name or ID..."
          clearable
          size="default"
          class="!w-64"
          :prefix-icon="Search"
          @keyup.enter="loadChannels"
        />

        <el-select v-model="statusFilter" placeholder="Channel Status" clearable size="default" class="!w-36">
          <el-option label="All Channels" value="" />
          <el-option label="Online Only" :value="true" />
          <el-option label="Offline Only" :value="false" />
        </el-select>

        <el-button type="primary" size="default" @click="loadChannels" class="!rounded-lg">
          <el-icon class="mr-1"><Search /></el-icon> Search
        </el-button>
      </div>

      <el-button size="default" @click="loadChannels" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
        <el-icon class="mr-1"><Refresh /></el-icon> Refresh
      </el-button>
    </div>

    <!-- Channels Data Table -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow overflow-hidden transition-colors">
      <el-table
        :data="channelList"
        v-loading="loading"
        style="width: 100%"
        class="custom-table"
      >
        <el-table-column prop="channelId" label="Channel ID" min-width="180">
          <template #default="{ row }">
            <span class="font-mono font-medium text-blue-600 dark:text-blue-400">{{ row.channelId }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="Channel Name" min-width="150">
          <template #default="{ row }">
            <span class="font-medium text-slate-800 dark:text-slate-200">{{ row.name || 'Unnamed Channel' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="deviceId" label="Parent Device ID" min-width="170">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-500 dark:text-slate-400">{{ row.deviceId }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="Status" width="110" align="center">
          <template #default="{ row }">
            <span
              v-if="row.status === 1 || row.status === true"
              class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Online
            </span>
            <span
              v-else
              class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-600 dark:text-rose-400 border border-rose-500/20"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-rose-500"></span>
              Offline
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="ptzType" label="PTZ Support" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.ptzType ? 'success' : 'info'">
              {{ row.ptzType ? 'Supported' : 'Fixed' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="subCount" label="Sub Channels" width="110" align="center">
          <template #default="{ row }">
            <span class="font-mono text-xs">{{ row.subCount || 0 }}</span>
          </template>
        </el-table-column>

        <!-- Actions -->
        <el-table-column label="Actions" width="310" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1">
              <el-button size="small" type="primary" plain @click="handlePlay(row)">Play</el-button>
              <el-button size="small" type="warning" plain @click="$router.push(`/channel/record/${row.channelId}?deviceId=${row.deviceId}`)">Record</el-button>
              <el-button size="small" type="success" plain @click="openTalkModal(row)">Talk</el-button>
              <el-button size="small" type="info" plain @click="openEditModal(row)">Edit</el-button>
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
          @size-change="loadChannels"
          @current-change="loadChannels"
        />
      </div>
    </div>

    <!-- Live Stream Modal with PTZ -->
    <el-dialog
      v-model="playModalVisible"
      :title="`Live: ${activeChannel?.name || activeChannel?.channelId}`"
      width="1040px"
      destroy-on-close
    >
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4 h-[480px]">
        <div class="lg:col-span-2 h-full bg-black rounded-lg overflow-hidden">
          <PlayerTabs v-if="streamInfo" :stream-info="streamInfo" :show-tab="true" />
        </div>
        <div class="h-full overflow-y-auto">
          <PtzControls :device-id="activeChannel?.deviceId" :channel-id="activeChannel?.channelId" />
        </div>
      </div>
    </el-dialog>

    <!-- Voice Talkback Modal -->
    <el-dialog v-model="talkModalVisible" :title="`Audio Talk: ${activeChannel?.name || activeChannel?.channelId}`" width="460px" destroy-on-close>
      <div class="flex flex-col items-center justify-center gap-4 py-4">
        <div class="w-16 h-16 rounded-full flex items-center justify-center text-3xl transition"
          :class="isTalking ? 'bg-rose-500/20 text-rose-500 animate-pulse' : 'bg-blue-500/20 text-blue-500'">
          <el-icon><Microphone /></el-icon>
        </div>
        <div class="text-center">
          <span class="font-bold text-sm block">{{ isTalking ? 'Transmitting Audio to Camera...' : 'Ready for Two-Way Audio Talk' }}</span>
          <span class="text-xs text-slate-400">Target: {{ activeChannel?.channelId }}</span>
        </div>

        <div class="flex gap-3 mt-2">
          <el-button v-if="!isTalking" type="primary" size="large" @click="startTalk">Start Talking</el-button>
          <el-button v-else type="danger" size="large" @click="stopTalk">Stop Talking</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- Edit Channel Modal -->
    <el-dialog v-model="editModalVisible" title="Edit Camera Channel Information" width="560px" destroy-on-close>
      <el-form label-width="120px" class="py-2 text-xs">
        <el-form-item label="Channel ID">
          <el-input :value="editForm.channelId" disabled />
        </el-form-item>
        <el-form-item label="Custom Name" required>
          <el-input v-model="editForm.customName" placeholder="Enter custom display name" />
        </el-form-item>
        <el-form-item label="PTZ Type">
          <el-select v-model="editForm.ptzType" class="w-full">
            <el-option label="Fixed Ball / Fixed Camera" :value="0" />
            <el-option label="Ball / PTZ Pan-Tilt" :value="1" />
            <el-option label="Infrared Ball Camera" :value="2" />
          </el-select>
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="Longitude" label-width="120px">
            <el-input v-model="editForm.longitude" placeholder="105.8342" />
          </el-form-item>
          <el-form-item label="Latitude" label-width="80px">
            <el-input v-model="editForm.latitude" placeholder="21.0278" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="editModalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="submitChannelEdit">Save Changes</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { deviceService, playService } from '@/api'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import PtzControls from '@/components/player/PtzControls.vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Microphone } from '@element-plus/icons-vue'

const searchQuery = ref('')
const statusFilter = ref<any>('')
const loading = ref(false)
const channelList = ref<any[]>([])

const pagination = reactive({
  page: 1,
  count: 15,
  total: 0,
})

const playModalVisible = ref(false)
const activeChannel = ref<any>(null)
const streamInfo = ref<any>(null)

// Talkback state
const talkModalVisible = ref(false)
const isTalking = ref(false)

// Edit state
const editModalVisible = ref(false)
const editForm = reactive({
  id: '',
  channelId: '',
  deviceId: '',
  customName: '',
  ptzType: 0,
  longitude: '',
  latitude: '',
})

async function loadChannels() {
  loading.value = true
  try {
    const res: any = await deviceService.getAllChannels({
      page: pagination.page,
      count: pagination.count,
      query: searchQuery.value || undefined,
      online: statusFilter.value !== '' ? statusFilter.value : undefined,
    })
    if (res && res.data) {
      channelList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    console.error('Failed to load channels:', e)
  } finally {
    loading.value = false
  }
}

async function handlePlay(row: any) {
  activeChannel.value = row
  try {
    const res: any = await playService.startPlay(row.deviceId, row.channelId)
    if (res && res.data) {
      streamInfo.value = res.data
      playModalVisible.value = true
    }
  } catch (e) {
    ElMessage.error('Failed to start camera playback')
  }
}

function openTalkModal(row: any) {
  activeChannel.value = row
  isTalking.value = false
  talkModalVisible.value = true
}

async function startTalk() {
  try {
    await playService.talkStart(activeChannel.value.deviceId, activeChannel.value.channelId)
    isTalking.value = true
    ElMessage.success('Audio channel connected. Start speaking.')
  } catch (e) {
    ElMessage.error('Failed to initiate talkback session')
  }
}

async function stopTalk() {
  try {
    await playService.talkStop(activeChannel.value.deviceId, activeChannel.value.channelId)
    isTalking.value = false
    ElMessage.info('Audio talkback session terminated')
  } catch (e) {
    console.error('Error stopping talk:', e)
  }
}

function openEditModal(row: any) {
  activeChannel.value = row
  editForm.id = row.id
  editForm.channelId = row.channelId
  editForm.deviceId = row.deviceId
  editForm.customName = row.customName || row.name || ''
  editForm.ptzType = row.ptzType || 0
  editForm.longitude = row.longitude || ''
  editForm.latitude = row.latitude || ''
  editModalVisible.value = true
}

async function submitChannelEdit() {
  try {
    await deviceService.updateChannel({
      id: editForm.id,
      channelId: editForm.channelId,
      deviceId: editForm.deviceId,
      customName: editForm.customName,
      ptzType: editForm.ptzType,
      longitude: editForm.longitude,
      latitude: editForm.latitude,
    })
    ElMessage.success('Channel updated successfully')
    editModalVisible.value = false
    loadChannels()
  } catch (e: any) {
    ElMessage.error(e.message || 'Failed to update channel')
  }
}

onMounted(() => {
  loadChannels()
})
</script>
