<template>
  <div>
    <el-dialog
      title="Voice intercom"
      top="10vh"
      width="61.5vw"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      @close="close()"
    >
      <div style="display: flex; gap: 16px;">
        <div style="flex: 1; min-width: 0;">
          <div v-if="!showPlayer" class="player-placeholder">
            <el-button
              type="primary"
              icon="el-icon-video-play"
              :loading="previewLoading"
              @click="startPreview"
            >Turn on preview</el-button>
          </div>
          <playerTabs
            v-if="showPlayer"
            ref="playerTabs"
            style="min-height: 60vh;"
            :has-audio="hasAudio"
            :show-button="true"
          />
        </div>

        <div class="broadcast-panel">
          <div style="text-align: center;">
            <video id="audioTalkVideo" controls autoplay style="width: 0; height: 0">
              Your browser is too old which doesn't support HTML5 video.
            </video>
            <el-radio-group v-model="talkMode" size="big" @change="onModeChange">
              <el-radio-button :label="true">shout</el-radio-button>
              <el-radio-button :label="false">intercom</el-radio-button>
            </el-radio-group>
            <p style="color: #909399; font-size: 14px; margin-top: 4px;">
              {{ talkMode ? 'One-way calling, only sending voice to the device' : 'Two-way voice interaction, device sound can be heard' }}
            </p>
          </div>
          <div style="text-align: center;">
            <el-button
              :type="getTalkButtonType()"
              :disabled="talkStatus === -2"
              circle
              icon="el-icon-microphone"
              style="font-size: 32px; padding: 24px;"
              @click="talkButtonClick()"
            />
            <p style="margin-top: 16px; color: #606266;">
              <span v-if="talkStatus === -2">Releasing resources</span>
              <span v-if="talkStatus === -1">Click to start{{ talkMode ? 'shout' : 'intercom' }}</span>
              <span v-if="talkStatus === 0">Waiting to be connected...</span>
              <span v-if="talkStatus === 1 && talkMode">shouting</span>
              <span v-if="talkStatus === 1 && !talkMode && !playConnected">Waiting to be connected...</span>
              <span v-if="talkStatus === 1 && !talkMode && playConnected">Talking</span>
            </p>
            <p v-if="talkStatus === 1 && !talkMode && talkAudioFailed" style="margin-top: 8px;">
              <el-button
                type="warning"
                size="mini"
                icon="el-icon-refresh"
                @click="retryTalkAudio"
              >Retry audio</el-button>
            </p>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import playerTabs from '../../common/playerTabs.vue'

