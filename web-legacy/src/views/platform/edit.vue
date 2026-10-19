<template>
  <div id="PlatformEdit" style="width: 100%">
    <div id="shared" style="text-align: right; margin-top: 1rem; background-color: #FFFFFF; padding-top: 2rem;">
      <el-row :gutter="24">
        <el-col :span="11">
          <el-form ref="platform1" :rules="rules" :model="value" size="medium" label-width="160px">
            <el-form-item label="Name" prop="name">
              <el-input v-model="value.name" />
            </el-form-item>
            <el-form-item label="SIPService national standard code" prop="serverGBId">
              <el-input v-model="value.serverGBId" clearable @input="serverGBIdChange" />
            </el-form-item>
            <el-form-item label="SIPService national standard domain" prop="serverGBDomain">
              <el-input v-model="value.serverGBDomain" clearable />
            </el-form-item>
            <el-form-item label="SIPserviceIP" prop="serverIp">
              <el-input v-model="value.serverIp" clearable />
            </el-form-item>
            <el-form-item label="SIPservice port" prop="serverPort">
              <el-input v-model="value.serverPort" clearable type="number" />
            </el-form-item>
            <el-form-item label="Equipment national standard number" prop="deviceGBId">
              <el-input v-model="value.deviceGBId" clearable @input="deviceGBIdChange" />
            </el-form-item>
            <el-form-item label="localIP" prop="deviceIp">
              <el-select v-model="value.deviceIp" placeholder="Please select a network card that is connected to your superior" style="width: 100%">
                <el-option
                  v-for="ip in deviceIps"
                  :key="ip"
                  :label="ip"
                  :value="ip"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="local port" prop="devicePort">
              <el-input v-model="value.devicePort" :disabled="true" type="number" />
            </el-form-item>

            <el-form-item label="SIPAuthentication username" prop="username">
              <el-input v-model="value.username" />
            </el-form-item>
            <el-form-item label="SIPAuthentication password" prop="password">
              <el-input v-model="value.password" />
            </el-form-item>
            <el-form-item label="Registration cycle(seconds)" prop="expires">
              <el-input v-model="value.expires" />
            </el-form-item>
            <el-form-item label="heartbeat cycle(seconds)" prop="keepTimeout">
              <el-input v-model="value.keepTimeout" />
            </el-form-item>
          </el-form>
        </el-col>
        <el-col :span="12">
          <el-form ref="platform2" :rules="rules" :model="value" size="medium" label-width="160px">
            <el-form-item label="SDPFlowIP" prop="sendStreamIp">
              <el-input v-model="value.sendStreamIp" />
            </el-form-item>
            <el-form-item label="Signaling transmission" prop="transport">
              <el-select
                v-model="value.transport"
                style="width: 100%"
                placeholder="Please select signaling transmission method"
              >
                <el-option label="UDP" value="UDP" />
                <el-option label="TCP" value="TCP" />
              </el-select>
            </el-form-item>
            <el-form-item label="Confidential attribute">
              <el-select v-model="value.secrecy" style="width: 100%" placeholder="Please select confidentiality attribute">
                <el-option label="Not confidential" :value="0" />
                <el-option label="Confidential" :value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="directory grouping" prop="catalogGroup">
              <el-select
                v-model="value.catalogGroup"
                style="width: 100%"
                placeholder="Please select a directory group"
              >
                <el-option label="1" value="1" />
                <el-option label="2" value="2" />
                <el-option label="4" value="4" />
                <el-option label="8" value="8" />
              </el-select>
            </el-form-item>
            <el-form-item label="character set" prop="characterSet">
              <el-select
                v-model="value.characterSet"
                style="width: 100%"
                placeholder="Please select a character set"
              >
                <el-option label="GB2312" value="GB2312" />
                <el-option label="UTF-8" value="UTF-8" />
              </el-select>
            </el-form-item>
            <el-form-item label="Administrative division" prop="civilCode">
              <el-input v-model="value.civilCode" clearable />
            </el-form-item>
            <el-form-item label="Platform vendors" prop="manufacturer">
              <el-input v-model="value.manufacturer" clearable />
            </el-form-item>
            <el-form-item label="Platform model" prop="model">
              <el-input v-model="value.model" clearable />
            </el-form-item>
            <el-form-item label="Platform installation address" prop="address">
              <el-input v-model="value.address" clearable />
            </el-form-item>
            <el-form-item label="Other options">
              <div style="text-align: left">
                <el-checkbox v-model="value.enable" label="enable" @change="checkExpires" />
                <!--                <el-checkbox label="PTZ control" v-model="value.ptz"></el-checkbox>-->
                <el-checkbox v-model="value.rtcp" label="RTCPkeep alive" @change="rtcpCheckBoxChange" />
                <el-checkbox v-model="value.asMessageChannel" label="message channel" />
                <el-checkbox v-model="value.autoPushChannel" label="Active push channel" />
                <el-checkbox
                  v-model="value.catalogWithPlatform"
                  label="Push platform information"
                  :true-label="1"
                  :false-label="0"
                />
                <el-checkbox
                  v-model="value.catalogWithGroup"
                  label="Push group information"
                  :true-label="1"
                  :false-label="0"
                />
                <el-checkbox
                  v-model="value.catalogWithRegion"
                  label="Push administrative divisions"
                  :true-label="1"
                  :false-label="0"
                />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onSubmit">{{ onSubmit_text }} </el-button>
              <el-button @click="close">Cancel</el-button>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>

