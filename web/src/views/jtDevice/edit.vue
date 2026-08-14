<template>
  <div id="deviceEdit" v-loading="isLoging">
    <el-dialog
      title="Device editing"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-top: 1rem;margin-right: 100px;">
        <el-form ref="form" :rules="rules" :model="form" label-width="200px">
          <el-form-item label="Terminal mobile phone number" prop="phoneNumber">
            <el-input v-model="form.phoneNumber" clearable />
          </el-form-item>
          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" @click="onSubmit">Confirm</el-button>
              <el-button @click="close">Cancel</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'DeviceEdit',
  props: {},
  data() {
    return {
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
      form: {},
      isEdit: false,
      rules: {
        deviceId: [{ required: true, message: 'Please enter the device number', trigger: 'blur' }]
      }
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(row, callback) {
      console.log(row)
      this.showDialog = true
      this.isEdit = false
      if (row) {
        this.isEdit = true
      }
      this.form = {}
      this.listChangeCallback = callback
      if (row != null) {
        this.form = row
      }
    },
    onSubmit: function() {
      console.log('onSubmit')
      if (this.isEdit) {
        this.$store.dispatch('jtDevice/update', this.form)
          .then(data => {
            this.listChangeCallback()
          })
          .catch((error) => {
            this.$message.error({
              showClose: true,
              message: error
            })
          })
      } else {
        this.$store.dispatch('jtDevice/add', this.form)
          .then(data => {
            this.listChangeCallback()
          })
          .catch((error) => {
            this.$message.error({
              showClose: true,
              message: error
            })
          })
      }
    },
    close: function() {
      this.showDialog = false
      this.$refs.form.resetFields()
    }
  }
}
</script>
