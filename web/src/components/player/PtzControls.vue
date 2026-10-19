<template>
  <div class="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col gap-4 text-slate-200 select-none shadow-xl">
    <div class="flex items-center justify-between border-b border-slate-800 pb-2">
      <div class="flex items-center gap-2">
        <span class="w-2.5 h-2.5 rounded-full bg-blue-500"></span>
        <span class="font-semibold text-sm">PTZ Controls</span>
      </div>
      <span class="text-xs text-slate-400 font-mono">Speed: {{ speed }}</span>
    </div>

    <!-- Directional Pad and Zoom Controls -->
    <div class="flex items-center justify-center gap-6 py-2">
      <!-- D-Pad Circle -->
      <div class="relative w-40 h-40 rounded-full bg-slate-950 border-2 border-slate-800 shadow-inner flex items-center justify-center">
        <!-- Up -->
        <button
          @mousedown.prevent="sendMove('up')"
          @mouseup.prevent="sendStop"
          class="absolute top-2 left-1/2 -translate-x-1/2 w-9 h-9 rounded-full bg-slate-800 hover:bg-blue-600 hover:text-white flex items-center justify-center text-sm transition active:scale-95 shadow"
          title="Tilt Up"
        >
          ▲
        </button>

        <!-- Down -->
        <button
          @mousedown.prevent="sendMove('down')"
          @mouseup.prevent="sendStop"
          class="absolute bottom-2 left-1/2 -translate-x-1/2 w-9 h-9 rounded-full bg-slate-800 hover:bg-blue-600 hover:text-white flex items-center justify-center text-sm transition active:scale-95 shadow"
          title="Tilt Down"
        >
          ▼
        </button>

        <!-- Left -->
        <button
          @mousedown.prevent="sendMove('left')"
          @mouseup.prevent="sendStop"
          class="absolute left-2 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-slate-800 hover:bg-blue-600 hover:text-white flex items-center justify-center text-sm transition active:scale-95 shadow"
          title="Pan Left"
        >
          ◀
        </button>

        <!-- Right -->
        <button
          @mousedown.prevent="sendMove('right')"
          @mouseup.prevent="sendStop"
          class="absolute right-2 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-slate-800 hover:bg-blue-600 hover:text-white flex items-center justify-center text-sm transition active:scale-95 shadow"
          title="Pan Right"
        >
          ▶
        </button>

        <!-- Diagonal Buttons (Up-Left, Up-Right, Down-Left, Down-Right) -->
        <button
          @mousedown.prevent="sendMove('upleft')"
          @mouseup.prevent="sendStop"
          class="absolute top-5 left-5 w-7 h-7 rounded-full bg-slate-800/80 hover:bg-blue-600 text-xs flex items-center justify-center transition active:scale-95"
          title="Up-Left"
        >
          ◤
        </button>
        <button
          @mousedown.prevent="sendMove('upright')"
          @mouseup.prevent="sendStop"
          class="absolute top-5 right-5 w-7 h-7 rounded-full bg-slate-800/80 hover:bg-blue-600 text-xs flex items-center justify-center transition active:scale-95"
          title="Up-Right"
        >
          ◥
        </button>
        <button
          @mousedown.prevent="sendMove('downleft')"
          @mouseup.prevent="sendStop"
          class="absolute bottom-5 left-5 w-7 h-7 rounded-full bg-slate-800/80 hover:bg-blue-600 text-xs flex items-center justify-center transition active:scale-95"
          title="Down-Left"
        >
          ◣
        </button>
        <button
          @mousedown.prevent="sendMove('downright')"
          @mouseup.prevent="sendStop"
          class="absolute bottom-5 right-5 w-7 h-7 rounded-full bg-slate-800/80 hover:bg-blue-600 text-xs flex items-center justify-center transition active:scale-95"
          title="Down-Right"
        >
          ◢
        </button>

        <!-- Center Stop Button -->
        <button
          @click="sendStop"
          class="w-10 h-10 rounded-full bg-slate-800 hover:bg-rose-600 text-white flex items-center justify-center text-xs font-bold transition shadow"
          title="Stop Motion"
        >
          STOP
        </button>
      </div>

      <!-- Zoom & Optics Controls -->
      <div class="flex flex-col gap-2">
        <div class="flex items-center gap-1.5">
          <button
            @mousedown.prevent="sendMove('zoomin')"
            @mouseup.prevent="sendStop"
            class="px-3 py-1.5 bg-slate-800 hover:bg-blue-600 rounded-lg text-xs font-medium flex items-center gap-1 transition"
          >
            <el-icon><ZoomIn /></el-icon> Zoom +
          </button>
          <button
            @mousedown.prevent="sendMove('zoomout')"
            @mouseup.prevent="sendStop"
            class="px-3 py-1.5 bg-slate-800 hover:bg-blue-600 rounded-lg text-xs font-medium flex items-center gap-1 transition"
          >
            <el-icon><ZoomOut /></el-icon> Zoom -
          </button>
        </div>

        <div class="flex items-center gap-1.5">
          <button
            @mousedown.prevent="sendMove('near')"
            @mouseup.prevent="sendStop"
            class="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 rounded-lg text-xs font-medium flex items-center gap-1 transition"
          >
            Focus +
          </button>
          <button
            @mousedown.prevent="sendMove('far')"
            @mouseup.prevent="sendStop"
            class="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 rounded-lg text-xs font-medium flex items-center gap-1 transition"
          >
            Focus -
          </button>
        </div>

        <div class="flex items-center gap-1.5">
          <button
            @mousedown.prevent="sendMove('in')"
            @mouseup.prevent="sendStop"
            class="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 rounded-lg text-xs font-medium flex items-center gap-1 transition"
          >
            Iris +
          </button>
          <button
            @mousedown.prevent="sendMove('out')"
            @mouseup.prevent="sendStop"
            class="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 rounded-lg text-xs font-medium flex items-center gap-1 transition"
          >
            Iris -
          </button>
        </div>
      </div>
    </div>

    <!-- Speed Slider -->
    <div class="px-2">
      <div class="flex items-center justify-between text-xs text-slate-400 mb-1">
        <span>Movement Speed</span>
        <span>{{ speed }}%</span>
      </div>
      <el-slider v-model="speed" :min="1" :max="100" size="small" />
    </div>

    <!-- Preset Manager -->
    <div class="border-t border-slate-800 pt-3">
      <div class="text-xs font-medium text-slate-300 mb-2">Preset Positions</div>
      <div class="flex items-center gap-2">
        <el-input-number v-model="presetId" :min="1" :max="255" size="small" class="!w-24" />
        <el-button type="primary" size="small" @click="handleCallPreset">Call</el-button>
        <el-button type="success" size="small" @click="handleAddPreset">Save</el-button>
        <el-button type="danger" size="small" @click="handleDeletePreset">Delete</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { playService } from '@/api'
