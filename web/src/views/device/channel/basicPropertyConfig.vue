<template>
  <div id="dhBasicPropertyPage">
    <div class="basic-property-body">
      <div class="config-section">
        <el-form ref="formRef" :model="form" label-width="140px">
          <el-form-item label="Device name" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="Registration expiration time(seconds)" prop="expiration">
            <el-input-number v-model="form.expiration" :min="60" :max="86400" />
          </el-form-item>
          <el-form-item label="heartbeat interval(seconds)" prop="heartBeatInterval">
            <el-input-number v-model="form.heartBeatInterval" :min="5" :max="3600" />
          </el-form-item>
          <el-form-item label="Number of heartbeat timeouts" prop="heartBeatCount">
            <el-input-number v-model="form.heartBeatCount" :min="1" :max="60" />
          </el-form-item>
          <el-form-item label="Positioning function" prop="positionCapability">
            <el-select v-model="form.positionCapability">
              <el-option :value="0" label="Not supported" />
              <el-option :value="1" label="GPSPositioning" />
              <el-option :value="2" label="Beidou positioning" />
            </el-select>
          </el-form-item>
          <el-form-item label="longitude" v-if="form.longitude !== null">
            <span class="readonly-field">{{ form.longitude }}</span>
          </el-form-item>
          <el-form-item label="Latitude" v-if="form.latitude !== null">
            <span class="readonly-field">{{ form.latitude }}</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">save</el-button>
            <el-button :loading="loading" @click="handleRefresh">Refresh</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BasicPropertyPage',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      loading: false,
      saving: false,
      form: {
        deviceId: this.deviceId,
        channelId: this.channelDeviceId,
        name: '',
        expiration: 3600,
        heartBeatInterval: 60,
        heartBeatCount: 3,
        positionCapability: 0,
        longitude: null,
        latitude: null
      }
    }
  },
  mounted() {
    this.loadConfig()
  },
  methods: {
    loadConfig() {
      this.loading = true
      this.$store.dispatch('device/queryBasicParam', {
        deviceId: this.deviceId,
        channelId: this.channelDeviceId
      })
        .then(data => {
          this.form = { ...this.form, ...data }
        }).finally(() => {
          this.loading = false
        })
    },
    handleSave() {
      this.saving = true
      this.$store.dispatch('device/setBasicParam', this.form)
        .then(() => {
          this.$message({ showClose: true, message: 'Saved successfully', type: 'success' })
        })
        .catch((error) => {
          this.$message({ showClose: true, message: error, type: 'error' })
        })
        .finally(() => {
          this.saving = false
        })
    },
    handleRefresh() {
      this.loadConfig()
    }
  }
}
</script>

<style scoped>
#dhBasicPropertyPage {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.basic-property-body {
  display: flex;
  gap: 16px;
  min-height: 30vw;
  flex: 1;
  padding-top: 16px;
}

.config-section {
  flex: 1;
  min-width: 0;
  max-width: 500px;
}

.config-section .el-input,
.config-section .el-input-number,
.config-section .el-select {
  width: 100%;
}

.readonly-field {
  color: #606266;
  line-height: 32px;
}
</style>
