<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyClaims, confirmClaim, cancelClaim, rejectClaim } from '@/api/claim'
import ReportDialog from '@/components/ReportDialog.vue'
import { ElMessage } from 'element-plus'
import { CircleAlert, Flag, RefreshCw } from 'lucide-vue-next'
import '@/styles/account-pages.css'

const router = useRouter()
const claims = ref([])
const loading = ref(true)
const confirming = ref(null)
const canceling = ref(null)
const rejecting = ref(null)
const reportTarget = ref(null)
const loadingError = ref('')
const rejectReason = ref('')

const searchText = ref('')
const filterStatus = ref('all')

async function loadClaims() {
  loading.value = true
  loadingError.value = ''
  try { claims.value = (await getMyClaims()).data || [] }
  catch (error) { loadingError.value = error?.msg || error?.message || '认领记录加载失败，请重试' }
  finally { loading.value = false }
}

onMounted(loadClaims)

function typeLabel(t) { return t === 0 ? '寻物' : '招领' }
function statusCn(s) {
  const map = {
    PENDING: '待发布者接受',
    ACCEPTED: '已接受，待认领人确认',
    HANDOFF: '交接中，待发布者确认',
    COMPLETED: '已完结',
    CANCELLED: '已取消',
    REJECTED: '已拒绝',
    DISPUTED: '处理中',
  }
  return map[s] || '未知'
}

function sortPriority(c) {
  if (['COMPLETED', 'CANCELLED', 'REJECTED', 'DISPUTED'].includes(c.status)) return 3
  if (c.status === 'HANDOFF') return 1
  return 0
}

function isOpen(c) {
  return !['COMPLETED', 'CANCELLED', 'REJECTED', 'DISPUTED'].includes(c.status)
}

const filteredClaims = computed(() => {
  let list = [...claims.value]
  list.sort((a, b) => sortPriority(a) - sortPriority(b))
  if (searchText.value.trim()) {
    const q = searchText.value.trim().toLowerCase()
    list = list.filter(c =>
      c.postTitle?.toLowerCase().includes(q) ||
      typeLabel(c.postType).includes(q) ||
      c.otherPartyName?.toLowerCase().includes(q)
    )
  }
  if (filterStatus.value === 'pending') {
    list = list.filter(c => {
      return isOpen(c) && ((c.myRole === 'owner' && c.status === 'HANDOFF')
        || (c.myRole === 'applicant' && c.status === 'ACCEPTED'))
    })
  } else if (filterStatus.value === 'waiting') {
    list = list.filter(c => {
      return isOpen(c) && ((c.myRole === 'owner' && c.status === 'ACCEPTED')
        || (c.myRole === 'applicant' && c.status === 'HANDOFF'))
    })
  } else if (filterStatus.value === 'done') {
    list = list.filter(c => c.status === 'COMPLETED')
  }
  return list
})

function progressStep(c) {
  if (c.status === 'COMPLETED') return 3
  if (c.status === 'HANDOFF') return 2
  if (c.status === 'ACCEPTED') return 1
  return 0
}

function actionLabel(c) {
  if (c.status === 'COMPLETED') return '已完成'
  if (['CANCELLED', 'REJECTED', 'DISPUTED'].includes(c.status)) return statusCn(c.status)
  if (c.myRole === 'owner' && c.status === 'PENDING') return '待我接受'
  if (c.myRole === 'applicant' && c.status === 'PENDING') return '等待发布者接受'
  if (c.myRole === 'applicant' && c.status === 'ACCEPTED') return '待我确认交接'
  if (c.myRole === 'owner' && c.status === 'ACCEPTED') return '等待认领人确认交接'
  if (c.myRole === 'owner' && c.status === 'HANDOFF') return '待我完成确认'
  return '流程处理中'
}

function canConfirm(c) {
  if (['COMPLETED', 'CANCELLED', 'REJECTED', 'DISPUTED'].includes(c.status)) return false
  return (c.myRole === 'owner' && ['PENDING', 'HANDOFF'].includes(c.status))
    || (c.myRole === 'applicant' && c.status === 'ACCEPTED')
}

function canReject(c) {
  return c.myRole === 'owner' && ['PENDING', 'ACCEPTED', 'HANDOFF'].includes(c.status)
}

