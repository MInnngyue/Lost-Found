import request from '@/utils/request'

export function listMatch(query) {
  return request({
    url: '/lostfound/match/manage/list',
    method: 'get',
    params: query
  })
}
