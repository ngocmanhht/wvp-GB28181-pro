<template>
  <div id="addUserApiKey" v-loading="isLoading">
    <el-dialog
      v-el-drag-dialog
      title="addApiKey"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 20px;">
        <el-form ref="formRef" :model="form" :rules="rules" status-icon label-width="80px">
          <el-form-item label="Application name" prop="app">
            <el-input
              v-model="form.app"
              property="app"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="Enabled status" prop="enable" style="text-align: left">
            <el-switch
              v-model="form.enable"
              property="enable"
              active-text="enable"
              inactive-text="deactivate"
            />
          </el-form-item>
          <el-form-item label="Expiration time" prop="expiresAt" style="text-align: left">
            <el-date-picker
              v-model="form.expiresAt"
              style="width: 100%"
              property="expiresAt"
              type="datetime"
              value-format="yyyy-MM-dd HH:mm:ss"
              format="yyyy-MM-dd HH:mm:ss"
              placeholder="Select expiration time"
            />
          </el-form-item>
          <el-form-item label="Remarks" prop="remark">
            <el-input
              v-model="form.remark"
              type="textarea"
              property="remark"
              autocomplete="off"
              :autosize="{ minRows: 5}"
              maxlength="255"
              show-word-limit
            />
          </el-form-item>
          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" @click="onSubmit">save</el-button>
              <el-button @click="close">Cancel</el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'AddUserApiKey',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      userId: null,
      form: {
        app: null,
        enable: true,
        expiresAt: null,
        remark: null
      },
      rules: {
        app: [{ required: true, trigger: 'blur', message: 'Application name cannot be empty' }]
      },
      listChangeCallback: null,
      showDialog: false,
      isLoading: false
    }
  },
  computed: {},
  created() {
  },
  methods: {
    resetForm() {
      this.form = {
        app: null,
        enable: true,
        expiresAt: null,
        remark: null
      }
    },
    openDialog(userId, callback) {
      this.resetForm()
      this.userId = userId
      this.listChangeCallback = callback
      this.showDialog = true
    },
    onSubmit() {
      this.$refs.formRef.validate((valid) => {
        if (valid) {
          this.$store.dispatch('userApiKeys/add', {
            userId: this.userId,
            app: this.form.app,
            enable: this.form.enable,
            expiresAt: this.form.expiresAt,
            remark: this.form.remark
          })
            .then(data => {
              this.$message({
                showClose: true,
                message: 'Added successfully',
                type: 'success'
              })
              this.showDialog = false
              if (this.listChangeCallback) {
                this.listChangeCallback()
              }
            })
            .catch((error) => {
              console.error(error)
            })
        }
      })
    },
    close() {
      this.showDialog = false
    }
  }
}
</script>
