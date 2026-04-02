<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: {
    type: [Number, String],
    default: 0
  },
  label: {
    type: String,
    default: ''
  },
  icon: {
    type: Boolean,
    default: false
  },
  trend: {
    type: Number,
    default: null
  },
  trendLabel: {
    type: String,
    default: ''
  },
  glow: {
    type: Boolean,
    default: false
  },
  color: {
    type: String,
    default: 'cyan',
    validator: (v) => ['cyan', 'magenta', 'amber', 'violet', 'green', 'red'].includes(v)
  }
})

const trendDirection = computed(() => {
  if (props.trend === null) return null
  if (props.trend > 0) return 'up'
  if (props.trend < 0) return 'down'
  return 'neutral'
})

const trendClass = computed(() => {
  if (trendDirection.value === 'up') return 'fg-stat-card__trend--up'
  if (trendDirection.value === 'down') return 'fg-stat-card__trend--down'
  return ''
})

const formattedValue = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toLocaleString()
  }
  return props.value
})
</script>

<template>
  <div class="fg-stat-card fg-glass" :class="{ 'fg-stat-card--glow': glow }">
    <!-- Glow effect behind icon -->
    <div v-if="glow" class="fg-stat-card__glow-bg" />
    
    <!-- Icon -->
    <div v-if="icon && $slots.icon" class="fg-stat-card__icon">
      <slot name="icon" />
    </div>
    
    <!-- Value -->
    <div class="fg-stat-card__value">
      {{ formattedValue }}
    </div>
    
    <!-- Label -->
    <div v-if="label" class="fg-stat-card__label">
      {{ label }}
    </div>
    
    <!-- Trend -->
    <div v-if="trend !== null" class="fg-stat-card__trend" :class="trendClass">
      <svg v-if="trendDirection === 'up'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
        <polyline points="17 6 23 6 23 12"/>
      </svg>
      <svg v-else-if="trendDirection === 'down'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="23 18 13.5 8.5 8.5 13.5 1 6"/>
        <polyline points="17 18 23 18 23 12"/>
      </svg>
      <span>{{ Math.abs(trend) }}%</span>
      <span v-if="trendLabel" class="fg-stat-card__trend-label">{{ trendLabel }}</span>
    </div>
    
    <!-- Subtitle slot -->
    <div v-if="$slots.subtitle" class="fg-stat-card__subtitle">
      <slot name="subtitle" />
    </div>
  </div>
</template>

<style scoped>
.fg-stat-card {
  position: relative;
  padding: 24px;
  text-align: center;
  overflow: hidden;
}

.fg-stat-card__glow-bg {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 120px;
  height: 120px;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(0, 245, 255, 0.15), transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  animation: fg-pulse-glow 3s ease-in-out infinite;
}

.fg-stat-card__icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 16px;
  border-radius: 50%;
  background: rgba(0, 245, 255, 0.1);
  border: 1px solid rgba(0, 245, 255, 0.2);
}

.fg-stat-card__icon :deep(svg) {
  width: 28px;
  height: 28px;
  color: var(--fg-neon-cyan);
}

.fg-stat-card--glow .fg-stat-card__icon {
  animation: fg-pulse-glow 3s ease-in-out infinite;
  background: rgba(0, 245, 255, 0.15);
}

.fg-stat-card__value {
  font-family: 'Manrope', sans-serif;
  font-size: 36px;
  font-weight: 800;
  line-height: 1;
  color: #ffffff;
  margin-bottom: 8px;
  position: relative;
}

.fg-stat-card__label {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.6);
}

.fg-stat-card__trend {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 12px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
  border-radius: var(--fg-radius-sm);
}

.fg-stat-card__trend svg {
  width: 14px;
  height: 14px;
}

.fg-stat-card__trend--up {
  color: var(--fg-neon-green);
  background: rgba(0, 255, 136, 0.1);
}

.fg-stat-card__trend--down {
  color: var(--fg-neon-red);
  background: rgba(255, 51, 102, 0.1);
}

.fg-stat-card__trend-label {
  color: rgba(255, 255, 255, 0.5);
  font-weight: 400;
  margin-left: 4px;
}

.fg-stat-card__subtitle {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}
</style>
