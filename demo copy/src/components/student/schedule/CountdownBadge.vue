<template>
  <div class="cd" :class="[`cd--${urgency}`, { 'cd--live': isLive }]">
    <span v-if="isLive" class="cd__live-dot" />
    <span class="cd__val">{{ mainVal }}</span>
    <span class="cd__unit">{{ unitLabel }}</span>
    <span v-if="subVal > 0" class="cd__sub">+{{ subVal }}{{ subUnit }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  targetTime: { type: [Date, String, Number], default: null },
  compact: { type: Boolean, default: false }
})

const now = computed(() => Date.now())

const diff = computed(() => {
  if (!props.targetTime) return null
  const target = new Date(props.targetTime).getTime()
  if (Number.isNaN(target)) return null
  return target - now.value
})

const isLive = computed(() => {
  if (!props.targetTime) return false
  const target = new Date(props.targetTime).getTime()
  if (Number.isNaN(target)) return false
  return Math.abs(target - now.value) < 60 * 1000
})

const urgency = computed(() => {
  if (!diff.value) return 'neutral'
  if (diff.value < 0) return 'past'
  const hours = diff.value / 1000 / 60 / 60
  if (hours <= 1) return 'critical'
  if (hours <= 6) return 'urgent'
  if (hours <= 24) return 'soon'
  if (hours <= 72) return 'upcoming'
  return 'normal'
})

const mainVal = computed(() => {
  if (!diff.value) return '—'
  if (diff.value < 0) {
    const abs = Math.abs(diff.value)
    const hours = Math.floor(abs / 1000 / 60 / 60)
    const days = Math.floor(abs / 1000 / 60 / 60 / 24)
    if (days > 0) return `-${days}`
    if (hours > 0) return `-${hours}`
    return '-' + Math.floor(abs / 1000 / 60)
  }
  const mins = Math.floor(diff.value / 1000 / 60)
  const hours = Math.floor(diff.value / 1000 / 60 / 60)
  const days = Math.floor(diff.value / 1000 / 60 / 60 / 24)
  if (days > 0) return `${days}`
  if (hours > 0) return `${hours}`
  return `${mins}`
})

const unitLabel = computed(() => {
  if (!diff.value) return ''
  if (diff.value < 0) {
    const abs = Math.abs(diff.value)
    const hours = Math.floor(abs / 1000 / 60 / 60)
    const days = Math.floor(abs / 1000 / 60 / 60 / 24)
    if (days > 0) return 'ngay'
    if (hours > 0) return 'gio'
    return 'phut'
  }
  const hours = Math.floor(diff.value / 1000 / 60 / 60)
  const days = Math.floor(diff.value / 1000 / 60 / 60 / 24)
  if (days > 0) return 'ngay'
  if (hours > 0) return 'gio'
  return 'phut'
})

const subVal = computed(() => {
  if (!diff.value || diff.value < 0) return 0
  const mins = Math.floor(diff.value / 1000 / 60)
  const hours = Math.floor(diff.value / 1000 / 60 / 60)
  if (hours > 0) return mins % 60
  return 0
})

const subUnit = computed(() => {
  if (subVal.value <= 0) return ''
  return 'm'
})
</script>

<style scoped>
.cd {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.3rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 800;
  position: relative;
  white-space: nowrap;
}

/* Live dot */
.cd__live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: cdPulse 1.5s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes cdPulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

/* Urgency colors */
.cd--neutral { background: var(--ds-gray-100); color: var(--ds-gray-500); }
.dark .cd--neutral { background: var(--ds-gray-700); color: var(--ds-gray-400); }

.cd--past { background: var(--ds-gray-100); color: var(--ds-gray-400); }
.dark .cd--past { background: var(--ds-gray-700); color: var(--ds-gray-500); }

.cd--normal { background: var(--ds-primary-soft); color: var(--ds-primary); }

.cd--upcoming { background: rgba(14, 165, 233, 0.1); color: var(--ds-info); }

.cd--soon { background: rgba(234, 179, 8, 0.12); color: var(--ds-warning); }

.cd--urgent { background: rgba(234, 88, 12, 0.1); color: #c2410c; }

.cd--critical, .cd--live {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  animation: cdBlink 2s ease-in-out infinite;
}

@keyframes cdBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.cd__val {
  font-family: var(--ds-font-display);
  font-size: 0.9rem;
  font-weight: 900;
  line-height: 1;
}

.cd__unit {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  opacity: 0.85;
}

.cd__sub {
  font-size: 0.65rem;
  font-weight: 600;
  opacity: 0.7;
  margin-left: 0.125rem;
}
</style>
