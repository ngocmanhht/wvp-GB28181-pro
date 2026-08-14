import request from '@/utils/request'

// Cloud recordingAPI

export function getAll() {
  return request({
    method: 'get',
    url: '/api/role/all'
  })
}

