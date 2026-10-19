import { BaseService } from './base.service'

export class RecordService extends BaseService {
  private static instance: RecordService

  private constructor() {
    super()
  }

  public static getInstance(): RecordService {
    if (!RecordService.instance) {
      RecordService.instance = new RecordService()
    }
    return RecordService.instance
  }

  // Cloud Recording
  public getCloudRecordList(params: {
    page: number
    count: number
    query?: string
    app?: string
    stream?: string
    startTime?: string
    endTime?: string
    mediaServerId?: string
  }) {
    return this.get('/api/cloud/record/list', params)
  }

  public getCloudRecordDateList(params: {
    app: string
    stream: string
    year?: number | string
    month?: number | string
    mediaServerId?: string
  }) {
    return this.get('/api/cloud/record/date/list', params)
  }

  public getCloudRecordFiles(params: {
    page: number
    count: number
    app: string
    stream: string
    startTime?: string
    endTime?: string
    mediaServerId?: string
  }) {
    return this.get('/api/cloud/record/file/list', params)
  }

  // GB28181 Device Local Record
  public queryGbRecordList(params: {
    deviceId: string
    channelId: string
    startTime: string
    endTime: string
  }) {
    return this.get(`/api/gb_record/query/${params.deviceId}/${params.channelId}`, {
      startTime: params.startTime,
      endTime: params.endTime,
    })
  }

  public startGbPlayback(deviceId: string, channelId: string, startTime: string, endTime: string) {
    return this.get(`/api/playback/start/${deviceId}/${channelId}`, { startTime, endTime })
  }

  public stopGbPlayback(deviceId: string, channelId: string, stream: string) {
    return this.get(`/api/playback/stop/${deviceId}/${channelId}/${stream}`)
  }

  public seekGbPlayback(streamId: string, seekTime: number | string) {
    return this.get(`/api/playback/seek/${streamId}/${seekTime}`)
  }
}

export const recordService = RecordService.getInstance()
