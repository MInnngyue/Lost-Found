import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdir } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(process.env.PLAYWRIGHT_MODULE_DIR
  ? path.join(process.env.PLAYWRIGHT_MODULE_DIR, 'browser-test.cjs') : import.meta.url)
const { chromium } = require('playwright')
const output = fileURLToPath(new URL('../../_data/acceptance/portal-layout/', import.meta.url))
const base = process.env.PORTAL_URL || 'http://localhost:5173'
const baseline = process.argv.includes('--baseline')
const browser = await chromium.launch({ channel: process.env.BROWSER_CHANNEL || 'msedge', headless: true })
const pageErrors = []
const categories = [
  { categoryId: 1, categoryType: 'ITEM', categoryName: '数码产品', parentId: 0, children: [] },
  { categoryId: 2, categoryType: 'COLOR', categoryName: '黑色', parentId: 0, children: [] },
  { categoryId: 10, categoryType: 'CAMPUS', categoryName: '中心校区', parentId: 0, children: [
    { categoryId: 11, categoryType: 'AREA', categoryName: '图书馆', parentId: 10, children: [] },
  ] },
]
const post = {
  postId: 91, publisherId: 20, postType: 'LOST', title: '图书馆遗失的黑色耳机',
  description: '在图书馆二楼遗失，收纳盒上有一枚蓝色贴纸。',
  itemCategoryId: 1, colorCategoryId: 2, campusCategoryId: 10, areaCategoryId: 11,
  itemCategoryName: '数码产品', colorCategoryName: '黑色', campusCategoryName: '中心校区',
  areaCategoryName: '图书馆', locationDetail: '二楼阅览室', eventDate: '2026-09-01',
  lifecycleStatus: 'PUBLISHED', createTime: '2026-09-01 10:00:00', createBy: '校园同学',
  images: [], version: 0,
}
const actions = [
  { actionId: 1, caseId: 1, actionType: 'POST_PUBLISH', reason: '同一物品短时间内重复发布，请核对已发布的信息。',
    startTime: '2026-09-01 09:00:00', endTime: '2099-09-08 09:00:00' },
  { actionId: 2, caseId: 2, actionType: 'COMMENT', reason: '评论内容已核查，相关限制已结束。',
    startTime: '2026-09-01 09:00:00', endTime: '2026-09-02 09:00:00' },
]
const appeals = [
  { appealId: 2, actionId: 2, caseId: 2, reason: '请求复核评论上下文。',
    status: 'APPROVED', createTime: '2026-09-01 12:00:00',
    reviewTime: '2026-09-02 09:00:00', reviewResult: '经复核，申诉成立，已终止相关限制。' },
]

