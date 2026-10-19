<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form v-loading="isLoading" ref="form" :model="form" label-width="240px" style="width: 90%; margin: 0 auto; ">
        <el-form-item label="Alarm mask word" prop="alarmMaskingWord">
          <alarmSign :fatherValue="form.alarmMaskingWord" @change="(data)=>{form.alarmMaskingWord = data}"></alarmSign>
        </el-form-item>
        <el-form-item label="Alarm send text SMS switch" prop="alarmSendsTextSmsSwitch">
          <alarmSign :fatherValue="form.alarmSendsTextSmsSwitch" @change="(data)=>{form.alarmSendsTextSmsSwitch = data}"></alarmSign>
        </el-form-item>
        <el-form-item label="Alarm shooting switch" prop="alarmShootingSwitch">
          <alarmSign :fatherValue="form.alarmShootingSwitch" @change="(data)=>{form.alarmShootingSwitch = data}"></alarmSign>
        </el-form-item>
        <el-form-item label="alarm shooting storage sign" prop="alarmShootingStorageFlags">
          <alarmSign :fatherValue="form.alarmShootingStorageFlags" @change="(data)=>{form.alarmShootingStorageFlags = data}"></alarmSign>
        </el-form-item>
        <el-form-item label="key sign" prop="keySign">
          <alarmSign :fatherValue="form.keySign" @change="(data)=>{form.keySign = data}"></alarmSign>
        </el-form-item>
        <el-form-item label="Video alarm masking words" prop="videoAlarmBit">
          <videoAlarmSign :fatherValue="form.videoAlarmBit" @change="(data)=>{form.videoAlarmBit = data}"></videoAlarmSign>
        </el-form-item>
        <el-form-item label="Image analysis alarm parameters-Number of people on board the vehicle" prop="numberForPeople">
          <el-input type="number" v-model="form.analyzeAlarmParam.numberForPeople" />
        </el-form-item>
        <el-form-item label="Image analysis alarm parameters-Fatigue level threshold" prop="fatigueThreshold">
          <el-input type="number" v-model="form.analyzeAlarmParam.fatigueThreshold" />
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
import alarmSign from './alarmSign.vue'
import videoAlarmSign from './videoAlarmSign.vue'

export default {
  name: 'communication',
  components: {
    alarmSign, videoAlarmSign
  },
  props: {
    phoneNumber: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      form: {
        alarmMaskingWord: null,
        alarmSendsTextSmsSwitch: null,
        alarmShootingSwitch: null,
        alarmShootingStorageFlags: null,
        keySign: null,
        videoAlarmBit: null,
        analyzeAlarmParam: null
      },
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
          if (!data.alarmMaskingWord) {
            data.alarmMaskingWord = {}
          }
          if (!data.alarmSendsTextSmsSwitch) {
            data.alarmSendsTextSmsSwitch = {}
          }
          if (!data.alarmShootingSwitch) {
            data.alarmShootingSwitch = {}
          }
          if (!data.alarmShootingStorageFlags) {
            data.alarmShootingStorageFlags = {}
          }
          if (!data.keySign) {
            data.keySign = {}
          }
          if (!data.videoAlarmBit) {
            data.videoAlarmBit = {}
          }
          if (!data.analyzeAlarmParam) {
            data.analyzeAlarmParam = {}
          }
          this.form = data
          // this.form.alarmMaskingWord = data.alarmMaskingWord
          // this.form.alarmSendsTextSmsSwitch = data.alarmSendsTextSmsSwitch
          // this.form.alarmShootingSwitch = data.alarmShootingSwitch
          // this.form.alarmShootingStorageFlags = data.alarmShootingStorageFlags
          // this.form.keySign = data.keySign
          // this.$forceUpdate()
        })
        .catch((e) => {
          console.log(e)
        })
        .finally(() => {
          this.isLoading = false
        })
    },
    formChange: function(data) {
      this.form = data
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
