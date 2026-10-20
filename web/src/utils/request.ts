import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken, clearAuth } from './auth'
import router from '@/router'

let isRelogging = false

const service: AxiosInstance = axios.create({
  baseURL: (import.meta.env.VITE_APP_BASE_API as string) || '',
  timeout: 30000,
})

// Request Interceptor
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token && !config.url?.includes('/api/user/login')) {
      config.headers['access-token'] = token
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// Response Interceptor
service.interceptors.response.use(
  (response: AxiosResponse) => {
    if (response.config.url?.includes('/api/user/logout')) {
      return response.data
    }

    const res = response.data

    // If binary data or raw file download
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return res
    }

    // Backend WVP returns { code: 0, msg: "success", data: ... }
    if (typeof res.code !== 'undefined' && res.code !== 0) {
      ElMessage.error(res.msg || 'Request failed with error code ' + res.code)
      return Promise.reject(new Error(res.msg || 'Error'))
    }

    return res
  },
  (error) => {
    console.error('Response error:', error)
    if (error.response?.status === 401) {
      if (!isRelogging) {
        isRelogging = true
        ElMessageBox.confirm(
          'Login session has expired. Would you like to login again?',
          'Session Expired',
          {
            confirmButtonText: 'Relogin',
            cancelButtonText: 'Cancel',
            type: 'warning',
          }
        )
          .then(() => {
            clearAuth()
            router.push('/login')
          })
          .finally(() => {
            isRelogging = false
          })
      }
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || 'Network request failed')
    }
    return Promise.reject(error)
  }
)

export default service
