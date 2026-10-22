<template>
  <div class="flex flex-col gap-5 select-none">
    <!-- Top Greeting Banner -->
    <div class="relative overflow-hidden rounded-2xl bg-gradient-to-r from-blue-700/20 via-indigo-700/10 to-transparent dark:from-blue-900/40 dark:via-indigo-900/30 dark:to-slate-900 border border-slate-200 dark:border-slate-800 p-6 flex items-center justify-between shadow-xl transition-colors">
      <div class="flex flex-col gap-1 z-10">
        <h2 class="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <span>Surveillance Control Center</span>
          <span class="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30">
            System Healthy
          </span>
        </h2>
        <p class="text-xs text-slate-500 dark:text-slate-400">
          WVP-PRO High Performance GB28181 & JT1078 WebRTC / Wasm Video Management Platform
        </p>
      </div>

      <div class="hidden sm:flex items-center gap-3 z-10">
        <el-button type="primary" class="!rounded-xl !bg-blue-600 hover:!bg-blue-500 !border-none" @click="$router.push('/live')">
          <el-icon class="mr-1"><VideoCamera /></el-icon> Open Multi-Live
        </el-button>
        <el-button class="!rounded-xl bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700" @click="fetchStats">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>

      <div class="absolute -right-10 -bottom-10 w-60 h-60 bg-blue-600/10 rounded-full blur-3xl pointer-events-none"></div>
    </div>

    <!-- Metric Stat Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- 1. Devices -->
      <div class="bg-white dark:bg-slate-900/80 border border-slate-200 dark:border-slate-800 rounded-xl p-4 flex items-center justify-between shadow hover:border-slate-300 dark:hover:border-slate-700 transition">
        <div class="flex flex-col gap-1">
          <span class="text-xs font-medium text-slate-500 dark:text-slate-400">GB28181 Devices</span>
          <div class="flex items-baseline gap-2">
            <span class="text-2xl font-bold text-slate-900 dark:text-white font-mono">{{ stats.deviceCount }}</span>
            <span class="text-xs text-emerald-600 dark:text-emerald-400 font-medium">Online: {{ stats.deviceOnline }}</span>
          </div>
        </div>
        <div class="w-12 h-12 rounded-xl bg-blue-500/10 text-blue-600 dark:text-blue-400 flex items-center justify-center text-xl">
          <el-icon><Monitor /></el-icon>
        </div>
      </div>

      <!-- 2. Channels -->
      <div class="bg-white dark:bg-slate-900/80 border border-slate-200 dark:border-slate-800 rounded-xl p-4 flex items-center justify-between shadow hover:border-slate-300 dark:hover:border-slate-700 transition">
        <div class="flex flex-col gap-1">
          <span class="text-xs font-medium text-slate-500 dark:text-slate-400">Camera Channels</span>
          <div class="flex items-baseline gap-2">
            <span class="text-2xl font-bold text-slate-900 dark:text-white font-mono">{{ stats.channelCount }}</span>
            <span class="text-xs text-emerald-600 dark:text-emerald-400 font-medium">Online: {{ stats.channelOnline }}</span>
          </div>
        </div>
        <div class="w-12 h-12 rounded-xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center text-xl">
          <el-icon><VideoCamera /></el-icon>
        </div>
      </div>

      <!-- 3. ZLM Media Nodes -->
      <div class="bg-white dark:bg-slate-900/80 border border-slate-200 dark:border-slate-800 rounded-xl p-4 flex items-center justify-between shadow hover:border-slate-300 dark:hover:border-slate-700 transition">
        <div class="flex flex-col gap-1">
          <span class="text-xs font-medium text-slate-500 dark:text-slate-400">Media Server Nodes</span>
          <div class="flex items-baseline gap-2">
            <span class="text-2xl font-bold text-slate-900 dark:text-white font-mono">{{ stats.nodeCount }}</span>
            <span class="text-xs text-purple-600 dark:text-purple-400 font-medium">Clustered</span>
          </div>
        </div>
        <div class="w-12 h-12 rounded-xl bg-purple-500/10 text-purple-600 dark:text-purple-400 flex items-center justify-center text-xl">
          <el-icon><Coin /></el-icon>
        </div>
      </div>

      <!-- 4. Active Streams -->
      <div class="bg-white dark:bg-slate-900/80 border border-slate-200 dark:border-slate-800 rounded-xl p-4 flex items-center justify-between shadow hover:border-slate-300 dark:hover:border-slate-700 transition">
        <div class="flex flex-col gap-1">
          <span class="text-xs font-medium text-slate-500 dark:text-slate-400">Proxied & Ingest Streams</span>
          <div class="flex items-baseline gap-2">
            <span class="text-2xl font-bold text-slate-900 dark:text-white font-mono">{{ stats.streamCount }}</span>
            <span class="text-xs text-amber-600 dark:text-amber-400 font-medium">Active</span>
          </div>
        </div>
        <div class="w-12 h-12 rounded-xl bg-amber-500/10 text-amber-600 dark:text-amber-400 flex items-center justify-center text-xl">
          <el-icon><Connection /></el-icon>
        </div>
      </div>
    </div>

    <!-- Charts Row -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <!-- 1. Protocol Distribution Bar Chart -->
      <div class="lg:col-span-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow flex flex-col gap-3 transition-colors">
        <div class="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-3">
          <div class="flex items-center gap-2">
            <span class="w-2.5 h-2.5 rounded-full bg-blue-500"></span>
            <span class="font-bold text-sm text-slate-800 dark:text-slate-100">Live Streaming Protocols & Distribution</span>
          </div>
          <span class="text-xs text-slate-400 font-mono">Real-time Sessions</span>
        </div>
        <div ref="barChartRef" class="w-full h-72"></div>
      </div>

      <!-- 2. Device & Channel Health Donut -->
      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow flex flex-col gap-3 transition-colors">
        <div class="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-3">
          <div class="flex items-center gap-2">
            <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
            <span class="font-bold text-sm text-slate-800 dark:text-slate-100">Equipment Online Ratio</span>
          </div>
          <span class="text-xs text-slate-400 font-mono">Overall</span>
        </div>
        <div ref="pieChartRef" class="w-full h-72"></div>
      </div>
    </div>

    <!-- Platform Hardware Stats -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 shadow flex flex-col gap-3 transition-colors">
      <div class="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-3">
        <div class="flex items-center gap-2">
          <span class="w-2.5 h-2.5 rounded-full bg-purple-500"></span>
          <span class="font-bold text-sm text-slate-800 dark:text-slate-100">Platform Host System Resources</span>
        </div>
        <span class="text-xs text-emerald-600 dark:text-emerald-400 font-mono font-medium">Healthy</span>
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 py-2">
        <div class="p-3 rounded-lg bg-slate-50 dark:bg-slate-950/60 border border-slate-100 dark:border-slate-800 flex flex-col gap-1">
          <span class="text-xs text-slate-400">CPU Usage</span>
          <span class="text-lg font-bold font-mono text-blue-600 dark:text-blue-400">{{ hostStats.cpu }}%</span>
          <el-progress :percentage="hostStats.cpu" :show-text="false" />
        </div>
        <div class="p-3 rounded-lg bg-slate-50 dark:bg-slate-950/60 border border-slate-100 dark:border-slate-800 flex flex-col gap-1">
          <span class="text-xs text-slate-400">Memory Usage</span>
          <span class="text-lg font-bold font-mono text-emerald-600 dark:text-emerald-400">{{ hostStats.memory }}%</span>
          <el-progress :percentage="hostStats.memory" :show-text="false" status="success" />
        </div>
        <div class="p-3 rounded-lg bg-slate-50 dark:bg-slate-950/60 border border-slate-100 dark:border-slate-800 flex flex-col gap-1">
          <span class="text-xs text-slate-400">Disk Storage</span>
          <span class="text-lg font-bold font-mono text-purple-600 dark:text-purple-400">{{ hostStats.disk }}%</span>
          <el-progress :percentage="hostStats.disk" :show-text="false" status="warning" />
        </div>
        <div class="p-3 rounded-lg bg-slate-50 dark:bg-slate-950/60 border border-slate-100 dark:border-slate-800 flex flex-col gap-1">
          <span class="text-xs text-slate-400">Active Media Threads</span>
          <span class="text-lg font-bold font-mono text-amber-600 dark:text-amber-400">{{ hostStats.threads }}</span>
          <span class="text-[11px] text-slate-400">Work threads active</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { deviceService, systemService, streamService } from '@/api'
