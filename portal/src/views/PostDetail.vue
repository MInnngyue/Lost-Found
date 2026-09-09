<script setup>
import { ref, onBeforeUnmount, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Clock3, Flag, Mail, MapPin, Tag, Trash2, UserRoundCheck } from 'lucide-vue-next'
import { getPostDetail, withdrawPost } from '@/api/post'
import { createClaim } from '@/api/claim'
import { getComments, addComment } from '@/api/comment'
import ReportDialog from '@/components/ReportDialog.vue'
import { imageUrl } from '@/utils/url'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const post = ref(null)
const loading = ref(true)
const comments = ref([])
const commentText = ref('')
const commentSubmitting = ref(false)
const reportTarget = ref(null)
let revealObserver = null

function observeRevealElements() {
  const pageRoot = document.querySelector('.detail-page')
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
  try {
    post.value = (await getPostDetail(route.params.id)).data
    comments.value = (await getComments(route.params.id)).data || []
  } finally {
    loading.value = false
  }
  await nextTick()
  observeRevealElements()
})

onBeforeUnmount(() => revealObserver?.disconnect())

function typeLabel(type) {
  return type === 0 ? '寻物启事' : '失物招领'
}
function statusLabel(status) {
  const map = { DRAFT: '草稿', PENDING_REVIEW: '待审核', PUBLISHED: '已发布', RESOLVED: '已解决', EXPIRED: '已过期', REJECTED: '审核未通过', WITHDRAWN: '已撤回', REMOVED: '已下架', 0: '进行中', 1: '已匹配', 2: '认领中', 3: '已完结', 4: '已归档', 5: '已下架' }
  return map[status] || '未知'
}
function currentUserId() {
  const info = localStorage.getItem('userInfo')
  if (!info) return null
  try {
    return JSON.parse(info).userId || JSON.parse(info).id
  } catch {
    return null
  }
}
function isOwner() {
  if (!post.value) return false
  return post.value.userId === currentUserId()
}
function isWithdrawable(status) {
  return ['DRAFT', 'PENDING_REVIEW', 'PUBLISHED'].includes(status)
}
function publisherName() {
  return post.value?.nickname || post.value?.createBy || '校园用户'
}

async function handleWithdraw() {
  try {
    await ElMessageBox.confirm('撤回后帖子将不再出现在广场中，确定继续吗？', '确认撤回', { type: 'warning' })
    await withdrawPost(post.value.id)
    ElMessage.success('帖子已撤回')
    router.push('/my-posts')
  } catch {
    // 用户取消时无需后续操作。
  }
}

function goChat(otherId) {
  router.push({ path: '/chat', query: { userId: otherId } })
}

async function handleClaim() {
  try {
    const { value } = await ElMessageBox.prompt(
      '请描述只有物品持有人才知道的特征，例如内部标签、磨损位置或物品内容。',
      '提交认领申请',
      {
        confirmButtonText: '提交申请',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入认领依据',
        inputType: 'textarea',
        inputValidator: (input) => input?.trim() ? true : '请填写认领依据',
      },
    )
    await createClaim(post.value.id, null, value.trim())
    ElMessage.success('认领申请已提交，请等待发布者确认')
    router.push('/claims')
  } catch {
    // 用户取消或请求失败时由请求层提示。
  }
}

async function submitComment() {
  const content = commentText.value.trim()
  if (!content || commentSubmitting.value) return
  commentSubmitting.value = true
  try {
    await addComment({ postId: post.value.id, content })
    comments.value = (await getComments(post.value.id)).data || []
    commentText.value = ''
    ElMessage.success('评论已发布')
  } finally {
    commentSubmitting.value = false
  }
}

function handleReport() {
  reportTarget.value = {
    targetType: 'POST', targetId: post.value.id, reasonCode: 'OTHER', summary: post.value.title,
  }
}
</script>