async function handleConfirm(c) {
  confirming.value = c.claimId
  try {
    await confirmClaim(c.claimId)
    ElMessage.success('确认成功！对方已收到通知')
    claims.value = (await getMyClaims()).data
  } catch { }
  finally { confirming.value = null }
}

async function handleCancel(c) {
  canceling.value = c.claimId
  try {
    await cancelClaim(c.claimId)
    ElMessage.success('已取消认领')
    claims.value = (await getMyClaims()).data
  } catch { }
  finally { canceling.value = null }
}

async function handleReject() {
  if (!rejecting.value || !rejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  const claim = rejecting.value
  rejecting.value = null
  try {
    await rejectClaim(claim.claimId, rejectReason.value.trim())
    ElMessage.success('已拒绝该认领申请')
    rejectReason.value = ''
    await loadClaims()
  } catch {
    rejecting.value = claim
  }
}

function openReject(c) {
  rejecting.value = c
  rejectReason.value = ''
}

function reportClaim(c) {
  reportTarget.value = {
    targetType: 'CLAIM', targetId: c.claimId, reasonCode: 'ABUSE', summary: c.postTitle,
  }
}

function goPost(id) { router.push(`/post/${id}`) }
</script>

<template>
  <div class="account-page claims-page">
    <header class="account-header">
      <div>
        <h1 class="account-title">认领进度</h1>
        <p class="account-summary">跟踪你的认领申请状态</p>
      </div>
    </header>

    <!-- search & filter -->
    <div class="search-bar">
      <el-input v-model="searchText" placeholder="搜索帖子标题、类型或对方昵称..." clearable prefix-icon="Search" />
      <div class="filter-chips">
        <button class="claim-tab" :class="{ active: filterStatus === 'all' }" @click="filterStatus = 'all'">全部</button>
        <button class="claim-tab" :class="{ active: filterStatus === 'pending' }" @click="filterStatus = 'pending'">待我确认</button>
        <button class="claim-tab" :class="{ active: filterStatus === 'waiting' }" @click="filterStatus = 'waiting'">等待对方</button>
        <button class="claim-tab" :class="{ active: filterStatus === 'done' }" @click="filterStatus = 'done'">已完成</button>
      </div>
    </div>

    <!-- claims list -->
    <div v-if="loadingError" class="account-state" role="alert">
      <CircleAlert aria-hidden="true" />
      <p>{{ loadingError }}</p>
      <button class="account-button" type="button" @click="loadClaims">
        <RefreshCw :size="16" aria-hidden="true" />重试
      </button>
    </div>
    <div v-else class="claims-list" v-loading="loading">
      <div v-if="!loading && filteredClaims.length === 0" class="empty-state">
        <p class="empty-text">还没有认领记录</p>
        <p class="empty-hint">去广场看看有没有你丢的东西</p>
        <button class="account-button is-primary" type="button" @click="router.push('/')">去失物广场</button>
      </div>

      <div v-for="c in filteredClaims" :key="c.claimId" class="account-record claim-card" :class="'status-' + c.status">
          <div class="claim-header" @click="goPost(c.postId)">
            <span class="tag-type" :class="c.postType === 0 ? 'lost' : 'found'">{{ typeLabel(c.postType) }}</span>
            <span class="claim-title">{{ c.postTitle }}</span>
            <span class="tag-status" :class="'s-' + c.status">{{ statusCn(c.status) }}</span>
          </div>

          <div class="party-row">
            <div class="party-card" :class="c.myRole === 'owner' ? 'pc-owner' : 'pc-claimer'">
              <span class="party-label">{{ c.myRole === 'owner' ? '我(发布者)' : '我(认领人)' }}</span>
              <span class="party-confirm" v-if="c.myRole === 'owner' ? ['ACCEPTED', 'HANDOFF', 'COMPLETED'].includes(c.status) : ['HANDOFF', 'COMPLETED'].includes(c.status)">已确认</span>
              <span class="party-confirm pending" v-else>待确认</span>
            </div>
            <span class="party-arrow">&harr;</span>
            <div class="party-card" :class="c.myRole === 'owner' ? 'pc-claimer' : 'pc-owner'">
              <span class="party-label">{{ c.otherPartyName }}</span>
              <span class="party-hint">{{ c.myRole === 'owner' ? '认领人' : '发布者' }}</span>
              <span class="party-confirm" v-if="c.myRole === 'owner' ? ['HANDOFF', 'COMPLETED'].includes(c.status) : ['ACCEPTED', 'HANDOFF', 'COMPLETED'].includes(c.status)">已确认</span>
              <span class="party-confirm pending" v-else>待确认</span>
            </div>
          </div>

          <div class="progress-bar">
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: `${progressStep(c) * 33.333}%` }"></div>
            </div>
            <div class="progress-labels">
              <span :class="progressStep(c) >= 1 ? 'pl-done' : 'pl-active'">发布者接受</span>
              <span :class="progressStep(c) >= 2 ? 'pl-done' : (progressStep(c) === 1 ? 'pl-active' : '')">交接确认</span>
              <span :class="progressStep(c) >= 3 ? 'pl-done' : ''">完结</span>
            </div>
          </div>

          <div class="claim-meta">
            <span class="claim-time">{{ c.updateTime?.substring(0, 16).replace('T', ' ') }}</span>
            <span class="action-hint" v-if="!['COMPLETED', 'CANCELLED', 'REJECTED', 'DISPUTED'].includes(c.status)">{{ actionLabel(c) }}</span>
            <span class="action-hint done" v-else>{{ actionLabel(c) }}</span>
          </div>

          <div class="claim-evidence">
            <div>
              <span class="evidence-label">认领依据</span>
              <p>{{ c.evidenceText || '未填写认领依据' }}</p>
            </div>
            <div v-if="c.ownerRemark" class="owner-remark">
              <span class="evidence-label">发布者备注</span>
              <p>{{ c.ownerRemark }}</p>
            </div>
          </div>

          <div class="claim-actions" v-if="!['COMPLETED', 'CANCELLED', 'REJECTED', 'DISPUTED'].includes(c.status)">
            <button v-if="canConfirm(c)" class="account-button is-primary" type="button" :disabled="confirming === c.claimId" @click="handleConfirm(c)">
              {{ confirming === c.claimId ? '处理中...' : (c.myRole === 'owner' && c.status === 'PENDING' ? '接受认领' : c.status === 'HANDOFF' ? '完成确认' : '确认交接') }}
            </button>
            <button class="claim-cancel-button" type="button" :disabled="canceling === c.claimId" @click="handleCancel(c)">
              {{ canceling === c.claimId ? '取消中...' : '取消认领' }}
            </button>
            <button v-if="canReject(c)" class="claim-reject-button" :disabled="rejecting === c.claimId" type="button" @click="openReject(c)">
              拒绝认领
            </button>
            <button class="claim-report-button" type="button" @click="reportClaim(c)">
              <Flag :size="15" aria-hidden="true" />
              举报
            </button>
          </div>
          <button v-else-if="c.status === 'COMPLETED' || c.status === 'DISPUTED'" class="claim-report-button" type="button" @click="reportClaim(c)">
            <Flag :size="15" aria-hidden="true" />
            举报认领过程
          </button>
        </div>
    </div>
  </div>
  <ReportDialog v-model="reportTarget" />
  <el-dialog
    :model-value="Boolean(rejecting)"
    title="拒绝认领"
    width="440px"
    class="portal-form-dialog"
    :close-on-click-modal="false"
    @close="rejecting = null"
  >
    <p class="dialog-context">请说明拒绝原因，提交后认领人将收到通知。</p>
    <el-input v-model="rejectReason" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请输入拒绝原因" />
    <template #footer>
      <el-button @click="rejecting = null">取消</el-button>
      <el-button type="primary" :disabled="!rejectReason.trim()" @click="handleReject">确认拒绝</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.claims-page { max-width: 800px; }
