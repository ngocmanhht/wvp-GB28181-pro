<template>
  <div
    class="relative w-full h-full bg-black select-none overflow-hidden group flex items-center justify-center"
    @mouseenter="showBar = true"
    @mouseleave="showBar = false"
  >
    <video
      ref="videoRef"
      class="w-full h-full object-contain"
      autoplay
      playsinline
      :muted="isMuted"
    ></video>

    <!-- Error message overlay -->
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
      <div class="flex items-center gap-3 text-sm">
        <span class="text-xs font-semibold px-2 py-0.5 rounded bg-blue-600/60 text-blue-200">
          WebRTC
        </span>

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
          @click.stop="isMuted = !isMuted"
        >
          <el-icon class="text-base">
            <MuteNotification v-if="isMuted" />
            <Bell v-else />
          </el-icon>
        </button>
      </div>

      <div class="flex items-center gap-3 text-xs text-slate-300">
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
  SwitchButton,
  Bell,
  MuteNotification,
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

const emit = defineEmits(['playStatusChange'])

const videoRef = ref<HTMLVideoElement | null>(null)
const rtcClient = ref<any>(null)
const isMuted = ref(true)
const showBar = ref(false)
const errorText = ref('')

function play(url?: string) {
  const targetUrl = url || props.videoUrl
  if (!targetUrl) return

  stop()

  if (typeof (window as any).ZLMRTCClient === 'undefined') {
    errorText.value = 'ZLMRTCClient library is not available'
    return
  }

  try {
    errorText.value = ''
    rtcClient.value = new (window as any).ZLMRTCClient.Endpoint({
      element: videoRef.value,
      debug: false,
      zlmsdpUrl: targetUrl,
      simulcast: false,
      useCamera: false,
      audioEnable: props.hasAudio,
      videoEnable: true,
      recvOnly: true,
      usedDataChannel: false,
    })

    rtcClient.value.on((window as any).ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, (e: any) => {
      console.warn('ICE Candidate error:', e)
    })

    rtcClient.value.on((window as any).ZLMRTCClient.Events.WEBRTC_ON_REMOTE_STREAMS, () => {
      emit('playStatusChange', 'playing')
    })
  } catch (err: any) {
    console.error('Failed to init WebRTC client:', err)
    errorText.value = err.message || 'WebRTC connection failed'
  }
}

function stop() {
  if (rtcClient.value) {
    try {
      rtcClient.value.close()
    } catch (e) {
      console.warn('Error closing RTC client:', e)
    }
    rtcClient.value = null
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
  errorText.value = ''
  emit('playStatusChange', 'stopped')
}

function toggleFullscreen() {
  if (videoRef.value) {
    if (document.fullscreenElement) {
      document.exitFullscreen()
    } else {
      videoRef.value.requestFullscreen?.()
    }
  }
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

onMounted(() => {
  if (props.videoUrl && props.autoplay) {
    play(props.videoUrl)
  }
})

onBeforeUnmount(() => {
  stop()
})

defineExpose({
  play,
  stop,
})
</script>
