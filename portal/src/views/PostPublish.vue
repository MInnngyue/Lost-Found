<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { publishPost } from '@/api/post'
import { ElMessage } from 'element-plus'
import { ImagePlus, X, RefreshCw } from 'lucide-vue-next'
import { useAuroraGlow } from '@/composables/useAuroraGlow'
import { usePostCategories } from '@/composables/usePostCategories'
import { postRules as rules, disabledEventDate } from '@/utils/postForm'

const router = useRouter()
const { glowX, glowY, glowActive, onMouseMove, onMouseLeave } = useAuroraGlow()
const formRef = ref(null)
const submitting = ref(false)
const form = ref({
  type: 0,
  itemCategory: '',
  color: '',
  locationCampus: '',
  locationArea: '',
  locationDetail: '',
  lostTime: '',
  title: '',
  description: '',
})
const { itemCategories, colors, campuses, areas, details, loading: categoriesLoading,
  error: categoriesError, ready: categoriesReady, loadCategories, onCampusChange,
  onAreaChange } = usePostCategories(form)
const imageFiles = ref([])
let revealObserver = null

function initRevealObserver() {
  if (!('IntersectionObserver' in window)) return
  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return
        entry.target.classList.add('is-visible')
        revealObserver?.unobserve(entry.target)
      })
    },
    { threshold: 0.12 },
  )
}

onMounted(async () => {
  initRevealObserver()
  await nextTick()
  document.querySelectorAll('.publish-page [data-reveal]').forEach((el) => {
    if (revealObserver) revealObserver.observe(el)
    else el.classList.add('is-visible')
  })
  await loadCategories()
})

onBeforeUnmount(() => revealObserver?.disconnect())

function handleFileChange(file) {
  if (!file.raw) return
  if (imageFiles.value.length >= 9) return ElMessage.warning('最多上传9张图片')
  if (!['image/jpeg', 'image/png', 'image/gif', 'image/bmp'].includes(file.raw.type)) {
    return ElMessage.warning('请选择 JPG、PNG、GIF 或 BMP 图片')
  }
  if (file.raw.size > 5 * 1024 * 1024) return ElMessage.warning('单张图片不能超过5MB')
  imageFiles.value.push(file.raw)
}

function removeFile(index) {
  imageFiles.value.splice(index, 1)
}

