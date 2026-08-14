<template>
  <div class="player-ptz-panel">
    <div class="player-section">
      <div class="player-wrapper" :style="{ height: playerHeight }">
        <playerTabs ref="playerTabs" :has-audio="hasAudio" :show-button="true" />
      </div>
    </div>
    <devicePtzPanel
      style="margin-top: 5vh"
      :device-id="deviceId"
      :channel-id="channelDeviceId"
      @drag-zoom-start="toggleDragZoom"
    />
  </div>
</template>

<script>
import playerTabs from '../../common/playerTabs.vue'
import devicePtzPanel from './devicePtzPanel.vue'

export default {
  name: 'PlayerPtzPanel',
  components: { playerTabs, devicePtzPanel },
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      hasAudio: false,
      playerHeight: '40vh',
      dragZoomDirection: ''
    }
  },
  mounted() {
    this.startPlay()
  },
  beforeDestroy() {
    this.stopPlay()
  },
  methods: {
    startPlay() {
      this.$store.dispatch('play/play', [this.deviceId, this.channelDeviceId])
        .then(data => {
          this.hasAudio = data.hasAudio
          this.$nextTick(() => {
            if (this.$refs.playerTabs) {
              this.$refs.playerTabs.setStreamInfo(data.transcodeStream || data)
            }
          })
        })
        .catch(e => {
          this.$message({ showClose: true, message: e || 'Play failed', type: 'error' })
        })
    },
    stopPlay() {
      this.$store.dispatch('play/stop', { deviceId: this.deviceId, channelId: this.channelDeviceId })
        .catch(() => {})
    },
    toggleDragZoom(direction) {
      this.dragZoomDirection = direction
      this.$refs.playerTabs.startDragZoom((params) => {
        params.deviceId = this.deviceId
        params.channelId = this.channelDeviceId
        const action = this.dragZoomDirection === 'in' ? 'frontEnd/dragZoomIn' : 'frontEnd/dragZoomOut'
        const successMsg = this.dragZoomDirection === 'in' ? 'Pull frame to enlarge successfully' : 'The frame was successfully reduced'
        const failMsg = this.dragZoomDirection === 'in' ? 'Failed to enlarge the frame' : 'Failed to shrink the frame'
        this.$store.dispatch(action, params).then(() => {
          this.$message({ showClose: true, message: successMsg, type: 'success' })
        }).catch(() => {
          this.$message({ showClose: true, message: failMsg, type: 'error' })
        })
        this.dragZoomDirection = ''
      })
    },
  }
}
</script>

<style scoped>
.player-ptz-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.player-section {
  flex: 0.8;
}
.player-wrapper {
  position: relative;
  width: 100%;
}
</style>
