<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'elevated', 'flat', 'glow', 'holographic'].includes(v)
  },
  hoverable: {
    type: Boolean,
    default: false
  },
  lift: {
    type: Boolean,
    default: false
  },
  glowColor: {
    type: String,
    default: 'cyan'
  },
  padding: {
    type: String,
    default: 'md'
  },
  radius: {
    type: String,
    default: 'lg'
  }
})

const emit = defineEmits(['click'])

const isHovered = ref(false)

const cardClasses = computed(() => [
  'fg-glass',
  `fg-glass--${props.variant}`,
  {
    'fg-glass--hover-lift': props.lift || props.hoverable,
    [`fg-shadow-${props.glowColor}`]: isHovered.value && props.variant !== 'flat'
  }
])

const paddingClass = computed(() => `fg-padding-${props.padding}`)
const radiusClass = computed(() => `fg-radius-${props.radius}`)

const handleMouseEnter = () => {
  isHovered.value = true
}

const handleMouseLeave = () => {
  isHovered.value = false
}
</script>

<template>
  <div
    :class="[...cardClasses, paddingClass, radiusClass]"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
    @click="emit('click', $event)"
  >
    <!-- Holographic shimmer overlay -->
    <div v-if="variant === 'holographic'" class="fg-holo-overlay" />
    
    <!-- Glow corner effects -->
    <div class="fg-corner-glow fg-corner-glow--tl" />
    <div class="fg-corner-glow fg-corner-glow--br" />
    
    <!-- Content slot -->
    <slot />
  </div>
</template>

<style scoped>
.fg-holo-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(0, 245, 255, 0.1) 0%,
    rgba(139, 92, 246, 0.1) 25%,
    rgba(255, 0, 255, 0.1) 50%,
    rgba(0, 245, 255, 0.1) 75%,
    rgba(139, 92, 246, 0.1) 100%
  );
  background-size: 400% 400%;
  animation: fg-holo-shift 8s ease infinite;
  border-radius: inherit;
  pointer-events: none;
  opacity: 0.5;
}

.fg-corner-glow {
  position: absolute;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.fg-corner-glow--tl {
  top: -50px;
  left: -50px;
  background: radial-gradient(circle, rgba(0, 245, 255, 0.3), transparent 70%);
}

.fg-corner-glow--br {
  bottom: -50px;
  right: -50px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.3), transparent 70%);
}

.fg-glass:hover .fg-corner-glow {
  opacity: 1;
}

/* Padding variants */
.fg-padding-none { padding: 0; }
.fg-padding-sm { padding: 12px; }
.fg-padding-md { padding: 20px; }
.fg-padding-lg { padding: 28px; }
.fg-padding-xl { padding: 36px; }

/* Radius variants */
.fg-radius-sm { border-radius: 8px; }
.fg-radius-md { border-radius: 12px; }
.fg-radius-lg { border-radius: 16px; }
.fg-radius-xl { border-radius: 24px; }
.fg-radius-2xl { border-radius: 32px; }
.fg-radius-full { border-radius: 9999px; }
</style>
