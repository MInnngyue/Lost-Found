<script setup>
import { ref, onBeforeUnmount, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from 'lucide-vue-next'
import { getPosts } from '@/api/post'
import { getCategories } from '@/api/category'
import { imageUrl } from '@/utils/url'

const router = useRouter()
const posts = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

const activeTab = ref(null)
const queryParams = ref({ type: null, itemCategoryId: null, keyword: '' })

const itemCategories = ref([])
const catExpanded = ref(false)
const placeholders = ['校园卡', '电子产品', '钱包', '钥匙', '书包', '水杯']
const currentPlaceholder = ref(0)
const glowX = ref(0)
const glowY = ref(0)
const glowActive = ref(false)
const glowTargetX = ref(0)
const glowTargetY = ref(0)
const glowCurrentX = ref(0)
const glowCurrentY = ref(0)
let glowTicking = false
let revealObserver = null
let placeholderTimer = null

function observeRevealElements() {
  const pageRoot = document.querySelector('.post-square')
  const elements = pageRoot?.querySelectorAll('[data-reveal]:not(.is-visible)') || []

  if (!revealObserver) {
    elements.forEach((element) => element.classList.add('is-visible'))
    return
  }

  elements.forEach((element) => revealObserver.observe(element))
}

function initRevealObserver() {
  if (!('IntersectionObserver' in window)) return
  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return
        entry.target.classList.add('is-visible')
        revealObserver?.unobserve(entry.target)
      })
    },
    { threshold: 0.12, rootMargin: '0px 0px -36px' },
  )
}

onMounted(async () => {
  initRevealObserver()
  placeholderTimer = window.setInterval(() => {
    currentPlaceholder.value = (currentPlaceholder.value + 1) % placeholders.length
  }, 2000)
  await nextTick()
  observeRevealElements()
  itemCategories.value = (await getCategories('item_category')).data
  fetchPosts()
})

onBeforeUnmount(() => {
  revealObserver?.disconnect()
  if (placeholderTimer !== null) window.clearInterval(placeholderTimer)
})

async function fetchPosts() {
  loading.value = true
  try {
    const params = { ...queryParams.value, page: page.value, pageSize: pageSize.value }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null) delete params[k]
    })
    const res = await getPosts(params)
    posts.value = res.data.records
    total.value = res.data.total
    await nextTick()
    observeRevealElements()
  } finally {
    loading.value = false
  }
}

function onTabChange(type) {
  activeTab.value = type
  queryParams.value.type = type
  page.value = 1
  fetchPosts()
}
function onSearch() {
  page.value = 1
  fetchPosts()
}
function onFilterClick(key, val) {
  if (queryParams.value[key] === val) {
    delete queryParams.value[key]
  } else {
    queryParams.value[key] = val
  }
  page.value = 1
  fetchPosts()
}
function goDetail(id) {
  router.push(`/post/${id}`)
}
function goPublish() {
  router.push('/publish')
}
function onPageChange(p) {
  page.value = p
  fetchPosts()
}

function onMouseMove(event) {
  glowTargetX.value = event.clientX
  glowTargetY.value = event.clientY
  glowActive.value = true
  if (!glowTicking) {
    glowTicking = true
    requestAnimationFrame(animateGlow)
  }
}

function animateGlow() {
  const ease = 0.15
  glowCurrentX.value += (glowTargetX.value - glowCurrentX.value) * ease
  glowCurrentY.value += (glowTargetY.value - glowCurrentY.value) * ease
  glowX.value = glowCurrentX.value
  glowY.value = glowCurrentY.value
  if (
    Math.abs(glowTargetX.value - glowCurrentX.value) > 0.5 ||
    Math.abs(glowTargetY.value - glowCurrentY.value) > 0.5
  ) {
    requestAnimationFrame(animateGlow)
  } else {
    glowTicking = false
  }
}

function onMouseLeave() {
  glowActive.value = false
}

