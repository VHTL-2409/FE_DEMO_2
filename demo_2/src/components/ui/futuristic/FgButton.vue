<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'cyan',
    validator: (v) => ['cyan', 'magenta', 'amber', 'violet', 'green', 'red', 'ghost', 'outline'].includes(v)
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  },
  full: {
    type: Boolean,
    default: false
  },
  glow: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  icon: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const buttonClasses = computed(() => [
  'fg-btn',
  `fg-btn--${props.variant}`,
  {
    [`fg-btn--${props.size}`]: props.size !== 'md',
    'fg-btn--full': props.full,
    'fg-btn--glow': props.glow,
    'fg-btn--loading': props.loading,
    'fg-btn--icon-only': props.icon
  }
])

const handleClick = (e) => {
  if (!props.disabled && !props.loading) {
    emit('click', e)
  }
}
</script>

<template>
  <button
    :class="buttonClasses"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <!-- Loading spinner -->
    <span v-if="loading" class="fg-btn__spinner" />
    
    <!-- Icon slot -->
    <span v-if="!loading && $slots.icon" class="fg-btn__icon">
      <slot name="icon" />
    </span>
    
    <!-- Default slot -->
    <span v-if="!loading" class="fg-btn__content">
      <slot />
    </span>
    
    <!-- Hover glow effect -->
    <span class="fg-btn__glow-effect" />
  </button>
</template>

<style scoped>
.fg-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-family: 'Manrope', sans-serif;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  border: none;
  border-radius: var(--fg-radius-md);
  cursor: pointer;
  transition: all var(--fg-duration-normal) var(--fg-ease-out);
  overflow: hidden;
  text-decoration: none;
  white-space: nowrap;
}

.fg-btn__content {
  position: relative;
  z-index: 1;
}

.fg-btn__icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.fg-btn__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.fg-btn__glow-effect {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transform: translateX(-100%);
  transition: transform var(--fg-duration-slow) var(--fg-ease-out);
  pointer-events: none;
}

.fg-btn:hover .fg-btn__glow-effect {
  transform: translateX(100%);
}

.fg-btn:active:not(:disabled) {
  transform: scale(0.96);
}

.fg-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* Size variants */
.fg-btn--sm {
  padding: 8px 16px;
  font-size: 12px;
}

.fg-btn--lg {
  padding: 16px 32px;
  font-size: 16px;
}

.fg-btn--full {
  width: 100%;
}

.fg-btn--icon-only {
  padding: 12px;
}

.fg-btn--icon-only.fg-btn--sm {
  padding: 8px;
}

.fg-btn--icon-only.fg-btn--lg {
  padding: 16px;
}

/* Loading spinner */
.fg-btn__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
  animation: fg-rotate-gradient 1s linear infinite;
}

/* Ghost variant */
.fg-btn--ghost {
  background: transparent;
  border: 1px solid;
}

.fg-btn--ghost.fg-btn--cyan {
  color: var(--fg-neon-cyan);
  border-color: var(--fg-neon-cyan-dim);
}

.fg-btn--ghost.fg-btn--magenta {
  color: var(--fg-neon-magenta);
  border-color: var(--fg-neon-magenta-dim);
}

.fg-btn--ghost.fg-btn--amber {
  color: var(--fg-neon-amber);
  border-color: var(--fg-neon-amber-dim);
}

.fg-btn--ghost.fg-btn--violet {
  color: var(--fg-neon-violet);
  border-color: var(--fg-neon-violet-dim);
}

.fg-btn--ghost.fg-btn--green {
  color: var(--fg-neon-green);
  border-color: var(--fg-neon-green-dim);
}

.fg-btn--ghost.fg-btn--red {
  color: var(--fg-neon-red);
  border-color: var(--fg-neon-red-dim);
}

.fg-btn--ghost:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.05);
}

/* Outline variant */
.fg-btn--outline {
  background: transparent;
  color: #ffffff;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.fg-btn--outline:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.6);
}
</style>
