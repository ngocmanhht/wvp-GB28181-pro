<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="Text message delivery"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div>
        <el-form >
          <el-divider content-position="center">logo</el-divider>
          <el-form-item label="Type">
            <el-radio-group v-model="form.sign.type">
              <el-radio :label="1">urgent</el-radio>
              <el-radio :label="2">service</el-radio>
              <el-radio :label="3">Notification</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="Terminal monitor display">
            <el-checkbox v-model="form.sign.terminalDisplay"></el-checkbox>
          </el-form-item>
          <el-form-item label="Terminal TTS reading">
            <el-checkbox v-model="form.sign.tts"></el-checkbox>
          </el-form-item>
          <el-form-item label="Advertising screen display">
            <el-checkbox v-model="form.sign.adScreen"></el-checkbox>
          </el-form-item>
          <el-form-item label="Information type">
            <el-radio-group v-model="form.sign.source">
              <el-radio :label="false">Center navigation information</el-radio>
              <el-radio :label="true">CANDTC information</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-divider content-position="center">Properties</el-divider>
          <el-form-item label="text type">
            <el-radio-group v-model="form.textType">
              <el-radio :label="1">Notification</el-radio>
              <el-radio :label="2">service</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="Message content">
            <el-input type="textarea" v-model="form.content" maxlength="1024" show-word-limit></el-input>
          </el-form-item>
          <el-form-item style="text-align: right">
            <el-button type="primary" @click="onSubmit">Issue</el-button>
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
      this.$store.dispatch('jtDevice/sendTextMessage', this.form)
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
