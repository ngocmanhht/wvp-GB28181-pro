<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Search Bar -->
    <div class="p-4 bg-slate-900 border border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow">
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

      <el-button size="default" @click="loadChannels" class="!rounded-lg !bg-slate-800 !text-slate-200 !border-slate-700">
        <el-icon class="mr-1"><Refresh /></el-icon> Refresh
      </el-button>
    </div>

    <!-- Channels Data Table -->
    <div class="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow overflow-hidden">
      <el-table
        :data="channelList"
        v-loading="loading"
        style="width: 100%"
        class="custom-table"
      >
        <el-table-column prop="channelId" label="Channel ID" min-width="180">
          <template #default="{ row }">
            <span class="font-mono font-medium text-blue-400">{{ row.channelId }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="Channel Name" min-width="150">
          <template #default="{ row }">
            <span class="font-medium text-slate-200">{{ row.name || 'Unnamed Channel' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="deviceId" label="Parent Device ID" min-width="170">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-400">{{ row.deviceId }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="Status" width="110" align="center">
          <template #default="{ row }">
            <span
              v-if="row.status === 1 || row.status === true"
              class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
            >
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Online
            </span>
            <span
              v-else
              class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20"
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

        <!-- Actions -->
        <el-table-column label="Actions" width="220" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1.5">
              <el-button
                size="small"
                type="primary"
                plain
                @click="handlePlay(row)"
              >
                Play
              </el-button>

              <el-button
                size="small"
                type="warning"
                plain
                @click="$router.push(`/channel/record/${row.channelId}?deviceId=${row.deviceId}`)"
              >
                Record
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
          :page-sizes="[15, 30, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadChannels"
          @current-change="loadChannels"
        />
      </div>
    </div>

    <!-- Live Stream Modal -->
    <el-dialog
      v-model="playModalVisible"
      :title="`Live: ${activeChannel?.name || activeChannel?.channelId}`"
      width="780px"
      destroy-on-close
    >
      <div class="w-full h-[450px]">
        <PlayerTabs v-if="streamInfo" :stream-info="streamInfo" :show-tab="true" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { deviceService, playService } from '@/api'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'

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

onMounted(() => {
  loadChannels()
})
</script>