async function scenario(viewport = { width: 1440, height: 1000 }) {
  const context = await browser.newContext({ viewport, reducedMotion: 'reduce' })
  await context.addInitScript(() => {
    localStorage.setItem('token', 'layout-fixture-not-a-real-token')
    localStorage.setItem('userInfo', JSON.stringify({ id: 10, userId: 10, username: '同学', role: 0 }))
  })
  const state = {
    fail: '', reports: [], submissions: [], edits: [], withdrawals: [], appealRequests: [],
    markReads: [], logoutCalls: [], avatarUploads: [], profileUpdates: [], claimRejects: [],
    actions: structuredClone(actions), appeals: structuredClone(appeals), claimStatus: 'PENDING',
    posts: ['DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'RESOLVED'].map((status, index) => ({
      ...post, postId: 91 + index, publisherId: 10, lifecycleStatus: status,
      title: index === 3 ? '教学楼拾到的校园卡，请失主核对后联系认领' : post.title,
      remark: index === 3 ? '请补充物品特征后重新提交。' : '',
    })),
  }
  // Intercept every API request; tests must never write fixtures to the live database.
  await context.route('http://localhost:8081/**', async route => {
    const request = route.request()
    const url = new URL(request.url())
    const endpoint = url.pathname
    const method = request.method()
    let body = { code: 200, data: [], rows: [], total: 0 }
    if (method === 'OPTIONS') {
      body = {}
    } else if (endpoint === '/lostfound/category/tree') {
      body.data = categories.filter(c => !url.searchParams.get('categoryType') ||
        c.categoryType === url.searchParams.get('categoryType'))
      if (state.fail === 'categories') body = { code: 500, msg: '分类加载失败' }
    } else if (endpoint === '/lostfound/post/mine/list') {
      body.rows = state.posts
      body.total = state.posts.length
      if (state.fail === 'posts') body = { code: 500, msg: '帖子加载失败' }
    } else if (endpoint === '/lostfound/post/list') {
      body.rows = [post]
      body.total = 1
    } else if (/^\/lostfound\/post\/\d+$/.test(endpoint)) {
      body.data = post
    } else if (endpoint === '/lostfound/post' && method === 'PUT') {
      state.edits.push(request.postDataJSON())
      if (state.fail === 'edit') body = { code: 500, msg: '保存失败，请重试' }
    } else if (endpoint.startsWith('/lostfound/post/submit/')) {
      state.submissions.push(endpoint)
      if (state.fail === 'submit') body = { code: 500, msg: '提交失败，请重试' }
      else state.posts.find(p => p.postId === Number(endpoint.split('/').at(-1))).lifecycleStatus = 'PENDING_REVIEW'
    } else if (endpoint.startsWith('/lostfound/post/withdraw/')) {
      state.withdrawals.push(endpoint)
      state.posts.find(p => p.postId === Number(endpoint.split('/').at(-1))).lifecycleStatus = 'WITHDRAWN'
    } else if (endpoint === '/lostfound/moderation/action/my') {
      body.data = state.actions
      if (state.fail === 'governance') body = { code: 500, msg: '记录加载失败' }
    } else if (endpoint === '/lostfound/moderation/appeal/my') {
      body.data = state.appeals
      if (state.fail === 'governance') body = { code: 500, msg: '记录加载失败' }
    } else if (endpoint === '/lostfound/moderation/appeal') {
      state.appealRequests.push(request.postDataJSON())
      if (state.fail === 'appeal') body = { code: 500, msg: '申诉提交失败，请重试' }
      else state.appeals.push({ ...request.postDataJSON(), appealId: 3, status: 'SUBMITTED',
        createTime: '2026-09-07 10:00:00' })
    } else if (endpoint === '/lostfound/moderation/report') {
      state.reports.push(request.postDataJSON())
      if (state.fail === 'report') body = { code: 500, msg: '举报提交失败，请重试' }
    } else if (endpoint === '/lostfound/claim/my') {
      body.data = [{ claimId: 1, postId: 91, postTitle: post.title, postType: 'LOST',
        myRole: 'owner', otherPartyName: '另一位同学', status: state.claimStatus,
        applicantConfirmed: '0', ownerConfirmed: '0', evidenceText: '收纳盒上有一枚蓝色贴纸。',
        updateTime: '2026-09-01 10:00:00' }]
    } else if (endpoint === '/lostfound/claim/1/reject') {
      state.claimRejects.push(request.postDataJSON())
      state.claimStatus = 'REJECTED'
    } else if (endpoint.startsWith('/lostfound/private-message/with/')) {
      body.data = [{ messageId: 4, fromUserId: 20, toUserId: 10, content: '你好，请问耳机是在二楼捡到的吗？',
        createTime: '2026-09-01 10:00:00' }]
    } else if (endpoint === '/lostfound/private-message/conversations') {
      body.data = [{ toUserId: 20, nickname: '另一位同学', unread: 0,
        lastMsg: '你好，请问耳机是在二楼捡到的吗？', lastTime: '2026-09-01 10:00:00' }]
    } else if (endpoint === '/lostfound/message/unread-count') {
      body.data = 0
    } else if (endpoint === '/lostfound/message') {
      body.data = [{ messageId: 7, title: '测试通知', content: '这是一条测试通知。',
        isRead: 0, relatedType: 'POST', relatedId: 91, createTime: '2026-09-01 11:00:00' }]
    } else if (/^\/lostfound\/message\/\d+\/read$/.test(endpoint)) {
      state.markReads.push(endpoint)
    } else if (endpoint.startsWith('/lostfound/comment/post/')) {
      body.data = []
    } else if (endpoint === '/getInfo') {
      body.user = { userId: 10, userName: '同学', nickName: '同学' }
      body.roles = ['common']
    } else if (endpoint === '/system/user/profile/avatar') {
      state.avatarUploads.push(request.postDataBuffer()?.length || 0)
      body.imgUrl = '/profile/avatar.png'
    } else if (endpoint === '/system/user/profile' && method === 'PUT') {
      state.profileUpdates.push(request.postDataJSON())
    } else if (endpoint === '/logout') {
      state.logoutCalls.push(1)
    } else if (endpoint === '/profile/avatar.png') {
      await route.fulfill({ status: 200, contentType: 'image/png',
        body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64') })
      return
    } else {
      pageErrors.push(`Unexpected API request: ${method} ${endpoint}`)
    }
    await route.fulfill({ json: body })
  })
  const page = await context.newPage()
  page.on('pageerror', error => pageErrors.push(error.message))
  return { page, context, state }
}

async function capture(page, name, folder = 'after') {
  await mkdir(path.join(output, folder), { recursive: true })
  await page.evaluate(() => document.fonts.ready)
  await page.screenshot({ path: path.join(output, folder, `${name}.png`),
    fullPage: true, animations: 'disabled' })
}

async function noOverflow(page, label = '') {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)
  assert.equal(
    overflow,
    false,
    `Horizontal overflow detected${label ? ` on ${label}` : ''}: ${JSON.stringify(overflow)}`,
  )
}

