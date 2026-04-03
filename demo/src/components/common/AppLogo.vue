<template>
  <component
    :is="tag"
    :class="wrapperClass"
    v-bind="$attrs"
  >
    <!-- SVG Mark -->
    <span :class="iconClass" aria-hidden="true">
      <slot name="icon">
        <svg
          :width="iconSize"
          :height="iconSize"
          viewBox="0 0 40 40"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          class="app-logo__svg"
        >
          <!-- Background circle -->
          <circle cx="20" cy="20" r="20" :fill="circleBg" />
          <!-- Book icon -->
          <path
            d="M20 10C19.5 10 19.1 10.4 19 11L11 16V27C11 28.1 11.9 29 13 29H17V19H23V29H27C28.1 29 29 28.1 29 27V16L21 11C20.9 10.4 20.5 10 20 10Z"
            :fill="iconFill"
          />
          <!-- Accent dot -->
          <circle cx="24" cy="15" r="3" :fill="accentColor" opacity="0.9" />
        </svg>
      </slot>
    </span>

    <!-- Text -->
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
    default: 'md', // 'sm' | 'md' | 'lg'
    validator: v => ['sm', 'md', 'lg'].includes(v)
  },
  variant: {
    type: String,
    default: 'default', // 'default' | 'light' | 'brand'
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

const iconSize = computed(() => ({
  sm: 28,
  md: 36,
  lg: 44
})[props.size])

const circleBg = computed(() => {
  const map = {
    light: 'rgba(255,255,255,0.18)',
    brand: '#4f46e5',
    default: 'rgba(79,70,229,0.12)'
  }
  return map[props.variant] || map.default
})

const iconFill = computed(() => {
  const map = {
    light: 'rgba(255,255,255,0.9)',
    brand: 'rgba(255,255,255,0.95)',
    default: '#4f46e5'
  }
  return map[props.variant] || map.default
})

const accentColor = computed(() => {
  const map = {
    light: '#fbbf24',
    brand: '#fbbf24',
    default: '#818cf8'
  }
  return map[props.variant] || map.default
})

const wrapperClass = computed(() => [
  'app-logo',
  `app-logo--${props.size}`,
  props.animate ? 'app-logo--animate' : ''
])

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
  from {
    opacity: 0;
    transform: scale(0.85) translateY(-4px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.app-logo__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.app-logo__svg {
  display: block;
  filter: drop-shadow(0 2px 8px rgba(79, 70, 229, 0.3));
  transition: filter 0.2s ease;
}

.app-logo:hover .app-logo__svg {
  filter: drop-shadow(0 4px 16px rgba(79, 70, 229, 0.45));
}

.app-logo__icon--sm .app-logo__svg { filter: none; }
.app-logo__icon--sm:hover .app-logo__svg { filter: none; }

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

.app-logo__wordmark { color: var(--color-text, #0f172a); }
.app-logo__wordmark--accent { color: #4f46e5; }

/* Light variant — for dark backgrounds */
.app-logo--light .app-logo__wordmark { color: rgba(255, 255, 255, 0.95); }
.app-logo--light .app-logo__wordmark--accent { color: #a5b4fc; }

/* Brand variant — fully white */
.app-logo--brand .app-logo__wordmark { color: rgba(255, 255, 255, 0.95); }
.app-logo--brand .app-logo__wordmark--accent { color: #c7d2fe; }

@media (prefers-reduced-motion: reduce) {
  .app-logo--animate {
    animation: none;
  }
}
</style>
