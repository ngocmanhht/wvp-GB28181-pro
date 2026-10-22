<template>
  <div class="h-full flex flex-col bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden select-none transition-colors">
    <!-- Header & Search input -->
    <div class="p-3 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/90 flex flex-col gap-2">
      <div class="flex items-center justify-between">
        <span class="font-semibold text-xs tracking-wide uppercase text-slate-500 dark:text-slate-400">Device Tree</span>
        <button
          @click="loadDevices"
          class="text-xs text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1"
          title="Refresh"
        >
          <el-icon><Refresh /></el-icon> Refresh
        </button>
      </div>

      <el-input
        v-model="searchQuery"
        placeholder="Filter cameras..."
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
              <span v-if="data.isDevice" class="text-blue-500">
                <el-icon><Monitor /></el-icon>
              </span>
              <span v-else :class="data.online ? 'text-emerald-500' : 'text-slate-400'">
                <el-icon><VideoCamera /></el-icon>
              </span>

              <span :class="data.online === false ? 'text-slate-400 line-through' : 'text-slate-700 dark:text-slate-200'" class="truncate">
                {{ data.name || data.deviceId || 'Channel' }}
              </span>
            </div>

            <!-- Badges -->
            <span
              v-if="!data.isDevice"
              class="w-2 h-2 rounded-full flex-shrink-0"
              :class="data.online ? 'bg-emerald-500 shadow-sm shadow-emerald-500/50' : 'bg-slate-300 dark:bg-slate-600'"
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
    const [devRes, chanRes]: any = await Promise.allSettled([
      deviceService.getDevices({ page: 1, count: 100 }),
      deviceService.getAllChannels({ page: 1, count: 500 }),
    ])

    const devList = (devRes.status === 'fulfilled' && devRes.value?.data?.list) ? devRes.value.data.list : []
    const allChannels = (chanRes.status === 'fulfilled' && chanRes.value?.data?.list) ? chanRes.value.data.list : []

    // Group channels by deviceId in memory (O(n) fast aggregation)
    const channelMap = new Map<string, any[]>()
    for (const ch of allChannels) {
      if (!channelMap.has(ch.deviceId)) {
        channelMap.set(ch.deviceId, [])
      }
      channelMap.get(ch.deviceId)!.push({
        id: `${ch.deviceId}_${ch.channelId}`,
        deviceId: ch.deviceId,
        channelId: ch.channelId,
        name: ch.name || `CH-${ch.channelId}`,
        isDevice: false,
        online: ch.status === 1 || ch.status === true || ch.online === true,
        channelInfo: ch,
      })
    }

    const tree: any[] = []
    for (const dev of devList) {
      tree.push({
        id: dev.deviceId,
        deviceId: dev.deviceId,
        name: dev.name || dev.deviceId,
        isDevice: true,
        online: dev.onLine,
        children: channelMap.get(dev.deviceId) || [],
      })
    }

    treeData.value = tree
  } catch (e) {
    console.warn('Failed to load device tree:', e)
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
  refresh: loadDevices,
})
</script>

<style scoped>
:deep(.el-tree) {
  background: transparent !important;
}
:deep(.el-tree-node__content) {
  border-radius: 6px;
  margin: 2px 0;
  transition: background-color 0.15s;
}
.dark :deep(.el-tree-node__content:hover) {
  background: rgba(51, 65, 85, 0.4) !important;
}
:deep(.el-tree-node__content:hover) {
  background: rgba(241, 245, 249, 0.8);
}
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(37, 99, 235, 0.1) !important;
  color: #2563eb !important;
}
.dark :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(56, 189, 248, 0.15) !important;
  color: #38bdf8 !important;
}
</style>
