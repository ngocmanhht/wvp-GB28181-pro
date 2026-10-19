<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item  label="Channel 1 acquisition time interval(milliseconds)" prop="canCollectionTimeForChannel1">
          <el-input v-model="form.canCollectionTimeForChannel1" placeholder="Channel 1 acquisition time interval, unit is milliseconds(ms), 0Indicates no collection"/>
        </el-form-item>
        <el-form-item  label="Channel 1 upload time interval(seconds)" prop="canUploadIntervalForChannel1">
          <el-input v-model="form.canUploadIntervalForChannel1" placeholder="Channel 1 upload time interval, unit is seconds(s), 0Indicates not uploading"/>
        </el-form-item>
        <el-form-item  label="Channel 2 acquisition time interval(milliseconds)" prop="canCollectionTimeForChannel2">
          <el-input v-model="form.canCollectionTimeForChannel2" placeholder="Channel 2 acquisition time interval, unit is milliseconds(ms), 0Indicates no collection"/>
        </el-form-item>
        <el-form-item  label="Channel 2 upload time interval(seconds)" prop="canUploadIntervalForChannel2">
          <el-input v-model="form.canUploadIntervalForChannel2" placeholder="Channel 2 upload time interval, unit is seconds(s), 0Indicates not uploading"/>
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
