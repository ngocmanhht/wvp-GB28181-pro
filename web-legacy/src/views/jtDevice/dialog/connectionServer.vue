<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="Connect to the specified server"
      width="=80%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div style="padding: 0 20px 0 10px">
        <el-form label-width="110px">
          <el-form-item label="Service type">
            <el-radio-group v-model="form.switchOn">
              <el-radio :label="false" border>Specify supervision platform server</el-radio>
              <el-radio :label="true" border>Original default monitoring platform server</el-radio>
            </el-radio-group>
          </el-form-item>
          <div v-if="form.switchOn != null && !form.switchOn">
            <el-form-item label="Platform authentication code">
              <el-input type="input" v-model="form.authentication" ></el-input>
            </el-form-item>
            <el-form-item label="dial point name">
              <el-input type="input" v-model="form.name" ></el-input>
            </el-form-item>
            <el-form-item label="Dial-up username">
              <el-input type="input" v-model="form.username" ></el-input>
            </el-form-item>
            <el-form-item label="Dial-up password">
              <el-input type="input" v-model="form.password" ></el-input>
            </el-form-item>
            <el-form-item label="IPaddress">
              <el-input type="input" v-model="form.address" ></el-input>
            </el-form-item>
            <el-form-item label="TCPport">
              <el-input type="input" v-model="form.tcpPort" ></el-input>
            </el-form-item>
            <el-form-item label="UDPport">
              <el-input type="input" v-model="form.udpPort" ></el-input>
            </el-form-item>
            <el-form-item label="time limit">
              <el-input type="input" v-model="form.timeLimit" ></el-input>
            </el-form-item>
          </div>

          <el-form-item style="text-align: right">
            <el-button type="primary" @click="onSubmit">Confirm</el-button>
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
  name: 'ConnectionServer',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showDialog: false,
      phoneNumber: null,
      form: {
        switchOn: null,
        authentication: null,
        name: null,
        username: null,
        password: null,
        address: null,
        tcpPort: null,
        udpPort: null,
        timeLimit: null,
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
      this.phoneNumber = data
      this.form = {
        switchOn: null,
        authentication: null,
        name: null,
        username: null,
        password: null,
        address: null,
        tcpPort: null,
        udpPort: null,
        timeLimit: null,
        sign: 0, // Flag: 0: normal call, 1: monitoring
        destPhoneNumber: null // call back phone number
      }
    },
    close: function() {
      this.showDialog = false
    },
    onSubmit: function() {
      this.$store.dispatch('jtDevice/connection', {
        phoneNumber: this.phoneNumber,
        control: this.form
      })
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
