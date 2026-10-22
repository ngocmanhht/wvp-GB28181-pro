import { BaseService } from './base.service'

export class PlayService extends BaseService {
  private static instance: PlayService

  private constructor() {
    super()
  }

  public static getInstance(): PlayService {
    if (!PlayService.instance) {
      PlayService.instance = new PlayService()
    }
    return PlayService.instance
  }

  public startPlay(deviceId: string, channelId: string) {
    return this.get(`/api/play/start/${deviceId}/${channelId}`)
  }

  public stopPlay(deviceId: string, channelId: string) {
    return this.get(`/api/play/stop/${deviceId}/${channelId}`)
  }

  public ptzControl(params: {
    deviceId: string
    channelId: string
    command: string
    horizonSpeed?: number
    verticalSpeed?: number
    zoomSpeed?: number
  }) {
    return this.get(`/api/front-end/ptz/${params.deviceId}/${params.channelId}`, {
      command: params.command,
      horizonSpeed: params.horizonSpeed || 50,
      verticalSpeed: params.verticalSpeed || 50,
      zoomSpeed: params.zoomSpeed || 50,
    })
  }

  public callPreset(deviceId: string, channelId: string, presetId: number | string) {
    return this.get(`/api/front-end/preset/call/${deviceId}/${channelId}`, { presetId })
  }

  public addPreset(deviceId: string, channelId: string, presetId: number | string) {
    return this.get(`/api/front-end/preset/add/${deviceId}/${channelId}`, { presetId })
  }

  public deletePreset(deviceId: string, channelId: string, presetId: number | string) {
    return this.get(`/api/front-end/preset/delete/${deviceId}/${channelId}`, { presetId })
  }

  public queryPreset(deviceId: string, channelId: string) {
    return this.get(`/api/front-end/preset/query/${deviceId}/${channelId}`)
  }

  // PTZ Scan
  public startScan(deviceId: string, channelId: string, scanId: number | string) {
    return this.get(`/api/front-end/scan/start/${deviceId}/${channelId}/${scanId}`)
  }

  public stopScan(deviceId: string, channelId: string, scanId: number | string) {
    return this.get(`/api/front-end/scan/stop/${deviceId}/${channelId}/${scanId}`)
  }

  public setLeftForScan(deviceId: string, channelId: string, scanId: number | string) {
    return this.get(`/api/front-end/scan/set/left/${deviceId}/${channelId}/${scanId}`)
  }

  public setRightForScan(deviceId: string, channelId: string, scanId: number | string) {
    return this.get(`/api/front-end/scan/set/right/${deviceId}/${channelId}/${scanId}`)
  }

  public setSpeedForScan(deviceId: string, channelId: string, scanId: number | string, speed: number) {
    return this.get(`/api/front-end/scan/set/speed/${deviceId}/${channelId}/${scanId}`, { speed })
  }

  // PTZ Cruise
  public startCruise(deviceId: string, channelId: string, cruiseId: number | string) {
    return this.get(`/api/front-end/cruise/start/${deviceId}/${channelId}/${cruiseId}`)
  }

  public stopCruise(deviceId: string, channelId: string, cruiseId: number | string) {
    return this.get(`/api/front-end/cruise/stop/${deviceId}/${channelId}/${cruiseId}`)
  }

  public addPointForCruise(deviceId: string, channelId: string, cruiseId: number | string, presetId: number | string) {
    return this.get(`/api/front-end/cruise/add/${deviceId}/${channelId}/${cruiseId}/${presetId}`)
  }

  public deletePointForCruise(deviceId: string, channelId: string, cruiseId: number | string, presetId: number | string) {
    return this.get(`/api/front-end/cruise/delete/${deviceId}/${channelId}/${cruiseId}/${presetId}`)
  }

  // Auxiliary and Wiper
  public wiper(deviceId: string, channelId: string, command: string) {
    return this.get(`/api/front-end/wiper/${deviceId}/${channelId}`, { command })
  }

  public auxiliary(deviceId: string, channelId: string, switchId: number | string, command: string) {
    return this.get(`/api/front-end/auxiliary/${deviceId}/${channelId}/${switchId}`, { command })
  }

  // Two-way Audio Talk & Broadcast
  public talkStart(deviceId: string, channelId: string) {
    return this.get(`/api/play/broadcast/${deviceId}/${channelId}`, { isAudio: true })
  }

  public talkStop(deviceId: string, channelId: string) {
    return this.get(`/api/play/broadcast/stop/${deviceId}/${channelId}`)
  }

  public channelTalkStart(channelId: string | number) {
    return this.get('/api/common/channel/talk/start', { channelId })
  }

  public channelTalkStop(channelId: string | number) {
    return this.get('/api/common/channel/talk/stop', { channelId })
  }
}

export const playService = PlayService.getInstance()
