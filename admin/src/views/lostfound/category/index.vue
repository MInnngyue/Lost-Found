<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryRef" :model="queryParams" :inline="true">
      <el-form-item label="分类类型" prop="categoryType">
        <el-select v-model="queryParams.categoryType" clearable placeholder="请选择分类类型" style="width: 180px">
          <el-option label="物品分类" value="ITEM" />
          <el-option label="颜色分类" value="COLOR" />
          <el-option label="校区" value="CAMPUS" />
          <el-option label="区域" value="AREA" />
          <el-option label="地点" value="LOCATION" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类名称" prop="categoryName">
        <el-input v-model="queryParams.categoryName" clearable placeholder="请输入分类名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" v-hasPermi="['lostfound:category:add']" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" v-hasPermi="['lostfound:category:edit']" @click="handleUpdate()">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" v-hasPermi="['lostfound:category:remove']" @click="handleDelete()">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="categoryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" prop="categoryId" align="center" width="90" />
      <el-table-column label="类型" prop="categoryType" align="center" width="120" />
      <el-table-column label="编码" prop="categoryCode" align="center" />
      <el-table-column label="名称" prop="categoryName" align="center" />
      <el-table-column label="父级编号" prop="parentId" align="center" width="100" />
      <el-table-column label="排序" prop="sortOrder" align="center" width="90" />
      <el-table-column label="状态" prop="status" align="center" width="100">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" align="center" width="180">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" v-hasPermi="['lostfound:category:edit']" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="primary" icon="Delete" v-hasPermi="['lostfound:category:remove']" @click="handleDelete(scope.row)">删除</el-button>
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

    <el-dialog v-model="open" :title="title" width="520px" append-to-body>
      <el-form ref="categoryRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="分类类型" prop="categoryType">
          <el-select v-model="form.categoryType" placeholder="请选择分类类型" style="width: 100%">
            <el-option label="物品分类" value="ITEM" />
            <el-option label="颜色分类" value="COLOR" />
            <el-option label="校区" value="CAMPUS" />
            <el-option label="区域" value="AREA" />
            <el-option label="地点" value="LOCATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级编号" prop="parentId">
          <el-input-number v-model="form.parentId" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="form.categoryCode" maxlength="64" placeholder="请输入稳定的业务编码" />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" maxlength="128" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="显示顺序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="cancel">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="LostFoundCategory">
import { addCategory, delCategory, getCategory, listCategory, updateCategory } from '@/api/lostfound/category'

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = useDict('sys_normal_disable')
const categoryList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    categoryType: undefined,
    categoryName: undefined
  },
  rules: {
    categoryType: [{ required: true, message: '分类类型不能为空', trigger: 'change' }],
    parentId: [{ required: true, message: '父级编号不能为空', trigger: 'blur' }],
    categoryCode: [{ required: true, message: '分类编码不能为空', trigger: 'blur' }],
    categoryName: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }],
    sortOrder: [{ required: true, message: '显示顺序不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listCategory(queryParams.value).then(response => {
    categoryList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function reset() {
  form.value = {
    categoryId: undefined,
    categoryType: undefined,
    parentId: 0,
    categoryCode: undefined,
    categoryName: undefined,
    sortOrder: 0,
    status: '0'
  }
  proxy.resetForm('categoryRef')
}

function cancel() {
  open.value = false
  reset()
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.categoryId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增分类'
}

function handleUpdate(row) {
  reset()
  const categoryId = row?.categoryId || ids.value[0]
  getCategory(categoryId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改分类'
  })
}

function handleDelete(row) {
  const categoryIds = row?.categoryId || ids.value
  proxy.$modal.confirm('是否确认删除选中的分类？').then(() => delCategory(categoryIds)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  })
}

function submitForm() {
  proxy.$refs.categoryRef.validate(valid => {
    if (!valid) return
    const action = form.value.categoryId ? updateCategory : addCategory
    action(form.value).then(() => {
      proxy.$modal.msgSuccess(form.value.categoryId ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

getList()
</script>