<template>
  <div class="detail-shell" v-loading="loading">
    <main class="page detail-page">
      <template v-if="post">
        <nav class="back-nav" aria-label="返回导航" data-reveal>
          <button type="button" class="back-btn" @click="router.push('/')">
            <ArrowLeft :size="17" aria-hidden="true" />
            <span>返回广场</span>
          </button>
        </nav>

        <section class="cover-section" data-reveal style="--reveal-delay: 70ms">
          <el-image
            v-if="post.images && post.images.length === 1"
            :src="imageUrl(post.images[0])"
            fit="cover"
            :preview-src-list="post.images.map((i) => imageUrl(i))"
            class="cover-image"
          />
          <div v-else-if="post.images && post.images.length > 1" class="image-grid">
            <el-image
              v-for="(img, idx) in post.images"
              :key="idx"
              :src="imageUrl(img)"
              fit="cover"
              :preview-src-list="post.images.map((i) => imageUrl(i))"
              class="detail-image"
            />
          </div>
          <el-image
            v-else-if="post.coverImage"
            :src="imageUrl(post.coverImage)"
            fit="cover"
            :preview-src-list="[imageUrl(post.coverImage)]"
            class="cover-image"
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
        </section>

        <article class="info-card" data-reveal style="--reveal-delay: 120ms">
          <div class="info-tags">
            <span class="tag-type" :class="post.type === 0 ? 'lost' : 'found'">{{
              typeLabel(post.type)
            }}</span>
            <span class="tag-status" :class="'s-' + post.status">{{
              statusLabel(post.status)
            }}</span>
          </div>
          <h1 class="info-title">{{ post.title || '无标题' }}</h1>
          <div class="info-meta">
            <span class="meta-item">
              <Tag class="meta-icon" :size="16" aria-hidden="true" />
              {{ post.itemCategory }}
            </span>
            <span class="meta-item">
              <MapPin class="meta-icon" :size="16" aria-hidden="true" />
              {{ post.locationCampus }}
            </span>
            <time class="meta-item">
              <Clock3 class="meta-icon" :size="16" aria-hidden="true" />
              {{ post.lostTime?.replace('T', ' ') }}
            </time>
          </div>
        </article>

        <section
          v-if="post.description"
          class="desc-card"
          data-reveal
          style="--reveal-delay: 150ms"
        >
          <h2 class="desc-label">物品描述</h2>
          <p class="desc-text">{{ post.description }}</p>
        </section>

        <section id="comments" class="comments-card" data-reveal style="--reveal-delay: 165ms">
          <div class="comments-heading">
            <h2 class="desc-label">评论</h2>
            <span class="comments-count">{{ comments.length }}</span>
          </div>
          <div v-if="comments.length === 0" class="comments-empty">还没有评论</div>
          <div v-else class="comments-list">
            <div v-for="comment in comments" :key="comment.commentId" class="comment-item">
              <div class="comment-avatar">{{ (comment.nickname || '校园用户').charAt(0) }}</div>
              <div class="comment-body">
                <div class="comment-meta">
                  <strong>{{ comment.nickname || '校园用户' }}</strong>
                  <time>{{ comment.createTime?.substring(0, 16).replace('T', ' ') }}</time>
                </div>
                <p>{{ comment.content }}</p>
              </div>
            </div>
          </div>
          <form class="comment-form" @submit.prevent="submitComment">
            <input v-model="commentText" maxlength="1000" placeholder="友善交流，补充物品线索..." aria-label="评论内容" />
            <button type="submit" :disabled="commentSubmitting || !commentText.trim()">
              {{ commentSubmitting ? '发布中...' : '发布评论' }}
            </button>
          </form>
        </section>

        <section class="publisher-card" data-reveal style="--reveal-delay: 180ms">
          <div class="publisher-row">
            <div class="publisher-info">
              <div class="publisher-avatar">{{ publisherName().charAt(0) }}</div>
              <div>
                <div class="publisher-name">{{ publisherName() }}</div>
                <div class="publisher-meta">
                  发布于
                  {{ post.createTime?.substring(0, 16).replace('T', ' ') }}
                </div>
              </div>
            </div>
            <button v-if="!isOwner()" type="button" class="dm-btn" @click="goChat(post.userId)">
              <Mail :size="16" aria-hidden="true" />
              <span>私信</span>
            </button>
          </div>

          <div class="action-area">
            <div v-if="isOwner() && isWithdrawable(post.status)" class="owner-actions">
              <button type="button" class="action-btn delete-btn" @click="handleWithdraw">
                <Trash2 :size="17" aria-hidden="true" />
                <span>撤回帖子</span>
              </button>
            </div>
            <button
              v-else-if="post.status === 'PUBLISHED'"
              type="button"
              class="action-btn claim-btn"
              @click="handleClaim"
            >
              <UserRoundCheck :size="17" aria-hidden="true" />
              <span>我要认领</span>
            </button>
            <button
              v-if="!isOwner()"
              type="button"
              class="action-btn report-btn"
              @click="handleReport"
            >
              <Flag :size="17" aria-hidden="true" />
              <span>举报</span>
            </button>
          </div>
        </section>

      </template>
    </main>
  </div>
  <ReportDialog v-model="reportTarget" />
</template>

