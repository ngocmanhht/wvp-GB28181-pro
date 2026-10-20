export interface PaginationParams {
  page: number
  count: number
  query?: string
}

export interface PaginatedResponse<T> {
  total: number
  list: T[]
}

export interface DeviceInfo {
  deviceId: string
  name: string
  manufacturer?: string
  model?: string
  firmware?: string
  transport?: string
  streamMode: 'UDP' | 'TCP-ACTIVE' | 'TCP-PASSIVE'
  onLine: boolean
  channelCount: number
  ip: string
  port: number
  keepaliveTime?: string
  registerTime?: string
  createTime?: string
}

export interface ChannelInfo {
  channelId: string
  deviceId: string
  name: string
  manufacture?: string
  model?: string
  owner?: string
  civilCode?: string
  address?: string
  parental?: number
  safetyWay?: number
  registerWay?: number
  secrecy?: number
  ipAddress?: string
  port?: number
  password?: string
  status: number | boolean
  longitude?: string | number
  latitude?: string | number
  ptzType?: number
  customName?: string
}

export interface StreamProxyInfo {
  app: string
  stream: string
  name?: string
  url: string
  srcUrl?: string
  timeoutMs?: number
  ffmpegCmdKey?: string
  rtpType?: number // 0: TCP, 1: UDP
  status?: boolean
  enable?: boolean
  enableAudio?: boolean
  enableMp4?: boolean
  mediaServerId?: string
}

export interface StreamPushInfo {
  app: string
  stream: string
  totalReaderCount?: number
  pushing?: boolean
  createTime?: string
  aliveSecond?: number
  bytesSpeed?: number
  mediaServerId?: string
}

export interface CloudRecordInfo {
  id?: number | string
  app: string
  stream: string
  fileName?: string
  filePath?: string
  fileSize: number
  timeLen: number // in seconds
  startTime: string
  endTime: string
  mediaServerId?: string
  mediaServerPath?: string
}

export interface MediaServerInfo {
  id: string
  ip: string
  httpPort: number
  rtspPort: number
  rtmpPort: number
  rtpPortRange: string
  status: boolean
  autoConfig: boolean
  secret?: string
}

export interface PlatformInfo {
  id?: number | string
  serverGBId: string
  name: string
  serverIP: string
  serverPort: number
  deviceGBId: string
  deviceIp: string
  devicePort: number
  username?: string
  password?: string
  status: boolean
  transport: 'UDP' | 'TCP'
  characterSet?: string
  catalogId?: string
}
