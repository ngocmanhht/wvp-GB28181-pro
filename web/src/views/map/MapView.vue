<template>
  <div class="relative w-full h-[calc(100vh-112px)] rounded-xl overflow-hidden border border-slate-800 shadow-2xl select-none">
    <!-- Map Container -->
    <div ref="mapContainerRef" class="w-full h-full bg-slate-950"></div>

    <!-- Map Floating Stats Overlay -->
    <div class="absolute top-4 left-4 z-10 bg-slate-900/90 border border-slate-800 backdrop-blur-md rounded-xl p-3 shadow-xl flex items-center gap-4 text-xs">
      <div class="flex items-center gap-2">
        <span class="w-2.5 h-2.5 rounded-full bg-blue-500"></span>
        <span class="font-semibold text-white">GIS Electronic Map</span>
      </div>
      <div class="h-4 w-[1px] bg-slate-800"></div>
      <span class="text-slate-400">Total Cameras: <strong class="text-blue-400 font-mono">{{ markerCount }}</strong></span>
      <el-button size="small" type="primary" plain @click="loadCameraMarkers" class="!rounded-lg">
        <el-icon class="mr-1"><Refresh /></el-icon> Refresh Pins
      </el-button>
    </div>

    <!-- Camera Preview Modal on Marker Click -->
    <el-dialog
      v-model="previewModalVisible"
      :title="`Map Camera: ${activeCamera?.name || activeCamera?.channelId}`"
      width="720px"
      destroy-on-close
    >
      <div class="w-full h-[400px]">
        <PlayerTabs v-if="streamInfo" :stream-info="streamInfo" :show-tab="true" />
      </div>
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
import { fromLonLat } from 'ol/proj'
import { Style, Circle, Fill, Stroke } from 'ol/style'
import { deviceService, playService } from '@/api'
import PlayerTabs from '@/components/player/PlayerTabs.vue'
import { Refresh } from '@element-plus/icons-vue'
import 'ol/ol.css'

const mapContainerRef = ref<HTMLDivElement | null>(null)
let olMap: Map | null = null
let vectorSource = new VectorSource()

const markerCount = ref(0)
const previewModalVisible = ref(false)
const activeCamera = ref<any>(null)
const streamInfo = ref<any>(null)

function initMap() {
  if (!mapContainerRef.value) return

  const vectorLayer = new VectorLayer({
    source: vectorSource,
    style: new Style({
      image: new Circle({
        radius: 8,
        fill: new Fill({ color: '#38bdf8' }),
        stroke: new Stroke({ color: '#ffffff', width: 2 }),
      }),
    }),
  })

  olMap = new Map({
    target: mapContainerRef.value,
    layers: [
      new TileLayer({
        source: new OSM(),
      }),
      vectorLayer,
    ],
    view: new View({
      center: fromLonLat([105.8342, 21.0278]), // Hanoi coordinates default (or customize via config)
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
      let count = 0

      for (const ch of channels) {
        if (ch.longitude && ch.latitude) {
          const feat = new Feature({
            geometry: new Point(fromLonLat([parseFloat(ch.longitude), parseFloat(ch.latitude)])),
            channelData: ch,
          })
          vectorSource.addFeature(feat)
          count++
        }
      }
      markerCount.value = count
    }
  } catch (e) {
    console.warn('Failed to load map markers:', e)
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
