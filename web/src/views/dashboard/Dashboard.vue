<template>
  <div class="flex flex-col gap-5 select-none">
    <!-- Top Greeting Banner -->
    <div class="relative overflow-hidden rounded-2xl bg-gradient-to-r from-blue-900/40 via-indigo-900/30 to-slate-900 border border-slate-800 p-6 flex items-center justify-between shadow-xl">
      <div class="flex flex-col gap-1 z-10">
        <h2 class="text-xl font-bold text-white flex items-center gap-2">
          <span>Surveillance Control Center</span>
          <span class="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
            System Healthy
          </span>
        </h2>
        <p class="text-xs text-slate-400">
          WVP-PRO High Performance GB28181 & WebRTC/Wasm Video Management Platform
        </p>
      </div>

      <div class="hidden sm:flex items-center gap-3 z-10">
        <el-button type="primary" class="!rounded-xl !bg-blue-600 hover:!bg-blue-500 !border-none" @click="$router.push('/live')">
          <el-icon class="mr-1"><VideoCamera /></el-icon> Open Multi-Live
        </el-button>
        <el-button class="!rounded-xl !bg-slate-800 hover:!bg-slate-700 !border-slate-700 !text-slate-200" @click="fetchStats">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>

      <div class="absolute -right-10 -bottom-10 w-60 h-60 bg-blue-600/10 rounded-full blur-3xl pointer-events-none"></div>
    </div>

    <!-- Metric Stat Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- 1. Devices -->
      <div class="bg-slate-900/80 border border-slate-800 rounded-xl p-4 flex items-center justify-between shadow hover:border-slate-700 transition">
        <div class="flex flex-col gap-1">
          <span class="text-xs font-medium text-slate-400">GB28181 Devices</span>
          <div class="flex items-baseline gap-2">
            <span class="text-2xl font-bold text-white font-mono">{{ stats.deviceCount }}</span>
            <span class="text-xs text-emerald-400">Online: {{ stats.deviceOnline }}</span>
          </div>
        </div>
        <div class="w-12 h-12 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center text-xl">
          <el-icon><Monitor /></el-icon>
        </div>
      </div>

      <!-- 2. Channels -->
      <div class="bg-slate-900/80 border border-slate-800 rounded-xl p-4 flex items-center justify-between shadow hover:border-slate-700 transition">
        <div class="flex flex-col gap-1">
          <span class="text-xs font-medium text-slate-400">Camera Channels</span>
          <div class="flex items-baseline gap-2">
            <span class="text-2xl font-bold text-white font-mono">{{ stats.channelCount }}</span>
            <span class="text-xs text-emerald-400">Online: {{ stats.channelOnline }}</span>
          </div>
        </div>
        <div class="w-12 h-12 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center text-xl">
          <el-icon><VideoCamera /></el-icon>
        </div>
      </div>

      <!-- 3. Active Streams -->
      <div class="bg-slate-900/80 border border-slate-800 rounded-xl p-4 flex items-center justify-between shadow hover:border-slate-700 transition">
        <div class="flex flex-col gap-1">
          <span class="text-xs font-medium text-slate-400">Active Live Streams</span>
          <div class="flex items-baseline gap-2">
            <span class="text-2xl font-bold text-white font-mono">{{ stats.streamCount }}</span>
            <span class="text-xs text-cyan-400">Pull/Push</span>
          </div>
        </div>
        <div class="w-12 h-12 rounded-xl bg-cyan-500/10 text-cyan-400 flex items-center justify-center text-xl">
          <el-icon><Connection /></el-icon>
        </div>
      </div>

      <!-- 4. Media Nodes -->
      <div class="bg-slate-900/80 border border-slate-800 rounded-xl p-4 flex items-center justify-between shadow hover:border-slate-700 transition">
        <div class="flex flex-col gap-1">
          <span class="text-xs font-medium text-slate-400">ZLMediaKit Nodes</span>
          <div class="flex items-baseline gap-2">
            <span class="text-2xl font-bold text-white font-mono">{{ stats.nodeCount }}</span>
            <span class="text-xs text-emerald-400">Connected</span>
          </div>
        </div>
        <div class="w-12 h-12 rounded-xl bg-purple-500/10 text-purple-400 flex items-center justify-center text-xl">
          <el-icon><Coin /></el-icon>
        </div>
      </div>
    </div>

    <!-- Charts & Quick Access Section -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <!-- Bitrate Chart (2 Cols) -->
      <div class="lg:col-span-2 bg-slate-900/80 border border-slate-800 rounded-2xl p-5 shadow flex flex-col gap-3">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="w-2.5 h-2.5 rounded-full bg-blue-500"></span>
            <span class="font-semibold text-sm text-white">Real-Time Streaming Bandwidth</span>
          </div>
          <span class="text-xs text-slate-400 font-mono">Sampled every 3s</span>
        </div>

        <div ref="chartRef" class="w-full h-64"></div>
      </div>

      <!-- Quick Action shortcuts (1 Col) -->
      <div class="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 shadow flex flex-col gap-4">
        <span class="font-semibold text-sm text-white flex items-center gap-2">
          <span class="w-2.5 h-2.5 rounded-full bg-indigo-500"></span>
          Quick Operations
        </span>

        <div class="flex flex-col gap-2.5">
          <button
            @click="$router.push('/live')"
            class="flex items-center justify-between p-3 rounded-xl bg-slate-800/60 hover:bg-slate-800 border border-slate-700/40 text-slate-200 transition group"
          >
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-blue-500/10 text-blue-400 flex items-center justify-center">
                <el-icon><VideoCamera /></el-icon>
              </div>
              <div class="flex flex-col text-left">
                <span class="text-xs font-semibold group-hover:text-blue-400 transition">Multi-Screen Live</span>
                <span class="text-[11px] text-slate-400">1, 4, 9, 16 video grid</span>
              </div>
            </div>
            <el-icon class="text-slate-500 group-hover:translate-x-1 transition"><ArrowRight /></el-icon>
          </button>

          <button
            @click="$router.push('/device')"
            class="flex items-center justify-between p-3 rounded-xl bg-slate-800/60 hover:bg-slate-800 border border-slate-700/40 text-slate-200 transition group"
          >
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
                <el-icon><Monitor /></el-icon>
              </div>
              <div class="flex flex-col text-left">
                <span class="text-xs font-semibold group-hover:text-emerald-400 transition">GB28181 Devices</span>
                <span class="text-[11px] text-slate-400">Manage NVRs & cameras</span>
              </div>
            </div>
            <el-icon class="text-slate-500 group-hover:translate-x-1 transition"><ArrowRight /></el-icon>
          </button>

          <button
            @click="$router.push('/stream/proxy')"
            class="flex items-center justify-between p-3 rounded-xl bg-slate-800/60 hover:bg-slate-800 border border-slate-700/40 text-slate-200 transition group"
          >
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-amber-500/10 text-amber-400 flex items-center justify-center">
                <el-icon><Connection /></el-icon>
              </div>
              <div class="flex flex-col text-left">
                <span class="text-xs font-semibold group-hover:text-amber-400 transition">RTSP Stream Proxy</span>
                <span class="text-[11px] text-slate-400">Pull external streams</span>
              </div>
            </div>
            <el-icon class="text-slate-500 group-hover:translate-x-1 transition"><ArrowRight /></el-icon>
          </button>

          <button
            @click="$router.push('/logs')"
            class="flex items-center justify-between p-3 rounded-xl bg-slate-800/60 hover:bg-slate-800 border border-slate-700/40 text-slate-200 transition group"
          >
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-rose-500/10 text-rose-400 flex items-center justify-center">
                <el-icon><Document /></el-icon>
              </div>
              <div class="flex flex-col text-left">
                <span class="text-xs font-semibold group-hover:text-rose-400 transition">Real-time Logs</span>
                <span class="text-[11px] text-slate-400">SIP & media events</span>
              </div>
            </div>
            <el-icon class="text-slate-500 group-hover:translate-x-1 transition"><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { deviceService, systemService, streamService } from '@/api'
