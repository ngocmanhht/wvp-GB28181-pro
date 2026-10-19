<template>
  <div class="h-full flex flex-col bg-slate-900 border border-slate-800 rounded-xl overflow-hidden select-none">
    <!-- Header & Search input -->
    <div class="p-3 border-b border-slate-800 bg-slate-900/90 flex flex-col gap-2">
      <div class="flex items-center justify-between">
        <span class="font-semibold text-xs tracking-wide uppercase text-slate-400">Device Tree</span>
        <button
          @click="loadDevices"
          class="text-xs text-blue-400 hover:text-blue-300 flex items-center gap-1"
          title="Refresh"
        >
          <el-icon><Refresh /></el-icon> Refresh
        </button>
      </div>

      <el-input
        v-model="searchQuery"
        placeholder="Filter channels..."
        size="small"
        clearable
        :prefix-icon="Search"
      />
    </div>

    <!-- Tree Container -->
    <div class="flex-1 overflow-y-auto p-2" v-loading="loading">
      <el-tree
        ref="treeRef"
        :data="treeData"
        :props="defaultProps"
        :filter-node-method="filterNode"
        node-key="id"
        highlight-current
        @node-click="handleNodeClick"
        class="custom-device-tree"
      >
        <template #default="{ node, data }">
          <div class="flex items-center justify-between w-full pr-2 text-xs py-1">
            <div class="flex items-center gap-2 truncate">
              <!-- Node Icon: Device vs Camera Channel -->
              <span v-if="data.isDevice" class="text-blue-400">
                <el-icon><Monitor /></el-icon>
              </span>
              <span v-else :class="data.online ? 'text-emerald-400' : 'text-slate-500'">
                <el-icon><VideoCamera /></el-icon>
              </span>

              <span :class="data.online === false ? 'text-slate-500 line-through' : 'text-slate-200'" class="truncate">
                {{ data.name || data.deviceId || 'Channel' }}
              </span>
            </div>

            <!-- Badges -->
            <span
              v-if="!data.isDevice"
              class="w-2 h-2 rounded-full flex-shrink-0"
              :class="data.online ? 'bg-emerald-500 shadow-sm shadow-emerald-500/50' : 'bg-slate-600'"
            ></span>
          </div>
        </template>
      </el-tree>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { deviceService } from '@/api'
import { Monitor, VideoCamera, Refresh, Search } from '@element-plus/icons-vue'

const emit = defineEmits(['channelClick', 'channelDblClick'])

const searchQuery = ref('')
const loading = ref(false)
const treeRef = ref<any>(null)
const treeData = ref<any[]>([])

const defaultProps = {
  children: 'children',
  label: 'name',
}

watch(searchQuery, (val) => {
  treeRef.value?.filter(val)
})

function filterNode(value: string, data: any) {
  if (!value) return true
  return data.name?.toLowerCase().includes(value.toLowerCase()) || data.deviceId?.includes(value)
}

async function loadDevices() {
  loading.value = true
  try {
    const res: any = await deviceService.getDevices({ page: 1, count: 50 })
    if (res && res.data && res.data.list) {
      const devList = res.data.list

      // Fetch channels for each device
      const tree: any[] = []
      for (const dev of devList) {
        const devNode: any = {
          id: dev.deviceId,
          deviceId: dev.deviceId,
          name: dev.name || dev.deviceId,
          isDevice: true,
          online: dev.onLine,
          children: []
        }

        try {
          const chanRes: any = await deviceService.getDeviceChannels({
            deviceId: dev.deviceId,
            page: 1,
            count: 100
          })
          if (chanRes && chanRes.data && chanRes.data.list) {
            devNode.children = chanRes.data.list.map((c: any) => ({
              id: `${dev.deviceId}_${c.channelId}`,
              deviceId: dev.deviceId,
              channelId: c.channelId,
              name: c.name || `CH-${c.channelId}`,
              isDevice: false,
              online: c.status === 1 || c.status === true || c.online === true,
              channelInfo: c
            }))
          }
        } catch (e) {
          console.warn('Failed to fetch channels for device', dev.deviceId)
        }

        tree.push(devNode)
      }

      treeData.value = tree
    }
  } catch (e) {
    console.error('Failed to load device tree:', e)
  } finally {
    loading.value = false
  }
}

function handleNodeClick(data: any) {
  if (!data.isDevice) {
    emit('channelClick', data)
  }
}

onMounted(() => {
  loadDevices()
})

defineExpose({
  refresh: loadDevices
})
</script>

<style scoped>
:deep(.el-tree) {
  background: transparent !important;
  color: #cbd5e1;
}
:deep(.el-tree-node__content) {
  border-radius: 6px;
  margin: 2px 0;
  transition: background-color 0.15s;
}
:deep(.el-tree-node__content:hover) {
  background: rgba(51, 65, 85, 0.4) !important;
}
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(56, 189, 248, 0.15) !important;
  color: #38bdf8 !important;
}
</style>
