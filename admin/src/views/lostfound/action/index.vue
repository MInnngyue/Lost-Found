<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryRef" :model="queryParams" :inline="true">
      <el-form-item label="用户编号" prop="userId"><el-input v-model="queryParams.userId" clearable placeholder="请输入用户编号" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="处罚动作" prop="actionType"><el-select v-model="queryParams.actionType" clearable placeholder="请选择" style="width: 170px"><el-option label="禁止发布" value="POST_PUBLISH" /><el-option label="禁止评论" value="COMMENT" /><el-option label="禁止认领" value="CLAIM_APPLY" /><el-option label="禁止私信" value="PRIVATE_MESSAGE" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8"><right-toolbar v-model:showSearch="showSearch" @queryTable="getList" /></el-row>
    <el-table v-loading="loading" :data="actionList">
      <el-table-column label="编号" prop="actionId" width="90" align="center" /><el-table-column label="案件编号" prop="caseId" width="100" align="center" /><el-table-column label="用户编号" prop="userId" width="100" align="center" /><el-table-column label="处罚动作" prop="actionType" width="150" align="center" /><el-table-column label="范围" prop="scope" width="90" align="center" /><el-table-column label="开始时间" prop="startTime" width="180" align="center"><template #default="scope">{{ parseTime(scope.row.startTime) }}</template></el-table-column><el-table-column label="结束时间" prop="endTime" width="180" align="center"><template #default="scope">{{ scope.row.endTime ? parseTime(scope.row.endTime) : '长期有效' }}</template></el-table-column><el-table-column label="理由" prop="reason" min-width="260" show-overflow-tooltip />
    </el-table>
    <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
  </div>
</template>

<script setup name="LostFoundAction">
import { listViolationAction } from '@/api/lostfound/moderation'
const { proxy } = getCurrentInstance()
const actionList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, userId: undefined, actionType: undefined })
function getList() { loading.value = true; listViolationAction(queryParams).then(response => { actionList.value = response.rows; total.value = response.total; loading.value = false }) }
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
getList()
</script>