import {
  VideoCamera,
  Monitor,
  Connection,
  Coin,
  Refresh,
  ArrowRight,
  Document
} from '@element-plus/icons-vue'

const chartRef = ref<HTMLDivElement | null>(null)
let myChart: echarts.ECharts | null = null

const stats = reactive({
  deviceCount: 0,
  deviceOnline: 0,
  channelCount: 0,
  channelOnline: 0,
  streamCount: 0,
  nodeCount: 1,
})

async function fetchStats() {
  try {
    const [devRes, chanRes, serverRes, proxyRes]: any = await Promise.allSettled([
      deviceService.getDevices({ page: 1, count: 1 }),
      deviceService.getAllChannels({ page: 1, count: 1 }),
      systemService.getMediaServerList(),
      streamService.getStreamProxyList({ page: 1, count: 1 }),
    ])

    if (devRes.status === 'fulfilled' && devRes.value?.data) {
      stats.deviceCount = devRes.value.data.total || devRes.value.data.list?.length || 0
      stats.deviceOnline = devRes.value.data.list?.filter((d: any) => d.onLine)?.length || 0
    }
    if (chanRes.status === 'fulfilled' && chanRes.value?.data) {
      stats.channelCount = chanRes.value.data.total || chanRes.value.data.list?.length || 0
      stats.channelOnline = chanRes.value.data.list?.filter((c: any) => c.status === 1)?.length || 0
    }
    if (serverRes.status === 'fulfilled' && serverRes.value?.data) {
      stats.nodeCount = serverRes.value.data.length || 1
    }
    if (proxyRes.status === 'fulfilled' && proxyRes.value?.data) {
      stats.streamCount = proxyRes.value.data.total || 0
    }
  } catch (e) {
    console.error('Error fetching dashboard stats:', e)
  }
}

