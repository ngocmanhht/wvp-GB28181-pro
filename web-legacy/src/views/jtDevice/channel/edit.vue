<template>
  <div id="channelEdit" style="width: 100%; height: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header content="Edit push information" @back="close" />
      </div>
    </div>
    <el-tabs tab-position="left" style="padding: 1rem; height: calc(100% - 24px)">
      <el-tab-pane label="Department logo channel editor" style="background-color: #FFFFFF;">
        <el-form ref="form" :rules="rules" :model="jtChannel" label-width="60px" style="width: 40rem; margin: 0 auto">
          <el-form-item label="No." prop="channelId">
            <el-input v-model="jtChannel.channelId" clearable />
          </el-form-item>
          <el-form-item label="Name" prop="name">
            <el-input v-model="jtChannel.name" clearable />
          </el-form-item>
          <el-form-item style="text-align: right">
            <el-button type="primary" @click="onSubmit">save</el-button>
            <el-button @click="close">Cancel</el-button>
          </el-form-item>
        </el-form>

      </el-tab-pane>
      <el-tab-pane label="National standard channel configuration">
        <CommonChannelEdit :id="jtChannel.gbId" ref="commonChannelEdit" :data-form="jtChannel" @cancel="close" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import CommonChannelEdit from '../../common/CommonChannelEdit'

export default {
  name: 'ChannelEdit',
  components: {
    CommonChannelEdit
  },
  props: ['jtChannel', 'closeEdit'],
  data() {
    return {
      version: 3,
      rules: {
        deviceId: [{ required: true, message: 'Please enter the device number', trigger: 'blur' }]
      },
      isLoading: false,
      loadSnap: {}
    }
  },

  mounted() {},
  methods: {
    onSubmit: function() {
      console.log(this.jtChannel)
      const isEdit = typeof (this.jtChannel.id) !== 'undefined'
      if (isEdit) {
        this.$store.dispatch('jtDevice/updateChannel', this.jtChannel)
          .then(data => {
            this.$message({
              showClose: true,
              message: 'Saved successfully',
              type: 'success'
            })
            this.jtChannel = data
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      } else {
        this.$store.dispatch('jtDevice/addChannel', this.jtChannel)
          .then(data => {
            this.$message({
              showClose: true,
              message: 'Saved successfully',
              type: 'success'
            })
            this.jtChannel = data
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      }
    },
    close: function() {
      this.closeEdit()
    }
  }
}
</script>
