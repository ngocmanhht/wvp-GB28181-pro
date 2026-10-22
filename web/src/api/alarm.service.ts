import { BaseService } from './base.service'

export class AlarmService extends BaseService {
  private static instance: AlarmService

  private constructor() {
    super()
  }

  public static getInstance(): AlarmService {
    if (!AlarmService.instance) {
      AlarmService.instance = new AlarmService()
    }
    return AlarmService.instance
  }

  public getAlarmList(params: {
    page: number
    count: number
    alarmType?: string[]
    beginTime?: string
    endTime?: string
  }) {
    const query = new URLSearchParams()
    query.append('page', String(params.page))
    query.append('count', String(params.count))
    if (params.alarmType && params.alarmType.length > 0) {
      params.alarmType.forEach((t) => query.append('alarmType', t))
    }
    if (params.beginTime) query.append('beginTime', params.beginTime)
    if (params.endTime) query.append('endTime', params.endTime)

    return this.get(`/api/alarm/list?${query.toString()}`)
  }

  public deleteAlarms(ids: (number | string)[]) {
    return this.delete('/api/alarm/delete', null, { data: ids })
  }

  public clearAlarms(params?: { alarmType?: string[]; beginTime?: string; endTime?: string }) {
    const query = new URLSearchParams()
    if (params?.alarmType && params.alarmType.length > 0) {
      params.alarmType.forEach((t) => query.append('alarmType', t))
    }
    if (params?.beginTime) query.append('beginTime', params.beginTime)
    if (params?.endTime) query.append('endTime', params.endTime)
    const qs = query.toString()
    return this.delete(`/api/alarm/clear${qs ? '?' + qs : ''}`)
  }
}

export const alarmService = AlarmService.getInstance()
