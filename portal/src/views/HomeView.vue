<template>
  <div class="account-page profile-page">
    <header class="account-header">
      <div>
        <h1 class="account-title">个人中心</h1>
        <p v-if="userInfo" class="account-summary">
          {{ userInfo.nickname || userInfo.username || '-' }}，欢迎回来
        </p>
      </div>
      <button
        class="account-icon-button"
        type="button"
        title="刷新个人资料"
        aria-label="刷新个人资料"
        :disabled="loading || postsLoading"
        @click="refreshProfile"
      >
        <RefreshCw :size="17" aria-hidden="true" />
      </button>
    </header>

    <div v-if="loading && !userInfo" class="account-state" role="status">正在加载个人资料...</div>
    <div v-else-if="profileError && !userInfo" class="account-state" role="alert">
      <CircleAlert aria-hidden="true" />
      <p>{{ profileError }}</p>
      <button class="account-button" type="button" @click="refreshProfile">
        <RefreshCw :size="16" aria-hidden="true" />重试
      </button>
    </div>
    <template v-else>
      <section class="profile-record account-record">
        <div class="profile-top">
          <button
            class="avatar avatar-lg"
            type="button"
            title="更换头像"
            aria-label="更换头像"
            :disabled="avatarUploading"
            @click="openAvatarPicker"
          >
            <img v-if="avatarUrl" :src="avatarUrl" alt="" />
            <span v-else>{{ displayName.charAt(0) }}</span>
            <span class="avatar-edit" aria-hidden="true"><Camera :size="15" /></span>
          </button>
          <input
            ref="avatarInput"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/bmp"
            hidden
            @change="handleAvatarChange"
          />
          <div class="profile-main">
            <div class="profile-name">{{ displayName }}</div>
            <div class="profile-sign">校园失物招领用户</div>
          </div>
          <button class="account-button" type="button" @click="openInfoDialog">
            <Pencil :size="16" aria-hidden="true" />编辑资料
          </button>
        </div>

        <div class="stats-row">
          <div class="stat-item">
            <div class="stat-value">{{ postsTotal }}</div>
            <div class="stat-label">帖子总数</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-value">{{ postStats.pending }}</div>
            <div class="stat-label">最近待审核</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-value">{{ postStats.published }}</div>
            <div class="stat-label">最近已发布</div>
          </div>
        </div>
      </section>

      <div class="quick-links" aria-label="快捷入口">
        <button class="quick-link" type="button" @click="router.push('/publish')">
          <FilePenLine :size="22" aria-hidden="true" />
          <span>发布帖子</span>
        </button>
        <button class="quick-link" type="button" @click="router.push('/')">
          <House :size="22" aria-hidden="true" />
          <span>浏览广场</span>
        </button>
        <button class="quick-link" type="button" @click="router.push('/my-posts')">
          <Files :size="22" aria-hidden="true" />
          <span>我的帖子</span>
        </button>
      </div>

      <section class="account-section">
        <div class="account-section-heading">
          <Files :size="20" aria-hidden="true" />
          <h2>最近帖子</h2>
          <span class="account-count">{{ postsTotal }} 条</span>
        </div>
        <div v-if="postsLoading" class="account-state" role="status">正在加载帖子...</div>
        <div v-else-if="postsError" class="account-state" role="alert">
          <CircleAlert aria-hidden="true" />
          <p>{{ postsError }}</p>
          <button class="account-button" type="button" @click="loadMyPosts">
            <RefreshCw :size="16" aria-hidden="true" />重试
          </button>
        </div>
        <div v-else-if="!myPosts.length" class="account-state">
          <FileText aria-hidden="true" />
          <p>还没有发布过帖子</p>
          <button class="account-button" type="button" @click="router.push('/publish')">
            <Plus :size="16" aria-hidden="true" />去发布帖子
          </button>
        </div>
        <ul v-else class="post-list">
          <li v-for="post in myPosts" :key="post.id" class="post-row">
            <button type="button" class="post-row-main" @click="goPost(post)">
              <span class="account-badge" :class="post.type === 0 ? 'is-info' : 'is-success'">
                {{ post.type === 0 ? '寻物' : '招领' }}
              </span>
              <span class="post-title">{{ post.title || '无标题' }}</span>
              <span class="post-time">{{ formatTime(post.createTime) }}</span>
            </button>
          </li>
        </ul>
        <button
          v-if="postsTotal > myPosts.length"
          class="account-button more-button"
          type="button"
          @click="router.push('/my-posts')"
        >
          查看全部帖子
        </button>
      </section>

      <section v-if="userInfo" class="account-section">
        <div class="account-section-heading">
          <UserRound :size="20" aria-hidden="true" />
          <h2>账号信息</h2>
        </div>
        <dl class="info-list">
          <div class="info-row"><dt>用户 ID</dt><dd>{{ userInfo.userId || userInfo.id || '-' }}</dd></div>
          <div class="info-row"><dt>用户名</dt><dd>{{ userInfo.username || '-' }}</dd></div>
          <div class="info-row"><dt>昵称</dt><dd>{{ userInfo.nickname || '-' }}</dd></div>
          <div class="info-row"><dt>邮箱</dt><dd>{{ userInfo.email || '未设置' }}</dd></div>
          <div class="info-row"><dt>手机号</dt><dd>{{ userInfo.phone || '未设置' }}</dd></div>
        </dl>
      </section>
    </template>

    <el-dialog
      v-model="showInfoDialog"
      title="修改个人信息"
      width="420px"
      class="portal-form-dialog"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="昵称">
          <el-input v-model="infoForm.nickname" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="infoForm.phone" maxlength="11" placeholder="选填" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="infoForm.email" maxlength="50" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInfoDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingInfo" @click="saveInfo">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Camera,
  CircleAlert,
  FilePenLine,
  FileText,
  Files,
  House,
  Pencil,
  Plus,
  RefreshCw,
  UserRound,
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { getMyPosts, getUserInfo, updateProfile } from '@/api/user'
import { uploadAvatar } from '@/api/upload'
import { imageUrl } from '@/utils/url'
import { getErrorMessage } from '@/utils/error'
import '@/styles/account-pages.css'

