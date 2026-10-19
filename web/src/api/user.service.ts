import { BaseService } from './base.service'
import CryptoJS from 'crypto-js'

export class UserService extends BaseService {
  private static instance: UserService

  private constructor() {
    super()
  }

  public static getInstance(): UserService {
    if (!UserService.instance) {
      UserService.instance = new UserService()
    }
    return UserService.instance
  }

  public login(params: { username: string; password: string }) {
    const md5Password = CryptoJS.MD5(params.password).toString()
    return this.get('/api/user/login', {
      username: params.username.trim(),
      password: md5Password,
    })
  }

  public logout() {
    return this.get('/api/user/logout')
  }

  public getUserInfo() {
    return this.post('/api/user/userInfo')
  }

  public getUserList(params: { page: number; count: number }) {
    return this.get('/api/user/users', params)
  }

  public deleteUser(id: number | string) {
    return this.delete('/api/user/delete', { id })
  }

  public addUser(data: { username: string; password?: string; roleId: number | string }) {
    const payload = { ...data }
    if (payload.password) {
      payload.password = CryptoJS.MD5(payload.password).toString()
    }
    return this.http.post('/api/user/add', null, { params: payload })
  }

  public changePassword(data: { oldPassword?: string; password: string }) {
    return this.http.post('/api/user/changePassword', null, {
      params: {
        oldPassword: data.oldPassword ? CryptoJS.MD5(data.oldPassword).toString() : '',
        password: CryptoJS.MD5(data.password).toString(),
      },
    })
  }

  public getApiKeyList(params: { page: number; count: number }) {
    return this.get('/api/userApiKey/list', params)
  }
}

export const userService = UserService.getInstance()
