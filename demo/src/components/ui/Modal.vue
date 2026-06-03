<template>
  <Teleport to="body">
    <Transition name="ds-modal">
      <div
        v-if="modelValue"
        class="ds-modal-overlay fixed inset-0 z-[1000] flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="title || $slots.header ? labelledBy : undefined"
        @click.self="!persistent && close()"
        @keydown.esc="!persistent && close()"
      >
        <div
          :class="['ds-modal-content rounded-[var(--ds-radius-2xl)] border bg-[var(--ds-surface)] shadow-[var(--ds-shadow-lg)]', sizeClass]"
          tabindex="-1"
          ref="dialogRef"
        >
          <!-- Header -->
          <div v-if="title || $slots.header" class="flex items-center justify-between gap-4 border-b border-[var(--ds-border)] px-6 py-4">
            <div>
              <slot name="header" :title-id="labelledBy" :title="title" :subtitle="subtitle">
                <h2 :id="labelledBy" class="text-lg font-bold text-[var(--ds-text)]">{{ title }}</h2>
                <p v-if="subtitle" class="mt-0.5 text-sm text-[var(--ds-text-muted)]">{{ subtitle }}</p>
              </slot>
            </div>
            <button
              v-if="!persistent"
              type="button"
              class="ds-modal-close ds-icon-btn size-8 min-h-8 min-w-8 border-0 shadow-none"
              aria-label="Đóng"
              @click="close"
            >
              <LucideIcon name="close" />
            </button>
          </div>

          <!-- Body -->
          <div class="ds-modal-body max-h-[calc(90vh-8rem)] overflow-y-auto px-6 py-5">
            <slot />
          </div>

          <!-- Footer -->
          <div v-if="$slots.footer" class="flex items-center justify-end gap-3 border-t border-[var(--ds-border)] px-6 py-4">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch, nextTick, onUnmounted } from 'vue'

defineOptions({ name: 'UiModal' })

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  labelId: { type: String, default: '' },
  size: { type: String, default: 'md', validator: (v) => ['sm', 'md', 'lg', 'xl', 'full'].includes(v) },
  persistent: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'close'])

const dialogRef = ref(null)
const titleId = `modal-title-${Math.random().toString(36).slice(2, 9)}`
const labelledBy = computed(() => props.labelId || titleId)

const sizeClass = computed(() => {
  const map = {
    sm: 'w-full max-w-sm',
    md: 'w-full max-w-lg',
    lg: 'w-full max-w-2xl',
    xl: 'w-full max-w-4xl',
    full: 'w-full max-w-[calc(100vw-2rem)] h-[calc(100vh-2rem)]'
  }
  return map[props.size] || map.md
})

const close = () => {
  emit('update:modelValue', false)
  emit('close')
}

// Global Escape handler: bắt cả khi focus rơi ra ngoài modal (vd: do Teleport,
// hoặc user vừa click overlay làm mất focus). @keydown.esc ở overlay không
// đủ vì không bubble nếu focus đang ở body.
const handleGlobalEscape = (event) => {
  if (event.key === 'Escape' && !props.persistent && props.modelValue) {
    event.stopPropagation()
    close()
  }
}

// Focus trap
let previousFocus = null

watch(() => props.modelValue, async (open) => {
  if (open) {
    previousFocus = document.activeElement
    await nextTick()
    dialogRef.value?.focus()
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', handleGlobalEscape)
  } else {
    document.body.style.overflow = ''
    window.removeEventListener('keydown', handleGlobalEscape)
    previousFocus?.focus?.()
  }
})

onUnmounted(() => {
  document.body.style.overflow = ''
  window.removeEventListener('keydown', handleGlobalEscape)
})
</script>


<style scoped>
.ds-modal-overlay {
  background: rgba(15, 23, 42, 0.54);
}

.ds-modal-enter-active,
.ds-modal-leave-active {
  transition: opacity 0.2s ease;
}
.ds-modal-enter-active .ds-modal-content,
.ds-modal-leave-active .ds-modal-content {
  transition: opacity 0.2s ease;
}
.ds-modal-enter-from,
.ds-modal-leave-to {
  opacity: 0;
}
.ds-modal-enter-from .ds-modal-content {
  opacity: 0;
}
.ds-modal-leave-to .ds-modal-content {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .ds-modal-enter-active,
  .ds-modal-leave-active,
  .ds-modal-enter-active .ds-modal-content,
  .ds-modal-leave-active .ds-modal-content {
    transition-duration: 0.01ms !important;
  }
}
</style>
