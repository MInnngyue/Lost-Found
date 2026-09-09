import request from './request'
import { normalizePost } from './post'

function normalizeUser(user, roles = []) {
  if (!user) return user
  return {
    ...user,
    id: user.userId,
    userId: user.userId,
    username: user.userName,
    nickname: user.nickName,
    phone: user.phonenumber,
    role: roles.includes('admin') ? 1 : 0,
  }
}

export function getCaptcha() {
  return request({
    url: '/captchaImage',
    method: 'get',
    skipAuth: true,
    handleErrorLocally: true,
  })
}

export function login(data) {
  return request({
    url: '/login',
    method: 'post',
    skipAuth: true,
    data,
    handleErrorLocally: true,
  }).then((res) => ({ ...res, data: { token: res.token } }))
}

export function register(data) {
  return request({
    url: '/register',
    method: 'post',
    skipAuth: true,
    data,
    handleErrorLocally: true,
  })
}

export function getUserInfo(token) {
  return request({
    url: '/getInfo',
    method: 'get',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    handleErrorLocally: Boolean(token),
  }).then((res) => ({ ...res, data: normalizeUser(res.user, res.roles || []) }))
}

export function updateProfile(data) {
  return request({
    url: '/system/user/profile',
    method: 'put',
    data: {
      nickName: data.nickname,
      email: data.email,
      phonenumber: data.phone,
      sex: data.sex,
    },
  })
}

export function updatePassword(data) {
  return request.put('/system/user/profile/updatePwd', data)
}

export function logout() {
  return request.post('/logout')
}

export function getMyPosts(params = {}) {
  return request({
    url: '/lostfound/post/mine/list',
    method: 'get',
    params: {
      ...params,
      pageNum: params.page || 1,
      pageSize: params.size || params.pageSize || 20,
    },
  }).then((res) => ({
    ...res,
    data: (res.rows || []).map(normalizePost),
    total: res.total || 0,
  }))
}
