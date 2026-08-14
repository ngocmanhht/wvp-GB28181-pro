<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="Audio and video properties"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared">
        <el-descriptions title="Audio" :column="2" v-if="attributeData" style="margin-bottom: 1rem;">
          <el-descriptions-item label="encoding">{{ audioEncoderEnum[attributeData.audioEncoder - 1] }}</el-descriptions-item>
          <el-descriptions-item label="Number of channels">{{ attributeData.audioChannels }}</el-descriptions-item>
          <el-descriptions-item label="Sampling rate">{{ audioSamplingRateEnum[attributeData.audioSamplingRate] }}</el-descriptions-item>
          <el-descriptions-item label="Number of sampling bits">{{ audioSamplingBitsEnum[attributeData.audioSamplingBits] }}</el-descriptions-item>
          <el-descriptions-item label="Frame length">{{ attributeData.audioFrameLength }}</el-descriptions-item>
          <el-descriptions-item label="audio output">{{ attributeData.audioOutputEnable === 0 ? 'Not supported':'support' }}</el-descriptions-item>
          <el-descriptions-item label="Maximum number of physical channels">{{ attributeData.audioChannelMax }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="video" :column="2" style="margin-bottom: 1rem;">
          <el-descriptions-item label="encoding method">{{ videoEncoderEnum[attributeData.videoEncoder] }}</el-descriptions-item>
          <el-descriptions-item label="Maximum number of physical channels">{{ attributeData.videoChannelMax }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showDialog: false,
      attributeData: null,
      audioEncoderEnum: [
        'G.721', 'G.722', 'G.723', 'G.728', 'G.729', 'G.711A', 'G.711U', 'G.726', 'G.729A', 'DVI4_3', 'DVI4_4', 'DVI4_8K'
        , 'DVI4_16K', 'LPC', 'S16BE_STEREO', 'S16BE_MONO', 'MPEGAUDIO', 'LPCM', 'AAC', 'WMA9STD', 'HEAAC', 'PCM_VOICE'
        , 'PCM_AUDIO', 'AACLC', 'MP3', 'ADPCMA', 'MP4AUDIO', 'AMR'
      ],
      audioSamplingRateEnum: [
        '8kHz', '22.05kHz', '44.1kHz', '48kHz'
      ],
      audioSamplingBitsEnum: [
        '8Bit', '16Bit', '32Bit'
      ],
      videoEncoderEnum: {
        98: 'H.264',
        99: 'H.265',
        100: 'AVS',
        101: 'SVAC'
      }
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(data) {
      this.showDialog = true
      this.attributeData = data
    },
    close: function() {
      this.showDialog = false
    }
  }
}
</script>
