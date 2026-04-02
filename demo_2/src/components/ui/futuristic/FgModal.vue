<script setup>
import { computed, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg', 'xl', 'full'].includes(v)
  },
  closable: {
    type: Boolean,
    default: true
  },
  closeOnBackdrop: {
    type: Boolean,
    default: true
  },
  showHeader: {
    type: Boolean,
    default: true
  },
  showFooter: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'close', 'opened', 'closed'])

const modalClasses = computed(() => [
  'fg-modal',
  `fg-modal--${props.size}`
])

const close = () => {
  emit('update:modelValue', false)
  emit('close')
}

const handleBackdropClick = (e) => {
  if (props.closeOnBackdrop && e.target === e.currentTarget) {
    close()
  }
}

const handleKeydown = (e) => {
  if (e.key === 'Escape' && props.closable) {
    close()
  }
}

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    emit('opened')
    document.body.style.overflow = 'hidden'
  } else {
    emit('closed')
    document.body.style.overflow = ''
  }
})

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fg-modal">
      <div 
        v-if="modelValue" 
        class="fg-modal-backdrop"
        @click="handleBackdropClick"
      >
        <div :class="modalClasses" role="dialog" aria-modal="true">
          <!-- Header -->
          <div v-if="showHeader" class="fg-modal__header">
            <h2 class="fg-modal__title">{{ title }}</h2>
            
            <button 
              v-if="closable"
              class="fg-modal__close"
              @click="close"
              aria-label="Close modal"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
          
          <!-- Body -->
          <div class="fg-modal__body">
            <slot />
          </div>
          
          <!-- Footer -->
          <div v-if="showFooter && ($slots.footer || $slots['footer-left'])" class="fg-modal__footer">
            <div class="fg-modal__footer-left">
              <slot name="footer-left" />
            </div>
            <div class="fg-modal__footer-right">
              <slot name="footer" />
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fg-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.fg-modal {
  position: relative;
  width: 100%;
  max-height: calc(100vh - 40px);
  overflow: hidden;
  background: var(--fg-surface-dark);
  border: 1px solid var(--fg-glass-border);
  border-radius: var(--fg-radius-xl);
  box-shadow: var(--fg-depth-float);
  display: flex;
  flex-direction: column;
}

.fg-modal::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  padding: 1px;
  background: linear-gradient(
    135deg,
    rgba(0, 245, 255, 0.3) 0%,
    transparent 30%,
    transparent 70%,
    rgba(139, 92, 246, 0.3) 100%
  );
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

/* Modal sizes */
.fg-modal--sm { max-width: 400px; }
.fg-modal--md { max-width: 560px; }
.fg-modal--lg { max-width: 800px; }
.fg-modal--xl { max-width: 1100px; }
.fg-modal--full { max-width: 95vw; max-height: 95vh; }

.fg-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.fg-modal__title {
  font-family: 'Manrope', sans-serif;
  font-size: 20px;
  font-weight: 700;
  color: #ffffff;
  margin: 0;
}

.fg-modal__close {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--fg-radius-sm);
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all var(--fg-duration-fast) var(--fg-ease-out);
}

.fg-modal__close svg {
  width: 18px;
  height: 18px;
}

.fg-modal__close:hover {
  background: rgba(255, 51, 102, 0.2);
  border-color: var(--fg-neon-red);
  color: var(--fg-neon-red);
}

.fg-modal__body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.fg-modal__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.fg-modal__footer-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.fg-modal__footer-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* Transitions */
.fg-modal-enter-active {
  transition: all var(--fg-duration-normal) var(--fg-ease-out);
}

.fg-modal-leave-active {
  transition: all var(--fg-duration-fast) var(--fg-ease-out);
}

.fg-modal-enter-from {
  opacity: 0;
}

.fg-modal-enter-from .fg-modal {
  transform: scale(0.95) translateY(20px);
  opacity: 0;
}

.fg-modal-leave-to {
  opacity: 0;
}

.fg-modal-leave-to .fg-modal {
  transform: scale(0.95);
  opacity: 0;
}
</style>
