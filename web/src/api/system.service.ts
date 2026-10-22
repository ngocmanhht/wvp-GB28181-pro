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

  // Media Servers
  public getMediaServerList() {
    return this.get('/api/server/media_server/list')
  }

  public getOnlineMediaServerList() {
    return this.get('/api/server/media_server/online/list')
  }

  public getMediaServer(id: string) {
    return this.get(`/api/server/media_server/one/${id}`)
  }

  public checkMediaServer(params: { ip: string; httpPort: number | string; secret?: string; type?: string }) {
    return this.get('/api/server/media_server/check', {
      ip: params.ip,
      port: params.httpPort,
      secret: params.secret,
      type: params.type,
    })
  }

  public checkMediaServerRecord(params: { ip: string; port: number | string }) {
    return this.get('/api/server/media_server/record/check', params)
  }

  public saveMediaServer(data: any) {
    return this.post('/api/server/media_server/save', data)
  }

  public deleteMediaServer(id: string) {
    return this.delete('/api/server/media_server/delete', { id })
  }

  public getMediaServerLoad() {
    return this.get('/api/server/media_server/load')
  }

  // GB Cascading Platform
  public getPlatformList(params: { page: number; count: number; query?: string }) {
    return this.get('/api/platform/query', params)
  }

  public addPlatform(data: any) {
    return this.post('/api/platform/add', data)
  }

  public updatePlatform(data: any) {
    return this.post('/api/platform/update', data)
  }

  public deletePlatform(id: string | number) {
    return this.delete('/api/platform/delete', { id })
  }

  public pushPlatformChannel(id: string | number) {
    return this.get('/api/platform/channel/push', { id })
  }

  public getPlatformChannelList(params: {
    platformId: string | number
    page: number
    count: number
    query?: string
    online?: boolean
    channelType?: boolean
    hasShare?: boolean
  }) {
    return this.get('/api/platform/channel/list', params)
  }

  public addPlatformChannel(params: { platformId: string | number; channelIds?: string[]; all?: boolean }) {
    return this.post('/api/platform/channel/add', params)
  }

  public removePlatformChannel(params: { platformId: string | number; channelIds?: string[]; all?: boolean }) {
    return this.http.delete('/api/platform/channel/remove', { data: params })
  }

  public addPlatformChannelByDevice(params: { platformId: string | number; deviceIds: string[] }) {
    return this.post('/api/platform/channel/device/add', params)
  }

  public removePlatformChannelByDevice(params: { platformId: string | number; deviceIds: string[] }) {
    return this.post('/api/platform/channel/device/remove', params)
  }

  // System Resources & Monitoring
  public getSystemInfo() {
    return this.get('/api/server/system/info')
  }

  public getResourceInfo() {
    return this.get('/api/server/resource/info')
  }

  public getMapConfig() {
    return this.get('/api/server/map/config')
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
}

export const systemService = SystemService.getInstance()