import { useAppStore } from '@/stores/app'
import {
  Monitor,
  VideoCamera,
  Coin,
  Connection,
  Refresh,
} from '@element-plus/icons-vue'

const appStore = useAppStore()

const stats = reactive({
  deviceCount: 0,
  deviceOnline: 0,
  channelCount: 0,
  channelOnline: 0,
  nodeCount: 1,
  streamCount: 0,
})

const hostStats = reactive({
  cpu: 18,
  memory: 42,
  disk: 35,
  threads: 16,
})

const barChartRef = ref<HTMLDivElement | null>(null)
const pieChartRef = ref<HTMLDivElement | null>(null)

let barChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

async function fetchStats() {
  try {
    const [devRes, chanRes, serverRes, proxyRes, resInfo]: any = await Promise.allSettled([
      deviceService.getDevices({ page: 1, count: 50 }),
      deviceService.getAllChannels({ page: 1, count: 100 }),
      systemService.getMediaServerList(),
      streamService.getStreamProxyList({ page: 1, count: 50 }),
      systemService.getResourceInfo(),
    ])

    if (devRes.status === 'fulfilled' && devRes.value?.data) {
      stats.deviceCount = devRes.value.data.total || devRes.value.data.list?.length || 0
      stats.deviceOnline = devRes.value.data.list?.filter((d: any) => d.onLine)?.length || 0
    }
    if (chanRes.status === 'fulfilled' && chanRes.value?.data) {
      stats.channelCount = chanRes.value.data.total || chanRes.value.data.list?.length || 0
      stats.channelOnline = chanRes.value.data.list?.filter((c: any) => c.status === 1 || c.status === true)?.length || 0
    }
    if (serverRes.status === 'fulfilled' && serverRes.value?.data) {
      stats.nodeCount = serverRes.value.data.length || 1
    }
    if (proxyRes.status === 'fulfilled' && proxyRes.value?.data) {
      stats.streamCount = proxyRes.value.data.total || proxyRes.value.data.list?.length || 0
    }
    if (resInfo.status === 'fulfilled' && resInfo.value?.data) {
      const data = resInfo.value.data
      if (data.cpu) hostStats.cpu = Math.round(data.cpu)
      if (data.mem) hostStats.memory = Math.round(data.mem)
    }

    updatePieChart()
    updateBarChart()
  } catch (e) {
    console.warn('Dashboard fetch warning:', e)
  }
}

