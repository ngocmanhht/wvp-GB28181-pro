<template>
  <div class="record-control-form">
    <el-alert title="Issue recording control instructions to the current channel" type="info" :closable="false" show-icon style="margin-bottom: 16px" />
    <el-button type="primary" :loading="startLoading" @click="handleRecord('Record')">Start recording</el-button>
    <el-button type="danger" :loading="stopLoading" @click="handleRecord('StopRecord')" style="margin-left: 12px">Stop recording</el-button>
  </div>
</template>

<script>
export default {
  name: 'RecordControlConfig',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      startLoading: false,
      stopLoading: false
    }
  },
  methods: {
    handleRecord(recordCmdStr) {
      const loadingKey = recordCmdStr === 'Record' ? 'startLoading' : 'stopLoading'
      this[loadingKey] = true
      const msg = recordCmdStr === 'Record' ? 'Start recording' : 'Stop recording'
      this.$store.dispatch('device/deviceRecord', {
        deviceId: this.deviceId,
        channelId: this.channelDeviceId,
        recordCmdStr: recordCmdStr
      }).then(() => {
        this.$message({ showClose: true, message: msg + 'success', type: 'success' })
      }).catch((error) => {
        this.$message({ showClose: true, message: error.message || msg + 'failed', type: 'error' })
      }).finally(() => {
        this[loadingKey] = false
      })
    }
  }
}
</script>

<style scoped>
.record-control-form {
  padding: 16px 0;
}
</style>
