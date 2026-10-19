<template>
  <div id="changePassword" v-loading="isLoging">
    <el-dialog
      title="Change password"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 20px;">
        <el-form ref="passwordForm" :rules="rules" status-icon label-width="80px">
          <el-form-item label="old password" prop="oldPassword">
            <el-input v-model="oldPassword" autocomplete="off" />
          </el-form-item>
          <el-form-item label="new password" prop="newPassword">
            <el-input v-model="newPassword" autocomplete="off" />
          </el-form-item>
          <el-form-item label="Confirm password" prop="confirmPassword">
            <el-input v-model="confirmPassword" autocomplete="off" />
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
import crypto from 'crypto'
export default {
  name: 'ChangePassword',
  props: {},
  data() {
    const validatePass0 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('Please enter old password'))
      } else {
        callback()
      }
    }
    const validatePass1 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('Please enter new password'))
      } else {
        if (this.confirmPassword !== '') {
          this.$refs.passwordForm.validateField('confirmPassword')
        }
        callback()
      }
    }
    const validatePass2 = (rule, value, callback) => {
      if (this.confirmPassword === '') {
        callback(new Error('Please enter password again'))
      } else if (this.confirmPassword !== this.newPassword) {
        callback(new Error('The password entered twice is inconsistent!'))
      } else {
        callback()
      }
    }
    return {
      oldPassword: null,
      newPassword: null,
      confirmPassword: null,
      showDialog: false,
      callback: null,
      isLoging: false,
      rules: {
        oldPassword: [{ required: true, validator: validatePass0, trigger: 'blur' }],
        newPassword: [{ required: true, validator: validatePass1, trigger: 'blur' }, {
          pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,.\/]).{8,20}$/,
          message: 'The password length is8-20between digits, by letters+numbers+Special characters'
        }],
        confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }]
      }
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(callback) {
      this.showDialog = true
      this.callback = callback
    },
    onSubmit: function() {
      this.$store.dispatch('user/changePassword', {
        oldPassword: crypto.createHash('md5').update(this.oldPassword, 'utf8').digest('hex'),
        password: this.newPassword
      })
        .then((data) => {
          this.$message({
            showClose: true,
            message: 'Modification successful, please log in again',
            type: 'success'
          })
          this.showDialog = false
          if (this.callback) {
            this.callback()
          }
        }).catch((error) => {
          console.error(error)
        })
    },
    close: function() {
      this.showDialog = false
      this.oldPassword = null
      this.newPassword = null
      this.confirmPassword = null
    }
  }
}
</script>
