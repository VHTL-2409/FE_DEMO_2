<template>
  <Teleport to="body">
    <div
      v-if="modelValue"
      class="fixed inset-0 z-[100] flex items-center justify-center p-4"
      role="presentation"
    >
      <div
        class="absolute inset-0 bg-slate-900/50 backdrop-blur-sm"
        aria-hidden="true"
        @click="onOverlayClick"
      />
      <div
        ref="panelRef"
        class="relative z-10 w-full max-w-lg rounded-2xl border border-[color:var(--color-border)] bg-[color:var(--color-surface)] dark:bg-slate-900 shadow-[var(--shadow-lg)] max-h-[90vh] flex flex-col"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
        @keydown="onPanelKeydown"
      >
        <div class="flex items-start justify-between gap-3 p-5 border-b border-slate-200 dark:border-slate-700 shrink-0">
          <h2 :id="titleId" class="text-lg font-bold text-slate-900 dark:text-slate-100 pr-2">
            {{ title }}
          </h2>
          <button
            v-if="!persistent"
            type="button"
            class="p-2 rounded-lg text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 portal-focus"
            aria-label="Đóng"
            @click="close"
          >
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="portal-scrollbar flex min-h-0 flex-1 flex-col overflow-y-auto overscroll-contain p-5">
          <slot />
        </div>
        <div v-if="$slots.footer" class="p-5 border-t border-slate-200 dark:border-slate-700 shrink-0">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useFocusTrap } from '../../composables/useFocusTrap'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  title: { type: String, required: true },
  persistent: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const panelRef = ref(null)
const titleId = `modal-title-${Math.random().toString(36).slice(2, 9)}`

const isActive = computed(() => props.modelValue)

useFocusTrap(panelRef, isActive)

const close = () => {
  emit('update:modelValue', false)
}

const onOverlayClick = () => {
  if (!props.persistent) close()
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
