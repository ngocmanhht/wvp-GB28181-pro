<template>
  <div class="flex flex-col gap-4 select-none">
    <!-- Header -->
    <div class="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex items-center justify-between shadow transition-colors">
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg bg-purple-500/10 text-purple-600 dark:text-purple-400 flex items-center justify-center font-bold">
          <el-icon><Coin /></el-icon>
        </div>
        <div>
          <h2 class="text-sm font-bold text-slate-800 dark:text-white">ZLMediaKit Media Server Nodes</h2>
          <p class="text-xs text-slate-500 dark:text-slate-400">Manage high-performance media clusters, streaming ports and WebHook integrations</p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <el-button type="primary" size="default" @click="openAddModal" class="!rounded-lg">
          <el-icon class="mr-1"><Plus /></el-icon> Add Media Node
        </el-button>
        <el-button size="default" @click="loadServers" class="!rounded-lg bg-slate-50 dark:bg-slate-800 text-slate-700 dark:text-slate-200 border-slate-200 dark:border-slate-700">
          <el-icon class="mr-1"><Refresh /></el-icon> Refresh
        </el-button>
      </div>
    </div>

    <!-- Media Server Cards Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4" v-loading="loading">
      <div
        v-for="node in serverList"
        :key="node.id"
        class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 flex flex-col gap-4 shadow hover:border-blue-400 dark:hover:border-slate-700 transition"
      >
        <div class="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-3">
          <div class="flex items-center gap-2.5">
            <div class="w-9 h-9 rounded-lg bg-purple-500/10 text-purple-600 dark:text-purple-400 flex items-center justify-center font-bold">
              <el-icon><Coin /></el-icon>
            </div>
            <div class="flex flex-col">
              <span class="font-bold text-sm text-slate-800 dark:text-slate-100">{{ node.id || 'Default Node' }}</span>
              <span class="text-xs text-slate-500 dark:text-slate-400 font-mono">{{ node.ip }}:{{ node.httpPort }}</span>
            </div>
          </div>

          <span
            class="px-2.5 py-0.5 rounded-full text-xs font-semibold"
            :class="node.status ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20' : 'bg-rose-500/10 text-rose-600 dark:text-rose-400'"
          >
            {{ node.status ? 'Online' : 'Offline' }}
          </span>
        </div>

        <div class="grid grid-cols-2 gap-2 text-xs">
          <div class="p-2 rounded bg-slate-50 dark:bg-slate-950/60 border border-slate-100 dark:border-slate-800 flex flex-col">
            <span class="text-slate-400">RTSP Port</span>
            <span class="font-mono text-slate-800 dark:text-slate-200 font-semibold">{{ node.rtspPort || 554 }}</span>
          </div>
          <div class="p-2 rounded bg-slate-50 dark:bg-slate-950/60 border border-slate-100 dark:border-slate-800 flex flex-col">
            <span class="text-slate-400">RTMP Port</span>
            <span class="font-mono text-slate-800 dark:text-slate-200 font-semibold">{{ node.rtmpPort || 1935 }}</span>
          </div>
          <div class="p-2 rounded bg-slate-50 dark:bg-slate-950/60 border border-slate-100 dark:border-slate-800 flex flex-col">
            <span class="text-slate-400">RTP Port Range</span>
            <span class="font-mono text-slate-800 dark:text-slate-200 font-semibold">{{ node.rtpPortRange || '30000-30500' }}</span>
          </div>
          <div class="p-2 rounded bg-slate-50 dark:bg-slate-950/60 border border-slate-100 dark:border-slate-800 flex flex-col">
            <span class="text-slate-400">Auto Config</span>
            <span class="font-mono text-slate-800 dark:text-slate-200 font-semibold">{{ node.autoConfig ? 'Enabled' : 'Disabled' }}</span>
          </div>
        </div>

        <!-- Node Actions -->
        <div class="flex items-center justify-between border-t border-slate-200 dark:border-slate-800 pt-3">
          <el-button size="small" type="success" plain @click="testConnection(node)">Test Ping</el-button>
          <div class="flex items-center gap-1.5">
            <el-button size="small" type="warning" plain @click="openEditModal(node)">Edit</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(node)" :disabled="node.defaultServer">Delete</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Media Server Modal -->
    <el-dialog v-model="modalVisible" :title="isEdit ? 'Edit Media Node' : 'Add ZLMediaKit Node'" width="650px" destroy-on-close>
      <el-form label-width="140px" class="py-2 text-xs">
        <el-form-item label="Node ID" required>
          <el-input v-model="form.id" placeholder="e.g. zlm_node_01" :disabled="isEdit" />
        </el-form-item>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="Server IP" required label-width="140px">
            <el-input v-model="form.ip" placeholder="127.0.0.1" />
          </el-form-item>
          <el-form-item label="HTTP Port" required label-width="100px">
            <el-input-number v-model="form.httpPort" :min="1" :max="65535" class="!w-full" />
          </el-form-item>
        </div>
        <div class="grid grid-cols-2 gap-2">
          <el-form-item label="RTSP Port" label-width="140px">
            <el-input-number v-model="form.rtspPort" :min="1" :max="65535" class="!w-full" />
          </el-form-item>
          <el-form-item label="RTMP Port" label-width="100px">
            <el-input-number v-model="form.rtmpPort" :min="1" :max="65535" class="!w-full" />
          </el-form-item>
        </div>
        <el-form-item label="RTP Port Range">
          <el-input v-model="form.rtpPortRange" placeholder="30000,30500" />
        </el-form-item>
        <el-form-item label="API Secret" required>
          <el-input v-model="form.secret" type="password" show-password placeholder="ZLM MD5 secret key" />
        </el-form-item>
        <el-form-item label="WebHook IP">
          <el-input v-model="form.hookIp" placeholder="WVP IP reachable by ZLM" />
        </el-form-item>
        <el-form-item label="Auto Configure">
          <el-switch v-model="form.autoConfig" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-between items-center">
          <el-button type="success" plain @click="testConnection(form)">Check ZLM Connection</el-button>
          <div class="flex gap-2">
            <el-button @click="modalVisible = false">Cancel</el-button>
            <el-button type="primary" @click="submitNode">Save Node</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { systemService } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Coin, Plus, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const serverList = ref<any[]>([])

