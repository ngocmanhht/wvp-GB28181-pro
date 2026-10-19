<template>
  <div class="relative w-screen h-screen overflow-hidden flex items-center justify-center bg-slate-950 text-slate-100 select-none">
    <!-- Ambient Background Effects -->
    <div class="absolute -top-40 -left-40 w-96 h-96 bg-blue-600/20 rounded-full blur-3xl pointer-events-none"></div>
    <div class="absolute -bottom-40 -right-40 w-96 h-96 bg-indigo-600/20 rounded-full blur-3xl pointer-events-none"></div>

    <!-- Login Glass Card -->
    <div class="relative w-full max-w-md p-8 mx-4 rounded-2xl bg-slate-900/80 border border-slate-800 backdrop-blur-xl shadow-2xl flex flex-col gap-6 z-10">
      <!-- Header / Logo -->
      <div class="flex flex-col items-center text-center gap-2">
        <div class="w-14 h-14 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-blue-500/30">
          <el-icon class="text-3xl text-white"><VideoCamera /></el-icon>
        </div>
        <h1 class="text-2xl font-bold tracking-tight bg-gradient-to-r from-white via-slate-200 to-slate-400 bg-clip-text text-transparent">
          WVP-PRO Platform
        </h1>
        <p class="text-xs text-slate-400">
          GB28181 & ZLMediaKit Video Surveillance Gateway
        </p>
      </div>

      <!-- Login Form -->
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin" class="flex flex-col gap-4">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="Username (e.g. admin)"
            size="large"
            :prefix-icon="User"
            class="custom-input"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="Password"
            size="large"
            :prefix-icon="Lock"
            show-password
            class="custom-input"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="w-full !rounded-xl !h-12 !font-semibold !text-base !bg-gradient-to-r !from-blue-600 !to-indigo-600 !border-none hover:!shadow-lg hover:!shadow-blue-500/25 transition-all mt-2"
          :loading="loading"
          @click="handleLogin"
        >
          Sign In to System
        </el-button>
      </el-form>

      <!-- Footer Info -->
      <div class="flex items-center justify-between text-[11px] text-slate-500 border-t border-slate-800 pt-4">
        <span class="flex items-center gap-1.5">
          <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          Gateway Ready
        </span>
        <span>Version 2.0 (Vue 3 Pro)</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, FormInstance } from 'element-plus'
import { VideoCamera, User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '',
})

const rules = {
  username: [{ required: true, message: 'Please enter your username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please enter your password', trigger: 'blur' }],
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login({
          username: form.username,
          password: form.password,
        })
        ElMessage.success('Welcome back to WVP-PRO!')
        const redirect = (route.query.redirect as string) || '/'
        router.push(redirect)
      } catch (e: any) {
        console.error('Login error:', e)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
:deep(.el-input__wrapper) {
  background-color: rgba(15, 23, 42, 0.8) !important;
  border: 1px solid #334155 !important;
  border-radius: 12px !important;
  box-shadow: none !important;
  padding: 8px 14px;
}
:deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 1px #3b82f6 !important;
}
:deep(.el-input__inner) {
  color: #f8fafc !important;
}
</style>