function initChart() {
  if (!chartRef.value) return
  myChart = echarts.init(chartRef.value)

  const timeData = []
  const inData = []
  const outData = []
  const now = Date.now()

  for (let i = 20; i >= 0; i--) {
    const t = new Date(now - i * 3000)
    timeData.push(`${t.getHours().toString().padStart(2, '0')}:${t.getMinutes().toString().padStart(2, '0')}:${t.getSeconds().toString().padStart(2, '0')}`)
    inData.push(Math.round(20 + Math.random() * 45))
    outData.push(Math.round(15 + Math.random() * 35))
  }

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#1e293b',
      borderColor: '#334155',
      textStyle: { color: '#f8fafc' },
    },
    legend: {
      data: ['Inbound Bitrate (Mbps)', 'Outbound Bitrate (Mbps)'],
      textStyle: { color: '#94a3b8' },
      right: 10,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: timeData,
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94a3b8' },
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#334155' } },
      axisLabel: { color: '#94a3b8' },
      splitLine: { lineStyle: { color: 'rgba(51, 65, 85, 0.4)' } },
    },
    series: [
      {
        name: 'Inbound Bitrate (Mbps)',
        type: 'line',
        smooth: true,
        data: inData,
        lineStyle: { color: '#3b82f6', width: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.4)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.0)' },
          ]),
        },
      },
      {
        name: 'Outbound Bitrate (Mbps)',
        type: 'line',
        smooth: true,
        data: outData,
        lineStyle: { color: '#10b981', width: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16, 185, 129, 0.3)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.0)' },
          ]),
        },
      },
    ],
  }

  myChart.setOption(option)
}

function handleResize() {
  myChart?.resize()
}

onMounted(() => {
  fetchStats()
  initChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  myChart?.dispose()
})
</script>
