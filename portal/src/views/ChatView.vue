<script setup>
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getChatWith, sendMessage } from '@/api/pm'
import ReportDialog from '@/components/ReportDialog.vue'
import { Flag, Send } from 'lucide-vue-next'
import { useAuroraGlow } from '@/composables/useAuroraGlow'
import { getErrorMessage } from '@/utils/error'

const route = useRoute()
const router = useRouter()
const { glowX, glowY, glowActive, onMouseMove, onMouseLeave } = useAuroraGlow()
const otherId = ref(Number.parseInt(route.query.userId, 10))
const messages = ref([])
const text = ref('')
const sending = ref(false)
const loading = ref(false)
const loadError = ref('')
const chatBox = ref(null)
const reportTarget = ref(null)

async function loadChat(userId) {
  if (!userId) {
    router.replace('/messages')
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const res = await getChatWith(userId)
    if (userId !== otherId.value) return
    messages.value = res.data
  } catch (error) {
    if (userId === otherId.value) loadError.value = getErrorMessage(error, '私信加载失败，请重试')
  } finally {
    if (userId === otherId.value) loading.value = false
  }
  nextTick(() => { chatBox.value?.scrollTo({ top: chatBox.value.scrollHeight }) })
}

onMounted(() => loadChat(otherId.value))
watch(() => route.query.userId, (value) => {
  const nextId = Number.parseInt(value, 10)
  if (nextId !== otherId.value) {
    otherId.value = nextId
    loadChat(nextId)
  }
})

async function send() {
  const content = text.value.trim()
  if (!content || sending.value || !otherId.value) return
  sending.value = true
  try {
    await sendMessage({ toUserId: otherId.value, content })
    text.value = ''
    await loadChat(otherId.value)
  } catch {
    // Request errors are displayed by the shared response interceptor.
  } finally { sending.value = false }
}

function isMe(msg) { const info = localStorage.getItem('userInfo'); if (!info) return false; const me = JSON.parse(info); return msg.fromUserId === (me.userId || me.id) }

function reportMessage(message) {
  reportTarget.value = {
    targetType: 'PRIVATE_MESSAGE', targetId: message.messageId, reasonCode: 'ABUSE', summary: message.content,
  }
}

function formatDateLabel(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr.replace(' ', 'T'))
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today - 86400000)
  const msgDay = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  if (msgDay.getTime() === today.getTime()) return '今天'
  if (msgDay.getTime() === yesterday.getTime()) return '昨天'
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

const displayMessages = computed(() => {
  const result = []
  let lastTime = null
  for (const m of messages.value) {
    const mTime = m.createTime ? new Date(m.createTime.replace(' ', 'T')).getTime() : null
    if (lastTime === null || (mTime && mTime - lastTime > 3 * 60 * 1000)) {
      result.push({ type: 'date', date: formatDateLabel(m.createTime), createTime: m.createTime })
    }
    result.push({ type: 'msg', ...m })
    lastTime = mTime
  }
  return result
})
</script>

<template>
  <div class="chat-page" @mousemove="onMouseMove" @mouseleave="onMouseLeave">
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

    <div class="chat-container">
      <header class="chat-header">
        <button class="back-btn" type="button" @click="router.push('/messages')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
          返回
        </button>
        <span class="chat-title">私信</span>
      </header>

      <div ref="chatBox" class="chat-box">
        <template v-for="item in displayMessages" :key="item.type === 'date' ? 'd-' + item.createTime : 'm-' + item.messageId">
          <div v-if="item.type === 'date'" class="chat-date-sep">
            <span>{{ item.date }}</span>
          </div>
          <div v-else class="chat-msg" :class="{ mine: isMe(item) }">
            <div class="chat-bubble">{{ item.content }}</div>
            <div class="chat-time">{{ item.createTime?.substring(11, 16) }}</div>
            <button v-if="!isMe(item)" type="button" class="report-message-btn" title="举报这条私信" @click.stop="reportMessage(item)">
              <Flag :size="13" aria-hidden="true" />
            </button>
          </div>
        </template>
        <div v-if="loading" class="chat-empty">正在加载消息...</div>
        <div v-else-if="loadError" class="chat-empty">
          <span>{{ loadError }}</span>
          <button type="button" class="retry-chat-btn" @click="loadChat(otherId)">重试</button>
        </div>
        <div v-else-if="messages.length === 0" class="chat-empty">还没有消息，和对方打个招呼吧</div>
      </div>

      <div class="chat-input">
        <input v-model="text" class="chat-input-field" placeholder="输入消息..." maxlength="2000" aria-label="输入消息" @keyup.enter="send" />
        <button class="send-btn" type="button" :disabled="sending || !text.trim()" @click="send">
          <Send :size="15" aria-hidden="true" />
          <span>{{ sending ? '发送中...' : '发送' }}</span>
        </button>
      </div>
    </div>
  </div>
  <ReportDialog v-model="reportTarget" />
