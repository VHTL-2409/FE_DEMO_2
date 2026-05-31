<template>
  <Teleport to="body">
    <Transition name="ds-drawer">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50"
        role="dialog"
        :aria-label="title || 'Drawer'"
        aria-modal="true"
      >
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-slate-950/55"
          @click="!persistent && close"
        />

        <!-- Panel -->
        <div
          ref="panelRef"
          :class="['ds-drawer-panel fixed top-0 bottom-0 flex flex-col bg-[var(--ds-surface)] shadow-[var(--ds-shadow-xl)] transition-all', widthClass]"
          :style="panelStyle"
        >
          <!-- Header -->
          <div class="flex items-center justify-between border-b border-[var(--ds-border)] px-6 py-4 shrink-0">
            <div>
              <h2 class="text-base font-bold text-[var(--ds-text)]">{{ title }}</h2>
              <p v-if="subtitle" class="mt-0.5 text-xs text-[var(--ds-text-muted)]">{{ subtitle }}</p>
            </div>
            <button
              v-if="!persistent"
              type="button"
              class="flex size-8 items-center justify-center rounded-[var(--ds-radius-md)] text-[var(--ds-text-muted)] transition-colors hover:bg-[var(--ds-gray-100)] hover:text-[var(--ds-text)]"
              aria-label="Đóng"
              @click="close"
            >
              <LucideIcon name="close" />
            </button>
          </div>

          <!-- Scrollable body -->
          <div class="flex-1 overflow-y-auto px-6 py-5">
            <slot />
          </div>

          <!-- Footer -->
          <div v-if="$slots.footer" class="flex items-center justify-end gap-3 border-t border-[var(--ds-border)] px-6 py-4 shrink-0">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  width: { type: String, default: 'md', validator: (v) => ['sm', 'md', 'lg', 'xl', 'full'].includes(v) },
  persistent: { type: Boolean, default: false },
  position: { type: String, default: 'right', validator: (v) => ['left', 'right'].includes(v) }
})

const emit = defineEmits(['update:modelValue', 'close'])

const panelRef = ref(null)

const widthClass = computed(() => {
  const map = { sm: 'w-full max-w-sm', md: 'w-full max-w-lg', lg: 'w-full max-w-2xl', xl: 'w-full max-w-4xl', full: 'w-full' }
  return map[props.width] || map.md
})

const panelStyle = computed(() => ({
  [props.position === 'right' ? 'right' : 'left']: '0',
  width: props.width === 'full' ? '100vw' : undefined
}))

const close = () => {
  emit('update:modelValue', false)
  emit('close')
}

watch(() => props.modelValue, (open) => {
  if (open) {
    document.body.style.overflow = 'hidden'
    nextTick(() => panelRef.value?.focus())
  } else {
    document.body.style.overflow = ''
  }
})
</script>


<style scoped>
.ds-drawer-enter-active,
.ds-drawer-leave-active {
  transition: opacity 0.25s ease;
}
.ds-drawer-enter-active .ds-drawer-panel,
.ds-drawer-leave-active .ds-drawer-panel {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.ds-drawer-enter-from,
.ds-drawer-leave-to {
  opacity: 0;
}
.ds-drawer-enter-from .ds-drawer-panel {
  transform: translateX(100%);
}
.ds-drawer-leave-to .ds-drawer-panel {
  transform: translateX(100%);
}
.ds-drawer-enter-from .ds-drawer-panel[style*="left: 0"] {
  transform: translateX(-100%);
}
.ds-drawer-leave-to .ds-drawer-panel[style*="left: 0"] {
  transform: translateX(-100%);
}
</style>