export default {
  name: 'AudioTalk',
  components: { playerTabs },
  data() {
    return {
      showDialog: false,
      showPlayer: false,
      previewLoading: false,
      deviceId: null,
      channelId: null,
      hasAudio: false,
      streamInfo: null,
      talkMode: true,
      talkStatus: -1,
      broadcastRtc: null,
      talkAudioRtc: null,
      talkAudioRetryTimer: null,
      talkAudioFailed: false,
      talkAudioPlayStream: null,
      playConnected: false
    }
  },
  created() {
    this.talkStatus = -1
  },
  methods: {
    openDialog(deviceId, channelId) {
      if (this.showDialog) return
      this.deviceId = deviceId
      this.channelId = channelId
      this.talkMode = false
      this.showPlayer = false
      this.streamInfo = null
      this.showDialog = true
    },
    onModeChange() {
      if (this.talkStatus > -1) {
        this.stopTalk()
      }
    },
    startPreview() {
      this.previewLoading = true
      this.$store.dispatch('play/play', [this.deviceId, this.channelId])
        .then(data => {
          this.streamInfo = data
          this.hasAudio = data.hasAudio
          this.showPlayer = true
          this.$nextTick(() => {
            if (this.$refs.playerTabs) {
              this.$refs.playerTabs.setStreamInfo(data.transcodeStream || data)
            }
          })
        })
        .catch(e => {
          this.$message({ showClose: true, message: e, type: 'error' })
        })
        .finally(() => {
          this.previewLoading = false
        })
    },
    getTalkButtonType() {
      if (this.talkStatus === -2) return 'primary'
      if (this.talkStatus === -1) return 'primary'
      if (this.talkStatus === 0) return 'warning'
      if (this.talkStatus === 1) {
        if (!this.talkMode && !this.playConnected) return 'warning'
        return 'danger'
      }
    },
    async talkButtonClick() {
      if (this.talkStatus === -1) {
        await this.startTalk()
      } else if (this.talkStatus === 1) {
        this.stopTalk()
      }
    },
    async startTalk() {
      this.talkStatus = 0
      try {
        const data = await this.$store.dispatch('play/broadcastStart', [this.deviceId, this.channelId, this.talkMode])
        const si = data.streamInfo
        const url = document.location.protocol.includes('https') ? si.rtcs : si.rtc
        this.startWebrtcPush(url)

        const playStreamInfo = data?.playStreamInfo
        if (!this.talkMode && playStreamInfo) {
          this.talkAudioPlayStream = playStreamInfo
          this.startTalkAudioPlay(playStreamInfo)
          this.muteVideoPlayer()
        }
      } catch (e) {
        this.$message({ showClose: true, message: e, type: 'error' })
        this.talkStatus = -1
      }
    },
    startWebrtcPush(url) {
      this.$store.dispatch('user/getUserInfo')
        .then((data) => {
          if (data === null) { this.talkStatus = -1; return }
          const pushKey = data.pushKey
          url += '&sign=' + pushKey

          if (this.broadcastRtc) {
            this.broadcastRtc.close()
          }
          this.broadcastRtc = new ZLMRTCClient.Endpoint({
            debug: true,
            zlmsdpUrl: url,
            simulecast: false,
            useCamera: false,
            audioEnable: true,
            videoEnable: false,
            recvOnly: false
          })
          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_NOT_SUPPORT, () => { this.talkStatus = -1 })
          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, () => { this.talkStatus = -1 })
          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, () => { this.talkStatus = -1 })
          this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_ON_CONNECTION_STATE_CHANGE, (e) => {
            if (e === 'connecting') this.talkStatus = 0
            else if (e === 'connected') this.talkStatus = 1
            else if (e === 'disconnected') this.talkStatus = -1
          })
          this.broadcastRtc.on(ZLMRTCClient.Events.CAPTURE_STREAM_FAILED, () => { this.talkStatus = -1 })
        })
        .catch(() => { this.talkStatus = -1 })
    },
    muteVideoPlayer() {
      const player = this.$refs.playerTabs
      if (!player) return
      if (player.mute) {
        player.mute()
      }
    },
    unmuteVideoPlayer() {
      const player = this.$refs.playerTabs
      if (!player) return
      if (player.cancelMute) {
        player.cancelMute()
      }
    },
    startTalkAudioPlay(playStreamInfo) {
      if (this.talkAudioRtc) {
        this.talkAudioRtc.close()
      }
      if (this.talkAudioRetryTimer) {
        clearTimeout(this.talkAudioRetryTimer)
      }

      const url = location.protocol === 'https:' ? playStreamInfo.rtcs : playStreamInfo.rtc
      if (!url) {
        console.warn('[AudioTalk] No device audio playback address available')
        return
      }
      this.talkAudioRetryTimer = setTimeout(() => {
        this.pollMediaInfoAndPlay(playStreamInfo)
      }, 800)
    },
    async pollMediaInfoAndPlay(playStreamInfo) {
      try {
        const data = await this.$store.dispatch('server/getMediaInfo', {
          app: playStreamInfo.app,
          stream: playStreamInfo.stream,
          mediaServerId: playStreamInfo.mediaServerId
        })
        if (data) {
          const url = location.protocol === 'https:' ? playStreamInfo.rtcs : playStreamInfo.rtc
          this.startTalkAudioByRtc(url)
        } else {
          throw new Error('no data')
        }
      } catch (e) {
        if (this.talkStatus === 1 || this.talkStatus === 0) {
          this.talkAudioRetryTimer = setTimeout(() => {
            this.pollMediaInfoAndPlay(playStreamInfo)
          }, 800)
        }
      }
    },
    startTalkAudioByRtc(url) {
      this.talkAudioFailed = false
      this.talkAudioRtc = new ZLMRTCClient.Endpoint({
        debug: false,
        element: document.getElementById('audioTalkVideo'),
        zlmsdpUrl: url,
        simulecast: false,
        useCamera: false,
        audioEnable: true,
        videoEnable: false,
        recvOnly: true,
        usedatachannel: false
      })

      this.talkAudioRtc.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, (e) => {
        console.warn('[AudioTalk] Playback stream offer failed:', e?.code, e?.msg)
        if (e && e.code == -400 && e.msg == 'Stream does not exist') {
          this.talkAudioRetryTimer = setTimeout(() => {
            this.startTalkAudioByRtc(url)
          }, 1000)
        }
      })

      this.talkAudioRtc.on(ZLMRTCClient.Events.WEBRTC_ON_REMOTE_STREAMS, () => {
        console.warn('[AudioTalk] Device audio stream arrives')
        this.playConnected = true
      })

      this.talkAudioRtc.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, () => {
        console.error('[AudioTalk] Audio playback ICE negotiation failed')
      })

      this.talkAudioRtc.on(ZLMRTCClient.Events.WEBRTC_ON_CONNECTION_STATE_CHANGE, (s) => {
        console.warn('[AudioTalk] Audio playback connection status:', s)
        if (s === 'connected') {
          this.playConnected = true
        } else if (s === 'disconnected' || s === 'failed' || s === 'closed') {
          this.playConnected = false
          this.talkAudioFailed = true
          if (this.talkStatus === 1) {
            this.talkAudioRetryTimer = setTimeout(() => {
              this.startTalkAudioByRtc(url)
            }, 2000)
          }
        }
      })
    },
    async stopTalk() {
      this.talkStatus = -2
      if (this.broadcastRtc) {
        this.broadcastRtc.close()
        this.broadcastRtc = null
      }
      if (this.talkAudioRtc) {
        this.talkAudioRtc.close()
        this.talkAudioRtc = null
      }
      if (this.talkAudioRetryTimer) {
        clearTimeout(this.talkAudioRetryTimer)
        this.talkAudioRetryTimer = null
      }
      this.talkAudioFailed = false
      this.talkAudioPlayStream = null
      this.playConnected = false
      this.unmuteVideoPlayer()
      try {
        await this.$store.dispatch('play/broadcastStop', [this.deviceId, this.channelId])
      } catch (e) {
        console.warn('Failed to stop intercom', e)
      }
      this.talkStatus = -1
    },
    retryTalkAudio() {
      if (this.talkAudioPlayStream) {
        this.startTalkAudioPlay(this.talkAudioPlayStream)
      }
    },
    close() {
      if (this.showPlayer && this.$refs.playerTabs) {
        this.$refs.playerTabs.stop()
      }
      this.stopTalk()
      this.streamInfo = null
      this.showPlayer = false
      this.showDialog = false
    }
  }
}
</script>

<style scoped>
.player-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 16 / 9;
  background: #1a1a1a;
}
.broadcast-panel {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 10px;
  border-left: 1px solid #ebeef5;
}
.broadcast-panel > div:first-child {
  flex-shrink: 0;
}
.broadcast-panel > div:last-child {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
</style>
