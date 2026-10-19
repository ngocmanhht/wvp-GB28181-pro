<template>
  <div class="upgrade-form">
    <div class="reboot-section">
      <h4>remote start</h4>
      <el-button type="warning" @click="handleReboot">Device restart</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UpgradeConfig',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      firmware: '',
      fileName: '',
      headers: {
        'access-token': this.$store.getters.token
      }
    }
  },
  methods: {
    handleReboot() {
      this.$confirm('Confirm remote restart of the device？', 'Tips', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('device/teleboot', this.deviceId).then(() => {
          this.$message({ showClose: true, message: 'Remote start command sent', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error.message, type: 'error' })
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.upgrade-form {
  padding: 16px 0;
}
.upgrade-form >>> .upgrade-input {
  width: 360px;
}
.upload-row {
  display: flex;
  align-items: center;
}
.reboot-section {
  padding: 8px 0;
}
.reboot-section h4 {
  margin: 0 0 12px;
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
</style>
