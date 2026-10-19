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
    <div class="flex-1 h-full flex flex-col bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-2xl">
      <!-- Live Toolbar -->
      <div class="h-12 bg-slate-900/90 border-b border-slate-800 px-3 flex items-center justify-between text-xs text-slate-300">
        <!-- Left: Toggle Tree + Splits -->
        <div class="flex items-center gap-3">
          <button
            @click="showTree = !showTree"
            class="px-2.5 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 flex items-center gap-1.5 transition text-xs font-medium"
            :title="showTree ? 'Hide Device Tree' : 'Show Device Tree'"
          >
            <el-icon><Menu /></el-icon>
            <span>{{ showTree ? 'Hide Tree' : 'Show Tree' }}</span>
          </button>

          <div class="h-4 w-[1px] bg-slate-800"></div>

          <!-- Split Screen Layout Selector -->
          <div class="flex items-center gap-1 bg-slate-950 p-1 rounded-lg border border-slate-800">
            <button
              v-for="s in splitOptions"
              :key="s.count"
              @click="setSplit(s.count)"
              class="px-2 py-0.5 rounded font-mono text-xs transition"
              :class="splitCount === s.count ? 'bg-blue-600 text-white font-bold shadow' : 'text-slate-400 hover:text-white'"
            >
              {{ s.label }}
            </button>
          </div>
        </div>

        <!-- Right: Global Player Selector + PTZ + Fullscreen -->
        <div class="flex items-center gap-3">
          <!-- Active Cell Info -->
          <span v-if="activeChannel" class="hidden md:inline text-slate-400 text-xs">
            Selected: <strong class="text-blue-400">{{ activeChannel.name || activeChannel.channelId }}</strong>
          </span>

          <!-- PTZ Drawer Toggle -->
          <button
            @click="showPtz = !showPtz"
            class="px-2.5 py-1.5 rounded-lg border flex items-center gap-1.5 transition text-xs font-medium"
            :class="showPtz ? 'bg-blue-600 border-blue-500 text-white' : 'bg-slate-800 border-slate-700 text-slate-300 hover:bg-slate-700'"
          >
            <el-icon><Compass /></el-icon>
            <span>PTZ Pad</span>
          </button>

          <!-- Stop All Streams -->
          <button
            @click="stopAllStreams"
            class="px-2.5 py-1.5 rounded-lg bg-rose-950/40 hover:bg-rose-900/60 border border-rose-800/60 text-rose-300 flex items-center gap-1.5 transition text-xs font-medium"
          >
            <el-icon><CircleCloseFilled /></el-icon>
            <span>Stop All</span>
          </button>

          <!-- Fullscreen Grid -->
          <button
            @click="toggleGridFullscreen"
            class="w-8 h-8 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 flex items-center justify-center transition"
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
        :style="gridLayoutStyle"
      >
        <div
          v-for="index in splitCount"
          :key="index - 1"
          class="relative w-full h-full bg-slate-900 border-2 rounded-lg overflow-hidden transition-all group flex items-center justify-center cursor-pointer"
          :class="activeIndex === index - 1 ? 'border-blue-500 shadow-md shadow-blue-500/20' : 'border-slate-800/80 hover:border-slate-700'"
          @click="activeIndex = index - 1"
          @dblclick="handleCellDblClick(index - 1)"
        >
          <!-- Active Stream Player -->
          <PlayerTabs
            v-if="cellStreams[index - 1]"
            :ref="(el) => setPlayerRef(el, index - 1)"
            :stream-info="cellStreams[index - 1]"
            :title="cellTitles[index - 1]"
            :show-tab="true"
            class="w-full h-full"
          />

          <!-- Empty Slot Placeholder -->
          <div v-else class="flex flex-col items-center justify-center text-slate-600 gap-2">
            <el-icon class="text-3xl text-slate-700 group-hover:text-blue-500/50 transition"><VideoCamera /></el-icon>
            <span class="text-xs font-mono tracking-wider">CH - {{ index }} (Select from Tree)</span>
          </div>

          <!-- Top-Right Close Stream Icon -->
          <button
            v-if="cellStreams[index - 1]"
            @click.stop="stopCellStream(index - 1)"
            class="absolute top-2 right-2 w-6 h-6 rounded-full bg-black/70 hover:bg-rose-600 text-white flex items-center justify-center text-xs opacity-0 group-hover:opacity-100 transition z-10"
            title="Close Stream"
          >
            ✕
          </button>
        </div>
      </div>
    </div>

    <!-- Right Slide-out: PTZ Control Panel -->
    <div
      v-if="showPtz"
      class="w-72 h-full flex-shrink-0 animate-in slide-in-from-right duration-200"
    >
      <PtzControls
        :device-id="activeChannel?.deviceId"
        :channel-id="activeChannel?.channelId"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import DeviceTree from '@/components/tree/DeviceTree.vue'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import PtzControls from '@/components/player/PtzControls.vue'