async function dialogFits(page) {
  await noOverflow(page)
  const dialog = page.getByRole('dialog')
  const box = await dialog.boundingBox()
  const viewport = page.viewportSize()
  assert.ok(box.x >= 0 && box.x + box.width <= viewport.width, 'Dialog must fit horizontally')
  assert.ok(box.y >= 0 && box.y + box.height <= viewport.height, 'Dialog must fit vertically')
  assert.equal(await dialog.evaluate(el => el.scrollWidth <= el.clientWidth), true)
}

async function submitTwice(form) {
  await form.evaluate(el => { el.requestSubmit(); el.requestSubmit() })
}

async function interactionTests(width) {
  const viewport = { width, height: width === 1440 ? 1000 : 844 }
  {
    const { page, context, state } = await scenario(viewport)
    state.fail = 'governance'
    await page.goto(`${base}/governance`)
    await page.locator('.account-state[role="alert"]').waitFor()
    assert.equal(await page.getByText('暂无处罚记录', { exact: true }).count(), 0)
    state.fail = ''
    await page.getByRole('button', { name: '重试', exact: true }).click()
    await page.getByRole('heading', { name: '限制发布', exact: true }).waitFor()
    await page.getByText(appeals[0].reviewResult, { exact: true }).waitFor()
    assert.equal(await page.locator('.account-record-footer button').count(), 1)
    await page.getByRole('button', { name: '提交申诉', exact: true }).click()
    const dialog = page.getByRole('dialog')
    const field = page.getByLabel('申诉理由', { exact: false })
    assert.equal(await field.getAttribute('maxlength'), '3000')
    await field.fill('   ')
    await dialog.getByRole('button', { name: '提交申诉', exact: true }).click()
    await dialog.getByText('请填写申诉理由', { exact: true }).waitFor()
    assert.equal(state.appealRequests.length, 0)
    const reason = '请复核本次发布记录，以下是补充说明。'.repeat(30)
    await field.fill(reason)
    await dialogFits(page)
    await capture(page, `appeal-dialog-${width}`)
    state.fail = 'appeal'
    await dialog.getByRole('button', { name: '提交申诉', exact: true }).click()
    await dialog.getByRole('alert').waitFor()
    assert.equal(await field.inputValue(), reason)
    state.fail = ''
    await submitTwice(page.locator('#appeal-form'))
    await dialog.waitFor({ state: 'hidden' })
    await page.locator('.account-record-text').getByText(reason, { exact: true }).waitFor()
    assert.equal(state.appealRequests.length, 2, 'Only one failed and one successful appeal request')
    assert.deepEqual(state.appealRequests.at(-1), { actionId: 1, reason })
    assert.equal(await page.locator('.account-record-footer button').count(), 0)
    await noOverflow(page)
    state.actions = []
    state.appeals = []
    await page.getByRole('button', { name: '刷新记录', exact: true }).click()
    await page.getByText('暂无处罚记录', { exact: true }).waitFor()
    await page.getByText('暂无申诉记录', { exact: true }).waitFor()
    await capture(page, `governance-empty-${width}`)
    await context.close()
  }
  {
    const { page, context, state } = await scenario(viewport)
    await page.goto(`${base}/profile`)
    await page.getByRole('heading', { name: '个人中心', exact: true }).waitFor()
    await page.locator('input[type="file"][accept*="image/jpeg"]').setInputFiles({
      name: 'avatar.png', mimeType: 'image/png',
      buffer: Buffer.from('test-avatar'),
    })
    await page.getByText('头像已更新', { exact: true }).waitFor()
    assert.equal(state.avatarUploads.length, 1)
    await page.getByRole('button', { name: '编辑资料', exact: true }).click()
    await page.getByRole('dialog').getByLabel('昵称').fill('新昵称')
    await page.getByRole('dialog').getByRole('button', { name: '保存', exact: true }).click()
    assert.deepEqual(state.profileUpdates.at(-1), { nickName: '新昵称', email: '', phonenumber: '' })
    await page.goto(`${base}/messages`)
    await page.getByRole('heading', { name: /私信/ }).waitFor()
    await page.getByRole('button', { name: '展开', exact: true }).click()
    await page.getByText('测试通知', { exact: true }).click()
    assert.deepEqual(state.markReads, ['/lostfound/message/7/read'])
    await page.goto(`${base}/messages`)
    await page.getByRole('heading', { name: /私信/ }).waitFor()
    await page.getByText('另一位同学', { exact: true }).click()
    await page.waitForURL(`${base}/chat?userId=20`)
    await page.getByText('你好，请问耳机是在二楼捡到的吗？', { exact: true }).waitFor()
    await page.locator('.header-logout-btn').click()
    await page.waitForURL(`${base}/login`)
    assert.equal(state.logoutCalls.length, 1)
    await context.close()
  }
  {
    const { page, context, state } = await scenario(viewport)
    state.fail = 'posts'
    await page.goto(`${base}/my-posts`)
    await page.locator('.account-state[role="alert"]').waitFor()
    assert.equal(await page.getByText('还没有发布过帖子', { exact: true }).count(), 0)
    state.fail = ''
    await page.getByRole('button', { name: '重试', exact: true }).click()
    await page.locator('.account-record').first().waitFor()
    assert.equal(await page.getByTitle('编辑并重新提交审核', { exact: true }).count(), 2)
    assert.equal(await page.getByTitle('撤回', { exact: true }).count(), 3)
    state.fail = 'categories'
    await page.getByTitle('编辑并重新提交审核', { exact: true }).first().click()
    const dialog = page.getByRole('dialog')
    await dialog.getByText('分类加载失败，请重试。', { exact: true }).waitFor()
    assert.equal(await dialog.getByRole('button', { name: '保存并提交审核' }).isDisabled(), true)
    state.fail = ''
    await dialog.getByRole('button', { name: '重试', exact: true }).click()
    const title = page.getByPlaceholder('请输入帖子标题')
    await title.fill('')
    await dialog.getByRole('button', { name: '保存并提交审核' }).click()
    await dialog.getByText('请输入帖子标题', { exact: true }).waitFor()
    assert.equal(state.edits.length, 0)
    await title.fill('  修改后的帖子标题  ')
    await dialogFits(page)
    await capture(page, `edit-dialog-${width}`)
    state.fail = 'edit'
    await dialog.getByRole('button', { name: '保存并提交审核' }).click()
    await dialog.locator('.dialog-error').waitFor()
    assert.equal(await title.inputValue(), '  修改后的帖子标题  ')
    assert.equal(state.submissions.length, 0)
    state.fail = ''
    await submitTwice(dialog.locator('form'))
    await dialog.waitFor({ state: 'hidden' })
    assert.equal(state.edits.length, 2)
    assert.equal(state.submissions.length, 1)
    assert.equal(state.edits.at(-1).title, '修改后的帖子标题')
    assert.equal(state.edits.at(-1).itemCategoryId, 1)
    await page.getByTitle('撤回', { exact: true }).first().click()
    await page.getByRole('button', { name: '取消', exact: true }).click()
    assert.equal(state.withdrawals.length, 0)
    await page.getByTitle('撤回', { exact: true }).first().click()
    await page.getByRole('button', { name: '确定', exact: true }).click()
    await page.locator('.account-badge').getByText('已撤回', { exact: true }).waitFor()
    assert.equal(state.withdrawals.length, 1)
    state.posts = []
    await page.getByRole('button', { name: '刷新帖子', exact: true }).click()
    await page.getByText('还没有发布过帖子', { exact: true }).waitFor()
    await capture(page, `my-posts-empty-${width}`)
    await context.close()
  }
  {
    const { page, context, state } = await scenario(viewport)
    await page.goto(`${base}/claims`)
    await page.getByText('认领依据', { exact: true }).waitFor()
    await page.getByRole('button', { name: '拒绝认领', exact: true }).click()
    const dialog = page.getByRole('dialog')
    await dialog.getByPlaceholder('请输入拒绝原因').fill('无法核实认领人提供的物品特征。')
    await dialog.getByRole('button', { name: '确认拒绝', exact: true }).click()
    assert.deepEqual(state.claimRejects.at(-1), { ownerRemark: '无法核实认领人提供的物品特征。' })
    await page.locator('.tag-status').getByText('已拒绝', { exact: true }).waitFor()
    await context.close()
  }
  for (const [url, selector, targetType, targetId, reasonCode] of [
    ['/post/91', '.report-btn', 'POST', 91, 'OTHER'],
    ['/chat?userId=20', '.report-message-btn', 'PRIVATE_MESSAGE', 4, 'ABUSE'],
    ['/claims', '.claim-report-button', 'CLAIM', 1, 'ABUSE'],
  ]) {
    const { page, context, state } = await scenario(viewport)
    await page.goto(`${base}${url}`)
    await page.locator(selector).click()
    const dialog = page.getByRole('dialog')
    const field = page.getByLabel('举报说明', { exact: false })
    assert.equal(await field.getAttribute('maxlength'), '2000')
    await field.fill('   ')
    await dialog.getByRole('button', { name: '提交举报' }).click()
    await dialog.getByText('请填写举报说明', { exact: true }).waitFor()
    assert.equal(state.reports.length, 0)
    const description = '请核查本条记录，补充事实如下。'.repeat(40)
    await field.fill(description)
    await dialogFits(page)
    await capture(page, `report-${targetType.toLowerCase()}-${width}`)
    state.fail = 'report'
    await dialog.getByRole('button', { name: '提交举报' }).click()
    await dialog.getByRole('alert').waitFor()
    assert.equal(await field.inputValue(), description)
    state.fail = ''
    await submitTwice(page.locator('#report-form'))
    await dialog.waitFor({ state: 'hidden' })
    assert.equal(state.reports.length, 2)
    assert.deepEqual(state.reports.at(-1), { targetType, targetId, reasonCode, description })
    await page.locator(selector).click()
    assert.equal(await field.inputValue(), '')
    await dialog.getByRole('button', { name: '取消', exact: true }).click()
    await dialog.waitFor({ state: 'hidden' })
    assert.equal(state.reports.length, 2)
    await context.close()
  }
  console.log(`PASS: appeal, edit, withdrawal, three report targets, errors/retry and empty states at ${width}px.`)
}