<style scoped>
.detail-shell {
  --bg-page: transparent;
  --bg-card: #ffffff;
  --bg-glass: rgba(255, 255, 255, 0.86);
  --bg-glass-soft: rgba(255, 255, 255, 0.68);
  --glass-border: rgba(255, 255, 255, 0.62);
  --ink: #0f172a;
  --ink-hover: #000000;
  --primary: #3b82f6;
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
  --danger-border: #fecaca;
  --danger-hover: #fee2e2;
  --transparent: transparent;
  --focus-ring: rgba(59, 130, 246, 0.1);
  --glass-shadow: 0 34px 90px rgba(34, 48, 132, 0.16), inset 0 1px 0 rgba(255, 255, 255, 0.8);
  --capsule-shadow: 0 12px 28px rgba(15, 23, 42, 0.24);
  --radius-sm: 8px;
  --radius-lg: 16px;
  --radius-full: 9999px;
  --font-heading: 'Noto Serif SC', serif;
  --font-body: 'HarmonyOS Sans', sans-serif;
  --font-mono: 'JetBrains Mono', monospace;

  min-height: calc(100vh - 60px);
  color: var(--gray-600);
  font-family: var(--font-body);
  font-size: 1.1rem;
  background: var(--bg-page);
}

.detail-page {
  width: 100%;
  max-width: 880px;
  min-height: calc(100vh - 60px);
  padding: 26px 22px 53px;
  margin: 0 auto;
  background: var(--transparent);
}

.back-nav {
  margin-bottom: 22px;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border: 0;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.92);
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition:
    transform 180ms ease,
    background-color 180ms ease,
    box-shadow 180ms ease;
}
.back-btn:hover {
  transform: translateY(-2px);
  background: #000000;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.24);
}

.cover-section {
  margin-bottom: 22px;
  overflow: hidden;
  background: var(--bg-glass);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--glass-shadow);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
}
.cover-image {
  display: block;
  width: 100%;
  height: 440px;
}
.cover-image :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-placeholder {
  display: flex;
  width: 100%;
  height: 440px;
  align-items: center;
  justify-content: center;
  color: var(--gray-300);
  background: var(--gray-100);
}
.cover-placeholder svg {
  width: 56px;
  height: 56px;
}
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 9px;
  padding: 9px;
}
.detail-image {
  width: 100%;
  height: 220px;
  overflow: hidden;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: transform 0.2s;
}
.detail-image:hover {
  transform: scale(1.02);
}
.detail-image :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-card,
.desc-card,
.publisher-card {
  padding: 26px;
  margin-bottom: 18px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.62);
  border-radius: 24px;
  box-shadow:
    0 34px 90px rgba(34, 48, 132, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
}
.info-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.tag-type,
.tag-status {
  display: inline-flex;
  min-height: 29px;
  align-items: center;
  padding: 4px 13px;
  border-radius: var(--radius-full);
  font-size: 14.3px;
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
.info-title {
  margin: 0 0 12px;
  color: var(--gray-800);
  font-family: var(--font-heading);
  font-size: 26.4px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}
.info-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 11px 18px;
  color: var(--gray-500);
  font-size: 15.4px;
}
.meta-item {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}
.meta-icon {
  flex: none;
  color: var(--gray-400);
}
.meta-item time,
time.meta-item {
  font-family: var(--font-mono);
}

