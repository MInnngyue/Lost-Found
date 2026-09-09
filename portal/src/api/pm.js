import request from './request'
export function getConversations() {
  return request.get('/lostfound/private-message/conversations').then((res) => ({
    ...res,
    data: (res.data || []).map((conversation) => ({
      ...conversation,
      partnerId: conversation.toUserId ?? conversation.userId,
      unread: Number(conversation.unread || 0),
    })),
  }))
}
export function getChatWith(userId) {
  return request.get(`/lostfound/private-message/with/${userId}`).then((res) => ({
    ...res,
    data: res.data || [],
  }))
}
export function sendMessage(data) { return request.post('/lostfound/private-message', data) }
