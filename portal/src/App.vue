<script setup>
import { ref, computed, onBeforeUnmount, onMounted, provide, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Backpack, LogOut, Plus } from 'lucide-vue-next'
import { getUnreadCount } from '@/api/message'
import { getConversations } from '@/api/pm'
import { logout as logoutApi } from '@/api/user'

const route = useRoute()
const router = useRouter()

const showNav = computed(() => route.path !== '/login')
const isPublishPage = computed(() => route.path === '/publish')
const unreadCount = ref(0)
const pmUnread = ref(0)
const isScrolled = ref(false)
let unreadTimer = null
let scrollFrame = null

async function logout() {
  try {
    if (localStorage.getItem('token')) await logoutApi()
  } catch {
    // Local session cleanup still prevents a stale client session.
  } finally {
    stopUnreadPolling()
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    router.push('/login')
  }
}

async function loadUnreadCount() {
  if (!localStorage.getItem('token')) return
  try {
    const [system, privateMessages] = await Promise.all([
      getUnreadCount(),
      getConversations(),
    ])
    unreadCount.value = Number(system.data || 0)
    pmUnread.value = (privateMessages.data || []).reduce(
      (total, conversation) => total + Number(conversation.unread || 0),
      0,
    )
  } catch {
    unreadCount.value = 0
    pmUnread.value = 0
  }
}

function stopUnreadPolling() {
  if (!unreadTimer) return
  window.clearInterval(unreadTimer)
  unreadTimer = null
}

function startUnreadPolling() {
  if (!showNav.value || !localStorage.getItem('token') || unreadTimer) return
  loadUnreadCount()
  unreadTimer = window.setInterval(loadUnreadCount, 30000)
}

function updateScrollState() {
  const scrollTop = window.scrollY || document.documentElement.scrollTop
  isScrolled.value = scrollTop > 8
  scrollFrame = null
}

function scheduleScrollUpdate() {
  if (scrollFrame !== null) return
  scrollFrame = window.requestAnimationFrame(updateScrollState)
}

provide('refreshUnread', loadUnreadCount)

onMounted(() => {
  startUnreadPolling()
  updateScrollState()
  window.addEventListener('scroll', scheduleScrollUpdate, { passive: true })
})

watch(showNav, (visible) => {
  if (visible) startUnreadPolling()
  else stopUnreadPolling()
})

onBeforeUnmount(() => {
  stopUnreadPolling()
  window.removeEventListener('scroll', scheduleScrollUpdate)
  if (scrollFrame !== null) window.cancelAnimationFrame(scrollFrame)
})
</script>

<template>
  <div class="app-shell">
    <header v-if="showNav" class="site-header" :class="{ 'is-scrolled': isScrolled }">
      <div class="header-inner">
        <div class="header-left">
          <div class="brand" @click="router.push('/')">
            <span class="brand-icon" aria-hidden="true">
              <Backpack :size="24" :stroke-width="1.8" />
            </span>
            <span class="brand-text">校园失物招领</span>
          </div>
        </div>

        <nav class="nav-links">
          <router-link to="/" :class="{ active: route.path === '/' }">失物广场</router-link>
          <router-link to="/publish" :class="{ active: route.path === '/publish' }"
            >发布帖子</router-link
          >
          <router-link
            to="/messages"
            :class="{ active: route.path === '/messages' }"
            class="nav-msg-link"
          >
            消息
            <span v-if="unreadCount + pmUnread > 0" class="nav-badge">{{
              unreadCount + pmUnread
            }}</span>
          </router-link>
          <router-link to="/claims" :class="{ active: route.path === '/claims' }"
            >认领进度</router-link
          >
          <router-link to="/governance" :class="{ active: route.path === '/governance' }"
            >处罚与申诉</router-link
          >
          <router-link to="/profile" :class="{ active: route.path === '/profile' }"
            >个人中心</router-link
          >
        </nav>

        <div class="header-right">
          <button v-if="isPublishPage" class="header-action-btn" @click="router.push('/')">
            <ArrowLeft :size="16" aria-hidden="true" />
            <span>返回广场</span>
          </button>
          <button v-else class="header-action-btn" @click="router.push('/publish')">
            <Plus :size="16" aria-hidden="true" />
            <span>发布帖子</span>
          </button>
          <button class="header-logout-btn" title="退出登录" @click="logout">
            <LogOut :size="16" aria-hidden="true" />
            <span>退出</span>
          </button>
        </div>
      </div>
    </header>
    <main :class="{ 'has-header': showNav }">
      <router-view />
    </main>
  </div>
</template>

