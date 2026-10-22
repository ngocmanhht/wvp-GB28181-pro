import { BaseService } from './base.service'

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
    return this.post('/api/user/login', {
      username: params.username.trim(),
      password: params.password,
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
    return this.delete(`/api/user/delete?id=${id}`)
  }

  public addUser(data: { username: string; password?: string; roleId: number | string }) {
    return this.http.post('/api/user/add', null, { params: data })
  }

  public changePassword(data: { oldPassword?: string; password: string }) {
    return this.http.post('/api/user/changePassword', null, {
      params: {
        oldPassword: data.oldPassword || '',
        password: data.password,
      },
    })
  }

  public changePasswordForAdmin(data: { userId: number | string; password: string }) {
    return this.http.post('/api/user/changePasswordForAdmin', null, {
      params: {
        userId: data.userId,
        password: data.password,
      },
    })
  }

  public changePushKey(params: { userId: number | string; pushKey: string }) {
    return this.http.post('/api/user/changePushKey', null, { params })
  }
}

export const userService = UserService.getInstance()
