<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryRef" :model="queryParams" :inline="true">
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择" style="width: 150px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已受理" value="ACCEPTED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标类型" prop="targetType">
        <el-select v-model="queryParams.targetType" clearable placeholder="请选择" style="width: 150px">
          <el-option label="帖子" value="POST" />
          <el-option label="评论" value="COMMENT" />
          <el-option label="用户" value="USER" />
          <el-option label="认领过程" value="CLAIM" />
          <el-option label="私信" value="PRIVATE_MESSAGE" />
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

    <el-table v-loading="loading" :data="reportList">
      <el-table-column label="编号" prop="reportId" width="90" align="center" />
      <el-table-column label="举报人" prop="reporterId" width="90" align="center" />
      <el-table-column label="目标" width="120" align="center">
        <template #default="scope">{{ targetLabel(scope.row.targetType) }} #{{ scope.row.targetId }}</template>
      </el-table-column>
      <el-table-column label="原因" prop="reasonCode" width="130" align="center" />
      <el-table-column label="说明" prop="description" min-width="250" show-overflow-tooltip />
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="scope"><el-tag :type="scope.row.status === 'PENDING' ? 'warning' : 'success'">{{ statusLabel(scope.row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="提交时间" prop="createTime" width="180" align="center">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center">
        <template #default="scope">
          <el-button v-if="scope.row.status === 'PENDING'" link type="primary" icon="Edit"
            v-hasPermi="['lostfound:report:handle']" @click="openHandle(scope.row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
      :total="total" @pagination="getList" />

    <el-dialog v-model="dialogOpen" title="处理举报" width="620px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="处理结果" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ACCEPTED">受理并建立案件</el-radio>
            <el-radio value="REJECTED">驳回举报</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.status === 'ACCEPTED'">
          <el-form-item label="违规类型" prop="violationType">
            <el-select v-model="form.violationType" style="width: 100%">
              <el-option v-for="item in reasonOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="严重程度" prop="severity">
            <el-select v-model="form.severity" style="width: 100%">
              <el-option label="低" value="LOW" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="高" value="HIGH" />
              <el-option label="严重" value="CRITICAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="处罚动作">
            <el-select v-model="form.actionType" clearable style="width: 100%">
              <el-option label="禁止发布" value="POST_PUBLISH" />
              <el-option label="禁止评论" value="COMMENT" />
              <el-option label="禁止认领" value="CLAIM_APPLY" />
              <el-option label="禁止私信" value="PRIVATE_MESSAGE" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.actionType" label="结束时间">
            <el-date-picker v-model="form.actionEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="不填表示长期有效" style="width: 100%" />
          </el-form-item>
          <el-form-item v-if="form.actionType" label="处罚理由" prop="actionReason">
            <el-input v-model="form.actionReason" type="textarea" maxlength="2000" show-word-limit />
          </el-form-item>
        </template>
        <el-form-item label="处理说明" prop="description">
          <el-input v-model="form.description" type="textarea" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitHandle">确定</el-button>
        <el-button @click="dialogOpen = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="LostFoundReport">
import { handleReport, listReport } from '@/api/lostfound/moderation'

const { proxy } = getCurrentInstance()
const reportList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dialogOpen = ref(false)
const form = ref({})
const reasonOptions = ['SPAM', 'FRAUD', 'ABUSE', 'ILLEGAL_CONTENT', 'PRIVACY', 'OTHER']
const queryParams = reactive({ pageNum: 1, pageSize: 10, status: undefined, targetType: undefined })
const rules = {
  status: [{ required: true, message: '处理结果不能为空', trigger: 'change' }],
  description: [{ required: true, message: '处理说明不能为空', trigger: 'blur' }],
  violationType: [{ required: true, message: '违规类型不能为空', trigger: 'change' }],
  severity: [{ required: true, message: '严重程度不能为空', trigger: 'change' }],
  actionReason: [{ required: true, message: '选择处罚动作后必须填写处罚理由', trigger: 'blur' }]
}

function getList() {
  loading.value = true
  listReport(queryParams).then(response => {
    reportList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function openHandle(row) {
  form.value = { reportId: row.reportId, status: 'ACCEPTED', violationType: row.reasonCode === 'OTHER' ? 'OTHER' : row.reasonCode,
    severity: 'MEDIUM', actionType: undefined, actionEndTime: undefined, actionReason: undefined, description: undefined }
  dialogOpen.value = true
}
function submitHandle() {
  proxy.$refs.formRef.validate(valid => {
    if (!valid) return
    handleReport(form.value.reportId, form.value).then(() => {
      proxy.$modal.msgSuccess('举报处理完成')
      dialogOpen.value = false
      getList()
    })
  })
}
function targetLabel(type) { return { POST: '帖子', COMMENT: '评论', USER: '用户', CLAIM: '认领过程', PRIVATE_MESSAGE: '私信' }[type] || type }
function statusLabel(status) { return { PENDING: '待处理', ACCEPTED: '已受理', REJECTED: '已驳回' }[status] || status }
getList()
</script>
