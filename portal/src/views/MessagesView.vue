<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, inject } from 'vue'
import { useRouter } from 'vue-router'
import { getMessages, markRead } from '@/api/message'
import { getConversations } from '@/api/pm'
import { useAuroraGlow } from '@/composables/useAuroraGlow'
import { getErrorMessage } from '@/utils/error'

const router = useRouter()
const { glowX, glowY, glowActive, onMouseMove, onMouseLeave } = useAuroraGlow()
const systemMsgs = ref([])
const conversations = ref([])
const sysExpanded = ref(false)
const loading = ref(true)
const loadError = ref('')

const systemUnread = computed(() => systemMsgs.value.filter(m => m.isRead === 0).length)
const refreshUnread = inject('refreshUnread', () => {})

let revealObserver = null

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
    { threshold: 0.12 },
  )
}

async function loadMessages() {
  loading.value = true
  loadError.value = ''
  try {
    const [messageResult, conversationResult] = await Promise.all([getMessages(), getConversations()])
    systemMsgs.value = messageResult.data || []
    conversations.value = conversationResult.data || []
  } catch (error) {
    loadError.value = getErrorMessage(error, '消息加载失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  initRevealObserver()
  await loadMessages()
  await nextTick()
  document.querySelectorAll('.msg-page [data-reveal]').forEach((el) => {
    revealObserver?.observe(el)
  })
})

onBeforeUnmount(() => {
  revealObserver?.disconnect()
})

function goPost(id) { router.push(`/post/${id}#comments`) }
function goChat(otherId) {
  if (otherId) router.push({ path: '/chat', query: { userId: otherId } })
}

async function handleMsgClick(m) {
  if (m.isRead === 0 && m.messageId) {
    await markRead(m.messageId)
    m.isRead = 1
    refreshUnread()
  }
  if (m.relatedType === 'POST' && m.relatedId) goPost(m.relatedId)
}
</script>

<template>
  <div class="page msg-page" @mousemove="onMouseMove" @mouseleave="onMouseLeave">
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

    <header class="page-header">
      <div>
        <h1 class="page-title">消息中心</h1>
        <p class="page-subtitle">系统通知与私信</p>
      </div>
      <div v-if="systemUnread > 0" class="stat-block">
        <span class="stat-num">{{ systemUnread }}</span>
        <span class="stat-label">条未读</span>
      </div>
    </header>

    <div v-if="loading" class="empty-state" role="status">正在加载消息...</div>
    <div v-else-if="loadError" class="empty-state" role="alert">
      <p class="empty-text">{{ loadError }}</p>
      <button class="retry-btn" type="button" @click="loadMessages">重试</button>
    </div>
    <div class="msg-content">
      <section v-if="!loading && !loadError" class="msg-section">
        <h2 class="section-title">
          <span class="section-num">01</span>
          系统通知
          <span v-if="systemUnread > 0" class="badge">{{ systemUnread }}</span>
          <button class="toggle-btn" type="button" @click="sysExpanded = !sysExpanded">
            {{ sysExpanded ? '收起' : '展开' }}
          </button>
        </h2>
        <div v-if="systemMsgs.length === 0" class="empty-state">
          <p class="empty-text">暂时没有通知</p>
        </div>
        <div v-if="sysExpanded" class="msg-list">
          <div v-for="m in systemMsgs" :key="m.messageId" class="msg-item" :class="{ unread: m.isRead === 0 }" @click="handleMsgClick(m)">
            <div class="msg-title">
              <span v-if="m.isRead === 0" class="unread-dot"></span>
              {{ m.title }}
            </div>
            <div class="msg-body">{{ m.content }}</div>
            <div class="msg-time">{{ m.createTime?.substring(0, 16).replace('T', ' ') }}</div>
          </div>
        </div>
      </section>

      <section v-if="!loading && !loadError" class="msg-section">
        <h2 class="section-title">
          <span class="section-num">02</span>
          私信
        </h2>
        <div v-if="conversations.length === 0" class="empty-state">
          <p class="empty-text">还没有私信记录</p>
        </div>
        <div v-else class="conv-list">
          <div v-for="c in conversations" :key="c.partnerId" class="conv-item" @click="goChat(c.partnerId)">
            <div class="avatar">{{ c.nickname?.charAt(0) || 'U' }}</div>
            <div class="conv-info">
              <div class="conv-name">
                {{ c.nickname }}
                <span v-if="c.unread" class="badge">{{ c.unread }}</span>
              </div>
              <div class="conv-last">{{ c.lastMsg?.substring(0, 50) }}</div>
            </div>
            <span class="conv-time">{{ c.lastTime?.substring(0, 16).replace('T', ' ') }}</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.msg-page {
  width: 100%;
  max-width: 720px;
  min-height: calc(100vh - 60px);
  margin: 0 auto;
  padding: 31px;
  position: relative;
  isolation: isolate;
  color: #475569;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
}

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

.page-header,
.msg-content {
  position: relative;
  z-index: 1;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}

.page-title {
  margin: 0;
  color: #1e293b;
  font-family: 'Noto Serif SC', serif;
  font-size: 36px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: 0;
}

.page-subtitle {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 16px;
  line-height: 1.6;
}

.stat-block {
  display: flex;
  align-items: baseline;
  gap: 6px;
  white-space: nowrap;
}

.stat-num {
  color: #3b82f6;
  font-family: 'JetBrains Mono', monospace;
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
}

.stat-label {
  color: #64748b;
  font-size: 16px;
}

.msg-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.msg-section {
  padding: 24px 28px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 18px;
  color: #1e293b;
  font-family: 'Noto Serif SC', serif;
  font-size: 17.6px;
  font-weight: 600;
  line-height: 1.4;
}

.section-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  font-weight: 700;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
  font-size: 12px;
  font-weight: 600;
}

