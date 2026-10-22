<template>
  <div class="h-[calc(100vh-112px)] flex gap-3 select-none overflow-hidden">
    <!-- Left: Device Tree Panel -->
    <div
      class="h-full transition-all duration-300 flex-shrink-0"
      :class="showTree ? 'w-72' : 'w-0 overflow-hidden opacity-0 pointer-events-none'"
    >
      <DeviceTree @channel-click="handleChannelSelect" />
    </div>

    <!-- Center: Main Video Grid & Controls -->
    <div class="flex-1 h-full flex flex-col bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-2xl transition-colors">
      <!-- Live Toolbar -->
      <div class="h-12 bg-slate-50 dark:bg-slate-900/90 border-b border-slate-200 dark:border-slate-800 px-3 flex items-center justify-between text-xs text-slate-700 dark:text-slate-300">
        <!-- Left: Toggle Tree + Splits -->
        <div class="flex items-center gap-3">
          <button
            @click="showTree = !showTree"
            class="px-2.5 py-1.5 rounded-lg bg-white dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 flex items-center gap-1.5 transition text-xs font-medium"
            :title="showTree ? 'Hide Device Tree' : 'Show Device Tree'"
          >
            <el-icon><Menu /></el-icon>
            <span>{{ showTree ? 'Hide Tree' : 'Show Tree' }}</span>
          </button>

          <div class="h-4 w-[1px] bg-slate-200 dark:bg-slate-800"></div>

          <!-- Split Screen Layout Selector -->
          <div class="flex items-center gap-1 bg-slate-100 dark:bg-slate-950 p-1 rounded-lg border border-slate-200 dark:border-slate-800">
            <button
              v-for="s in splitOptions"
              :key="s.count"
              @click="setSplit(s.count)"
              class="px-2 py-0.5 rounded font-mono text-xs transition"
              :class="splitCount === s.count ? 'bg-blue-600 text-white font-bold shadow' : 'text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'"
            >
              {{ s.label }}
            </button>
          </div>
        </div>

        <!-- Right: Global Player Selector + PTZ + Fullscreen -->
        <div class="flex items-center gap-3">
          <!-- Active Cell Info -->
          <span v-if="activeChannel" class="hidden md:inline text-slate-500 dark:text-slate-400 text-xs">
            Selected: <strong class="text-blue-600 dark:text-blue-400">{{ activeChannel.name || activeChannel.channelId }}</strong>
          </span>

          <!-- PTZ Drawer Toggle -->
          <button
            @click="showPtz = !showPtz"
            class="px-2.5 py-1.5 rounded-lg border flex items-center gap-1.5 transition text-xs font-medium"
            :class="showPtz ? 'bg-blue-600 border-blue-500 text-white' : 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700'"
          >
            <el-icon><Compass /></el-icon>
            <span>PTZ Controls</span>
          </button>

          <!-- Stop All Streams -->
          <button
            @click="stopAllStreams"
            class="px-2.5 py-1.5 rounded-lg bg-rose-50 dark:bg-rose-950/40 hover:bg-rose-100 dark:hover:bg-rose-900/60 border border-rose-200 dark:border-rose-800/60 text-rose-600 dark:text-rose-300 flex items-center gap-1.5 transition text-xs font-medium"
          >
            <el-icon><CircleCloseFilled /></el-icon>
            <span>Stop All</span>
          </button>

          <!-- Fullscreen Grid -->
          <button
            @click="toggleGridFullscreen"
            class="w-8 h-8 rounded-lg bg-white dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 flex items-center justify-center transition"
            title="Grid Fullscreen"
          >
            <el-icon class="text-base"><FullScreen /></el-icon>
          </button>
        </div>
      </div>

      <!-- Main Video Grid Area -->
      <div
        ref="gridContainerRef"
        class="flex-1 p-2 bg-slate-950 grid gap-2 overflow-hidden"
        :class="gridClass"
      >
        <div
          v-for="(cell, index) in cells"
          :key="index"
          @click="selectCell(index)"
          class="relative bg-black rounded-lg overflow-hidden border-2 transition-all flex items-center justify-center group"
          :class="activeCellIndex === index ? 'border-blue-500 shadow-lg shadow-blue-500/20' : 'border-slate-800/80 hover:border-slate-700'"
        >
          <!-- Cell Player Instance -->
          <template v-if="cell.streamInfo">
            <PlayerTabs
              :ref="(el) => setPlayerRef(el, index)"
              :stream-info="cell.streamInfo"
              :title="cell.title"
              :show-tab="false"
              class="w-full h-full"
            />

            <!-- Cell Top Action Buttons -->
            <div class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex items-center gap-1 bg-black/60 backdrop-blur-sm p-1 rounded-md z-20">
              <button
                @click.stop="stopCellStream(index)"
                class="w-6 h-6 rounded bg-rose-600/80 hover:bg-rose-600 text-white flex items-center justify-center transition"
                title="Close Stream"
              >
                <el-icon class="text-xs"><Close /></el-icon>
              </button>
            </div>
          </template>

          <!-- Empty Cell Placeholder -->
          <div
            v-else
            class="flex flex-col items-center justify-center gap-2 text-slate-600 select-none cursor-pointer"
          >
            <div class="w-10 h-10 rounded-full border border-slate-800 flex items-center justify-center text-slate-500 group-hover:border-blue-500 group-hover:text-blue-400 transition">
              <el-icon class="text-xl"><VideoCamera /></el-icon>
            </div>
            <span class="text-[11px] font-mono tracking-wider text-slate-500">Screen #{{ index + 1 }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: PTZ Pad Panel (Collapsible) -->
    <div
      class="h-full transition-all duration-300 flex-shrink-0"
      :class="showPtz ? 'w-80' : 'w-0 overflow-hidden opacity-0 pointer-events-none'"
    >
      <PtzControls
        :device-id="activeChannel?.deviceId"
        :channel-id="activeChannel?.channelId"
        class="h-full"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import DeviceTree from '@/components/tree/DeviceTree.vue'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import PtzControls from '@/components/player/PtzControls.vue'
