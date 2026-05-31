<template>
  <div :class="['auto-save-indicator flex items-center gap-2 text-[11px] font-semibold', colorClass]">
    <LucideIcon :name="icon" size="14" />
    <span>{{ label }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: 'idle' }
})

const icon = computed(() => {
  const map = {
    saving: 'progress_activity',
    saved: 'check_circle',
    pending: 'schedule',
    error: 'cloud_off',
    idle: 'cloud_done'
  }
  return map[props.status] || map.idle
})

const colorClass = computed(() => {
  const map = {
    saving: 'text-[var(--ds-info)]',
    saved: 'text-[var(--ds-success)]',
    pending: 'text-[var(--ds-warning)]',
    error: 'text-[var(--ds-danger)]',
    idle: 'text-[var(--ds-text-muted)]'
  }
  return map[props.status] || map.idle
})

const label = computed(() => {
  const map = {
    saving: 'Đang lưu...',
    saved: 'Đã lưu',
    pending: 'Chưa lưu',
    error: 'Lỗi lưu',
    idle: ''
  }
  return map[props.status] || ''
})
</script>

