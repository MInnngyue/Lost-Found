import request from '@/utils/request'

export function listReport(query) {
  return request({
    url: '/lostfound/moderation/report/manage/list',
    method: 'get',
    params: query
  })
}

export function handleReport(reportId, data) {
  return request({
    url: '/lostfound/moderation/report/' + reportId + '/handle',
    method: 'put',
    data
  })
}

export function listViolationCase(query) {
  return request({
    url: '/lostfound/moderation/case/manage/list',
    method: 'get',
    params: query
  })
}

export function resolveViolationCase(caseId, data) {
  return request({
    url: '/lostfound/moderation/case/' + caseId + '/resolve',
    method: 'put',
    data
  })
}

export function listViolationAction(query) {
  return request({
    url: '/lostfound/moderation/action/manage/list',
    method: 'get',
    params: query
  })
}

export function listAppeal(query) {
  return request({
    url: '/lostfound/moderation/appeal/manage/list',
    method: 'get',
    params: query
  })
}

export function reviewAppeal(appealId, data) {
  return request({
    url: '/lostfound/moderation/appeal/' + appealId + '/review',
    method: 'put',
    data
  })
}
