import { BaseService } from './base.service'

export class UserApiKeyService extends BaseService {
  private static instance: UserApiKeyService

  private constructor() {
    super()
  }

  public static getInstance(): UserApiKeyService {
    if (!UserApiKeyService.instance) {
      UserApiKeyService.instance = new UserApiKeyService()
    }
    return UserApiKeyService.instance
  }

  public getApiKeyList(params: { page: number; count: number; userId?: number | string }) {
    return this.get('/api/userApiKey/userApiKeys', params)
  }

  public addApiKey(data: { userId: number | string; app: string; enable: boolean; expiresAt?: string; remark?: string }) {
    return this.http.post('/api/userApiKey/add', null, { params: data })
  }

  public enableApiKey(id: number | string) {
    return this.post(`/api/userApiKey/enable?id=${id}`)
  }

  public disableApiKey(id: number | string) {
    return this.post(`/api/userApiKey/disable?id=${id}`)
  }

  public resetApiKey(id: number | string) {
    return this.post(`/api/userApiKey/reset?id=${id}`)
  }

  public deleteApiKey(id: number | string) {
    return this.delete(`/api/userApiKey/delete?id=${id}`)
  }

  public remarkApiKey(params: { id: number | string; remark: string }) {
    return this.http.post('/api/userApiKey/remark', null, { params })
  }
}

export const userApiKeyService = UserApiKeyService.getInstance()
