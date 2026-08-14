<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="location information"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      :append-to-body="true"
      @close="close()"
    >
      <div id="shared" style="height: 45rem; overflow: auto">
        <el-descriptions title="Basic information" :column="3" v-if="positionData" style="margin-bottom: 1rem;">
          <el-descriptions-item label="longitude">{{ positionData.longitude }}</el-descriptions-item>
          <el-descriptions-item label="Latitude">{{ positionData.latitude }}</el-descriptions-item>
          <el-descriptions-item label="elevation">{{ positionData.altitude }}</el-descriptions-item>
          <el-descriptions-item label="speed">{{ positionData.speed }}</el-descriptions-item>
          <el-descriptions-item label="direction">{{ positionData.direction }}</el-descriptions-item>
          <el-descriptions-item label="time">{{ positionData.time }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="Alarm sign" :column="3" v-if="positionData.alarmSign" style="margin-bottom: 1rem;">
          <el-descriptions-item label="emergency alarm">{{ positionData.alarmSign.urgent?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="speed alarm">{{ positionData.alarmSign.alarmSpeeding?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Fatigue driving warning">{{ positionData.alarmSign.alarmTired?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Dangerous driving behavior warning">{{ positionData.alarmSign.alarmDangerous?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="GNSSModule fault alarm">{{ positionData.alarmSign.alarmGnssFault?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="GNSSAlarm if antenna is not connected or cut off">{{ positionData.alarmSign.alarmGnssBreak?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="GNSSAntenna short circuit alarm">{{ positionData.alarmSign.alarmGnssShortCircuited?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Terminal main power undervoltage alarm">{{ positionData.alarmSign.alarmUnderVoltage?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Terminal main power failure alarm">{{ positionData.alarmSign.alarmPowerOff?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Terminal LCD or display failure alarm">{{ positionData.alarmSign.alarmLCD?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="TTSModule fault alarm">{{ positionData.alarmSign.alarmTtsFault?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Camera failure alarm">{{ positionData.alarmSign.alarmCameraFault?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="ICCard module failure alarm">{{ positionData.alarmSign.alarmIcFault?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="speed warning">{{ positionData.alarmSign.warningSpeeding?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Fatigue driving warning">{{ positionData.alarmSign.warningTired?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Violation driving alarm">{{ positionData.alarmSign.alarmwrong?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Tire pressure warning">{{ positionData.alarmSign.warningTirePressure?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Right turn blind spot abnormality alarm">{{ positionData.alarmSign.alarmBlindZone?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Accumulated driving overtime alarm for the day">{{ positionData.alarmSign.alarmDrivingTimeout?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Overtime parking alarm">{{ positionData.alarmSign.alarmParkingTimeout?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Alarm for entry and exit areas">{{ positionData.alarmSign.alarmRegion?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Alarm for entry and exit routes">{{ positionData.alarmSign.alarmRoute?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Insufficient travel time on the road section/Alarm if too long">{{ positionData.alarmSign.alarmTravelTime?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Route deviation alarm">{{ positionData.alarmSign.alarmRouteDeviation?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Vehicle VSS failure">{{ positionData.alarmSign.alarmVSS?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Vehicle oil level abnormality alarm">{{ positionData.alarmSign.alarmOil?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="vehicle stolen alarm">{{ positionData.alarmSign.alarmStolen?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Illegal vehicle ignition alarm">{{ positionData.alarmSign.alarmIllegalIgnition?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Vehicle illegal displacement alarm">{{ positionData.alarmSign.alarmIllegalDisplacement?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Collision and rollover alarm">{{ positionData.alarmSign.alarmRollover?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Rollover warning">{{ positionData.alarmSign.warningRollover?'Yes': 'No' }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="Status" :column="3" v-if="positionData.status" style="margin-bottom: 1rem;">
          <el-descriptions-item label="ACC">{{ positionData.status.acc?'open': 'close' }}</el-descriptions-item>
          <el-descriptions-item label="Positioning">{{ positionData.status.positioning?'Targeted': 'Not located' }}</el-descriptions-item>
          <el-descriptions-item label="Northern latitude/Southern latitude">{{ positionData.status.southLatitude?'Southern latitude': 'Northern latitude' }}</el-descriptions-item>
          <el-descriptions-item label="east longitude/west longitude">{{ positionData.status.wesLongitude?'west longitude': 'east longitude' }}</el-descriptions-item>
          <el-descriptions-item label="Operation status">{{ positionData.status.outage?'Operation': 'Out of service' }}</el-descriptions-item>
          <el-descriptions-item label="Longitude and latitude security plug-in encryption">{{ positionData.status.positionEncryption?'Not encrypted': 'Encrypted' }}</el-descriptions-item>
          <el-descriptions-item label="Forward collision warning collected by emergency braking system">{{ positionData.status.warningFrontCrash?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Lane departure warning">{{ positionData.status.warningShifting?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Cargo">{{ getLoadStatus(positionData.status.load)}}</el-descriptions-item>
          <el-descriptions-item label="vehicle oil circuit">{{ positionData.status.oilWayBreak?'normal': 'Disconnect' }}</el-descriptions-item>
          <el-descriptions-item label="vehicle circuit">{{ positionData.status.circuitBreak?'normal': 'Disconnect' }}</el-descriptions-item>
          <el-descriptions-item label="door lock">{{ positionData.status.doorLocking?'Lock': 'Unlock' }}</el-descriptions-item>
          <el-descriptions-item label="door1（(front door)）">{{ positionData.status.door1Open?'close': 'open' }}</el-descriptions-item>
          <el-descriptions-item label="door2（(middle gate)）">{{ positionData.status.door2Open?'close': 'open' }}</el-descriptions-item>
          <el-descriptions-item label="door3（(back door)）">{{ positionData.status.door3Open?'close': 'open' }}</el-descriptions-item>
          <el-descriptions-item label="door4（(driver's seat door)）">{{ positionData.status.door4Open?'close': 'open' }}</el-descriptions-item>
          <el-descriptions-item label="door5">{{ positionData.status.door5Open?'close': 'open' }}</el-descriptions-item>
          <el-descriptions-item label="GPSSatellite positioning">{{ positionData.status.gps?'Use': 'Not used' }}</el-descriptions-item>
          <el-descriptions-item label="Beidou satellite positioning">{{ positionData.status.beidou?'Use': 'Not used' }}</el-descriptions-item>
          <el-descriptions-item label="GLONASSSatellite positioning">{{ positionData.status.glonass?'Use': 'Not used' }}</el-descriptions-item>
          <el-descriptions-item label="GaLiLeoSatellite positioning">{{ positionData.status.gaLiLeo?'Use': 'Not used' }}</el-descriptions-item>
          <el-descriptions-item label="Driving status">{{ positionData.status.driving?'exercise': 'stop' }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="Video alarm" :column="2" v-if="positionData.videoAlarm" style="margin-bottom: 1rem;">
          <el-descriptions-item label="Video signal loss alarm channel">{{ positionData.videoAlarm.videoLossChannels?positionData.videoAlarm.videoLossChannels.join(','): 'None' }}</el-descriptions-item>
          <el-descriptions-item label="Video signal blocking alarm channel">{{ positionData.videoAlarm.videoOcclusionChannels?positionData.videoAlarm.videoOcclusionChannels.join(','): 'None' }}</el-descriptions-item>
          <el-descriptions-item label="Memory fault alarm status">{{ positionData.videoAlarm.storageFaultAlarm?positionData.videoAlarm.storageFaultAlarm.join(','): 'None' }}</el-descriptions-item>
          <el-descriptions-item label="Abnormal driving behavior-fatigue">{{ positionData.videoAlarm.drivingForFatigue?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Abnormal driving behavior-call">{{ positionData.videoAlarm.drivingForCall?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Abnormal driving behavior-Smoking">{{ positionData.videoAlarm.drivingSmoking?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Other video equipment failure">{{ positionData.videoAlarm.otherDeviceFailure?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Bus overcrowding alarm">{{ positionData.videoAlarm.overcrowding?'Yes': 'No' }}</el-descriptions-item>
          <el-descriptions-item label="Special alarm: The recording reaches the storage threshold alarm.">{{ positionData.videoAlarm.specialRecordFull?'Yes': 'No' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showDialog: false,
      positionData: null
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(data) {
      this.showDialog = true
      this.positionData = data
    },
    getLoadStatus: function(load) {
      switch (load) {
        case 0:
          return 'Empty car'
        case 1:
          return 'Half a year'
        case 2:
          return 'Reserve'
        case 3:
          return 'Fully loaded'
      }
    },

    close: function() {
      this.showDialog = false
    }
  }
}
</script>
