<script setup>
import { useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
const router = useRouter()
function goHome() { router.push('/') }
</script>

<template>
  <div class="notfound-page">
    <div class="nf-content">
      <div class="nf-code">404</div>
      <h2 class="nf-title">页面不存在</h2>
      <p class="nf-desc">地址可能输错了，或者页面已经被移走了。</p>
      <button class="nf-btn" type="button" @click="goHome">
        <ArrowLeft :size="14" aria-hidden="true" />
        <span>返回广场</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.notfound-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 40px 20px;
  position: relative;
  z-index: 1;
}

.nf-content {
  text-align: center;
  max-width: 480px;
  padding: 56px 48px;
  background: rgba(255, 255, 255, 0.48);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  box-shadow: 0 18px 54px rgba(31, 38, 135, 0.12);
  backdrop-filter: blur(18px) saturate(150%);
  -webkit-backdrop-filter: blur(18px) saturate(150%);
}

.nf-code {
  font-family: var(--mono-font);
  font-size: 120px;
  font-weight: 700;
  color: var(--text-soft);
  line-height: 1;
  letter-spacing: -4px;
  margin-bottom: 16px;
}

.nf-title {
  font-family: 'Noto Serif SC', Georgia, serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-strong);
  margin: 0 0 10px;
  letter-spacing: -0.3px;
}

.nf-desc {
  font-size: 15px;
  color: var(--text-muted);
  margin: 0 0 32px;
  line-height: 1.6;
}

.nf-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 40px;
  padding: 9px 18px;
  border: 0;
  border-radius: 999px;
  background: var(--ink);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  margin: 0 auto;
}
.nf-btn:hover { background: var(--ink-hover); }
.nf-btn:focus-visible { outline: 3px solid var(--focus-ring); outline-offset: 3px; }
</style>
