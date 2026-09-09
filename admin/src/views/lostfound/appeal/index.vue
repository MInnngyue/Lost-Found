<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryRef" :model="queryParams" :inline="true">
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" clearable placeholder="请选择" style="width: 150px"><el-option label="待审核" value="SUBMITTED" /><el-option label="已通过" value="APPROVED" /><el-option label="已驳回" value="REJECTED" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8"><right-toolbar v-model:showSearch="showSearch" @queryTable="getList" /></el-row>
    <el-table v-loading="loading" :data="appealList">
      <el-table-column label="编号" prop="appealId" width="90" align="center" /><el-table-column label="案件编号" prop="caseId" width="100" align="center" /><el-table-column label="处罚编号" prop="actionId" width="100" align="center" /><el-table-column label="申诉人" prop="appellantId" width="100" align="center" /><el-table-column label="申诉理由" prop="reason" min-width="260" show-overflow-tooltip /><el-table-column label="状态" prop="status" width="100" align="center" /><el-table-column label="提交时间" prop="createTime" width="180" align="center"><template #default="scope">{{ parseTime(scope.row.createTime) }}</template></el-table-column><el-table-column label="操作" width="100" align="center"><template #default="scope"><el-button v-if="scope.row.status === 'SUBMITTED'" link type="primary" icon="Edit" v-hasPermi="['lostfound:appeal:handle']" @click="openReview(scope.row)">审核</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    <el-dialog v-model="dialogOpen" title="审核申诉" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px"><el-form-item label="审核结果" prop="status"><el-radio-group v-model="form.status"><el-radio value="APPROVED">通过</el-radio><el-radio value="REJECTED">驳回</el-radio></el-radio-group></el-form-item><el-form-item label="处理说明" prop="reviewResult"><el-input v-model="form.reviewResult" type="textarea" maxlength="2000" show-word-limit /></el-form-item></el-form>
      <template #footer><el-button type="primary" @click="submitReview">确定</el-button><el-button @click="dialogOpen = false">取消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="LostFoundAppeal">
import { listAppeal, reviewAppeal } from '@/api/lostfound/moderation'
const { proxy } = getCurrentInstance()
const appealList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dialogOpen = ref(false)
const form = ref({})
const queryParams = reactive({ pageNum: 1, pageSize: 10, status: undefined })
const rules = { status: [{ required: true, message: '审核结果不能为空', trigger: 'change' }], reviewResult: [{ required: true, message: '处理说明不能为空', trigger: 'blur' }] }
function getList() { loading.value = true; listAppeal(queryParams).then(response => { appealList.value = response.rows; total.value = response.total; loading.value = false }) }
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function openReview(row) { form.value = { appealId: row.appealId, status: 'APPROVED', reviewResult: undefined }; dialogOpen.value = true }
function submitReview() { proxy.$refs.formRef.validate(valid => { if (!valid) return; reviewAppeal(form.value.appealId, form.value).then(() => { proxy.$modal.msgSuccess('申诉审核完成'); dialogOpen.value = false; getList() }) }) }
getList()
</script>
