<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Top Filter & Query Bar -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow transition-colors">
      <div class="flex items-center gap-3 flex-wrap">
        <span class="text-xs text-slate-500 dark:text-slate-400 font-medium">Record Date:</span>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="Pick a date"
          value-format="YYYY-MM-DD"
          :clearable="false"
          size="default"
        />

        <el-button type="primary" size="default" :loading="loading" @click="queryRecords" class="!rounded-lg">
          <el-icon class="mr-1"><Search /></el-icon> Query Records
        </el-button>
      </div>

      <div class="flex items-center gap-2">
        <el-button size="default" @click="$router.back()" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">Back</el-button>
      </div>
    </div>

    <!-- Playback Container (Player + Controls + Timeline) -->
    <div class="grid grid-cols-1 lg:grid-cols-4 gap-4">
      <!-- Player Video Box (3 cols) -->
      <div class="lg:col-span-3 h-[520px] bg-black rounded-xl overflow-hidden border border-slate-200 dark:border-slate-800 shadow-xl flex flex-col">
        <div class="flex-1 w-full h-full relative">
          <PlayerTabs
            v-if="playbackStreamInfo"
            ref="playerRef"
            :stream-info="playbackStreamInfo"
            :show-tab="true"
          />
          <div v-else class="w-full h-full flex flex-col items-center justify-center text-slate-500 gap-2">
            <el-icon class="text-4xl text-slate-600"><Film /></el-icon>
            <span class="text-xs font-mono">Select a recording slice from the timeline below to play</span>
          </div>
        </div>

        <!-- Playback Speed & Transport Bar -->
        <div v-if="playbackStreamInfo" class="h-11 bg-slate-900/95 border-t border-slate-800 px-4 flex items-center justify-between text-xs text-slate-300">
          <div class="flex items-center gap-2">
            <button
              @click="togglePause"
              class="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-white font-medium flex items-center gap-1 transition"
            >
              <el-icon><VideoPause v-if="!isPaused" /><VideoPlay v-else /></el-icon>
              <span>{{ isPaused ? 'Resume' : 'Pause' }}</span>
            </button>
            <button
              @click="stopPlayback"
              class="px-2.5 py-1 rounded bg-rose-950/40 hover:bg-rose-900/60 border border-rose-800/60 text-rose-300 transition"
            >
              Stop
            </button>
          </div>

          <!-- Speed Selector -->
          <div class="flex items-center gap-1.5 bg-slate-950 px-2 py-0.5 rounded border border-slate-800">
            <span class="text-slate-400">Speed:</span>
            <button
              v-for="s in [0.5, 1, 2, 4]"
              :key="s"
              @click="changeSpeed(s)"
              class="px-1.5 py-0.5 rounded font-mono text-[11px] transition"
              :class="playbackSpeed === s ? 'bg-blue-600 text-white font-bold' : 'text-slate-400 hover:text-white'"
            >
              {{ s }}x
            </button>
          </div>
        </div>
      </div>

      <!-- Record Slices List (1 col) -->
      <div class="h-[520px] bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-3 flex flex-col gap-2 overflow-hidden shadow transition-colors">
        <span class="text-xs font-semibold text-slate-700 dark:text-slate-300 border-b border-slate-200 dark:border-slate-800 pb-2">
          Recorded Clips ({{ recordSlices.length }})
        </span>

        <div class="flex-1 overflow-y-auto flex flex-col gap-1.5 pr-1">
          <div
            v-for="(clip, idx) in recordSlices"
            :key="idx"
            class="p-2.5 rounded-lg bg-slate-50 dark:bg-slate-950/60 hover:bg-blue-50 dark:hover:bg-blue-600/20 border border-slate-200 dark:border-slate-800/80 transition flex flex-col gap-1.5"
          >
            <div class="flex items-center justify-between text-xs font-mono text-emerald-600 dark:text-emerald-400">
              <span>{{ formatTime(clip.startTime) }}</span>
              <span>-</span>
              <span>{{ formatTime(clip.endTime) }}</span>
            </div>
            <div class="flex items-center justify-between pt-1 border-t border-slate-100 dark:border-slate-800/60">
              <span class="text-[11px] text-slate-400">Continuous Record</span>
              <div class="flex gap-1">
                <el-button size="small" type="primary" link @click="playClip(clip)">Play</el-button>
                <el-button size="small" type="success" link @click="downloadClip(clip)">Download</el-button>
              </div>
            </div>
          </div>

          <div v-if="recordSlices.length === 0 && !loading" class="text-center py-10 text-xs text-slate-400">
            No video recordings found for this date
          </div>
        </div>
      </div>
    </div>

    <!-- Scrubber Timeline -->
    <VideoTimeline
      :records="recordSlices"
      :current-timestamp="currentTimestamp"
      @seek="handleTimelineSeek"
    />

    <!-- Download Progress Modal -->
    <el-dialog v-model="downloadModalVisible" title="Downloading Camera Recording" width="450px" destroy-on-close>
      <div class="flex flex-col gap-4 py-2">
        <span class="text-xs text-slate-500">Transmitting video from GB28181 camera to server...</span>
        <el-progress :percentage="downloadProgress" :status="downloadProgress === 100 ? 'success' : undefined" />
        <div class="flex justify-end gap-2 mt-2">
          <el-button v-if="downloadProgress < 100" type="danger" size="small" @click="cancelDownload">Cancel Download</el-button>
          <el-button v-else type="primary" size="small" @click="downloadModalVisible = false">Done</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { recordService } from '@/api'
