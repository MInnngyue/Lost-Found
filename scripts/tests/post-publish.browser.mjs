import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdir } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const require = createRequire(process.env.PLAYWRIGHT_MODULE_DIR
  ? path.join(process.env.PLAYWRIGHT_MODULE_DIR, 'browser-test.cjs') : import.meta.url)
const { chromium } = require('playwright')
const output = fileURLToPath(new URL('../../_data/acceptance/m3-01/', import.meta.url))
await mkdir(output, { recursive: true })
const base = process.env.PORTAL_URL || 'http://localhost:5173'
const browser = await chromium.launch({ channel: process.env.BROWSER_CHANNEL || 'msedge', headless: true })
const categories = [
  { categoryId: 1, categoryType: 'ITEM', categoryName: '测试物品', parentId: 0, children: [] },
  { categoryId: 2, categoryType: 'COLOR', categoryName: '测试颜色', parentId: 0, children: [] },
  { categoryId: 10, categoryType: 'CAMPUS', categoryName: '测试校区', parentId: 0, children: [
    { categoryId: 11, categoryType: 'AREA', categoryName: '测试区域', parentId: 10, children: [] },
  ] },
]
const draft = { postId: 91, publisherId: 10, postType: 'LOST', title: 'Test draft',
  itemCategoryId: 1, colorCategoryId: 2, campusCategoryId: 10, areaCategoryId: null,
  eventDate: '2026-09-01', lifecycleStatus: 'DRAFT' }
const pageErrors = []

async function scenario(mode, viewport = { width: 1440, height: 1000 }) {
  const context = await browser.newContext({ viewport, reducedMotion: 'reduce' })
  await context.addInitScript(() => {
    localStorage.setItem('token', 'browser-test-fixture-not-a-real-token')
    localStorage.setItem('userInfo', JSON.stringify({ id: 10, role: 0 }))
  })
  const page = await context.newPage()
  page.on('pageerror', error => pageErrors.push(error.message))
  const state = { mode, creates: [], submissions: 0, edits: 0 }
  // All authenticated API calls are intercepted. No fixture is written to the real database.
  await context.route('http://localhost:8081/**', async route => {
    const url = new URL(route.request().url())
    const method = route.request().method()
    let body = { code: 200, data: [], rows: [], total: 0 }
    if (url.pathname === '/lostfound/category/tree') {
      if (state.mode === 'error') body = { code: 500, msg: 'Test category failure' }
      else body.data = state.mode === 'empty' ? [] :
        categories.filter(category => !url.searchParams.get('categoryType') ||
          category.categoryType === url.searchParams.get('categoryType'))
    } else if (url.pathname === '/lostfound/post' && method === 'POST') {
      state.creates.push(route.request().postDataJSON())
      body.data = draft
    } else if (url.pathname.startsWith('/lostfound/post/submit/')) {
      state.submissions++
      body = state.mode === 'submit-error' ? { code: 403, msg: 'Test submission denied' } : { code: 200 }
    } else if (url.pathname === '/lostfound/post/mine/list') {
      body.rows = [draft]
      body.total = 1
    } else if (url.pathname === '/lostfound/message/unread-count') {
      body.data = 0
    } else if (url.pathname === '/lostfound/private-message/conversations' ||
      url.pathname.startsWith('/lostfound/private-message/with/')) {
      body.data = []
    } else if (url.pathname === '/lostfound/post' && method === 'PUT') {
      state.edits++
    } else if (method !== 'OPTIONS' && url.pathname !== '/lostfound/post/list') {
      throw new Error(`Unexpected API request: ${method} ${url.pathname}`)
    }
    await route.fulfill({ json: body, headers: {
      'access-control-allow-origin': base,
      'access-control-allow-headers': 'authorization,content-type',
      'access-control-allow-methods': 'GET,POST,PUT,OPTIONS',
    } })
  })
  await page.goto(`${base}/publish`)
  await page.getByRole('heading', { name: '发布帖子', exact: true }).waitFor()
  return { page, state, context }
}

