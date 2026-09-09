<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleAlert, FileCheck2, MessageSquareText, RefreshCw, ShieldCheck } from 'lucide-vue-next'
import { getMyActions, getMyAppeals, submitAppeal } from '@/api/moderation'
import { getErrorMessage } from '@/utils/error'
import '@/styles/account-pages.css'

const actions = ref([])
const appeals = ref([])
const loading = ref(false)
const loadError = ref('')
const appealAction = ref(null)
const appealReason = ref('')
const appealError = ref('')
const submitting = ref(false)
const activeActions = computed(() => actions.value.filter(action => actionState(action) === '生效中'))

function actionLabel(type) {
  return {
    POST_PUBLISH: '限制发布', COMMENT: '限制评论', CLAIM_APPLY: '限制认领',
    PRIVATE_MESSAGE: '限制私信', TEMP_SUSPENSION: '临时封禁', PERMANENT_BAN: '永久封禁',
  }[type] || '其他限制'
}

function actionState(action) {
  const end = action.endTime ? new Date(action.endTime).getTime() : Infinity
  if (end <= Date.now()) return '已结束'
  if (action.startTime && new Date(action.startTime).getTime() > Date.now()) return '未生效'
  return '生效中'
}

function appealLabel(status) {
  return { SUBMITTED: '处理中', APPROVED: '申诉通过', REJECTED: '申诉未通过' }[status] || '状态待确认'
}

function appealTone(status) {
  return { SUBMITTED: 'is-info', APPROVED: 'is-success', REJECTED: 'is-danger' }[status] || ''
}

function formatTime(value, fallback = '未记录') {
  return value ? String(value).substring(0, 16).replace('T', ' ') : fallback
}

function appealFor(actionId) {
  return appeals.value.find(appeal => appeal.actionId === actionId)
}

function relatedAction(appeal) {
  return actions.value.find(action => action.actionId === appeal.actionId)
}

function openAppeal(action) {
  appealAction.value = action
  appealReason.value = ''
  appealError.value = ''
}

async function loadRecords() {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  try {
    const [actionResult, appealResult] = await Promise.all([getMyActions(), getMyAppeals()])
    actions.value = actionResult.data || []
    appeals.value = appealResult.data || []
  } catch (error) {
    loadError.value = getErrorMessage(error, '记录加载失败，请重试')
  } finally {
    loading.value = false
  }
}

