<template>
  <div id="ChannelEdit" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header content="Edit push information" @back="close" />
      </div>
    </div>
    <el-tabs tab-position="top" style="padding-top: 1rem;">
      <el-tab-pane label="Push information editing" style="background-color: #FFFFFF; padding: 1rem; height: calc(-218px + 100vh);
    overflow: auto;">
        <el-divider content-position="center">Basic information</el-divider>
        <el-form ref="streamPushForm" v-loading="locading" status-icon label-width="160px" class="channel-form">
          <el-form-item label="Application name">
            <el-input v-model="streamPush.app" placeholder="Please enter application name" />
          </el-form-item>
          <el-form-item label="flowID">
            <el-input v-model="streamPush.stream" placeholder="Please enter the streamID" />
          </el-form-item>
        </el-form>
        <el-divider content-position="center">Strategy</el-divider>
        <el-form ref="streamPushForm" v-loading="locading" status-icon label-width="160px">
          <el-form-item style="text-align: left">
            <el-checkbox v-model="streamPush.startOfflinePush">Pull up offline push flow</el-checkbox>
          </el-form-item>

        </el-form>
        <el-form style="text-align: right">
          <el-form-item>
            <el-button type="primary" @click="onSubmit">save</el-button>
            <el-button @click="close">Cancel</el-button>
          </el-form-item>
        </el-form>

      </el-tab-pane>
      <el-tab-pane v-if="streamPush.id" label="National standard channel configuration">
        <CommonChannelEdit ref="commonChannelEdit" :showCancel="true" :data-form="streamPush" @cancel="close" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CommonChannelEdit from '../common/CommonChannelEdit'

export default {
  name: 'ChannelEdit',
  components: {
    CommonChannelEdit
  },
  props: ['streamPush', 'closeEdit'],
  data() {
    return {
      locading: false
    }
  },
  created() {
    console.log(this.streamPush)
  },
  methods: {
    onSubmit: function() {
      console.log(this.streamPush)
      this.locading = true
      if (this.streamPush.id) {
        this.$store.dispatch('streamPush/update', this.streamPush)
          .then(data => {
            this.$message.success({
              showClose: true,
              message: 'Saved successfully'
            })
          })
          .finally(() => {
            this.locading = false
          })
      } else {
        this.$store.dispatch('streamPush/add', this.streamPush)
          .then(data => {
            this.$message.success({
              showClose: true,
              message: 'Saved successfully'
            })
          })
          .finally(() => {
            this.locading = false
          })
      }
    },
    close: function() {
      this.closeEdit()
    }
  }
}
</script>
<style>
.channel-form {
  display: grid;
  background-color: #FFFFFF;
  padding: 1rem 2rem 0 2rem;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}
</style>
