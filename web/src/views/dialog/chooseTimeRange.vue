<template>
  <div id="chooseDateTimeRange">
    <el-dialog
      v-el-drag-dialog
      title="Select time period"
      width="500px"
      top="5rem"
      :append-to-body="true"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div style="width:fit-content; margin: 0 auto">
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          is-range
          range-separator="to"
          start-placeholder="start time"
          end-placeholder="end time"
          placeholder="Select time range"
        />
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="close">Cancel</el-button>
        <el-button type="primary" @click="onSubmit">Confirm</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ChooseDateTimeRange',
  directives: {
    elDragDialog
  },
  props: {},
  data() {
    return {
      showDialog: false,
      endCallback: null,
      timeRange: '',
      businessGroup: ''
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(initTime, callback) {
      console.log(initTime)
      if (initTime) {
        this.timeRange = initTime
      }
      this.showDialog = true
      this.endCallback = callback
    },
    onSubmit: function() {
      if (this.endCallback) {
        this.endCallback(this.timeRange)
      }
      this.close()
    },
    close: function() {
      this.timeRange = ''
      this.showDialog = false
      this.endCallback = null
    }
  }
}
</script>
<style>
#chooseDateTimeRange .el-dialog__body {
  padding: 30px 20px 2px 20px;
}
</style>
