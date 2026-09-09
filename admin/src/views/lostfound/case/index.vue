<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryRef" :model="queryParams" :inline="true">
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择" style="width: 150px">
          <el-option label="处理中" value="OPEN" />
          <el-option label="已解决" value="RESOLVED" />
          <el-option label="已驳回" value="DISMISSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="严重程度" prop="severity">
        <el-select v-model="queryParams.severity" clearable placeholder="请选择" style="width: 150px">
          <el-option label="低" value="LOW" /><el-option label="中" value="MEDIUM" />
          <el-option label="高" value="HIGH" /><el-option label="严重" value="CRITICAL" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8"><right-toolbar v-model:showSearch="showSearch" @queryTable="getList" /></el-row>
    <el-table v-loading="loading" :data="caseList">
      <el-table-column label="编号" prop="caseId" width="90" align="center" />
      <el-table-column label="涉事用户" prop="subjectUserId" width="100" align="center" />
      <el-table-column label="举报编号" prop="sourceReportId" width="100" align="center" />
      <el-table-column label="违规类型" prop="violationType" width="140" align="center" />
      <el-table-column label="严重程度" prop="severity" width="110" align="center" />
      <el-table-column label="状态" prop="status" width="100" align="center" />
      <el-table-column label="说明" prop="description" min-width="260" show-overflow-tooltip />
      <el-table-column label="创建时间" prop="createTime" width="180" align="center"><template #default="scope">{{ parseTime(scope.row.createTime) }}</template></el-table-column>
      <el-table-column label="操作" width="100" align="center"><template #default="scope"><el-button v-if="scope.row.status === 'OPEN'" link type="primary" icon="Edit" v-hasPermi="['lostfound:case:handle']" @click="openResolve(scope.row)">结案</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    <el-dialog v-model="dialogOpen" title="处理违规案件" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="结案状态" prop="status"><el-radio-group v-model="form.status"><el-radio value="RESOLVED">已解决</el-radio><el-radio value="DISMISSED">已驳回</el-radio></el-radio-group></el-form-item>
        <el-form-item label="处理结果" prop="description"><el-input v-model="form.description" type="textarea" maxlength="2000" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="submitResolve">确定</el-button><el-button @click="dialogOpen = false">取消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="LostFoundCase">
import { listViolationCase, resolveViolationCase } from '@/api/lostfound/moderation'
const { proxy } = getCurrentInstance()
const caseList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dialogOpen = ref(false)
const form = ref({})
const queryParams = reactive({ pageNum: 1, pageSize: 10, status: undefined, severity: undefined })
const rules = { status: [{ required: true, message: '结案状态不能为空', trigger: 'change' }], description: [{ required: true, message: '处理结果不能为空', trigger: 'blur' }] }
function getList() { loading.value = true; listViolationCase(queryParams).then(response => { caseList.value = response.rows; total.value = response.total; loading.value = false }) }
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function openResolve(row) { form.value = { caseId: row.caseId, status: 'RESOLVED', description: undefined }; dialogOpen.value = true }
function submitResolve() { proxy.$refs.formRef.validate(valid => { if (!valid) return; resolveViolationCase(form.value.caseId, form.value).then(() => { proxy.$modal.msgSuccess('案件已处理'); dialogOpen.value = false; getList() }) }) }
getList()
</script>