.toggle-btn {
  margin-left: auto;
  padding: 4px 0 4px 12px;
  border: 0;
  background: transparent;
  color: #3b82f6;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.toggle-btn:hover {
  color: #1d4ed8;
}

.toggle-btn:focus-visible,
.msg-item:focus-visible,
.conv-item:focus-visible {
  outline: 3px solid rgba(59, 130, 246, 0.18);
  outline-offset: 2px;
}

.msg-list,
.conv-list {
  display: flex;
  flex-direction: column;
}

.msg-item {
  padding: 14px 12px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.msg-item:last-child {
  border-bottom-color: transparent;
}

.msg-item:hover {
  background: rgba(59, 130, 246, 0.04);
}

.msg-item.unread {
  background: rgba(59, 130, 246, 0.06);
}

.msg-item.unread:hover {
  background: rgba(59, 130, 246, 0.09);
}

.msg-title {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 4px;
  color: #1e293b;
  font-size: 14px;
  font-weight: 600;
}

.unread-dot {
  width: 8px;
  height: 8px;
  flex: 0 0 8px;
  border-radius: 50%;
  background: #3b82f6;
}

.msg-body {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-line;
}

.msg-time,
.conv-time {
  color: #94a3b8;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
}

.msg-time {
  margin-top: 6px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 12px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.conv-item:last-child {
  border-bottom-color: transparent;
}

.conv-item:hover {
  background: rgba(59, 130, 246, 0.04);
}

.avatar {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  border-radius: 50%;
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  font-size: 16px;
  font-weight: 700;
  place-items: center;
}

.conv-info {
  min-width: 0;
  flex: 1;
}

.conv-name {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #1e293b;
  font-size: 14px;
  font-weight: 600;
}

.conv-last {
  margin-top: 3px;
  overflow: hidden;
  color: #64748b;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  flex-shrink: 0;
  white-space: nowrap;
}

.empty-state {
  padding: 40px 0;
  color: #94a3b8;
  text-align: center;
}

.retry-btn {
  min-height: 36px;
  padding: 7px 16px;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.retry-btn:hover {
  border-color: #3b82f6;
  color: #1d4ed8;
}

.empty-text {
  margin: 0;
  font-size: 14px;
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

@media (max-width: 768px) {
  .msg-page {
    min-height: calc(100vh - 108px);
    padding: 24px 16px 36px;
  }

  .page-header {
    align-items: flex-start;
  }

  .page-title {
    font-size: 29px;
  }

  .stat-num {
    font-size: 27px;
  }

  .msg-section {
    padding: 18px 20px;
  }

  .msg-item,
  .conv-item {
    padding-right: 8px;
    padding-left: 8px;
  }

  .conv-item {
    align-items: flex-start;
  }

  .conv-time {
    max-width: 94px;
    overflow: hidden;
    font-size: 11px;
    text-overflow: ellipsis;
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
