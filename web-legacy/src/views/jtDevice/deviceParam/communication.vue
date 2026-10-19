<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="Heartbeat sending interval(seconds)" prop="keepaliveInterval">
          <el-input v-model="form.keepaliveInterval" clearable />
        </el-form-item>
        <el-form-item label="TCPMessage response timeout(seconds)" prop="tcpResponseTimeout">
          <el-input v-model="form.tcpResponseTimeout" clearable />
        </el-form-item>
        <el-form-item label="TCPNumber of message retransmissions" prop="tcpRetransmissionCount">
          <el-input v-model="form.tcpRetransmissionCount" clearable />
        </el-form-item>
        <el-form-item label="UDPMessage response timeout(seconds)" prop="udpResponseTimeout">
          <el-input v-model="form.udpResponseTimeout" clearable />
        </el-form-item>
        <el-form-item label="UDPNumber of message retransmissions" prop="udpRetransmissionCount">
          <el-input v-model="form.udpRetransmissionCount" clearable />
        </el-form-item>
        <el-form-item label="SMS Message response timeout(seconds)" prop="smsResponseTimeout">
          <el-input v-model="form.smsResponseTimeout" clearable />
        </el-form-item>
        <el-form-item label="SMS Number of message retransmissions" prop="smsRetransmissionCount">
          <el-input v-model="form.smsRetransmissionCount" clearable />
        </el-form-item>
      </el-form>
    </div>
    <p style="text-align: right">
      <el-button type="primary" @click="onSubmit">Confirm</el-button>
      <el-button @click="showDevice">Cancel</el-button>
    </p>

  </div>
</template>

<script>

export default {
  name: 'communication',
  components: {
  },
  props: {
    phoneNumber: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      form: {},
      isLoading: false
    }
  },

  mounted() {
    this.initData()
  },
  methods: {
    initData: function() {
      this.isLoading = true
      this.$store.dispatch('jtDevice/queryConfig', this.phoneNumber)
        .then((data) => {
          this.form = data
        })
        .catch((e) => {
          console.log(e)
        })
        .finally(() => {
          this.isLoading = false
        })
    },
    onSubmit: function() {
      this.$emit('submit', this.form)
    },
    showDevice: function() {
      this.$emit('show-device')
    }
  }
}
</script>
