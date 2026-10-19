<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="driver information"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared">
        <el-descriptions :column="2" v-if="driverInfo" style="margin-bottom: 1rem;">
          <el-descriptions-item label="Status">{{ getStatus(driverInfo.status) }}</el-descriptions-item>
          <el-descriptions-item label="time">{{ driverInfo.time }}</el-descriptions-item>
          <el-descriptions-item label="ICCard reading result">{{ getICInfo(driverInfo.result) }}</el-descriptions-item>
          <el-descriptions-item label="driver name">{{ driverInfo.name }}</el-descriptions-item>
          <el-descriptions-item label="Professional qualification certificate code">{{ driverInfo.certificateCode }}</el-descriptions-item>
          <el-descriptions-item label="Name of issuing authority">{{ driverInfo.certificateIssuanceMechanismName }}</el-descriptions-item>
          <el-descriptions-item label="Certificate validity period">{{ driverInfo.expire }}</el-descriptions-item>
          <el-descriptions-item label="Driver ID number">{{ driverInfo.driverIdNumber }}</el-descriptions-item>
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
      driverInfo: null
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(data) {
      this.showDialog = true
      this.driverInfo = data
    },
    close: function() {
      this.showDialog = false
    },
    getStatus: function(status) {
      switch (status) {
        case 1:
          return 'ICCard inserted'
        case 2:
          return 'ICCard pulled out'
        default:
          return 'unknown'

      }
    },
    getICInfo: function(result) {
      switch (result) {
        case 0:
          return 'ICCard reading successful'
        case 1:
          return 'Card reading failed: Card key authentication failed'
        case 2:
          return 'Card reading failed: card has been locked'
        case 3:
          return 'Card reading failed: card was pulled out'
        case 4:
          return 'Card reading failed: Data verification error'
        default:
          return 'Unknown reason for failure'

      }
    }
  }
}
</script>
