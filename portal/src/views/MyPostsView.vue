<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyPosts } from '@/api/user'
import { updatePost, submitPost, withdrawPost } from '@/api/post'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePostCategories } from '@/composables/usePostCategories'
import { postRules, disabledEventDate } from '@/utils/postForm'
import { ArrowLeft, CircleAlert, Eye, FileText, Pencil, Plus, RefreshCw, Undo2 } from 'lucide-vue-next'
import { getErrorMessage } from '@/utils/error'
import '@/styles/account-pages.css'

const router = useRouter()
const posts = ref([])
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const saveError = ref('')
const withdrawing = ref(null)
const postsTotal = ref(0)
const page = ref(1)
const pageSize = 20

const editFormRef = ref(null)

const showEditDialog = ref(false)
const editForm = ref({
  id: null, type: 1, itemCategory: '', color: '', title: '', description: '',
  locationCampus: '', locationArea: '', locationDetail: '', lostTime: '', images: []
})
const { itemCategories: categories, colors, campuses, areas, details, ready: categoriesReady,
  loading: categoriesLoading, error: categoriesError, loadCategories, onCampusChange,
  onAreaChange } = usePostCategories(editForm)

onMounted(async () => {
  await Promise.all([loadPosts(), loadCategories()])
})

async function openEdit(p) {
  if (!isEditable(p.status)) return
  editForm.value = {
    id: p.id, type: p.type, itemCategory: p.itemCategoryId || '', color: p.colorCategoryId || '',
    title: p.title || '', description: p.description || '',
    locationCampus: p.campusCategoryId || '', locationArea: p.areaCategoryId || '',
    locationDetail: p.locationDetail || '',
    lostTime: (p.lostTime || '').substring(0, 10),
    images: p.images || []
  }
  showEditDialog.value = true
  saveError.value = ''
  editFormRef.value?.clearValidate()
  await loadCategories()
}

async function saveEdit() {
  if (saving.value || !categoriesReady.value) return
  saving.value = true
  saveError.value = ''
  try {
    const valid = await editFormRef.value.validate().catch(() => false)
    if (!valid) return
    const postId = editForm.value.id
    await updatePost(postId, { ...editForm.value, title: editForm.value.title.trim() })
    try {
      await submitPost(postId)
      ElMessage.success('修改已保存，并已重新提交审核')
    } catch {
      ElMessage.warning('修改已保存，但提交审核失败，请稍后重试')
    }
    showEditDialog.value = false
    await loadPosts()
  } catch (error) {
    saveError.value = getErrorMessage(error, '保存失败，请重试')
  }
  finally { saving.value = false }
}

async function handleWithdraw(id) {
  if (withdrawing.value !== null) return
  withdrawing.value = id
  try {
    await ElMessageBox.confirm('撤回后帖子将不再出现在广场中，确定继续吗？', '确认撤回', { type: 'warning' })
    await withdrawPost(id)
    ElMessage.success('帖子已撤回')
    await loadPosts()
  } catch {
    // Cancellation needs no message; API errors are shown by the request layer.
  } finally { withdrawing.value = null }
}

function typeLabel(t) { return t === 0 ? '寻物' : '招领' }
function statusLabel(s) {
  const map = { DRAFT: '草稿', PENDING_REVIEW: '待审核', PUBLISHED: '已发布', RESOLVED: '已解决', EXPIRED: '已过期', REJECTED: '审核未通过', WITHDRAWN: '已撤回', REMOVED: '已下架', 0: '进行中', 1: '已匹配', 2: '认领中', 3: '已完结', 4: '已归档', 5: '已下架' }
  return map[s] || '未知'
}
function isEditable(status) { return status === 'DRAFT' || status === 'REJECTED' }
function statusTone(status) {
  return { PUBLISHED: 'is-success', RESOLVED: 'is-success', PENDING_REVIEW: 'is-info',
    REJECTED: 'is-danger', REMOVED: 'is-danger' }[status] || ''
}
function isWithdrawable(status) {
  return status === 'DRAFT' || status === 'PENDING_REVIEW' || status === 'PUBLISHED'
}
function canViewPublic(status) { return status === 'PUBLISHED' || status === 'RESOLVED' }
function goPost(post) {
  if (canViewPublic(post.status)) router.push(`/post/${post.id}`)
}

async function loadPosts() {
  if (loading.value) return
  loading.value = true
  loadError.value = ''
  try {
    const result = await getMyPosts({ page: page.value, size: pageSize })
    posts.value = result.data || []
    postsTotal.value = Number(result.total || 0)
  }
  catch (error) { loadError.value = getErrorMessage(error, '帖子加载失败，请重试') }
  finally { loading.value = false }
}
</script>

