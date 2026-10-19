import { BaseService } from './base.service'

export class DeviceService extends BaseService {
  private static instance: DeviceService

  private constructor() {
    super()
  }

  public static getInstance(): DeviceService {
    if (!DeviceService.instance) {
      DeviceService.instance = new DeviceService()
    }
    return DeviceService.instance
  }

  public getDevices(params: { page: number; count: number; query?: string; status?: boolean | string }) {
    return this.get('/api/device/query/devices', params)
  }

  public getDeviceSyncStatus(deviceId: string) {
    return this.get('/api/device/query/sync_status', { deviceId })
  }

  public syncDevice(deviceId: string) {
    return this.get(`/api/device/query/devices/${deviceId}/sync`)
  }

  public deleteDevice(deviceId: string) {
    return this.delete(`/api/device/query/devices/${deviceId}/delete`)
  }

  public updateDeviceTransport(deviceId: string, streamMode: string) {
    return this.post(`/api/device/query/transport/${deviceId}/${streamMode}`)
  }

  public getDeviceChannels(params: {
    deviceId: string
    page: number
    count: number
    query?: string
    online?: boolean | string
    channelType?: boolean | string
  }) {
    return this.get(`/api/device/query/devices/${params.deviceId}/channels`, {
      page: params.page,
      count: params.count,
      query: params.query,
      online: params.online,
      channelType: params.channelType,
    })
  }

  public getAllChannels(params: {
    page: number
    count: number
    query?: string
    online?: boolean | string
    channelType?: boolean | string
  }) {
    return this.get('/api/common/channel/list', params)
  }

  public updateChannel(channel: any) {
    return this.post('/api/device/query/channel/update', channel)
  }
}

export const deviceService = DeviceService.getInstance()
