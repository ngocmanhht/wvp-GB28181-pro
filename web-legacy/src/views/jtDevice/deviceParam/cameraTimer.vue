<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="Take photos regularly" prop="rolloverAlarm">
          <el-checkbox label="channel1" v-model="form.cameraTimer.switchForChannel1"></el-checkbox>
          <el-checkbox label="channel2" v-model="form.cameraTimer.switchForChannel2"></el-checkbox>
          <el-checkbox label="channel3" v-model="form.cameraTimer.switchForChannel3"></el-checkbox>
          <el-checkbox label="channel4" v-model="form.cameraTimer.switchForChannel4"></el-checkbox>
          <el-checkbox label="channel5" v-model="form.cameraTimer.switchForChannel5"></el-checkbox>
        </el-form-item>
        <el-form-item label="Scheduled photo storage" prop="rolloverAlarm">
          <el-checkbox label="channel1" v-model="form.cameraTimer.storageFlagsForChannel1"></el-checkbox>
          <el-checkbox label="channel2" v-model="form.cameraTimer.storageFlagsForChannel2"></el-checkbox>
          <el-checkbox label="channel3" v-model="form.cameraTimer.storageFlagsForChannel3"></el-checkbox>
          <el-checkbox label="channel4" v-model="form.cameraTimer.storageFlagsForChannel4"></el-checkbox>
          <el-checkbox label="channel5" v-model="form.cameraTimer.storageFlagsForChannel5"></el-checkbox>
        </el-form-item>
        <el-form-item label="Timing time unit" prop="rolloverAlarm">
          <el-radio-group v-model="form.cameraTimer.timeUnit">
            <el-radio :label="true">points</el-radio>
            <el-radio :label="false">seconds</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="scheduled time interval" prop="timeInterval">
          <el-input v-model="form.cameraTimer.timeInterval" clearable />
        </el-form-item>

      </el-form>
    </div>
    <p style="text-align: right">
      <el-button type="primary" @click="onSubmit">Confirm</el-button>
      <el-button @click="showDevice">Cancel</el-button>
    </p>

  </div>
</template>

<script>

export default {
  name: 'communication',
  components: {
  },
  props: {
    phoneNumber: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      form: {},
      isLoading: false
    }
  },

  mounted() {
    this.initData()
  },
  methods: {
    initData: function() {
      this.isLoading = true
      this.$store.dispatch('jtDevice/queryConfig', this.phoneNumber)
        .then((data) => {
          if (!data.cameraTimer) {
            data.cameraTimer = {}
          }
          this.form = data
        })
        .catch((e) => {
          console.log(e)
        })
        .finally(() => {
          this.isLoading = false
        })
    },
    onSubmit: function() {
      this.$emit('submit', this.form)
    },
    showDevice: function() {
      this.$emit('show-device')
    }
  }
}
</script>
