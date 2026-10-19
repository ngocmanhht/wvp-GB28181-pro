<template>
  <div style="width: 100%;">
    <el-checkbox label="emergency alarm" v-model="form.urgent" @change="change"></el-checkbox>
    <el-checkbox label="speed alarm" v-model="form.alarmSpeeding" @change="change"></el-checkbox>
    <el-checkbox label="Fatigue driving alarm" v-model="form.alarmTired" @change="change"></el-checkbox>
    <el-checkbox label="Dangerous driving behavior warning" v-model="form.alarmDangerous" @change="change"></el-checkbox>
    <el-checkbox label="GNSSModule failure alarm" v-model="form.alarmGnssFault" @change="change"></el-checkbox>
    <el-checkbox label="GNSSAlarm if antenna is not connected or cut off" v-model="form.alarmGnssBreak" @change="change"></el-checkbox>
    <el-checkbox label="GNSSAntenna short circuit alarm" v-model="form.alarmGnssShortCircuited" @change="change"></el-checkbox>
    <el-checkbox label="Terminal main power undervoltage alarm" v-model="form.alarmUnderVoltage" @change="change"></el-checkbox>
    <el-checkbox label="Terminal main power failure alarm" v-model="form.alarmPowerOff" @change="change"></el-checkbox>
    <el-checkbox label="Terminal LCD or display failure alarm" v-model="form.alarmLCD" @change="change"></el-checkbox>
    <el-checkbox label="TTSModule fault alarm" v-model="form.alarmTtsFault" @change="change"></el-checkbox>
    <el-checkbox label="Camera failure alarm" v-model="form.alarmCameraFault" @change="change"></el-checkbox>
    <el-checkbox label="ICCard module failure alarm" v-model="form.alarmIcFault" @change="change"></el-checkbox>
    <el-checkbox label="speed warning" v-model="form.warningSpeeding" @change="change"></el-checkbox>
    <el-checkbox label="Fatigue driving warning" v-model="form.warningTired" @change="change"></el-checkbox>
    <el-checkbox label="Violation driving alarm" v-model="form.alarmWrong" @change="change"></el-checkbox>
    <el-checkbox label="Tire pressure warning" v-model="form.warningTirePressure" @change="change"></el-checkbox>
    <el-checkbox label="Right turn blind spot abnormality alarm" v-model="form.alarmBlindZone" @change="change"></el-checkbox>
    <el-checkbox label="Accumulated driving overtime alarm for the day" v-model="form.alarmDrivingTimeout" @change="change"></el-checkbox>
    <el-checkbox label="Overtime parking alarm" v-model="form.alarmParkingTimeout" @change="change"></el-checkbox>
    <el-checkbox label="Alarm for entry and exit areas" v-model="form.alarmRegion" @change="change"></el-checkbox>
    <el-checkbox label="Alarm for entry and exit routes" v-model="form.alarmRoute" @change="change"></el-checkbox>
    <el-checkbox label="Insufficient travel time on the road section/Alarm if too long" v-model="form.alarmTravelTime" @change="change"></el-checkbox>
    <el-checkbox label="Route deviation alarm" v-model="form.alarmRouteDeviation" @change="change"></el-checkbox>
    <el-checkbox label="Vehicle VSS failure" v-model="form.alarmVSS" @change="change"></el-checkbox>
    <el-checkbox label="Vehicle oil level abnormality alarm" v-model="form.alarmOil" @change="change"></el-checkbox>
    <el-checkbox label="vehicle stolen alarm" v-model="form.alarmStolen" @change="change"></el-checkbox>
    <el-checkbox label="Illegal vehicle ignition alarm" v-model="form.alarmIllegalIgnition" @change="change"></el-checkbox>
    <el-checkbox label="Vehicle illegal displacement alarm" v-model="form.alarmIllegalDisplacement" @change="change"></el-checkbox>
    <el-checkbox label="Collision and rollover alarm" v-model="form.alarmRollover" @change="change"></el-checkbox>
    <el-checkbox label="Rollover warning" v-model="form.warningRollover" @change="change"></el-checkbox>
  </div>
