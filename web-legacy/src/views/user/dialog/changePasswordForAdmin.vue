<template>
  <div id="changePassword" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
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

import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ChangePasswordForAdmin',
  directives: { elDragDialog },
  props: {},
  data() {
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
      newPassword: null,
      confirmPassword: null,
      userId: null,
      showDialog: false,
      isLoging: false,
      listChangeCallback: null,
      form: {},
      rules: {
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
    openDialog: function(row, callback) {
      console.log(row)
      this.showDialog = true
      this.listChangeCallback = callback
      if (row != null) {
        this.form = row
      }
    },
    onSubmit: function() {
      this.$store.dispatch('user/changePasswordForAdmin', {
        password: this.newPassword,
        userId: this.form.id
      })
        .then(data => {
          this.$message({
            showClose: true,
            message: 'Modification successful',
            type: 'success'
          })
          this.showDialog = false
        })
        .catch((error) => {
          console.error(error)
        })
        .finally(() => {
          this.showDialog = false
        })
    },
    close: function() {
      this.showDialog = false
      this.newPassword = null
      this.confirmPassword = null
      this.userId = null
      this.adminId = null
    }
  }
}
</script>
