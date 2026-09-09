import test from 'node:test'
import assert from 'node:assert/strict'
import * as vue from 'vue'
import { loadModule } from './load-module.mjs'

test('campus tree retains children across CAMPUS, AREA and LOCATION types', async () => {
  const calls = []
  const tree = [{ categoryId: 10, categoryType: 'CAMPUS', categoryName: 'Campus', children: [
    { categoryId: 11, categoryType: 'AREA', categoryName: 'Area', children: [
      { categoryId: 12, categoryType: 'LOCATION', categoryName: 'Room', children: [] },
    ] },
  ] }]
  const request = { get: async (url, config) => {
    calls.push(config)
    return { code: 200, data: tree }
  } }
  const api = await loadModule('api/category.js', { './request': { default: request } })
  const campuses = await api.getCategories('location')
  assert.equal(campuses.data[0]?.id, 10)
  const areas = await api.getCategoryChildren('location', 10)
  assert.equal(areas.data[0]?.id, 11)
  const details = await api.getCategoryChildren('location', 11)
  assert.equal(details.data[0]?.name, 'Room')
  assert.equal(calls.every(config => !config.params?.categoryType), true)
})

test('publishing maps selected IDs and does not send empty IDs or client-owned state', async () => {
  let payload
  const request = {
    post: async (url, body) => { payload = body; return { code: 200, data: { postId: 42 } } },
    put: async () => ({ code: 200 }),
  }
  const api = await loadModule('api/post.js', { './request': { default: request } })
  await api.publishPost({ type: 0, title: 'Phone', itemCategory: 3, color: '',
    locationCampus: 10, locationArea: '', lostTime: '2026-09-05', publisherId: 999,
    lifecycleStatus: 'PUBLISHED', version: 0 })
  assert.equal(payload.postType, 'LOST')
  assert.equal(payload.itemCategoryId, 3)
  assert.equal(payload.campusCategoryId, 10)
  assert.equal(payload.colorCategoryId, null)
  assert.equal(payload.areaCategoryId, null)
  assert.equal(payload.publisherId, undefined)
  assert.equal(payload.lifecycleStatus, undefined)
})

test('submit failure retains saved draft ID so retry does not create a duplicate', async () => {
  let creates = 0
  let submissions = 0
  const request = {
    post: async () => { creates++; return { code: 200, data: { postId: 42 } } },
    put: async () => { submissions++; throw { code: 403, msg: 'Denied' } },
  }
  const api = await loadModule('api/post.js', { './request': { default: request } })
  await assert.rejects(api.publishPost({ title: 'Phone', type: 0 }), error => error.postId === 42)
  assert.equal(creates, 1)
  assert.equal(submissions, 1)
})

test('captcha fetch failure blocks login instead of turning captcha off', async () => {
  let loginCalls = 0
  const api = await loadModule('composables/useAuthForm.js', {
    vue,
    '@/api/user': { getCaptcha: async () => { throw new Error('offline') },
      login: async () => { loginCalls++ }, register: async () => {}, getUserInfo: async () => {} },
    '@/utils/authSession': { saveAuthSession: () => {} },
  })
  const auth = api.useAuthForm()
  await auth.refreshCaptcha()
  auth.updateField('username', 'student')
  auth.updateField('password', 'Test-only-123')
  await auth.submit()
  assert.equal(auth.captcha.enabled, true)
  assert.equal(loginCalls, 0)
})

test('explicitly disabled captcha allows login and stores verified user identity', async () => {
  let saved
  let payload
  const api = await loadModule('composables/useAuthForm.js', {
    vue,
    '@/api/user': { getCaptcha: async () => ({ captchaEnabled: false }),
      login: async body => { payload = body; return { data: { token: 'test-only-token' } } },
      getUserInfo: async () => ({ data: { userId: 17, username: 'student' }, permissions: [] }),
      register: async () => {} },
    '@/utils/authSession': { saveAuthSession: data => { saved = data } },
  })
  const auth = api.useAuthForm()
  await auth.refreshCaptcha()
  auth.updateField('username', 'student')
  auth.updateField('password', 'Test-only-123')
  assert.equal((await auth.submit()).type, 'authenticated')
  assert.equal(payload.code, '')
  assert.equal(saved.userId, 17)
})