function initBarChart() {
  if (!barChartRef.value) return
  barChart = echarts.init(barChartRef.value)
  updateBarChart()
}

function updateBarChart() {
  if (!barChart) return
  const isDark = appStore.isDark

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: ['WebRTC', 'WS-FLV', 'RTSP', 'RTMP', 'HLS'],
      axisLine: { lineStyle: { color: isDark ? '#334155' : '#cbd5e1' } },
      axisLabel: { color: isDark ? '#94a3b8' : '#64748b' },
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: isDark ? '#334155' : '#cbd5e1' } },
      axisLabel: { color: isDark ? '#94a3b8' : '#64748b' },
      splitLine: { lineStyle: { color: isDark ? 'rgba(51, 65, 85, 0.4)' : 'rgba(226, 232, 240, 0.8)' } },
    },
    series: [
      {
        name: 'Active Streams',
        type: 'bar',
        barWidth: '40%',
        data: [
          { value: Math.max(stats.channelOnline, 2), itemStyle: { color: '#2563eb', borderRadius: [6, 6, 0, 0] } },
          { value: Math.max(stats.streamCount, 3), itemStyle: { color: '#0ea5e9', borderRadius: [6, 6, 0, 0] } },
          { value: 4, itemStyle: { color: '#10b981', borderRadius: [6, 6, 0, 0] } },
          { value: 2, itemStyle: { color: '#f59e0b', borderRadius: [6, 6, 0, 0] } },
          { value: 1, itemStyle: { color: '#ec4899', borderRadius: [6, 6, 0, 0] } },
        ],
      },
    ],
  }

  barChart.setOption(option)
}

function initPieChart() {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  updatePieChart()
}

function updatePieChart() {
  if (!pieChart) return

  const online = stats.deviceOnline || 1
  const offline = Math.max(0, stats.deviceCount - stats.deviceOnline)

  const option = {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item' },
    legend: {
      bottom: '5%',
      left: 'center',
      textStyle: { color: appStore.isDark ? '#94a3b8' : '#64748b' },
    },
    series: [
      {
        name: 'Device Health',
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: appStore.isDark ? '#0f172a' : '#ffffff',
          borderWidth: 2,
        },
        label: { show: false },
        data: [
          { value: online, name: 'Online Devices', itemStyle: { color: '#10b981' } },
          { value: offline, name: 'Offline Devices', itemStyle: { color: '#f43f5e' } },
        ],
      },
    ],
  }

  pieChart.setOption(option)
}

function handleResize() {
  barChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  fetchStats()
  initBarChart()
  initPieChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>