function typeLabel(type) {
  return type === 0 ? '寻物' : '招领'
}
function statusLabel(status) {
  const map = { DRAFT: '草稿', PENDING_REVIEW: '待审核', PUBLISHED: '已发布', RESOLVED: '已解决', EXPIRED: '已过期', REJECTED: '审核未通过', WITHDRAWN: '已撤回', REMOVED: '已下架', 0: '进行中', 1: '已匹配', 2: '认领中', 3: '已完结', 4: '已归档', 5: '已下架' }
  return map[status] || '未知'
}
</script>

<template>
  <div class="page post-square" @mousemove="onMouseMove" @mouseleave="onMouseLeave">
    <div
      class="aurora-glow"
      :class="{ active: glowActive }"
      :style="{ transform: `translate3d(${glowX - 150}px, ${glowY - 100}px, 0)` }"
      aria-hidden="true"
    />
    <div
      class="aurora-glow aurora-glow--teal"
      :class="{ active: glowActive }"
      :style="{ transform: `translate3d(${glowX - 130}px, ${glowY - 160}px, 0)` }"
      aria-hidden="true"
    />
    <div class="square-inner">
      <div class="main-layout">
        <aside class="sidebar" data-reveal style="--reveal-delay: 90ms">
          <section class="filter-card search-card">
            <div class="site-search">
              <Search class="site-search-icon" :size="19" aria-hidden="true" />
              <el-input
                v-model="queryParams.keyword"
                class="search-input"
                :placeholder="placeholders[currentPlaceholder]"
                clearable
                @keyup.enter="onSearch"
                @clear="onSearch"
              />
              <button type="button" class="site-search-button" @click="onSearch">搜索</button>
            </div>
          </section>

          <section class="filter-card">
            <h2 class="filter-title">类型</h2>
            <div class="chip-list">
              <button
                type="button"
                class="chip"
                :class="{ active: activeTab === null }"
                @click="onTabChange(null)"
              >
                全部
              </button>
              <button
                type="button"
                class="chip"
                :class="{ active: activeTab === 0 }"
                @click="onTabChange(0)"
              >
                寻物
              </button>
              <button
                type="button"
                class="chip"
                :class="{ active: activeTab === 1 }"
                @click="onTabChange(1)"
              >
                招领
              </button>
            </div>
          </section>

          <section class="filter-card">
            <h2 class="filter-title">分类</h2>
            <div
              class="category-list"
              :class="{ collapsed: !catExpanded && itemCategories.length > 5 }"
            >
              <div class="chip-list">
                <button
                  v-for="c in catExpanded ? itemCategories : itemCategories.slice(0, 5)"
                  :key="c.id"
                  type="button"
                  class="chip"
                  :class="{ active: queryParams.itemCategoryId === c.id }"
                  @click="onFilterClick('itemCategoryId', c.id)"
                >
                  {{ c.name }}
                </button>
              </div>
            </div>
            <button
              v-if="itemCategories.length > 5"
              type="button"
              class="toggle-more-btn"
              @click="catExpanded = !catExpanded"
            >
              {{ catExpanded ? '收起' : '更多' }}
            </button>
          </section>
        </aside>

        <div class="content-col">
          <header class="page-header" data-reveal>
            <div>
              <h1 class="page-title">失物广场</h1>
              <p class="page-subtitle">在这里找到你丢失的，或拾到他人遗失的</p>
            </div>
            <div class="total-block">
              <span class="total-prefix">共</span>
              <span class="total-count">{{ total }}</span>
              <span class="total-label">条帖子</span>
            </div>
          </header>

          <main class="content-area" v-loading="loading" data-reveal style="--reveal-delay: 160ms">
            <div v-if="posts.length === 0 && !loading" class="empty-state" data-reveal>
              <svg
                class="empty-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <path d="M20 7h-4a2 2 0 0 1-2-2V1" />
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z" />
                <path d="M8 13h8M8 17h5" />
              </svg>
              <p class="empty-text">广场暂时空空如也</p>
              <p class="empty-hint">发一条寻物或招领，帮自己也帮别人</p>
              <button type="button" class="empty-btn" @click="goPublish">去发布帖子</button>
            </div>

            <div v-else class="card-grid">
              <article
                v-for="(post, index) in posts"
                :key="post.id"
                class="post-card"
                tabindex="0"
                data-reveal
                :style="{ '--reveal-delay': `${Math.min(index, 5) * 65}ms` }"
                @click="goDetail(post.id)"
                @keyup.enter="goDetail(post.id)"
              >
                <div class="card-cover">
                  <el-image
                    v-if="post.coverImage"
                    :src="imageUrl(post.coverImage)"
                    fit="cover"
                    class="cover-img"
                  />
                  <div v-else class="cover-placeholder">
                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      aria-hidden="true"
                    >
                      <rect x="3" y="3" width="18" height="18" rx="2" />
                      <circle cx="8.5" cy="8.5" r="1.5" />
                      <path d="m21 15-5-5L5 21" />
                    </svg>
                  </div>
                </div>

                <div class="card-body">
                  <div class="card-tags">
                    <span class="tag-type" :class="post.type === 0 ? 'lost' : 'found'">{{
                      typeLabel(post.type)
                    }}</span>
                    <span class="tag-status" :class="'s-' + post.status">{{
                      statusLabel(post.status)
                    }}</span>
                  </div>
                  <h2 class="card-title">{{ post.title || '无标题' }}</h2>
                  <div class="card-meta">
                    <span>{{ post.itemCategory }}</span>
                    <span class="divider">·</span>
                    <span>{{ post.locationCampus }}</span>
                    <span class="divider">·</span>
                    <time>{{ post.lostTime?.substring(0, 10) }}</time>
                  </div>
                </div>
              </article>
            </div>

            <div v-if="total > pageSize" class="pagination">
              <el-pagination
                background
                layout="prev, pager, next"
                :total="total"
                :page-size="pageSize"
                v-model:current-page="page"
                @current-change="onPageChange"
              />
            </div>
          </main>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.post-square {
  --bg-page: transparent;
  --bg-card: #ffffff;
  --bg-sidebar: rgba(255, 255, 255, 0.72);
  --bg-glass: rgba(255, 255, 255, 0.86);
  --bg-search: rgba(255, 255, 255, 0.46);
  --glass-border: rgba(255, 255, 255, 0.62);
  --search-border: rgba(15, 23, 42, 0.1);
  --ink: #0f172a;
  --ink-hover: #000000;
  --primary: #3b82f6;
  --primary-hover: #2563eb;
  --primary-light: #eff6ff;
  --gray-50: #f8fafc;
  --gray-100: #f1f5f9;
  --gray-200: #e2e8f0;
  --gray-300: #cbd5e1;
  --gray-400: #94a3b8;
  --gray-500: #64748b;
  --gray-600: #475569;
  --gray-700: #334155;
  --gray-800: #1e293b;
  --type-lost: #ef4444;
  --type-lost-bg: #fef2f2;
  --type-found: #10b981;
  --type-found-bg: #f0fdf4;
  --status-active: #3b82f6;
  --status-active-bg: #eff6ff;
  --status-matched: #f59e0b;
  --status-matched-bg: #fffbeb;
  --status-done: #10b981;
  --status-done-bg: #f0fdf4;
  --status-archived: #6b7280;
  --status-removed: #9ca3af;
  --status-muted-bg: #f3f4f6;
  --transparent: transparent;
  --focus-ring: rgba(59, 130, 246, 0.1);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.08);
  --shadow-filter: 0 12px 32px rgba(34, 48, 132, 0.08);
  --shadow-search: 0 12px 28px rgba(34, 48, 132, 0.1);
  --shadow-glass: 0 34px 90px rgba(34, 48, 132, 0.16), inset 0 1px 0 rgba(255, 255, 255, 0.8);
  --shadow-capsule: 0 12px 28px rgba(15, 23, 42, 0.24);
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-full: 9999px;
  --font-heading: 'Noto Serif SC', serif;
  --font-body: 'HarmonyOS Sans', sans-serif;
  --font-mono: 'JetBrains Mono', monospace;

  min-height: calc(100vh - 60px);
  position: relative;
  isolation: isolate;
  padding: 31px;
  color: var(--gray-600);
  font-family: var(--font-body);
  font-size: 1.1rem;
}

