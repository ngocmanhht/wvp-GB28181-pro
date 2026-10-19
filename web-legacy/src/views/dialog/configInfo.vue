<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="access information"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-top: 1rem;margin-right: 100px;">
        <el-descriptions v-if="(!key || key === 'sip') && configInfoData.sip" title="National standard service information" :span="2">
          <el-descriptions-item label="No.">{{ configInfoData.sip.id }}</el-descriptions-item>
          <el-descriptions-item label="domain">{{ configInfoData.sip.domain }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ configInfoData.sip.showIp }}</el-descriptions-item>
          <el-descriptions-item label="port">{{ configInfoData.sip.port }}</el-descriptions-item>
          <el-descriptions-item label="Password">
            <el-tag size="small">{{ configInfoData.sip.password }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <el-descriptions v-if="key === 'jt1078Config' && configInfoData.jt1078Config" title="Ministry standard service information" :span="2">
          <el-descriptions-item label="port">{{ configInfoData.jt1078Config.port }}</el-descriptions-item>
          <el-descriptions-item label="Password">
            <el-tag size="small">{{ configInfoData.jt1078Config.password }}</el-tag>
          </el-descriptions-item>
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
      key: null,
      configInfoData: {
        sip: {}

      }
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(data, key) {
      console.log(data)
      this.showDialog = true
      this.key = key
      this.configInfoData = data
    },
    close: function() {
      this.showDialog = false
    }
  }
}
</script>