</template>

<style scoped>
.chat-page {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: calc(100vh - 60px);
  padding: 31px;
  position: relative;
  isolation: isolate;
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

.chat-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 720px;
  height: calc(100vh - 122px);
  min-height: calc(100vh - 122px);
  overflow: hidden;
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: none;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border: 1.5px solid rgba(15, 23, 42, 0.2);
  border-radius: 999px;
  background: transparent;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.back-btn:hover {
  border-color: rgba(15, 23, 42, 0.4);
  background: rgba(15, 23, 42, 0.04);
}

.back-btn:focus-visible,
.chat-input-field:focus-visible,
.send-btn:focus-visible {
  outline: none;
}

.back-btn:focus-visible,
.send-btn:focus-visible {
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.16);
}

.chat-title {
  color: #1e293b;
  font-family: 'Noto Serif SC', serif;
  font-size: 17.6px;
  font-weight: 600;
}

.chat-box {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  padding: 20px;
  overflow-y: auto;
  background: transparent;
  scrollbar-color: rgba(100, 116, 139, 0.35) transparent;
  scrollbar-width: thin;
}

.chat-date-sep {
  padding: 4px 0;
  color: #94a3b8;
  font-size: 12px;
  text-align: center;
}

.chat-date-sep span {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.6);
}

.chat-msg {
  display: flex;
  flex-direction: column;
  width: fit-content;
  max-width: 70%;
  align-self: flex-start;
}

.chat-msg.mine {
  align-self: flex-end;
}

.chat-bubble {
  padding: 10px 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px 16px 16px 4px;
  background: rgba(255, 255, 255, 0.6);
  color: #1e293b;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
  font-size: 14px;
  line-height: 1.5;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.chat-msg.mine .chat-bubble {
  border: none;
  border-radius: 16px 16px 4px 16px;
  background: rgba(15, 23, 42, 0.92);
  color: #ffffff;
}

.chat-time {
  margin-top: 4px;
  padding: 0 4px;
  color: #94a3b8;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
}

.chat-msg.mine .chat-time {
  text-align: right;
}

.report-message-btn {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin-top: 4px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
}

.report-message-btn:hover {
  background: rgba(220, 38, 38, 0.08);
  color: #dc2626;
}

.chat-empty {
  padding: 60px 0;
  color: #94a3b8;
  font-size: 14px;
  text-align: center;
}

.retry-chat-btn {
  display: block;
  margin: 12px auto 0;
  padding: 7px 16px;
  border: 1px solid rgba(15, 23, 42, 0.16);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #334155;
  font-size: 13px;
  cursor: pointer;
}

.chat-input {
  display: flex;
  gap: 10px;
  flex: none;
  padding: 14px 20px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

.chat-input-field {
  min-width: 0;
  flex: 1;
  padding: 10px 18px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  border-radius: 999px;
  outline: none;
  background: rgba(255, 255, 255, 0.6);
  color: #1e293b;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
  font-size: 15.4px;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.chat-input-field::placeholder {
  color: #94a3b8;
}

.chat-input-field:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.send-btn {
  min-width: 72px;
  padding: 10px 20px;
  border: none;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.92);
  color: #ffffff;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
  font-size: 15.4px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  background: #000000;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.24);
  transform: translateY(-1px);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .chat-page {
    height: calc(100vh - 108px);
    padding: 16px;
  }

  .chat-container {
    height: calc(100vh - 140px);
    min-height: calc(100vh - 140px);
    border-radius: 16px;
  }

  .chat-header,
  .chat-input {
    padding-right: 14px;
    padding-left: 14px;
  }

  .chat-box {
    padding: 16px 14px;
  }

  .chat-msg {
    max-width: 84%;
  }

  .chat-input-field {
    padding-right: 14px;
    padding-left: 14px;
  }

  .send-btn {
    min-width: 64px;
    padding-right: 14px;
    padding-left: 14px;
  }
}

@media (max-width: 420px) {
  .chat-input {
    gap: 8px;
  }

  .send-btn {
    min-width: 58px;
    padding-right: 12px;
    padding-left: 12px;
    font-size: 14px;
  }
}
</style>
