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

  public deleteCloudRecords(ids: (number | string)[]) {
    return this.delete('/api/cloud/record/delete', null, { data: ids })
  }

  public addCloudRecordTask(params: {
    app: string
    stream: string
    mediaServerId?: string
    startTime: string
    endTime: string
  }) {
    return this.get('/api/cloud/record/task/add', params)
  }

  public getCloudRecordTaskList(params: { page: number; count: number; query?: string }) {
    return this.get('/api/cloud/record/task/list', params)
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

  public pauseGbPlayback(streamId: string) {
    return this.get(`/api/playback/pause/${streamId}`)
  }

  public resumeGbPlayback(streamId: string) {
    return this.get(`/api/playback/resume/${streamId}`)
  }

  public setGbPlaybackSpeed(streamId: string, speed: number | string) {
    return this.get(`/api/playback/speed/${streamId}/${speed}`)
  }

  public seekGbPlayback(streamId: string, seekTime: number | string) {
    return this.get(`/api/playback/seek/${streamId}/${seekTime}`)
  }

  // GB Record Download
  public startGbDownload(params: {
    deviceId: string
    channelId: string
    startTime: string
    endTime: string
    downloadSpeed?: number | string
  }) {
    return this.get(`/api/gb_record/download/start/${params.deviceId}/${params.channelId}`, {
      startTime: params.startTime,
      endTime: params.endTime,
      downloadSpeed: params.downloadSpeed || 4,
    })
  }

  public stopGbDownload(deviceId: string, channelId: string, streamId: string) {
    return this.get(`/api/gb_record/download/stop/${deviceId}/${channelId}/${streamId}`)
  }

  public queryGbDownloadProgress(deviceId: string, channelId: string, streamId: string) {
    return this.get(`/api/gb_record/download/progress/${deviceId}/${channelId}/${streamId}`)
  }
}

export const recordService = RecordService.getInstance()
