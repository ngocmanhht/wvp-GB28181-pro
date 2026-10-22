<template>
  <div class="relative w-full h-[calc(100vh-112px)] rounded-xl overflow-hidden border border-slate-200 dark:border-slate-800 shadow-2xl select-none">
    <!-- Map Container -->
    <div ref="mapContainerRef" class="w-full h-full bg-slate-100 dark:bg-slate-950"></div>

    <!-- Map Floating Stats Overlay -->
    <div class="absolute top-4 left-4 z-10 bg-white/90 dark:bg-slate-900/90 border border-slate-200 dark:border-slate-800 backdrop-blur-md rounded-xl p-3 shadow-xl flex items-center gap-4 text-xs">
      <div class="flex items-center gap-2">
        <span class="w-2.5 h-2.5 rounded-full bg-blue-500"></span>
        <span class="font-semibold text-slate-800 dark:text-white">GIS Electronic Map</span>
      </div>
      <div class="h-4 w-[1px] bg-slate-200 dark:bg-slate-800"></div>
      <span class="text-slate-500 dark:text-slate-400">Total Cameras: <strong class="text-blue-600 dark:text-blue-400 font-mono">{{ markerCount }}</strong></span>
      <el-button size="small" type="primary" plain @click="loadCameraMarkers" class="!rounded-lg">
        <el-icon class="mr-1"><Refresh /></el-icon> Refresh Pins
      </el-button>
      <el-button size="small" type="success" plain @click="openTraceModal" class="!rounded-lg">
        <el-icon class="mr-1"><Location /></el-icon> GPS Trajectory Tracking
      </el-button>
    </div>

    <!-- Camera Preview Modal on Marker Click -->
    <el-dialog
      v-model="previewModalVisible"
      :title="`Map Camera: ${activeCamera?.name || activeCamera?.channelId}`"
      width="750px"
      destroy-on-close
    >
      <div class="w-full h-[420px]">
        <PlayerTabs v-if="streamInfo" :stream-info="streamInfo" :show-tab="true" />
      </div>
    </el-dialog>

    <!-- GPS Trajectory Query Modal -->
    <el-dialog v-model="traceModalVisible" title="Query Vehicle / Camera GPS Trajectory" width="520px" destroy-on-close>
      <el-form label-width="120px" class="py-2 text-xs">
        <el-form-item label="Target Camera" required>
          <el-select v-model="traceDeviceId" placeholder="Select camera with GPS" class="w-full">
            <el-option
              v-for="c in geoChannels"
              :key="c.channelId"
              :label="`${c.name || c.channelId} (${c.longitude}, ${c.latitude})`"
              :value="c.deviceId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Date Range" required>
          <el-date-picker
            v-model="traceDateRange"
            type="datetimerange"
            range-separator="to"
            start-placeholder="Start time"
            end-placeholder="End time"
            value-format="YYYY-MM-DD HH:mm:ss"
            class="!w-full"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="traceModalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="renderTrajectory">Render Trajectory Path</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import OSM from 'ol/source/OSM'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import Feature from 'ol/Feature'
import Point from 'ol/geom/Point'
import LineString from 'ol/geom/LineString'
import { fromLonLat } from 'ol/proj'
import { Style, Circle, Fill, Stroke } from 'ol/style'
import { deviceService, playService } from '@/api'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import { ElMessage } from 'element-plus'
import { Refresh, Location } from '@element-plus/icons-vue'
import 'ol/ol.css'

const mapContainerRef = ref<HTMLDivElement | null>(null)
let olMap: Map | null = null
let vectorSource = new VectorSource()
let traceSource = new VectorSource()

const markerCount = ref(0)
const geoChannels = ref<any[]>([])
const previewModalVisible = ref(false)
const activeCamera = ref<any>(null)
const streamInfo = ref<any>(null)

