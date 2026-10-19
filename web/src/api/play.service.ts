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
}

export const playService = PlayService.getInstance()
