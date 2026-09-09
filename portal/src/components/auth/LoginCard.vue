<script setup>
import { ref, watch } from 'vue'
import { Backpack, LockKeyhole, LogIn, RotateCw, User, UserPlus } from 'lucide-vue-next'
import AuthButton from './AuthButton.vue'
import AuthInput from './AuthInput.vue'
import StatusAlert from './StatusAlert.vue'

const props = defineProps({
  alert: { type: Object, default: null },
  captcha: { type: Object, required: true },
  errors: { type: Object, required: true },
  form: { type: Object, required: true },
  isRegister: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['submit', 'switch-mode', 'update-field', 'refresh-captcha'])
const passwordVisible = ref(false)
const confirmPasswordVisible = ref(false)

watch(
  () => props.isRegister,
  () => {
    passwordVisible.value = false
    confirmPasswordVisible.value = false
  },
)

function updateField(field, value) {
  emit('update-field', field, value)
}
</script>

<template>
  <section class="login-card" aria-labelledby="login-card-title">
    <header class="login-card__header">
      <div class="login-card__logo" aria-hidden="true">
        <Backpack :size="31" :stroke-width="1.8" />
      </div>
      <h1 id="login-card-title" class="login-card__title">校园失物招领平台</h1>
      <p class="login-card__subtitle">
        {{ isRegister ? '创建账号，加入校园互助社区' : '欢迎回来，继续寻找与归还' }}
      </p>
    </header>

    <StatusAlert v-if="alert" :type="alert.type" :message="alert.message" />
    <StatusAlert v-if="captcha.error" type="error" :message="captcha.error" />

    <form class="login-card__form" novalidate @submit.prevent="emit('submit')">
      <AuthInput
        id="username"
        :model-value="form.username"
        :error="errors.username"
        :icon="User"
        label="用户名"
        placeholder="请输入用户名"
        autocomplete="username"
        @update:model-value="updateField('username', $event)"
      />

      <AuthInput
        id="password"
        v-model:visible="passwordVisible"
        :model-value="form.password"
        :error="errors.password"
        :icon="LockKeyhole"
        label="密码"
        placeholder="请输入密码"
        type="password"
        :autocomplete="isRegister ? 'new-password' : 'current-password'"
        revealable
        @update:model-value="updateField('password', $event)"
      />

      <div v-if="captcha.enabled" class="captcha-row">
        <AuthInput
          id="captcha-code"
          :model-value="form.code"
          :error="errors.code"
          :icon="RotateCw"
          label="验证码"
          placeholder="验证码"
          autocomplete="off"
          @update:model-value="updateField('code', $event)"
        />
        <button
          type="button"
          class="captcha-image-button"
          title="刷新验证码"
          aria-label="刷新验证码"
          :disabled="captcha.loading || loading"
          @click="emit('refresh-captcha')"
        >
          <img v-if="captcha.image" :src="captcha.image" alt="验证码" />
          <RotateCw v-else :size="20" aria-hidden="true" />
        </button>
      </div>

      <AuthInput
        v-if="isRegister"
        id="confirm-password"
        v-model:visible="confirmPasswordVisible"
        :model-value="form.confirmPassword"
        :error="errors.confirmPassword"
        :icon="LockKeyhole"
        label="确认密码"
        placeholder="请再次输入密码"
        type="password"
        autocomplete="new-password"
        revealable
        @update:model-value="updateField('confirmPassword', $event)"
      />

      <AuthButton :loading="loading" :disabled="!captcha.ready || captcha.loading" :icon="isRegister ? UserPlus : LogIn">
        {{ loading ? (isRegister ? '正在注册' : '正在登录') : isRegister ? '注册' : '登录' }}
      </AuthButton>
    </form>

    <p class="login-card__switch">
      <span>{{ isRegister ? '已有账号？' : '还没有账号？' }}</span>
      <button type="button" :disabled="loading" @click="emit('switch-mode')">
        {{ isRegister ? '返回登录' : '立即注册' }}
      </button>
    </p>
  </section>
</template>

<style scoped>
.login-card {
  width: min(100%, 420px);
  padding: 44px 40px 34px;
  color: var(--gray-800);
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.86);
  border-radius: 16px;
  box-shadow:
    0 18px 45px rgba(30, 64, 175, 0.1),
    0 4px 10px rgba(15, 23, 42, 0.06),
    0 0 0 1px rgba(148, 163, 184, 0.08);
  backdrop-filter: blur(16px);
  animation: card-in 0.6s ease-out both;
}

.login-card__header {
  margin-bottom: 28px;
  text-align: center;
}

.login-card__logo {
  display: grid;
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  color: #fff;
  background: linear-gradient(135deg, var(--primary-500), var(--primary-600));
  border-radius: 50%;
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.24);
  place-items: center;
  animation: logo-pulse 2.4s ease-in-out infinite;
}

.login-card__title {
  margin: 0 0 8px;
  color: var(--gray-800);
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: 0;
}

.login-card__subtitle {
  margin: 0;
  color: var(--gray-500);
  font-size: 16px;
  line-height: 1.5;
}

.status-alert {
  margin-bottom: 20px;
}

.login-card__form {
  display: grid;
  gap: 18px;
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 116px;
  gap: 10px;
  align-items: start;
}

.captcha-image-button {
  display: grid;
  width: 116px;
  height: 48px;
  margin-top: 30px;
  padding: 0;
  color: var(--gray-500);
  cursor: pointer;
  background: var(--gray-50);
  border: 1px solid var(--gray-200);
  border-radius: 8px;
  place-items: center;
  overflow: hidden;
}

.captcha-image-button:hover {
  border-color: var(--primary-300);
}

.captcha-image-button img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.login-card__form :deep(.auth-button) {
  margin-top: 6px;
  font-size: 18px;
}

.login-card__form :deep(.auth-field__label) {
  font-size: 16px;
}

.login-card__switch {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin: 22px 0 0;
  color: var(--gray-500);
  font-size: 16px;
  line-height: 1.5;
}

.login-card__switch button {
  padding: 3px 2px;
  color: var(--primary-600);
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 4px;
  transition: color 0.2s ease;
}

.login-card__switch button:hover {
  color: var(--primary-700);
  text-decoration: underline;
}

.login-card__switch button:focus-visible {
  outline: 2px solid var(--primary-500);
  outline-offset: 2px;
}

@keyframes card-in {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes logo-pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

@media (max-width: 640px) {
  .login-card {
    padding: 32px 24px 26px;
  }

  .login-card__header {
    margin-bottom: 24px;
  }

  .login-card__logo {
    width: 56px;
    height: 56px;
    margin-bottom: 14px;
  }

  .login-card__title {
    font-size: 28px;
  }

  .login-card__form {
    gap: 16px;
  }
}

@media (min-width: 641px) and (max-width: 1024px) {
  .login-card {
    max-width: 380px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card,
  .login-card__logo {
    animation: none;
  }
}
</style>
