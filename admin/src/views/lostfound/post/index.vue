<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryRef" :model="queryParams" :inline="true">
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" clearable placeholder="标题或描述" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="帖子类型" prop="postType">
        <el-select v-model="queryParams.postType" clearable placeholder="请选择" style="width: 150px">
          <el-option label="寻物" value="LOST" />
          <el-option label="招领" value="FOUND" />
        </el-select>
      </el-form-item>
      <el-form-item label="生命周期" prop="lifecycleStatus">
        <el-select v-model="queryParams.lifecycleStatus" clearable placeholder="请选择" style="width: 170px">
          <el-option label="待审核" value="PENDING_REVIEW" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已解决" value="RESOLVED" />
          <el-option label="已过期" value="EXPIRED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已下架" value="REMOVED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="postList">
      <el-table-column label="编号" prop="postId" align="center" width="90" />
      <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip />
      <el-table-column label="类型" prop="postType" align="center" width="90">
        <template #default="scope">{{ scope.row.postType === 'LOST' ? '寻物' : '招领' }}</template>
      </el-table-column>
      <el-table-column label="生命周期" prop="lifecycleStatus" align="center" width="120">
        <template #default="scope"><el-tag>{{ lifecycleLabel(scope.row.lifecycleStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="审核状态" prop="reviewStatus" align="center" width="110">
        <template #default="scope"><el-tag :type="scope.row.reviewStatus === 'APPROVED' ? 'success' : 'warning'">{{ reviewLabel(scope.row.reviewStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="发布时间" prop="createTime" align="center" width="180">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220">
        <template #default="scope">
          <el-button
            v-if="scope.row.lifecycleStatus === 'PENDING_REVIEW'"
            link
            type="success"
            icon="CircleCheck"
            v-hasPermi="['lostfound:post:review']"
            @click="openReview(scope.row, 'APPROVE')"
          >通过</el-button>
          <el-button
            v-if="scope.row.lifecycleStatus === 'PENDING_REVIEW'"
            link
            type="warning"
            icon="CircleClose"
            v-hasPermi="['lostfound:post:review']"
            @click="openReview(scope.row, 'REJECT')"
          >拒绝</el-button>
          <el-button
            v-if="scope.row.lifecycleStatus === 'PUBLISHED'"
            link
            type="danger"
            icon="Remove"
            v-hasPermi="['lostfound:post:review']"
            @click="openReview(scope.row, 'REMOVE')"
          >下架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />

    <el-dialog v-model="reviewOpen" title="审核帖子" width="520px" append-to-body>
      <el-form ref="reviewRef" :model="reviewForm" :rules="reviewRules" label-width="80px">
        <el-form-item label="帖子标题">
          <span>{{ reviewPostData.title }}</span>
        </el-form-item>
        <el-form-item label="审核动作">
          <el-tag>{{ actionLabel(reviewForm.action) }}</el-tag>
        </el-form-item>
        <el-form-item label="审核理由" prop="reason">
          <el-input v-model="reviewForm.reason" type="textarea" maxlength="500" show-word-limit placeholder="拒绝或下架时必须填写理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitReview">确定</el-button>
        <el-button @click="reviewOpen = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="LostFoundPost">
import { listPost, reviewPost } from '@/api/lostfound/post'

const { proxy } = getCurrentInstance()
const postList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const reviewOpen = ref(false)
const reviewPostData = ref({})
const reviewForm = ref({})
const reviewRules = {
  reason: [{ required: true, message: '审核理由不能为空', trigger: 'blur' }]
}

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: undefined,
  postType: undefined,
  lifecycleStatus: undefined
})

function getList() {
  loading.value = true
  listPost(queryParams.value).then(response => {
    postList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function openReview(row, action) {
  reviewPostData.value = row
  reviewForm.value = { postId: row.postId, action, reason: undefined }
  reviewOpen.value = true
}

function submitReview() {
  proxy.$refs.reviewRef.validate(valid => {
    if (!valid) return
    reviewPost(reviewForm.value).then(() => {
      proxy.$modal.msgSuccess('审核完成')
      reviewOpen.value = false
      getList()
    })
  })
}

function lifecycleLabel(status) {
  return { PENDING_REVIEW: '待审核', PUBLISHED: '已发布', RESOLVED: '已解决', EXPIRED: '已过期', REJECTED: '已拒绝', REMOVED: '已下架' }[status] || status
}

function reviewLabel(status) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '未通过' }[status] || status
}

function actionLabel(action) {
  return { APPROVE: '通过', REJECT: '拒绝', REMOVE: '下架' }[action] || action
}

getList()
</script>