</template>

<script>

export default {
  name: 'communication',
  components: {
  },
  model: {
    prop: 'fatherValue',
    event: 'change'
  },
  props: {
    fatherValue: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      form: {
        urgent: false,
        alarmSpeeding: false,
        alarmTired: false,
        alarmDangerous: false,
        alarmGnssFault: false,
        alarmGnssBreak: false,
        alarmGnssShortCircuited: false,
        alarmUnderVoltage: false,
        alarmPowerOff: false,
        alarmLCD: false,
        alarmTtsFault: false,
        alarmCameraFault: false,
        alarmIcFault: false,
        warningSpeeding: false,
        warningTired: false,
        alarmWrong: false,
        warningTirePressure: false,
        alarmBlindZone: false,
        alarmDrivingTimeout: false,
        alarmParkingTimeout: false,
        alarmRegion: false,
        alarmRoute: false,
        alarmTravelTime: false,
        alarmRouteDeviation: false,
        alarmVSS: false,
        alarmOil: false,
        alarmStolen: false,
        alarmIllegalIgnition: false,
        alarmIllegalDisplacement: false,
        alarmRollover: false,
        warningRollover: false
      },
      isLoading: false
    }
  },

  mounted() {
    if (this.fatherValue !== null) {
      console.log(this.fatherValue)
      this.form.urgent = this.fatherValue.urgent || false
      this.form.alarmSpeeding = this.fatherValue.alarmSpeeding || false
      this.form.alarmTired = this.fatherValue.alarmTired || false
      this.form.alarmDangerous = this.fatherValue.alarmDangerous || false
      this.form.alarmGnssFault = this.fatherValue.alarmGnssFault || false
      this.form.alarmGnssBreak = this.fatherValue.alarmGnssBreak || false
      this.form.alarmGnssShortCircuited = this.fatherValue.alarmGnssShortCircuited || false
      this.form.alarmUnderVoltage = this.fatherValue.alarmUnderVoltage || false
      this.form.alarmPowerOff = this.fatherValue.alarmPowerOff || false
      this.form.alarmLCD = this.fatherValue.alarmLCD || false
      this.form.alarmTtsFault = this.fatherValue.alarmTtsFault || false
      this.form.alarmCameraFault = this.fatherValue.alarmCameraFault || false
      this.form.alarmIcFault = this.fatherValue.alarmIcFault || false
      this.form.warningSpeeding = this.fatherValue.warningSpeeding || false
      this.form.warningTired = this.fatherValue.warningTired || false
      this.form.alarmWrong = this.fatherValue.alarmWrong || false
      this.form.warningTirePressure = this.fatherValue.warningTirePressure || false
      this.form.alarmBlindZone = this.fatherValue.alarmBlindZone || false
      this.form.alarmDrivingTimeout = this.fatherValue.alarmDrivingTimeout || false
      this.form.alarmParkingTimeout = this.fatherValue.alarmParkingTimeout || false
      this.form.alarmRegion = this.fatherValue.alarmRegion || false
      this.form.alarmRoute = this.fatherValue.alarmRoute || false
      this.form.alarmTravelTime = this.fatherValue.alarmTravelTime || false
      this.form.alarmRouteDeviation = this.fatherValue.alarmRouteDeviation || false
      this.form.alarmVSS = this.fatherValue.alarmVSS || false
      this.form.alarmOil = this.fatherValue.alarmOil || false
      this.form.alarmStolen = this.fatherValue.alarmStolen || false
      this.form.alarmIllegalIgnition = this.fatherValue.alarmIllegalIgnition || false
      this.form.alarmIllegalDisplacement = this.fatherValue.alarmIllegalDisplacement || false
      this.form.alarmRollover = this.fatherValue.alarmRollover || false
      this.form.warningRollover = this.fatherValue.warningRollover || false
    }
  },
  methods: {
    change() {
      this.$emit('change', this.form)
    }
  }
}
</script>
