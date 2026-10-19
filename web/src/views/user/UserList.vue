<template>
  <div class="flex flex-col gap-4 select-none">
    <div class="p-4 bg-slate-900 border border-slate-800 rounded-xl flex items-center justify-between shadow">
      <div class="flex items-center gap-2">
        <span class="w-2.5 h-2.5 rounded-full bg-blue-500"></span>
        <span class="font-semibold text-sm text-white">System Users & Authentication</span>
      </div>
      <div class="flex items-center gap-2">
        <el-button type="primary" size="default" @click="openAddModal" class="!rounded-lg">
          <el-icon class="mr-1"><Plus /></el-icon> Add User
        </el-button>
        <el-button size="default" @click="loadUsers" class="!rounded-lg !bg-slate-800 !text-slate-200 !border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Data Table -->
    <div class="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow overflow-hidden">
      <el-table :data="userList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="Username" min-width="140">
          <template #default="{ row }">
            <span class="font-bold text-slate-200">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="Role" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.role?.id === 1 ? 'danger' : 'info'">
              {{ row.role?.name || 'User' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pushKey" label="Stream Push Key" min-width="200">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-400">{{ row.pushKey || 'None' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="180" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1.5">
              <el-button size="small" type="primary" plain @click="openPasswordModal(row)">Password</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)" :disabled="row.id === 1">Delete</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Add User Modal -->
    <el-dialog v-model="addModalVisible" title="Create New User" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="py-2">
        <el-form-item label="Username" prop="username">
          <el-input v-model="form.username" placeholder="Username" />
        </el-form-item>
        <el-form-item label="Password" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="Password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="addModalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="submitAddUser">Save</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Change Password Modal -->
    <el-dialog v-model="pwdModalVisible" title="Change Password" width="460px" destroy-on-close>
      <el-form label-width="120px" class="py-2">
        <el-form-item label="New Password">
          <el-input v-model="newPassword" type="password" show-password placeholder="Enter new password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="pwdModalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="submitChangePassword">Update</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { userService } from '@/api'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const userList = ref<any[]>([])

const addModalVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  password: '',
  roleId: 2,
})

const rules = {
  username: [{ required: true, message: 'Please input username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please input password', trigger: 'blur' }],
}

const pwdModalVisible = ref(false)
const activeUser = ref<any>(null)
const newPassword = ref('')

async function loadUsers() {
  loading.value = true
  try {
    const res: any = await userService.getUserList({ page: 1, count: 50 })
    if (res && res.data) {
      userList.value = res.data.list || []
    }
  } catch (e) {
    console.error('Failed to load users:', e)
  } finally {
    loading.value = false
  }
}

function openAddModal() {
  form.username = ''
  form.password = ''
  addModalVisible.value = true
}

async function submitAddUser() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      await userService.addUser(form)
      ElMessage.success('User created successfully')
      addModalVisible.value = false
      loadUsers()
    }
  })
}

function openPasswordModal(row: any) {
  activeUser.value = row
  newPassword.value = ''
  pwdModalVisible.value = true
}

async function submitChangePassword() {
  if (!newPassword.value) {
    ElMessage.warning('Please input new password')
    return
  }
  await userService.changePassword({ password: newPassword.value })
  ElMessage.success('Password updated')
  pwdModalVisible.value = false
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`Delete user ${row.username}?`, 'Confirm Delete', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning'
  }).then(async () => {
    await userService.deleteUser(row.id)
    ElMessage.success('User deleted')
    loadUsers()
  })
}

onMounted(() => {
  loadUsers()
})
</script>