/* 极光 Aurora 双光团 */
.aurora-glow {
  position: fixed;
  width: 300px;
  height: 300px;
  left: 0;
  top: 0;
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
  will-change: transform;
  opacity: 0;
  transition: opacity 0.5s ease;
  filter: blur(40px);
  background: radial-gradient(circle, rgba(139, 92, 246, 0.3) 0%, rgba(139, 92, 246, 0.1) 40%, transparent 70%);
  mix-blend-mode: normal;
}

.aurora-glow--teal {
  width: 260px;
  height: 260px;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.25) 0%, rgba(6, 182, 212, 0.08) 40%, transparent 70%);
  mix-blend-mode: screen;
}

.aurora-glow.active {
  opacity: 1;
}

.square-inner {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1540px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding-bottom: 18px;
  margin-bottom: 26px;
}

.page-title {
  margin: 0;
  color: var(--gray-800);
  font-family: var(--font-heading);
  font-size: 36px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: 0;
}

.page-subtitle {
  margin: 4px 0 0;
  color: var(--gray-500);
  font-size: 16px;
  line-height: 1.6;
}
.total-block {
  display: flex;
  gap: 6px;
  align-items: baseline;
  white-space: nowrap;
}
.total-prefix,
.total-label {
  color: var(--gray-500);
  font-size: 15.4px;
}
.total-label {
  font-size: 16px;
}
.total-count {
  color: var(--primary);
  font-family: var(--font-mono);
  font-size: 36px;
  font-weight: 700;
}

