import test from 'node:test'
import assert from 'node:assert/strict'
import * as vue from 'vue'
import validator from 'async-validator'
import { loadModule } from './load-module.mjs'

const Schema = validator.default || validator
const tree = [{ id: 10, name: 'Test campus', children: [
  { id: 11, name: 'Test area', children: [{ id: 12, name: 'Test room' }] },
] }]

function form() {
  return vue.ref({ type: 0, title: 'Test post', itemCategory: 1, color: 2,
    locationCampus: 10, locationArea: '', locationDetail: '', lostTime: '2026-09-01' })
}

async function categories(getCategories) {
  return loadModule('composables/usePostCategories.js', {
    vue, '@/api/category': { getCategories },
  })
}

test('required post fields include title, including whitespace-only input', async () => {
  const { postRules } = await loadModule('utils/postForm.js')
  const schema = new Schema(postRules)
  for (const field of ['title', 'itemCategory', 'color', 'locationCampus', 'lostTime']) {
    await assert.rejects(schema.validate({ ...form().value, [field]: '' }),
      error => error.errors.some(item => item.field === field))
  }
  await assert.rejects(schema.validate({ ...form().value, title: '   ' }))
  await schema.validate(form().value)
})

test('invalid type, long title and future date cannot be submitted', async () => {
  const { postRules } = await loadModule('utils/postForm.js')
  const schema = new Schema(postRules)
  for (const patch of [{ type: 9 }, { title: 'x'.repeat(129) }, { lostTime: '2999-01-01' },
    { lostTime: '2026-02-30' }]) {
    await assert.rejects(schema.validate({ ...form().value, ...patch }))
  }
})

test('empty categories block publishing and identify each missing prerequisite', async () => {
  const { usePostCategories } = await categories(async () => ({ data: [] }))
  const state = usePostCategories(form())
  assert.equal(state.ready.value, false)
  await state.loadCategories()
  assert.equal(state.ready.value, false)
  assert.match(state.error.value, /物品分类.*颜色.*校区/)
})

test('failed category loading stays blocked and can be retried', async () => {
  let fail = true
  const { usePostCategories } = await categories(async type => {
    if (fail) throw new Error('offline')
    return { data: type === 'location' ? tree : [{ id: 1, name: type }] }
  })
  const state = usePostCategories(form())
  await state.loadCategories()
  assert.equal(state.ready.value, false)
  assert.equal(state.loading.value, false)
  assert.match(state.error.value, /加载失败/)
  fail = false
  await state.loadCategories()
  assert.equal(state.ready.value, true)
  assert.equal(state.error.value, '')
})

test('complete categories enable publishing and location changes clear stale selections', async () => {
  let calls = 0
  const { usePostCategories } = await categories(async type => {
    calls++
    return { data: type === 'location' ? tree : [{ id: 1, name: type }] }
  })
  const model = form()
  const state = usePostCategories(model)
  await state.loadCategories()
  assert.equal(state.ready.value, true)
  assert.equal(state.areas.value[0].id, 11)
  model.value.locationArea = 11
  assert.equal(state.details.value[0].id, 12)
  model.value.locationDetail = 'Test room'
  model.value.locationCampus = ''
  state.onCampusChange()
  assert.equal(model.value.locationArea, '')
  assert.equal(model.value.locationDetail, '')
  assert.deepEqual(state.areas.value, [])
  assert.equal(calls, 3)
})

test('an in-progress reload disables publishing and removes stale options on failure', async () => {
  let fail = false
  const { usePostCategories } = await categories(async type => {
    if (fail) throw new Error('offline')
    return { data: type === 'location' ? tree : [{ id: 1, name: type }] }
  })
  const state = usePostCategories(form())
  await state.loadCategories()
  fail = true
  const reload = state.loadCategories()
  assert.equal(state.ready.value, false)
  await reload
  assert.deepEqual(state.campuses.value, [])
  assert.equal(state.ready.value, false)
})

test('reloading category data clears selections that no longer exist', async () => {
  const { usePostCategories } = await categories(async type => ({
    data: type === 'location' ? tree : [{ id: type === 'color' ? 2 : 1, name: type }],
  }))
  const model = form()
  model.value.itemCategory = 999
  model.value.locationCampus = 999
  model.value.locationArea = 999
  model.value.locationDetail = 'Old place'
  const state = usePostCategories(model)
  await state.loadCategories()
  assert.equal(state.ready.value, true)
  assert.equal(model.value.itemCategory, '')
  assert.equal(model.value.color, 2)
  assert.equal(model.value.locationCampus, '')
  assert.equal(model.value.locationArea, '')
  assert.equal(model.value.locationDetail, '')
})
