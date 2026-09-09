<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { submitReport } from '@/api/moderation'
import { getErrorMessage } from '@/utils/error'
import '@/styles/account-pages.css'

const props = defineProps({ modelValue: { type: Object, default: null } })
const emit = defineEmits(['update:modelValue'])
const description = ref('')
const submitting = ref(false)
const errorMessage = ref('')
const title = computed(() => ({
  POST: '举报帖子', PRIVATE_MESSAGE: '举报私信', CLAIM: '举报认领过程',
})[props.modelValue?.targetType] || '提交举报')

watch(() => props.modelValue, () => {
  description.value = ''
  errorMessage.value = ''
})

function close() {
  if (!submitting.value) emit('update:modelValue', null)
}

async function submit() {
  if (!props.modelValue || submitting.value) return
  errorMessage.value = ''
  const content = description.value.trim()
  if (!content || content.length > 2000) {
    errorMessage.value = !content ? '请填写举报说明' : '举报说明不能超过2000个字符'
    return
  }
  submitting.value = true
  try {
    await submitReport({
      targetType: props.modelValue.targetType,
      targetId: props.modelValue.targetId,
      reasonCode: props.modelValue.reasonCode,
      description: content,
    })
    emit('update:modelValue', null)
    ElMessage.success('举报已提交')
  } catch (error) {
    errorMessage.value = getErrorMessage(error, '举报提交失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog :model-value="Boolean(modelValue)" :title="title" width="520px" class="portal-form-dialog"
    :close-on-click-modal="false" :close-on-press-escape="!submitting" :show-close="!submitting" @close="close">
    <div v-if="modelValue?.summary" class="dialog-context">
      <span class="dialog-context-label">举报对象</span>
      {{ modelValue.summary }}
    </div>
    <form id="report-form" @submit.prevent="submit">
      <label for="report-description" class="dialog-field-label"><span class="required-mark" aria-hidden="true">*</span>举报说明</label>
      <el-input id="report-description" v-model="description" type="textarea" :rows="5" maxlength="2000"
        show-word-limit :disabled="submitting" aria-required="true" :aria-invalid="Boolean(errorMessage)"
        aria-describedby="report-error" />
      <p v-if="errorMessage" id="report-error" class="dialog-error" role="alert">{{ errorMessage }}</p>
    </form>
    <template #footer>
      <el-button :disabled="submitting" @click="close">取消</el-button>
      <el-button type="primary" native-type="submit" form="report-form" :loading="submitting">提交举报</el-button>
    </template>
  </el-dialog>
</template>
