import { BaseService } from './base.service'

export class SystemService extends BaseService {
  private static instance: SystemService

  private constructor() {
    super()
  }

  public static getInstance(): SystemService {
    if (!SystemService.instance) {
      SystemService.instance = new SystemService()
    }
    return SystemService.instance
  }

  public getMediaServerList() {
    return this.get('/api/server/media_server/list')
  }

  public getPlatformList(params: { page: number; count: number }) {
    return this.get('/api/platform/query/platforms', params)
  }

  public getSystemInfo() {
    return this.get('/api/server/system/info')
  }

  public getHistoryLogs(params: {
    page: number
    count: number
    query?: string
    type?: string
    startTime?: string
    endTime?: string
  }) {
    return this.get('/api/log/all', params)
  }

  public getRegionTree(parent?: string) {
    return this.get('/api/region/tree', { parent })
  }

  public getGroupTree(parent?: string) {
    return this.get('/api/group/tree', { parent })
  }
}

export const systemService = SystemService.getInstance()