async function fillRequired(page, title = 'Test publish') {
  for (const [name, label] of [['物品分类', '测试物品'], ['颜色', '测试颜色'], ['校区', '测试校区']]) {
    await page.locator('.el-select__wrapper').filter({
      has: page.getByRole('combobox', { name, exact: true }),
    }).click()
    await page.getByRole('option', { name: label, exact: true }).click()
  }
  await page.getByPlaceholder('选择遗失/捡到日期').fill('2026-09-01')
  await page.getByPlaceholder('选择遗失/捡到日期').press('Tab')
  await page.getByLabel('标题', { exact: false }).fill(title)
}

try {
  for (const viewport of [{ width: 1440, height: 1000 }, { width: 390, height: 844 }]) {
    const { page, state, context } = await scenario('empty', viewport)
    await page.getByText('暂不能发布：物品分类、颜色、校区尚未配置，请联系管理员。').waitFor()
    assert.equal(await page.getByRole('button', { name: '提交审核', exact: true }).isDisabled(), true)
    await page.locator('form').evaluate(form => form.requestSubmit())
    assert.equal(state.creates.length, 0)
    await page.getByLabel('标题', { exact: false }).scrollIntoViewIfNeeded()
    await page.evaluate(() => window.scrollTo(0, 0))
    await page.screenshot({ path: path.join(output, `publish-empty-${viewport.width}.png`), fullPage: true })
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true)
    await context.close()
  }
  {
    const { page, state, context } = await scenario('error')
    await page.getByText('分类加载失败，请重试。', { exact: true }).waitFor()
    assert.equal(await page.getByRole('button', { name: '提交审核', exact: true }).isDisabled(), true)
    state.mode = 'complete'
    await page.getByRole('button', { name: '重试', exact: true }).click()
    await page.getByRole('combobox', { name: '物品分类', exact: true }).waitFor()
    await fillRequired(page, '')
    await page.getByRole('button', { name: '提交审核', exact: true }).click()
    await page.getByText('请输入帖子标题', { exact: true }).waitFor()
    assert.equal(state.creates.length, 0)
    await page.getByLabel('标题', { exact: false }).fill('Test publish')
    // Two synchronous submit events reproduce a rapid double click.
    await page.locator('form').evaluate(form => { form.requestSubmit(); form.requestSubmit() })
    await page.waitForURL(`${base}/`)
    assert.equal(state.creates.length, 1)
    assert.equal(state.submissions, 1)
    assert.equal(state.creates[0].title, 'Test publish')
    assert.equal(state.creates[0].itemCategoryId, 1)
    assert.equal(state.creates[0].areaCategoryId, null)
    await context.close()
  }
  {
    const { page, state, context } = await scenario('submit-error', { width: 390, height: 844 })
    await fillRequired(page)
    await page.getByRole('button', { name: '提交审核', exact: true }).click()
    await page.waitForURL(`${base}/my-posts`)
    assert.equal(state.creates.length, 1)
    assert.equal(state.submissions, 1)
    await page.getByTitle('编辑并重新提交审核', { exact: true }).click()
    await page.getByRole('dialog').waitFor()
    await page.getByPlaceholder('请输入帖子标题').fill('')
    await page.getByRole('button', { name: '保存并提交审核', exact: true }).click()
    await page.getByText('请输入帖子标题', { exact: true }).waitFor()
    assert.equal(state.edits, 0)
    await page.screenshot({ path: path.join(output, 'edit-required-mobile.png'), fullPage: true })
    await context.close()
  }
  assert.deepEqual(pageErrors, [])
  console.log('PASS: mocked browser regression for empty/error categories, retry, required title, full form, duplicate submit guard, saved-draft recovery and edit validation.')
  console.log(`Screenshots: ${output}`)
  console.log('Authenticated calls were mocked; this is not a real publishing/review acceptance test.')
} finally {
  await browser.close()
}