.desc-label {
  margin: 0 0 12px;
  color: var(--gray-800);
  font-family: var(--font-heading);
  font-size: 17.6px;
  font-weight: 600;
  line-height: 1.5;
  letter-spacing: 0;
}
.desc-text {
  margin: 0;
  color: var(--gray-600);
  font-size: 16.5px;
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.publisher-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 18px;
  margin-bottom: 18px;
  border-bottom: 1px solid var(--gray-100);
}
.publisher-info {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 13px;
}
.publisher-avatar {
  display: flex;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  align-items: center;
  justify-content: center;
  color: var(--bg-card);
  font-size: 19.8px;
  font-weight: 700;
  background: var(--primary);
  border-radius: var(--radius-full);
}
.publisher-name {
  color: var(--gray-800);
  font-size: 17.6px;
  font-weight: 600;
}
.publisher-meta {
  margin-top: 2px;
  color: var(--gray-400);
  font-family: var(--font-mono);
  font-size: 14.3px;
  line-height: 1.5;
}
.dm-btn {
  display: inline-flex;
  min-height: 42px;
  flex: none;
  align-items: center;
  gap: 7px;
  padding: 9px 22px;
  color: var(--bg-card);
  font-family: var(--font-body);
  font-size: 15.4px;
  font-weight: 700;
  background: var(--ink);
  border: 1px solid var(--ink);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all 0.2s;
}
.dm-btn:hover {
  color: var(--bg-card);
  background: var(--ink-hover);
  border-color: var(--ink-hover);
  box-shadow: var(--capsule-shadow);
  transform: translateY(-2px);
}
.action-area {
  display: flex;
  flex-direction: column;
  gap: 11px;
}
.action-btn {
  display: inline-flex;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 22px;
  font-family: var(--font-body);
  font-size: 16.5px;
  font-weight: 700;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all 0.2s;
}
.action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
.owner-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
.delete-btn {
  color: var(--type-lost);
  background: var(--type-lost-bg);
  border: 1px solid var(--danger-border);
}
.delete-btn:hover {
  background: var(--danger-hover);
}
.claim-btn {
  color: var(--bg-card);
  background: var(--primary);
  border: 1px solid var(--primary);
}
.claim-btn:hover {
  filter: brightness(0.94);
  transform: translateY(-2px);
}
.report-btn {
  color: var(--gray-600);
  background: var(--gray-100);
  border: 1px solid var(--gray-200);
}
.report-btn:hover {
  background: var(--gray-200);
}
.comments-card {
  padding: 22px 24px;
  background: var(--bg-card);
  border: 1px solid var(--gray-200);
  border-radius: 16px;
}
.comments-heading {
  display: flex;
  align-items: center;
  gap: 8px;
}
.comments-count {
  color: var(--gray-400);
  font-size: 13px;
}
.comments-empty {
  padding: 18px 0;
  color: var(--gray-400);
  font-size: 14px;
}
.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}
.comment-item {
  display: flex;
  gap: 10px;
}
.comment-avatar {
  display: grid;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  place-items: center;
  color: var(--bg-card);
  background: var(--primary);
  border-radius: 50%;
  font-size: 14px;
  font-weight: 700;
}
.comment-body {
  min-width: 0;
  flex: 1;
}
.comment-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  color: var(--ink);
  font-size: 14px;
}
.comment-meta time {
  color: var(--gray-400);
  font-size: 12px;
}
.comment-body p {
  margin: 5px 0 0;
  color: var(--gray-600);
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.comment-form {
  display: flex;
  gap: 8px;
  margin-top: 20px;
}
.comment-form input {
  min-width: 0;
  flex: 1;
  height: 40px;
  padding: 0 12px;
  border: 1px solid var(--gray-200);
  border-radius: 8px;
  color: var(--ink);
  background: var(--gray-50);
}
.comment-form button {
  flex: none;
  padding: 0 14px;
  color: var(--bg-card);
  background: var(--primary);
  border: 0;
  border-radius: 8px;
  cursor: pointer;
}
.comment-form button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

[data-reveal] {
  opacity: 0;
  transform: translate3d(0, 30px, 0) scale(0.985);
  transition:
    opacity 760ms cubic-bezier(0.16, 1, 0.3, 1),
    transform 760ms cubic-bezier(0.16, 1, 0.3, 1);
  transition-delay: var(--reveal-delay, 0ms);
}

[data-reveal].is-visible {
  opacity: 1;
  transform: translate3d(0, 0, 0) scale(1);
}

:deep(.el-loading-mask) {
  background-color: var(--bg-glass);
  backdrop-filter: blur(8px);
}

@media (max-width: 768px) {
  .detail-shell,
  .detail-page {
    min-height: calc(100vh - 108px);
  }
  .detail-page {
    padding: 20px 16px 44px;
  }
  .cover-image {
    height: 330px;
  }
  .cover-placeholder {
    height: 330px;
  }
  .info-card,
  .desc-card,
  .publisher-card {
    padding: 22px;
  }
  .image-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .detail-image {
    height: 198px;
  }
}

@media (max-width: 520px) {
  .cover-image {
    height: 264px;
  }
  .cover-placeholder {
    height: 264px;
  }
  .publisher-row {
    align-items: flex-start;
    gap: 12px;
  }
  .publisher-meta {
    font-family: var(--font-body);
    font-size: 13.2px;
  }
  .dm-btn {
    padding: 8px 12px;
  }
  .owner-actions {
    flex-direction: column;
  }
  .owner-actions .action-btn {
    width: 100%;
  }
  .image-grid {
    grid-template-columns: 1fr;
  }
  .detail-image {
    height: 242px;
  }
}

@media (prefers-reduced-motion: reduce) {
  [data-reveal],
  [data-reveal].is-visible {
    opacity: 1;
    transform: none;
    transition: none;
  }
}
</style>
