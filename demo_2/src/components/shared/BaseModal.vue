<template>
  <Teleport to="body">
    <Transition name="gs-modal">
      <div
        v-if="modelValue"
        class="gs-modal-backdrop"
        role="presentation"
      >
        <div
          class="gs-modal"
          :class="sizeClass"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="titleId"
          @keydown="onPanelKeydown"
        >
          <!-- Header -->
          <div class="gs-modal__header">
            <h2 :id="titleId" class="gs-modal__title">
              {{ title }}
            </h2>
            <button
              v-if="!persistent"
              type="button"
              class="gs-spring gs-btn gs-btn--ghost w-9 h-9 p-0"
              aria-label="Đóng"
              @click="close"
            >
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <!-- Body -->
          <div class="gs-modal__body">
            <slot />
          </div>

          <!-- Footer -->
          <div v-if="$slots.footer && !hideFooter" class="gs-modal__footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useFocusTrap } from '../../composables/useFocusTrap'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  title: { type: String, required: true },
  persistent: { type: Boolean, default: false },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg', 'xl'].includes(v)
  },
  hideFooter: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const panelRef = ref(null)
const titleId = `modal-title-${Math.random().toString(36).slice(2, 9)}`

const isActive = computed(() => props.modelValue)

useFocusTrap(panelRef, isActive)

const sizeClass = computed(() => {
  const sizeMap = {
    sm: 'gs-modal--sm',
    md: '',
    lg: 'gs-modal--lg',
    xl: 'gs-modal--xl'
  }
  return sizeMap[props.size] || ''
})

const close = () => {
  emit('update:modelValue', false)
}

const onPanelKeydown = (e) => {
  if (e.key === 'Escape') {
    e.stopPropagation()
    close()
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
  }
)
</script>

<style scoped>
/* Transition animations */
.gs-modal-enter-active {
  transition: opacity var(--duration-base) var(--ease-out);
}

.gs-modal-leave-active {
  transition: opacity var(--duration-fast) var(--ease-in);
}

.gs-modal-enter-from,
.gs-modal-leave-to {
  opacity: 0;
}

.gs-modal-enter-active .gs-modal {
  animation: gsModalIn var(--duration-slow) var(--ease-spring) forwards;
}

.gs-modal-leave-active .gs-modal {
  animation: gsModalOut var(--duration-fast) var(--ease-in) forwards;
}

@keyframes gsModalIn {
  from {
    opacity: 0;
    transform: scale(0.93) translateY(12px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes gsModalOut {
  from {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
  to {
    opacity: 0;
    transform: scale(0.95) translateY(8px);
  }
}

/* XL size variant */
.gs-modal--xl {
  max-width: 900px;
}

/* Scrollable body */
.gs-modal__body {
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: var(--glass-border-hover) transparent;
}

.gs-modal__body::-webkit-scrollbar {
  width: 4px;
}

.gs-modal__body::-webkit-scrollbar-thumb {
  background: var(--glass-border-hover);
  border-radius: 2px;
}
</style>