export default {
  name: 'PlatformEdit',
  components: {},
  props: ['value', 'closeEdit', 'deviceIps'],
  data() {
    var deviceGBIdRules = async(rule, value, callback) => {
      console.log(value)
      if (value === '') {
        callback(new Error('Please enter the equipment national standard number'))
      } else {
        var exit = await this.deviceGBIdExit(value)
        if (exit) {
          callback(new Error('The equipment national standard number format is wrong or already exists'))
        } else {
          callback()
        }
      }
    }
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      onSubmit_text: 'save',

      rules: {
        name: [{ required: true, message: 'Please enter the platform name', trigger: 'blur' }],
        serverGBId: [
          { required: true, message: 'Please enter the SIP service national standard code', trigger: 'blur' }
        ],
        serverGBDomain: [
          { required: true, message: 'Please enter the SIP service national standard domain', trigger: 'blur' }
        ],
        serverIp: [{ required: true, message: 'Please enter SIP serviceIP', trigger: 'blur' }],
        serverPort: [{ required: true, message: 'Please enter the SIP service port', trigger: 'blur' }],
        deviceGBId: [{ validator: deviceGBIdRules, trigger: 'blur' }],
        username: [{ required: false, message: 'Please enter the SIP authentication username', trigger: 'blur' }],
        password: [{ required: false, message: 'Please enter the SIP authentication password', trigger: 'blur' }],
        expires: [{ required: true, message: 'Please enter the registration period', trigger: 'blur' }],
        keepTimeout: [{ required: true, message: 'Please enter the heartbeat cycle', trigger: 'blur' }],
        transport: [{ required: true, message: 'Please select signaling transmission', trigger: 'blur' }],
        characterSet: [{ required: true, message: 'Please select an encoding character set', trigger: 'blur' }],
        deviceIp: [{ required: true, message: 'Please select localIP', trigger: 'blur' }]
      },

      saveLoading: false
    }
  },
  watch: {
    value(newValue, oldValue) {
      this.streamProxy = newValue
    }
  },
  created() {

  },
  methods: {
    onSubmit: function() {
      this.saveLoading = true
      if (this.value.id) {
        this.$store.dispatch('platform/update', this.value)
          .then(data => {
            this.$message({
              showClose: true,
              message: 'Saved successfully',
              type: 'success'
            })
            if (this.closeEdit) {
              this.closeEdit()
            }
          })
          .catch(error => {
            console.log(error)
          })
          .finally(() => {
            this.saveLoading = false
          })
      } else {
        this.$store.dispatch('platform/add', this.value)
          .then(data => {
            this.$message({
              showClose: true,
              message: 'Saved successfully',
              type: 'success'
            })
            if (this.closeEdit) {
              this.closeEdit()
            }
          })
          .catch(error => {
            console.log(error)
          })
          .finally(() => {
            this.saveLoading = false
          })
      }
    },
    serverGBIdChange: function() {
      if (this.value.serverGBId.length > 10) {
        this.value.serverGBDomain = this.value.serverGBId.substr(0, 10)
      }
    },
    deviceGBIdChange: function() {
      this.value.username = this.value.deviceGBId
    },
    checkExpires: function() {
      if (this.value.enable && this.value.expires === '0') {
        this.value.expires = '3600'
      }
    },
    rtcpCheckBoxChange: function(result) {
      if (result) {
        this.$message({
          showClose: true,
          message: 'Enabling RTCP keep-alive requires the support of the upper-level platform to avoid invalid push streams.',
          type: 'warning'
        })
      }
    },
    deviceGBIdExit: async function(deviceGbId) {
      let result = false
      await this.$store.dispatch('platform/exit', deviceGbId)
        .then((data) => {
          result = data
        }).catch((error) => {
          console.log(error)
        })
      return result
    },
    close: function() {
      this.closeEdit()
    }
  }
}
</script>
