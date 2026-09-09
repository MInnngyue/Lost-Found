import request from './request'

export function submitReport(data) {
  return request.post('/lostfound/moderation/report', data)
}

export function submitAppeal(data) {
  return request.post('/lostfound/moderation/appeal', data)
}

export function getMyActions() {
  return request.get('/lostfound/moderation/action/my')
}

export function getMyAppeals() {
  return request.get('/lostfound/moderation/appeal/my')
}
