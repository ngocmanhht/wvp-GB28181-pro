<template>
  <div id="dhAlarmConfigPage">
    <div class="alarm-config-body">
      <div class="card-list">
        <div class="alarm-section">
          <div class="section-header">Alarm settings</div>
          <el-form ref="alarmSettingForm">
            <el-form-item>
              <el-button type="primary" @click="handleSetGuard">arm</el-button>
              <el-button type="warning" @click="handleResetGuard">disarm</el-button>
              <el-button type="danger" @click="handleResetAlarm">reset</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AlarmConfigPage',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {

    }
  },
  mounted() {},
  methods: {
    handleSetGuard() {
      this.$confirm('Confirm to arm the channel？', 'Tips', {
        confirmButtonText: 'OK', cancelButtonText: 'Cancel', type: 'warning'
      }).then(() => {
        this.$store.dispatch('device/setGuard', this.deviceId).then(() => {
          this.$message.success('Armed successfully')
        })
      }).catch(() => {})
    },
    handleResetGuard() {
      this.$confirm('Confirm to disarm the channel？', 'Tips', {
        confirmButtonText: 'OK', cancelButtonText: 'Cancel', type: 'warning'
      }).then(() => {
        this.$store.dispatch('device/resetGuard', this.deviceId).then(() => {
          this.$message.success('Disarmed successfully')
        })
      }).catch(() => {})
    },
    handleResetAlarm() {
      this.$confirm('Confirm the reset operation for the channel？', 'Tips', {
        confirmButtonText: 'OK', cancelButtonText: 'Cancel', type: 'warning'
      }).then(() => {
        this.$store.dispatch('device/resetAlarm', {
          deviceId: this.deviceId,
          channelId: this.channelDeviceId
        }).then(() => {
          this.$message.success('Reset successful')
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
#dhAlarmConfigPage {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.alarm-config-body {
  flex: 1;
  padding-top: 16px;
  overflow: auto;
}

.card-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-content: flex-start;
}

.alarm-section {
  width: 380px;
  flex-shrink: 0;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  padding: 16px;
}

.section-header {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.alarm-section .el-select,
.alarm-section .el-input-number {
  width: 100%;
}

.alarm-section .el-button + .el-button {
  margin-left: 12px;
}

</style>
