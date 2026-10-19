<template>
  <div class="relative w-full h-full flex flex-col bg-black overflow-hidden rounded-lg border border-slate-800">
    <!-- Header Protocol Selector (if showTab is true) -->
    <div v-if="showTab" class="h-8 bg-slate-900 border-b border-slate-800 px-3 flex items-center justify-between text-xs text-slate-300">
      <div class="flex items-center gap-1">
        <button
          v-for="p in playerList"
          :key="p.key"
          @click="switchPlayer(p.key as 'jessibuca' | 'webRTC')"
          class="px-2.5 py-1 rounded transition text-[11px] font-medium"
          :class="activePlayer === p.key ? 'bg-blue-600 text-white shadow-sm' : 'hover:bg-slate-800 text-slate-400'"
        >
          {{ p.label }}
        </button>
      </div>

      <span v-if="title" class="truncate max-w-[200px] text-slate-400 font-mono text-[11px]">
        {{ title }}
      </span>
    </div>

    <!-- Video Render Area -->
    <div class="flex-1 w-full h-full relative">
      <JessibucaPlayer
        v-if="activePlayer === 'jessibuca'"
        ref="jessibucaRef"
        :video-url="currentPlayUrl"
        :has-audio="hasAudio"
        :show-button="showButton"
        @play-status-change="$emit('playStatusChange', $event)"
      />
      <RtcPlayer
        v-else-if="activePlayer === 'webRTC'"
        ref="webRtcRef"
        :video-url="currentPlayUrl"
        :has-audio="hasAudio"
        :show-button="showButton"
        @play-status-change="$emit('playStatusChange', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import JessibucaPlayer from './JessibucaPlayer.vue'
import RtcPlayer from './RtcPlayer.vue'

const props = withDefaults(
  defineProps<{
    streamInfo?: any
    title?: string
    hasAudio?: boolean
    showButton?: boolean
    showTab?: boolean
    defaultPlayer?: 'jessibuca' | 'webRTC'
  }>(),
  {
    streamInfo: null,
    title: '',
    hasAudio: false,
    showButton: true,
    showTab: false,
    defaultPlayer: 'jessibuca',
  }
)

defineEmits(['playStatusChange'])

const activePlayer = ref<'jessibuca' | 'webRTC'>(props.defaultPlayer)
const jessibucaRef = ref<any>(null)
const webRtcRef = ref<any>(null)

const playerList = [
  { key: 'jessibuca', label: 'Jessibuca (Wasm)' },
  { key: 'webRTC', label: 'WebRTC (Low Latency)' },
]

const currentPlayUrl = computed(() => {
  if (!props.streamInfo) return ''
  const info = props.streamInfo
  const isHttps = location.protocol === 'https:'

  if (activePlayer.value === 'webRTC') {
    return isHttps ? (info.rtcs || info.rtc) : (info.rtc || info.rtcs)
  } else {
    // Jessibuca prefers ws-flv / wss-flv / flv
    if (isHttps) {
      return info.wss_flv || info.https_flv || info.ws_flv || info.flv
    }
    return info.ws_flv || info.flv || info.wss_flv
  }
})

function switchPlayer(key: 'jessibuca' | 'webRTC') {
  activePlayer.value = key
}

function stop() {
  if (jessibucaRef.value) jessibucaRef.value.stop()
  if (webRtcRef.value) webRtcRef.value.stop()
}

defineExpose({
  stop,
  switchPlayer,
})
</script>
