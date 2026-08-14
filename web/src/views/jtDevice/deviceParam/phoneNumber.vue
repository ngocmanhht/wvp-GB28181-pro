<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="Monitoring platform phone number" prop="platformPhoneNumber">
          <el-input v-model="form.platformPhoneNumber" clearable />
        </el-form-item>
        <el-form-item label="Reset phone number" prop="phoneNumberForFactoryReset">
          <el-input v-model="form.phoneNumberForFactoryReset" clearable />
        </el-form-item>
        <el-form-item label="Monitoring platform SMS phone number" prop="phoneNumberForSms">
          <el-input v-model="form.phoneNumberForSms" clearable />
        </el-form-item>
        <el-form-item label="Receive terminal SMS text alarm number" prop="phoneNumberForReceiveTextAlarm">
          <el-input v-model="form.phoneNumberForReceiveTextAlarm" clearable />
        </el-form-item>
        <el-form-item label="Terminal call answering strategy" prop="locationReportingStrategy">
          <el-select v-model="form.locationReportingStrategy" style="float: left; width: 100%">
            <el-option label="Automatic answer" :value="0">Report regularly</el-option>
            <el-option label="ACC ONAnswer automatically when OFF, answer manually when OFF" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="Maximum call time per call(seconds)" prop="longestCallTimeForPerSession">
          <el-input v-model="form.longestCallTimeForPerSession" clearable />
        </el-form-item>
        <el-form-item label="Longest call time in the month(seconds)" prop="longestCallTimeInMonth">
          <el-input v-model="form.longestCallTimeInMonth" clearable />
        </el-form-item>
        <el-form-item label="Monitor phone number" prop="phoneNumbersForListen">
          <el-input v-model="form.phoneNumbersForListen" clearable />
        </el-form-item>
        <el-form-item label="Supervision platform privileged SMS number" prop="privilegedSMSNumber">
          <el-input v-model="form.privilegedSMSNumber" clearable />
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
