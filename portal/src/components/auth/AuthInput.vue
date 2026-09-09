<script setup>
import { computed } from 'vue'
import { Eye, EyeOff } from 'lucide-vue-next'

const props = defineProps({
  autocomplete: { type: String, default: 'off' },
  error: { type: String, default: '' },
  icon: { type: [Object, Function], required: true },
  id: { type: String, required: true },
  label: { type: String, required: true },
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  revealable: { type: Boolean, default: false },
  type: { type: String, default: 'text' },
})

const emit = defineEmits(['clear-error', 'update:modelValue'])
const passwordVisible = defineModel('visible', { type: Boolean, default: false })
const inputType = computed(() => (props.revealable && passwordVisible.value ? 'text' : props.type))
const errorId = computed(() => `${props.id}-error`)

function handleInput(event) {
  emit('update:modelValue', event.target.value)
  emit('clear-error')
}
</script>

<template>
  <div class="auth-field" :class="{ 'has-error': error }">
    <label class="auth-field__label" :for="id">{{ label }}</label>
    <div class="auth-field__control">
      <component :is="icon" class="auth-field__leading" :size="19" :stroke-width="1.8" />
      <input
        :id="id"
        class="auth-field__input"
        :type="inputType"
        :value="modelValue"
        :placeholder="placeholder"
        :autocomplete="autocomplete"
        :aria-label="label"
        :aria-invalid="Boolean(error)"
        :aria-describedby="error ? errorId : undefined"
        @input="handleInput"
      />
      <button
        v-if="revealable"
        class="auth-field__toggle"
        type="button"
        :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
        :title="passwordVisible ? '隐藏密码' : '显示密码'"
        @click="passwordVisible = !passwordVisible"
      >
        <EyeOff v-if="passwordVisible" :size="19" aria-hidden="true" />
        <Eye v-else :size="19" aria-hidden="true" />
      </button>
    </div>
    <p v-if="error" :id="errorId" class="auth-field__error">{{ error }}</p>
  </div>
</template>

<style scoped>
.auth-field {
  display: grid;
  gap: 8px;
}

.auth-field__label {
  color: var(--gray-700);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
}

.auth-field__control {
  position: relative;
  display: flex;
  align-items: center;
  height: 48px;
  color: var(--gray-400);
  background: var(--gray-50);
  border: 1px solid var(--gray-200);
  border-radius: 12px;
  transition:
    border-color 0.3s ease,
    box-shadow 0.3s ease,
    background-color 0.3s ease,
    transform 0.3s ease;
}

.auth-field__control:hover {
  border-color: var(--primary-300);
}

.auth-field__control:focus-within {
  color: var(--primary-500);
  background: #fff;
  border-color: var(--primary-500);
  box-shadow:
    0 0 0 3px rgba(59, 130, 246, 0.12),
    0 0 18px rgba(96, 165, 250, 0.14);
  transform: scale(1.01);
}

.auth-field__leading {
  position: absolute;
  left: 15px;
  pointer-events: none;
  transition: color 0.2s ease;
}

.auth-field__input {
  width: 100%;
  height: 100%;
  min-width: 0;
  padding: 0 46px 0 45px;
  color: var(--gray-800);
  font: inherit;
  font-size: 16px;
  letter-spacing: 0;
  background: transparent;
  border: 0;
  outline: 0;
}

.auth-field__input::placeholder {
  color: var(--gray-400);
}

.auth-field__input:focus-visible {
  outline: none;
}

.auth-field__toggle {
  position: absolute;
  right: 8px;
  display: grid;
  width: 34px;
  height: 34px;
  padding: 0;
  color: var(--gray-500);
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 50%;
  place-items: center;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;
}

.auth-field__toggle:hover {
  color: var(--primary-600);
  background: var(--primary-50);
}

.auth-field__toggle:focus-visible {
  outline: 2px solid var(--primary-500);
  outline-offset: 1px;
}

.auth-field__error {
  color: var(--danger);
  font-size: 12px;
  line-height: 1.4;
}

.has-error .auth-field__control {
  border-color: var(--danger);
  animation: field-shake 0.45s ease-in-out;
}

@keyframes field-shake {
  0%,
  100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-4px);
  }
  50% {
    transform: translateX(4px);
  }
  75% {
    transform: translateX(-2px);
  }
}

@media (max-width: 640px) {
  .auth-field__control {
    height: 44px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-field__control,
  .auth-field__toggle {
    transition: none;
  }

  .has-error .auth-field__control {
    animation: none;
  }
}
</style>