try {
  const references = [
    ['square', '/'], ['publish', '/publish'], ['messages', '/messages'],
    ['detail', '/post/91'], ['chat', '/chat?userId=20'], ['profile', '/profile'],
  ]
  for (const width of [1440, 390, 320]) {
    const { page, context } = await scenario({ width, height: width === 1440 ? 1000 : 844 })
    for (const [name, url] of references) {
      await page.goto(`${base}${url}`)
      await page.waitForLoadState('networkidle')
      await capture(page, `${name}-${width}`, baseline ? 'baseline' : 'after')
      if (!baseline) await noOverflow(page, `${name}-${width}`)
    }
    for (const [name, url] of [['governance', '/governance'], ['my-posts', '/my-posts'], ['claims', '/claims']]) {
      await page.goto(`${base}${url}`)
      await page.waitForLoadState('networkidle')
      await capture(page, `${name}-${width}`, baseline ? 'baseline' : 'after')
      if (!baseline && name !== 'claims') await noOverflow(page)
    }
    await context.close()
  }
  if (!baseline) for (const width of [1440, 390, 320]) await interactionTests(width)
  assert.deepEqual(pageErrors, [])
  console.log(`PASS: ${baseline ? 'baseline captured' : 'page captures and overflow checks passed'} at 1440, 390 and 320px.`)
  console.log(`Screenshots: ${output}`)
  console.log('Authenticated calls were mocked; no test data was written to the live database.')
} finally {
  await browser.close()
}