const modalVisible = ref(false)
const isEdit = ref(false)
const form = reactive({
  id: '',
  ip: '127.0.0.1',
  httpPort: 18080,
  rtspPort: 554,
  rtmpPort: 1935,
  rtpPortRange: '30000,30500',
  secret: '035c73f7-bb6b-4889-a715-d9eb2d1925cc',
  hookIp: '127.0.0.1',
  autoConfig: true,
})

async function loadServers() {
  loading.value = true
  try {
    const res: any = await systemService.getMediaServerList()
    if (res && res.data) {
      serverList.value = res.data || []
    }
  } catch (e) {
    console.warn('Failed to load media servers:', e)
  } finally {
    loading.value = false
  }
}

function openAddModal() {
  isEdit.value = false
  form.id = `zlm_node_${Date.now().toString().slice(-4)}`
  form.ip = '127.0.0.1'
  form.httpPort = 18080
  form.rtspPort = 554
  form.rtmpPort = 1935
  form.rtpPortRange = '30000,30500'
  form.secret = '035c73f7-bb6b-4889-a715-d9eb2d1925cc'
  form.hookIp = '127.0.0.1'
  form.autoConfig = true
  modalVisible.value = true
}

function openEditModal(row: any) {
  isEdit.value = true
  Object.assign(form, row)
  modalVisible.value = true
}

async function testConnection(node: any) {
  try {
    const res: any = await systemService.checkMediaServer({
      ip: node.ip,
      httpPort: node.httpPort,
      secret: node.secret,
    })
    if (res && res.code === 0) {
      ElMessage.success('ZLMediaKit node connection verified successfully!')
    } else {
      ElMessage.warning('Connection test completed with warning')
    }
  } catch (e: any) {
    ElMessage.error('Unable to connect to ZLMediaKit node')
  }
}

async function submitNode() {
  if (!form.id || !form.ip || !form.httpPort) {
    ElMessage.warning('Please fill in required fields')
    return
  }
  try {
    await systemService.saveMediaServer(form)
    ElMessage.success('Media server node saved successfully')
    modalVisible.value = false
    loadServers()
  } catch (e: any) {
    ElMessage.error(e.message || 'Action failed')
  }
}

function handleDelete(node: any) {
  ElMessageBox.confirm(`Delete media node ${node.id}?`, 'Confirm Delete', {
    confirmButtonText: 'Delete',
    cancelButtonText: 'Cancel',
    type: 'warning',
  }).then(async () => {
    await systemService.deleteMediaServer(node.id)
    ElMessage.success('Media server removed')
    loadServers()
  })
}

onMounted(() => {
  loadServers()
})
</script>