.main-layout {
  display: flex;
  gap: 26px;
  align-items: flex-start;
}
.content-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.sidebar,
.content-area {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.62);
  border-radius: 24px;
  box-shadow:
    0 34px 90px rgba(34, 48, 132, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
}
.sidebar {
  position: sticky;
  top: 76px;
  display: flex;
  width: 280px;
  padding: 14px;
  flex: 0 0 280px;
  flex-direction: column;
  gap: 14px;
  transition: transform 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
}
.post-card,
.filter-card {
  background: rgba(255, 255, 255, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
}
.filter-card {
  padding: 18px;
}
.search-card {
  padding: 0;
  background: var(--transparent);
  border: 0;
  box-shadow: none;
  backdrop-filter: none;
}
.filter-title {
  margin: 0 0 10px;
  color: var(--gray-800);
  font-family: var(--font-heading);
  font-size: 17.6px;
  font-weight: 600;
  line-height: 1.5;
  letter-spacing: 0;
}
.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 6px;
}
.chip {
  min-height: 35px;
  padding: 7px 17px;
  color: var(--gray-600);
  font-family: var(--font-body);
  font-size: 15.4px;
  line-height: 1.2;
  background: var(--bg-card);
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all 0.2s;
}
.sidebar .chip:hover {
  color: var(--ink);
  background: var(--primary-light);
  border-color: var(--ink);
}
.sidebar .chip.active {
  color: var(--bg-card);
  background: var(--ink);
  border-color: var(--ink);
}
.category-list {
  overflow: hidden;
}
.category-list.collapsed {
  max-height: 192px;
}
.sidebar .toggle-more-btn {
  display: block;
  width: 100%;
  padding: 8px 0 0;
  margin-top: 4px;
  color: var(--ink);
  font-family: var(--font-body);
  font-size: 14.3px;
  font-weight: 600;
  background: var(--transparent);
  border: 0;
  cursor: pointer;
}
.sidebar .toggle-more-btn:hover {
  color: var(--ink-hover);
}

