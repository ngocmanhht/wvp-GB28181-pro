<template>
  <div class="w-screen h-screen bg-black flex flex-col items-center justify-center relative select-none">
    <!-- Top Floating Header -->
    <div class="absolute top-3 left-4 z-20 flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-900/80 backdrop-blur-md border border-slate-700/60 text-xs text-white">
      <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
      <span class="font-bold">WVP-PRO Live Stream</span>
      <span class="text-slate-400 font-mono">{{ streamKey }}</span>
    </div>

    <!-- Video Area -->
    <div class="w-full h-full relative">
      <PlayerTabs v-if="streamInfo" :stream-info="streamInfo" :show-tab="true" />
      <div v-else class="w-full h-full flex flex-col items-center justify-center text-slate-500 gap-2">
        <el-icon class="text-5xl animate-spin text-blue-500"><Loading /></el-icon>
        <span class="text-xs font-mono">Initializing video stream...</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { playService } from '@/api'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import { Loading } from '@element-plus/icons-vue'

const route = useRoute()
const streamKey = ref('')
const streamInfo = ref<any>(null)

async function initStream() {
  const deviceId = route.query.deviceId as string
  const channelId = route.query.channelId as string
  const app = (route.query.app as string) || 'live'
  const stream = route.query.stream as string

  if (deviceId && channelId) {
    streamKey.value = `${deviceId}/${channelId}`
    try {
      const res: any = await playService.startPlay(deviceId, channelId)
      if (res && res.data) {
        streamInfo.value = res.data
      }
    } catch (e) {
      console.error('Failed to load device stream:', e)
    }
  } else if (stream) {
    streamKey.value = `${app}/${stream}`
    streamInfo.value = {
      app,
      stream,
      ws_flv: `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/${app}/${stream}.live.flv`,
      flv: `${location.origin}/${app}/${stream}.live.flv`,
      rtc: `${location.origin}/index/api/webrtc?app=${app}&stream=${stream}&type=play`,
    }
  }
}

onMounted(() => {
  initStream()
})
</script>
