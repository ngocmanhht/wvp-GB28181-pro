import { defineStore } from 'pinia'
import { ref } from 'vue'
import { userService } from '@/api'
import { getToken, setToken, clearAuth, getName, setName, setServerId, getServerId } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const username = ref<string | null>(getName())
  const serverId = ref<string | null>(getServerId())
  const role = ref<string>('')

  async function login(loginParams: { username: string; password: string }) {
    const res: any = await userService.login(loginParams)
    if (res && res.data) {
      const data = res.data
      token.value = data.accessToken
      username.value = data.username
      serverId.value = data.serverId

      setToken(data.accessToken)
      setName(data.username)
      if (data.serverId) setServerId(data.serverId)
    }
    return res
  }

  async function logout() {
    try {
      await userService.logout()
    } catch (e) {
      console.warn('Logout API failed, clearing local state', e)
    } finally {
      token.value = null
      username.value = null
      serverId.value = null
      clearAuth()
    }
  }

  async function fetchUserInfo() {
    if (!token.value) return null
    const res: any = await userService.getUserInfo()
    if (res && res.data) {
      role.value = res.data.role?.name || 'admin'
    }
    return res
  }

  return {
    token,
    username,
    serverId,
    role,
    login,
    logout,
    fetchUserInfo,
  }
})