import VideoTimeline, { RecordSlice } from '@/components/timeline/VideoTimeline.vue'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import { ElMessage } from 'element-plus'
import { Search, Film, VideoPlay, VideoPause } from '@element-plus/icons-vue'

const route = useRoute()
const channelId = ref((route.params.channelId as string) || '')
const deviceId = ref((route.query.deviceId as string) || '')

const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const loading = ref(false)
const recordSlices = ref<RecordSlice[]>([])
const playbackStreamInfo = ref<any>(null)
const currentTimestamp = ref<number>(Date.now())
const currentStreamId = ref<string>('')

const isPaused = ref(false)
const playbackSpeed = ref<number>(1)

const downloadModalVisible = ref(false)
const downloadProgress = ref(0)
let downloadStreamId = ''
let downloadTimer: any = null

function formatTime(ts: number) {
  return dayjs(ts).format('HH:mm:ss')
}

async function queryRecords() {
  if (!deviceId.value || !channelId.value) {
    ElMessage.warning('Missing device or channel ID')
    return
  }

  loading.value = true
  const startTime = `${selectedDate.value} 00:00:00`
  const endTime = `${selectedDate.value} 23:59:59`

  try {
    const res: any = await recordService.queryGbRecordList({
      deviceId: deviceId.value,
      channelId: channelId.value,
      startTime,
      endTime,
    })

    if (res && res.data && res.data.recordList) {
      recordSlices.value = res.data.recordList.map((item: any) => ({
        startTime: dayjs(item.startTime).valueOf(),
        endTime: dayjs(item.endTime).valueOf(),
        type: 'normal',
      }))
      ElMessage.success(`Found ${recordSlices.value.length} recording files`)
    } else {
      recordSlices.value = []
    }
  } catch (e) {
    console.warn('Failed to query GB records:', e)
  } finally {
    loading.value = false
  }
}

async function playClip(clip: RecordSlice) {
  const startStr = dayjs(clip.startTime).format('YYYY-MM-DD HH:mm:ss')
  const endStr = dayjs(clip.endTime).format('YYYY-MM-DD HH:mm:ss')

  try {
    const res: any = await recordService.startGbPlayback(deviceId.value, channelId.value, startStr, endStr)
    if (res && res.data) {
      playbackStreamInfo.value = res.data
      currentStreamId.value = res.data.stream
      currentTimestamp.value = clip.startTime
      isPaused.value = false
      playbackSpeed.value = 1
    }
  } catch (e) {
    ElMessage.error('Failed to initiate playback stream')
  }
}

async function togglePause() {
  if (!currentStreamId.value) return
  if (isPaused.value) {
    await recordService.resumeGbPlayback(currentStreamId.value)
    isPaused.value = false
  } else {
    await recordService.pauseGbPlayback(currentStreamId.value)
    isPaused.value = true
  }
}

async function changeSpeed(speed: number) {
  if (!currentStreamId.value) return
  try {
    await recordService.setGbPlaybackSpeed(currentStreamId.value, speed)
    playbackSpeed.value = speed
    ElMessage.success(`Playback speed: ${speed}x`)
  } catch (e) {
    ElMessage.error('Failed to set speed')
  }
}

async function stopPlayback() {
  if (currentStreamId.value) {
    await recordService.stopGbPlayback(deviceId.value, channelId.value, currentStreamId.value)
    playbackStreamInfo.value = null
    currentStreamId.value = ''
  }
}

async function downloadClip(clip: RecordSlice) {
  const startStr = dayjs(clip.startTime).format('YYYY-MM-DD HH:mm:ss')
  const endStr = dayjs(clip.endTime).format('YYYY-MM-DD HH:mm:ss')

  try {
    const res: any = await recordService.startGbDownload({
      deviceId: deviceId.value,
      channelId: channelId.value,
      startTime: startStr,
      endTime: endStr,
      downloadSpeed: 4,
    })
    if (res && res.data) {
      downloadStreamId = res.data.stream
      downloadProgress.value = 5
      downloadModalVisible.value = true
      startProgressPolling()
    }
  } catch (e) {
    ElMessage.error('Failed to start download task')
  }
}

function startProgressPolling() {
  clearInterval(downloadTimer)
  downloadTimer = setInterval(async () => {
    if (!downloadStreamId) return
    try {
      const res: any = await recordService.queryGbDownloadProgress(deviceId.value, channelId.value, downloadStreamId)
      if (res && res.data) {
        downloadProgress.value = Math.min(100, Math.round(res.data.progress * 100))
        if (downloadProgress.value >= 100) {
          clearInterval(downloadTimer)
          ElMessage.success('Video clip successfully downloaded!')
        }
      }
    } catch (e) {
      clearInterval(downloadTimer)
    }
  }, 1500)
}

async function cancelDownload() {
  if (downloadStreamId) {
    await recordService.stopGbDownload(deviceId.value, channelId.value, downloadStreamId)
  }
  clearInterval(downloadTimer)
  downloadModalVisible.value = false
}

async function handleTimelineSeek(ts: number) {
  currentTimestamp.value = ts
  if (currentStreamId.value) {
    const seekSeconds = Math.round((ts - dayjs(selectedDate.value).startOf('day').valueOf()) / 1000)
    try {
      await recordService.seekGbPlayback(currentStreamId.value, seekSeconds)
    } catch (e) {
      console.warn('Playback seek failed:', e)
    }
  }
}

onMounted(() => {
  queryRecords()
})
</script>
