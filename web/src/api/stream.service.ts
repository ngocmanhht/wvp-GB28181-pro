import { BaseService } from './base.service'

export class StreamService extends BaseService {
  private static instance: StreamService

  private constructor() {
    super()
  }

  public static getInstance(): StreamService {
    if (!StreamService.instance) {
      StreamService.instance = new StreamService()
    }
    return StreamService.instance
  }

  // Stream Proxy (Pull)
  public getStreamProxyList(params: { page: number; count: number; query?: string; online?: boolean | string }) {
    return this.get('/api/proxy/list', params)
  }

  public saveStreamProxy(data: any) {
    return this.post('/api/proxy/save', data)
  }

  public deleteStreamProxy(app: string, stream: string) {
    return this.delete('/api/proxy/del', { app, stream })
  }

  public startStreamProxy(app: string, stream: string) {
    return this.get('/api/proxy/start', { app, stream })
  }

  public stopStreamProxy(app: string, stream: string) {
    return this.get('/api/proxy/stop', { app, stream })
  }

  // Stream Push (Ingest)
  public getStreamPushList(params: { page: number; count: number; query?: string; pushing?: boolean | string }) {
    return this.get('/api/push/list', params)
  }

  public addStreamPush(data: any) {
    return this.post('/api/push/add', data)
  }

  public deleteStreamPush(app: string, stream: string) {
    return this.delete('/api/push/delete', { app, stream })
  }
}

export const streamService = StreamService.getInstance()
