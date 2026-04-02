<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  size: {
    type: Number,
    default: 100
  },
  strokeWidth: {
    type: Number,
    default: 6
  },
  value: {
    type: Number,
    default: 0
  },
  maxValue: {
    type: Number,
    default: 100
  },
  color: {
    type: String,
    default: 'cyan',
    validator: (v) => ['cyan', 'magenta', 'amber', 'violet', 'green', 'red'].includes(v)
  },
  showValue: {
    type: Boolean,
    default: true
  },
  showPercent: {
    type: Boolean,
    default: false
  },
  animated: {
    type: Boolean,
    default: true
  },
  duration: {
    type: Number,
    default: 1000
  }
})

const progress = ref(0)
const displayValue = ref(0)

const radius = (props.size - props.strokeWidth) / 2
const circumference = 2 * Math.PI * radius

const colorMap = {
  cyan: 'url(#fg-gradient-cyan)',
  magenta: 'url(#fg-gradient-magenta)',
  amber: 'url(#fg-gradient-amber)',
  violet: 'url(#fg-gradient-violet)',
  green: 'url(#fg-gradient-green)',
  red: 'url(#fg-gradient-red)'
}

const strokeDashoffset = computed(() => {
  const percent = (props.animated ? progress.value : (props.value / props.maxValue * 100)) / 100
  return circumference * (1 - percent)
})

import { computed } from 'vue'

onMounted(() => {
  if (props.animated) {
    const startTime = Date.now()
    const animate = () => {
      const elapsed = Date.now() - startTime
      const percent = Math.min(elapsed / props.duration, 1)
      
      progress.value = (props.value / props.maxValue) * 100 * easeOutQuart(percent)
      
      if (percent < 1) {
        requestAnimationFrame(animate)
      }
    }
    requestAnimationFrame(animate)
  }
})

const easeOutQuart = (t) => {
  return 1 - Math.pow(1 - t, 4)
}
</script>

<template>
  <div class="fg-progress-ring" :style="{ width: size + 'px', height: size + 'px' }">
    <svg 
      :width="size" 
      :height="size" 
      :viewBox="`0 0 ${size} ${size}`"
      class="fg-progress-ring__svg"
    >
      <defs>
        <linearGradient id="fg-gradient-cyan" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#00f5ff" />
          <stop offset="100%" stop-color="#006d75" />
        </linearGradient>
        <linearGradient id="fg-gradient-magenta" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#ff00ff" />
          <stop offset="100%" stop-color="#990066" />
        </linearGradient>
        <linearGradient id="fg-gradient-amber" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#ffb800" />
          <stop offset="100%" stop-color="#996600" />
        </linearGradient>
        <linearGradient id="fg-gradient-violet" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#8b5cf6" />
          <stop offset="100%" stop-color="#5b21b6" />
        </linearGradient>
        <linearGradient id="fg-gradient-green" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#00ff88" />
          <stop offset="100%" stop-color="#009955" />
        </linearGradient>
        <linearGradient id="fg-gradient-red" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#ff3366" />
          <stop offset="100%" stop-color="#991133" />
        </linearGradient>
      </defs>
      
      <!-- Background track -->
      <circle
        :cx="size / 2"
        :cy="size / 2"
        :r="radius"
        fill="none"
        :stroke="'rgba(255, 255, 255, 0.1)'"
        :stroke-width="strokeWidth"
      />
      
      <!-- Progress fill -->
      <circle
        :cx="size / 2"
        :cy="size / 2"
        :r="radius"
        fill="none"
        :stroke="colorMap[color]"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="strokeDashoffset"
        class="fg-progress-ring__fill"
      />
    </svg>
    
    <!-- Center content -->
    <div v-if="showValue || showPercent" class="fg-progress-ring__content">
      <span v-if="showValue" class="fg-progress-ring__value">{{ Math.round(value) }}</span>
      <span v-if="showPercent" class="fg-progress-ring__percent">%</span>
    </div>
  </div>
</template>

<style scoped>
.fg-progress-ring {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.fg-progress-ring__svg {
  transform: rotate(-90deg);
}

.fg-progress-ring__fill {
  transition: stroke-dashoffset 1s cubic-bezier(0.33, 1, 0.68, 1);
  filter: drop-shadow(0 0 6px rgba(0, 245, 255, 0.5));
}

.fg-progress-ring__content {
  position: absolute;
  display: flex;
  align-items: baseline;
  justify-content: center;
}

.fg-progress-ring__value {
  font-family: 'Manrope', sans-serif;
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
}

.fg-progress-ring__percent {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.6);
  margin-left: 2px;
}
</style>
