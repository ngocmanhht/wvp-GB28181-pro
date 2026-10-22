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

  public teleboot(deviceId: string) {
    return this.get(`/api/front-end/teleboot/${deviceId}`)
  }

  public setGuard(deviceId: string, guardCmd: string) {
    return this.get(`/api/front-end/guard/${deviceId}/${guardCmd}`)
  }

  public resetGuard(deviceId: string, guardCmd: string) {
    return this.get(`/api/front-end/resetguard/${deviceId}/${guardCmd}`)
  }

  public resetAlarm(deviceId: string) {
    return this.get(`/api/front-end/alarm/reset/${deviceId}`)
  }

  public homePosition(deviceId: string, channelId: string, params: any) {
    return this.get(`/api/front-end/home_position/${deviceId}/${channelId}`, params)
  }

  public subscribeCatalog(deviceId: string, cycle: number) {
    return this.get(`/api/device/query/catalog/subscribe/${deviceId}`, { cycle })
  }

  public subscribeMobilePosition(deviceId: string, cycle: number, interval: number) {
    return this.get(`/api/device/query/mobile_position/subscribe/${deviceId}`, { cycle, interval })
  }

  public subscribeForAlarm(deviceId: string, cycle: number) {
    return this.get(`/api/device/query/alarm/subscribe/${deviceId}`, { cycle })
  }

  public queryBasicParam(deviceId: string, channelId: string) {
    return this.get(`/api/device/config/basic_param/${deviceId}/${channelId}`)
  }

  public setBasicParam(deviceId: string, channelId: string, params: any) {
    return this.post(`/api/device/config/basic_param/${deviceId}/${channelId}`, null, { params })
  }

  public queryTimeStatistics(deviceId: string) {
    return this.get(`/api/device/query/time_statistics/${deviceId}`)
  }

  public addDevice(data: any) {
    return this.post('/api/device/query/device/add', data)
  }

  public updateDevice(data: any) {
    return this.post('/api/device/query/device/update', data)
  }
}

export const deviceService = DeviceService.getInstance()