.content-area {
  min-width: 0;
  min-height: 360px;
  padding: 22px;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}
.post-card {
  min-width: 0;
  min-height: 320px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}
.post-card:hover,
.post-card:focus-visible {
  border-color: var(--gray-300);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  outline: none;
}
.card-cover {
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: var(--gray-100);
}
.cover-img {
  display: block;
  width: 100%;
  height: 100%;
}
.cover-img :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s ease;
}
.post-card:hover .cover-img :deep(img) {
  transform: scale(1.02);
}
.cover-placeholder {
  display: flex;
  width: 100%;
  height: 180px;
  align-items: center;
  justify-content: center;
  color: var(--gray-300);
  background: var(--gray-100);
}
.cover-placeholder svg {
  width: 40px;
  height: 40px;
}
.card-body {
  min-height: 139px;
  padding: 15px 17px;
}
.card-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}
.tag-type,
.tag-status {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 4px 11px;
  border-radius: var(--radius-full);
  font-size: 13.2px;
  font-weight: 600;
  line-height: 1.3;
}
.tag-type.lost {
  color: var(--type-lost);
  background: var(--type-lost-bg);
}
.tag-type.found {
  color: var(--type-found);
  background: var(--type-found-bg);
}
.tag-status.s-0 {
  color: var(--status-active);
  background: var(--status-active-bg);
}
.tag-status.s-1,
.tag-status.s-2 {
  color: var(--status-matched);
  background: var(--status-matched-bg);
}
.tag-status.s-3 {
  color: var(--status-done);
  background: var(--status-done-bg);
}
.tag-status.s-4 {
  color: var(--status-archived);
  background: var(--status-muted-bg);
}
.tag-status.s-5 {
  color: var(--status-removed);
  background: var(--status-muted-bg);
}
.card-title {
  display: -webkit-box;
  min-height: 49.3px;
  margin: 0 0 8px;
  overflow: hidden;
  color: var(--gray-800);
  font-family: var(--font-heading);
  font-size: 17.6px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: 0;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.card-meta {
  display: flex;
  min-width: 0;
  align-items: center;
  overflow: hidden;
  color: var(--gray-400);
  font-size: 14.3px;
  line-height: 1.5;
  white-space: nowrap;
}
.card-meta > span:not(.divider) {
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-meta .divider {
  flex: none;
  margin: 0 4px;
  color: var(--gray-300);
}
.card-meta time {
  flex: none;
  font-family: var(--font-mono);
}

.empty-state {
  padding: 72px 22px;
  text-align: center;
}
.empty-icon {
  display: block;
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  color: var(--gray-300);
}
.empty-text {
  margin: 0 0 8px;
  color: var(--gray-500);
  font-size: 17.6px;
}
.empty-hint {
  margin: 0 0 20px;
  color: var(--gray-400);
  font-size: 15.4px;
}
.empty-btn {
  padding: 12px 28px;
  color: var(--bg-card);
  font-family: var(--font-body);
  font-size: 15.4px;
  font-weight: 700;
  background: var(--ink);
  border: 0;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition:
    background-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}
.empty-btn:hover {
  background: var(--ink-hover);
  box-shadow: var(--shadow-capsule);
  transform: translateY(-2px);
}
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.site-search {
  display: flex;
  width: 100%;
  min-width: 0;
  height: 50px;
  padding: 5px 6px 5px 15px;
  align-items: center;
  gap: 8px;
  background: var(--bg-search);
  border: 1px solid var(--search-border);
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-search);
  backdrop-filter: blur(12px) saturate(160%);
  -webkit-backdrop-filter: blur(12px) saturate(160%);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}
.sidebar .site-search:focus-within {
  border-color: var(--ink);
  box-shadow: 0 0 0 4px rgba(15, 23, 42, 0.1);
}
.site-search-icon {
  flex: none;
  color: var(--gray-400);
}
.search-input {
  min-width: 0;
  flex: 1;
}
.search-input :deep(.el-input__wrapper),
.search-input :deep(.el-input__wrapper:hover),
.search-input :deep(.el-input__wrapper.is-focus) {
  min-height: 38px;
  padding: 0;
  background: var(--transparent);
  border-radius: 0;
  box-shadow: none;
}
.search-input :deep(.el-input__inner) {
  color: var(--gray-700);
  font-family: var(--font-body);
  font-size: 15.4px;
}
.search-input :deep(.el-input__inner::placeholder),
.search-input :deep(.el-input__prefix),
.search-input :deep(.el-input__clear) {
  color: var(--gray-400);
}
.site-search-button {
  min-width: 64px;
  height: 38px;
  padding: 0 17px;
  flex: none;
  color: var(--bg-card);
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 800;
  background: var(--ink);
  border: 0;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition:
    background-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}
.site-search-button:hover,
.site-search-button:focus-visible {
  background: var(--ink-hover);
  box-shadow: var(--shadow-capsule);
  transform: translateY(-1px);
  outline: none;
}
:deep(.el-pagination button),
:deep(.el-pagination .el-pager li) {
  color: var(--gray-600);
  background: var(--bg-card) !important;
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-sm);
}
:deep(.el-pagination .el-pager li:hover) {
  color: var(--primary);
}
:deep(.el-pagination .el-pager li.is-active) {
  color: var(--bg-card);
  background: var(--primary) !important;
  border-color: var(--primary);
}