async function onSubmit() {
  if (submitting.value || !categoriesReady.value) return
  submitting.value = true
  try {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
    const payload = { ...form.value, title: form.value.title.trim() }
    await publishPost({ ...payload, images: [...imageFiles.value] })
    ElMessage.success('已提交审核，请等待管理员处理')
    router.push('/')
  } catch (error) {
    if (error.saved) {
      ElMessage.warning(error.message)
      router.push('/my-posts')
    } else {
      ElMessage.error(error.message || '发布失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page publish-page" @mousemove="onMouseMove" @mouseleave="onMouseLeave">
    <div
      class="aurora-glow"
      :class="{ active: glowActive }"
      :style="{ transform: `translate3d(${glowX - 150}px, ${glowY - 100}px, 0)` }"
      aria-hidden="true"
    />
    <div
      class="aurora-glow aurora-glow--teal"
      :class="{ active: glowActive }"
      :style="{ transform: `translate3d(${glowX - 130}px, ${glowY - 160}px, 0)` }"
      aria-hidden="true"
    />
    <header class="page-header" data-reveal>
      <h1 class="page-title">发布帖子</h1>
      <p class="page-subtitle">填写物品信息，帮助找回或归还</p>
    </header>

    <div class="category-status" role="status" aria-live="polite">
      <p v-if="categoriesLoading">正在加载分类...</p>
      <template v-else-if="categoriesError">
        <el-alert :title="categoriesError" type="warning" :closable="false" show-icon />
        <button type="button" class="btn-secondary retry-button" @click="loadCategories">
          <RefreshCw :size="16" aria-hidden="true" />重试
        </button>
      </template>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" :disabled="submitting" class="publish-form" @submit.prevent="onSubmit">
      <section class="form-section" data-reveal style="--reveal-delay: 60ms">
        <h2 class="section-title">
          <span class="section-num">01</span>
          物品信息
        </h2>
        <div class="section-body">
          <div class="field-group">
            <label class="field-label">类型 <span class="required">*</span></label>
            <el-form-item prop="type">
              <div class="type-toggle">
                <button type="button" class="type-btn" :disabled="submitting" :class="{ active: form.type === 0 }" @click="form.type = 0">
                  寻物启事
                </button>
                <button type="button" class="type-btn" :disabled="submitting" :class="{ active: form.type === 1 }" @click="form.type = 1">
                  失物招领
                </button>
              </div>
            </el-form-item>
          </div>

          <div class="field-row">
            <div class="field-group">
              <label class="field-label">物品大类 <span class="required">*</span></label>
              <el-form-item prop="itemCategory">
                <el-select v-model="form.itemCategory" aria-label="物品分类" placeholder="选择类别" size="large" style="width: 100%" :disabled="!categoriesReady">
                  <el-option v-for="c in itemCategories" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
              </el-form-item>
            </div>
            <div class="field-group">
              <label class="field-label">颜色 <span class="required">*</span></label>
              <el-form-item prop="color">
                <el-select v-model="form.color" aria-label="颜色" placeholder="选择颜色" size="large" style="width: 100%" :disabled="!categoriesReady">
                  <el-option v-for="c in colors" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
              </el-form-item>
            </div>
          </div>
        </div>
      </section>

      <section class="form-section" data-reveal style="--reveal-delay: 120ms">
        <h2 class="section-title">
          <span class="section-num">02</span>
          时间地点
        </h2>
        <div class="section-body">
          <div class="field-group">
            <label class="field-label">地点 <span class="required">*</span></label>
            <div class="location-row">
              <el-form-item prop="locationCampus">
                <el-select
                  v-model="form.locationCampus"
                  aria-label="校区"
                  placeholder="校区"
                  size="large"
                  style="width: 100%"
                  :disabled="!categoriesReady"
                  @change="onCampusChange"
                >
                  <el-option v-for="c in campuses" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
              </el-form-item>
              <el-select
                v-model="form.locationArea"
                aria-label="区域"
                placeholder="区域（选填）"
                size="large"
                style="width: 100%"
                :disabled="!categoriesReady || !form.locationCampus || !areas.length"
                @change="onAreaChange"
              >
                <el-option v-for="c in areas" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
              <template v-if="details.length > 0">
                <el-select v-model="form.locationDetail" aria-label="具体地点" placeholder="具体地点（选填）" size="large" style="width: 100%">
                  <el-option v-for="c in details" :key="c.id" :label="c.name" :value="c.name" />
                </el-select>
              </template>
              <template v-else>
                <el-input
                  v-model="form.locationDetail"
                  aria-label="具体地点"
                  placeholder="具体地点（选填）"
                  maxlength="255"
                  size="large"
                  :disabled="!categoriesReady || !form.locationCampus"
                />
              </template>
            </div>
          </div>

          <div class="field-group">
            <label class="field-label">时间 <span class="required">*</span></label>
            <el-form-item prop="lostTime">
              <el-date-picker
                v-model="form.lostTime"
                type="date"
                placeholder="选择遗失/捡到日期"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledEventDate"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
          </div>
        </div>
      </section>

      <section class="form-section" data-reveal style="--reveal-delay: 180ms">
        <h2 class="section-title">
          <span class="section-num">03</span>
          补充描述
        </h2>
        <div class="section-body">
          <div class="field-group">
            <label class="field-label" for="post-title">标题 <span class="required">*</span></label>
            <el-form-item prop="title">
              <el-input
                id="post-title"
                v-model="form.title"
                placeholder="如「图书馆二楼捡到黑色双肩包」"
                maxlength="128"
                show-word-limit
                size="large"
              />
            </el-form-item>
          </div>

          <div class="field-group">
            <label class="field-label">描述</label>
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              placeholder="补充物品特征、品牌、特殊标记等"
              maxlength="500"
              show-word-limit
            />
          </div>

          <div class="field-group">
            <label class="field-label">图片</label>
            <el-upload :auto-upload="false" :file-list="[]" accept="image/jpeg,image/png,image/gif,image/bmp" :on-change="handleFileChange" :disabled="submitting">
              <button type="button" class="upload-btn" :disabled="submitting"><ImagePlus :size="18" aria-hidden="true" />选择图片</button>
            </el-upload>
            <span class="upload-hint">最多9张，单张≤5MB</span>
            <div v-if="imageFiles.length > 0" class="preview-list">
              <div v-for="(file, index) in imageFiles" :key="index" class="preview-item">
                <span>{{ file.name }}</span>
                <button type="button" class="remove-btn" title="移除图片" aria-label="移除图片" :disabled="submitting" @click="removeFile(index)"><X :size="16" /></button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div class="submit-area" data-reveal style="--reveal-delay: 240ms">
        <button type="submit" class="btn-primary" :disabled="submitting || !categoriesReady">
          {{ submitting ? '发布中...' : '提交审核' }}
        </button>
        <button type="button" class="btn-secondary" @click="router.push('/')">取消</button>
      </div>
    </el-form>
  </div>
</template>

<style scoped>
.publish-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 31px 20px 48px;
  min-height: calc(100vh - 60px);
  position: relative;
  isolation: isolate;
}

/* 极光 Aurora 双光团 */
.aurora-glow {
  position: fixed;
  width: 300px;
  height: 300px;
  left: 0;
  top: 0;
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
  will-change: transform;
  opacity: 0;
  transition: opacity 0.5s ease;
  filter: blur(40px);
  background: radial-gradient(circle, rgba(139, 92, 246, 0.3) 0%, rgba(139, 92, 246, 0.1) 40%, transparent 70%);
  mix-blend-mode: normal;
}

.aurora-glow--teal {
  width: 260px;
  height: 260px;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.25) 0%, rgba(6, 182, 212, 0.08) 40%, transparent 70%);
  mix-blend-mode: screen;
}

.aurora-glow.active {
  opacity: 1;
}

.page-header,
.category-status,
.publish-form {
  position: relative;
  z-index: 1;
}

.retry-button,
.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.category-status {
  margin-bottom: 16px;
}

.category-status:empty {
  display: none;
}

.retry-button {
  margin-top: 8px;
}

.page-header {
  margin-bottom: 28px;
}

.page-title {
  margin: 0;
  color: #1e293b;
  font-family: 'Noto Serif SC', serif;
  font-size: 36px;
  font-weight: 700;
  line-height: 1.35;
}

.page-subtitle {
  margin: 4px 0 0;
  color: #64748b;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
  font-size: 16px;
}

.publish-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 毛玻璃卡片 — 与广场小卡片完全一致 */
.form-section {
  background: rgba(255, 255, 255, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  padding: 24px 28px;
  overflow: hidden;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 18px;
  color: #1e293b;
  font-family: 'Noto Serif SC', serif;
  font-size: 17.6px;
  font-weight: 600;
}

.section-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  font-weight: 700;
}

.section-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  color: #334155;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
  font-size: 14.3px;
  font-weight: 600;
}

