<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryRef" :model="queryParams" :inline="true">
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择" style="width: 150px">
          <el-option label="建议" value="SUGGESTED" />
          <el-option label="已查看" value="VIEWED" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="已忽略" value="IGNORED" />
          <el-option label="已失效" value="EXPIRED" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-select v-model="queryParams.source" clearable placeholder="请选择" style="width: 150px">
          <el-option label="自动匹配" value="AUTO" />
          <el-option label="人工创建" value="MANUAL" />
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

    <el-table v-loading="loading" :data="matchList">
      <el-table-column label="编号" prop="matchId" width="90" align="center" />
      <el-table-column label="寻物帖子" prop="lostPostId" width="110" align="center" />
      <el-table-column label="招领帖子" prop="foundPostId" width="110" align="center" />
      <el-table-column label="总分" prop="totalScore" width="90" align="center">
        <template #default="scope"><el-tag type="success">{{ scope.row.totalScore }}</el-tag></template>
      </el-table-column>
      <el-table-column label="物品" prop="itemScore" width="80" align="center" />
      <el-table-column label="颜色" prop="colorScore" width="80" align="center" />
      <el-table-column label="位置" prop="locationScore" width="80" align="center" />
      <el-table-column label="日期" prop="timeScore" width="80" align="center" />
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="scope">{{ statusLabel(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column label="来源" prop="source" width="100" align="center" />
      <el-table-column label="创建时间" prop="createTime" width="180" align="center">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
      :total="total" @pagination="getList" />
  </div>
</template>

<script setup name="LostFoundMatch">
import { listMatch } from '@/api/lostfound/match'

const { proxy } = getCurrentInstance()
const matchList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, status: undefined, source: undefined })

function getList() {
  loading.value = true
  listMatch(queryParams).then(response => {
    matchList.value = response.rows
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
    SUGGESTED: '建议',
    VIEWED: '已查看',
    CONFIRMED: '已确认',
    IGNORED: '已忽略',
    EXPIRED: '已失效'
  }[status] || status
}
getList()
</script>