const router = useRouter()
const avatarInput = ref(null)
const userInfo = ref(null)
const myPosts = ref([])
const postsTotal = ref(0)
const loading = ref(false)
const postsLoading = ref(false)
const avatarUploading = ref(false)
const savingInfo = ref(false)
const profileError = ref('')
const postsError = ref('')
const showInfoDialog = ref(false)
const infoForm = ref({ nickname: '', phone: '', email: '' })

const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '校园用户')
const avatarUrl = computed(() => imageUrl(userInfo.value?.avatar))
const postStats = computed(() => ({
  pending: myPosts.value.filter((post) => post.status === 'PENDING_REVIEW').length,
  published: myPosts.value.filter((post) => post.status === 'PUBLISHED').length,
}))

function localUserInfo() {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || 'null')
  } catch {
    localStorage.removeItem('userInfo')
    return null
  }
}

async function loadUserInfo() {
  loading.value = true
  profileError.value = ''
  try {
    const result = await getUserInfo()
    userInfo.value = result.data
    localStorage.setItem('userInfo', JSON.stringify(result.data))
  } catch (error) {
    userInfo.value = localUserInfo()
    if (!userInfo.value) profileError.value = getErrorMessage(error, '个人资料加载失败，请重试')
  } finally {
    loading.value = false
  }
}

async function loadMyPosts() {
  postsLoading.value = true
  postsError.value = ''
  try {
    const result = await getMyPosts({ page: 1, size: 5 })
    myPosts.value = result.data || []
    postsTotal.value = Number(result.total || 0)
  } catch (error) {
    postsError.value = getErrorMessage(error, '帖子加载失败，请重试')
  } finally {
    postsLoading.value = false
  }
}

async function refreshProfile() {
  await Promise.all([loadUserInfo(), loadMyPosts()])
}

function openAvatarPicker() {
  avatarInput.value?.click()
}

async function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/gif', 'image/bmp'].includes(file.type)) {
    ElMessage.warning('请选择 JPG、PNG、GIF 或 BMP 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 5MB')
    return
  }
  avatarUploading.value = true
  try {
    const result = await uploadAvatar(file)
    const imgUrl = result.data?.imgUrl || result.imgUrl
    if (!imgUrl) throw new Error('服务器未返回头像地址')
    userInfo.value = { ...userInfo.value, avatar: imgUrl }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    ElMessage.success('头像已更新')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '头像上传失败，请重试'))
  } finally {
    avatarUploading.value = false
  }
}

function openInfoDialog() {
  infoForm.value = {
    nickname: userInfo.value?.nickname || '',
    phone: userInfo.value?.phone || '',
    email: userInfo.value?.email || '',
  }
  showInfoDialog.value = true
}

