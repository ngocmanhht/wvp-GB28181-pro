<template>
  <div id="StreamProxyEdit" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header content="Edit pull agent information" @back="close" />
      </div>
    </div>
    <el-tabs tab-position="top" style="padding-top: 1rem">
      <el-tab-pane label="Pull streaming agent information" style="padding-top: 1rem; height: calc(-218px + 100vh);
    overflow: auto;">
        <el-form ref="streamProxy" :rules="rules" :model="streamProxy" label-width="140px" style="width: 50%; margin: 0 auto">
          <el-form-item label="Type" prop="type">
            <el-select
              v-model="streamProxy.type"
              style="width: 100%"
              placeholder="Please select agent type"
            >
              <el-option key="Default" label="Default" value="default" />
              <el-option key="FFmpeg" label="FFmpeg" value="ffmpeg" />
            </el-select>
          </el-form-item>
          <el-form-item label="Application name" prop="app">
            <el-input v-model="streamProxy.app" clearable />
          </el-form-item>
          <el-form-item label="flowID" prop="stream">
            <el-input v-model="streamProxy.stream" clearable />
          </el-form-item>
          <el-form-item label="Pull address" prop="url">
            <el-input v-model="streamProxy.srcUrl" clearable />
          </el-form-item>
          <el-form-item label="timeout(seconds)" prop="timeoutMs">
            <el-input v-model="streamProxy.timeout" clearable />
          </el-form-item>
          <el-form-item label="Node selection" prop="rtpType">
            <el-select
              v-model="streamProxy.relatesMediaServerId"
              style="width: 100%"
              placeholder="Please select the streaming node"
              @change="mediaServerIdChange"
            >
              <el-option key="auto" label="automatic selection" value="" />
              <el-option
                v-for="item in mediaServerList"
                :key="item.id"
                :label="item.id"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="streamProxy.type=='ffmpeg'" label="FFmpegcommand template" prop="ffmpegCmdKey">
            <el-select
              v-model="streamProxy.ffmpegCmdKey"
              style="width: 100%"
              placeholder="Please select FFmpeg command template"
            >
              <el-option
                v-for="item in Object.keys(ffmpegCmdList)"
                :key="item"
                :label="ffmpegCmdList[item]"
                :value="item"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="Pull method(RTSP)" prop="rtspType">
            <el-select
              v-model="streamProxy.rtspType"
              style="width: 100%"
              placeholder="Please select the streaming method"
            >
              <el-option label="TCP" value="0" />
              <el-option label="UDP" value="1" />
              <el-option label="multicast" value="2" />
            </el-select>
          </el-form-item>

          <el-form-item label="no one watching" prop="noneReader">
            <el-radio-group v-model="streamProxy.noneReader">
              <el-radio :label="0">No processing</el-radio>
              <el-radio :label="1">deactivate</el-radio>
              <el-radio :label="2">Remove</el-radio>
            </el-radio-group>

          </el-form-item>
          <el-form-item label="Other options">
            <div style="float: left;">
              <el-checkbox v-model="streamProxy.enable" label="enable" />
              <el-checkbox v-model="streamProxy.enableAudio" label="Turn on audio" />
              <el-checkbox v-model="streamProxy.enableMp4" label="Record" />
            </div>

          </el-form-item>
          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" :loading="saveLoading" @click="onSubmit">save</el-button>
              <el-button @click="close">Cancel</el-button>
            </div>

          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane v-if="streamProxy.id" label="National standard channel configuration">
        <CommonChannelEdit ref="commonChannelEdit" :showCancel="true" :data-form="streamProxy" @cancel="close" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CommonChannelEdit from '../common/CommonChannelEdit'

export default {
  name: 'ChannelEdit',
  components: {
    CommonChannelEdit
  },
  props: ['value', 'closeEdit'],
  data() {
    return {
      saveLoading: false,
      streamProxy: this.value,
      mediaServerList: {},
      ffmpegCmdList: {},
      rules: {
        name: [{ required: true, message: 'Please enter name', trigger: 'blur' }],
        app: [{ required: true, message: 'Please enter application name', trigger: 'blur' }],
        stream: [{ required: true, message: 'Please enter the streamID', trigger: 'blur' }],
        srcUrl: [{ required: true, message: 'Please enter the stream to be proxied', trigger: 'blur' }],
        timeout: [{ required: true, message: 'Please enter the timeout for successful FFmpeg streaming', trigger: 'blur' }],
        ffmpegCmdKey: [{ required: false, message: 'Please enter the FFmpeg command parameter template (optional）', trigger: 'blur' }]
      }
    }
  },
  watch: {
    value(newValue, oldValue) {
      this.streamProxy = newValue
    }
  },
  created() {
    console.log(this.streamProxy)
    this.$store.dispatch('server/getOnlineMediaServerList')
      .then((data) => {
        this.mediaServerList = data
      })
  },
  methods: {
    onSubmit: function() {
      this.saveLoading = true
      this.noneReaderHandler()
      if (this.streamProxy.id) {
        this.$store.dispatch('streamProxy/update', this.streamProxy)
          .then((data) => {
            this.saveLoading = false
            this.$message.success({
              showClose: true,
              message: 'Saved successfully'
            })
            this.streamProxy = data
          })
          .catch((error) => {
            this.$message.error({
              showClose: true,
              message: error
            })
            this.saveLoading = false
          }).finally(() => {
            this.saveLoading = false
          })
      } else {
        this.$store.dispatch('streamProxy/add', this.streamProxy)
          .then((data) => {
            this.saveLoading = false
            this.$message.success({
              showClose: true,
              message: 'Saved successfully'
            })
            this.streamProxy = data
          })
          .catch((error) => {
            this.$message.error({
              showClose: true,
              message: error
            })
            this.saveLoading = false
          })
          .finally(() => {
            this.saveLoading = false
          })
      }
    },
    close: function() {
      this.closeEdit()
    },
    mediaServerIdChange: function() {
      if (this.streamProxy.relatesMediaServerId !== 'auto') {
        this.$store.dispatch('streamProxy/queryFfmpegCmdList', this.streamProxy.relatesMediaServerId)
          .then((data) => {
            this.ffmpegCmdList = data
            this.streamProxy.ffmpegCmdKey = Object.keys(data)[0]
          })
      }
    },
    noneReaderHandler: function() {
      console.log(this.streamProxy)
      this.streamProxy.enableDisableNoneReader = this.streamProxy.noneReader && this.streamProxy.noneReader === 1
    }
  }
}
</script>
