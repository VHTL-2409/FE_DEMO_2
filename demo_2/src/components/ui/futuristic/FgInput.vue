<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  label: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'text'
  },
  error: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  floatLabel: {
    type: Boolean,
    default: false
  },
  icon: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'focus', 'blur', 'enter'])

const isFocused = ref(false)
const inputId = ref(`fg-input-${Math.random().toString(36).substr(2, 9)}`)

const inputClasses = computed(() => [
  'fg-input',
  {
    'fg-input--error': !!props.error,
    'fg-input--float': props.floatLabel,
    'fg-input--with-icon': props.icon
  }
])

const handleInput = (e) => {
  emit('update:modelValue', e.target.value)
}

const handleFocus = (e) => {
  isFocused.value = true
  emit('focus', e)
}

const handleBlur = (e) => {
  isFocused.value = false
  emit('blur', e)
}

const handleKeydown = (e) => {
  if (e.key === 'Enter') {
    emit('enter', e)
  }
}
</script>

<template>
  <div class="fg-input-wrapper">
    <!-- Label -->
    <label 
      v-if="label && !floatLabel" 
      :for="inputId"
      class="fg-input-label"
    >
      {{ label }}
    </label>
    
    <!-- Input container -->
    <div class="fg-input-container" :class="{ 'fg-input-container--focused': isFocused }">
      <!-- Icon slot -->
      <span v-if="icon && $slots.icon" class="fg-input-icon">
        <slot name="icon" />
      </span>
      
      <!-- Input -->
      <input
        :id="inputId"
        :type="type"
        :value="modelValue"
        :placeholder="floatLabel ? placeholder : ''"
        :disabled="disabled"
        :class="inputClasses"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown="handleKeydown"
      />
      
      <!-- Float label -->
      <label 
        v-if="floatLabel" 
        :for="inputId"
        class="fg-float-label"
      >
        {{ placeholder || label }}
      </label>
      
      <!-- Focus glow -->
      <span class="fg-input-glow" />
    </div>
    
    <!-- Error message -->
    <Transition name="fg-error">
      <span v-if="error" class="fg-input-error">
        <svg class="fg-error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        {{ error }}
      </span>
    </Transition>
  </div>
</template>

<style scoped>
.fg-input-wrapper {
  position: relative;
  margin-bottom: 20px;
}

.fg-input-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.7);
}

.fg-input-container {
  position: relative;
}

.fg-input {
  width: 100%;
  padding: 14px 18px;
  font-family: 'Manrope', sans-serif;
  font-size: 14px;
  color: #ffffff;
  background: rgba(10, 10, 20, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--fg-radius-md);
  outline: none;
  transition: all var(--fg-duration-normal) var(--fg-ease-out);
}

.fg-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.fg-input:hover:not(:disabled) {
  border-color: rgba(0, 245, 255, 0.3);
}

.fg-input:focus {
  border-color: var(--fg-neon-cyan);
  box-shadow: 0 0 0 3px rgba(0, 245, 255, 0.15), 0 0 20px rgba(0, 245, 255, 0.1);
}

.fg-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.fg-input--error {
  border-color: var(--fg-neon-red);
}

.fg-input--error:focus {
  box-shadow: 0 0 0 3px rgba(255, 51, 102, 0.15), 0 0 20px rgba(255, 51, 102, 0.1);
}

/* Float label input */
.fg-input--float {
  padding-top: 22px;
  padding-bottom: 8px;
}

.fg-input-container:focus-within .fg-float-label,
.fg-input:not(:placeholder-shown) + .fg-float-label {
  top: 6px;
  font-size: 11px;
  color: var(--fg-neon-cyan);
}

.fg-float-label {
  position: absolute;
  top: 50%;
  left: 18px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  transform: translateY(-50%);
  transition: all var(--fg-duration-fast) var(--fg-ease-out);
  pointer-events: none;
}

/* With icon */
.fg-input--with-icon {
  padding-left: 44px;
}

.fg-input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.4);
  transition: color var(--fg-duration-fast) var(--fg-ease-out);
}

.fg-input-icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.fg-input-container--focused .fg-input-icon {
  color: var(--fg-neon-cyan);
}

/* Focus glow effect */
.fg-input-glow {
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  opacity: 0;
  transition: opacity var(--fg-duration-normal) var(--fg-ease-out);
  pointer-events: none;
  box-shadow: 0 0 20px rgba(0, 245, 255, 0.3);
}

.fg-input-container--focused .fg-input-glow {
  opacity: 1;
}

/* Error message */
.fg-input-error {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--fg-neon-red);
}

.fg-error-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

/* Error transition */
.fg-error-enter-active,
.fg-error-leave-active {
  transition: all 0.2s ease;
}

.fg-error-enter-from,
.fg-error-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
