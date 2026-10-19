<template>
  <div style="width: 100%;">
    <div style="height: calc(100vh - 260px); overflow: auto">
      <el-form ref="form" :model="form" label-width="240px" style="width: 50%; margin: 0 auto">
        <el-form-item label="odometer reading(1/10km)" prop="mileage">
          <el-input type="number" v-model="form.mileage" />
        </el-form-item>
        <el-form-item label="Provincial areaID" prop="provincialId">
          <el-input v-model="form.provincialId" />
        </el-form-item>
        <el-form-item label="city areaID" prop="cityId">
          <el-input v-model="form.cityId" />
        </el-form-item>
        <el-form-item label="Motor vehicle license plate" prop="licensePlate">
          <el-input v-model="form.licensePlate" />
        </el-form-item>
        <el-form-item label="license plate color" prop="licensePlateColor">
          <el-select
            v-model="form.licensePlateColor"
            style="width: 100%"
            placeholder="Please select license plate color"
          >
            <el-option label="Not listed" :value="0" />
            <el-option label="blue" :value="1" />
            <el-option label="yellow" :value="2" />
            <el-option label="black" :value="3" />
            <el-option label="white" :value="4" />
            <el-option label="green" :value="5" />
            <el-option label="Farm yellow" :value="91" />
            <el-option label="Farm green" :value="92" />
            <el-option label="Yellow-green" :value="93" />
            <el-option label="gradient green" :value="94" />
          </el-select>
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
