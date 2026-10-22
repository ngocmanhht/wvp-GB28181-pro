import { BaseService } from './base.service'

export class JtDeviceService extends BaseService {
  private static instance: JtDeviceService

  private constructor() {
    super()
  }

  public static getInstance(): JtDeviceService {
    if (!JtDeviceService.instance) {
      JtDeviceService.instance = new JtDeviceService()
    }
    return JtDeviceService.instance
  }

  public getDevices(params: { page: number; count: number; query?: string; online?: boolean | string }) {
    return this.get('/api/jt1078/terminal/list', params)
  }

  public getDeviceById(deviceId: string) {
    return this.get('/api/jt1078/terminal/query', { deviceId })
  }

  public addDevice(form: any) {
    return this.post('/api/jt1078/terminal/add', null, { params: form })
  }

  public updateDevice(form: any) {
    return this.post('/api/jt1078/terminal/update', null, { params: form })
  }

  public deleteDevice(phoneNumber: string) {
    return this.delete('/api/jt1078/terminal/delete', { phoneNumber })
  }

  public getChannels(params: { page: number; count: number; deviceId: string; query?: string }) {
    return this.get('/api/jt1078/terminal/channel/list', params)
  }

  public startPlay(phoneNumber: string, channelId: string | number) {
    return this.get(`/api/jt1078/play/${phoneNumber}/${channelId}`)
  }

  public stopPlay(phoneNumber: string, channelId: string | number) {
    return this.get(`/api/jt1078/stop/${phoneNumber}/${channelId}`)
  }

  public queryMediaList(params: {
    phoneNumber: string
    channelId: string | number
    startTime: string
    endTime: string
    mediaType?: number
    codeType?: number
    storageType?: number
  }) {
    return this.get(`/api/jt1078/record/list/${params.phoneNumber}/${params.channelId}`, params)
  }

  public startPlayback(phoneNumber: string, channelId: string | number, params: any) {
    return this.get(`/api/jt1078/playback/start/${phoneNumber}/${channelId}`, params)
  }

  public stopPlayback(phoneNumber: string, channelId: string | number, stream: string) {
    return this.get(`/api/jt1078/playback/stop/${phoneNumber}/${channelId}/${stream}`)
  }

  public sendTextMessage(params: { phoneNumber: string; text: string; flag?: number }) {
    return this.post('/api/jt1078/terminal/text_message', null, { params })
  }

  public telephoneCallback(params: { phoneNumber: string; callerPhoneNumber: string }) {
    return this.post('/api/jt1078/terminal/callback', null, { params })
  }

  public takePhoto(params: { phoneNumber: string; channelId: string | number; count?: number; interval?: number }) {
    return this.get(`/api/jt1078/terminal/shooting/${params.phoneNumber}/${params.channelId}`, params)
  }

  public controlDoor(params: { phoneNumber: string; doorId: number; state: number }) {
    return this.get('/api/jt1078/terminal/door', params)
  }

  public getPosition(phoneNumber: string) {
    return this.get(`/api/jt1078/terminal/position/${phoneNumber}`)
  }
}

export const jtDeviceService = JtDeviceService.getInstance()
