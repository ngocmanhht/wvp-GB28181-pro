import { BaseService } from './base.service'

export class RecordPlanService extends BaseService {
  private static instance: RecordPlanService

  private constructor() {
    super()
  }

  public static getInstance(): RecordPlanService {
    if (!RecordPlanService.instance) {
      RecordPlanService.instance = new RecordPlanService()
    }
    return RecordPlanService.instance
  }

  public getPlanList(params: { page: number; count: number; query?: string }) {
    return this.get('/api/record/plan/query', params)
  }

  public getPlan(planId: string | number) {
    return this.get('/api/record/plan/get', { planId })
  }

  public addPlan(data: { name: string; planList: any[] }) {
    return this.post('/api/record/plan/add', {
      name: data.name,
      planItemList: data.planList,
    })
  }

  public updatePlan(data: { id: string | number; name: string; planList: any[] }) {
    return this.post('/api/record/plan/update', {
      id: data.id,
      name: data.name,
      planItemList: data.planList,
    })
  }

  public deletePlan(planId: string | number) {
    return this.delete('/api/record/plan/delete', { planId })
  }

  public getChannelList(params: {
    page: number
    count: number
    planId: string | number
    query?: string
    online?: boolean
    channelType?: boolean
    hasLink?: boolean
  }) {
    return this.get('/api/record/plan/channel/list', params)
  }

  public linkPlan(data: { planId: string | number; channelIds: (string | number)[]; all?: boolean }) {
    return this.post('/api/record/plan/link', data)
  }
}

export const recordPlanService = RecordPlanService.getInstance()