.required {
  color: #ef4444;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.location-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: start;
  gap: 12px;
}

/* 类型切换胶囊按钮组 */
.type-toggle {
  display: flex;
  gap: 8px;
}

.type-btn {
  padding: 10px 24px;
  border-radius: 999px;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
  font-size: 15.4px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  background: rgba(255, 255, 255, 0.6);
  border: 1.5px solid rgba(15, 23, 42, 0.12);
  color: #64748b;
}

.type-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.type-btn.active {
  background: rgba(15, 23, 42, 0.92);
  border-color: rgba(15, 23, 42, 0.92);
  color: #ffffff;
}

/* 上传按钮 */
.upload-btn {
  padding: 10px 20px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.6);
  border: 1.5px solid rgba(15, 23, 42, 0.12);
  color: #334155;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
  font-size: 14.3px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.upload-hint {
  color: #94a3b8;
  font-size: 13.2px;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
}

.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.6);
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13.2px;
  color: #334155;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.remove-btn {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  line-height: 1;
}

.remove-btn:hover {
  color: #ef4444;
}

/* 提交区 */
.submit-area {
  display: flex;
  gap: 12px;
  padding: 0 4px;
}

.btn-primary {
  flex: 1;
  padding: 14px 28px;
  background: rgba(15, 23, 42, 0.92);
  color: #ffffff;
  border: none;
  border-radius: 999px;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover:not(:disabled) {
  background: #000000;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.24);
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 14px 28px;
  background: transparent;
  border: 1.5px solid rgba(15, 23, 42, 0.2);
  border-radius: 999px;
  color: #334155;
  font-family: 'HarmonyOS Sans', 'PingFang SC', sans-serif;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  border-color: rgba(15, 23, 42, 0.4);
  background: rgba(15, 23, 42, 0.04);
}

/* data-reveal 渐入动画 — 与广场一致 */
[data-reveal] {
  opacity: 0;
  transform: translate3d(0, 30px, 0) scale(0.985);
  transition:
    opacity 760ms cubic-bezier(0.16, 1, 0.3, 1),
    transform 760ms cubic-bezier(0.16, 1, 0.3, 1);
  transition-delay: var(--reveal-delay, 0ms);
}

[data-reveal].is-visible {
  opacity: 1;
  transform: translate3d(0, 0, 0) scale(1);
}

@media (prefers-reduced-motion: reduce) {
  [data-reveal] {
    opacity: 1;
    transform: none;
    transition: none;
  }
}

/* el-input / el-select 样式覆盖 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  background: rgba(255, 255, 255, 0.6) !important;
  border: 1px solid rgba(15, 23, 42, 0.1) !important;
  border-radius: 12px !important;
  box-shadow: none !important;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover) {
  border-color: #3b82f6 !important;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focus) {
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
}

:deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.6) !important;
  border: 1px solid rgba(15, 23, 42, 0.1) !important;
  border-radius: 12px !important;
  box-shadow: none !important;
}

:deep(.el-textarea__inner:focus) {
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .publish-page {
    padding: 24px 16px 36px;
  }
  .page-title {
    font-size: 29px;
  }
  .field-row {
    grid-template-columns: 1fr;
  }
  .location-row {
    grid-template-columns: 1fr;
  }
  .form-section {
    padding: 18px 20px;
  }
}
</style>
