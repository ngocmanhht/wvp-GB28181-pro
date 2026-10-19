<template>
  <div>
    <el-button size="small" :loading="loading" :disabled="loading" @click="control('on')">turn on</el-button>
    <el-button size="small" :loading="loading" :disabled="loading" @click="control('off')">Close</el-button>
  </div>
</template>

<script>
export default {
  name: 'PtzWiperConfig',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      loading: false
    }
  },
  methods: {
    control(command) {
      this.loading = true
      this.$store.dispatch('frontEnd/wiper', [this.deviceId, this.channelDeviceId, command])
        .then(() => {
          this.$message({ showClose: true, message: command === 'on' ? 'Wipers are on' : 'Wipers are off', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
