<template>
  <div style="width: 100%;">
    <el-checkbox label="Video signal loss alarm" v-model="form.lossSignal" @change="change"></el-checkbox>
    <el-checkbox label="Video signal blocking alarm" v-model="form.occlusionSignal" @change="change"></el-checkbox>
    <el-checkbox label="Storage unit failure alarm" v-model="form.storageFault" @change="change"></el-checkbox>
    <el-checkbox label="Other video equipment failure alarms" v-model="form.otherDeviceFailure" @change="change"></el-checkbox>
    <el-checkbox label="Bus overcrowding alarm" v-model="form.overcrowding" @change="change"></el-checkbox>
    <el-checkbox label="Abnormal driving behavior alarm" v-model="form.abnormalDriving" @change="change"></el-checkbox>
    <el-checkbox label="Special alarm: The recording reaches the storage threshold alarm." v-model="form.storageLimit" @change="change"></el-checkbox>
  </div>
</template>

<script>

export default {
  name: 'communication',
  components: {
  },
  model: {
    prop: 'fatherValue',
    event: 'change'
  },
  props: {
    fatherValue: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      form: {
        lossSignal: false,
        occlusionSignal: false,
        storageFault: false,
        otherDeviceFailure: false,
        overcrowding: false,
        abnormalDriving: false,
        storageLimit: false
      },
      isLoading: false
    }
  },

  mounted() {
    if (this.fatherValue !== null) {
      console.log(this.fatherValue)
      this.form.lossSignal = this.fatherValue.lossSignal || false
      this.form.occlusionSignal = this.fatherValue.occlusionSignal || false
      this.form.storageFault = this.fatherValue.storageFault || false
      this.form.otherDeviceFailure = this.fatherValue.otherDeviceFailure || false
      this.form.overcrowding = this.fatherValue.overcrowding || false
      this.form.abnormalDriving = this.fatherValue.abnormalDriving || false
      this.form.storageLimit = this.fatherValue.storageLimit || false
    }
  },
  methods: {
    change() {
      this.$emit('change', this.form)
    }
  }
}
</script>
