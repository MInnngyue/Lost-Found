import request from './request'

export function getMessages() {
  return request.get('/lostfound/message').then((res) => ({
    ...res,
    data: (res.data || []).map((message) => ({
      ...message,
      messageId: message.messageId ?? message.id,
      isRead: Number(message.isRead || 0),
    })),
  }))
}

export function getUnreadCount() {
  return request.get('/lostfound/message/unread-count')
}

export function markRead(messageId) {
  return request.put(`/lostfound/message/${messageId}/read`)
}

export function markAllRead() {
  return request.put('/lostfound/message/read-all')
}
