<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'cyan',
    validator: (v) => ['cyan', 'magenta', 'amber', 'violet', 'green', 'red'].includes(v)
  },
  pulse: {
    type: Boolean,
    default: false
  },
  outline: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  }
})

const badgeClasses = computed(() => [
  'fg-badge',
  `fg-badge--${props.variant}`,
  {
    'fg-badge--outline': props.outline,
    'fg-badge--sm': props.size === 'sm',
    'fg-badge--lg': props.size === 'lg'
  }
])
</script>

<template>
  <span :class="badgeClasses">
    <span v-if="pulse" class="fg-badge__dot" />
    <slot />
  </span>
</template>

<style scoped>
.fg-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-family: 'Manrope', sans-serif;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-radius: var(--fg-radius-sm);
  white-space: nowrap;
}

/* Size variants */
.fg-badge--sm {
  padding: 4px 8px;
  font-size: 10px;
}

.fg-badge--lg {
  padding: 8px 16px;
  font-size: 12px;
}

/* Color variants */
.fg-badge--cyan {
  background: rgba(0, 245, 255, 0.15);
  color: var(--fg-neon-cyan);
  border: 1px solid rgba(0, 245, 255, 0.3);
}

.fg-badge--cyan.fg-badge--outline {
  background: transparent;
  box-shadow: 0 0 10px rgba(0, 245, 255, 0.1);
}

.fg-badge--magenta {
  background: rgba(255, 0, 255, 0.15);
  color: var(--fg-neon-magenta);
  border: 1px solid rgba(255, 0, 255, 0.3);
}

.fg-badge--magenta.fg-badge--outline {
  background: transparent;
  box-shadow: 0 0 10px rgba(255, 0, 255, 0.1);
}

.fg-badge--amber {
  background: rgba(255, 184, 0, 0.15);
  color: var(--fg-neon-amber);
  border: 1px solid rgba(255, 184, 0, 0.3);
}

.fg-badge--amber.fg-badge--outline {
  background: transparent;
  box-shadow: 0 0 10px rgba(255, 184, 0, 0.1);
}

.fg-badge--violet {
  background: rgba(139, 92, 246, 0.15);
  color: var(--fg-neon-violet);
  border: 1px solid rgba(139, 92, 246, 0.3);
}

.fg-badge--violet.fg-badge--outline {
  background: transparent;
  box-shadow: 0 0 10px rgba(139, 92, 246, 0.1);
}

.fg-badge--green {
  background: rgba(0, 255, 136, 0.15);
  color: var(--fg-neon-green);
  border: 1px solid rgba(0, 255, 136, 0.3);
}

.fg-badge--green.fg-badge--outline {
  background: transparent;
  box-shadow: 0 0 10px rgba(0, 255, 136, 0.1);
}

.fg-badge--red {
  background: rgba(255, 51, 102, 0.15);
  color: var(--fg-neon-red);
  border: 1px solid rgba(255, 51, 102, 0.3);
}

.fg-badge--red.fg-badge--outline {
  background: transparent;
  box-shadow: 0 0 10px rgba(255, 51, 102, 0.1);
}

/* Pulse dot */
.fg-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: fg-corner-glow 2s ease-in-out infinite;
}
</style>