<template>
  <div class="account-page myposts-page">
    <header class="account-header">
      <div>
        <h1 class="account-title">我的帖子</h1>
        <p v-if="!loading && !loadError" class="account-summary">共 {{ postsTotal }} 条帖子</p>
      </div>
      <div class="account-toolbar">
        <button class="account-icon-button" type="button" title="返回个人中心" aria-label="返回个人中心" @click="router.push('/profile')"><ArrowLeft :size="18" /></button>
        <button class="account-icon-button" type="button" title="刷新帖子" aria-label="刷新帖子" :disabled="loading" @click="loadPosts"><RefreshCw :size="17" /></button>
        <button class="account-button is-primary" type="button" @click="router.push('/publish')"><Plus :size="17" />发布帖子</button>
      </div>
    </header>

    <div v-if="loading" class="account-state" role="status">正在加载帖子...</div>
    <div v-else-if="loadError" class="account-state" role="alert">
      <CircleAlert aria-hidden="true" /><p>{{ loadError }}</p>
      <button class="account-button" type="button" @click="loadPosts"><RefreshCw :size="16" />重试</button>
    </div>
    <div v-else-if="!posts.length" class="account-state">
      <FileText aria-hidden="true" /><p>还没有发布过帖子</p>
      <button class="account-button" type="button" @click="router.push('/publish')"><Plus :size="16" />去发布帖子</button>
    </div>
    <ul v-else class="account-list" aria-label="我的帖子列表">
      <li v-for="row in posts" :key="row.id" class="account-record">
        <div class="account-record-heading">
          <span class="account-badge" :class="row.type === 0 ? 'is-info' : 'is-success'">{{ typeLabel(row.type) }}</span>
          <span class="account-badge" :class="statusTone(row.status)">{{ statusLabel(row.status) }}</span>
          <span class="account-count">#{{ row.id }}</span>
        </div>
        <h2 class="post-record-title">{{ row.title }}</h2>
        <p v-if="row.description" class="post-record-description">{{ row.description }}</p>
        <div class="account-meta">
          <span v-if="row.itemCategory">{{ row.itemCategory }}</span>
          <span>{{ [row.locationCampus, row.locationArea, row.locationDetail].filter(Boolean).join(' · ') || '地点未记录' }}</span>
        </div>
        <div class="account-record-footer">
          <span class="account-meta">{{ row.type === 0 ? '遗失' : '拾获' }}日期 {{ (row.lostTime || '').substring(0, 10) || '未记录' }}</span>
          <div class="account-toolbar">
            <button v-if="canViewPublic(row.status)" class="account-icon-button" type="button" title="查看公开帖子" aria-label="查看公开帖子" @click="goPost(row)"><Eye :size="17" /></button>
            <span v-else class="private-state">仅本人可见</span>
            <button v-if="isEditable(row.status)" class="account-icon-button" type="button" title="编辑并重新提交审核" aria-label="编辑并重新提交审核" @click="openEdit(row)"><Pencil :size="17" /></button>
            <button v-if="isWithdrawable(row.status)" class="account-icon-button is-danger" type="button" title="撤回" aria-label="撤回帖子" :disabled="withdrawing !== null" @click="handleWithdraw(row.id)"><Undo2 :size="17" /></button>
          </div>
        </div>
      </li>
    </ul>
    <div v-if="postsTotal > pageSize" class="posts-pagination">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="postsTotal"
        :page-size="pageSize"
        v-model:current-page="page"
        @current-change="loadPosts"
      />
    </div>

    <el-dialog v-model="showEditDialog" title="编辑帖子" width="580px" class="portal-form-dialog"
      :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving">
      <p v-if="categoriesLoading" class="dialog-context" role="status">正在加载分类...</p>
      <div v-if="categoriesError" role="status">
        <el-alert :title="categoriesError" type="warning" :closable="false" show-icon />
        <el-button :loading="categoriesLoading" @click="loadCategories">重试</el-button>
      </div>
      <el-form ref="editFormRef" :model="editForm" :rules="postRules" label-position="top" class="edit-form" :disabled="saving || !categoriesReady" @submit.prevent="saveEdit">
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="editForm.type">
            <el-radio :value="0">寻物启事</el-radio>
            <el-radio :value="1">失物招领</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="editForm.title" maxlength="128" placeholder="请输入帖子标题" />
        </el-form-item>
        <el-form-item label="物品分类" prop="itemCategory">
          <el-select v-model="editForm.itemCategory" placeholder="选择物品分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-select v-model="editForm.color" placeholder="选择颜色" style="width:100%">
            <el-option v-for="c in colors" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="地点" prop="locationCampus">
          <div class="edit-location">
            <el-select v-model="editForm.locationCampus" placeholder="校区" style="flex:1" @change="onCampusChange">
              <el-option v-for="c in campuses" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-select v-model="editForm.locationArea" placeholder="区域（选填）" style="flex:1" :disabled="!editForm.locationCampus || !areas.length" @change="onAreaChange">
              <el-option v-for="c in areas" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-select v-if="details.length > 0" v-model="editForm.locationDetail" placeholder="具体地点" style="flex:1">
              <el-option v-for="c in details" :key="c.id" :label="c.name" :value="c.name" />
            </el-select>
            <el-input v-else v-model="editForm.locationDetail" placeholder="具体地点（选填）" maxlength="255" style="flex:1" :disabled="!editForm.locationCampus" />
          </div>
        </el-form-item>
        <el-form-item label="时间" prop="lostTime">
          <el-date-picker v-model="editForm.lostTime" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" :disabled-date="disabledEventDate" style="width:100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="请详细描述物品特征..." />
        </el-form-item>
      </el-form>
      <p v-if="saveError" class="dialog-error" role="alert">{{ saveError }}</p>
      <template #footer>
        <el-button :disabled="saving" @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!categoriesReady" @click="saveEdit">保存并提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.post-record-title { margin: 12px 0 8px; color: var(--text-strong); font-size: 18px; line-height: 1.5; overflow-wrap: anywhere; }
.post-record-description { display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; margin-bottom: 12px; color: var(--text-muted); font-size: 14px; overflow-wrap: anywhere; }
.edit-form { padding: 4px 0; }
.private-state { color: var(--text-soft); font-size: 12px; white-space: nowrap; }
.posts-pagination { display: flex; justify-content: center; margin-top: 24px; }
.edit-location { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; width: 100%; }
@media (max-width: 600px) {
  .edit-location { grid-template-columns: minmax(0, 1fr); }
}
</style>
