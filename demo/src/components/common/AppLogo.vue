<template>
  <component
    :is="tag"
    :class="wrapperClass"
    v-bind="$attrs"
  >
    <!-- SVG Brand Mark — inline để không phụ thuộc file ngoài -->
    <span :class="iconClass" aria-hidden="true">
      <slot name="icon">
        <svg
          :width="iconSize"
          :height="iconSize"
          :viewBox="viewBox"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          class="app-logo__svg"
        >
          <defs>
            <linearGradient id="logo-grad" x1="0" y1="0" x2="1" y2="1" gradientUnits="objectBoundingBox">
              <stop offset="0%" :stop-color="gradStart" />
              <stop offset="100%" :stop-color="gradEnd" />
            </linearGradient>
          </defs>

          <!-- Squircle background -->
          <rect
            x="0.5" y="0.5"
            :width="canvasSize - 1" :height="canvasSize - 1"
            rx="25%"
            fill="url(#logo-grad)"
          />

          <!-- Graduation cap body -->
          <path
            :d="capPath"
            fill="white"
            fill-opacity="0.95"
          />
          <!-- Cap button / tassel knob -->
          <circle
            :cx="tasselKnob.cx"
            :cy="tasselKnob.cy"
            :r="tasselKnob.r"
            fill="#fbbf24"
          />
          <!-- Tassel string -->
          <path
            :d="tasselString"
            stroke="#fbbf24"
            :stroke-width="tasselStroke"
            stroke-linecap="round"
            fill="none"
          />
          <!-- Tassel fringe -->
          <path
            :d="tasselFringe"
            stroke="#fbbf24"
            :stroke-width="tasselStroke"
            stroke-linecap="round"
            stroke-linejoin="round"
            fill="none"
          />
        </svg>
      </slot>
    </span>

    <span v-if="showText" :class="textClass">
      <slot>
        <span class="app-logo__wordmark">Edu</span><span class="app-logo__wordmark app-logo__wordmark--accent">Exam</span>
      </slot>
    </span>
  </component>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: {
    type: String,
    default: 'md',
    validator: v => ['sm', 'md', 'lg'].includes(v)
  },
  variant: {
    type: String,
    default: 'default'
  },
  showText: {
    type: Boolean,
    default: true
  },
  tag: {
    type: String,
    default: 'div'
  },
  animate: {
    type: Boolean,
    default: false
  }
})

/* ─── Kích thước ───────────────────────────────────────────── */
const iconSize = computed(() => ({
  sm: 36,
  md: 44,
  lg: 56
})[props.size])

/* Canvas gốc cho viewBox */
const canvasSize = 64

const viewBox = `0 0 ${canvasSize} ${canvasSize}`

/* ─── Gradient màu theo variant ─────────────────────────── */
const gradStart = computed(() => ({
  light:  '#6366f1',
  brand:  '#4f46e5',
  default:'#4f46e5'
})[props.variant] || '#4f46e5')

const gradEnd = computed(() => ({
  light:  '#818cf8',
  brand:  '#6366f1',
  default:'#6366f1'
})[props.variant] || '#6366f1')

/* ─── Cap shape — scale từ gốc 64 ─────────────────────── */
const s = canvasSize / 64  // 1

const capPath = computed(() => {
  const p = s
  // Diamond top
  const mx = 32 * p
  const my = 18 * p
  const hw = 22 * p  // half width of cap top
  const hh = 7 * p   // half height
  // Left / right corners of cap base
  const lx = 14 * p
  const ly = 33 * p
  const rx = 50 * p
  const ry = 33 * p
  // Cap board
  return [
    `M ${mx} ${my - hh}`,
    `L ${mx + hw} ${my}`,
    `L ${rx} ${ly}`,
    `L ${mx} ${ly}`,
    `L ${lx} ${ly}`,
    `L ${mx - hw} ${my}`,
    'Z'
  ].join(' ')
})

const tasselKnob = computed(() => {
  const p = s
  return { cx: 50 * p, cy: 24 * p, r: 2.5 * p }
})

const tasselStroke = computed(() => 1.8 * s)

const tasselString = computed(() => {
  const p = s
  const kx = tasselKnob.value.cx
  const ky = tasselKnob.value.cy
  const tx = 50 * p
  const ty = 42 * p
  return `M ${kx} ${ky} Q ${kx + 2 * p} ${ky + 10 * p} ${tx} ${ty}`
})

const tasselFringe = computed(() => {
  const p = s
  const tx = 50 * p
  const ty = 42 * p
  const tw = 4 * p
  const th = 7 * p
  return [
    `M ${tx - tw} ${ty}`,
    `L ${tx} ${ty + th}`,
    `L ${tx + tw} ${ty}`
  ].join(' ')
})

/* ─── Classes ─────────────────────────────────────────────── */
const wrapperClass = computed(() => [
  'app-logo',
  `app-logo--${props.size}`,
  props.variant === 'light' ? 'app-logo--light' : '',
  props.variant === 'brand' ? 'app-logo--brand' : '',
  props.animate ? 'app-logo--animate' : ''
].filter(Boolean))

const iconClass = computed(() => [
  'app-logo__icon',
  `app-logo__icon--${props.size}`
])

const textClass = computed(() => [
  'app-logo__text',
  `app-logo__text--${props.size}`
])
</script>

<style scoped>
.app-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: inherit;
  flex-shrink: 0;
}

.app-logo--animate {
  animation: app-logo-enter 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

@keyframes app-logo-enter {
  from { opacity: 0; transform: scale(0.85) translateY(-4px); }
  to   { opacity: 1; transform: scale(1) translateY(0); }
}

.app-logo__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 0;
}

.app-logo__svg {
  display: block;
  filter: drop-shadow(0 3px 10px rgba(79, 70, 229, 0.35));
  transition: filter 0.2s ease, transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.app-logo:hover .app-logo__svg {
  filter: drop-shadow(0 6px 18px rgba(79, 70, 229, 0.5));
  transform: scale(1.05);
}

.app-logo__icon--sm .app-logo__svg {
  filter: drop-shadow(0 2px 8px rgba(79, 70, 229, 0.25));
}

.app-logo__icon--sm:hover .app-logo__svg {
  filter: drop-shadow(0 4px 14px rgba(79, 70, 229, 0.4));
  transform: scale(1.04);
}

/* Text */
.app-logo__text {
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1;
  white-space: nowrap;
  user-select: none;
}

.app-logo__text--sm { font-size: 0.95rem; }
.app-logo__text--md { font-size: 1.2rem; }
.app-logo__text--lg { font-size: 1.5rem; }

.app-logo__wordmark {
  color: var(--ds-text, var(--color-text, #0f172a));
}
.app-logo__wordmark--accent {
  color: var(--ds-primary, #4f46e5);
}

/* Light variant */
.app-logo--light .app-logo__wordmark { color: rgba(255, 255, 255, 0.95); }
.app-logo--light .app-logo__wordmark--accent { color: #a5b4fc; }

/* Brand variant */
.app-logo--brand .app-logo__wordmark { color: rgba(255, 255, 255, 0.95); }
.app-logo--brand .app-logo__wordmark--accent { color: #c7d2fe; }

@media (prefers-reduced-motion: reduce) {
  .app-logo--animate { animation: none; }
  .app-logo:hover .app-logo__svg { transform: none; }
}
</style>
