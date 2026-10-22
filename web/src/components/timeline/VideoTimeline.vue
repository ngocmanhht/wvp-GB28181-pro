<template>
  <div class="w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-3 flex flex-col gap-2 select-none shadow-lg transition-colors">
    <!-- Timeline Header -->
    <div class="flex items-center justify-between text-xs text-slate-700 dark:text-slate-300">
      <div class="flex items-center gap-2">
        <span class="w-2 h-2 rounded-full bg-blue-500"></span>
        <span class="font-medium">Video Scrubber Timeline</span>
        <span class="font-mono text-emerald-600 dark:text-emerald-400 font-semibold ml-2">{{ currentTimeText }}</span>
      </div>

      <!-- Zoom and controls -->
      <div class="flex items-center gap-2">
        <el-radio-group v-model="zoomLevel" size="small" @change="drawTimeline">
          <el-radio-button :value="24">24h</el-radio-button>
          <el-radio-button :value="12">12h</el-radio-button>
          <el-radio-button :value="4">4h</el-radio-button>
          <el-radio-button :value="1">1h</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- Canvas Scrubber Bar -->
    <div
      ref="containerRef"
      class="relative w-full h-16 bg-slate-100 dark:bg-slate-950 rounded-lg overflow-hidden cursor-pointer border border-slate-200 dark:border-slate-800"
      @mousedown="startDragging"
    >
      <canvas ref="canvasRef" class="w-full h-full block"></canvas>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import dayjs from 'dayjs'
import { useAppStore } from '@/stores/app'

export interface RecordSlice {
  startTime: number // timestamp in ms
  endTime: number   // timestamp in ms
  type?: 'normal' | 'alarm'
}

const props = withDefaults(
  defineProps<{
    records?: RecordSlice[]
    currentTimestamp?: number
  }>(),
  {
    records: () => [],
    currentTimestamp: Date.now(),
  }
)

const emit = defineEmits(['timeChange', 'seek'])

const appStore = useAppStore()
const containerRef = ref<HTMLDivElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)

const zoomLevel = ref<number>(24) // 24h, 12h, 4h, 1h
const internalTime = ref<number>(props.currentTimestamp || Date.now())
const isDragging = ref<boolean>(false)

const currentTimeText = computed(() => {
  return dayjs(internalTime.value).format('YYYY-MM-DD HH:mm:ss')
})

function drawTimeline() {
  const canvas = canvasRef.value
  if (!canvas || !containerRef.value) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight

  const dpr = window.devicePixelRatio || 1
  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`

  ctx.setTransform(1, 0, 0, 1, 0, 0)
  ctx.scale(dpr, dpr)

  const isDark = appStore.isDark

  // Background
  ctx.fillStyle = isDark ? '#020617' : '#f8fafc'
  ctx.fillRect(0, 0, width, height)

  // Calculate start and end range around internalTime or start of today
  const rangeMs = zoomLevel.value * 3600 * 1000
  const startMs = internalTime.value - rangeMs / 2
  const endMs = internalTime.value + rangeMs / 2

  // 1. Draw Recording Slices
  if (props.records && props.records.length > 0) {
    for (const slice of props.records) {
      if (slice.endTime < startMs || slice.startTime > endMs) continue

      const x1 = Math.max(0, ((slice.startTime - startMs) / rangeMs) * width)
      const x2 = Math.min(width, ((slice.endTime - startMs) / rangeMs) * width)
      const sliceWidth = Math.max(2, x2 - x1)

      ctx.fillStyle = slice.type === 'alarm' ? '#f43f5e' : '#10b981'
      ctx.fillRect(x1, 10, sliceWidth, 24)
    }
  }

  // 2. Draw Time Ruler Ticks
  ctx.strokeStyle = isDark ? '#334155' : '#cbd5e1'
  ctx.fillStyle = isDark ? '#94a3b8' : '#64748b'
  ctx.font = '10px monospace'
  ctx.textAlign = 'center'

  const stepMs = rangeMs / (zoomLevel.value <= 1 ? 6 : 12)

  const firstTick = Math.ceil(startMs / stepMs) * stepMs
  for (let t = firstTick; t <= endMs; t += stepMs) {
    const x = ((t - startMs) / rangeMs) * width
    ctx.beginPath()
    ctx.moveTo(x, 40)
    ctx.lineTo(x, 48)
    ctx.stroke()

    ctx.fillText(dayjs(t).format('HH:mm'), x, 58)
  }

  // 3. Draw Center Current Time Indicator (Red line)
  const centerX = width / 2
  ctx.strokeStyle = '#ef4444'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(centerX, 0)
  ctx.lineTo(centerX, height)
  ctx.stroke()

  // Top Triangle
  ctx.fillStyle = '#ef4444'
  ctx.beginPath()
  ctx.moveTo(centerX - 5, 0)
  ctx.lineTo(centerX + 5, 0)
  ctx.lineTo(centerX, 7)
  ctx.fill()
}

function handleMouseMove(e: MouseEvent) {
  if (!isDragging.value || !containerRef.value) return
  const rect = containerRef.value.getBoundingClientRect()
  const dx = e.clientX - rect.left - rect.width / 2

  const rangeMs = zoomLevel.value * 3600 * 1000
  const deltaMs = (dx / rect.width) * rangeMs

  internalTime.value += deltaMs
  drawTimeline()
  emit('timeChange', internalTime.value)
}

function handleMouseUp() {
  if (isDragging.value) {
    isDragging.value = false
    emit('seek', internalTime.value)
  }
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseup', handleMouseUp)
}

function startDragging(e: MouseEvent) {
  isDragging.value = true
  if (containerRef.value) {
    const rect = containerRef.value.getBoundingClientRect()
    const clickX = e.clientX - rect.left
    const rangeMs = zoomLevel.value * 3600 * 1000
    const startMs = internalTime.value - rangeMs / 2
    internalTime.value = startMs + (clickX / rect.width) * rangeMs
    drawTimeline()
    emit('seek', internalTime.value)
  }
  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mouseup', handleMouseUp)
}

watch(
  () => props.records,
  () => {
    drawTimeline()
  },
  { deep: true }
)

watch(
  () => props.currentTimestamp,
  (val) => {
    if (val && !isDragging.value) {
      internalTime.value = val
      drawTimeline()
    }
  }
)

watch(
  () => appStore.isDark,
  () => {
    drawTimeline()
  }
)

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  drawTimeline()
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      drawTimeline()
    })
    resizeObserver.observe(containerRef.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseup', handleMouseUp)
})

defineExpose({
  setTime: (ts: number) => {
    internalTime.value = ts
    drawTimeline()
  },
})
</script>
