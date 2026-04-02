/**
 * ProgressRing.vue
 * Circular progress indicator with customizable size, color, and value
 */
<template>
  <div class="progress-ring" :style="wrapperStyle">
    <svg
      :width="size"
      :height="size"
      :viewBox="`0 0 ${size} ${size}`"
      class="progress-ring__svg"
    >
      <!-- Background circle -->
      <circle
        :cx="center"
        :cy="center"
        :r="radius"
        fill="none"
        :stroke="trackColor"
        :stroke-width="strokeWidth"
        class="progress-ring__track"
      />
      
      <!-- Progress circle -->
      <circle
        :cx="center"
        :cy="center"
        :r="radius"
        fill="none"
        :stroke="progressColor"
        :stroke-width="strokeWidth"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="dashOffset"
        stroke-linecap="round"
        class="progress-ring__progress"
        :style="progressStyle"
      />

      <!-- Glow effect for animated rings -->
      <circle
        v-if="glow"
        :cx="center"
        :cy="center"
        :r="radius"
        fill="none"
        :stroke="progressColor"
        :stroke-width="strokeWidth + 4"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="dashOffset"
        stroke-linecap="round"
        class="progress-ring__glow"
        :style="glowStyle"
      />
    </svg>

    <!-- Center content -->
    <div v-if="$slots.default" class="progress-ring__center">
      <slot />
    </div>

    <!-- Percentage display -->
    <div v-else-if="showValue" class="progress-ring__center">
      <span class="progress-ring__value" :style="{ fontSize: valueFontSize }">
        {{ displayValue }}
      </span>
      <span v-if="showLabel" class="progress-ring__label">{{ label }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: {
    type: Number,
    default: 0
  },
  max: {
    type: Number,
    default: 100
  },
  size: {
    type: Number,
    default: 120
  },
  strokeWidth: {
    type: Number,
    default: 8
  },
  progressColor: {
    type: String,
    default: 'var(--ds-primary)'
  },
  trackColor: {
    type: String,
    default: 'var(--ds-border)'
  },
  animated: {
    type: Boolean,
    default: true
  },
  glow: {
    type: Boolean,
    default: false
  },
  showValue: {
    type: Boolean,
    default: true
  },
  showLabel: {
    type: Boolean,
    default: false
  },
  label: {
    type: String,
    default: '%'
  },
  displayFormat: {
    type: String,
    default: 'percent',
    validator: (v) => ['percent', 'value', 'decimal'].includes(v)
  }
})

const center = computed(() => props.size / 2)
const radius = computed(() => (props.size - props.strokeWidth) / 2)
const circumference = computed(() => 2 * Math.PI * radius.value)

const percentage = computed(() => {
  if (props.max === 0) return 0
  return Math.min(Math.max((props.value / props.max) * 100, 0), 100)
})

const dashOffset = computed(() => {
  const percent = percentage.value
  return circumference.value * (1 - percent / 100)
})

const displayValue = computed(() => {
  if (props.displayFormat === 'percent') {
    return `${Math.round(percentage.value)}`
  }
  if (props.displayFormat === 'value') {
    return `${props.value}/${props.max}`
  }
  return `${props.value.toFixed(1)}`
})

const valueFontSize = computed(() => {
  if (props.size >= 150) return '1.5rem'
  if (props.size >= 100) return '1.25rem'
  return '1rem'
})

const wrapperStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`
}))

const progressStyle = computed(() => {
  const style = {}
  if (props.animated) {
    style.transition = 'stroke-dashoffset 0.8s cubic-bezier(0.4, 0, 0.2, 1)'
  }
  return style
})

const glowStyle = computed(() => {
  const style = {
    filter: `blur(${props.strokeWidth / 2 + 2}px)`,
    opacity: 0.4
  }
  if (props.animated) {
    style.transition = 'stroke-dashoffset 0.8s cubic-bezier(0.4, 0, 0.2, 1)'
  }
  return style
})
</script>

<style scoped>
.progress-ring {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.progress-ring__svg {
  transform: rotate(-90deg);
}

.progress-ring__track {
  opacity: 0.3;
}

.progress-ring__progress {
  transform-origin: center;
  transform: rotate(-90deg);
}

.progress-ring__center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.progress-ring__value {
  font-family: var(--ds-font-display);
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
  letter-spacing: -0.02em;
}

.dark .progress-ring__value {
  color: #f1f5f9;
}

.progress-ring__label {
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-top: 0.125rem;
}
</style>
