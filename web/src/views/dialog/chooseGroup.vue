<template>
  <div id="chooseGroup">
    <el-dialog
      v-el-drag-dialog
      title="Select a virtual organization"
      width="30%"
      top="5rem"
      :append-to-body="true"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <GroupTree
        ref="regionTree"
        :show-header="true"
        :edit="true"
        :enable-add-channel="false"
        @clickEvent="treeNodeClickEvent"
        :on-channel-change="onChannelChange"
        :tree-height="'45vh'"
      />
      <el-form>
        <el-form-item>
          <div style="text-align: right">
            <el-button type="primary" @click="onSubmit">save</el-button>
            <el-button @click="close">Cancel</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'
import GroupTree from '../common/GroupTree.vue'

export default {
  name: 'ChooseCivilCode',
  directives: { elDragDialog },
  components: { GroupTree },
  props: {},
  data() {
    return {
      showDialog: false,
      endCallback: false,
      groupDeviceId: '',
      groupName: '',
      businessGroup: ''
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(callback) {
      this.showDialog = true
      this.endCallback = callback
      this.groupDeviceId = ''
      this.groupName = ''
      this.businessGroup = ''
    },
    onSubmit: function() {
      if (this.endCallback) {
        this.endCallback(this.groupDeviceId, this.businessGroup, this.groupName)
      }
      this.close()
    },
    close: function() {
      this.showDialog = false
    },
    treeNodeClickEvent: function(group) {
      console.log(111)
      console.log(group)
      if (group.deviceId === '' || group.deviceId === group.businessGroup) {
        return
      }
      this.groupDeviceId = group.deviceId
      this.businessGroup = group.businessGroup
      this.groupName = group.name
    },
    onChannelChange: function(deviceId) {
      //
    }
  }
}
</script>
