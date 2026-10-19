<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="baud rate" prop="gnssBaudRate">
          <el-select
            v-model="form.gnssBaudRate"
            style="width: 100%"
            placeholder="Please select a baud rate"
          >
            <el-option label="4800" :value="0" />
            <el-option label="19200" :value="1" />
            <el-option label="38400" :value="2" />
            <el-option label="57600" :value="3" />
            <el-option label="115200" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="Output frequency" prop="gnssOutputFrequency">
          <el-select
            v-model="form.gnssOutputFrequency"
            style="width: 100%"
            placeholder="Please select output frequency"
          >
            <el-option label="500ms" :value="0" />
            <el-option label="1000ms" :value="1" />
            <el-option label="2000ms" :value="2" />
            <el-option label="3000ms" :value="3" />
            <el-option label="4000ms" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="Collection frequency(seconds)" prop="gnssCollectionFrequency">
          <el-input v-model="form.gnssCollectionFrequency" />
        </el-form-item>
        <el-form-item label="Upload method" prop="gnssDataUploadMethod">
          <el-select
            v-model="form.gnssDataUploadMethod"
            style="width: 100%"
            placeholder="Please select upload method"
          >
            <el-option label="Local storage, no upload" :value="0" />
            <el-option label="Upload by time interval" :value="1" />
            <el-option label="Upload by distance interval" :value="2" />
            <el-option label="Upload according to the cumulative time, and automatically stop uploading after the transmission time is reached" :value="11" />
            <el-option label="Upload according to the cumulative distance, and automatically stop uploading after reaching the distance" :value="12" />
            <el-option label="Upload based on the cumulative number of items, and automatically stop uploading when the number of uploads is reached." :value="13" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.gnssDataUploadMethod > 0" :label="gnssDataUploadMethodUnitLable" prop="gnssDataUploadMethodUnit">
          <el-input v-model="form.gnssDataUploadMethodUnit" />
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
  components: {},
  computed: {
    gnssDataUploadMethodUnitLable(){
      switch (this.form.gnssDataUploadMethod) {
        case 1:
        case 11:
          return 'Upload settings (seconds）'
        case 2:
        case 12:
          return 'Upload settings (m）'
        case 13:
          return 'Upload settings (bar）'
        default:
          return 'Upload settings'
      }
    }
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
