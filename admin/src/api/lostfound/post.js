import request from '@/utils/request'

export function listPost(query) {
  return request({
    url: '/lostfound/post/manage/list',
    method: 'get',
    params: query
  })
}

export function getPost(postId) {
  return request({
    url: '/lostfound/post/manage/' + postId,
    method: 'get'
  })
}

export function reviewPost(data) {
  return request({
    url: '/lostfound/post/review',
    method: 'put',
    data
  })
}
