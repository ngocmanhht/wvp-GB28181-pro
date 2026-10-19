<template>
  <div id="jtDeviceParam" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header content="Terminal parameters" @back="showDevice" />
      </div>
    </div>
    <el-tabs tab-position="left" v-model="activeTab" style="height: calc(100vh - 164px); padding: 20px">
      <el-tab-pane label="Communication parameters" name="communication">
        <communication v-if="activeTab === 'communication'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></communication>
      </el-tab-pane>
      <el-tab-pane label="server" name="server">
        <server v-if="activeTab === 'server'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></server>
      </el-tab-pane>
      <el-tab-pane label="location report" name="position">
        <position v-if="activeTab === 'position'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></position>
      </el-tab-pane>
      <el-tab-pane label="phone number" name="phoneNumber">
        <phoneNumber v-if="activeTab === 'phoneNumber'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></phoneNumber>
      </el-tab-pane>
      <el-tab-pane label="Alarm parameters" name="alarm">
        <alarm v-if="activeTab === 'alarm'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></alarm>
      </el-tab-pane>
      <el-tab-pane label="Driving parameters" name="driving">
        <driving v-if="activeTab === 'driving'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></driving>
      </el-tab-pane>
      <el-tab-pane label="Take photos regularly" name="cameraTimer">
        <cameraTimer v-if="activeTab === 'cameraTimer'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></cameraTimer>
      </el-tab-pane>
      <el-tab-pane label="Image parameters" name="imageConfig">
        <imageConfig v-if="activeTab === 'imageConfig'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></imageConfig>
      </el-tab-pane>
      <el-tab-pane label="Video parameters" name="videoParam">
        <videoParam v-if="activeTab === 'videoParam'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></videoParam>
      </el-tab-pane>
      <el-tab-pane label="wake up from sleep" name="awakenParam">
        <awakenParam v-if="activeTab === 'awakenParam'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></awakenParam>
      </el-tab-pane>
      <el-tab-pane label="vehicle information" name="carInfo">
        <carInfo v-if="activeTab === 'carInfo'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></carInfo>
      </el-tab-pane>
      <el-tab-pane label="Satellite positioning" name="gnssParam">
        <gnssParam v-if="activeTab === 'gnssParam'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></gnssParam>
      </el-tab-pane>
      <el-tab-pane label="CANbus" name="canCollection">
        <canCollectionParam v-if="activeTab === 'canCollection'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></canCollectionParam>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import communication from './communication.vue'
import server from './server.vue'
import position from './position.vue'
import phoneNumber from './phoneNumber.vue'
import alarm from './alarm.vue'
import driving from './driving.vue'
import cameraTimer from './cameraTimer.vue'
import imageConfig from './imageConfig.vue'
import videoParam from './videoParam.vue'
import awakenParam from './awakenParam.vue'
import carInfo from './carInfo.vue'
import gnssParam from './gnssParam.vue'
import canCollectionParam from './canCollectionParam.vue'

export default {
  name: 'JTDeviceParam',
  components: {
    communication, server, position, phoneNumber, alarm, driving, cameraTimer, imageConfig, videoParam, awakenParam, carInfo, gnssParam, canCollectionParam
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
      rules: {
        deviceId: [{ required: true, message: 'Please enter the device number', trigger: 'blur' }]
      },
      isLoading: false,
      activeTab: 'communication'
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
    onSubmit: function(data) {
      this.$store.dispatch('jtDevice/setConfig', {
        phoneNumber: this.phoneNumber,
        config: data
      })
        .then((data) => {
          this.$message.success({
            showClose: true,
            message: 'Saved successfully'
          })
        })
    },
    showDevice: function() {
      this.$emit('show-device')
    }
  }
}
</script>