[data-reveal] {
  opacity: 0;
  transform: translate3d(0, 30px, 0) scale(0.985);
  transition:
    opacity 760ms cubic-bezier(0.16, 1, 0.3, 1),
    transform 760ms cubic-bezier(0.16, 1, 0.3, 1);
  transition-delay: var(--reveal-delay, 0ms);
}

.sidebar[data-reveal] {
  transition:
    opacity 760ms cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
}

[data-reveal].is-visible {
  opacity: 1;
  transform: translate3d(0, 0, 0) scale(1);
}

[data-reveal].is-visible.post-card:hover,
[data-reveal].is-visible.post-card:focus-visible {
  transform: translateY(-2px);
}

:deep(.el-loading-mask) {
  background-color: var(--bg-glass);
  border-radius: var(--radius-lg);
  backdrop-filter: blur(8px);
}

@media (min-width: 769px) and (max-width: 1280px) {
  .post-square {
    padding: 26px 24px;
  }
  .main-layout {
    gap: 20px;
  }
  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .post-square {
    min-height: calc(100vh - 108px);
    padding: 24px 16px 36px;
  }
  .page-header {
    align-items: flex-start;
  }
  .main-layout {
    flex-direction: column;
  }
  .sidebar {
    position: static;
    display: grid;
    width: 100%;
    flex: none;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .search-card {
    grid-column: 1 / -1;
  }
  .content-area {
    width: 100%;
    padding: 18px;
  }
  .card-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .page-header {
    flex-direction: column;
    gap: 12px;
  }
  .total-count {
    font-size: 27px;
  }
  .page-title {
    font-size: 29px;
  }
  .sidebar {
    grid-template-columns: 1fr;
  }
  .search-card {
    grid-column: auto;
  }
  .content-area {
    padding: 12px;
  }
  .site-search {
    padding-left: 13px;
  }
  .site-search-button {
    min-width: 58px;
    padding: 0 14px;
  }
}

@media (prefers-reduced-motion: reduce) {
  [data-reveal],
  [data-reveal].is-visible,
  [data-reveal].is-visible.post-card:hover,
  [data-reveal].is-visible.post-card:focus-visible {
    opacity: 1;
    transform: none;
    transition: none;
  }
}
</style>
