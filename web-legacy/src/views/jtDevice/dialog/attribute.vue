<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="terminal properties"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared">
        <el-descriptions title="Basic properties" :column="2" v-if="attributeData" style="margin-bottom: 1rem;">
          <el-descriptions-item label="manufacturerID">{{ attributeData.makerId }}</el-descriptions-item>
          <el-descriptions-item label="Terminal model">{{ attributeData.deviceModel }}</el-descriptions-item>
          <el-descriptions-item label="terminalID">{{ attributeData.terminalId }}</el-descriptions-item>
          <el-descriptions-item label="SIMcardICCID">{{ attributeData.iccId }}</el-descriptions-item>
          <el-descriptions-item label="Hardware version number">{{ attributeData.hardwareVersion }}</el-descriptions-item>
          <el-descriptions-item label="Firmware version number">{{ attributeData.firmwareVersion }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="terminal type" :column="2" v-if="attributeData.type" style="margin-bottom: 1rem;">
          <el-descriptions-item label="passenger vehicle">{{ attributeData.type.passengerVehicles }}</el-descriptions-item>
          <el-descriptions-item label="Dangerous goods vehicles">{{ attributeData.type.dangerousGoodsVehicles }}</el-descriptions-item>
          <el-descriptions-item label="Ordinary freight vehicles">{{ attributeData.type.freightVehicles }}</el-descriptions-item>
          <el-descriptions-item label="rental vehicle">{{ attributeData.type.rentalVehicles }}</el-descriptions-item>
          <el-descriptions-item label="hard disk video">{{ attributeData.type.hardDiskRecording?'support': 'Not supported' }}</el-descriptions-item>
          <el-descriptions-item label="Type">{{ attributeData.type.splittingMachine?'Split machine': 'All-in-one machine' }}</el-descriptions-item>
          <el-descriptions-item label="Suitable for trailers">{{ attributeData.type.trailer?'Yes': 'No' }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="GNSSmodule" :column="2" v-if="attributeData.gnssAttribute" style="margin-bottom: 1rem;">
          <el-descriptions-item label="GPSsatellite">{{ attributeData.gnssAttribute.gps?'support': 'Not supported' }}</el-descriptions-item>
          <el-descriptions-item label="Beidou satellite">{{ attributeData.gnssAttribute.beidou?'support': 'Not supported' }}</el-descriptions-item>
          <el-descriptions-item label="GLONASSsatellite">{{ attributeData.gnssAttribute.glonass?'support': 'Not supported' }}</el-descriptions-item>
          <el-descriptions-item label="Galileosatellite">{{ attributeData.gnssAttribute.gaLiLeo?'support': 'Not supported' }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions title="Communication module" :column="2" v-if="attributeData.communicationModuleAttribute" style="margin-bottom: 1rem;">
          <el-descriptions-item label="GPRScommunication">{{ attributeData.communicationModuleAttribute.gprs?'support': 'Not supported' }}</el-descriptions-item>
          <el-descriptions-item label="CDMAcommunication">{{ attributeData.communicationModuleAttribute.cdma?'support': 'Not supported' }}</el-descriptions-item>
          <el-descriptions-item label="TD-SCDMAcommunication">{{ attributeData.communicationModuleAttribute.tdScdma?'support': 'Not supported' }}</el-descriptions-item>
          <el-descriptions-item label="WCDMAcommunication">{{ attributeData.communicationModuleAttribute.wcdma?'support': 'Not supported' }}</el-descriptions-item>
          <el-descriptions-item label="CDMA2000communication">{{ attributeData.communicationModuleAttribute.cdma2000?'support': 'Not supported' }}</el-descriptions-item>
          <el-descriptions-item label="TD-LTEcommunication">{{ attributeData.communicationModuleAttribute.tdLte?'support': 'Not supported' }}</el-descriptions-item>
          <el-descriptions-item label="Other means of communication">{{ attributeData.communicationModuleAttribute.other?'support': 'Not supported' }}</el-descriptions-item>
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
      attributeData: null
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(data) {
      this.showDialog = true
      this.attributeData = data
    },
    close: function() {
      this.showDialog = false
    }
  }
}
</script>