import { ElMessage } from 'element-plus'
import { ZoomIn, ZoomOut } from '@element-plus/icons-vue'

const props = defineProps<{
  deviceId?: string
  channelId?: string
}>()

const speed = ref(50)
const presetId = ref(1)

async function sendMove(command: string) {
  if (!props.deviceId || !props.channelId) {
    ElMessage.warning('No active camera selected')
    return
  }
  try {
    await playService.ptzControl({
      deviceId: props.deviceId,
      channelId: props.channelId,
      command,
      horizonSpeed: speed.value,
      verticalSpeed: speed.value,
      zoomSpeed: speed.value,
    })
  } catch (e: any) {
    console.error('PTZ error:', e)
  }
}

async function sendStop() {
  if (!props.deviceId || !props.channelId) return
  try {
    await playService.ptzControl({
      deviceId: props.deviceId,
      channelId: props.channelId,
      command: 'stop',
    })
  } catch (e: any) {
    console.error('PTZ stop error:', e)
  }
}

async function handleCallPreset() {
  if (!props.deviceId || !props.channelId) return
  await playService.callPreset(props.deviceId, props.channelId, presetId.value)
  ElMessage.success(`Called Preset #${presetId.value}`)
}

async function handleAddPreset() {
  if (!props.deviceId || !props.channelId) return
  await playService.addPreset(props.deviceId, props.channelId, presetId.value)
  ElMessage.success(`Saved Preset #${presetId.value}`)
}

async function handleDeletePreset() {
  if (!props.deviceId || !props.channelId) return
  await playService.deletePreset(props.deviceId, props.channelId, presetId.value)
  ElMessage.success(`Deleted Preset #${presetId.value}`)
}
</script>
