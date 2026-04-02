<template>
  <Modal
    v-model="open"
    :title="title"
    :subtitle="subtitle"
    :persistent="persistent"
    size="sm"
  >
    <!-- Icon -->
    <div class="mb-4 flex items-center gap-3">
      <div
        :class="['flex size-11 shrink-0 items-center justify-center rounded-[var(--ds-radius-lg)]', iconBgClass]"
      >
        <LucideIcon :name="icon" size="24" :class="iconColorClass" />
      </div>
      <div>
        <p class="text-sm font-semibold text-[var(--ds-text)]">{{ message }}</p>
        <p v-if="description" class="mt-0.5 text-xs text-[var(--ds-text-muted)]">{{ description }}</p>
      </div>
    </div>

    <slot />

    <template #footer>
      <button
        type="button"
        class="rounded-[var(--ds-radius-lg)] border border-[var(--ds-border)] px-5 py-2.5 text-sm font-semibold text-[var(--ds-text-secondary)] transition-colors hover:bg-[var(--ds-gray-100)]"
        :disabled="loading"
        @click="cancel"
      >
        {{ cancelLabel }}
      </button>
      <button
        type="button"
        :class="['rounded-[var(--ds-radius-lg)] px-5 py-2.5 text-sm font-bold text-white transition-colors disabled:opacity-60', confirmClass]"
        :disabled="loading"
        @click="confirm"
      >
        <span v-if="loading"><LucideIcon name="loader-2" size="18" class="animate-spin" /></span>
        {{ loading ? loadingLabel : confirmLabel }}
      </button>
    </template>
  </Modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import Modal from './Modal.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: 'Xác nhận' },
  subtitle: { type: String, default: '' },
  message: { type: String, default: 'Bạn có chắc chắn muốn tiếp tục?' },
  description: { type: String, default: '' },
  confirmLabel: { type: String, default: 'Xác nhận' },
  cancelLabel: { type: String, default: 'Hủy' },
  loadingLabel: { type: String, default: 'Đang xử lý...' },
  variant: { type: String, default: 'primary', validator: (v) => ['primary', 'danger', 'warning'].includes(v) },
  icon: { type: String, default: 'help' },
  persistent: { type: Boolean, default: false },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const open = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const confirmClass = computed(() => {
  const map = {
    primary: 'bg-[var(--ds-primary)] hover:bg-[var(--ds-primary-hover)]',
    danger: 'bg-[var(--ds-danger)] hover:bg-[var(--ds-danger)]/90',
    warning: 'bg-[var(--ds-accent)] hover:bg-[var(--ds-accent)]/90'
  }
  return map[props.variant] || map.primary
})

const iconBgClass = computed(() => {
  const map = {
    primary: 'bg-[var(--ds-primary-soft)] text-[var(--ds-primary)]',
    danger: 'bg-[var(--ds-danger-bg)] text-[var(--ds-danger)]',
    warning: 'bg-[var(--ds-accent-bg)] text-[var(--ds-accent)]'
  }
  return map[props.variant] || map.primary
})

const iconColorClass = computed(() => {
  const map = {
    primary: 'text-[var(--ds-primary)]',
    danger: 'text-[var(--ds-danger)]',
    warning: 'text-[var(--ds-accent)]'
  }
  return map[props.variant] || map.primary
})

const confirm = () => emit('confirm')
const cancel = () => emit('cancel')
</script>

