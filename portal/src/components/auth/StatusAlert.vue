<script setup>
import { CircleCheck, TriangleAlert } from 'lucide-vue-next'

defineProps({
  message: { type: String, required: true },
  type: { type: String, default: 'error' },
})
</script>

<template>
  <div
    class="status-alert"
    :class="`status-alert--${type}`"
    :role="type === 'error' ? 'alert' : 'status'"
    aria-live="polite"
  >
    <CircleCheck v-if="type === 'success'" :size="18" aria-hidden="true" />
    <TriangleAlert v-else :size="18" aria-hidden="true" />
    <span>{{ message }}</span>
  </div>
</template>

<style scoped>
.status-alert {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 11px 13px;
  font-size: 14px;
  line-height: 1.45;
  border: 1px solid;
  border-radius: 12px;
  animation: alert-in 0.3s ease-out;
}

.status-alert svg {
  flex: 0 0 auto;
  margin-top: 1px;
}

.status-alert--error {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.status-alert--success {
  color: #047857;
  background: #f0fdf4;
  border-color: #bbf7d0;
}

@keyframes alert-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .status-alert {
    animation: none;
  }
}
</style>
