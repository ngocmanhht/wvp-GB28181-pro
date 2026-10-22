import { BaseService } from './base.service'

export class OrgService extends BaseService {
  private static instance: OrgService

  private constructor() {
    super()
  }

  public static getInstance(): OrgService {
    if (!OrgService.instance) {
      OrgService.instance = new OrgService()
    }
    return OrgService.instance
  }

  // Administrative Division (Region)
  public getRegionTree(params?: { query?: string; parent?: string; hasChannel?: boolean }) {
    return this.get('/api/region/tree/list', params)
  }

  public queryRegionTree(params: { page: number; count: number; query?: string }) {
    return this.get('/api/region/tree/query', params)
  }

  public addRegion(data: any) {
    return this.post('/api/region/add', data)
  }

  public updateRegion(data: any) {
    return this.post('/api/region/update', data)
  }

  public deleteRegion(id: string | number) {
    return this.delete('/api/region/delete', { id })
  }

  public addRegionByCivilCode(civilCode: string) {
    return this.get('/api/region/addByCivilCode', { civilCode })
  }

  public getChildListInBase(parent?: string) {
    return this.get('/api/region/base/child/list', { parent })
  }

  // Business Grouping (Group)
  public getGroupTree(params?: { query?: string; parent?: string; hasChannel?: boolean; page?: number; count?: number }) {
    return this.get('/api/group/tree/list', params)
  }

  public queryGroupTree(params: { page: number; count: number; query?: string }) {
    return this.get('/api/group/tree/query', params)
  }

  public addGroup(data: any) {
    return this.post('/api/group/add', data)
  }

  public updateGroup(data: any) {
    return this.post('/api/group/update', data)
  }

  public deleteGroup(id: string | number) {
    return this.delete('/api/group/delete', { id })
  }

  // Region & Group Channel Association
  public addChannelToRegion(params: { regionId: string | number; channelIds: (string | number)[] }) {
    return this.post('/api/common/channel/region/add', params)
  }

  public deleteChannelFromRegion(channelIds: (string | number)[]) {
    return this.post('/api/common/channel/region/delete', channelIds)
  }

  public addChannelToGroup(params: { groupId: string | number; channelIds: (string | number)[] }) {
    return this.post('/api/common/channel/group/add', params)
  }

  public deleteChannelFromGroup(channelIds: (string | number)[]) {
    return this.post('/api/common/channel/group/delete', channelIds)
  }
}

export const orgService = OrgService.getInstance()
