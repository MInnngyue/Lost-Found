import request from './request'

function toPostType(type) {
  if (type === 0 || type === 'LOST') return 'LOST'
  if (type === 1 || type === 'FOUND') return 'FOUND'
  return type
}

function optionalId(value) {
  return value === '' || value === undefined || value === null ? null : value
}

export function normalizePost(post) {
  if (!post) return post
  return {
    ...post,
    images: (post.images || []).map(image => typeof image === 'string' ? image : image.accessUrl),
    id: post.postId,
    type: post.postType === 'LOST' ? 0 : 1,
    itemCategory: post.itemCategoryName || post.itemCategory,
    color: post.colorCategoryName || post.color,
    locationCampus: post.campusCategoryName || post.locationCampus,
    locationArea: post.areaCategoryName || post.locationArea,
    lostTime: post.eventDate,
    userId: post.publisherId,
    status: post.lifecycleStatus,
  }
}

function toBackendPost(data, postId) {
  return {
    postId,
    postType: toPostType(data.type ?? data.postType),
    title: data.title,
    description: data.description,
    itemCategoryId: optionalId(data.itemCategoryId ?? data.itemCategory),
    colorCategoryId: optionalId(data.colorCategoryId ?? data.color),
    campusCategoryId: optionalId(data.campusCategoryId ?? data.locationCampus),
    areaCategoryId: optionalId(data.areaCategoryId ?? data.locationArea),
    locationDetail: data.locationDetail,
    eventDate: data.eventDate || data.lostTime,
    expireTime: data.expireTime,
    version: data.version,
    remark: data.remark,
  }
}

export function getPosts(params = {}) {
  const backendParams = {
    postType: toPostType(params.type),
    keyword: params.keyword,
    itemCategoryId: params.itemCategoryId,
    campusCategoryId: params.campusCategoryId,
    areaCategoryId: params.areaCategoryId,
    pageNum: params.page || 1,
    pageSize: params.pageSize || 20,
  }
  return request.get('/lostfound/post/list', { params: backendParams }).then((res) => ({
    ...res,
    data: {
      records: (res.rows || []).map(normalizePost),
      total: res.total || 0,
    },
  }))
}

export function getPostDetail(id) {
  return request.get(`/lostfound/post/${id}`).then((res) => ({
    ...res,
    data: normalizePost(res.data),
  }))
}

export function uploadPostImage(postId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/lostfound/post/${postId}/images`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export async function publishPost(data) {
  const res = await request.post('/lostfound/post', toBackendPost(data))
  const postId = res.data?.postId
  if (!postId) throw Object.assign(new Error('草稿已保存，请到我的帖子中提交审核'), { saved: true })
  try {
    for (const file of data.images || []) {
      await uploadPostImage(postId, file)
    }
    await submitPost(postId)
  } catch (error) {
    throw Object.assign(new Error('草稿已保存，但图片上传或提交审核失败，请到我的帖子重试', { cause: error }),
      { postId, saved: true })
  }
  return { ...res, data: normalizePost(res.data) }
}

export function withdrawPost(id) {
  return request.put(`/lostfound/post/withdraw/${id}`)
}

export const deletePost = withdrawPost

export function updatePost(id, data) {
  return request.put('/lostfound/post', toBackendPost(data, id))
}

export function submitPost(id) {
  return request.put(`/lostfound/post/submit/${id}`)
}
