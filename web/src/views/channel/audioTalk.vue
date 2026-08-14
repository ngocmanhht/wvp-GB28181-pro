<template>
  <div>
    <el-dialog
      title="Voice intercom"
      top="10vh"
      width="65vw"
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
              <el-radio-button :label="false">shout</el-radio-button>
              <el-radio-button :label="true">intercom</el-radio-button>
            </el-radio-group>
            <p style="color: #909399; font-size: 14px; margin-top: 4px;">
              {{ talkMode ? 'Two-way voice interaction, device sound can be heard' : 'One-way calling, only sending voice to the device' }}
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
              <span v-if="talkStatus === -1">Click to start{{ talkMode ? 'intercom' : 'shout' }}</span>
              <span v-if="talkStatus === 0">Waiting to be connected...</span>
              <span v-if="talkStatus === 1 && !talkMode">shouting</span>
              <span v-if="talkStatus === 1 && talkMode && !playConnected">Waiting to be connected...</span>
              <span v-if="talkStatus === 1 && talkMode && playConnected">Talking</span>
            </p>
            <p v-if="talkStatus === 1 && talkMode && talkAudioFailed" style="margin-top: 8px;">
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
import playerTabs from '../common/playerTabs.vue'

export default {
  name: 'ChAudioTalk',
  components: { playerTabs },
  data() {
    return {
      showDialog: false,
      showPlayer: false,
      previewLoading: false,
      channelId: null,
      hasAudio: false,
      streamInfo: null,
      talkMode: false,
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
    openDialog(channelId) {
      if (this.showDialog) return
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
      this.$store.dispatch('commonChanel/playChannel', this.channelId)
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
        if (this.talkMode && !this.playConnected) return 'warning'
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
      try {
        await this.checkMicrophoneAvailability()
      } catch (e) {
        this.$message({ showClose: true, message: this.getMicrophoneErrorMessage(e), type: 'error' })
        return
      }

      this.talkStatus = 0

      try {
        const storeName = 'commonChanel'
        const actionName = this.talkMode ? 'talkStart' : 'broadcastStart'
        const data = await this.$store.dispatch(storeName + '/' + actionName, this.channelId)

        const pushStream = data?.pushStream
        const playStream = data?.playStream

        if (this.talkMode && playStream) {
          this.talkAudioPlayStream = playStream
          this.startTalkAudioPlay(playStream)
          this.muteVideoPlayer()
        }

        this.startWebrtcPush(pushStream)
      } catch (e) {
        this.$message({ showClose: true, message: e, type: 'error' })
        this.talkStatus = -1
      }
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
    getMicrophoneErrorMessage(error) {
      if (!error || !error.name) return 'Local microphone detection failed, please check the browser audio collection permissions'
      if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError' || error.name === 'SecurityError') {
        return 'Browser microphone permission is not granted and voice intercom cannot be initiated.'
      }
      if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
        return 'No available microphone is detected and voice intercom cannot be initiated.'
      }
      if (error.name === 'NotReadableError' || error.name === 'TrackStartError' || error.name === 'AbortError') {
        return 'The local microphone is occupied or temporarily unavailable. Please check and try again.'
      }
      if (error.name === 'OverconstrainedError' || error.name === 'ConstraintNotSatisfiedError') {
        return 'The current microphone does not meet the collection conditions and cannot initiate voice intercom.'
      }
      return 'Local microphone detection failed: ' + (error.message || error.name)
    },
    async checkMicrophoneAvailability() {
      if (!window.isSecureContext && location.hostname !== 'localhost' && location.hostname !== '127.0.0.1') {
        throw new Error('The current page is not a secure context and the browser cannot collect microphone audio.')
      }
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        throw new Error('The current browser does not support microphone collection')
      }
      let stream = null
      try {
        stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false })
        const audioTracks = stream.getAudioTracks()
        if (!audioTracks.length) throw new Error('No valid microphone track detected')
        if (audioTracks.every(track => track.readyState === 'ended')) {
          throw new Error('Microphone is disconnected or unavailable')
        }
      } finally {
        if (stream) stream.getTracks().forEach(t => t.stop())
      }
    },
    startWebrtcPush(pushStream) {
      if (!pushStream) return
      let url = location.protocol === 'https:' ? pushStream.rtcs : (pushStream.rtc || pushStream.rtcs)
      if (!url) {
        console.warn('[ChAudioTalk] RTC push address not found')
        return
      }

      this.$store.dispatch('user/getUserInfo').then(user => {
        if (user && user.pushKey) {
          url += '&sign=' + user.pushKey
        } else {
          console.warn('[ChAudioTalk] The pushKey is not obtained, and push authentication may fail.')
        }

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

        this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_NOT_SUPPORT, () => {
          this.$message({ showClose: true, message: 'Does not support WebRTC and cannot perform voice intercom', type: 'error' })
          this.talkStatus = -1
        })
        this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, () => {
          this.$message({ showClose: true, message: 'ICENegotiation error', type: 'error' })
          this.talkStatus = -1
        })
        this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, () => {
          this.$message({ showClose: true, message: 'offer/answerExchange failed', type: 'error' })
          this.talkStatus = -1
        })
        this.broadcastRtc.on(ZLMRTCClient.Events.WEBRTC_ON_CONNECTION_STATE_CHANGE, (e) => {
          if (e === 'connecting') {
            this.talkStatus = 0
          } else if (e === 'connected') {
            this.talkStatus = 1
          } else if (e === 'disconnected') {
            this.talkStatus = -1
          }
        })
      }).catch(e => {
        console.warn('[ChAudioTalk] Failed to obtain user pushKey', e)
        this.talkStatus = -1
      })
    },
    startTalkAudioPlay(playStream) {
      if (this.talkAudioRtc) {
        this.talkAudioRtc.close()
      }
      if (this.talkAudioRetryTimer) {
        clearTimeout(this.talkAudioRetryTimer)
      }

      const url = location.protocol === 'https:' ? playStream.rtcs : playStream.rtc
      if (!url) {
        console.warn('[ChAudioTalk] No device audio playback address available')
        return
      }
      this.talkAudioRetryTimer = setTimeout(() => {
        this.pollMediaInfoAndPlay(playStream)
      }, 800)
    },
    async pollMediaInfoAndPlay(playStream) {
      try {
        const data = await this.$store.dispatch('server/getMediaInfo', {
          app: playStream.app,
          stream: playStream.stream,
          mediaServerId: playStream.mediaServerId
        })
        if (data) {
          const url = location.protocol === 'https:' ? playStream.rtcs : playStream.rtc
          this.startTalkAudioByRtc(url)
        } else {
          throw new Error('no data')
        }
      } catch (e) {
        if (this.talkStatus === 1 || this.talkStatus === 0) {
          this.talkAudioRetryTimer = setTimeout(() => {
            this.pollMediaInfoAndPlay(playStream)
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
        console.warn('[ChAudioTalk] Playback stream offer failed:', e?.code, e?.msg)
        if (e && e.code == -400 && e.msg == 'Stream does not exist') {
          this.talkAudioRetryTimer = setTimeout(() => {
            this.startTalkAudioByRtc(url)
          }, 1000)
        }
      })

      this.talkAudioRtc.on(ZLMRTCClient.Events.WEBRTC_ON_REMOTE_STREAMS, () => {
        console.warn('[ChAudioTalk] Device audio stream arrives')
        this.playConnected = true
      })

      this.talkAudioRtc.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, () => {
        console.error('[ChAudioTalk] Audio playback ICE negotiation failed')
      })

      this.talkAudioRtc.on(ZLMRTCClient.Events.WEBRTC_ON_CONNECTION_STATE_CHANGE, (s) => {
        console.warn('[ChAudioTalk] Audio playback connection status:', s)
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

      const storeName = 'commonChanel'
      const actionName = this.talkMode ? 'talkStop' : 'broadcastStop'
      try {
        await this.$store.dispatch(storeName + '/' + actionName, this.channelId)
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
