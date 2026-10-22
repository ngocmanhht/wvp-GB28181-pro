<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Header -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow transition-colors">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold">
          <el-icon><Calendar /></el-icon>
        </div>
        <div>
          <h2 class="text-sm font-bold text-slate-800 dark:text-white">Scheduled Recording Plans</h2>
          <p class="text-xs text-slate-500 dark:text-slate-400">Configure weekly automated recording schedules for camera channels</p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <el-button type="primary" size="default" @click="openAddModal" class="!rounded-lg">
          <el-icon class="mr-1"><Plus /></el-icon> Add Plan
        </el-button>
        <el-button size="default" @click="loadPlans" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Data Table -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow overflow-hidden transition-colors">
      <el-table :data="planList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="Plan ID" width="90" align="center" />
        <el-table-column prop="name" label="Plan Name" min-width="160">
          <template #default="{ row }">
            <span class="font-bold text-slate-800 dark:text-slate-200">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="channelCount" label="Associated Channels" width="160" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="success" class="font-mono">{{ row.channelCount || 0 }} Channels</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="Last Updated" min-width="160">
          <template #default="{ row }">
            <span class="text-xs text-slate-500 dark:text-slate-400 font-mono">{{ row.updateTime || 'N/A' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="240" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1.5">
              <el-button size="small" type="primary" plain @click="openLinkChannelsModal(row)">Channels</el-button>
              <el-button size="small" type="warning" plain @click="openEditModal(row)">Edit</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">Delete</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Create/Edit Plan Modal -->
    <el-dialog v-model="modalVisible" :title="isEdit ? 'Edit Recording Plan' : 'Create Recording Plan'" width="560px" destroy-on-close>
      <el-form label-width="100px" class="py-2">
        <el-form-item label="Plan Name" required>
          <el-input v-model="form.name" placeholder="e.g. 24/7 Full Recording or Working Hours" />
        </el-form-item>
        <el-form-item label="Schedule">
          <div class="flex flex-col gap-2 w-full text-xs">
            <div class="p-3 rounded-lg bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-between">
              <span>Monday - Sunday (24 Hours)</span>
              <el-switch v-model="form.allWeek" active-text="Full Week" />
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="modalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="submitPlan">Save</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Link Channels Modal -->
    <el-dialog v-model="linkModalVisible" :title="`Associate Channels: ${activePlan?.name}`" width="750px" destroy-on-close>
      <div class="flex flex-col gap-3 max-h-[480px]">
        <div class="flex items-center justify-between text-xs">
          <span class="text-slate-500">Select camera channels to record according to this schedule:</span>
          <el-button size="small" type="primary" @click="saveChannelLinks">Save Selection</el-button>
        </div>

        <el-table
          ref="channelTableRef"
          :data="availableChannels"
          v-loading="channelsLoading"
          style="width: 100%"
          max-height="380"
          size="small"
          @selection-change="handleChannelSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="channelId" label="Channel ID" min-width="160">
            <template #default="{ row }">
              <span class="font-mono text-xs text-blue-600 dark:text-blue-400">{{ row.channelId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="Camera Name" min-width="160" />
          <el-table-column prop="deviceId" label="Device ID" min-width="160">
            <template #default="{ row }">
              <span class="font-mono text-xs text-slate-400">{{ row.deviceId }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { recordPlanService } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Plus, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const planList = ref<any[]>([])

const modalVisible = ref(false)
const isEdit = ref(false)
const form = reactive({
  id: '',
  name: '',
  allWeek: true,
})

const linkModalVisible = ref(false)
const activePlan = ref<any>(null)
const channelsLoading = ref(false)
const availableChannels = ref<any[]>([])
const selectedChannelIds = ref<any[]>([])

async function loadPlans() {
  loading.value = true
  try {
    const res: any = await recordPlanService.getPlanList({ page: 1, count: 50 })
    if (res && res.data) {
      planList.value = res.data.list || []
    }
  } catch (e) {
    console.warn('Failed to load record plans:', e)
  } finally {
    loading.value = false
  }
}

function openAddModal() {
  isEdit.value = false
  form.id = ''
  form.name = ''
  form.allWeek = true
  modalVisible.value = true
}

function openEditModal(row: any) {
  isEdit.value = true
  form.id = row.id
  form.name = row.name
  form.allWeek = true
  modalVisible.value = true
}

async function submitPlan() {
  if (!form.name) {
    ElMessage.warning('Please enter plan name')
    return
  }

  const defaultItems = [
    { start: '00:00:00', stop: '23:59:59', weekDay: 1 },
    { start: '00:00:00', stop: '23:59:59', weekDay: 2 },
    { start: '00:00:00', stop: '23:59:59', weekDay: 3 },
    { start: '00:00:00', stop: '23:59:59', weekDay: 4 },
    { start: '00:00:00', stop: '23:59:59', weekDay: 5 },
    { start: '00:00:00', stop: '23:59:59', weekDay: 6 },
    { start: '00:00:00', stop: '23:59:59', weekDay: 7 },
  ]

  try {
    if (isEdit.value) {
      await recordPlanService.updatePlan({ id: form.id, name: form.name, planList: defaultItems })
      ElMessage.success('Plan updated successfully')
    } else {
      await recordPlanService.addPlan({ name: form.name, planList: defaultItems })
      ElMessage.success('Plan created successfully')
    }
    modalVisible.value = false
    loadPlans()
  } catch (e: any) {
    ElMessage.error(e.message || 'Action failed')
  }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`Delete recording plan ${row.name}?`, 'Confirm Delete', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(async () => {
    await recordPlanService.deletePlan(row.id)
    ElMessage.success('Plan removed')
    loadPlans()
  })
}

async function openLinkChannelsModal(row: any) {
  activePlan.value = row
  linkModalVisible.value = true
  channelsLoading.value = true
  try {
    const res: any = await recordPlanService.getChannelList({
      page: 1,
      count: 100,
      planId: row.id,
    })
    if (res && res.data) {
      availableChannels.value = res.data.list || []
    }
  } catch (e) {
    console.warn('Failed to fetch channels for plan:', e)
  } finally {
    channelsLoading.value = false
  }
}

function handleChannelSelectionChange(selection: any[]) {
  selectedChannelIds.value = selection.map((s) => s.channelId)
}

async function saveChannelLinks() {
  if (!activePlan.value) return
  try {
    await recordPlanService.linkPlan({
      planId: activePlan.value.id,
      channelIds: selectedChannelIds.value,
    })
    ElMessage.success('Channels linked to plan successfully')
    linkModalVisible.value = false
    loadPlans()
  } catch (e: any) {
    ElMessage.error(e.message || 'Linking failed')
  }
}

onMounted(() => {
  loadPlans()
})
</script>
