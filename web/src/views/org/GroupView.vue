<template>
  <div class="flex flex-col gap-4 select-none h-[calc(100vh-112px)]">
    <!-- Top Action Header -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow transition-colors">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold">
          <el-icon><Folder /></el-icon>
        </div>
        <div>
          <h2 class="text-sm font-bold text-slate-800 dark:text-white">Business Grouping (Group)</h2>
          <p class="text-xs text-slate-500 dark:text-slate-400">Custom business groupings for cameras and streams</p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <el-button type="primary" size="default" @click="openAddModal" class="!rounded-lg">
          <el-icon class="mr-1"><Plus /></el-icon> Add Group
        </el-button>
        <el-button size="default" @click="loadGroupTree" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Main Content Split Grid -->
    <div class="flex-1 grid grid-cols-1 md:grid-cols-3 gap-4 min-h-0">
      <!-- Left: Group Tree -->
      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow flex flex-col min-h-0 transition-colors">
        <div class="flex items-center justify-between pb-3 border-b border-slate-200 dark:border-slate-800 mb-2">
          <span class="text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider">Group Tree</span>
          <el-input v-model="treeFilter" placeholder="Filter..." size="small" class="!w-36" clearable />
        </div>

        <div class="flex-1 overflow-y-auto" v-loading="treeLoading">
          <el-tree
            ref="treeRef"
            :data="groupTreeData"
            :props="{ children: 'children', label: 'name' }"
            :filter-node-method="filterNode"
            node-key="id"
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="flex items-center justify-between w-full pr-2 text-xs py-1">
                <span class="truncate font-medium text-slate-700 dark:text-slate-200">{{ data.name }}</span>
                <span class="text-[10px] font-mono text-slate-400">{{ data.deviceId }}</span>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- Right: Channel & Info Panel -->
      <div class="md:col-span-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow flex flex-col min-h-0 transition-colors">
        <div class="flex items-center justify-between pb-3 border-b border-slate-200 dark:border-slate-800 mb-3">
          <div class="flex items-center gap-2">
            <span class="text-xs font-semibold text-slate-700 dark:text-slate-300">
              Selected Group: <strong class="text-indigo-600 dark:text-indigo-400">{{ selectedGroup?.name || 'None' }}</strong>
            </span>
          </div>

          <div class="flex items-center gap-2" v-if="selectedGroup">
            <el-button size="small" type="warning" plain @click="openEditModal(selectedGroup)">Edit</el-button>
            <el-button size="small" type="danger" plain @click="deleteGroup(selectedGroup)">Delete</el-button>
          </div>
        </div>

        <!-- Associated Channels Table -->
        <div class="flex-1 overflow-y-auto" v-loading="channelsLoading">
          <el-table :data="associatedChannels" style="width: 100%" size="small">
            <el-table-column prop="channelId" label="Channel ID" min-width="160">
              <template #default="{ row }">
                <span class="font-mono text-xs text-indigo-600 dark:text-indigo-400">{{ row.channelId }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="Channel Name" min-width="140" />
            <el-table-column prop="status" label="Status" width="100" align="center">
              <template #default="{ row }">
                <span
                  class="px-2 py-0.5 rounded text-xs font-semibold"
                  :class="row.status === 1 ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' : 'bg-slate-100 dark:bg-slate-800 text-slate-500'"
                >
                  {{ row.status === 1 ? 'Online' : 'Offline' }}
                </span>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="associatedChannels.length === 0 && !channelsLoading" class="text-center py-16 text-xs text-slate-400">
            Select a group on the left to view assigned channels
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Group Modal -->
    <el-dialog v-model="modalVisible" :title="isEdit ? 'Edit Group' : 'Add Group'" width="480px" destroy-on-close>
      <el-form label-width="110px" class="py-2">
        <el-form-item label="Group Name" required>
          <el-input v-model="form.name" placeholder="e.g. Traffic Cameras or Zone A" />
        </el-form-item>
        <el-form-item label="Group Code">
          <el-input v-model="form.deviceId" placeholder="Optional GB Device Code" />
        </el-form-item>
        <el-form-item label="Parent ID">
          <el-input v-model="form.parentDeviceId" placeholder="Optional Parent Group ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="modalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="submitGroup">Save</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { orgService } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Plus, Refresh } from '@element-plus/icons-vue'

const treeFilter = ref('')
const treeLoading = ref(false)
const channelsLoading = ref(false)
const groupTreeData = ref<any[]>([])
const treeRef = ref<any>(null)
const selectedGroup = ref<any>(null)
const associatedChannels = ref<any[]>([])

const modalVisible = ref(false)
const isEdit = ref(false)
const form = reactive({
  id: '',
  name: '',
  deviceId: '',
  parentDeviceId: '',
})

watch(treeFilter, (val) => {
  treeRef.value?.filter(val)
})

function filterNode(value: string, data: any) {
  if (!value) return true
  return data.name?.toLowerCase().includes(value.toLowerCase()) || data.deviceId?.includes(value)
}

async function loadGroupTree() {
  treeLoading.value = true
  try {
    const res: any = await orgService.getGroupTree()
    if (res && res.data) {
      groupTreeData.value = Array.isArray(res.data) ? res.data : [res.data]
    }
  } catch (e) {
    console.warn('Failed to load group tree:', e)
  } finally {
    treeLoading.value = false
  }
}

function handleNodeClick(data: any) {
  selectedGroup.value = data
  if (data.children && data.children.length > 0) {
    associatedChannels.value = data.children.filter((c: any) => !c.children)
  } else {
    associatedChannels.value = []
  }
}

function openAddModal() {
  isEdit.value = false
  form.id = ''
  form.name = ''
  form.deviceId = ''
  form.parentDeviceId = selectedGroup.value?.deviceId || ''
  modalVisible.value = true
}

function openEditModal(group: any) {
  isEdit.value = true
  form.id = group.id
  form.name = group.name
  form.deviceId = group.deviceId
  form.parentDeviceId = group.parentDeviceId || ''
  modalVisible.value = true
}

async function submitGroup() {
  if (!form.name) {
    ElMessage.warning('Please enter group name')
    return
  }
  try {
    if (isEdit.value) {
      await orgService.updateGroup(form)
      ElMessage.success('Group updated successfully')
    } else {
      await orgService.addGroup(form)
      ElMessage.success('Group created successfully')
    }
    modalVisible.value = false
    loadGroupTree()
  } catch (e: any) {
    ElMessage.error(e.message || 'Action failed')
  }
}

function deleteGroup(group: any) {
  ElMessageBox.confirm(`Delete group ${group.name}?`, 'Confirm Delete', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(async () => {
    await orgService.deleteGroup(group.id)
    ElMessage.success('Group deleted')
    selectedGroup.value = null
    loadGroupTree()
  })
}

onMounted(() => {
  loadGroupTree()
})
</script>
