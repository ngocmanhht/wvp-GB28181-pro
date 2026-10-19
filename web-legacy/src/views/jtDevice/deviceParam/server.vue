<template>
  <div style="width: 100%;">
  <div style="height: calc(100vh - 260px); overflow: auto">
    <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto; ">
      <el-divider content-position="center">main server</el-divider>
      <el-form-item label="APN(Lord)" prop="apnMaster">
        <el-input v-model="form.apnMaster" clearable />
      </el-form-item>
      <el-form-item label="Wireless communication dial-up user name(Lord)" prop="dialingUsernameMaster">
        <el-input v-model="form.dialingUsernameMaster" clearable />
      </el-form-item>
      <el-form-item label="Wireless communication dial-up password(Lord)" prop="dialingPasswordMaster">
        <el-input v-model="form.dialingPasswordMaster" clearable />
      </el-form-item>
      <el-form-item label="IPor domain name(Lord)" prop="addressMaster">
        <el-input v-model="form.addressMaster" clearable />
      </el-form-item>


      <el-divider content-position="center">Backup server</el-divider>
      <el-form-item label="APN(Prepare)" prop="apnBackup">
        <el-input v-model="form.apnBackup" clearable />
      </el-form-item>
      <el-form-item label="Wireless communication dial-up user name(Prepare)" prop="dialingUsernameBackup">
        <el-input v-model="form.dialingUsernameBackup" clearable />
      </el-form-item>
      <el-form-item label="Wireless communication dial-up password(Prepare)" prop="dialingPasswordBackup">
        <el-input v-model="form.dialingPasswordBackup" clearable />
      </el-form-item>
      <el-form-item label="IPor domain name(Prepare)" prop="addressBackup">
        <el-input v-model="form.addressBackup" clearable />
      </el-form-item>


      <el-divider content-position="center">from server</el-divider>
      <el-form-item label="APN(from)" prop="apnBackup">
        <el-input v-model="form.apnBackup" clearable />
      </el-form-item>
      <el-form-item label="Wireless communication dial-up user name(from)" prop="dialingUsernameSlave">
        <el-input v-model="form.dialingUsernameSlave" clearable />
      </el-form-item>
      <el-form-item label="Wireless communication dial-up password(from)" prop="dialingPasswordSlave">
        <el-input v-model="form.dialingPasswordSlave" clearable />
      </el-form-item>
      <el-form-item label="IPor domain name(from)" prop="addressSlave">
        <el-input v-model="form.addressSlave" clearable />
      </el-form-item>

      <el-divider content-position="center">ICcard authentication server</el-divider>
      <el-form-item label="ICcard authentication serverIP(Lord)" prop="addressIcMaster">
        <el-input v-model="form.addressIcMaster" clearable />
      </el-form-item>
      <el-form-item label="ICcard authentication serverIP(Prepare)" prop="addressIcMaster">
        <el-input v-model="form.addressIcBackup" clearable />
      </el-form-item>
      <el-form-item label="ICCard authentication server TCP port" prop="tcpPortIcMaster">
        <el-input v-model="form.tcpPortIcMaster" clearable />
      </el-form-item>
      <el-form-item label="ICCard authentication server UDP port" prop="udpPortIcMaster">
        <el-input v-model="form.udpPortIcMaster" clearable />
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
  name: 'server',
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
