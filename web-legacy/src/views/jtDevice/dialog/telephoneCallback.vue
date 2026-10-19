<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="Call back"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div>
        <el-form >
          <el-form-item label="logo">
            <el-radio-group v-model="form.sign">
              <el-radio :label="0">Ordinary call</el-radio>
              <el-radio :label="1">monitor</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="call back phone number">
            <el-input type="input" v-model="form.destPhoneNumber" ></el-input>
          </el-form-item>
          <el-form-item style="text-align: right">
            <el-button type="primary" @click="onSubmit">call back</el-button>
            <el-button @click="close" >Cancel</el-button>
          </el-form-item>
        </el-form>
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
      form: {
        phoneNumber: null,
        sign: 0, // Flag: 0: normal call, 1: monitoring
        destPhoneNumber: null // call back phone number
      }
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(data) {

      this.showDialog = true
      this.form = {
        phoneNumber: null,
          sign: {
          type: 3, // 1Emergency, 2 services, 3 notifications
            terminalDisplay: true, // 1Terminal monitor display
            tts: true, // Create a new temporary file from the selection
            adScreen: true, // Advertising screen display
            source: false // false: Center navigation information true CAN fault code information
        },
        textType: 1, // text type,1 = Notification ，2 = service
          content: '' // Message content, up to 1024 bytes
      }
      this.form.phoneNumber = data.phoneNumber
    },
    close: function() {
      this.showDialog = false
    },
    onSubmit: function() {
      this.$store.dispatch('jtDevice/telephoneCallback', this.form)
        .then(data => {
          this.$message.success({
            showClose: true,
            message: 'Sent successfully'
          })
          this.close()
        })
    }
  }
}
</script>
