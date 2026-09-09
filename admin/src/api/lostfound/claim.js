import request from '@/utils/request'

export function listClaim(query) {
  return request({
    url: '/lostfound/claim/manage/list',
    method: 'get',
    params: query
  })
}
