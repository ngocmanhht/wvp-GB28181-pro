<template>
  <div id="addUser" v-loading="isLoging">
    <el-dialog
      v-el-drag-dialog
      title="Add user"
      width="40%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div id="shared" style="margin-right: 20px;">
        <el-form ref="passwordForm" :rules="rules" status-icon label-width="80px">
          <el-form-item label="Username" prop="username">
            <el-input v-model="username" autocomplete="off" />
          </el-form-item>
          <el-form-item label="User type" prop="roleId">
            <el-select v-model="roleId" placeholder="Please select" style="width: 100%">
              <el-option
                v-for="item in options"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="Password" prop="password">
            <el-input v-model="password" autocomplete="off" />
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
  name: 'AddUser',
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
      } else if (this.confirmPassword !== this.password) {
        callback(new Error('The password entered twice is inconsistent!'))
      } else {
        callback()
      }
    }
    return {
      value: '',
      options: [],
      loading: false,
      username: null,
      password: null,
      roleId: null,
      confirmPassword: null,
      listChangeCallback: null,
      showDialog: false,
      isLoging: false,
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
  created() {
    this.getAllRole()
  },
  methods: {
    openDialog: function(callback) {
      this.listChangeCallback = callback
      this.showDialog = true
    },
    onSubmit: function() {
      this.$store.dispatch('user/add', {
        username: this.username,
        password: this.password,
        roleId: this.roleId
      })
        .then(data => {
          this.$message({
            showClose: true,
            message: 'Added successfully',
            type: 'success'
          })
          this.showDialog = false
          this.listChangeCallback()
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
    },
    close: function() {
      this.showDialog = false
      this.password = null
      this.confirmPassword = null
      this.username = null
      this.roleId = null
    },
    getAllRole: function() {
      this.loading = true
      this.$store.dispatch('role/getAll').then(data => {
        this.loading = false
        this.options = data
      })
    }
  }
}
</script>