<style>
:root {
  --font-size-base: 1.1rem;
  --app-bg: #eef2f7;
  --app-wash-start: rgba(219, 234, 254, 0.7);
  --app-wash-mid: rgba(255, 255, 255, 0.42);
  --app-wash-end: rgba(237, 233, 254, 0.56);
  --surface-white: #ffffff;
  --surface-glass: rgba(255, 255, 255, 0.58);
  --header-scrolled: rgba(245, 247, 252, 0.92);
  --text-strong: #1e293b;
  --text-body: #475569;
  --text-muted: #64748b;
  --text-soft: #94a3b8;
  --ink: #0f172a;
  --ink-hover: #000000;
  --danger: #dc2626;
  --danger-soft: #fef2f2;
  --line: #e2e8f0;
  --line-strong: #cbd5e1;
  --transparent: transparent;
  --header-shadow: 0 18px 54px rgba(7, 11, 24, 0.14);
  --button-shadow: 0 12px 28px rgba(15, 23, 42, 0.24);
  --focus-ring: rgba(37, 99, 235, 0.22);
  --app-background: linear-gradient(
    125deg,
    var(--app-wash-start) 0%,
    var(--app-wash-mid) 48%,
    var(--app-wash-end) 100%
  );
}

* {
  box-sizing: border-box;
  padding: 0;
  margin: 0;
}

html {
  overflow-y: scroll;
  scroll-behavior: smooth;
}

body {
  min-width: 320px;
  min-height: 100vh;
  color: var(--text-body);
  font-family:
    'Noto Sans SC',
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    Roboto,
    sans-serif;
  font-size: var(--font-size-base);
  background-color: var(--app-bg);
  background-image: var(--app-background);
  background-attachment: fixed;
}

button,
input,
textarea,
select {
  font: inherit;
}

.app-shell {
  position: relative;
  z-index: 1;
}

.site-header {
  position: fixed;
  top: 0;
  right: 0;
  left: 0;
  z-index: 50;
  height: 60px;
  background: transparent;
  border-bottom: 1px solid transparent;
  transition:
    background-color 220ms ease,
    border-color 220ms ease,
    box-shadow 220ms ease,
    backdrop-filter 220ms ease;
}