import { playService } from '@/api'
import { ElMessage } from 'element-plus'
import screenfull from 'screenfull'
import {
  Menu,
  Compass,
  CircleCloseFilled,
  FullScreen,
  VideoCamera
} from '@element-plus/icons-vue'

const showTree = ref(true)
const showPtz = ref(false)
const splitCount = ref<number>(4)
const previousSplit = ref<number>(4)
const activeIndex = ref<number>(0)
const gridContainerRef = ref<HTMLDivElement | null>(null)

const splitOptions = [
  { label: '1 Screen', count: 1 },
  { label: '4 Screens', count: 4 },
  { label: '9 Screens', count: 9 },
  { label: '16 Screens', count: 16 },
]

// Slot data: up to 16 slots
const cellStreams = ref<any[]>(new Array(16).fill(null))
const cellTitles = ref<string[]>(new Array(16).fill(''))
const cellChannels = ref<any[]>(new Array(16).fill(null))
const playerRefs = ref<any[]>([])

function setPlayerRef(el: any, idx: number) {
  if (el) {
    playerRefs.value[idx] = el
  }
}

const activeChannel = computed(() => {
  return cellChannels.value[activeIndex.value] || null
})

const gridLayoutStyle = computed(() => {
  if (splitCount.value === 1) {
    return { gridTemplateColumns: '1fr', gridTemplateRows: '1fr' }
  } else if (splitCount.value === 4) {
    return { gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gridTemplateRows: 'repeat(2, minmax(0, 1fr))' }
  } else if (splitCount.value === 9) {
    return { gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gridTemplateRows: 'repeat(3, minmax(0, 1fr))' }
  } else {
    return { gridTemplateColumns: 'repeat(4, minmax(0, 1fr))', gridTemplateRows: 'repeat(4, minmax(0, 1fr))' }
  }
})

function setSplit(count: number) {
  splitCount.value = count
}

function handleCellDblClick(index: number) {
  if (splitCount.value === 1) {
    splitCount.value = previousSplit.value
  } else {
    previousSplit.value = splitCount.value
    splitCount.value = 1
    activeIndex.value = index
  }
}

async function handleChannelSelect(channelData: any) {
  const idx = activeIndex.value
  const devId = channelData.deviceId
  const chanId = channelData.channelId

  if (!devId || !chanId) return

  ElMessage.info(`Starting stream for ${channelData.name}...`)

  try {
    const res: any = await playService.startPlay(devId, chanId)
    if (res && res.data) {
      cellStreams.value[idx] = res.data
      cellTitles.value[idx] = channelData.name || `CH-${chanId}`
      cellChannels.value[idx] = { deviceId: devId, channelId: chanId, name: channelData.name }
      ElMessage.success(`Stream loaded on slot #${idx + 1}`)

      // Advance active slot to the next cell automatically for convenience!
      activeIndex.value = (idx + 1) % splitCount.value
    }
  } catch (e: any) {
    console.error('Play stream failed:', e)
  }
}

async function stopCellStream(idx: number) {
  const channel = cellChannels.value[idx]
  if (channel) {
    try {
      await playService.stopPlay(channel.deviceId, channel.channelId)
    } catch (e) {
      console.warn('Stop stream warning:', e)
    }
  }

  if (playerRefs.value[idx]) {
    playerRefs.value[idx].stop()
  }

  cellStreams.value[idx] = null
  cellTitles.value[idx] = ''
  cellChannels.value[idx] = null
}

async function stopAllStreams() {
  for (let i = 0; i < 16; i++) {
    if (cellStreams.value[i]) {
      stopCellStream(i)
    }
  }
  ElMessage.info('All active streams stopped')
}

function toggleGridFullscreen() {
  if (gridContainerRef.value) {
    if (screenfull.isEnabled) {
      screenfull.toggle(gridContainerRef.value)
    }
  }
}
</script>
