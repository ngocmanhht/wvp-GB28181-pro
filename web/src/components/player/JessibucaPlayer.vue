<template>
  <div
    ref="containerRef"
    class="relative w-full h-full bg-black select-none overflow-hidden group flex items-center justify-center"
    @dblclick="toggleFullscreen"
    @mouseenter="showBar = true"
    @mouseleave="showBar = false"
  >
    <!-- Video Canvas / Container rendered by Jessibuca -->
    <div ref="playBoxRef" class="w-full h-full"></div>

    <!-- Error / No signal overlay -->
    <div v-if="errorText" class="absolute inset-0 flex flex-col items-center justify-center bg-black/80 text-rose-400 gap-2 p-4 text-center">
      <el-icon class="text-3xl"><WarningFilled /></el-icon>
      <span class="text-sm font-medium">{{ errorText }}</span>
    </div>

    <!-- Hover Controls Bar -->
    <div
      v-if="showButton"
      class="absolute bottom-0 left-0 right-0 h-10 bg-gradient-to-t from-black/90 via-black/60 to-transparent px-3 flex items-center justify-between text-white transition-opacity duration-200"
      :class="showBar ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'"
    >
      <!-- Left Controls: Play/Stop/Audio -->
      <div class="flex items-center gap-3 text-sm">
        <button
          v-if="!playing"
          class="hover:text-blue-400 transition"
          title="Play"
          @click.stop="play(videoUrl)"
        >
          <el-icon class="text-base"><VideoPlay /></el-icon>
        </button>
        <button
          v-else
          class="hover:text-blue-400 transition"
          title="Pause"
          @click.stop="pause"
        >
          <el-icon class="text-base"><VideoPause /></el-icon>
        </button>

        <button
          class="hover:text-rose-400 transition"
          title="Stop"
          @click.stop="stop"
        >
          <el-icon class="text-base"><SwitchButton /></el-icon>
        </button>

        <button
          class="hover:text-blue-400 transition"
          :title="isMuted ? 'Unmute' : 'Mute'"
          @click.stop="toggleMute"
        >
          <el-icon class="text-base">
            <MuteNotification v-if="isMuted" />
            <Bell v-else />
          </el-icon>
        </button>
      </div>

      <!-- Right Controls: Bitrate / Snap / Fullscreen -->
      <div class="flex items-center gap-3 text-xs text-slate-300">
        <span v-if="kBps > 0" class="font-mono text-[11px] bg-slate-800/80 px-1.5 py-0.5 rounded text-emerald-400">
          {{ kBps }} kb/s
        </span>

        <button
          class="hover:text-blue-400 transition p-1"
          title="Take Screenshot"
          @click.stop="screenshot"
        >
          <el-icon class="text-base"><Camera /></el-icon>
        </button>

        <button
          class="hover:text-blue-400 transition p-1"
          title="Fullscreen"
          @click.stop="toggleFullscreen"
        >
          <el-icon class="text-base"><FullScreen /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import {
  VideoPlay,
  VideoPause,
  SwitchButton,
  Bell,
  MuteNotification,
  Camera,
  FullScreen,
  WarningFilled
} from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    videoUrl?: string
    hasAudio?: boolean
    showButton?: boolean
    autoplay?: boolean
  }>(),
  {
    videoUrl: '',
    hasAudio: false,
    showButton: true,
    autoplay: true,
  }
)

const emit = defineEmits(['playTimeChange', 'playStatusChange', 'screenshot'])

const containerRef = ref<HTMLDivElement | null>(null)
const playBoxRef = ref<HTMLDivElement | null>(null)

const playerInstance = ref<any>(null)
const playing = ref(false)
const isMuted = ref(true)
const kBps = ref(0)
const showBar = ref(false)
const errorText = ref('')

function createPlayer() {
  if (typeof (window as any).Jessibuca === 'undefined') {
    errorText.value = 'Jessibuca player library is not loaded'
    return
  }

  destroyPlayer()

  try {
    const options = {
      container: playBoxRef.value,
      videoBuffer: 0.1,
      isResize: true,
      useMSE: true,
      useWCS: false,
      text: '',
      loadingText: 'Loading stream...',
      debug: false,
      hotKey: true,
      decoder: '/static/js/jessibuca/decoder.js',
      isNotMute: !isMuted.value,
      timeout: 10,
      supportH265: true,
      heartTimeout: 10,
      heartTimeoutReplay: true,
    }

    playerInstance.value = new (window as any).Jessibuca(options)

    playerInstance.value.on('load', () => {
      playing.value = true
      errorText.value = ''
      emit('playStatusChange', 'playing')
    })

    playerInstance.value.on('error', (err: any) => {
      console.warn('Jessibuca error:', err)
      errorText.value = typeof err === 'string' ? err : 'Stream playback error'
      emit('playStatusChange', 'error')
    })

    playerInstance.value.on('kBps', (val: number) => {
      kBps.value = Math.round(val)
    })

    playerInstance.value.on('timeUpdate', (ts: number) => {
      emit('playTimeChange', ts)
    })
  } catch (e: any) {
    console.error('Failed to init Jessibuca:', e)
    errorText.value = e.message || 'Player initialization error'
  }
}

function play(url?: string) {
  const targetUrl = url || props.videoUrl
  if (!targetUrl) return

  errorText.value = ''
  if (!playerInstance.value) {
    createPlayer()
  }

  if (playerInstance.value) {
    playerInstance.value.play(targetUrl)
    playing.value = true
  }
}

function pause() {
  if (playerInstance.value) {
    playerInstance.value.pause()
    playing.value = false
    emit('playStatusChange', 'paused')
  }
}

function stop() {
  if (playerInstance.value) {
    playerInstance.value.destroy()
    playerInstance.value = null
  }
  playing.value = false
  kBps.value = 0
  errorText.value = ''
  emit('playStatusChange', 'stopped')
}

function toggleMute() {
  if (!playerInstance.value) return
  if (isMuted.value) {
    playerInstance.value.cancelMute()
    isMuted.value = false
  } else {
    playerInstance.value.mute()
    isMuted.value = true
  }
}

function screenshot() {
  if (playerInstance.value) {
    const filename = `snapshot_${Date.now()}.png`
    playerInstance.value.screenshot(filename, 'png', 0.92, 'download')
    emit('screenshot', filename)
  }
}

function toggleFullscreen() {
  if (playerInstance.value) {
    playerInstance.value.setFullscreen(true)
  }
}

function destroyPlayer() {
  if (playerInstance.value) {
    try {
      playerInstance.value.destroy()
    } catch (e) {
      console.warn('Error destroying player:', e)
    }
    playerInstance.value = null
  }
  playing.value = false
  kBps.value = 0
}

watch(
  () => props.videoUrl,
  (newUrl) => {
    if (newUrl) {
      play(newUrl)
    } else {
      stop()
    }
  }
)

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  if (props.videoUrl && props.autoplay) {
    play(props.videoUrl)
  }

  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      if (playerInstance.value && typeof playerInstance.value.resize === 'function') {
        playerInstance.value.resize()
      }
    })
    resizeObserver.observe(containerRef.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  destroyPlayer()
})

defineExpose({
  play,
  pause,
  stop,
  screenshot,
  destroy: destroyPlayer,
})
</script>