.site-header.is-scrolled {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  border-bottom-color: rgba(226, 232, 240, 0.6);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.header-inner {
  display: flex;
  width: 100%;
  max-width: 1540px;
  height: 100%;
  padding: 0 31px;
  margin: 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.header-left,
.header-right {
  flex: none;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-strong);
  font-family: 'Noto Serif SC', Georgia, serif;
  font-size: 29px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0;
  white-space: nowrap;
  cursor: pointer;
}

.brand-icon {
  display: grid;
  width: 36px;
  height: 36px;
  flex: none;
  color: #fff;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.24);
  place-items: center;
}

.nav-links {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: stretch;
  align-self: stretch;
  justify-content: center;
  gap: 4px;
}

.nav-links a {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 0 15px;
  color: var(--text-muted);
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
  text-decoration: none;
  white-space: nowrap;
  transition: color 180ms ease;
}

.nav-links a:hover,
.nav-links a.active {
  color: var(--text-strong);
}

.nav-links a.active {
  font-weight: 700;
}

.nav-links a::after {
  position: absolute;
  right: 15px;
  bottom: 7px;
  left: 15px;
  height: 2px;
  content: '';
  background: var(--ink);
  border-radius: 999px;
  opacity: 0;
  transform: scaleX(0.35);
  transition:
    opacity 180ms ease,
    transform 180ms ease;
}

.nav-links a.active::after {
  opacity: 1;
  transform: scaleX(1);
}

.nav-msg-link {
  position: relative;
}

.nav-badge {
  position: absolute;
  top: 8px;
  right: 2px;
  display: flex;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  align-items: center;
  justify-content: center;
  color: var(--surface-white);
  font-size: 10px;
  font-weight: 800;
  line-height: 1;
  background: var(--ink);
  border-radius: 999px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-action-btn {
  display: inline-flex;
  min-height: 38px;
  padding: 0 18px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  color: var(--surface-white);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  background: var(--ink);
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  transition:
    color 180ms ease,
    background-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.header-logout-btn {
  display: inline-flex;
  padding: 8px 18px;
  align-items: center;
  gap: 6px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(15, 23, 42, 0.16);
  border-radius: 8px;
  cursor: pointer;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  transition: all 0.2s ease;
}

.header-action-btn:hover,
.header-action-btn:focus-visible {
  background: var(--ink-hover);
  box-shadow: var(--button-shadow);
  transform: translateY(-2px);
  outline: none;
}

.header-logout-btn:hover,
.header-logout-btn:focus-visible {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(15, 23, 42, 0.32);
  outline: none;
}

.header-action-btn:active,
.header-logout-btn:active {
  transform: translateY(0) scale(0.97);
}

main.has-header {
  min-height: 100vh;
  padding-top: 60px;
}

.notify-bell {
  position: relative;
  margin-right: 16px;
}

.notify-overlay {
  position: fixed;
  inset: 0;
  z-index: 99;
}

.notify-dropdown {
  position: absolute;
  top: 48px;
  right: 80px;
  z-index: 101;
  width: 340px;
  max-height: 420px;
  overflow: hidden;
  background: var(--header-scrolled);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: var(--header-shadow);
  backdrop-filter: blur(18px);
}

.notify-header {
  display: flex;
  padding: 14px 16px;
  align-items: center;
  justify-content: space-between;
  color: var(--text-strong);
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid var(--line);
}

.notify-list {
  max-height: 350px;
  overflow-y: auto;
}

.notify-item {
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
  transition: background-color 150ms ease;
}

.notify-item:hover,
.notify-item.unread {
  background: var(--surface-glass);
}

.notify-title {
  margin-bottom: 4px;
  color: var(--text-strong);
  font-size: 14px;
  font-weight: 600;
}

.notify-content {
  margin-bottom: 4px;
  color: var(--text-muted);
  font-size: 12px;
  white-space: pre-line;
}

.notify-time {
  color: var(--text-soft);
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  font-size: 11px;
}

.notify-empty {
  padding: 40px 0;
  color: var(--text-soft);
  font-size: 14px;
  text-align: center;
}

.el-dialog,
.el-message-box {
  overflow: hidden;
  background: var(--header-scrolled) !important;
  border: 1px solid var(--line) !important;
  border-radius: 12px !important;
  box-shadow: var(--header-shadow) !important;
  backdrop-filter: blur(20px) saturate(180%);
}

.el-dialog__header {
  padding: 18px 24px 0 !important;
  margin: 0 !important;
}

.el-dialog__title,
.el-message-box__title {
  color: var(--text-strong) !important;
  font-family: 'Noto Serif SC', Georgia, serif;
  font-size: 17px !important;
  font-weight: 700 !important;
}

.el-dialog__body {
  padding: 22px 24px !important;
  color: var(--text-body);
}

.el-dialog__footer {
  padding: 10px 24px 18px !important;
}

.el-dialog .el-input__wrapper,
.el-message-box__input .el-input__wrapper {
  background: var(--surface-white);
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--line) inset;
}

.el-dialog .el-input__wrapper:hover,
.el-message-box__input .el-input__wrapper:hover {
  box-shadow: 0 0 0 1px var(--line-strong) inset;
}

.el-dialog .el-input.is-focus .el-input__wrapper,
.el-message-box__input .el-input.is-focus .el-input__wrapper {
  box-shadow: 0 0 0 2px var(--focus-ring) inset;
}

.el-dialog .el-button,
.el-message-box__btns .el-button {
  border-radius: 999px !important;
}

.el-dialog .el-button--primary,
.el-message-box__btns .el-button--primary {
  color: var(--surface-white) !important;
  font-weight: 700 !important;
  background: var(--ink) !important;
  border-color: var(--ink) !important;
}

.el-dialog .el-button--primary:hover,
.el-message-box__btns .el-button--primary:hover {
  background: var(--ink-hover) !important;
  border-color: var(--ink-hover) !important;
}

.el-message-box {
  padding: 0 !important;
}

.el-message-box__header {
  padding: 18px 24px 6px !important;
}

.el-message-box__body {
  padding: 6px 24px 22px !important;
}

.el-message-box__message,
.el-message-box__content {
  padding: 0 !important;
  margin: 0 !important;
  color: var(--text-body) !important;
  font-size: 14px !important;
}

.el-message-box__input {
  padding-top: 8px !important;
}

.el-message-box__btns {
  padding: 0 24px 20px !important;
}

.el-message-box__status.el-icon-warning {
  color: var(--danger) !important;
}

@media (max-width: 1120px) {
  .header-inner {
    padding: 0 20px;
    gap: 12px;
  }

  .nav-links a {
    padding: 0 10px;
  }

  .nav-links a::after {
    right: 10px;
    left: 10px;
  }

  .brand {
    font-size: 25px;
  }
}

@media (max-width: 800px) {
  .site-header {
    height: 108px;
  }

  .header-inner {
    height: 108px;
    padding: 8px 16px 0;
    flex-wrap: wrap;
    gap: 0 12px;
  }

  .brand {
    font-size: 24px;
  }

  .header-right {
    margin-left: auto;
  }

  .nav-links {
    order: 3;
    width: 100%;
    height: 46px;
    flex: 0 0 100%;
    justify-content: flex-start;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .nav-links::-webkit-scrollbar {
    display: none;
  }

  .nav-links a {
    flex: none;
    padding: 0 14px;
    font-size: 17px;
  }

  .nav-links a:first-child {
    padding-left: 0;
  }

  .nav-links a::after {
    right: 14px;
    left: 14px;
  }

  .nav-links a:first-child::after {
    left: 0;
  }

  .nav-badge {
    top: 4px;
  }

  main.has-header {
    padding-top: 108px;
  }
}

@media (max-width: 420px) {
  .header-action-btn {
    width: 36px;
    min-height: 36px;
    padding: 0;
  }

  .header-action-btn span {
    display: none;
  }

  .header-logout-btn {
    padding: 8px 10px;
  }
}

@media (max-width: 360px) {
  .header-inner {
    gap: 0 4px;
  }

  .brand {
    font-size: 23px;
  }

  .header-action-btn {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  html {
    scroll-behavior: auto;
  }

  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