async function saveInfo() {
  if (savingInfo.value) return
  savingInfo.value = true
  try {
    await updateProfile(infoForm.value)
    userInfo.value = { ...userInfo.value, ...infoForm.value }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    showInfoDialog.value = false
    ElMessage.success('资料已更新')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '资料更新失败，请重试'))
  } finally {
    savingInfo.value = false
  }
}

function formatTime(value) {
  return value ? String(value).substring(0, 16).replace('T', ' ') : '未记录'
}

function goPost(post) {
  if (['DRAFT', 'PENDING_REVIEW', 'REJECTED'].includes(post.status)) {
    router.push('/my-posts')
    return
  }
  router.push(`/post/${post.id}`)
}

onMounted(refreshProfile)
</script>

<style scoped>
.profile-page { max-width: 780px; }
.profile-record { padding: 24px; }
.profile-top { display: flex; align-items: center; gap: 16px; }
.profile-main { min-width: 0; flex: 1; }
.profile-name { color: var(--text-strong); font-size: 20px; font-weight: 700; overflow-wrap: anywhere; }
.profile-sign { margin-top: 4px; color: var(--text-muted); font-size: 13px; }
.avatar {
  position: relative;
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 50%;
  background: #dbeafe;
  color: #1d4ed8;
  cursor: pointer;
}
.avatar:disabled { cursor: wait; opacity: 0.65; }
.avatar-lg { width: 72px; height: 72px; font-size: 26px; font-weight: 700; }
.avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar-edit {
  position: absolute;
  right: 0;
  bottom: 0;
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  color: #fff;
  background: rgba(15, 23, 42, 0.8);
}
.stats-row { display: flex; align-items: center; margin-top: 24px; padding-top: 18px; border-top: 1px solid var(--line); }
.stat-item { flex: 1; text-align: center; }
.stat-value { color: #2563eb; font-family: 'JetBrains Mono', monospace; font-size: 25px; font-weight: 700; }
.stat-label { margin-top: 4px; color: var(--text-muted); font-size: 12px; }
.stat-divider { width: 1px; height: 34px; background: var(--line); }
.quick-links { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 20px; }
.quick-link {
  display: flex; min-height: 86px; align-items: center; justify-content: center; gap: 8px; flex-direction: column;
  padding: 16px 12px; border: 1px solid rgba(255, 255, 255, 0.62); border-radius: 8px;
  background: rgba(255, 255, 255, 0.35); color: #2563eb; cursor: pointer;
}
.quick-link:hover { border-color: #93c5fd; background: rgba(255, 255, 255, 0.72); }
.quick-link span { color: var(--text-body); font-size: 13px; font-weight: 600; }
.account-section { margin-top: 32px; }
.post-list { display: grid; gap: 0; list-style: none; }
.post-row { max-width: 100%; min-width: 0; border-bottom: 1px solid rgba(15, 23, 42, 0.08); }
.post-row:last-child { border-bottom: 0; }
.post-row-main {
  display: flex; width: 100%; max-width: 100%; min-width: 0; align-items: center; gap: 10px; padding: 13px 4px;
  border: 0; background: transparent; color: inherit; text-align: left; cursor: pointer;
}
.post-row-main:hover { background: rgba(255, 255, 255, 0.5); }
.post-title { min-width: 0; flex: 1; overflow: hidden; color: var(--text-strong); font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.post-time { flex: 0 0 auto; color: var(--text-soft); font-family: 'JetBrains Mono', monospace; font-size: 11px; }
.more-button { margin-top: 14px; }
.info-list { display: grid; margin: 0; }
.info-row { display: flex; justify-content: space-between; gap: 20px; padding: 12px 0; border-bottom: 1px solid rgba(15, 23, 42, 0.08); }
.info-row:last-child { border-bottom: 0; }
.info-row dt { color: var(--text-muted); font-size: 14px; }
.info-row dd { margin: 0; color: var(--text-strong); font-size: 14px; font-weight: 600; text-align: right; overflow-wrap: anywhere; }
@media (max-width: 560px) {
  .profile-record { padding: 18px 16px; }
  .profile-top { align-items: flex-start; flex-wrap: wrap; }
  .profile-main { min-width: calc(100% - 92px); }
  .profile-top > .account-button { width: 100%; }
  .quick-links { grid-template-columns: 1fr; }
  .post-time { display: none; }
}
</style>
