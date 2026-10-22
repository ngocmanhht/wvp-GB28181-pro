<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Header -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex items-center justify-between shadow transition-colors">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg bg-blue-500/10 text-blue-600 dark:text-blue-400 flex items-center justify-center font-bold">
          <el-icon><Share /></el-icon>
        </div>
        <div>
          <h2 class="text-sm font-bold text-slate-800 dark:text-white">GB28181 Cascading Upstream Platforms</h2>
          <p class="text-xs text-slate-500 dark:text-slate-400">Manage cascading connections and channel push to provincial / central platforms</p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <el-button type="primary" size="default" @click="openAddModal" class="!rounded-lg">
          <el-icon class="mr-1"><Plus /></el-icon> Add Upstream Platform
        </el-button>
        <el-button size="default" @click="loadPlatforms" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Data Table -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow overflow-hidden transition-colors">
      <el-table :data="platformList" v-loading="loading" style="width: 100%">
        <el-table-column prop="serverGBId" label="Platform SIP ID" min-width="180">
          <template #default="{ row }">
            <span class="font-mono text-xs font-semibold text-blue-600 dark:text-blue-400">{{ row.serverGBId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="Platform Name" min-width="150" />
        <el-table-column prop="serverIP" label="Upstream IP & Port" min-width="150">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-700 dark:text-slate-300">{{ row.serverIP }}:{{ row.serverPort }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deviceGBId" label="Local Device SIP ID" min-width="180">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-500 dark:text-slate-400">{{ row.deviceGBId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="120" align="center">
          <template #default="{ row }">
            <span
              class="px-2.5 py-0.5 rounded-full text-xs font-semibold"
              :class="row.status ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20' : 'bg-slate-100 dark:bg-slate-800 text-slate-500'"
            >
              {{ row.status ? 'Registered' : 'Offline' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="transport" label="Transport" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.transport || 'UDP' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="280" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1">
              <el-button size="small" type="primary" plain @click="openShareModal(row)">Share Channels</el-button>
              <el-button size="small" type="success" plain @click="pushCatalog(row)">Push Catalog</el-button>
              <el-button size="small" type="warning" plain @click="openEditModal(row)">Edit</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">Delete</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Create/Edit Platform Modal -->
    <el-dialog v-model="modalVisible" :title="isEdit ? 'Edit Upstream Platform' : 'Add Upstream Platform'" width="640px" destroy-on-close>
      <el-form label-width="150px" class="py-2 text-xs">
        <el-form-item label="Platform Name" required>
          <el-input v-model="form.name" placeholder="e.g. Higher Province Platform" />
        </el-form-item>
        <el-form-item label="Platform SIP ID" required>
          <el-input v-model="form.serverGBId" placeholder="20-digit GB SIP ID" />
        </el-form-item>
        <el-form-item label="Platform SIP Domain" required>
          <el-input v-model="form.serverGBDomain" placeholder="10-digit SIP domain" />
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="Server IP" required label-width="150px">
            <el-input v-model="form.serverIP" placeholder="192.168.1.100" />
          </el-form-item>
          <el-form-item label="Server Port" required label-width="90px">
            <el-input-number v-model="form.serverPort" :min="1" :max="65535" class="!w-full" />
          </el-form-item>
        </div>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="Local Device ID" required label-width="150px">
            <el-input v-model="form.deviceGBId" placeholder="Local SIP ID" />
          </el-form-item>
          <el-form-item label="SIP Password" label-width="90px">
            <el-input v-model="form.password" type="password" show-password placeholder="Optional" />
          </el-form-item>
        </div>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="Transport" label-width="150px">
            <el-select v-model="form.transport" class="w-full">
              <el-option label="UDP" value="UDP" />
              <el-option label="TCP" value="TCP" />
            </el-select>
          </el-form-item>
          <el-form-item label="Encoding" label-width="90px">
            <el-select v-model="form.characterSet" class="w-full">
              <el-option label="UTF-8" value="UTF-8" />
              <el-option label="GB2312" value="GB2312" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="Enable Cascade">
          <el-switch v-model="form.enable" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="modalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="submitPlatform">Save Platform</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Share Channels Modal -->
    <el-dialog v-model="shareModalVisible" :title="`Share Channels with Platform: ${activePlatform?.name}`" width="850px" destroy-on-close>
      <div class="flex flex-col gap-3 max-h-[520px]">
        <div class="flex items-center justify-between text-xs">
          <span class="text-slate-500">Select channels to push to upstream platform:</span>
          <div class="flex items-center gap-2">
            <el-button size="small" type="primary" @click="addSelectedChannels">Share Selected</el-button>
            <el-button size="small" type="danger" plain @click="removeSelectedChannels">Unshare Selected</el-button>
          </div>
        </div>

        <el-table
          ref="shareTableRef"
          :data="shareChannelList"
          v-loading="shareLoading"
          style="width: 100%"
          max-height="400"
          size="small"
          @selection-change="handleShareSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="channelId" label="Channel ID" min-width="160">
            <template #default="{ row }">
              <span class="font-mono text-xs text-blue-600 dark:text-blue-400">{{ row.channelId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="Camera Name" min-width="140" />
          <el-table-column prop="hasShare" label="Share Status" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.hasShare ? 'success' : 'info'">
                {{ row.hasShare ? 'Shared' : 'Not Shared' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { systemService } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Share, Plus, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const platformList = ref<any[]>([])

const modalVisible = ref(false)
const isEdit = ref(false)
const form = reactive({
  id: '',
  name: '',
  serverGBId: '',
  serverGBDomain: '',
  serverIP: '',
  serverPort: 5060,
  deviceGBId: '',
  password: '',
  transport: 'UDP',
  characterSet: 'UTF-8',
  enable: true,
})

const shareModalVisible = ref(false)
const activePlatform = ref<any>(null)
const shareLoading = ref(false)
const shareChannelList = ref<any[]>([])
const selectedShareChannels = ref<any[]>([])

async function loadPlatforms() {
  loading.value = true
  try {
    const res: any = await systemService.getPlatformList({ page: 1, count: 50 })
    if (res && res.data) {
      platformList.value = res.data.list || []
    }
  } catch (e) {
    console.warn('Failed to load platforms:', e)
  } finally {
    loading.value = false
  }
}

function openAddModal() {
  isEdit.value = false
  form.id = ''
  form.name = ''
  form.serverGBId = ''
  form.serverGBDomain = ''
  form.serverIP = ''
  form.serverPort = 5060
  form.deviceGBId = ''
  form.password = ''
  form.transport = 'UDP'
  form.characterSet = 'UTF-8'
  form.enable = true
  modalVisible.value = true
}

function openEditModal(row: any) {
  isEdit.value = true
  Object.assign(form, row)
  modalVisible.value = true
}

async function submitPlatform() {
  if (!form.name || !form.serverGBId || !form.serverIP) {
    ElMessage.warning('Please fill in required fields')
    return
  }
  try {
    if (isEdit.value) {
      await systemService.updatePlatform(form)
      ElMessage.success('Platform updated successfully')
    } else {
      await systemService.addPlatform(form)
      ElMessage.success('Platform created successfully')
    }
    modalVisible.value = false
    loadPlatforms()
  } catch (e: any) {
    ElMessage.error(e.message || 'Action failed')
  }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`Delete cascade platform ${row.name}?`, 'Confirm Delete', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(async () => {
    await systemService.deletePlatform(row.id || row.serverGBId)
    ElMessage.success('Platform removed')
    loadPlatforms()
  })
}

async function pushCatalog(row: any) {
  try {
    await systemService.pushPlatformChannel(row.id || row.serverGBId)
    ElMessage.success('Catalog notification pushed to upstream platform')
  } catch (e: any) {
    ElMessage.error('Failed to push catalog')
  }
}

async function openShareModal(row: any) {
  activePlatform.value = row
  shareModalVisible.value = true
  shareLoading.value = true
  try {
    const res: any = await systemService.getPlatformChannelList({
      platformId: row.id || row.serverGBId,
      page: 1,
      count: 100,
    })
    if (res && res.data) {
      shareChannelList.value = res.data.list || []
    }
  } catch (e) {
    console.warn('Failed to load share channels:', e)
  } finally {
    shareLoading.value = false
  }
}

function handleShareSelectionChange(selection: any[]) {
  selectedShareChannels.value = selection.map((s) => s.channelId)
}

async function addSelectedChannels() {
  if (selectedShareChannels.value.length === 0) {
    ElMessage.warning('Please select channels')
    return
  }
  try {
    await systemService.addPlatformChannel({
      platformId: activePlatform.value.id || activePlatform.value.serverGBId,
      channelIds: selectedShareChannels.value,
    })
    ElMessage.success('Channels shared successfully')
    openShareModal(activePlatform.value)
  } catch (e: any) {
    ElMessage.error('Failed to share channels')
  }
}

async function removeSelectedChannels() {
  if (selectedShareChannels.value.length === 0) {
    ElMessage.warning('Please select channels')
    return
  }
  try {
    await systemService.removePlatformChannel({
      platformId: activePlatform.value.id || activePlatform.value.serverGBId,
      channelIds: selectedShareChannels.value,
    })
    ElMessage.success('Channels unshared successfully')
    openShareModal(activePlatform.value)
  } catch (e: any) {
    ElMessage.error('Failed to unshare channels')
  }
}

onMounted(() => {
  loadPlatforms()
})
</script>
