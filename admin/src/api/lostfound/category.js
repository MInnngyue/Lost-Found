import request from '@/utils/request'

export function listCategory(query) {
  return request({
    url: '/lostfound/category/list',
    method: 'get',
    params: query
  })
}

export function getCategory(categoryId) {
  return request({
    url: '/lostfound/category/' + categoryId,
    method: 'get'
  })
}

export function addCategory(data) {
  return request({
    url: '/lostfound/category',
    method: 'post',
    data
  })
}

export function updateCategory(data) {
  return request({
    url: '/lostfound/category',
    method: 'put',
    data
  })
}

export function delCategory(categoryIds) {
  return request({
    url: '/lostfound/category/' + categoryIds,
    method: 'delete'
  })
}
