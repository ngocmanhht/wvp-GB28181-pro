<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="top speed(kilometers per hour)" prop="topSpeed">
          <el-input type="number" v-model="form.topSpeed" />
        </el-form-item>
        <el-form-item label="Speeding duration(seconds)" prop="overSpeedDuration">
          <el-input type="number" v-model="form.overSpeedDuration" />
        </el-form-item>
        <el-form-item label="Continuous driving time threshold(seconds)" prop="continuousDrivingTimeThreshold">
          <el-input type="number" v-model="form.continuousDrivingTimeThreshold" />
        </el-form-item>
        <el-form-item label="Cumulative driving time threshold for the day(seconds)" prop="cumulativeDrivingTimeThresholdForTheDay">
          <el-input type="number" v-model="form.cumulativeDrivingTimeThresholdForTheDay" />
        </el-form-item>
        <el-form-item label="minimum rest time(seconds)" prop="minimumBreakTime">
          <el-input type="number" v-model="form.minimumBreakTime" />
        </el-form-item>
        <el-form-item label="Maximum parking time(seconds)" prop="maximumParkingTime">
          <el-input type="number" v-model="form.maximumParkingTime" />
        </el-form-item>
        <el-form-item label="Speed warning difference(1/10 kilometers per hour)" prop="overSpeedWarningDifference">
          <el-input type="number" v-model="form.overSpeedWarningDifference" />
        </el-form-item>
        <el-form-item label="Fatigue driving warning difference(seconds)" prop="drowsyDrivingWarningDifference">
          <el-input type="number" v-model="form.drowsyDrivingWarningDifference" />
        </el-form-item>
        <div v-if="form.collisionAlarmParams">
          <el-form-item label="Collision alarm-collision time(milliseconds)" prop="collisionAlarmParamsCollisionAlarmTime">
            <el-input type="number" v-model="form.collisionAlarmParams.collisionAlarmTime" />
          </el-form-item>
          <el-form-item label="Collision alarm-collision acceleration(0.1g)" prop="collisionAlarmParamsCollisionAcceleration">
            <el-input type="number" v-model="form.collisionAlarmParams.collisionAcceleration" />
          </el-form-item>
        </div>

        <el-form-item label="Rollover alarm parameters-Rollover angle(Degree)" prop="rolloverAlarm">
          <el-input v-model="form.rolloverAlarm" clearable />
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
