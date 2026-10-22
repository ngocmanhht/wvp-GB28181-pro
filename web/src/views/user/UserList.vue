<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Header -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex items-center justify-between shadow transition-colors">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg bg-blue-500/10 text-blue-600 dark:text-blue-400 flex items-center justify-center font-bold">
          <el-icon><User /></el-icon>
        </div>
        <div>
          <h2 class="text-sm font-bold text-slate-800 dark:text-white">System Users & API Key Authentication</h2>
          <p class="text-xs text-slate-500 dark:text-slate-400">Manage user credentials, push keys and third-party API integration tokens</p>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <el-button type="primary" size="default" @click="openAddModal" class="!rounded-lg">
          <el-icon class="mr-1"><Plus /></el-icon> Add User
        </el-button>
        <el-button size="default" @click="loadUsers" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- User Data Table -->
    <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 shadow overflow-hidden transition-colors">
      <el-table :data="userList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="Username" min-width="140">
          <template #default="{ row }">
            <span class="font-bold text-slate-800 dark:text-slate-200">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="Role" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.role?.id === 1 ? 'danger' : 'info'">
              {{ row.role?.name || (row.roleId === 1 ? 'Administrator' : 'User') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pushKey" label="Stream Push Key" min-width="200">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-500 dark:text-slate-400">{{ row.pushKey || 'None' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="310" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-end gap-1.5">
              <el-button size="small" type="primary" plain @click="openApiKeyModal(row)">API Keys</el-button>
              <el-button size="small" type="warning" plain @click="openPasswordModal(row)">Password</el-button>
              <el-button size="small" type="info" plain @click="openPushKeyModal(row)">Push Key</el-button>
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
        <el-form-item label="Role">
          <el-select v-model="form.roleId" class="w-full">
            <el-option label="Administrator" :value="1" />
            <el-option label="Standard User" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="addModalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="submitAddUser">Save</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Admin Reset Password Modal -->
    <el-dialog v-model="pwdModalVisible" :title="`Reset Password for: ${activeUser?.username}`" width="460px" destroy-on-close>
      <el-form label-width="120px" class="py-2">
        <el-form-item label="New Password">
          <el-input v-model="newPassword" type="password" show-password placeholder="Enter new user password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="pwdModalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="submitChangePassword">Update Password</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Change Push Key Modal -->
    <el-dialog v-model="pushKeyModalVisible" :title="`Stream Push Key: ${activeUser?.username}`" width="460px" destroy-on-close>
      <el-form label-width="120px" class="py-2">
        <el-form-item label="New Push Key">
          <el-input v-model="newPushKey" placeholder="Enter secret push key" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="pushKeyModalVisible = false">Cancel</el-button>
          <el-button type="primary" @click="submitChangePushKey">Save Key</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- API Key Manager Modal -->
    <el-dialog v-model="apiKeyModalVisible" :title="`Manage API Keys: ${activeUser?.username}`" width="750px" destroy-on-close>
      <div class="flex flex-col gap-3 max-h-[500px]">
        <div class="flex items-center justify-between text-xs">
          <span class="text-slate-500">API keys used for third-party application access tokens:</span>
          <el-button size="small" type="primary" @click="generateNewApiKey">Generate New Key</el-button>
        </div>

        <el-table :data="apiKeyList" v-loading="apiKeyLoading" size="small">
          <el-table-column prop="apiKey" label="API Key Token" min-width="220">
            <template #default="{ row }">
              <span class="font-mono text-xs text-blue-600 dark:text-blue-400 select-all">{{ row.apiKey }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="enable" label="Status" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enable ? 'success' : 'info'">
                {{ row.enable ? 'Active' : 'Disabled' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="180" align="right">
            <template #default="{ row }">
              <div class="flex items-center justify-end gap-1">
                <el-button v-if="!row.enable" size="small" type="success" link @click="toggleApiKey(row, true)">Enable</el-button>
                <el-button v-else size="small" type="warning" link @click="toggleApiKey(row, false)">Disable</el-button>
                <el-button size="small" type="danger" link @click="deleteApiKey(row)">Delete</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { userService, userApiKeyService } from '@/api'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import { User, Plus, Refresh } from '@element-plus/icons-vue'

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

const pushKeyModalVisible = ref(false)
const newPushKey = ref('')

// API Key Modal State
const apiKeyModalVisible = ref(false)
const apiKeyLoading = ref(false)
const apiKeyList = ref<any[]>([])

async function loadUsers() {
  loading.value = true
  try {
    const res: any = await userService.getUserList({ page: 1, count: 50 })
    if (res && res.data) {
      userList.value = res.data.list || []
    }
  } catch (e) {
    console.warn('Failed to load users:', e)
  } finally {
    loading.value = false
  }
}

function openAddModal() {
  form.username = ''
  form.password = ''
  form.roleId = 2
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
  try {
    await userService.changePasswordForAdmin({
      userId: activeUser.value.id,
      password: newPassword.value,
    })
    ElMessage.success(`Password updated for user ${activeUser.value.username}`)
    pwdModalVisible.value = false
  } catch (e: any) {
    ElMessage.error(e.message || 'Failed to update password')
  }
}

function openPushKeyModal(row: any) {
  activeUser.value = row
  newPushKey.value = row.pushKey || ''
  pushKeyModalVisible.value = true
}

async function submitChangePushKey() {
  try {
    await userService.changePushKey({
      userId: activeUser.value.id,
      pushKey: newPushKey.value,
    })
    ElMessage.success('Push key updated successfully')
    pushKeyModalVisible.value = false
    loadUsers()
  } catch (e: any) {
    ElMessage.error(e.message || 'Failed to update push key')
  }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`Delete user ${row.username}?`, 'Confirm Delete', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(async () => {
    await userService.deleteUser(row.id)
    ElMessage.success('User deleted')
    loadUsers()
  })
}

async function openApiKeyModal(row: any) {
  activeUser.value = row
  apiKeyModalVisible.value = true
  apiKeyLoading.value = true
  try {
    const res: any = await userApiKeyService.getApiKeyList({ page: 1, count: 50, userId: row.id })
    if (res && res.data) {
      apiKeyList.value = res.data.list || []
    }
  } catch (e) {
    console.warn('Failed to load api keys:', e)
  } finally {
    apiKeyLoading.value = false
  }
}

async function generateNewApiKey() {
  try {
    await userApiKeyService.addApiKey({
      userId: activeUser.value.id,
      app: 'wvp',
      enable: true,
      remark: 'Auto-generated Key',
    })
    ElMessage.success('New API Key created')
    openApiKeyModal(activeUser.value)
  } catch (e: any) {
    ElMessage.error('Failed to generate key')
  }
}

async function toggleApiKey(row: any, enable: boolean) {
  try {
    if (enable) {
      await userApiKeyService.enableApiKey(row.id)
    } else {
      await userApiKeyService.disableApiKey(row.id)
    }
    ElMessage.success(`API key ${enable ? 'enabled' : 'disabled'}`)
    openApiKeyModal(activeUser.value)
  } catch (e: any) {
    ElMessage.error('Action failed')
  }
}

async function deleteApiKey(row: any) {
  try {
    await userApiKeyService.deleteApiKey(row.id)
    ElMessage.success('API key deleted')
    openApiKeyModal(activeUser.value)
  } catch (e: any) {
    ElMessage.error('Failed to delete key')
  }
}

onMounted(() => {
  loadUsers()
})
</script>
