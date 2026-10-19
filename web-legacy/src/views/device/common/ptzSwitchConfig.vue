<template>
  <div>
    <el-form inline label-width="120px" size="small">
      <el-form-item label="Switch number" style="margin-bottom: 0;">
        <el-input-number v-model="switchId" :min="1" :max="255" controls-position="right" style="width: 140px" />
      </el-form-item>
      <el-form-item style="margin-bottom: 0;">
        <el-button type="primary" :loading="loading" :disabled="loading" @click="control('on')">turn on</el-button>
        <el-button :loading="loading" :disabled="loading" @click="control('off')">Close</el-button>
      </el-form-item>
      <el-divider />

      <el-form-item style="margin-bottom: 0;" label="wipers">
        <el-button type="primary" :loading="wiperLoading" :disabled="wiperLoading" @click="wiperControl('on')">turn on</el-button>
        <el-button :loading="wiperLoading" :disabled="wiperLoading" @click="wiperControl('off')">Close</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'PtzSwitchConfig',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      switchId: 1,
      loading: false,
      wiperLoading: false
    }
  },
  methods: {
    wiperControl(command) {
      this.wiperLoading = true
      this.$store.dispatch('frontEnd/wiper', [this.deviceId, this.channelDeviceId, command])
        .then(() => {
          this.$message({ showClose: true, message: command === 'on' ? 'Wipers are on' : 'Wipers are off', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.wiperLoading = false
        })
    },
    control(command) {
      this.loading = true
      this.$store.dispatch('frontEnd/auxiliary', [this.deviceId, this.channelDeviceId, command, this.switchId])
        .then(() => {
          this.$message({ showClose: true, message: command === 'on' ? 'switch is on' : 'switch is off', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