async function handleAppeal() {
  if (!appealAction.value || submitting.value) return
  appealError.value = ''
  const reason = appealReason.value.trim()
  if (!reason || reason.length > 3000) {
    appealError.value = !reason ? '请填写申诉理由' : '申诉理由不能超过3000个字符'
    return
  }
  submitting.value = true
  try {
    await submitAppeal({ actionId: appealAction.value.actionId, reason })
    appealAction.value = null
    ElMessage.success('申诉已提交')
    await loadRecords()
  } catch (error) {
    appealError.value = getErrorMessage(error, '申诉提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(loadRecords)
</script>

<template>
  <div class="account-page governance-page">
    <header class="account-header">
      <div>
        <h1 class="account-title">处罚与申诉</h1>
        <p v-if="!loading && !loadError" class="account-summary">
          {{ activeActions.length }} 项限制生效中 · {{ appeals.length }} 条申诉记录
        </p>
      </div>
      <button class="account-icon-button" type="button" title="刷新记录" aria-label="刷新记录" :disabled="loading" @click="loadRecords">
        <RefreshCw :size="17" aria-hidden="true" />
      </button>
    </header>

    <div v-if="loading" class="account-state" role="status">正在加载记录...</div>
    <div v-else-if="loadError" class="account-state" role="alert">
      <CircleAlert aria-hidden="true" />
      <p>{{ loadError }}</p>
      <button class="account-button" type="button" @click="loadRecords"><RefreshCw :size="16" aria-hidden="true" />重试</button>
    </div>
    <template v-else>
      <section class="account-section" aria-labelledby="actions-heading">
        <div class="account-section-heading">
          <ShieldCheck :size="20" aria-hidden="true" />
          <h2 id="actions-heading">处罚记录</h2>
          <span class="account-count">{{ actions.length }} 条</span>
        </div>
        <div v-if="!actions.length" class="account-state">
          <ShieldCheck aria-hidden="true" /><p>暂无处罚记录</p>
        </div>
        <ul v-else class="account-list">
          <li v-for="action in actions" :key="action.actionId" class="account-record">
            <div class="account-record-heading">
              <h3>{{ actionLabel(action.actionType) }}</h3>
              <span class="account-badge" :class="actionState(action) === '生效中' ? 'is-warning' : ''">{{ actionState(action) }}</span>
            </div>
            <p class="account-record-text">{{ action.reason }}</p>
            <div class="account-meta">
              <span>开始 {{ formatTime(action.startTime) }}</span>
              <span>结束 {{ formatTime(action.endTime, '无固定期限') }}</span>
            </div>
            <div class="account-record-footer">
              <span class="account-meta">记录 #{{ action.actionId }}</span>
              <span v-if="appealFor(action.actionId)" class="account-badge" :class="appealTone(appealFor(action.actionId).status)">
                {{ appealLabel(appealFor(action.actionId).status) }}
              </span>
              <button v-else-if="actionState(action) === '生效中'" class="account-button" type="button" @click="openAppeal(action)">
                <MessageSquareText :size="16" aria-hidden="true" />提交申诉
              </button>
            </div>
          </li>
        </ul>
      </section>

      <section class="account-section" aria-labelledby="appeals-heading">
        <div class="account-section-heading">
          <FileCheck2 :size="20" aria-hidden="true" />
          <h2 id="appeals-heading">申诉记录</h2>
          <span class="account-count">{{ appeals.length }} 条</span>
        </div>
        <div v-if="!appeals.length" class="account-state">
          <MessageSquareText aria-hidden="true" /><p>暂无申诉记录</p>
        </div>
        <ul v-else class="account-list">
          <li v-for="appeal in appeals" :key="appeal.appealId" class="account-record">
            <div class="account-record-heading">
              <h3>{{ relatedAction(appeal) ? actionLabel(relatedAction(appeal).actionType) : '处罚记录' }} · #{{ appeal.actionId }}</h3>
              <span class="account-badge" :class="appealTone(appeal.status)">{{ appealLabel(appeal.status) }}</span>
            </div>
            <p class="account-record-text">{{ appeal.reason }}</p>
            <p class="account-meta">提交于 {{ formatTime(appeal.createTime) }}</p>
            <div v-if="appeal.reviewResult || appeal.reviewTime" class="appeal-result">
              <strong>处理意见</strong>
              <p class="account-record-text">{{ appeal.reviewResult || '暂无处理意见' }}</p>
              <span v-if="appeal.reviewTime" class="account-meta">处理于 {{ formatTime(appeal.reviewTime) }}</span>
            </div>
          </li>
        </ul>
      </section>
    </template>

    <el-dialog :model-value="Boolean(appealAction)" title="提交申诉" width="520px"
      class="portal-form-dialog" :close-on-click-modal="false" :close-on-press-escape="!submitting"
      :show-close="!submitting" @close="appealAction = null">
      <div v-if="appealAction" class="dialog-context">
        <span class="dialog-context-label">关联处罚 · {{ actionLabel(appealAction.actionType) }}</span>
        {{ appealAction.reason }}
      </div>
      <form id="appeal-form" @submit.prevent="handleAppeal">
        <label for="appeal-reason" class="dialog-field-label"><span class="required-mark" aria-hidden="true">*</span>申诉理由</label>
        <el-input id="appeal-reason" v-model="appealReason" type="textarea" :rows="5" maxlength="3000"
          show-word-limit :disabled="submitting" aria-required="true" :aria-invalid="Boolean(appealError)"
          aria-describedby="appeal-error" />
        <p v-if="appealError" id="appeal-error" class="dialog-error" role="alert">{{ appealError }}</p>
      </form>
      <template #footer>
        <el-button :disabled="submitting" @click="appealAction = null">取消</el-button>
        <el-button type="primary" native-type="submit" form="appeal-form" :loading="submitting">提交申诉</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.governance-page { max-width: 780px; }
.appeal-result { margin-top: 16px; padding-top: 14px; border-top: 1px solid var(--line); }
.appeal-result strong { color: var(--text-strong); font-size: 13px; }
.appeal-result .account-record-text { margin-top: 6px; }
</style>
