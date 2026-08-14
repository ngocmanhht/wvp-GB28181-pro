<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="reporting strategy" prop="locationReportingStrategy">
          <el-select v-model="form.locationReportingStrategy" style="float: left; width: 100%">
            <el-option label="Report regularly" :value="0">Report regularly</el-option>
            <el-option label="Regular reporting" :value="1" />
            <el-option label="Timed and interval reporting" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="Reporting plan" prop="locationReportingPlan">
          <el-select v-model="form.locationReportingPlan" style="float: left; width: 100%">
            <el-option label="According to ACC status" :value="0" />
            <el-option label="Login status and ACC status" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="Driver not logged in reporting time interval(seconds)" prop="reportingIntervalOffline">
          <el-input type="number" v-model="form.reportingIntervalOffline" placeholder="Please enter the driver not logged in reporting time interval" />
        </el-form-item>
        <el-form-item label="Reporting interval during sleep(seconds)" prop="reportingIntervalDormancy">
          <el-input type="number" v-model="form.reportingIntervalDormancy" placeholder="Please enter the hibernation reporting interval" />
        </el-form-item>
        <el-form-item label="Reporting interval during emergency alarm(seconds)" prop="reportingIntervalEmergencyAlarm">
          <el-input type="number" v-model="form.reportingIntervalEmergencyAlarm" placeholder="Please enter the reporting time interval for emergency alarms" />
        </el-form-item>
        <el-form-item label="Default time reporting interval(seconds)" prop="reportingIntervalDefault">
          <el-input type="number" v-model="form.reportingIntervalDefault" placeholder="Please enter the default time reporting interval" />
        </el-form-item>
        <el-form-item label="Default distance reporting interval(meters)" prop="reportingDistanceDefault">
          <el-input type="number" v-model="form.reportingDistanceDefault" placeholder="Please enter the default distance reporting interval" />
        </el-form-item>
        <el-form-item label="Driver not logged in reporting distance interval(meters)" prop="reportingDistanceOffline">
          <el-input type="number" v-model="form.reportingDistanceOffline" placeholder="Please enter the reporting distance interval when the driver is not logged in" />
        </el-form-item>
        <el-form-item label="Report distance interval while sleeping(meters)" prop="reportingDistanceDormancy">
          <el-input type="number" v-model="form.reportingDistanceDormancy" placeholder="Please enter the reporting distance interval when sleeping" />
        </el-form-item>
        <el-form-item label="Reporting distance interval during emergency alarm(meters)" prop="reportingDistanceEmergencyAlarm">
          <el-input type="number" v-model="form.reportingDistanceEmergencyAlarm" placeholder="Please enter the reporting distance interval during emergency alarm" />
        </el-form-item>
        <el-form-item label="Inflection point compensation angle(degree, less than180)" prop="inflectionPointAngle">
          <el-input type="number" v-model="form.inflectionPointAngle" placeholder="Please enter the angle of inflection point." />
        </el-form-item>
        <el-form-item label="Electronic fence radius(meters)" prop="fenceRadius">
          <el-input type="number" v-model="form.fenceRadius" placeholder="Please enter the electronic fence radius" />
        </el-form-item>
        <el-form-item label="illegal driving period-start time(HH:mm)" prop="illegalDrivingPeriods">
          <el-input v-model="form.illegalDrivingPeriods.startTime" clearable />
        </el-form-item>
        <el-form-item label="illegal driving period-end time(HH:mm)" prop="illegalDrivingPeriods">
          <el-input v-model="form.illegalDrivingPeriods.endTime" clearable />
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
      illegalDrivingPeriods: [new Date(), new Date()],
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
          if (!data.illegalDrivingPeriods) {
            data.illegalDrivingPeriods = {
              startTime: null,
              endTime: null
            }
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
