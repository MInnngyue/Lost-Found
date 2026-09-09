<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryRef" :model="queryParams" :inline="true">
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择" style="width: 160px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已接受" value="ACCEPTED" />
          <el-option label="交接中" value="HANDOFF" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="有争议" value="DISPUTED" />
        </el-select>
      </el-form-item>
      <el-form-item label="帖子编号" prop="postId">
        <el-input v-model="queryParams.postId" clearable placeholder="请输入帖子编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="claimList">
      <el-table-column label="编号" prop="claimId" width="90" align="center" />
      <el-table-column label="帖子" prop="postTitle" min-width="220" show-overflow-tooltip />
      <el-table-column label="帖子编号" prop="postId" width="100" align="center" />
      <el-table-column label="申请人" prop="applicantId" width="100" align="center" />
      <el-table-column label="帖子发布者" prop="postOwnerId" width="110" align="center" />
      <el-table-column label="状态" prop="status" width="120" align="center">
        <template #default="scope"><el-tag>{{ statusLabel(scope.row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="认领依据" prop="evidenceText" min-width="260" show-overflow-tooltip />
      <el-table-column label="更新时间" prop="updateTime" width="180" align="center">
        <template #default="scope">{{ parseTime(scope.row.updateTime) }}</template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
      :total="total" @pagination="getList" />
  </div>
</template>

<script setup name="LostFoundClaim">
import { listClaim } from '@/api/lostfound/claim'

const { proxy } = getCurrentInstance()
const claimList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, status: undefined, postId: undefined })

function getList() {
  loading.value = true
  listClaim(queryParams).then(response => {
    claimList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function statusLabel(status) {
  return {
    PENDING: '待处理',
    ACCEPTED: '已接受',
    HANDOFF: '交接中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REJECTED: '已拒绝',
    DISPUTED: '有争议'
  }[status] || status
}

getList()
</script>