import { playService } from '@/api'
import screenfull from 'screenfull'
import { ElMessage } from 'element-plus'
import {
  Menu,
  Compass,
  CircleCloseFilled,
  FullScreen,
  VideoCamera,
  Close,
} from '@element-plus/icons-vue'

interface VideoCell {
  channelId?: string
  deviceId?: string
  streamInfo?: any
  title?: string
}

const showTree = ref(true)
const showPtz = ref(false)
const splitCount = ref(4)
const activeCellIndex = ref(0)
const cells = ref<VideoCell[]>([])
const activeChannel = ref<any>(null)
const playerRefs = ref<any[]>([])
const gridContainerRef = ref<HTMLDivElement | null>(null)

const splitOptions = [
  { label: '1x1', count: 1 },
  { label: '2x2', count: 4 },
  { label: '3x3', count: 9 },
  { label: '4x4', count: 16 },
]

const gridClass = computed(() => {
  switch (splitCount.value) {
    case 1:
      return 'grid-cols-1 grid-rows-1'
    case 4:
      return 'grid-cols-2 grid-rows-2'
    case 9:
      return 'grid-cols-3 grid-rows-3'
    case 16:
      return 'grid-cols-4 grid-rows-4'
    default:
      return 'grid-cols-2 grid-rows-2'
  }
})

function initCells(count: number) {
  const current = cells.value
  const newCells: VideoCell[] = []
  for (let i = 0; i < count; i++) {
    newCells.push(current[i] || {})
  }
  cells.value = newCells
  if (activeCellIndex.value >= count) {
    activeCellIndex.value = 0
  }
}

function setSplit(count: number) {
  splitCount.value = count
  initCells(count)
}

function selectCell(idx: number) {
  activeCellIndex.value = idx
  const cell = cells.value[idx]
  if (cell && cell.channelId) {
    activeChannel.value = {
      deviceId: cell.deviceId,
      channelId: cell.channelId,
      name: cell.title,
    }
  }
}

function setPlayerRef(el: any, index: number) {
  if (el) {
    playerRefs.value[index] = el
  }
}

async function handleChannelSelect(channel: any) {
  const targetIndex = activeCellIndex.value
  activeChannel.value = channel

  try {
    const res: any = await playService.startPlay(channel.deviceId, channel.channelId)
    if (res && res.data) {
      cells.value[targetIndex] = {
        deviceId: channel.deviceId,
        channelId: channel.channelId,
        streamInfo: res.data,
        title: channel.name || channel.channelId,
      }
      activeCellIndex.value = (targetIndex + 1) % splitCount.value
    }
  } catch (e: any) {
    ElMessage.error('Failed to start camera playback')
  }
}

function stopCellStream(index: number) {
  const cell = cells.value[index]
  if (cell && cell.deviceId && cell.channelId) {
    playService.stopPlay(cell.deviceId, cell.channelId).catch(() => {})
  }
  cells.value[index] = {}
}

function stopAllStreams() {
  for (let i = 0; i < cells.value.length; i++) {
    stopCellStream(i)
  }
  ElMessage.info('All active streams closed')
}

function toggleGridFullscreen() {
  if (gridContainerRef.value && screenfull.isEnabled) {
    screenfull.toggle(gridContainerRef.value)
  }
}

onMounted(() => {
  initCells(splitCount.value)
})
</script>
