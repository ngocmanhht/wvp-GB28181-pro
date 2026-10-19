<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Top Filter & Query Bar -->
    <div class="p-4 bg-slate-900 border border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-3 shadow">
      <div class="flex items-center gap-3 flex-wrap">
        <span class="text-xs text-slate-400 font-medium">Record Date:</span>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="Pick a date"
          value-format="YYYY-MM-DD"
          :clearable="false"
          size="default"
        />

        <el-button type="primary" size="default" :loading="loading" @click="queryRecords">
          <el-icon class="mr-1"><Search /></el-icon> Query Records
        </el-button>
      </div>

      <div class="flex items-center gap-2">
        <el-button size="default" @click="$router.back()">Back</el-button>
      </div>
    </div>

    <!-- Playback Container (Player + Timeline) -->
    <div class="grid grid-cols-1 lg:grid-cols-4 gap-4">
      <!-- Player Video Box (3 cols) -->
      <div class="lg:col-span-3 h-[520px] bg-black rounded-xl overflow-hidden border border-slate-800 shadow-xl flex flex-col">
        <div class="flex-1 w-full h-full relative">
          <PlayerTabs
            v-if="playbackStreamInfo"
            ref="playerRef"
            :stream-info="playbackStreamInfo"
            :show-tab="true"
          />
          <div v-else class="w-full h-full flex flex-col items-center justify-center text-slate-600 gap-2">
            <el-icon class="text-4xl text-slate-700"><Film /></el-icon>
            <span class="text-xs font-mono">Select a recording slice from the timeline below to play</span>
          </div>
        </div>
      </div>

      <!-- Record Slices List (1 col) -->
      <div class="h-[520px] bg-slate-900 border border-slate-800 rounded-xl p-3 flex flex-col gap-2 overflow-hidden shadow">
        <span class="text-xs font-semibold text-slate-300 border-b border-slate-800 pb-2">
          Recorded Clips ({{ recordSlices.length }})
        </span>

        <div class="flex-1 overflow-y-auto flex flex-col gap-1.5 pr-1">
          <div
            v-for="(clip, idx) in recordSlices"
            :key="idx"
            @click="playClip(clip)"
            class="p-2.5 rounded-lg bg-slate-950/60 hover:bg-blue-600/20 border border-slate-800/80 hover:border-blue-500/40 cursor-pointer transition flex flex-col gap-1"
          >
            <div class="flex items-center justify-between text-xs font-mono text-emerald-400">
              <span>{{ formatTime(clip.startTime) }}</span>
              <span>-</span>
              <span>{{ formatTime(clip.endTime) }}</span>
            </div>
            <span class="text-[11px] text-slate-400">Normal Continuous Record</span>
          </div>

          <div v-if="recordSlices.length === 0 && !loading" class="text-center py-10 text-xs text-slate-500">
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
import { Search, Film } from '@element-plus/icons-vue'

const route = useRoute()
const channelId = ref((route.params.channelId as string) || '')
const deviceId = ref((route.query.deviceId as string) || '')

const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const loading = ref(false)
const recordSlices = ref<RecordSlice[]>([])
const playbackStreamInfo = ref<any>(null)
const currentTimestamp = ref<number>(Date.now())
const currentStreamId = ref<string>('')

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
    }
  } catch (e) {
    ElMessage.error('Failed to initiate playback stream')
  }
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
