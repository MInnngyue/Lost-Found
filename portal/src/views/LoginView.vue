<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import DotBackground from '@/components/auth/DotBackground.vue'
import LoginCard from '@/components/auth/LoginCard.vue'
import { useAuthForm } from '@/composables/useAuthForm'

const router = useRouter()
const { alert, captcha, errors, form, isRegister, loading, refreshCaptcha, submit, switchMode, updateField } = useAuthForm()

onMounted(() => refreshCaptcha())

async function handleSubmit() {
  const result = await submit()
  if (result?.type === 'authenticated') {
    await router.replace('/')
  }
}
</script>

<template>
  <div class="login-page">
    <DotBackground />

    <main class="login-page__content">
      <LoginCard
        :alert="alert"
        :captcha="captcha"
        :errors="errors"
        :form="form"
        :is-register="isRegister"
        :loading="loading"
        @submit="handleSubmit"
        @switch-mode="switchMode"
        @update-field="updateField"
        @refresh-captcha="refreshCaptcha"
      />
    </main>

    <footer class="login-page__footer"></footer>
  </div>
</template>

<style scoped>
.login-page {
  --primary-50: #eff6ff;
  --primary-100: #dbeafe;
  --primary-200: #bfdbfe;
  --primary-300: #93c5fd;
  --primary-400: #60a5fa;
  --primary-500: #3b82f6;
  --primary-600: #2563eb;
  --primary-700: #1d4ed8;
  --primary-800: #1e40af;
  --gray-50: #f8fafc;
  --gray-200: #e2e8f0;
  --gray-400: #94a3b8;
  --gray-500: #64748b;
  --gray-700: #334155;
  --gray-800: #1e293b;
  --danger: #ef4444;
  --font-heading: 'Noto Serif SC', 'Source Han Serif SC', SimSun, serif;
  --font-body: 'HarmonyOS Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;

  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  overflow-x: hidden;
  color: var(--gray-800);
  font-family: var(--font-body);
  background: linear-gradient(135deg, #f8fafc, #eff6ff);
  isolation: isolate;
}

.login-page__content {
  position: relative;
  z-index: 1;
  display: grid;
  flex: 1;
  width: 100%;
  padding: 40px 16px 24px;
  place-items: center;
}

.login-page__footer {
  position: relative;
  z-index: 1;
  padding: 0 16px 24px;
  color: var(--gray-500);
  font-size: 13px;
  line-height: 1.5;
  text-align: center;
  letter-spacing: 0;
}

@media (max-width: 640px) {
  .login-page__content {
    align-items: start;
    padding-top: 24px;
  }

  .login-page__footer {
    padding-bottom: 18px;
    font-size: 12px;
  }
}
</style>