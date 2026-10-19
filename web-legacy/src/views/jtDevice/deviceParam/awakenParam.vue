<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 80%; margin: 0 auto">
        <el-form-item label="sleep wake mode" prop="wakeUpModeByCondition">
          <el-checkbox label="Conditional wake-up" v-model="form.awakenParam.wakeUpModeByCondition" ></el-checkbox>
          <el-checkbox label="Wake up regularly" v-model="form.awakenParam.wakeUpModeByTime" ></el-checkbox>
          <el-checkbox label="Manual wake up" v-model="form.awakenParam.wakeUpModeByManual" ></el-checkbox>
        </el-form-item>
        <el-form-item label="wake condition type" prop="wakeUpConditionsByAlarm">
          <el-checkbox label="emergency alarm" v-model="form.awakenParam.wakeUpConditionsByAlarm" ></el-checkbox>
          <el-checkbox label="Collision and rollover alarm" v-model="form.awakenParam.wakeUpConditionsByRollover" ></el-checkbox>
          <el-checkbox label="vehicle door opening" v-model="form.awakenParam.wakeUpConditionsByOpenTheDoor" ></el-checkbox>
        </el-form-item>
        <el-form-item label="Scheduled wake-up day settings" prop="awakeningDayForMonday">
          <el-checkbox label="Monday" v-model="form.awakenParam.awakeningDayForMonday" ></el-checkbox>
          <el-checkbox label="Tuesday" v-model="form.awakenParam.awakeningDayForTuesday" ></el-checkbox>
          <el-checkbox label="wednesday" v-model="form.awakenParam.awakeningDayForWednesday" ></el-checkbox>
          <el-checkbox label="Thursday" v-model="form.awakenParam.awakeningDayForThursday" ></el-checkbox>
          <el-checkbox label="Friday" v-model="form.awakenParam.awakeningDayForFriday" ></el-checkbox>
          <el-checkbox label="Saturday" v-model="form.awakenParam.awakeningDayForSaturday" ></el-checkbox>
          <el-checkbox label="Sunday" v-model="form.awakenParam.awakeningDayForSunday" ></el-checkbox>
        </el-form-item>
        <el-form-item label="Daily wake up time-time period1" prop="time1Enable" >
          <div style="display: grid; grid-template-columns: 52px auto">
            <el-checkbox label="enable" v-model="form.awakenParam.time1Enable" ></el-checkbox>
            <div v-if="form.awakenParam.time1Enable" style="width: calc(100% - 52px); display: grid; grid-template-columns: 1fr 24px 1fr; padding: 0 10px">
              <el-input v-model="form.awakenParam.time1StartTime" placeholder="Please enter the start time of time period 1" clearable size="small"/>
              <span style="text-align: center">to</span>
              <el-input v-model="form.awakenParam.time1EndTime" placeholder="Please enter the end time of time period 1" clearable size="small"/>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="Daily wake up time-time period2" prop="time1Enable" >
          <div style="display: grid; grid-template-columns: 52px auto">
            <el-checkbox label="enable" v-model="form.awakenParam.time2Enable" ></el-checkbox>
            <div v-if="form.awakenParam.time2Enable" style="width: calc(100% - 52px); display: grid; grid-template-columns: 1fr 24px 1fr; padding: 0 10px">
              <el-input v-model="form.awakenParam.time2StartTime" placeholder="Please enter the start time of time period 2" clearable size="small"/>
              <span style="text-align: center">to</span>
              <el-input v-model="form.awakenParam.time2EndTime" placeholder="Please enter the end time of time period 2" clearable size="small"/>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="Daily wake up time-time period3" prop="time1Enable" >
          <div style="display: grid; grid-template-columns: 52px auto">
            <el-checkbox label="enable" v-model="form.awakenParam.time3Enable" ></el-checkbox>
            <div v-if="form.awakenParam.time3Enable" style="width: calc(100% - 52px); display: grid; grid-template-columns: 1fr 24px 1fr; padding: 0 10px">
              <el-input v-model="form.awakenParam.time3StartTime" placeholder="Please enter the start time of time period 3" clearable size="small"/>
              <span style="text-align: center">to</span>
              <el-input v-model="form.awakenParam.time3EndTime" placeholder="Please enter the end time of time period 3" clearable size="small"/>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="Daily wake up time-time period4" prop="time1Enable" >
          <div style="display: grid; grid-template-columns: 52px auto">
            <el-checkbox label="enable" v-model="form.awakenParam.time4Enable" ></el-checkbox>
            <div v-if="form.awakenParam.time4Enable" style="width: calc(100% - 52px); display: grid; grid-template-columns: 1fr 24px 1fr; padding: 0 10px">
              <el-input v-model="form.awakenParam.time4StartTime" placeholder="Please enter the start time of time period 4" clearable size="small"/>
              <span style="text-align: center">to</span>
              <el-input v-model="form.awakenParam.time4EndTime" placeholder="Please enter the end time of time period 4" clearable size="small"/>
            </div>
          </div>
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
          if (!data.awakenParam) {
            data.awakenParam = {}
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