.claim-tab {
  min-height: 36px; padding: 7px 14px; border: 1px solid var(--line); border-radius: 999px;
  background: rgba(255, 255, 255, 0.55); color: var(--text-muted); font-size: 13px; font-weight: 600; cursor: pointer;
}
.claim-tab:hover { border-color: #93c5fd; color: #1d4ed8; }
.claim-tab.active { border-color: #1d4ed8; background: #1d4ed8; color: #fff; }
.claim-cancel-button {
  min-height: 36px; padding: 7px 14px; border: 1px solid #fecaca; border-radius: 999px;
  background: #fff7f7; color: #b91c1c; font-size: 13px; font-weight: 600; cursor: pointer;
}
.claim-cancel-button:hover { background: #fef2f2; }
.claim-report-button {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  min-height: 36px; padding: 7px 14px; border: 1px solid var(--line);
  border-radius: 999px; background: rgba(255, 255, 255, 0.5); color: var(--text-muted);
  font-size: 13px; font-weight: 600; cursor: pointer;
}
.claim-report-button:hover { background: rgba(255, 255, 255, 0.9); color: var(--text-strong); }
.claim-report-button:focus-visible { outline: 3px solid var(--focus-ring); outline-offset: 3px; }
.claim-report-button svg { flex: none; }

/* search bar */
.search-bar { margin-bottom: 24px; display: flex; flex-direction: column; gap: 12px; }
.search-bar .el-input { max-width: 520px; }
.filter-chips { display: flex; gap: 8px; flex-wrap: wrap; }

/* claims list */
.claims-list { display: flex; flex-direction: column; gap: 20px; }

/* claim card */
.claim-card.status-3 { opacity: 0.6; }
.claim-header { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; cursor: pointer; }
.claim-title { flex: 1; font-size: 16px; font-weight: 600; color: var(--text-strong); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* party row */
.party-row { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.party-card {
  flex: 1; padding: 12px 16px; border-radius: 8px;
  display: flex; flex-direction: column; gap: 4px;
}
.pc-owner { background: #eff6ff; border: 1px solid #bfdbfe; }
.pc-claimer { background: #fffbeb; border: 1px solid #fde68a; }
.party-label { font-size: 14px; font-weight: 700; color: var(--text-strong); }
.party-hint { font-size: 11px; color: var(--text-muted); }
.party-confirm { font-size: 12px; font-weight: 600; color: #047857; }
.party-confirm.pending { color: var(--text-soft); }
.party-arrow { font-size: 18px; color: var(--text-soft); flex-shrink: 0; }

/* progress */
.progress-bar { margin-bottom: 12px; }
.progress-track { height: 3px; background: var(--line); border-radius: 2px; margin-bottom: 8px; }
.progress-fill { height: 100%; background: #2563eb; border-radius: 2px; transition: width 0.5s; }
.progress-labels { display: flex; justify-content: space-between; font-size: 11px; color: var(--text-soft); }
.pl-active { color: #2563eb; font-weight: 600; }
.pl-done { color: #b45309; font-weight: 600; }

/* meta & actions */
.claim-meta { display: flex; justify-content: space-between; align-items: center; padding-top: 12px; border-top: 1px solid #f3f4f6; margin-bottom: 12px; }
.claim-time { font-family: 'JetBrains Mono', monospace; font-size: 12px; color: var(--text-soft); }
.action-hint { font-size: 13px; color: var(--text-muted); }
.action-hint.done { color: #b45309; font-weight: 600; }
.claim-actions { display: flex; gap: 10px; }
.claim-actions .btn-secondary,
.claim-card > .btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.claim-evidence { display: grid; gap: 12px; margin-bottom: 16px; padding: 13px 14px; border: 1px solid rgba(15, 23, 42, 0.08); border-radius: 8px; background: rgba(248, 250, 252, 0.7); }
.claim-evidence p { margin: 4px 0 0; color: var(--text-body); font-size: 13px; line-height: 1.6; white-space: pre-wrap; overflow-wrap: anywhere; }
.evidence-label { color: var(--text-muted); font-size: 12px; font-weight: 700; }
.owner-remark { padding-top: 10px; border-top: 1px solid var(--line); }
.claim-reject-button { min-height: 36px; padding: 7px 14px; border: 1px solid #fecaca; border-radius: 999px; background: #fff7f7; color: #b91c1c; font-size: 13px; font-weight: 600; cursor: pointer; }
.claim-reject-button:hover { background: #fef2f2; border-color: #fca5a5; }
.dialog-context { color: var(--text-body); font-size: 14px; line-height: 1.6; }
@media (max-width: 600px) {
  .claim-header { align-items: flex-start; flex-wrap: wrap; }
  .claim-title { flex-basis: calc(100% - 70px); }
  .party-row { align-items: stretch; flex-direction: column; }
  .party-arrow { align-self: center; transform: rotate(90deg); }
  .claim-actions { flex-wrap: wrap; }
}
</style>
