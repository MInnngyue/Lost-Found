<script setup>
import { LoaderCircle } from 'lucide-vue-next'

defineProps({
  icon: { type: [Object, Function], default: null },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
})
</script>

<template>
  <button class="auth-button" type="submit" :disabled="loading || disabled" :aria-busy="loading">
    <LoaderCircle v-if="loading" class="auth-button__spinner" :size="20" aria-hidden="true" />
    <component :is="icon" v-else-if="icon" :size="19" :stroke-width="2" aria-hidden="true" />
    <span><slot /></span>
  </button>
</template>

<style scoped>
.auth-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 48px;
  gap: 8px;
  padding: 0 20px;
  color: #fff;
  font: inherit;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0;
  cursor: pointer;
  background: linear-gradient(135deg, var(--primary-500), var(--primary-600));
  border: 0;
  border-radius: 12px;
  box-shadow: 0 2px 5px rgba(37, 99, 235, 0.18);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease;
}

.auth-button:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.3);
  transform: translateY(-2px);
}

.auth-button:active:not(:disabled) {
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
  transform: translateY(0);
}

.auth-button:focus-visible {
  outline: 2px solid var(--primary-500);
  outline-offset: 3px;
}

.auth-button:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.auth-button__spinner {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .auth-button {
    height: 44px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-button {
    transition: none;
  }

  .auth-button__spinner {
    animation-duration: 1.6s;
  }
}
</style>
