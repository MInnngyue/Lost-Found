import request from './request'
export function getComments(postId) { return request.get(`/lostfound/comment/post/${postId}`) }
export function addComment(data) { return request.post('/lostfound/comment', data) }
