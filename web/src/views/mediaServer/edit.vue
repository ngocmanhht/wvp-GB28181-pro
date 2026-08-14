<template>
  <div id="mediaServerEdit" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title="media node"
      :width="dialogWidth"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="formStep" style="margin-top: 1rem; margin-right: 20px;">
        <el-form v-if="currentStep === 1" ref="mediaServerForm" :rules="rules" :model="mediaServerForm" label-width="140px">
          <el-form-item label="IP" prop="ip">
            <el-input v-model="mediaServerForm.ip" placeholder="media servicesIP" clearable :disabled="mediaServerForm.defaultServer" />
          </el-form-item>
          <el-form-item label="HTTPport" prop="httpPort">
            <el-input v-model="mediaServerForm.httpPort" placeholder="Media service HTTP port" clearable :disabled="mediaServerForm.defaultServer" />
          </el-form-item>
          <el-form-item label="SECRET" prop="secret">
            <el-input v-model="mediaServerForm.secret" placeholder="media servicesSECRET" clearable :disabled="mediaServerForm.defaultServer" />
          </el-form-item>
          <el-form-item label="Type" prop="type">
            <el-select v-model="mediaServerForm.type" style="float: left; width: 100%">
              <el-option key="zlm" label="ZLMediaKit" value="zlm" />
              <el-option key="abl" label="ABLMediaServer" value="abl" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <div style="float: right;">
              <el-button v-if="currentStep === 1 && serverCheck === 1" type="primary" @click="next">Next step</el-button>
              <el-button @click="close">Cancel</el-button>
              <el-button type="primary" @click="checkServer">test</el-button>
              <i v-if="serverCheck === 1" class="el-icon-success" style="color: #3caf36" />
              <i v-if="serverCheck === -1" class="el-icon-error" style="color: #c80000" />
            </div>
          </el-form-item>
        </el-form>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form v-if="currentStep === 2 || currentStep === 3" ref="mediaServerForm1" :rules="rules" :model="mediaServerForm" label-width="140px">
              <el-form-item label="IP" prop="ip">
                <el-input v-if="currentStep === 2" v-model="mediaServerForm.ip" :disabled="mediaServerForm.defaultServer" />
                <el-input v-if="currentStep === 3" v-model="mediaServerForm.ip" :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="HTTPport" prop="httpPort">
                <el-input v-if="currentStep === 2" v-model="mediaServerForm.httpPort" :disabled="mediaServerForm.defaultServer" />
                <el-input v-if="currentStep === 3" v-model="mediaServerForm.httpPort" :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="HOOK IP" prop="ip">
                <el-input v-model="mediaServerForm.hookIp" placeholder="media servicesHOOK_IP" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="SDP IP" prop="ip">
                <el-input v-model="mediaServerForm.sdpIp" placeholder="media servicesSDP_IP" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="flowIP" prop="ip">
                <el-input v-model="mediaServerForm.streamIp" placeholder="media service flowIP" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="HTTPS PORT" prop="httpSSlPort">
                <el-input v-model="mediaServerForm.httpSSlPort" placeholder="media servicesHTTPS_PORT" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="RTSP PORT" prop="rtspPort">
                <el-input v-model="mediaServerForm.rtspPort" placeholder="media servicesRTSP_PORT" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="RTSPS PORT" prop="rtspSSLPort">
                <el-input v-model="mediaServerForm.rtspSSLPort" placeholder="media servicesRTSPS_PORT" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>

            </el-form>
          </el-col>
          <el-col :span="12">
            <el-form v-if="currentStep === 2 || currentStep === 3" ref="mediaServerForm2" :rules="rules" :model="mediaServerForm" label-width="180px">
              <el-form-item label="RTMP PORT" prop="rtmpPort">
                <el-input v-model="mediaServerForm.rtmpPort" placeholder="media servicesRTMP_PORT" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="RTMPS PORT" prop="rtmpSSlPort">
                <el-input v-model="mediaServerForm.rtmpSSlPort" placeholder="media servicesRTMPS_PORT" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="SECRET" prop="secret">
                <el-input v-if="currentStep === 2" v-model="mediaServerForm.secret" :disabled="mediaServerForm.defaultServer" />
                <el-input v-if="currentStep === 3" v-model="mediaServerForm.secret" :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="Automatically configure media services">
                <el-switch v-model="mediaServerForm.autoConfig" :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item label="Traffic collection port mode">
                <el-switch v-model="mediaServerForm.rtpEnable" active-text="multi-port" inactive-text="single port" :disabled="mediaServerForm.defaultServer" @change="portRangeChange" />
              </el-form-item>

              <el-form-item v-if="!mediaServerForm.rtpEnable" label="Flow collection port" prop="rtpProxyPort">
                <el-input v-model.number="mediaServerForm.rtpProxyPort" clearable :disabled="mediaServerForm.defaultServer" />
              </el-form-item>
              <el-form-item v-if="mediaServerForm.rtpEnable" label="Flow collection port">
                <el-input v-model="rtpPortRange1" placeholder="start" clearable style="width: 100px" prop="rtpPortRange1" :disabled="mediaServerForm.defaultServer" @change="portRangeChange" />
                -
                <el-input v-model="rtpPortRange2" placeholder="terminate" clearable style="width: 100px" prop="rtpPortRange2" :disabled="mediaServerForm.defaultServer" @change="portRangeChange" />
              </el-form-item>
              <el-form-item v-if="mediaServerForm.sendRtpEnable" label="Streaming port">
                <el-input v-model="sendRtpPortRange1" placeholder="start" clearable style="width: 100px" prop="rtpPortRange1" :disabled="mediaServerForm.defaultServer" @change="portRangeChange" />
                -
                <el-input v-model="sendRtpPortRange2" placeholder="terminate" clearable style="width: 100px" prop="rtpPortRange2" :disabled="mediaServerForm.defaultServer" @change="portRangeChange" />
              </el-form-item>
              <el-form-item>
                <div style="float: right;">
                  <el-button v-if="!mediaServerForm.defaultServer" type="primary" @click="onSubmit">Submit</el-button>
                  <el-button v-if="!mediaServerForm.defaultServer" @click="close">Cancel</el-button>
                  <el-button v-if="mediaServerForm.defaultServer" @click="close">Close</el-button>
                </div>
              </el-form-item>
            </el-form>
          </el-col>
        </el-row>

      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'MediaServerEdit',
  directives: { elDragDialog },
  props: {},
  data() {
    const isValidIp = (rule, value, callback) => { // Verify whether the IP complies with the rules
      var reg = /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/
      console.log(this.mediaServerForm.ip)
      if (!reg.test(this.mediaServerForm.ip)) {
        return callback(new Error('Please enter a valid IP address'))
      } else {
        callback()
      }
      return true
    }
    const isValidPort = (rule, value, callback) => { // Verify whether the IP complies with the rules
      var reg = /^(([0-9]|[1-9]\d{1,3}|[1-5]\d{4}|6[0-5]{2}[0-3][0-5]))$/
      if (!reg.test(this.mediaServerForm.httpPort)) {
        return callback(new Error('Please enter a valid port number'))
      } else {
        callback()
      }
      return true
    }
    return {
      dialogWidth: 0,
      defaultWidth: 1000,
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      dialogLoading: false,

      currentStep: 1,
      platformList: [],
      serverCheck: 0,
      recordServerCheck: 0,
      mediaServerForm: {
        id: '',
        ip: '',
        autoConfig: true,
        hookIp: '',
        sdpIp: '',
        streamIp: '',
        secret: '',
        httpPort: '',
        httpSSlPort: '',
        recordAssistPort: '',
        rtmpPort: '',
        rtmpSSlPort: '',
        rtpEnable: false,
        rtpPortRange: '',
        sendRtpPortRange: '',
        rtpProxyPort: '',
        rtspPort: '',
        rtspSSLPort: '',
        type: 'zlm'
      },
      rtpPortRange1: 30000,
      rtpPortRange2: 30500,

      sendRtpPortRange1: 50000,
      sendRtpPortRange2: 60000,

      rules: {
        ip: [{ required: true, validator: isValidIp, message: 'Please enter a valid IP address', trigger: 'blur' }],
        httpPort: [{ required: true, validator: isValidPort, message: 'Please enter a valid port number', trigger: 'blur' }],
        httpSSlPort: [{ required: true, validator: isValidPort, message: 'Please enter a valid port number', trigger: 'blur' }],
        recordAssistPort: [{ required: true, validator: isValidPort, message: 'Please enter a valid port number', trigger: 'blur' }],
        rtmpPort: [{ required: true, validator: isValidPort, message: 'Please enter a valid port number', trigger: 'blur' }],
        rtmpSSlPort: [{ required: true, validator: isValidPort, message: 'Please enter a valid port number', trigger: 'blur' }],
        rtpPortRange1: [{ required: true, validator: isValidPort, message: 'Please enter a valid port number', trigger: 'blur' }],
        rtpPortRange2: [{ required: true, validator: isValidPort, message: 'Please enter a valid port number', trigger: 'blur' }],
        rtpProxyPort: [{ required: true, validator: isValidPort, message: 'Please enter a valid port number', trigger: 'blur' }],
        rtspPort: [{ required: true, validator: isValidPort, message: 'Please enter a valid port number', trigger: 'blur' }],
        rtspSSLPort: [{ required: true, validator: isValidPort, message: 'Please enter a valid port number', trigger: 'blur' }],
        secret: [{ required: true, message: 'Please entersecret', trigger: 'blur' }],
        timeout_ms: [{ required: true, message: 'Please enter the timeout for successful FFmpeg streaming', trigger: 'blur' }],
        ffmpeg_cmd_key: [{ required: false, message: 'Please enter the FFmpeg command parameter template (optional）', trigger: 'blur' }]
      }
    }
  },
  computed: {},
  created() {
    this.setDialogWidth()
  },
  methods: {
    setDialogWidth() {
      const val = document.body.clientWidth
      if (val < this.defaultWidth) {
        this.dialogWidth = '100%'
      } else {
        this.dialogWidth = this.defaultWidth + 'px'
      }
    },
    openDialog: function(param, callback) {
      this.showDialog = true
      this.listChangeCallback = callback
      if (param != null) {
        this.mediaServerForm = param
        this.currentStep = 3
        if (param.rtpPortRange) {
          const rtpPortRange = this.mediaServerForm.rtpPortRange.split(',')
          const sendRtpPortRange = this.mediaServerForm.sendRtpPortRange.split(',')
          if (rtpPortRange.length > 0) {
            this.rtpPortRange1 = rtpPortRange[0]
            this.rtpPortRange2 = rtpPortRange[1]
          }
          if (sendRtpPortRange.length > 0) {
            this.sendRtpPortRange1 = sendRtpPortRange[0]
            this.sendRtpPortRange2 = sendRtpPortRange[1]
          }
        }
      }
    },
    checkServer: function() {
      this.serverCheck = 0
      this.$store.dispatch('server/checkMediaServer', this.mediaServerForm)
        .then(data => {
          if (parseInt(this.mediaServerForm.httpPort) !== parseInt(data.httpPort)) {
            this.$message({
              showClose: true,
              message: 'If you are using docker to deploy your media service, please pay attention to the port mapping。',
              type: 'warning',
              duration: 0
            })
          }
          const httpPort = this.mediaServerForm.httpPort
          this.mediaServerForm = data
          this.mediaServerForm.httpPort = httpPort
          this.mediaServerForm.autoConfig = true
          this.rtpPortRange1 = 30000
          this.rtpPortRange2 = 30500
          this.sendRtpPortRange1 = 50000
          this.sendRtpPortRange2 = 60000
          this.serverCheck = 1
        })
        .catch(() => {
          this.$message({
            showClose: true,
            message: 'The test failed, please check whether the media service address and port are correct.！',
            type: 'warning'
          })
        })
    },
    next: function() {
      this.currentStep = 2
      this.defaultWidth = 900
      this.setDialogWidth()
    },
    onSubmit: function() {
      this.dialogLoading = true
      this.$store.dispatch('server/saveMediaServer', this.mediaServerForm)
        .then(data => {
          this.$message({
            showClose: true,
            message: 'Saved successfully',
            type: 'success'
          })
          if (this.listChangeCallback) this.listChangeCallback()
          this.close()
        })
    },
    close: function() {
      this.showDialog = false
      this.dialogLoading = false
      this.mediaServerForm = {
        id: '',
        ip: '',
        autoConfig: true,
        hookIp: '',
        sdpIp: '',
        streamIp: '',
        secret: '',
        httpPort: '',
        httpSSlPort: '',
        recordAssistPort: '',
        rtmpPort: '',
        rtmpSSlPort: '',
        rtpEnable: false,
        rtpPortRange: '',
        sendRtpPortRange: '',
        rtpProxyPort: '',
        rtspPort: '',
        rtspSSLPort: ''
      }
      this.rtpPortRange1 = 30500
      this.rtpPortRange2 = 30500
      this.sendRtpPortRange1 = 50000
      this.sendRtpPortRange2 = 60000
      this.listChangeCallback = null
      this.currentStep = 1
    },
    portRangeChange: function() {
      if (this.mediaServerForm.rtpEnable) {
        this.mediaServerForm.rtpPortRange = this.rtpPortRange1 + ',' + this.rtpPortRange2
        this.mediaServerForm.sendRtpPortRange = this.sendRtpPortRange1 + ',' + this.sendRtpPortRange2
      }
    }
  }
}
</script>
