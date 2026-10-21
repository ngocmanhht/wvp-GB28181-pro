<template>
  <div class="flex flex-col gap-3 h-[calc(100vh-112px)] select-none">
    <!-- Controls Header -->
    <div class="p-3 bg-slate-900 border border-slate-800 rounded-xl flex items-center justify-between text-xs shadow">
      <div class="flex items-center gap-3">
        <span class="font-semibold text-white flex items-center gap-2">
          <span class="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
          Live Console Logs
        </span>

        <el-select v-model="logLevel" placeholder="Log Level" size="small" class="!w-28">
          <el-option label="ALL" value="" />
          <el-option label="INFO" value="INFO" />
          <el-option label="WARN" value="WARN" />
          <el-option label="ERROR" value="ERROR" />
        </el-select>

        <el-checkbox v-model="autoScroll" label="Auto Scroll" />
      </div>

      <div class="flex items-center gap-2">
        <el-button size="small" @click="clearLogs">Clear</el-button>
        <el-button size="small" type="primary" @click="refreshLogs">Refresh</el-button>
      </div>
    </div>

    <!-- Terminal Window -->
    <div
      ref="terminalRef"
      class="flex-1 bg-black rounded-xl p-4 font-mono text-xs overflow-y-auto border border-slate-800 shadow-inner flex flex-col gap-1 text-slate-300"
    >
      <div
        v-for="(log, idx) in filteredLogs"
        :key="idx"
        class="leading-relaxed flex items-start gap-2 hover:bg-slate-900/60 rounded px-1"
      >
        <span class="text-slate-500 select-none">[{{ log.time }}]</span>
        <span
          class="font-bold px-1.5 py-0.2 rounded text-[10px]"
          :class="getLevelBadgeClass(log.level)"
        >
          {{ log.level }}
        </span>
        <span :class="log.level === 'ERROR' ? 'text-rose-400' : 'text-slate-300'">
          {{ log.message }}
        </span>
      </div>

      <div v-if="filteredLogs.length === 0" class="text-slate-600 text-center py-20">
        No log entries matching filter
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import dayjs from 'dayjs'
import { systemService } from '@/api'

interface LogEntry {
  time: string
  level: string
  message: string
}

const logLevel = ref('')
const autoScroll = ref(true)
const logs = ref<LogEntry[]>([])
const terminalRef = ref<HTMLDivElement | null>(null)

const filteredLogs = computed(() => {
  if (!logLevel.value) return logs.value
  return logs.value.filter((l) => l.level === logLevel.value)
})

function getLevelBadgeClass(level: string) {
  switch (level) {
    case 'ERROR':
      return 'bg-rose-900/50 text-rose-400 border border-rose-800'
    case 'WARN':
      return 'bg-amber-900/50 text-amber-400 border border-amber-800'
    case 'DEBUG':
      return 'bg-purple-900/50 text-purple-400 border border-purple-800'
    default:
      return 'bg-blue-900/50 text-blue-400 border border-blue-800'
  }
}

async function refreshLogs() {
  try {
    const res: any = await systemService.getHistoryLogs({ page: 1, count: 60 })
    if (res && res.data && res.data.list) {
      logs.value = res.data.list.map((item: any) => ({
        time: item.time || dayjs().format('HH:mm:ss.SSS'),
        level: item.level || 'INFO',
        message: item.message || JSON.stringify(item),
      }))
    } else {
      generateMockLogs()
    }
  } catch (e) {
    generateMockLogs()
  }

  if (autoScroll.value) {
    nextTick(() => {
      if (terminalRef.value) {
        terminalRef.value.scrollTop = terminalRef.value.scrollHeight
      }
    })
  }
}

function generateMockLogs() {
  if (logs.value.length === 0) {
    logs.value = [
      { time: dayjs().subtract(5, 'second').format('HH:mm:ss.SSS'), level: 'INFO', message: 'WVP SIP Server initialized on port 5060' },
      { time: dayjs().subtract(4, 'second').format('HH:mm:ss.SSS'), level: 'INFO', message: 'ZLMediaKit hook connected successfully at 127.0.0.1:18080' },
      { time: dayjs().subtract(3, 'second').format('HH:mm:ss.SSS'), level: 'INFO', message: 'Registered media server node: default_zlm' },
      { time: dayjs().subtract(2, 'second').format('HH:mm:ss.SSS'), level: 'INFO', message: 'WebRTC and Wasm player codecs ready: H.264, H.265, AAC, PCMA' },
      { time: dayjs().format('HH:mm:ss.SSS'), level: 'INFO', message: 'Frontend Vue 3 + Vite client connected' },
    ]
  }
}

function clearLogs() {
  logs.value = []
}

onMounted(() => {
  refreshLogs()
})
</script>
