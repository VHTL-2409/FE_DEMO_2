<template>
  <div :class="rootClass" role="progressbar" :aria-valuenow="value" :aria-valuemin="0" :aria-valuemax="100">
    <div v-if="showLabel" class="progress-label">
      <span v-if="label" class="progress-label-text">{{ label }}</span>
      <span v-if="percent !== null" class="progress-label-value">{{ percent }}%</span>
    </div>
    <div class="progress-track">
      <div
        class="progress-fill"
        :class="fillClass"
        :style="{ width: `${percent}%` }"
      />
    </div>
    <div v-if="steps" class="progress-steps">
      <div
        v-for="i in steps"
        :key="i"
        class="progress-step"
        :class="{ 'is-active': i <= Math.ceil((percent / 100) * steps) }"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: Number, default: 0 },
  max: { type: Number, default: 100 },
  label: { type: String, default: '' },
  showLabel: { type: Boolean, default: false },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  },
  color: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'success', 'warning', 'danger', 'info', 'gradient'].includes(v)
  },
  animated: { type: Boolean, default: true },
  steps: { type: Number, default: 0 }
})

const percent = computed(() => {
  return Math.min(100, Math.max(0, Math.round((props.value / props.max) * 100)))
})

const fillClass = computed(() => {
  const classes = []
  if (props.color !== 'primary') classes.push(`progress-fill--${props.color}`)
  if (props.animated) classes.push('progress-fill--animated')
  return classes.join(' ')
})

const rootClass = computed(() => {
  return ['progress', `progress--${props.size}`].join(' ')
})
</script>

<style scoped>
.progress {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8rem;
}

.progress-label-text {
  color: var(--glass-text-secondary, #57534e);
  font-weight: 500;
}

.progress-label-value {
  color: var(--glass-amber, #8d4b00);
  font-weight: 700;
}

.progress-track {
  width: 100%;
  background: var(--glass-bg-mid, #efeeea);
  border-radius: var(--radius-glass-pill, 999px);
  overflow: hidden;
}

.progress--sm .progress-track { height: 4px; }
.progress--md .progress-track { height: 8px; }
.progress--lg .progress-track { height: 12px; }

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--glass-amber) 0%, var(--glass-amber-hover) 100%);
  transition: width 0.6s cubic-bezier(0.22, 1, 0.36, 1);
  min-width: 0;
}

.progress-fill--animated {
  background: linear-gradient(
    90deg,
    var(--glass-amber) 0%,
    var(--glass-amber-400, #ea8c3c) 50%,
    var(--glass-amber) 100%
  );
  background-size: 200% 100%;
  animation: progressShimmer 2s ease-in-out infinite;
}

.progress-fill--success {
  background: var(--glass-success, #16a34a) !important;
}
.progress-fill--warning {
  background: var(--glass-warning, #d97706) !important;
}
.progress-fill--danger {
  background: var(--glass-danger, #dc2626) !important;
}
.progress-fill--info {
  background: var(--glass-info, #0284c7) !important;
}
.progress-fill--gradient {
  background: linear-gradient(90deg, var(--glass-amber), #6366f1) !important;
}

@keyframes progressShimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Step indicators */
.progress-steps {
  display: flex;
  gap: 0.25rem;
}

.progress-step {
  flex: 1;
  height: 3px;
  border-radius: 2px;
  background: var(--glass-border, rgba(0,0,0,0.07));
  transition: background 0.3s;
}

.progress-step.is-active {
  background: var(--glass-amber, #8d4b00);
}
</style>