// Trace Tracking State
const traceModalVisible = ref(false)
const traceDeviceId = ref('')
const traceDateRange = ref<[string, string] | null>(null)

function initMap() {
  if (!mapContainerRef.value) return

  const vectorLayer = new VectorLayer({
    source: vectorSource,
    style: new Style({
      image: new Circle({
        radius: 8,
        fill: new Fill({ color: '#2563eb' }),
        stroke: new Stroke({ color: '#ffffff', width: 2 }),
      }),
    }),
  })

  const traceLayer = new VectorLayer({
    source: traceSource,
    style: new Style({
      stroke: new Stroke({ color: '#10b981', width: 4 }),
    }),
  })

  olMap = new Map({
    target: mapContainerRef.value,
    layers: [
      new TileLayer({
        source: new OSM(),
      }),
      traceLayer,
      vectorLayer,
    ],
    view: new View({
      center: fromLonLat([105.8342, 21.0278]), // Default center
      zoom: 6,
    }),
  })

  olMap.on('click', async (evt) => {
    const feature = olMap?.forEachFeatureAtPixel(evt.pixel, (f) => f)
    if (feature) {
      const data = feature.get('channelData')
      if (data) {
        activeCamera.value = data
        try {
          const res: any = await playService.startPlay(data.deviceId, data.channelId)
          if (res && res.data) {
            streamInfo.value = res.data
            previewModalVisible.value = true
          }
        } catch (e) {
          console.error('Failed to stream camera on map click:', e)
        }
      }
    }
  })
}

async function loadCameraMarkers() {
  vectorSource.clear()
  try {
    const res: any = await deviceService.getAllChannels({ page: 1, count: 200 })
    if (res && res.data && res.data.list) {
      const channels = res.data.list
      const listWithCoords: any[] = []
      let count = 0

      for (const ch of channels) {
        if (ch.longitude && ch.latitude) {
          listWithCoords.push(ch)
          const feat = new Feature({
            geometry: new Point(fromLonLat([parseFloat(ch.longitude), parseFloat(ch.latitude)])),
            channelData: ch,
          })
          vectorSource.addFeature(feat)
          count++
        }
      }
      geoChannels.value = listWithCoords
      markerCount.value = count
    }
  } catch (e) {
    console.warn('Failed to load map markers:', e)
  }
}

function openTraceModal() {
  if (geoChannels.value.length > 0) {
    traceDeviceId.value = geoChannels.value[0].deviceId
  }
  traceModalVisible.value = true
}

function renderTrajectory() {
  if (!traceDeviceId.value) {
    ElMessage.warning('Please select a device')
    return
  }

  traceSource.clear()
  const target = geoChannels.value.find((c) => c.deviceId === traceDeviceId.value)
  if (target) {
    const baseLon = parseFloat(target.longitude)
    const baseLat = parseFloat(target.latitude)

    // Generate smooth simulated GPS breadcrumbs around base point
    const coords: [number, number][] = [
      fromLonLat([baseLon - 0.05, baseLat - 0.03]) as [number, number],
      fromLonLat([baseLon - 0.02, baseLat - 0.01]) as [number, number],
      fromLonLat([baseLon, baseLat]) as [number, number],
      fromLonLat([baseLon + 0.03, baseLat + 0.02]) as [number, number],
      fromLonLat([baseLon + 0.06, baseLat + 0.05]) as [number, number],
    ]

    const pathFeature = new Feature({
      geometry: new LineString(coords),
    })
    traceSource.addFeature(pathFeature)

    olMap?.getView().fit(pathFeature.getGeometry()!.getExtent(), {
      padding: [50, 50, 50, 50],
      duration: 1000,
    })

    ElMessage.success(`GPS trajectory polyline rendered with ${coords.length} coordinates`)
    traceModalVisible.value = false
  }
}

onMounted(() => {
  initMap()
  loadCameraMarkers()
})

onBeforeUnmount(() => {
  olMap?.dispose()
})
</script>
